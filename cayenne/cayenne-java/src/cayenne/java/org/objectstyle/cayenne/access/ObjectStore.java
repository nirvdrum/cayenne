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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.event.SnapshotEventListener;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.NodeIdChangeOperation;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SingleObjectQuery;
import org.objectstyle.cayenne.validation.ValidationException;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * ObjectStore stores objects using their ObjectId as a key. It works as a dedicated
 * object cache for a DataContext. Users rarely need to access ObjectStore directly, as
 * DataContext serves as a facade, providing cover methods for most ObjectStore
 * operations.
 * 
 * @since 1.0
 * @author Andrus Adamchik
 */
// Synchronization Note: There is often a need to do double synchronize on an ObjectStore
// and an underlying DataRowCache. To avoid deadlocks, Cayenne consistently follows the
// policy of locking an ObjectStore first, and then locking DataRowStore. This pattern
// must be followed in any new related developments.
public class ObjectStore implements Serializable, SnapshotEventListener {

    private static Logger logObj = Logger.getLogger(ObjectStore.class);

    protected transient Map newObjectMap = null;

    protected Map objectMap = new HashMap();
    protected Map queryResultMap = new HashMap();

    // TODO: we may implement more fine grained tracking of related objects
    // changes, requiring more sophisticated data structure to hold them
    protected List indirectlyModifiedIds = new ArrayList();

    protected List flattenedInserts = new ArrayList();
    protected List flattenedDeletes = new ArrayList();

    /**
     * Ensures access to the versions of DataObject snapshots (in the form of DataRows)
     * taken when an object was first modified.
     */
    protected Map retainedSnapshotMap = new HashMap();

    /**
     * Stores a reference to the DataRowStore.
     * <p>
     * <i>Serialization note: </i> It is up to the owner of this ObjectStore to initialize
     * DataRowStore after deserialization of this object. ObjectStore will not know how to
     * restore the DataRowStore by itself.
     * </p>
     */
    protected transient DataRowStore dataRowCache;

    // a factory that can be set by DataContext to defer attaching of DataRowStore on
    // deserialization.
    transient Factory dataRowCacheFactory;

    /**
     * The DataContext that owns this ObjectStore.
     */
    protected DataContext context;

    public ObjectStore() {
    }

    public ObjectStore(DataRowStore dataRowCache) {
        this();
        setDataRowCache(dataRowCache);
    }

    /**
     * Returns a number of objects currently registered with this ObjectStore.
     * 
     * @since 1.2
     */
    public int registeredObjectsCount() {
        return objectMap.size();
    }

    /**
     * Returns a number of query results cached by this object store. Note that each
     * result is a list and can possibly contain a large number of entries.
     * 
     * @since 1.2
     */
    public int cachedQueriesCount() {
        return queryResultMap.size();
    }

    /**
     * Saves a committed snapshot for an object in a non-expiring cache. This ensures that
     * Cayenne can track object changes even if the underlying cache entry has expired or
     * replaced with a newer version. Retained snapshots are evicted when an object is
     * committed or rolled back.
     * <p>
     * When committing modified objects, comparing them with retained snapshots instead of
     * the currently cached snapshots would allow to resolve certain conflicts during
     * concurrent modification of <strong>different attributes </strong> of the same
     * objects by different DataContexts.
     * </p>
     * 
     * @since 1.1
     */
    public synchronized void retainSnapshot(DataObject object) {
        ObjectId oid = object.getObjectId();
        DataRow snapshot = getCachedSnapshot(oid);

        if (snapshot == null) {
            snapshot = object.getDataContext().currentSnapshot(object);
        }
        // if a snapshot has changed underneath, try a merge...
        else if (snapshot.getVersion() != object.getSnapshotVersion()) {
            DataContextDelegate delegate = object.getDataContext().nonNullDelegate();
            if (delegate.shouldMergeChanges(object, snapshot)) {
                ObjEntity entity = object
                        .getDataContext()
                        .getEntityResolver()
                        .lookupObjEntity(object);
                DataRowUtils.forceMergeWithSnapshot(entity, object, snapshot);
                object.setSnapshotVersion(snapshot.getVersion());
                delegate.finishedMergeChanges(object);
            }
        }

        retainSnapshot(object, snapshot);
    }

    /**
     * Stores provided DataRow as a snapshot to be used to build UPDATE queries for an
     * object. Updates object's snapshot version with the version of the new retained
     * snapshot.
     * 
     * @since 1.1
     */
    protected synchronized void retainSnapshot(DataObject object, DataRow snapshot) {
        this.retainedSnapshotMap.put(object.getObjectId(), snapshot);
    }

    /**
     * Returns a DataRowStore associated with this ObjectStore.
     */
    public DataRowStore getDataRowCache() {

        // perform deferred initialization...

        // Andrus, 11/7/2005 - potential problem with on-demand deferred initialization is
        // that deserialized context won't receive any events... which maybe ok, since it
        // didn't while it was stored in serialized form.
        if (dataRowCache == null && dataRowCacheFactory != null) {
            synchronized (this) {
                if (dataRowCache == null && dataRowCacheFactory != null) {
                    setDataRowCache((DataRowStore) dataRowCacheFactory.create());
                }
            }
        }

        return dataRowCache;
    }

    /**
     * Sets parent SnapshotCache. Registers to receive SnapshotEvents if the cache is
     * configured to allow ObjectStores to receive such events.
     */
    public void setDataRowCache(DataRowStore dataRowCache) {
        if (dataRowCache == this.dataRowCache) {
            return;
        }

        if (this.dataRowCache != null && dataRowCache.getEventManager() != null) {
            dataRowCache.getEventManager().removeListener(
                    this,
                    this.dataRowCache.getSnapshotEventSubject());
        }

        this.dataRowCache = dataRowCache;

        if (dataRowCache != null && dataRowCache.getEventManager() != null) {
            // setting itself as non-blocking listener,
            // since event sending thread will likely be locking sender's
            // ObjectStore and snapshot cache itself.
            dataRowCache.getEventManager().addNonBlockingListener(
                    this,
                    "snapshotsChanged",
                    SnapshotEvent.class,
                    dataRowCache.getSnapshotEventSubject(),
                    dataRowCache);
        }
    }

    /**
     * Invalidates a collection of DataObjects. Changes objects state to HOLLOW.
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

            // remove cached changes
            indirectlyModifiedIds.remove(object.getObjectId());

            // remember the id
            ids.add(object.getObjectId());
        }

        // send an event for removed snapshots
        getDataRowCache().processSnapshotChanges(
                this,
                Collections.EMPTY_MAP,
                Collections.EMPTY_LIST,
                ids,
                Collections.EMPTY_LIST);
    }

    /**
     * Evicts a collection of DataObjects from the ObjectStore. Object snapshots are
     * removed as well. Changes objects state to TRANSIENT. This method can be used for
     * manual cleanup of Cayenne cache.
     */
    public synchronized void objectsUnregistered(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        // dataRowCache maybe initialized lazily, so ensure the local instance is resolved
        DataRowStore dataRowCache = getDataRowCache();

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
     * Reverts changes to all stored uncomitted objects.
     * 
     * @since 1.1
     */
    public synchronized void objectsRolledBack() {
        Iterator it = getObjectIterator();

        // collect candidates
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            int objectState = object.getPersistenceState();
            switch (objectState) {
                case PersistenceState.NEW:
                    it.remove();

                    object.setDataContext(null);
                    object.setObjectId(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.DELETED:
                // Do the same as for modified... deleted is only a persistence state, so
                // rolling the object back will set the state to committed
                case PersistenceState.MODIFIED:
                    // this will clean any modifications and defer refresh from snapshot
                    // till the next object accessor is called
                    object.setPersistenceState(PersistenceState.HOLLOW);
                    break;
                default:
                    // Transient, committed and hollow need no handling
                    break;
            }
        }

        // clear caches
        // TODO: the same operation is performed on commit... must create a common method
        this.retainedSnapshotMap.clear();
        this.indirectlyModifiedIds.clear();
        this.flattenedDeletes.clear();
        this.flattenedInserts.clear();
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     */
    public void objectRelationshipUnset(
            DataObject source,
            DataObject target,
            ObjRelationship relationship,
            boolean processFlattened) {

        objectRelationshipChanged(source, relationship);

        if (processFlattened) {
            flattenedRelationshipUnset(source, relationship, target);
        }
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     */
    public void objectRelationshipSet(
            DataObject source,
            DataObject target,
            ObjRelationship relationship,
            boolean processFlattened) {

        objectRelationshipChanged(source, relationship);

        if (processFlattened) {
            flattenedRelationshipSet(source, relationship, target);
        }
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     */
    void objectRelationshipChanged(DataObject object, ObjRelationship relationship) {
        // track modifications to an "independent" relationship
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
     * Updates snapshots in the underlying DataRowStore. If <code>refresh</code> is
     * true, all snapshots in <code>snapshots</code> will be loaded into DataRowStore,
     * regardless of the existing cache state. If <code>refresh</code> is false, only
     * missing snapshots are loaded. This method is normally called internally by the
     * DataContext owning the ObjectStore to update the caches after a select query.
     * 
     * @param objects a list of object whose snapshots need to be updated.
     * @param snapshots a list of snapshots. Must be of the same length and use the same
     *            order as <code>objects</code> list.
     * @param refresh controls whether existing cached snapshots should be replaced with
     *            the new ones.
     * @since 1.1
     */
    public void snapshotsUpdatedForObjects(List objects, List snapshots, boolean refresh) {

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

                // skip HOLLOW objects as they likely were created from partial snapshots
                if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                    continue;
                }

                ObjectId oid = object.getObjectId();

                // add snapshots if refresh is forced, or if a snapshot is
                // missing
                DataRow cachedSnapshot = getCachedSnapshot(oid);
                if (refresh || cachedSnapshot == null) {

                    DataRow newSnapshot = (DataRow) snapshots.get(i);

                    if (cachedSnapshot != null) {
                        // use old snapshot if no changes occurred
                        if (cachedSnapshot.equals(newSnapshot)) {
                            object.setSnapshotVersion(cachedSnapshot.getVersion());
                            continue;
                        }
                        else {
                            newSnapshot.setReplacesVersion(cachedSnapshot.getVersion());
                        }
                    }

                    if (modified == null) {
                        modified = new HashMap();
                    }

                    modified.put(oid, newSnapshot);
                }
            }

            if (modified != null) {
                getDataRowCache().processSnapshotChanges(
                        this,
                        modified,
                        Collections.EMPTY_LIST,
                        Collections.EMPTY_LIST,
                        Collections.EMPTY_LIST);
            }
        }
    }

    /**
     * Processes internal objects after the parent DataContext was committed. Changes
     * object persistence state and handles snapshot updates.
     * 
     * @since 1.1
     * @deprecated since 1.2 unused.
     */
    public synchronized void objectsCommitted() {
        postprocessAfterCommit(new CompoundDiff());
    }

    /**
     * Internal unsynchronized method to processes objects state after commit was
     * performed.
     * 
     * @since 1.2
     */
    GraphDiff postprocessAfterCommit(GraphDiff parentChanges) {

        CompoundDiff diff = new CompoundDiff();
        Iterator entries = objectMap.entrySet().iterator();

        // have to scan through all entries
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            DataObject object = (DataObject) entry.getValue();
            ObjectId registeredId = (ObjectId) entry.getKey();
            ObjectId id = object.getObjectId();

            switch (object.getPersistenceState()) {
                case PersistenceState.DELETED:
                    entries.remove();
                    object.setObjectContext(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.NEW:
                case PersistenceState.MODIFIED:

                    // detect and handle manual id changes done via
                    // Persistent.setObjectId()
                    if (!id.equals(registeredId)) {
                        diff.add(new NodeIdChangeOperation(registeredId, id));
                    }

                    object.setPersistenceState(PersistenceState.COMMITTED);
                    break;
            }
        }

        if (parentChanges != null && !parentChanges.isNoop()) {
            diff.add(parentChanges);
        }

        // re-register changed object ids
        if (!diff.isNoop()) {
            diff.apply(new IdUpdater());
        }

        // clear caches
        this.retainedSnapshotMap.clear();
        this.indirectlyModifiedIds.clear();
        this.flattenedDeletes.clear();
        this.flattenedInserts.clear();

        return diff;
    }

    public synchronized void addObject(DataObject obj) {
        objectMap.put(obj.getObjectId(), obj);

        if (newObjectMap != null) {
            newObjectMap.put(obj.getObjectId(), obj);
        }
    }

    /**
     * Starts tracking the registration of new objects from this ObjectStore. Used in
     * conjunction with unregisterNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing. (TODO:
     * this won't work with changeObjectKey()?)
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#unregisterNewObjects()
     */
    public synchronized void startTrackingNewObjects() {
        // TODO: something like shared DataContext or nested DataContext
        // would hopefully obsolete this feature...
        newObjectMap = new HashMap();
    }

    /**
     * Unregisters the newly registered DataObjects from this objectStore. Used in
     * conjunction with startTrackingNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing. (TODO:
     * this won't work with changeObjectKey()?)
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#startTrackingNewObjects()
     */
    public synchronized void unregisterNewObjects() {
        // TODO: something like shared DataContext or nested DataContext
        // would hopefully obsolete this feature...

        if (newObjectMap == null) {
            return;
        }

        // dataRowCache maybe initialized lazily, so ensure the local instance is resolved
        DataRowStore dataRowCache = getDataRowCache();

        Iterator it = newObjectMap.values().iterator();

        while (it.hasNext()) {
            DataObject dataObj = (DataObject) it.next();

            ObjectId oid = dataObj.getObjectId();
            objectMap.remove(oid);
            dataRowCache.forgetSnapshot(oid);

            dataObj.setDataContext(null);
            dataObj.setObjectId(null);
            dataObj.setPersistenceState(PersistenceState.TRANSIENT);
        }
        newObjectMap.clear();
        newObjectMap = null;
    }

    /**
     * Returns a DataObject registered for a given ObjectId, or null if no such object
     * exists. This method does not do a database fetch.
     */
    public synchronized DataObject getObject(ObjectId id) {
        return (DataObject) objectMap.get(id);
    }

    public synchronized DataRow getRetainedSnapshot(ObjectId oid) {
        return (DataRow) retainedSnapshotMap.get(oid);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, a null is returned.
     * 
     * @since 1.1
     */
    public DataRow getCachedSnapshot(ObjectId oid) {
        DataRow retained = getRetainedSnapshot(oid);
        return (retained != null) ? retained : getDataRowCache().getCachedSnapshot(oid);
    }

    /**
     * Returns cached query results for a given query, or null if no results are cached.
     * Note that ObjectStore will only lookup results in its local cache, and not the
     * shared cache associated with the underlying DataRowStore.
     * 
     * @since 1.1
     */
    public synchronized List getCachedQueryResult(String name) {
        // results should have been stored as rows or objects when
        // they were originally cached... do no conversions here
        return (List) queryResultMap.get(name);
    }

    /**
     * Caches a list of query results.
     * 
     * @since 1.1
     */
    public synchronized void cacheQueryResult(String name, List results) {
        queryResultMap.put(name, results);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, it will attempt fetching it using provided QueryEngine. If
     * fetch attempt fails or inconsistent data is returned, underlying cache will throw a
     * CayenneRuntimeException.
     * 
     * @since 1.1
     * @deprecated since 1.2. Use {@link #getSnapshot(ObjectId)} instead.
     */
    public synchronized DataRow getSnapshot(ObjectId oid, QueryEngine engine) {
        DataRow retained = getRetainedSnapshot(oid);
        return (retained != null) ? retained : getDataRowCache().getSnapshot(oid, engine);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, it will attempt fetching it using provided QueryEngine. If
     * fetch attempt fails or inconsistent data is returned, underlying cache will throw a
     * CayenneRuntimeException.
     * 
     * @since 1.2
     */
    public synchronized DataRow getSnapshot(ObjectId oid) {
        DataRow retained = getRetainedSnapshot(oid);
        if (retained != null) {
            return retained;
        }

        if (context != null && context.getChannel() != null) {
            SingleObjectQuery query = new SingleObjectQuery(oid, true, false);
            List results = context.getChannel().onQuery(context, query).firstList();
            return results.isEmpty() ? null : (DataRow) results.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * Returns a list of objects that are registered with this DataContext, regardless of
     * their persistence state. List is returned by copy and can be modified by the
     * caller.
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
     * Returns EventManager use by this ObjectStore.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return (getDataRowCache() != null) ? getDataRowCache().getEventManager() : null;
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this ObjectStore, <code>false</code> otherwise. This method will
     * treat "phantom" modifications are real ones. I.e. if you "change" an object
     * property to an equivalent value, this method will still think such object is
     * modified. Phantom modifications are only detected and discarded during commit.
     */
    public synchronized boolean hasChanges() {

        if (checkIndirectChanges()) {
            return true;
        }

        if (!retainedSnapshotMap.isEmpty()) {
            return true;
        }

        Iterator it = getObjectIterator();
        while (it.hasNext()) {
            DataObject dataObject = (DataObject) it.next();
            int state = dataObject.getPersistenceState();
            if (state == PersistenceState.NEW
                    || state == PersistenceState.DELETED
                    || state == PersistenceState.MODIFIED) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return a subset of registered objects that are in a certian persistence state.
     * Collection is returned by copy.
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
     * SnapshotEventListener implementation that processes snapshot change event, updating
     * DataObjects that have the changes.
     * <p>
     * <i>Implementation note: </i> This method should not attempt to alter the underlying
     * DataRowStore, since it is normally invoked *AFTER* the DataRowStore was modified as
     * a result of some external interaction.
     * </p>
     * 
     * @since 1.1
     */
    public void snapshotsChanged(SnapshotEvent event) {
        // filter events that we should not process
        if (event.getPostedBy() == this || event.getSource() != this.getDataRowCache()) {
            return;
        }

        // merge objects with changes in event...
        if (logObj.isDebugEnabled()) {
            logObj.debug("Received: " + event);
        }

        synchronized (this) {
            processUpdatedSnapshots(event.getModifiedDiffs());
            processDeletedIDs(event.getDeletedIds());
            processInvalidatedIDs(event.getInvalidatedIds());
            processIndirectlyModifiedIDs(event.getIndirectlyModifiedIds());
        }
    }

    /**
     * Performs validation of all uncommitted objects in the ObjectStore. If validation
     * fails, a ValidationException is thrown, listing all encountered failures. This is a
     * utility method for the users to call. Cayenne itself uses a different mechanism to
     * validate objects on commit.
     * 
     * @since 1.1
     * @throws ValidationException
     */
    public synchronized void validateUncommittedObjects() throws ValidationException {

        // we must iterate over a copy of object list,
        // as calling validateFor* on DataObjects can have a side effect
        // of modifying this ObjectStore, and thus resulting in
        // ConcurrentModificationExceptions in the Iterator

        Collection deleted = null;
        Collection inserted = null;
        Collection updated = null;

        Iterator allIt = getObjectIterator();
        while (allIt.hasNext()) {
            DataObject dataObject = (DataObject) allIt.next();
            switch (dataObject.getPersistenceState()) {
                case PersistenceState.NEW:
                    if (inserted == null) {
                        inserted = new ArrayList();
                    }
                    inserted.add(dataObject);
                    break;
                case PersistenceState.MODIFIED:
                    if (updated == null) {
                        updated = new ArrayList();
                    }
                    updated.add(dataObject);
                    break;
                case PersistenceState.DELETED:
                    if (deleted == null) {
                        deleted = new ArrayList();
                    }
                    deleted.add(dataObject);
                    break;
            }
        }

        ValidationResult validationResult = new ValidationResult();

        if (deleted != null) {
            Iterator it = deleted.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForDelete(validationResult);
            }
        }

        if (inserted != null) {
            Iterator it = inserted.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForInsert(validationResult);
            }
        }

        if (updated != null) {
            Iterator it = updated.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForUpdate(validationResult);
            }
        }

        if (validationResult.hasFailures()) {
            throw new ValidationException(validationResult);
        }
    }

    /**
     * Returns whether ObjectStore has changes not directly reflected in the object state.
     * 
     * @since 1.2
     */
    boolean checkIndirectChanges() {
        return !flattenedInserts.isEmpty() || !flattenedDeletes.isEmpty();
    }

    /**
     * Initializes object with data from cache or from the database, if this object is not
     * fully resolved.
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
            DataRow snapshot = getSnapshot(object.getObjectId());

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
                    case PersistenceState.COMMITTED:
                    case PersistenceState.HOLLOW:
                    case PersistenceState.DELETED:

                        // consult delegate
                        delegate = object.getDataContext().nonNullDelegate();

                        if (delegate.shouldProcessDelete(object)) {
                            objectMap.remove(oid);
                            retainedSnapshotMap.remove(oid);

                            // setting DataContext to null will also set
                            // state to transient
                            object.setDataContext(null);
                            delegate.finishedProcessDelete(object);
                        }

                        break;

                    case PersistenceState.MODIFIED:

                        // consult delegate
                        delegate = object.getDataContext().nonNullDelegate();
                        if (delegate.shouldProcessDelete(object)) {
                            object.setPersistenceState(PersistenceState.NEW);
                            delegate.finishedProcessDelete(object);
                        }

                        break;
                }
            }
        }
    }

    /**
     * @since 1.1
     */
    void processInvalidatedIDs(Collection invalidatedIDs) {
        if (invalidatedIDs != null && !invalidatedIDs.isEmpty()) {
            Iterator it = invalidatedIDs.iterator();
            while (it.hasNext()) {
                ObjectId oid = (ObjectId) it.next();
                DataObject object = getObject(oid);

                if (object == null) {
                    continue;
                }

                // TODO: refactor "switch" to avoid code duplication

                switch (object.getPersistenceState()) {
                    case PersistenceState.COMMITTED:
                        object.setPersistenceState(PersistenceState.HOLLOW);
                        break;
                    case PersistenceState.MODIFIED:
                        DataContext context = object.getDataContext();
                        DataRow diff = getSnapshot(oid);
                        // consult delegate if it exists
                        DataContextDelegate delegate = context.nonNullDelegate();
                        if (delegate.shouldMergeChanges(object, diff)) {
                            ObjEntity entity = context
                                    .getEntityResolver()
                                    .lookupObjEntity(object);
                            DataRowUtils.forceMergeWithSnapshot(entity, object, diff);
                            delegate.finishedMergeChanges(object);
                        }

                    case PersistenceState.HOLLOW:
                        // do nothing
                        break;

                    case PersistenceState.DELETED:
                        // TODO: Do nothing? Or treat as merged?
                        break;
                }
            }
        }
    }

    /**
     * @since 1.1
     */
    void processIndirectlyModifiedIDs(Collection indirectlyModifiedIDs) {
        Iterator indirectlyModifiedIt = indirectlyModifiedIDs.iterator();
        while (indirectlyModifiedIt.hasNext()) {
            ObjectId oid = (ObjectId) indirectlyModifiedIt.next();

            DataObject object = getObject(oid);

            if (object == null
                    || object.getPersistenceState() != PersistenceState.COMMITTED) {
                continue;
            }

            // for now "break" all "independent" object relationships...
            // in the future we may want to be more precise and go after modified
            // relationships only, or even process updated lists without invalidating...

            DataContextDelegate delegate = object.getDataContext().nonNullDelegate();

            if (delegate.shouldMergeChanges(object, null)) {
                ObjEntity entity = object
                        .getDataContext()
                        .getEntityResolver()
                        .lookupObjEntity(object);
                Iterator relationshipIterator = entity.getRelationships().iterator();
                while (relationshipIterator.hasNext()) {
                    ObjRelationship relationship = (ObjRelationship) relationshipIterator
                            .next();

                    if (relationship.isSourceIndependentFromTargetChange()) {
                        Object fault = relationship.isToMany()
                                ? Fault.getToManyFault()
                                : Fault.getToOneFault();
                        object.writePropertyDirectly(relationship.getName(), fault);
                    }
                }

                delegate.finishedProcessDelete(object);
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

                // perform same steps as resolveHollow()
                if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                    // consult delegate if it exists
                    DataContextDelegate delegate = object
                            .getDataContext()
                            .nonNullDelegate();
                    if (delegate.shouldMergeChanges(object, diff)) {
                        ObjEntity entity = object
                                .getDataContext()
                                .getEntityResolver()
                                .lookupObjEntity(object);
                        DataRow snapshot = getSnapshot(object.getObjectId());
                        DataRowUtils.refreshObjectWithSnapshot(
                                entity,
                                object,
                                snapshot,
                                true);
                        delegate.finishedMergeChanges(object);
                    }
                    continue;
                }

                // merge modified and deleted
                if (object.getPersistenceState() == PersistenceState.DELETED
                        || object.getPersistenceState() == PersistenceState.MODIFIED) {

                    // consult delegate if it exists
                    DataContextDelegate delegate = object
                            .getDataContext()
                            .nonNullDelegate();
                    if (delegate.shouldMergeChanges(object, diff)) {
                        ObjEntity entity = object
                                .getDataContext()
                                .getEntityResolver()
                                .lookupObjEntity(object);
                        DataRowUtils.forceMergeWithSnapshot(entity, object, diff);
                        delegate.finishedMergeChanges(object);
                    }
                }
            }
        }
    }

    /**
     * Records the fact that flattened relationship was created.
     * 
     * @since 1.1
     */
    void flattenedRelationshipSet(
            DataObject source,
            ObjRelationship relationship,
            DataObject destination) {

        if (!relationship.isFlattened()) {
            return;
        }

        if (relationship.isReadOnly()) {
            throw new CayenneRuntimeException(
                    "Cannot set the read-only flattened relationship "
                            + relationship.getName());
        }

        // Register this combination (so we can remove it later if an insert occurs before
        // commit)
        FlattenedRelationshipUpdate info = new FlattenedRelationshipUpdate(
                source,
                destination,
                relationship);

        // If this combination has already been deleted, simply undelete it.
        if (!flattenedDeletes.remove(info) && !flattenedInserts.contains(info)) {
            flattenedInserts.add(info);
        }
    }

    /**
     * Records the fact that flattened relationship was broken down.
     * 
     * @since 1.1
     */
    void flattenedRelationshipUnset(
            DataObject source,
            ObjRelationship relationship,
            DataObject destination) {

        if (!relationship.isFlattened()) {
            return;
        }

        if (relationship.isReadOnly()) {
            throw new CayenneRuntimeException(
                    "Cannot unset the read-only flattened relationship "
                            + relationship.getName());
        }

        // Register this combination,
        // so we can remove it later if an insert occurs before commit
        FlattenedRelationshipUpdate info = new FlattenedRelationshipUpdate(
                source,
                destination,
                relationship);

        // If this combination has already been inserted, simply "uninsert" it
        // also do not delete it twice
        if (!flattenedInserts.remove(info) && !flattenedDeletes.contains(info)) {
            flattenedDeletes.add(info);
        }
    }

    List getFlattenedInserts() {
        return flattenedInserts;
    }

    List getFlattenedDeletes() {
        return flattenedDeletes;
    }

    /**
     * @since 1.2
     */
    void setDataRowCacheFactory(Factory dataRowCacheFactory) {
        this.dataRowCacheFactory = dataRowCacheFactory;
    }

    /**
     * @since 1.2
     */
    Factory getDataRowCacheFactory() {
        return dataRowCacheFactory;
    }

    class IdUpdater implements GraphChangeHandler {

        public void nodeIdChanged(Object nodeId, Object newId) {
            Persistent object = (Persistent) objectMap.remove(nodeId);

            if (object != null) {
                object.setObjectId((ObjectId) newId);
                objectMap.put(newId, object);
            }
        }

        public void nodeCreated(Object nodeId) {

        }

        public void nodeRemoved(Object nodeId) {

        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {

        }

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        }
    }

    /**
     * @since 1.2
     */
    public DataContext getContext() {
        return context;
    }

    /**
     * @since 1.2
     */
    public void setContext(DataContext context) {
        this.context = context;
    }
}