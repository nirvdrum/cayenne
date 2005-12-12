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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.opp.BootstrapMessage;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.ObjectSelectMessage;
import org.objectstyle.cayenne.opp.QueryMessage;
import org.objectstyle.cayenne.opp.SyncMessage;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;

/**
 * An OPPChannel adapter that connects client ObjectContext children to a server
 * ObjectContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientServerChannel implements OPPChannel {

    protected ObjectDataContext serverContext;
    protected boolean lifecycleEventsEnabled;

    public ClientServerChannel(DataDomain domain) {
        this(domain, false);
    }

    public ClientServerChannel(DataDomain domain, boolean lifecycleEventsEnabled) {
        this(new ObjectDataContext(domain), lifecycleEventsEnabled);
    }

    ClientServerChannel(ObjectDataContext serverContext, boolean lifecycleEventsEnabled) {

        this.serverContext = serverContext;
        this.lifecycleEventsEnabled = lifecycleEventsEnabled;
    }

    public EventManager getEventManager() {
        return serverContext != null
                ? serverContext.getObjectStore().getEventManager()
                : null;
    }

    public boolean isLifecycleEventsEnabled() {
        return lifecycleEventsEnabled;
    }

    public void setLifecycleEventsEnabled(boolean lifecycleEventsEnabled) {
        this.lifecycleEventsEnabled = lifecycleEventsEnabled;
    }

    public GraphDiff onSync(SyncMessage message) {

        // sync client changes
        switch (message.getType()) {
            case SyncMessage.ROLLBACK_TYPE:
                return onRollback(message.getSenderChanges());
            case SyncMessage.FLUSH_TYPE:
                return onFlush(message.getSenderChanges());
            case SyncMessage.COMMIT_TYPE:
                return onCommit(message.getSenderChanges());
            default:
                throw new CayenneRuntimeException("Unrecognized SyncMessage type: "
                        + message.getType());
        }
    }

    GraphDiff onRollback(GraphDiff childDiff) {

        if (serverContext.hasChanges()) {
            serverContext.rollbackChanges();

            if (lifecycleEventsEnabled) {
                EventManager eventManager = getEventManager();
                if (eventManager != null) {
                    eventManager.postEvent(
                            new GraphEvent(this, null),
                            OPPChannel.GRAPH_ROLLEDBACK_SUBJECT);
                }
            }
        }

        return null;
    }

    /**
     * Applies child diff, without returning anything back.
     */
    GraphDiff onFlush(GraphDiff childDiff) {
        childDiff.apply(new ClientToServerDiffConverter(serverContext));

        if (lifecycleEventsEnabled) {
            EventManager eventManager = getEventManager();

            if (eventManager != null) {
                eventManager.postEvent(
                        new GraphEvent(this, childDiff),
                        OPPChannel.GRAPH_CHANGED_SUBJECT);
            }
        }

        return null;
    }

    /**
     * Applies child diff, and then commits.
     */
    GraphDiff onCommit(GraphDiff childDiff) {
        childDiff.apply(new ClientToServerDiffConverter(serverContext));
        GraphDiff diff = serverContext.doCommitChanges();

        GraphDiff returnClientDiff;

        if (diff.isNoop()) {
            returnClientDiff = diff;
        }
        else {
            // create client diff
            ServerToClientDiffConverter clientConverter = new ServerToClientDiffConverter(
                    serverContext.getEntityResolver());
            diff.apply(clientConverter);
            returnClientDiff = clientConverter.getClientDiff();
        }

        if (lifecycleEventsEnabled) {
            EventManager eventManager = getEventManager();

            if (eventManager != null) {
                CompoundDiff notification = new CompoundDiff();
                notification.add(childDiff);
                notification.add(returnClientDiff);

                eventManager.postEvent(
                        new GraphEvent(this, notification),
                        OPPChannel.GRAPH_COMMITTED_SUBJECT);
            }
        }

        return returnClientDiff;
    }

    public QueryResponse onQuery(QueryMessage message) {
        return serverContext.performGenericQuery(rewriteQuery(message.getQuery()));
    }

    public List onSelectObjects(ObjectSelectMessage message) {

        List objects = serverContext.performQuery(rewriteQuery(message.getQuery()));

        // create client objects for a list of server object

        if (objects.isEmpty()) {
            return new ArrayList(0);
        }

        // detect prefetches...
        PrefetchTreeNode prefetchTree = null;
        if (message.getQuery() instanceof GenericSelectQuery) {
            prefetchTree = ((GenericSelectQuery) message.getQuery()).getPrefetchTree();
        }

        try {
            return new ServerToClientObjectConverter(objects, serverContext
                    .getEntityResolver(), prefetchTree).getConverted();
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error converting to client objects: "
                    + e.getLocalizedMessage(), e);
        }
    }

    public EntityResolver onBootstrap(BootstrapMessage message) {
        return serverContext.getEntityResolver().getClientEntityResolver();
    }

    /**
     * Performs any needed query preprocessing to be able to execute it on the server.
     * Note that this method may modify the client query. Normally Cayenne doesn't do
     * that, but it is acceptable in this case as deserialized instance of the query is
     * not accessible by a user anywhere else.
     */
    Query rewriteQuery(Query clientQuery) {

        // replace client class with server class
        EntityResolver clientResolver = serverContext
                .getEntityResolver()
                .getClientEntityResolver();
        Object root = clientQuery.getRoot(clientResolver);
        if (root instanceof Class) {
            ObjEntity entity = clientResolver.lookupObjEntity((Class) root);
            if (entity == null) {
                throw new CayenneRuntimeException("Unmapped client class: " + root);
            }
            clientQuery.setRoot(entity.getName());
        }

        return clientQuery;
    }
}