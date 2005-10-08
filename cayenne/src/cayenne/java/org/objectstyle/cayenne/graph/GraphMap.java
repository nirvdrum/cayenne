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
 * An implementation of GraphManager that stores graph nodes keyed by their ids.
 * <h3>Tracking Object Changes</h3>
 * <p>
 * Registered objects may choose to notify GraphMap of their changes by using callback
 * methods defined in GraphChangeHandler interface. GraphMap itself does not send
 * GraphEvents, instead it directly notifies registered change handlers. An example of an
 * event mechanism built on top of GraphMap is OperationRecorder - it broadcasts events
 * via EventManager if configured to do so.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class GraphMap implements GraphManager {

    protected Map nodes;
    protected Collection changeHandlers;

    /**
     * Creates a new GraphMap.
     */
    public GraphMap() {
        this.nodes = new HashMap();
    }

    public synchronized void addChangeHandler(GraphChangeHandler handler) {
        if (handler != null) {
            if (changeHandlers == null) {
                changeHandlers = new ArrayList();
            }

            changeHandlers.add(handler);
        }
    }

    public synchronized void removeChangeHandler(GraphChangeHandler handler) {
        if (handler != null && changeHandlers != null) {
            changeHandlers.remove(handler);

            if (changeHandlers.isEmpty()) {
                changeHandlers = null;
            }
        }
    }

    /**
     * Returns a collection of registered change handlers.
     */
    protected Collection getChangeHandlers() {
        return changeHandlers;
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
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).graphCommitted();
            }
        }
    }

    public void graphRolledback() {
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).graphRolledback();
            }
        }
    }

    public synchronized void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).arcCreated(nodeId, targetNodeId, arcId);
            }
        }
    }

    public synchronized void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).arcDeleted(nodeId, targetNodeId, arcId);
            }
        }
    }

    public synchronized void nodeCreated(Object nodeId) {
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).nodeCreated(nodeId);
            }
        }
    }

    public synchronized void nodeRemoved(Object nodeId) {
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
            while (it.hasNext()) {
                ((GraphChangeHandler) it.next()).nodeRemoved(nodeId);
            }
        }
    }

    public synchronized void nodeIdChanged(Object nodeId, Object newId) {
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
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
        if (changeHandlers != null) {
            Iterator it = changeHandlers.iterator();
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
