/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.ArcCreateOperation;
import org.objectstyle.cayenne.graph.ArcDeleteOperation;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.graph.GraphMap;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.graph.NodeDeleteOperation;
import org.objectstyle.cayenne.graph.NodeIdChangeOperation;
import org.objectstyle.cayenne.graph.NodePropertyChangeOperation;

/**
 * A GraphMap extension that works together with ObjectContext to track persistent object
 * changes and send events.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
final class ObjectContextGraphManager extends GraphMap {

    static final String COMMIT_MARKER = "commit";
    static final String FLUSH_MARKER = "flush";

    ObjectContext context;
    Collection deadIds;
    boolean changeEventsEnabled;
    boolean lifecycleEventsEnabled;

    ObjectContextStateRecorder stateRecorder;
    ObjectContextOperationRecorder opRecorder;

    ObjectContextGraphManager(ObjectContext context, boolean changeEventsEnabled,
            boolean lifecycleEventsEnabled) {

        this.context = context;
        this.changeEventsEnabled = changeEventsEnabled;
        this.lifecycleEventsEnabled = lifecycleEventsEnabled;

        this.stateRecorder = new ObjectContextStateRecorder(this);
        this.opRecorder = new ObjectContextOperationRecorder();
    }

    boolean hasChanges() {
        return opRecorder.size() > 0;
    }

    boolean hasChangesSinceLastFlush() {
        int size = opRecorder.hasMarker(FLUSH_MARKER) ? opRecorder
                .sizeAfterMarker(FLUSH_MARKER) : opRecorder.size();
        return size > 0;
    }

    GraphDiff getDiffs() {
        return opRecorder.getDiffs();
    }

    GraphDiff getDiffsSinceLastFlush() {
        return opRecorder.hasMarker(FLUSH_MARKER) ? opRecorder
                .getDiffsAfterMarker(FLUSH_MARKER) : opRecorder.getDiffs();
    }

    Collection dirtyNodes() {
        return stateRecorder.dirtyNodes();
    }

    Collection dirtyNodes(int state) {
        return stateRecorder.dirtyNodes(state);
    }

    // ****** Sync Events API *****
    void processSyncWithChild(GraphDiff childDiff) {
        throw new CayenneRuntimeException("Not implemented yet");
    }

    /**
     * Clears commit marker, but keeps all recorded operations.
     */
    void graphCommitAborted() {
        opRecorder.removeMarker(COMMIT_MARKER);

        if (lifecycleEventsEnabled) {
            send(null, ObjectContext.GRAPH_COMMIT_ABORTED_SUBJECT);
        }
    }

    void graphCommitStarted() {
        if (lifecycleEventsEnabled) {

            GraphDiff diff = opRecorder.getDiffs();
            opRecorder.setMarker(COMMIT_MARKER);

            // include all diffs up to this point
            send(diff, ObjectContext.GRAPH_COMMIT_STARTED_SUBJECT);
        }
        else {
            opRecorder.setMarker(COMMIT_MARKER);
        }
    }

    void graphCommitted(GraphDiff parentSyncDiff) {
        processParentSync(parentSyncDiff);

        if (lifecycleEventsEnabled) {
            GraphDiff diff = opRecorder.getDiffsAfterMarker(COMMIT_MARKER);

            stateRecorder.graphCommitted();
            opRecorder.reset();
            reset();

            // include all diffs after the commit start marker.
            send(diff, ObjectContext.GRAPH_COMMITTED_SUBJECT);
        }
        else {
            stateRecorder.graphCommitted();
            opRecorder.reset();
        }
    }

    void graphFlushed() {
        opRecorder.setMarker(FLUSH_MARKER);
    }

    void graphReverted() {
        GraphDiff diff = opRecorder.getDiffs();

        diff.undo(new NullChangeHandler());
        stateRecorder.graphReverted();
        opRecorder.reset();
        reset();

        if (lifecycleEventsEnabled) {
            send(diff, ObjectContext.GRAPH_ROLLEDBACK_SUBJECT);
        }
    }

    // ****** GraphChangeHandler API ******
    // =====================================================

    public void nodeIdChanged(Object nodeId, Object newId) {
        stateRecorder.nodeIdChanged(nodeId, newId);
        processChange(new NodeIdChangeOperation(nodeId, newId));
    }

    public void nodeCreated(Object nodeId) {
        stateRecorder.nodeCreated(nodeId);
        processChange(new NodeCreateOperation(nodeId));
    }

    public void nodeRemoved(Object nodeId) {
        stateRecorder.nodeRemoved(nodeId);
        processChange(new NodeDeleteOperation(nodeId));
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        stateRecorder.nodePropertyChanged(nodeId, property, oldValue, newValue);
        processChange(new NodePropertyChangeOperation(
                nodeId,
                property,
                oldValue,
                newValue));
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        stateRecorder.arcCreated(nodeId, targetNodeId, arcId);
        processChange(new ArcCreateOperation(nodeId, targetNodeId, arcId));
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        stateRecorder.arcDeleted(nodeId, targetNodeId, arcId);
        processChange(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
    }

    // ****** helper methods ******
    // =====================================================

    private void processChange(GraphDiff diff) {
        opRecorder.addOperation(diff);

        if (changeEventsEnabled) {
            send(diff, ObjectContext.GRAPH_CHANGED_SUBJECT);
        }
    }

    /**
     * Wraps GraphDiff in a GraphEvent and sends it via EventManager with specified
     * subject.
     */
    private void send(GraphDiff diff, EventSubject subject) {
        EventManager manager = (context.getChannel() != null) ? context
                .getChannel()
                .getEventManager() : null;

        if (manager != null) {
            GraphEvent e = new GraphEvent(context, diff);
            manager.postEvent(e, subject);
        }
    }

    private void processParentSync(GraphDiff parentSyncDiff) {
        if (parentSyncDiff != null) {
            parentSyncDiff.apply(new ParentSyncHandler());
        }
    }

    private void reset() {
        if (deadIds != null) {

            // unregister dead ids...
            Iterator it = deadIds.iterator();
            while (it.hasNext()) {
                nodes.remove(it.next());
            }

            deadIds = null;
        }
    }

    class NullChangeHandler implements GraphChangeHandler {

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void nodeCreated(Object nodeId) {
        }

        public void nodeIdChanged(Object nodeId, Object newId) {
        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {
        }

        public void nodeRemoved(Object nodeId) {
        }

        public void graphCommitAborted() {
        }

        public void graphCommitStarted() {
        }

        public void graphCommitted() {
        }

        public void graphRolledback() {
        }
    }

    class ParentSyncHandler implements GraphChangeHandler {

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void nodeCreated(Object nodeId) {
        }

        public void nodeIdChanged(Object nodeId, Object newId) {

            // do not unregister the node just yet... only put replaced id in deadIds to
            // remove it later. Otherwise stored operations will not work
            Object node = getNode(nodeId);

            if (node != null) {
                if (deadIds == null) {
                    deadIds = new ArrayList();
                }

                deadIds.add(nodeId);

                registerNode(newId, node);

                if (node instanceof Persistent) {
                    // inject new id
                    ((Persistent) node).setGlobalID((GlobalID) newId);
                }
            }
        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {
        }

        public void nodeRemoved(Object nodeId) {
        }

        public void graphCommitAborted() {
        }

        public void graphCommitStarted() {
        }

        public void graphCommitted() {
        }

        public void graphRolledback() {
        }
    }
}
