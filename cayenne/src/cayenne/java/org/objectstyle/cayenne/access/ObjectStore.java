/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.access;

import java.io.*;
import java.util.*;

import org.objectstyle.cayenne.*;

/**
 * ObjectStore maintains a cache of objects and their snapshots.
 * 
 * @author Andrei Adamchik
 */
public class ObjectStore implements Serializable {
    protected transient Map objectMap = new HashMap();
    protected Map snapshotMap = new HashMap();

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(objectMap);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        objectMap = (Map) in.readObject();
    }

    /**
     * Reregisters an object using a new id as a key.
     * Returns the object if it is found, or null if
     * it is not registered in the object store.
     */
    public synchronized DataObject changeObjectKey(ObjectId oldId, ObjectId newId) {
        DataObject obj = (DataObject) objectMap.remove(oldId);
        if (obj != null) {
            Object snapshot = snapshotMap.remove(obj.getObjectId());

            if (snapshot != null) {
                snapshotMap.put(newId, snapshot);
            }
            objectMap.put(newId, obj);
        }
        return obj;
    }

    public synchronized void addObject(DataObject obj) {
        objectMap.put(obj.getObjectId(), obj);
    }

    public synchronized DataObject getObject(ObjectId id) {
        return (DataObject) objectMap.get(id);
    }

    public synchronized void addSnapshot(ObjectId id, Map snapshot) {
        snapshotMap.put(id, snapshot);
    }

    public synchronized Map getSnapshot(ObjectId id) {
        return (Map) snapshotMap.get(id);
    }

    public synchronized void removeObject(ObjectId id) {
        if (id != null) {
            objectMap.remove(id);
            snapshotMap.remove(id);
        }
    }

    public synchronized void removeSnapshot(ObjectId id) {
        snapshotMap.remove(id);
    }

    /** 
     * Returns a list of objects that are registered
     * with this DataContext, regardless of their persistence state.
     * List is returned by copy and can be modified by the caller.
     */
    public synchronized List getObjects() {
        return new ArrayList(objectMap.values());
    }

    /**
     * Returns an iterator over the registered objects.
     * 
     * <p><i>Caller should synchronize on the instance of ObjectStore
     * while iterating over the list.</i></p>
     */
    public Iterator getObjectIterator() {
        return objectMap.values().iterator();
    }

    /**
     * Returns <code>true</code> if there are any modified,
     * deleted or new objects registered with this DataContext,
     * <code>false</code> otherwise.
     * 
     * <p><i>
     * This implementation is rather naive and would scan all registered
     * objects. A better implementation based on catching context events
     * may be needed.</i></p>
     */
    public synchronized boolean hasChanges() {
        Iterator it = objectMap.values().iterator();
        while (it.hasNext()) {
            DataObject dobj = (DataObject) it.next();
            int state = dobj.getPersistenceState();
            if (state == PersistenceState.NEW
                || state == PersistenceState.DELETED
                || state == PersistenceState.MODIFIED) {
                return true;
            }
        }
        return false;
    }

    /** 
     * Return a subset of registered objects that are in a 
     * certian persistence state. Collection is returned by
     * copy.
     */
    public synchronized List objectsInState(int state) {
        List filteredObjects = new ArrayList();

        Iterator it = objectMap.values().iterator();
        while (it.hasNext()) {
            DataObject nextObj = (DataObject) it.next();
            if (nextObj.getPersistenceState() == state)
                filteredObjects.add(nextObj);
        }

        return filteredObjects;
    }
}
