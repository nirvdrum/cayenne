/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.event.SnapshotEventListener;
import org.objectstyle.cayenne.access.util.QueryUtils;

/**
 * ObjectStore maintains a cache of objects and their snapshots.
 * 
 * @author Andrei Adamchik
 */
public class ObjectStore implements Serializable, SnapshotEventListener {
    private static Logger logObj = Logger.getLogger(ObjectStore.class);

    protected transient Map objectMap = new HashMap();
    protected transient Map newObjectMap = null;
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
     * Invalidates a collection of DataObjects. Changes objects
     * state to HOLLOW.
     */
    public synchronized void objectsInvalidated(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // we don't care about NEW objects, 
            // but we still do care about HOLLOW, since snapshot might still be present
            if (object.getPersistenceState() == PersistenceState.NEW) {
                continue;
            }

            object.setPersistenceState(PersistenceState.HOLLOW);

            // remove snapshot, but keep the object
            removeSnapshot(object.getObjectId());
        }
    }

    /**
     * Evicts a collection of DataObjects from this ObjectStore. Changes objects
     * state to TRANSIENT.
     */
    public synchronized void objectsUnregistered(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // remove object and snapshot (for now)
            removeObject(object.getObjectId());

            object.setDataContext(null);
            object.setObjectId(null);
            object.setPersistenceState(PersistenceState.TRANSIENT);
        }
    }

    /**
     * Updates underlying SnapshotCache. If <code>refresh</code> is true,
     * all snapshots in <code>snapshots</code> will be loaded into SnapshotCache,
     * regardless of the existing cache state. If <code>refresh</code> is false,
     * only missing snapshots are loaded. This method is normally called by Cayenne
     * internally to synchronized snapshots of recently fetched objects.
     * 
     * @param objects a list of object whose snapshots need to be updated in the SnapshotCache
     * @param snapshots a list of snapshots. Must be the same size and use the same order as 
     * <code>objects</code> list.
     * @param refresh controls whether existing cached snapshots should be replaced with
     * the new ones.
     */
    public synchronized void snapshotsUpdatedForObjects(
        List objects,
        List snapshots,
        boolean refresh) {

        // sanity check
        if (objects.size() != snapshots.size()) {
            throw new IllegalArgumentException(
                "Counts of objects and corresponding snapshots do not match. "
                    + "Objects count: "
                    + objects.size()
                    + ", snapshots count: "
                    + snapshots.size());
        }

        int size = objects.size();
        for (int i = 0; i < size; i++) {
            DataObject object = (DataObject) objects.get(i);

            // add snapshots if refresh is forced, or if a snapshot is missing
            if (refresh || getSnapshot(object.getObjectId()) == null) {
                addSnapshot(object.getObjectId(), (Map) snapshots.get(i));
            }
        }
    }

    /**
     * Processes internal objects after the parent DataContext was committed.
     * Changes object persistence state and handles snapshot updates.
     */
    public synchronized void objectsCommitted() {
        Iterator objects = this.getObjectIterator();
        List modifiedIds = null;

        while (objects.hasNext()) {
            DataObject object = (DataObject) objects.next();
            int state = object.getPersistenceState();
            ObjectId id = object.getObjectId();

            if (id.getReplacementId() != null) {
                if (modifiedIds == null) {
                    modifiedIds = new ArrayList();
                }

                modifiedIds.add(id);

                // postpone processing of objects that require an id change
                continue;
            }

            // inserted will all have replacement ids, so do not check for inserts here
            // ...

            // deleted
            if (state == PersistenceState.DELETED) {
				objects.remove();
				removeSnapshot(id);
                object.setDataContext(null);
                object.setPersistenceState(PersistenceState.TRANSIENT);
            }
            // modified
            else if (state == PersistenceState.MODIFIED) {
                addSnapshot(id, object.getDataContext().takeObjectSnapshot(object));
                object.setPersistenceState(PersistenceState.COMMITTED);
            }
        }

        // process id replacements
        if (modifiedIds != null) {
            Iterator ids = modifiedIds.iterator();
            while (ids.hasNext()) {
                ObjectId id = (ObjectId) ids.next();
                DataObject object = getObject(id);

                if (object == null) {
                    throw new CayenneRuntimeException("No object for id: " + id);
                }

                object.setObjectId(id.getReplacementId());
                object.setPersistenceState(PersistenceState.COMMITTED);
                addObject(object);
                addSnapshot(
                    id.getReplacementId(),
                    object.getDataContext().takeObjectSnapshot(object));
                removeObject(id);
            }
        }
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

        if (newObjectMap != null) {
            newObjectMap.put(obj.getObjectId(), obj);
        }
    }

    /**
     * Start tracking the registration of new objects.
     * from this objectStore. Used in conjunction
     * with unregisterNewObjects() to control garbage
     * collection when an instance of ObjectStore
     * is used over a longer time for batch processing.
     * (TODO: this won't work with changeObjectKey()?)
     *
     * @see org.objectstyle.cayenne.access.ObjectStore#unregisterNewObjects()
     */
    public synchronized void startTrackingNewObjects() {
        // TODO: something like shared DataContext or nested DataContext
        // would hopefully obsolete this feature...
        newObjectMap = new HashMap();
    }

    /**
     * Unregisters the newly registered DataObjects
     * from this objectStore. Used in conjunction
     * with startTrackingNewObjects() to control garbage
     * collection when an instance of ObjectStore
     * is used over a longer time for batch processing.
     * (TODO: this won't work with changeObjectKey()?)
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#startTrackingNewObjects()
     */
    public synchronized void unregisterNewObjects() {
        // TODO: something like shared DataContext or nested DataContext
        // would hopefully obsolete this feature...

        Iterator it = newObjectMap.values().iterator();

        while (it.hasNext()) {
            DataObject dataObj = (DataObject) it.next();

            ObjectId oid = dataObj.getObjectId();
            removeObject(oid);
            removeSnapshot(oid);

            dataObj.setDataContext(null);
            dataObj.setObjectId(null);
            dataObj.setPersistenceState(PersistenceState.TRANSIENT);
        }
        newObjectMap.clear();
        newObjectMap = null;
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
     */
    public synchronized Iterator getObjectIterator() {
        return objectMap.values().iterator();
    }

    /**
     * Returns <code>true</code> if there are any modified,
     * deleted or new objects registered with this ObjectStore,
     * <code>false</code> otherwise.
     */
    public synchronized boolean hasChanges() {

        // TODO: This implementation is rather naive and would scan all registered
        // objects. Any better ideas? Catching events or something....

        Iterator it = getObjectIterator();
        while (it.hasNext()) {
            DataObject dobj = (DataObject) it.next();
            int state = dobj.getPersistenceState();
            if (state == PersistenceState.MODIFIED) {
                if (QueryUtils.updatedProperties(dobj) != null) {
                    return true; //There were some updated properties
                } //no updated properties, continue and see if any other objects have changed
            }
            else if (
                state == PersistenceState.NEW || state == PersistenceState.DELETED) {
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

    /**
     * Processes snapshot change event, updating DataObjects whose
     * snapshots have changed.
     */
    public void snapshotsChanged(SnapshotEvent event) {
        // ignore event if this ObjectStore was the originator
        if (event.getRootSource() == this) {
            logObj.debug("Ignoring snapshot event sent by us: " + event);
            return;
        }

        logObj.debug("Processing snapshot event: " + event);
    }
}