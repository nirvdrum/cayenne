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
package org.objectstyle.cayenne.client;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.objectstyle.cayenne.GlobalID;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.graph.OperationRecorder;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.opp.BootstrapMessage;
import org.objectstyle.cayenne.opp.CommitMessage;
import org.objectstyle.cayenne.opp.GenericQueryMessage;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.SelectMessage;
import org.objectstyle.cayenne.opp.UpdateMessage;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.QueryExecutionPlan;
import org.objectstyle.cayenne.query.SingleObjectQuery;

/**
 * An client tier ObjectContext implementation in a 3+ tier Cayenne application. Instead
 * of using regular Cayenne stack for database updates ClientObjectContext uses a
 * connector object to communicate with server-side peer.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientObjectContext implements ObjectContext {

    // if we are to pass ClientObjectContext around, channel should be left alone and
    // reinjected later if needed
    protected transient OPPChannel channel;

    protected ClientEntityResolver entityResolver;
    protected GraphManager graphManager;
    protected OperationRecorder changeRecorder;
    protected ClientStateRecorder stateRecorder;

    // note that it is important to reuse the same action within the property change
    // thread to avoid a loop of "propertyChange" calls on handling reverse relationships.
    // Here we go further and make action a thread-safe ivar that tracks its own thread
    // state.
    ClientObjectContextGraphAction graphAction;

    /**
     * Creates a new ClientObjectContext. Note that it is not fully functional until its
     * "connector" property is set.
     */
    public ClientObjectContext() {
        this(null);
    }

    /**
     * Creates a new ClientObjectContext, initializaing it with a connector instance that
     * should be used to connect to a remote Cayenne service.
     */
    public ClientObjectContext(OPPChannel channel) {
        this.channel = channel;

        // assemble objects that track graph changes
        this.graphManager = new GraphManager();

        this.changeRecorder = new OperationRecorder();
        this.stateRecorder = new ClientStateRecorder(graphManager);

        graphManager.addLocalChangeHandler(changeRecorder);
        graphManager.addLocalChangeHandler(stateRecorder);
        graphManager.setRemoteChangeHandler(new ClientObjectContextMergeHandler(
                stateRecorder,
                graphManager));

        graphAction = new ClientObjectContextGraphAction(this);
    }

    public OPPChannel getChannel() {
        return channel;
    }

    public void setChannel(OPPChannel channel) {
        this.channel = channel;
    }

    /**
     * Returns a ClientEntityResolver that provides limited mapping information needed for
     * ClientObjectContext operation. If ClientEntityResolver is not set, this method
     * would obtain one from the server on demand by sending BootstrapMessage.
     */
    public ClientEntityResolver getEntityResolver() {
        // load entity resolver on demand
        if (entityResolver == null) {
            synchronized (this) {
                if (entityResolver == null) {
                    setEntityResolver(channel.onBootstrap(new BootstrapMessage()));
                }
            }
        }

        return entityResolver;
    }

    public void setEntityResolver(ClientEntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }

    public void setGraphManager(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    /**
     * Commits changes to uncommitted objects. First checks if there are changes in this
     * context and if any changes are detected, sends a commit message to remote Cayenne
     * service via an internal instance of CayenneConnector.
     */
    public GraphDiff commit() {

        if (!changeRecorder.isEmpty()) {
            GraphDiff commitDiff = channel.onCommit(new CommitMessage(changeRecorder
                    .getDiffs()));

            graphManager.mergeRemoteChange(commitDiff);
            graphManager.graphCommitted();

            return commitDiff;
        }
        else {
            return new CompoundDiff();
        }
    }

    public void rollback() {
        if (!changeRecorder.isEmpty()) {
            changeRecorder.getDiffs().undo(new NullChangeHandler());
            graphManager.graphRolledback();
        }
    }

    /**
     * Deletes an object locally, scheduling it for future deletion from the external data
     * store.
     */
    public void deleteObject(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.TRANSIENT) {
            return;
        }

        if (object.getPersistenceState() == PersistenceState.DELETED) {
            return;
        }

        if (object.getPersistenceState() == PersistenceState.NEW) {
            // kick it out of context
            object.setPersistenceState(PersistenceState.TRANSIENT);
            graphManager.unregisterNode(object.getGlobalID());
            return;
        }

        // TODO: no delete rules (yet)

        object.setPersistenceState(PersistenceState.DELETED);
        graphManager.nodeRemoved(object.getGlobalID());
    }

    /**
     * Creates and registers a new Persistent object instance.
     */
    public Persistent newObject(Class persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Persistent class can't be null.");
        }

        ObjEntity entity = getEntityResolver().entityForClass(persistentClass);
        ClassDescriptor descriptor = entity.getClassDescriptor();

        Persistent object = (Persistent) descriptor.createObject();
        descriptor.prepareForAccess(object);

        object.setGlobalID(new GlobalID(entity.getName()));
        object.setPersistenceState(PersistenceState.NEW);
        object.setObjectContext(this);

        graphManager.registerNode(object.getGlobalID(), object);
        graphManager.nodeCreated(object.getGlobalID());

        return object;
    }

    public int[] performUpdateQuery(QueryExecutionPlan query) {
        return channel.onUpdateQuery(new UpdateMessage(query));
    }

    // TODO: maybe change the api to be "performSelectQuery(Class, QueryExecutionPlan)"?
    public List performSelectQuery(QueryExecutionPlan query) {
        List objects = channel.onSelectQuery(new SelectMessage(query));
        if (objects.isEmpty()) {
            return objects;
        }

        // postprocess fetched objects...

        ListIterator it = objects.listIterator();
        while (it.hasNext()) {

            Persistent fetchedObject = (Persistent) it.next();

            // sanity check
            if (fetchedObject.getGlobalID() == null) {
                throw new CayenneClientException(
                        "Server returned an object without an id: " + fetchedObject);
            }

            Persistent cachedObject = (Persistent) graphManager.getNode(fetchedObject
                    .getGlobalID());

            if (cachedObject != null) {

                // TODO: implement smart merge for modified objects...
                if (cachedObject.getPersistenceState() != PersistenceState.MODIFIED) {

                    // refresh existing object...

                    // lookup descriptor on the spot - we can be dealing with a mix of
                    // different objects in the inheritance hierarchy...
                    ClassDescriptor descriptor = getClassDescriptor(cachedObject);

                    if (cachedObject.getPersistenceState() == PersistenceState.HOLLOW) {
                        cachedObject.setPersistenceState(PersistenceState.COMMITTED);
                        descriptor.prepareForAccess(cachedObject);
                    }

                    descriptor.copyProperties(fetchedObject, cachedObject);
                }

                it.set(cachedObject);
            }
            else {

                // lookup descriptor on the spot - we can deal with a mix of different
                // objects in the hierarchy...
                ClassDescriptor descriptor = getClassDescriptor(fetchedObject);

                fetchedObject.setPersistenceState(PersistenceState.COMMITTED);
                fetchedObject.setObjectContext(this);
                descriptor.prepareForAccess(fetchedObject);
                graphManager.registerNode(fetchedObject.getGlobalID(), fetchedObject);
            }
        }

        return objects;
    }

    public QueryResponse performGenericQuery(QueryExecutionPlan query) {
        return channel.onGenericQuery(new GenericQueryMessage(query));
    }

    public void prepareForAccess(Persistent object, String property) {
        ensureFaultResolved(object);
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

        graphAction.handlePropertyChange(object, property, oldValue, newValue);
    }

    public Collection uncommittedObjects() {
        // TODO: sync on graphManager?
        return stateRecorder.dirtyNodes();
    }

    public Collection deletedObjects() {
        // TODO: sync on graphManager?
        return stateRecorder.dirtyNodes(PersistenceState.DELETED);
    }

    public Collection modifiedObjects() {
        // TODO: sync on graphManager?
        return stateRecorder.dirtyNodes(PersistenceState.MODIFIED);
    }

    public Collection newObjects() {
        // TODO: sync on graphManager?
        return stateRecorder.dirtyNodes(PersistenceState.NEW);
    }

    /**
     * Checks if an object is HOLLOW and if so, resolves it.
     */
    protected void ensureFaultResolved(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.HOLLOW) {
            performSelectQuery(new SingleObjectQuery(object.getGlobalID()));
        }
    }

    protected ClassDescriptor getClassDescriptor(Persistent object) {
        ObjEntity entity = getEntityResolver().entityForName(
                object.getGlobalID().getEntityName());
        return entity.getClassDescriptor();
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

        public void graphCommitted() {
        }

        public void graphRolledback() {
        }
    }
}