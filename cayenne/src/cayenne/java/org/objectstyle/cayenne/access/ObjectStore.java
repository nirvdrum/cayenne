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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.event.SnapshotEventListener;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * ObjectStore maintains a cache of objects and their snapshots.
 * 
 * <p>
 * <strong>Synchronization Note:</strong> Since there is often a need to
 * synchronize on both, ObjectStore and underlying DataRowCache, there must be
 * a consistent synchronization policy to avoid deadlocks. Whenever ObjectStore
 * needs to obtain a lock on DataRowStore, it must obtain a lock on self.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class ObjectStore implements Serializable, SnapshotEventListener {
    private static Logger logObj = Logger.getLogger(ObjectStore.class);

    protected transient Map newObjectMap = null;

    protected Map objectMap = new HashMap();

    // TODO: we may implement more fine grained tracking of related objects
    // changes, requiring more sophisticated data structure to hold them
    protected List indirectlyModifiedIds = new ArrayList();

    /**
	 * Ensures access to the versions of DataObject snapshots (in the form of
	 * DataRows) taken when an object was first modified.
	 */
    protected Map retainedSnapshotMap = new HashMap();

    /**
	 * Stores a reference to the SnapshotCache.
	 * 
	 * <p>
	 * <i>Serialization note:</i> It is up to the owner of this ObjectStore
	 * to initialize snapshot cache after deserialization of this object.
	 * ObjectStore will not know how to restore the SnapshotCache by itself.
	 * </p>
	 */
    protected transient DataRowStore dataRowCache;

    public ObjectStore() {
    }

    public ObjectStore(DataRowStore dataRowCache) {
        this();
        setDataRowCache(dataRowCache);
    }

    /**
	 * Saves a committed snapshot for an object in a non-expiring cache. This
	 * ensures that Cayenne can track object changes even if the underlying
	 * cache entry has expired or replaced with a newer version. Retained
	 * snapshots are evicted when an object is committed or rolled back.
	 * 
	 * <p>
	 * When committing modified objects, comparing them with retained snapshots
	 * instead of the currently cached snapshots would allow to resolve certain
	 * conflicts during concurrent modification of <strong>different
	 * attributes</strong> of the same objects by different DataContexts.
	 * </p>
	 * 
	 * @since 1.1
	 */
    public synchronized void retainSnapshot(DataObject dataObject) {
        ObjectId oid = dataObject.getObjectId();
        DataRow snapshot = getCachedSnapshot(oid);

        // if cached snapshot is different or absent, use snapshot built from
        // object
        if (snapshot == null
            || snapshot.getVersion() != dataObject.getSnapshotVersion()) {
            snapshot = dataObject.getDataContext().currentSnapshot(dataObject);
        }

        retainSnapshot(dataObject, snapshot);
    }

    /**
	 * Stores provided DataRow as a snapshot to be used to build UPDATE queries
	 * for an object. Updates object's snapshot version with the version of the
	 * new retained snapshot.
	 * 
	 * @since 1.1
	 */
    public synchronized void retainSnapshot(DataObject object, DataRow snapshot) {
        this.retainedSnapshotMap.put(object.getObjectId(), snapshot);
        object.setSnapshotVersion(snapshot.getVersion());
    }

    /**
	 * Returns a SnapshotCache associated with this ObjectStore.
	 */
    public DataRowStore getDataRowCache() {
        return dataRowCache;
    }

    /**
	 * Sets parent SnapshotCache. Registers to receive SnapshotEvents if the
	 * cache is configured to allow ObjectStores to receive such events.
	 */
    public void setDataRowCache(DataRowStore dataRowCache) {
        if (dataRowCache == this.dataRowCache) {
            return;
        }

        // IMPORTANT: listen for all senders on a given EventSubject,
        // filtering of events will be done in the handler method.

        if (this.dataRowCache != null) {
            EventManager.getDefaultManager().removeListener(
                this,
                this.dataRowCache.getSnapshotEventSubject());
        }

        this.dataRowCache = dataRowCache;

        if (dataRowCache != null) {
            // setting itself as non-blocking listener,
            // since event sending thread will likely be locking sender's
            // ObjectStore and snapshot cache itself.
            EventManager.getDefaultManager().addNonBlockingListener(
                this,
                "snapshotsChanged",
                SnapshotEvent.class,
                dataRowCache.getSnapshotEventSubject());
        }
    }

    /**
	 * Invalidates a collection of DataObjects. Changes objects state to
	 * HOLLOW.
	 */
    public synchronized void objectsInvalidated(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Collection ids = new ArrayList(objects.size());
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // we don't care about NEW objects,
            // but we still do care about HOLLOW, since snapshot might still be
            // present
            if (object.getPersistenceState() == PersistenceState.NEW) {
                continue;
            }

            object.setPersistenceState(PersistenceState.HOLLOW);

            // remove snapshot, but keep the object
            removeSnapshot(object.getObjectId());
            
            // remove cached changes
            indirectlyModifiedIds.remove(object.getObjectId());

            // remember the id
            ids.add(object.getObjectId());
        }

        // send an event for removed snapshots
        getDataRowCache().processSnapshotChanges(
            this,
            Collections.EMPTY_MAP,
            ids,
            Collections.EMPTY_LIST);
    }

    /**
	 * Evicts a collection of DataObjects from the ObjectStore. Object
	 * snapshots are removed as well. Changes objects state to TRANSIENT. This
	 * method can be used for manual cleanup of Cayenne cache.
	 */
    public synchronized void objectsUnregistered(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // remove object but not snapshot
            objectMap.remove(object.getObjectId());
            indirectlyModifiedIds.remove(object.getObjectId());
            dataRowCache.forgetSnapshot(object.getObjectId());

            object.setDataContext(null);
            object.setObjectId(null);
            object.setPersistenceState(PersistenceState.TRANSIENT);
        }

        // no snapshot events needed... snapshots maybe cleared, but no
        // database changes have occured.
    }

    /**
	 * Performs tracking of object relationship changes.
	 * 
	 * @since 1.1
	 */
    public void objectRelationshipChanged(
        DataObject object,
        ObjRelationship relationship) {
        if (relationship.isSourceIndependentFromTargetChange()) {
            int state = object.getPersistenceState();
            if (state == PersistenceState.COMMITTED
                || state == PersistenceState.HOLLOW
                || state == PersistenceState.MODIFIED) {

                synchronized (this) {
                    indirectlyModifiedIds.add(object.getObjectId());
                }
            }
        }
    }

    /**
	 * Updates underlying DataRowStore. If <code>refresh</code> is true, all
	 * snapshots in <code>snapshots</code> will be loaded into DataRowStore,
	 * regardless of the existing cache state. If <code>refresh</code> is
	 * false, only missing snapshots are loaded. This method is normally called
	 * by Cayenne internally to synchronized snapshots of recently fetched
	 * objects.
	 * 
	 * @param objects
	 *            a list of object whose snapshots need to be updated in the
	 *            SnapshotCache
	 * @param snapshots
	 *            a list of snapshots. Must be the same size and use the same
	 *            order as <code>objects</code> list.
	 * @param refresh
	 *            controls whether existing cached snapshots should be replaced
	 *            with the new ones.
	 */
    public void snapshotsUpdatedForObjects(
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

        Map modified = null;

        synchronized (this) {
            int size = objects.size();
            for (int i = 0; i < size; i++) {
                DataObject object = (DataObject) objects.get(i);
                ObjectId oid = object.getObjectId();

                // add snapshots if refresh is forced, or if a snapshot is
                // missing
                DataRow cachedSnapshot = getCachedSnapshot(oid);
                if (refresh || cachedSnapshot == null) {
                    if (modified == null) {
                        modified = new HashMap();
                    }

                    DataRow newSnapshot = (DataRow) snapshots.get(i);
                    if (cachedSnapshot != null) {
                        newSnapshot.setReplacesVersion(cachedSnapshot.getVersion());
                    }

                    modified.put(oid, newSnapshot);
                }
            }

            if (modified != null) {
                getDataRowCache().processSnapshotChanges(
                    this,
                    modified,
                    Collections.EMPTY_LIST,
                    Collections.EMPTY_LIST);
            }
        }
    }

    /**
	 * Processes internal objects after the parent DataContext was committed.
	 * Changes object persistence state and handles snapshot updates.
	 * 
	 * @since 1.1
	 */
    public synchronized void objectsCommitted() {
        // these will store snapshot changes
        List deletedIds = null;
        Map modifiedSnapshots = null;

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

            // inserted will all have replacement ids, so do not check for
            // inserts here
            // ...

            // deleted
            if (state == PersistenceState.DELETED) {
                objects.remove();
                removeSnapshot(id);
                object.setDataContext(null);
                object.setPersistenceState(PersistenceState.TRANSIENT);

                if (deletedIds == null) {
                    deletedIds = new ArrayList();
                }

                deletedIds.add(id);
            }
            // modified
            else if (state == PersistenceState.MODIFIED) {
                if (modifiedSnapshots == null) {
                    modifiedSnapshots = new HashMap();
                }

                DataRow dataRow = object.getDataContext().currentSnapshot(object);

                modifiedSnapshots.put(id, dataRow);
                dataRow.setReplacesVersion(object.getSnapshotVersion());

                object.setPersistenceState(PersistenceState.COMMITTED);
                object.setSnapshotVersion(dataRow.getVersion());
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

                // store old snapshot as deleted,
                // even though the object was modified, not deleted
                // from the common logic standpoint..
                if (!id.isTemporary()) {
                    if (deletedIds == null) {
                        deletedIds = new ArrayList();
                    }

                    deletedIds.add(id);
                }

                // store the new snapshot
                if (modifiedSnapshots == null) {
                    modifiedSnapshots = new HashMap();
                }

                DataRow dataRow = object.getDataContext().currentSnapshot(object);
                modifiedSnapshots.put(id.getReplacementId(), dataRow);
                dataRow.setReplacesVersion(object.getSnapshotVersion());

                // fix object state
                object.setObjectId(id.getReplacementId());
                object.setSnapshotVersion(dataRow.getVersion());
                object.setPersistenceState(PersistenceState.COMMITTED);
                addObject(object);
                removeObject(id);
            }
        }

        // notify parent cache
        if (deletedIds != null || modifiedSnapshots != null) {
            getDataRowCache().processSnapshotChanges(
                this,
                modifiedSnapshots != null ? modifiedSnapshots : Collections.EMPTY_MAP,
                deletedIds != null ? deletedIds : Collections.EMPTY_LIST,
                !indirectlyModifiedIds.isEmpty()
                    ? new ArrayList(indirectlyModifiedIds)
                    : Collections.EMPTY_LIST);
        }

        // clear caches
        this.retainedSnapshotMap.clear();
        this.indirectlyModifiedIds.clear();
    }

    /**
	 * Reregisters an object using a new id as a key. Returns the object if it
	 * is found, or null if it is not registered in the object store.
	 * 
	 * @deprecated Since 1.1 all methods for snapshot manipulation via
	 *             ObjectStore are deprecated due to architecture changes.
	 */
    public synchronized DataObject changeObjectKey(ObjectId oldId, ObjectId newId) {
        DataObject object = (DataObject) objectMap.remove(oldId);
        if (object != null) {

            Map snapshot = getDataRowCache().getCachedSnapshot(oldId);
            objectMap.put(newId, object);

            if (snapshot != null) {
                getDataRowCache().processSnapshotChanges(
                    this,
                    Collections.singletonMap(newId, snapshot),
                    Collections.singletonList(oldId),
                    Collections.EMPTY_LIST);
            }
        }

        return object;
    }

    public synchronized void addObject(DataObject obj) {
        objectMap.put(obj.getObjectId(), obj);

        if (newObjectMap != null) {
            newObjectMap.put(obj.getObjectId(), obj);
        }
    }

    /**
	 * Starts tracking the registration of new objects from this ObjectStore.
	 * Used in conjunction with unregisterNewObjects() to control garbage
	 * collection when an instance of ObjectStore is used over a longer time
	 * for batch processing. (TODO: this won't work with changeObjectKey()?)
	 * 
	 * @see org.objectstyle.cayenne.access.ObjectStore#unregisterNewObjects()
	 */
    public synchronized void startTrackingNewObjects() {
        // TODO: something like shared DataContext or nested DataContext
        // would hopefully obsolete this feature...
        newObjectMap = new HashMap();
    }

    /**
	 * Unregisters the newly registered DataObjects from this objectStore. Used
	 * in conjunction with startTrackingNewObjects() to control garbage
	 * collection when an instance of ObjectStore is used over a longer time
	 * for batch processing. (TODO: this won't work with changeObjectKey()?)
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
            objectMap.remove(oid);
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

    /**
	 * @deprecated Since 1.1 all methods for snapshot manipulation via
	 *             ObjectStore are deprecated due to architecture changes.
	 */
    public void addSnapshot(ObjectId id, Map snapshot) {
        getDataRowCache().processSnapshotChanges(
            this,
            Collections.singletonMap(id, snapshot),
            Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);
    }

    /**
	 * @deprecated Since 1.1 getCachedSnapshot(ObjectId) or
	 *             getSnapshot(ObjectId,QueryEngine) must be used.
	 */
    public Map getSnapshot(ObjectId id) {
        return getCachedSnapshot(id);
    }

    public synchronized DataRow getRetainedSnapshot(ObjectId oid) {
        return (DataRow) retainedSnapshotMap.get(oid);
    }

    /**
	 * Returns a snapshot for ObjectId from the underlying snapshot cache. If
	 * cache contains no snapshot, a null is returned.
	 * 
	 * @since 1.1
	 */
    public DataRow getCachedSnapshot(ObjectId oid) {
        DataRow retained = getRetainedSnapshot(oid);
        return (retained != null) ? retained : getDataRowCache().getCachedSnapshot(oid);
    }

    /**
	 * Returns a snapshot for ObjectId from the underlying snapshot cache. If
	 * cache contains no snapshot, it will attempt fetching it using provided
	 * QueryEngine. If fetch attempt fails or inconsistent data is returned,
	 * underlying cache will throw a CayenneRuntimeException.
	 * 
	 * @since 1.1
	 */
    public synchronized DataRow getSnapshot(ObjectId oid, QueryEngine engine) {
        DataRow retained = getRetainedSnapshot(oid);
        return (retained != null) ? retained : getDataRowCache().getSnapshot(oid, engine);
    }

    /**
	 * @deprecated Since 1.1 all methods for snapshot manipulation via
	 *             ObjectStore are deprecated due to architecture changes.
	 */
    public synchronized void removeObject(ObjectId id) {
        if (id != null) {
            objectMap.remove(id);
            removeSnapshot(id);
        }
    }

    /**
	 * @deprecated Since 1.1 all methods for snapshot manipulation via
	 *             ObjectStore are deprecated due to architecture changes.
	 */
    public void removeSnapshot(ObjectId id) {
        dataRowCache.forgetSnapshot(id);
    }

    /**
	 * Returns a list of objects that are registered with this DataContext,
	 * regardless of their persistence state. List is returned by copy and can
	 * be modified by the caller.
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
	 * Returns <code>true</code> if there are any modified, deleted or new
	 * objects registered with this ObjectStore, <code>false</code>
	 * otherwise.
	 */
    public synchronized boolean hasChanges() {

        // TODO: This implementation is rather naive and would scan all
        // registered
        // objects. Any better ideas? Catching events or something....

        Iterator it = getObjectIterator();
        while (it.hasNext()) {
            DataObject dobj = (DataObject) it.next();
            int state = dobj.getPersistenceState();
            if (state == PersistenceState.MODIFIED) {
                if (QueryUtils.updatedProperties(dobj) != null) {
                    return true; //There were some updated properties
                } //no updated properties, continue and see if any other
                // objects have changed
            }
            else if (
                state == PersistenceState.NEW || state == PersistenceState.DELETED) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Return a subset of registered objects that are in a certian persistence
	 * state. Collection is returned by copy.
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
	 * SnapshotEventListener implementation that processes snapshot change
	 * event, updating DataObjects that have the changes.
	 * 
	 * <p>
	 * <i>Implementation note:</i> This method should not attempt to alter
	 * the underlying DataRowStore, since it is normally invoked *AFTER* the
	 * DataRowStore was modified as a result of some external interaction.
	 * </p>
	 * 
	 * @since 1.1
	 */
    public void snapshotsChanged(SnapshotEvent event) {
        // filter events that we should not process
        if (event.getPostedBy() != this.dataRowCache || event.getSource() == this) {
            return;
        }

        // merge objects with changes in event...
        if (logObj.isDebugEnabled()) {
            logObj.debug("Received: " + event);
        }

        synchronized (this) {
            processUpdatedSnapshots(event.modifiedDiffs());
            processDeletedIDs(event.deletedIds());
        }
    }

    /**
	 * Initializes object with data from cache or from the database, if this
	 * object is not fully resolved.
	 * 
	 * @since 1.1
	 */
    public void resolveHollow(DataObject object) {
        if (object.getPersistenceState() != PersistenceState.HOLLOW) {
            return;
        }

        // no way to resolve faults outside of DataContext.
        DataContext context = object.getDataContext();
        if (context == null) {
            object.setPersistenceState(PersistenceState.TRANSIENT);
            return;
        }

        synchronized (this) {
            DataRow snapshot = getSnapshot(object.getObjectId(), context);

            // handle deleted object
            if (snapshot == null) {
                processDeletedIDs(Collections.singletonList(object.getObjectId()));
            }
            else {
                ObjEntity entity = context.getEntityResolver().lookupObjEntity(object);
                DataRowUtils.refreshObjectWithSnapshot(entity, object, snapshot, true);

                if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                    object.setPersistenceState(PersistenceState.COMMITTED);
                }
            }
        }
    }

    /**
	 * @since 1.1
	 */
    void processDeletedIDs(Collection deletedIDs) {
        if (deletedIDs != null && !deletedIDs.isEmpty()) {
            Iterator it = deletedIDs.iterator();
            while (it.hasNext()) {
                ObjectId oid = (ObjectId) it.next();
                DataObject object = getObject(oid);

                if (object == null) {
                    continue;
                }

                DataContextDelegate delegate;

                // TODO: refactor "switch" to avoid code duplication

                switch (object.getPersistenceState()) {
                    case PersistenceState.COMMITTED :
                        // must remove object from to-many lists
                        cleanDeletedObjectFromCollections(object, deletedIDs);

                    case PersistenceState.HOLLOW :
                        // TODO: HOLLOW objects have a good chance to be
                        // present in
                        // ToMany lists, but we can't detect this here... Need
                        // a solution

                    case PersistenceState.DELETED :

                        // consult delegate if it exists
                        delegate = object.getDataContext().getDelegate();

                        if (delegate == null || delegate.shouldProcessDelete(object)) {
                            objectMap.remove(oid);
                            retainedSnapshotMap.remove(oid);

                            // setting DataContext to null will also set
                            // state to transient
                            object.setDataContext(null);

                            if (delegate != null) {
                                delegate.finishedProcessDelete(object);
                            }
                        }

                        break;

                    case PersistenceState.MODIFIED :

                        // consult delegate if it exists
                        delegate = object.getDataContext().getDelegate();
                        if (delegate == null || delegate.shouldProcessDelete(object)) {
                            object.setPersistenceState(PersistenceState.NEW);

                            if (delegate != null) {
                                delegate.finishedProcessDelete(object);
                            }
                        }

                        break;
                }
            }
        }
    }

    /**
	 * @since 1.1
	 */
    void cleanDeletedObjectFromCollections(DataObject object, Collection deletedIDs) {
        ObjEntity entity =
            object.getDataContext().getEntityResolver().lookupObjEntity(object);
        Iterator relationshipIterator = entity.getRelationships().iterator();
        while (relationshipIterator.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) relationshipIterator.next();

            // only deal with NULLIFY rules that have a reverse relationship
            // as to-many.. the rest of the cases should be handled elsewhere
            if (relationship.getDeleteRule() != DeleteRule.NULLIFY) {
                continue;
            }

            ObjRelationship inverseRelationship = relationship.getReverseRelationship();
            if (inverseRelationship == null || !inverseRelationship.isToMany()) {
                continue;
            }

            List relatedObjects = null;
            if (relationship.isToMany()) {
                List toMany = (List) object.readPropertyDirectly(relationship.getName());

                if (toMany.size() == 0) {
                    continue;
                }

                // Get a copy of the list so that deleting objects doesn't
                // result in concurrent modification exceptions
                relatedObjects = new ArrayList(toMany);
            }
            else {
                Object relatedObject =
                    object.readPropertyDirectly(relationship.getName());

                if (relatedObject == null) {
                    continue;
                }

                if (relatedObject instanceof Fault) {
                    continue;
                }

                if (relatedObject instanceof DataObject) {
                    int state = ((DataObject) relatedObject).getPersistenceState();
                    if (state == PersistenceState.TRANSIENT
                        || state == PersistenceState.HOLLOW) {
                        continue;
                    }
                }

                relatedObjects = Collections.singletonList(relatedObject);
            }

            Iterator related = relatedObjects.iterator();
            while (related.hasNext()) {
                DataObject relatedObject = (DataObject) related.next();
                if (deletedIDs.contains(relatedObject.getObjectId())) {
                    continue;
                }

                logObj.info(
                    "removing "
                        + object.getObjectId()
                        + " from "
                        + relatedObject.getObjectId());

                // make sure we do not modify object state
                int state = relatedObject.getPersistenceState();

                try {
                    relatedObject.removeToManyTarget(
                        inverseRelationship.getName(),
                        object,
                        false);
                }
                finally {
                    relatedObject.setPersistenceState(state);
                }
            }
        }
    }

    /**
	 * @since 1.1
	 */
    void processUpdatedSnapshots(Map diffs) {
        if (diffs != null && !diffs.isEmpty()) {
            Iterator oids = diffs.entrySet().iterator();

            while (oids.hasNext()) {
                Map.Entry entry = (Map.Entry) oids.next();

                ObjectId oid = (ObjectId) entry.getKey();
                DataObject object = getObject(oid);

                // no object, or HOLLOW object require no processing
                if (object == null
                    || object.getPersistenceState() == PersistenceState.HOLLOW) {
                    continue;
                }

                DataRow diff = (DataRow) entry.getValue();

                // we are lazy, just turn COMMITTED object into HOLLOW instead
                // of
                // actually updating it
                if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                    // consult delegate if it exists
                    DataContextDelegate delegate = object.getDataContext().getDelegate();
                    if (delegate == null || delegate.shouldMergeChanges(object, diff)) {
                        object.setPersistenceState(PersistenceState.HOLLOW);

                        if (delegate != null) {
                            delegate.finishedMergeChanges(object);
                        }
                    }
                    continue;
                }

                // merge modified and deleted
                if (object.getPersistenceState() == PersistenceState.DELETED
                    || object.getPersistenceState() == PersistenceState.MODIFIED) {

                    // consult delegate if it exists
                    DataContextDelegate delegate = object.getDataContext().getDelegate();
                    if (delegate == null || delegate.shouldMergeChanges(object, diff)) {
                        ObjEntity entity =
                            object.getDataContext().getEntityResolver().lookupObjEntity(
                                object);
                        DataRowUtils.forceMergeWithSnapshot(entity, object, diff);

                        if (delegate != null) {
                            delegate.finishedMergeChanges(object);
                        }
                    }
                }
            }
        }
    }

}