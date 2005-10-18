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

import java.util.Collection;

import org.apache.commons.collections.Closure;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.graph.GraphEventListener;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.property.IndirectProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.util.IndexPropertyList;
import org.objectstyle.cayenne.util.Util;

/**
 * An object that performs "backdoor" modifications of the object graph. When doing an
 * update, ObjectContextMergeHandler blocks broadcasting of GraphManager events.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectContextMergeHandler implements GraphChangeHandler, GraphEventListener {

    CayenneContext context;
    boolean active;

    ObjectContextMergeHandler(CayenneContext context) {
        this.context = context;
        this.active = true;
    }

    // ******* GraphEventListener methods *******

    public void graphChanged(final GraphEvent e) {
        // process flush
        if (shouldProcessEvent(e) && e.getDiff() != null) {
            execute(new Closure() {

                public void execute(Object arg0) {
                    merge(e.getDiff());
                }
            });
        }
    }

    public void graphCommitted(final GraphEvent e) {
        // TODO (Andrus, 10/17/2005) - there are a few problems with commit processing:

        // 1. Event mechanism reliability:
        // - events may come out of order (commit and then preceeding flush)
        // - events may be missing all together (commit arrived, while prior flush did
        // not)
        // Possible solution - an "event_version_id" to be used for optimistic locking

        // 2. We don't know if our own dirty objects were committed or not...
        // For now we will simply merge the changes, and keep the context dirty

        if (shouldProcessEvent(e)) {
            final boolean hadChanges = context.internalGraphManager().hasChanges();

            execute(new Closure() {

                public void execute(Object arg0) {

                    if (e.getDiff() != null) {
                        merge(e.getDiff());
                    }

                    if (!hadChanges) {
                        context.internalGraphManager().stateLog.graphCommitted();
                        context.internalGraphManager().reset();
                    }
                }
            });
        }
    }

    public void graphRolledback(GraphEvent e) {

        if (shouldProcessEvent(e)) {

            // do we need to merge anything?
            if (context.internalGraphManager().hasChanges()) {
                execute(new Closure() {

                    public void execute(Object arg0) {
                        context.internalGraphManager().graphReverted();
                    }
                });
            }
        }
    }

    public void graphCommitAborted(GraphEvent e) {
        // noop
    }

    public void graphCommitStarted(GraphEvent e) {
        // noop
    }

    // ******* End GraphEventListener methods *******

    /**
     * Executes merging of the external diff.
     */
    void merge(final GraphDiff diff) {
        execute(new Closure() {

            public void execute(Object arg0) {
                diff.apply(ObjectContextMergeHandler.this);
            }
        });
    }

    // ******* GraphChangeHandler methods *********

    public void nodeIdChanged(Object nodeId, Object newId) {
        // do not unregister the node just yet... only put replaced id in deadIds to
        // remove it later. Otherwise stored operations will not work
        Object node = context.internalGraphManager().getNode(nodeId);

        if (node != null) {
            context.internalGraphManager().deadIds().add(nodeId);
            context.internalGraphManager().registerNode(newId, node);

            if (node instanceof Persistent) {
                // inject new id
                ((Persistent) node).setGlobalID((GlobalID) newId);
            }
        }
    }

    public void nodeCreated(Object nodeId) {
        context._newObject(entityForId(nodeId), (GlobalID) nodeId);
    }

    public void nodeRemoved(Object nodeId) {
        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {
            context.deleteObject((Persistent) object);
        }
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {

            // do not override local changes....
            Property p = propertyForId(nodeId, property);
            if (Util.nullSafeEquals(p.readValue(object), oldValue)) {

                context.internalGraphAction().handleSimplePropertyChange(
                        (Persistent) object,
                        property,
                        oldValue,
                        newValue);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        Object source = context.internalGraphManager().getNode(nodeId);
        if (source != null) {

            Object target = context.internalGraphManager().getNode(targetNodeId);
            if (target != null) {

                // TODO (Andrus, 10/17/2005) - to avoid overriding local changes (and
                // processing duplication) we need to pair up complimentary arc change
                // events; or maybe redesign graph notifications to batch arc changes in a
                // single atomic operation. E.g. an arcId can be denoted as a combination
                // of forward and reverse relationships, like "toArtist|paintingArray",
                // simplifying processing.

                IndirectProperty p = (IndirectProperty) propertyForId(nodeId, arcId
                        .toString());
                Object holder = p.readValueHolder(source);
                if (holder instanceof ValueHolder) {
                    ((ValueHolder) holder).setValue(p.getPropertyType(), target);
                }
                else if (holder instanceof Collection) {
                    ((Collection) holder).add(target);

                    // handle ordered lists...
                    // TODO (Andrus, 10/17/2005) - ordering info should be available
                    // from property descriptor to avoid concrete class cast
                    if (holder instanceof IndexPropertyList) {
                        ((IndexPropertyList) holder).touch();
                    }
                }
            }
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        Object source = context.internalGraphManager().getNode(nodeId);
        if (source != null) {

            Object target = context.internalGraphManager().getNode(targetNodeId);
            if (target != null) {

                // (see "TODO" in 'arcCreated')

                IndirectProperty p = (IndirectProperty) propertyForId(nodeId, arcId
                        .toString());
                Object holder = p.readValueHolder(source);
                if (holder instanceof ValueHolder) {
                    ((ValueHolder) holder).setValue(p.getPropertyType(), null);
                }
                else if (holder instanceof Collection) {
                    ((Collection) holder).remove(target);
                }
            }
        }
    }

    private Property propertyForId(Object nodeId, String propertyName) {
        return entityForId(nodeId).getClassDescriptor().getProperty(propertyName);
    }

    private ObjEntity entityForId(Object nodeId) {
        return context.getEntityResolver().lookupObjEntity(
                ((GlobalID) nodeId).getEntityName());
    }

    // Returns true if this object is active; an event came from our channel, but did not
    // originate in it.
    private boolean shouldProcessEvent(GraphEvent e) {
        // only process events that came from our channel, but did not originate in it
        return active
                && e.getSource() != context.getChannel()
                && e.getPostedBy() == context.getChannel();
    }

    // executes a closure, disabling ObjectContext events for the duration of the
    // execution.

    private void execute(Closure closure) {

        synchronized (context.internalGraphManager()) {
            boolean changeEventsEnabled = context.internalGraphManager().changeEventsEnabled;
            context.internalGraphManager().changeEventsEnabled = false;

            boolean lifecycleEventsEnabled = context.internalGraphManager().lifecycleEventsEnabled;
            context.internalGraphManager().lifecycleEventsEnabled = false;

            context.internalGraphAction().setArcChangeInProcess(true);

            try {
                closure.execute(null);
            }
            finally {
                context.internalGraphManager().changeEventsEnabled = changeEventsEnabled;
                context.internalGraphManager().lifecycleEventsEnabled = lifecycleEventsEnabled;
                context.internalGraphAction().setArcChangeInProcess(false);
            }
        }
    }
}
