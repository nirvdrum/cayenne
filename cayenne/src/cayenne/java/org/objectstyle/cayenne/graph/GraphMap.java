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
package org.objectstyle.cayenne.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An implementation of GraphManager that stores graph nodes keyed by their ids and serves
 * as a single event source for all graph changes (provided that graph nodes invoke
 * appropriate callback methods on their changes). Processing of events received via
 * GraphEventListener API and not originating in this GraphMap is delegated to
 * externalChangeHandler.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class GraphMap implements GraphManager, GraphEventListener {

    protected Map nodes;

    protected Collection localChangeHandlers;
    protected GraphChangeHandler externalChangeHandler;
    protected boolean recordingLocalChanges;

    /**
     * Creates a new GraphMap.
     */
    public GraphMap() {
        this.nodes = new HashMap();
        this.recordingLocalChanges = true;
    }

    /**
     * A listener method that delegates graph events that did not originate in this
     * GraphManager to an 'externalChangeHandler' object. Events sent by this GraphMap are
     * discarded. One common use of the listener is to synchronize changes in multiple
     * peer graphs.
     */
    public void graphChanged(GraphEvent event) {
        if (event.getSource() == this) {
            return;
        }

        if (externalChangeHandler == null) {
            return;
        }

        // temporarily block recording of local changes...
        setRecordingLocalChanges(false);

        try {
            event.getDiff().apply(externalChangeHandler);
        }
        finally {
            setRecordingLocalChanges(true);
        }
    }

    public boolean isRecordingLocalChanges() {
        return recordingLocalChanges;
    }

    public void setRecordingLocalChanges(boolean active) {
        this.recordingLocalChanges = active;
    }

    public synchronized void addLocalChangeHandler(GraphChangeHandler handler) {
        if (handler != null) {
            if (localChangeHandlers == null) {
                localChangeHandlers = new ArrayList();
            }

            localChangeHandlers.add(handler);
        }
    }

    public synchronized void removeLocalChangeHandler(GraphChangeHandler handler) {
        if (handler != null && localChangeHandlers != null) {
            localChangeHandlers.remove(handler);

            if (localChangeHandlers.isEmpty()) {
                localChangeHandlers = null;
            }
        }
    }

    /**
     * Returns an object that is delegated handling of GraphEvents received by GraphMap.
     */
    public GraphChangeHandler getExternalChangeHandler() {
        return externalChangeHandler;
    }

    /**
     * Sets an object that will be delegated handling of GraphEvents received by GraphMap.
     * If set to null, GraphEvents are discarded.
     */
    public void setExternalChangeHandler(GraphChangeHandler handler) {
        this.externalChangeHandler = handler;
    }

    protected Collection getLocalChangeHandlers() {
        return localChangeHandlers;
    }

    // *** GraphMap methods

    public synchronized Object getNode(Object nodeId) {
        return nodes.get(nodeId);
    }

    public synchronized void registerNode(Object nodeId, Object nodeObject) {
        nodes.put(nodeId, nodeObject);
    }

    public synchronized Object unregisterNode(Object nodeId) {
        return nodes.remove(nodeId);
    }

    // *** methods for tracking local changes declared in GraphChangeHandler interface

    public void graphCommitted() {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).graphCommitted();
            }
        }
    }

    public void graphRolledback() {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).graphRolledback();
            }
        }
    }

    public synchronized void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).arcCreated(nodeId, targetNodeId, arcId);
            }
        }
    }

    public synchronized void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).arcDeleted(nodeId, targetNodeId, arcId);
            }
        }
    }

    public synchronized void nodeCreated(Object nodeId) {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).nodeCreated(nodeId);
            }
        }
    }

    public synchronized void nodeRemoved(Object nodeId) {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).nodeRemoved(nodeId);
            }
        }
    }

    public synchronized void nodeIdChanged(Object nodeId, Object newId) {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).nodeIdChanged(nodeId, newId);
            }
        }
    }

    public synchronized void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        if (recordingLocalChanges && localChangeHandlers != null) {
            Iterator it = localChangeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).nodePropertyChanged(
                        nodeId,
                        property,
                        oldValue,
                        newValue);
            }
        }
    }
}
