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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.FlattenedObjectId;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.access.util.PrefetchHelper;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.access.util.RelationshipDataSource;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * User-level Cayenne access class. Provides isolated object view of
 * the datasource to the application code. Normal use pattern is to
 * create one DataContext per session (whatever session may mean in
 * a given application).
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public class DataContext implements QueryEngine, Serializable {
    private static Logger logObj = Logger.getLogger(DataContext.class);

    // DataContext events
    public static final EventSubject WILL_COMMIT =
        EventSubject.getSubject(DataContext.class, "DataContextWillCommit");
    public static final EventSubject DID_COMMIT =
        EventSubject.getSubject(DataContext.class, "DataContextDidCommit");
    public static final EventSubject DID_ROLLBACK =
        EventSubject.getSubject(DataContext.class, "DataContextDidRollback");

    // event posting default for new DataContexts
    private static boolean transactionEventsEnabledDefault;

    // enable/disable event handling for individual instances
    private boolean transactionEventsEnabled;

    private List flattenedInserts = new ArrayList();
    private List flattenedDeletes = new ArrayList();

    protected transient QueryEngine parent;
    // When deserialized, the parent domain name is stored in
    // this variable until the parent is actually needed.  This helps
    // avoid an issue with certain servlet engines (e.g. Tomcat) where
    // HttpSessions with DataContext's are deserialized at startup
    // before the configuration has been read.
    protected transient String lazyInitParentDomainName;

    protected transient ObjectStore objectStore;
    protected transient ToManyListDataSource relationshipDataSource;

    /**
     * Convenience method to create a new instance of
     * DataContext based on default domain. If more
     * than one domain exists, createDataContext(String)
     * must be used.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
     * Convenience method to create a new instance of
     * DataContext based on a named domain.
     * If there is no domain matching the name,
     * an exception is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    public DataContext() {
        this(null);
    }

    /**
     * Creates new DataContext and initializes it
     * with the parent QueryEngine. Normally parent is an
     * instance of DataDomain. DataContext will use parent
     * to execute database queries, updates, and access
     * DataMap objects.
     */
    public DataContext(QueryEngine parent) {
        setParent(parent);
        this.objectStore = new ObjectStore();
        this.relationshipDataSource = new RelationshipDataSource(this);
        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
    }

    /** Returns parent QueryEngine object. */
    public QueryEngine getParent() {
        if (parent == null && lazyInitParentDomainName != null) {
            this.parent =
                Configuration.getSharedConfiguration().getDomain(
                    lazyInitParentDomainName);
        }
        return parent;
    }

    /**
     * Sets parent QueryEngine of this DataContext.
     */
    public void setParent(QueryEngine parent) {
        this.parent = parent;
    }

    /**
     * @deprecated Since 1.1 all SnapshotManager methods are static
     */
    public SnapshotManager getSnapshotManager() {
        return new SnapshotManager();
    }

    public ToManyListDataSource getRelationshipDataSource() {
        return this.relationshipDataSource;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified,
     * deleted or new objects registered with this DataContext,
     * <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        // TODO: the logic checking flattened relationships should 
        // most likely be pushed to the ObjectStore

        return !this.getFlattenedInserts().isEmpty()
            || !this.getFlattenedDeletes().isEmpty()
            || objectStore.hasChanges();
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.NEW
     */
    public Collection newObjects() {
        return objectStore.objectsInState(PersistenceState.NEW);
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.DELETED
     */
    public Collection deletedObjects() {
        return objectStore.objectsInState(PersistenceState.DELETED);
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.MODIFIED
     */
    public Collection modifiedObjects() {
        return objectStore.objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns an object for a given ObjectId.
     * If object is not registered with this context,
     * a "hollow" object fault is created, registered, 
     * and returned to the caller.
     */
    public DataObject registeredObject(ObjectId oid) {
        // must synchronize on ObjectStore since we must read and write atomically
        synchronized (objectStore) {
            DataObject obj = objectStore.getObject(oid);
            if (obj == null) {
                try {
                    obj = SnapshotManager.newDataObject(oid.getObjClass().getName());
                }
                catch (Exception ex) {
                    String entity =
                        (oid != null)
                            ? getEntityResolver()
                                .lookupObjEntity(oid.getObjClass())
                                .getName()
                            : null;
                    throw new CayenneRuntimeException(
                        "Error creating object for entity '" + entity + "'.",
                        ex);
                }

                obj.setObjectId(oid);
                obj.setPersistenceState(PersistenceState.HOLLOW);
                obj.setDataContext(this);
                objectStore.addObject(obj);
            }
            return obj;
        }
    }

    /** Takes a snapshot of current object state. */
    public Map takeObjectSnapshot(DataObject anObject) {
        ObjEntity ent = getEntityResolver().lookupObjEntity(anObject);
        return SnapshotManager.takeObjectSnapshot(ent, anObject);
    }

    /**
     * Creates and returns a DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     *
     * <p>Internally this method calls
     *  #objectFromDataRow(org.objectststyle.cayenne.map.ObjEntity,java.util.Map,boolean)
     * objectFromDataRow(ObjEntity, Map, boolean)}
     * with <code>false</code> "refersh" parameter.</p>
     */
    public DataObject objectFromDataRow(String entityName, Map dataRow) {
        ObjEntity ent = this.getEntityResolver().lookupObjEntity(entityName);
        return objectFromDataRow(ent, dataRow, false);
    }

    /**
     * Creates and returns a DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     */
    public DataObject objectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {

        ObjectId anId = SnapshotManager.objectIdFromSnapshot(objEntity, dataRow);

        // synchronized on objectstore, since read/write
        // must be performed atomically
        synchronized (objectStore) {
            // this will create a HOLLOW object if it is not registered yet
            DataObject obj = registeredObject(anId);

            if (refresh || obj.getPersistenceState() == PersistenceState.HOLLOW) {
                // we are asked to refresh an existing object with new values
                SnapshotManager.mergeObjectWithSnapshot(objEntity, obj, dataRow);

                //The merge might leave the object in hollow state if
                // dataRow was only partial.  If so, do not add the snapshot
                // to the objectstore.
                if (obj.getPersistenceState() != PersistenceState.HOLLOW) {
                    objectStore.addSnapshot(anId, dataRow);
                }

                // notify object that it was fetched
                obj.fetchFinished();
            }

            return obj;
        }
    }

    /**
     * Creates and returns a read-only DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     * 
     * @deprecated Since 1.1 This method is not used in Cayenne anymore. Use
     * #objectFromDataRow(org.objectststyle.cayenne.map.ObjEntity,java.util.Map,boolean)
     * objectFromDataRow(ObjEntity, Map, boolean)} instead.
     */
    protected DataObject readOnlyObjectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {

        return this.objectFromDataRow(objEntity, dataRow, refresh);
    }

    /**
     * Instantiates new object and registers it with itself. Object class
     * is determined from ObjEntity. Object class must have a default constructor.
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objEntityName);

        if (entity == null) {
            throw new IllegalArgumentException("Invalid entity name: " + objEntityName);
        }

        String objClassName = entity.getClassName();
        DataObject dobj = null;
        try {
            dobj = SnapshotManager.newDataObject(objClassName);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObject(dobj, objEntityName);
        return dobj;
    }

    /** Registers a new object (that is not yet persistent) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     * @param objEntityName a name of the ObjEntity in the map used to get
     *  persistence information for this object.
     */
    public void registerNewObject(DataObject dataObject, String objEntityName) {
        ObjEntity objEntity = getEntityResolver().lookupObjEntity(objEntityName);
        registerNewObjectWithEntity(dataObject, objEntity);
    }

    /** Registers a new object (that is not yet persistent) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     */
    public void registerNewObject(DataObject dataObject) {
        ObjEntity objEntity = getEntityResolver().lookupObjEntity(dataObject);
        registerNewObjectWithEntity(dataObject, objEntity);
    }

    private void registerNewObjectWithEntity(
        DataObject dataObject,
        ObjEntity objEntity) {
        TempObjectId tempId = new TempObjectId(dataObject.getClass());
        dataObject.setObjectId(tempId);

        // initialize to-many relationships with empty lists
        Iterator it = objEntity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.isToMany()) {
                ToManyList relList =
                    new ToManyList(
                        relationshipDataSource,
                        dataObject.getObjectId(),
                        rel.getName());
                dataObject.writePropertyDirectly(rel.getName(), relList);
            }
        }

        objectStore.addObject(dataObject);
        dataObject.setDataContext(this);
        dataObject.setPersistenceState(PersistenceState.NEW);
    }

    /**
     * @deprecated Since 1.1, use 
     * {@link #unregisterObjects(java.util.Collection) unregisterObjects(Collections.singletonList(dataObject))}
     * to invalidate a single object.
     */
    public void unregisterObject(DataObject dataObject) {
        unregisterObjects(Collections.singletonList(dataObject));
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext 
     * and the underlying ObjectStore. This operation also unsets 
     * DataContext and ObjectId for each object and changes its state 
     * to TRANSIENT.
     */
    public void unregisterObjects(Collection dataObjects) {
        objectStore.objectsUnregistered(dataObjects);
    }

    /**
     * @deprecated Since 1.1, use 
     * {@link #invalidateObjects(java.util.Collection) invalidateObjects(Collections.singletonList(dataObject))}
     * to invalidate a single object.
     */
    public void invalidateObject(DataObject dataObject) {
        invalidateObjects(Collections.singletonList(dataObject));
    }

    /**
      * "Invalidates" a Collection of DataObject. This operation would remove 
      * each object's snapshot from cache and change object's state to HOLLOW.
      * On the next access to this object, it will be refeched.
      */
    public void invalidateObjects(Collection dataObjects) {
        objectStore.objectsInvalidated(dataObjects);
    }

    /**
     * Notifies data context that a registered object need to be deleted on
     * next commit.
     *
     * @param anObject data object that we want to delete.
     */

    public void deleteObject(DataObject anObject) {
        if (anObject.getPersistenceState() == PersistenceState.DELETED) {
            //Drop out... we might be about to get into a horrible
            // recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return;
        }

        //Save the current state in case of a deny, in which case it should be reset.
        //We cannot delay setting it to deleted, as Cascade deletes might cause
        // recursion, and the "deleted" state is the best way we have of noticing that and bailing out (see above)
        int oldState = anObject.getPersistenceState();

        //TODO - figure out what to do when an object is still in
        //PersistenceState.NEW (unregister maybe?)
        anObject.setPersistenceState(PersistenceState.DELETED);

        //Do the right thing with all the relationships of the deleted object
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(anObject);
        Iterator relationshipIterator = entity.getRelationships().iterator();
        while (relationshipIterator.hasNext()) {
            ObjRelationship thisRelationship =
                (ObjRelationship) relationshipIterator.next();
            String thisRelationshipName = thisRelationship.getName();

            List relatedObjects;
            if (thisRelationship.isToMany()) {
                //Get an independent copy of the list so that
                // deleting objects doesn't result in concurrent modification
                // exceptions
                relatedObjects =
                    new ArrayList(
                        (List) anObject.readPropertyDirectly(thisRelationship.getName()));
            }
            else {
                //thisRelationship is toOne... make a list of one object
                relatedObjects = new ArrayList(1);
                DataObject relatedObject =
                    (DataObject) anObject.readPropertyDirectly(thisRelationshipName);
                if (relatedObject != null) {
                    relatedObjects.add(relatedObject);
                }
            }

            switch (thisRelationship.getDeleteRule()) {
                case DeleteRule.NULLIFY :
                    ObjRelationship inverseRelationship =
                        thisRelationship.getReverseRelationship();
                    if (null == inverseRelationship) {
                        continue;
                        //with next relationship... nothing we can do here
                    }
                    String inverseRelationshipName = inverseRelationship.getName();

                    if (inverseRelationship.isToMany()) {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.removeToManyTarget(
                                inverseRelationshipName,
                                anObject,
                                true);
                        }
                    }
                    else {
                        //Inverse is to-one - find all related objects and
                        // nullify the reverse relationship
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            if (inverseRelationship.isToDependentEntity()) {
                                relatedObject.setToOneDependentTarget(
                                    inverseRelationshipName,
                                    null);
                            }
                            else {
                                relatedObject.setToOneTarget(
                                    inverseRelationshipName,
                                    null,
                                    true);
                            }
                        }
                    }
                    break;
                case DeleteRule.CASCADE :
                    //Delete all related objects
                    Iterator iterator = relatedObjects.iterator();
                    while (iterator.hasNext()) {
                        DataObject relatedObject = (DataObject) iterator.next();
                        this.deleteObject(relatedObject);
                    }
                    break;
                case DeleteRule.DENY :
                    int relatedObjectsCount = relatedObjects.size();
                    if (relatedObjectsCount != 0) {
                        //Clean up - we shouldn't be deleting this object
                        anObject.setPersistenceState(oldState);
                        throw new DeleteDenyException(
                            "Cannot delete a "
                                + getEntityResolver().lookupObjEntity(anObject).getName()
                                + " because it has "
                                + relatedObjectsCount
                                + " object"
                                + (relatedObjectsCount > 1 ? "s" : "")
                                + "in it's "
                                + thisRelationshipName
                                + " relationship"
                                + " and this relationship has DENY "
                                + "as it's delete rule");
                    }
                    break;
                case DeleteRule.NO_ACTION :
                    // no action it is...
                    break;
                default :
                    //Clean up - we shouldn't be deleting this object
                    anObject.setPersistenceState(oldState);
                    throw new CayenneRuntimeException(
                        "Unknown type of delete rule "
                            + thisRelationship.getDeleteRule());
            }
        }
    }

    /**
     * Refetches object data for ObjectId. This method is used
     * internally by Cayenne to resolve objects in state
     * <code>PersistenceState.HOLLOW</code>. It can also be used
     * to refresh certain objects.
     *
     * @throws CayenneRuntimeException if object id doesn't match
     * any records, or if there is more than one object is fetched.
     */
    public DataObject refetchObject(ObjectId oid) {

        synchronized (objectStore) {
            DataObject object = objectStore.getObject(oid);

            // clean up any cached data for this object
            if (object != null) {
                this.invalidateObject(object);
            }
        }

        List results;
        if (oid instanceof FlattenedObjectId) {
            FlattenedObjectId foid = (FlattenedObjectId) oid;
            SelectQuery sel = QueryUtils.selectObjectForFlattenedObjectId(this, foid);
            FlattenedSelectObserver observer = new FlattenedSelectObserver(foid);
            this.performQueries(Collections.singletonList(sel), observer);
            results = observer.getResults(sel);
        }
        else {
            SelectQuery sel = QueryUtils.selectObjectForId(oid);
            results = this.performQuery(sel);
        }

        if (results.size() != 1) {
            String msg =
                (results.size() == 0)
                    ? "No matching objects found for ObjectId " + oid
                    : "More than 1 object found for ObjectId "
                        + oid
                        + ". Fetch matched "
                        + results.size()
                        + " objects.";

            throw new CayenneRuntimeException(msg);
        }

        return (DataObject) results.get(0);
    }

    /**
     * Rollsback any changes that have occurred to objects
     * registered with this data context.
     */
    public void rollbackChanges() {
        synchronized (objectStore) {
            List objectsToUnregister = new ArrayList();
            Iterator it = objectStore.getObjectIterator();

            // collect candidates
            while (it.hasNext()) {
                DataObject thisObject = (DataObject) it.next();
                int objectState = thisObject.getPersistenceState();
                switch (objectState) {
                    case PersistenceState.NEW :
                        //We cannot unregister at this stage, because that would modify the map upon which
                        // the iterator returned by objectStore.getObjectIterator() is based.  It is done outside the iterator loop
                        objectsToUnregister.add(thisObject);
                        break;
                    case PersistenceState.DELETED :
                        //Do the same as for modified... deleted is only a persistence state, so
                        // rolling the object back will set the state to committed
                    case PersistenceState.MODIFIED :
                        // this will clean any modifications and deferrefresh from snapshot
                        // till the next object accessor is called
                        thisObject.setPersistenceState(PersistenceState.HOLLOW);
                        break;
                    default :
                        //Transient, committed and hollow need no handling
                        break;
                }
            }

            // unregister candidates
			unregisterObjects(objectsToUnregister);

            // finally clear flattened inserts & deletes
            this.clearFlattenedUpdateQueries();
        }

    }

    /**
     * Synchronizes object graph with the database. Executes needed
     * insert, update and delete queries (generated internally).
     */
    public void commitChanges() throws CayenneRuntimeException {
        commitChanges(null);
    }

    /**
     * Synchronizes object graph with the database. Executes needed
     * insert, update and delete queries (generated internally).
     *
     * @param logLevel if logLevel is higher or equals to the level
     * set for QueryLogger, statements execution will be logged.
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {

        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        // prevent multiple commits occuring simulteneously 
        synchronized (objectStore) {
            // is there anything to do?
            if (this.hasChanges() == false) {
                return;
            }

            ContextCommit worker = new ContextCommit(this);

            try {
                worker.commit(logLevel);
                this.clearFlattenedUpdateQueries();
            }
            catch (CayenneException ex) {
                throw new CayenneRuntimeException(ex);
            }
        }
    }

    /**
     * Performs a single database select query.
     *
     * @return A list of DataObjects or a list of data rows
     * depending on the value returned by <code>query.isFetchingDataRows()</code>.
     */
    public List performQuery(GenericSelectQuery query) {

        // check if result pagination is requested
        // let a list handle fetch in this case
        if (query.getPageSize() > 0) {
            return new IncrementalFaultList(this, query);
        }

        SelectObserver observer = new SelectObserver(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);

        return (query.isFetchingDataRows())
            ? observer.getResults(query)
            : observer.getResultsAsObjects(this, query);
    }

    /**
     * Performs a single database select query returning result as a ResultIterator.
     * Returned ResultIterator will provide access to "data rows"
     * - maps with database data that can be used to create DataObjects.
     */
    public ResultIterator performIteratedQuery(GenericSelectQuery query)
        throws CayenneException {

        IteratedSelectObserver observer = new IteratedSelectObserver();
        observer.setLoggingLevel(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
    }

    /** Delegates node lookup to parent QueryEngine. */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().dataNodeForObjEntity(objEntity);
    }

    /**
     * Delegates queries execution to parent QueryEngine. If there are select
     * queries that require prefetching relationships, will create additional
     * queries to perform necessary prefetching.
     */
    public void performQueries(List queries, OperationObserver resultConsumer) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        // find queries that require prefetching
        List prefetch = new ArrayList();

        // if we expect iterated queries, ignore prefetching
        if (!resultConsumer.isIteratedResult()) {
            Iterator it = queries.iterator();
            while (it.hasNext()) {
                Object q = it.next();
                if (q instanceof SelectQuery) {
                    SelectQuery sel = (SelectQuery) q;
                    Collection prefetchRels = sel.getPrefetches();
                    if (prefetchRels.size() > 0) {
                        Iterator prIt = prefetchRels.iterator();
                        while (prIt.hasNext()) {
                            prefetch.add(
                                QueryUtils.selectPrefetchPath(
                                    this,
                                    sel,
                                    (String) prIt.next()));
                        }
                    }
                }
            }
        }

        List finalQueries = null;
        if (prefetch.size() == 0) {
            finalQueries = queries;
        }
        else {
            prefetch.addAll(0, queries);
            finalQueries = prefetch;
        }

        this.getParent().performQueries(finalQueries, resultConsumer);
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships
     * for a list of DataObjects in the most optimized way (preferrably in
     * a single query per relationship).
     *
     * <p><i>WARNING: Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed eventually.</i></p>
     */
    public void prefetchRelationships(SelectQuery query, List objects) {
        Collection prefetches = query.getPrefetches();

        if (objects == null || objects.size() == 0 || prefetches.size() == 0) {
            return;
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(query);
        Iterator prefetchesIt = prefetches.iterator();
        while (prefetchesIt.hasNext()) {
            String prefetchKey = (String) prefetchesIt.next();
            if (prefetchKey.indexOf(Entity.PATH_SEPARATOR) >= 0) {
                throw new CayenneRuntimeException(
                    "Only one-step relationships are "
                        + "supported at the moment, this will be fixed soon. "
                        + "Unsupported path : "
                        + prefetchKey);
            }

            ObjRelationship relationship =
                (ObjRelationship) entity.getRelationship(prefetchKey);
            if (relationship == null) {
                throw new CayenneRuntimeException("Invalid relationship: " + prefetchKey);
            }

            if (relationship.isToMany()) {
                throw new CayenneRuntimeException(
                    "Only to-one relationships are supported at the moment. "
                        + "Can't prefetch to-many: "
                        + prefetchKey);
            }

            PrefetchHelper.resolveToOneRelations(this, objects, prefetchKey);
        }

    }

    /** 
     * Delegates query execution to parent QueryEngine.
     *  
     * @deprecated Since 1.1 use performQueries(List, OperationObserver).
     * This method is redundant and doesn't add value.
     */
    public void performQuery(Query query, OperationObserver operationObserver) {
        this.performQueries(Collections.singletonList(query), operationObserver);
    }

    /**
     *  Populates the <code>map</code> with ObjectId values from master objects
     *  related to this object.
     */
    private void appendPkFromMasterRelationships(Map map, DataObject dataObject) {
        ObjEntity objEntity = this.getEntityResolver().lookupObjEntity(dataObject);
        DbEntity dbEntity = objEntity.getDbEntity();

        Iterator it = dbEntity.getRelationshipMap().values().iterator();
        while (it.hasNext()) {
            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK()) {
                continue;
            }

            ObjRelationship rel = objEntity.getRelationshipForDbRelationship(dbRel);
            if (rel == null) {
                continue;
            }

            DataObject targetDo =
                (DataObject) dataObject.readPropertyDirectly(rel.getName());
            if (targetDo == null) {
                // this is bad, since we will not be able to obtain PK in any other way
                // throw an exception
                throw new CayenneRuntimeException("Null master object, can't create primary key.");
            }

            Map idMap = targetDo.getObjectId().getIdSnapshot();
            if (idMap == null) {
                // this is bad, since we will not be able to obtain PK in any other way
                // provide a detailed error message
                StringBuffer msg =
                    new StringBuffer("Can't create primary key, master object has no PK snapshot.");
                msg
                    .append("\nrelationship name: ")
                    .append(dbRel.getName())
                    .append(", src object: ")
                    .append(dataObject.getObjectId().getObjClass().getName())
                    .append(", target obj: ")
                    .append(targetDo.getObjectId().getObjClass().getName());
                throw new CayenneRuntimeException(msg.toString());
            }

            map.putAll(dbRel.srcFkSnapshotWithTargetSnapshot(idMap));
        }
    }

    /** Creates permanent ObjectId for <code>anObject</code>.
     *  Object must already have a temporary ObjectId.
     *
     *  <p>This method is called when we are about to save a new object to
     *  the database. Primary key columns are populated assigning values
     *  in the following sequence:
     *  <ul>
     *     <li>Object attribute values are used.</li>
     *     <li>Values from ObjectId's propagated from master relationshop
     *     are used. <i>If master object does not have a permanent id
     *     created yet, an exception is thrown.</i></li>
     *     <li>Values generated from the database provided by DbAdapter.
     *     <i>Autogeneration only works for a single column. If more than
     *     one column requires an autogenerated primary key, an exception is
     *     thrown</i></li>
     *   </ul>
     *
     *   @return Newly created ObjectId.
     */
    public ObjectId createPermId(DataObject anObject) throws CayenneRuntimeException {
        ObjectId id = anObject.getObjectId();
        if (!(id instanceof TempObjectId)) {
            return id;
            //If the id is not a temp, then it must be permanent.  Return it and do nothing else
        }
        TempObjectId tempId = (TempObjectId) id;
        if (tempId.getPermId() != null) {
            return tempId.getPermId();
        }
        ObjEntity objEntity =
            this.getEntityResolver().lookupObjEntity(tempId.getObjClass());
        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode aNode = this.dataNodeForObjEntity(objEntity);

        Map idMap = new HashMap();
        // first get values delivered via relationships
        appendPkFromMasterRelationships(idMap, anObject);

        boolean autoPkDone = false;
        Iterator it = dbEntity.getPrimaryKey().iterator();
        while (it.hasNext()) {
            DbAttribute attr = (DbAttribute) it.next();

            // see if it is there already
            if (idMap.get(attr.getName()) != null) {
                continue;
            }

            // try object value as PK
            ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(attr);
            if (objAttr != null) {
                idMap.put(
                    attr.getName(),
                    anObject.readPropertyDirectly(objAttr.getName()));
                continue;
            }

            // run PK autogeneration
            if (autoPkDone) {
                throw new CayenneRuntimeException("Primary Key autogeneration only works for a single attribute.");
            }

            try {
                PkGenerator gen = aNode.getAdapter().getPkGenerator();
                Object pk = gen.generatePkForDbEntity(aNode, objEntity.getDbEntity());
                autoPkDone = true;
                idMap.put(attr.getName(), pk);
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK", ex);
            }
        }

        ObjectId permId = new ObjectId(anObject.getClass(), idMap);

        // note that object registration did not change (new id is not attached to context, only to temp. oid)
        tempId.setPermId(permId);
        return permId;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it.  Then when deser happens, we can get back the DataDomain by name,
        // from the shared configuration (which will either load it if need be, or return
        // an existing one.
        out.defaultWriteObject();
        if (this.parent == null && this.lazyInitParentDomainName != null) {
            out.writeObject(lazyInitParentDomainName);
        }
        else if (this.parent instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.parent;
            out.writeObject(domain.getName());
        }
        else {
            out.writeObject(this.parent);
            //Hope that whatever this.parent is, that it is Serializable
        }

        //For writing, just write the objects.  They will be serialized possibly
        // as just objectIds... it's up to the object itself.  Reading will do magic
        out.writeObject(objectStore);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        Object value = in.readObject();
        if (value instanceof QueryEngine) {
            //Must be a real QueryEngine object - use it
            this.parent = (QueryEngine) value;
        }
        else if (value instanceof String) {
            //Must be the name of a DataDomain - use it
            this.lazyInitParentDomainName = (String) value;
        }
        else {
            throw new CayenneRuntimeException(
                "Parent attribute of DataContext was neither a QueryEngine nor "
                    + "the name of a valid DataDomain:"
                    + value);
        }

        // initialized new relationship datasource
        this.relationshipDataSource = new RelationshipDataSource(this);

        // CayenneDataObjects have a transient datacontext
        // because at deserialize time the datacontext may need to be different
        // than the one at serialize time (for programmer defined reasons).
        // So, when a dataobject is resurrected because it's datacontext was
        // serialized, it will then set the objects datacontext to the correctone
        // If deser'd "otherwise", it will not have a datacontext (good)

        objectStore = (ObjectStore) in.readObject();

        synchronized (objectStore) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject obj = (DataObject) it.next();
                obj.setDataContext(this);
            }
        }
    }

    public EntityResolver getEntityResolver() {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().getEntityResolver();
    }

    public void registerFlattenedRelationshipInsert(
        DataObject source,
        ObjRelationship relationship,
        DataObject destination) {
        if (logObj.isDebugEnabled()) {
            logObj.debug(
                "registerFlattenedRelationshipInsert for source of class "
                    + source.getClass().getName()
                    + ", rel="
                    + relationship.getName()
                    + ", destination class="
                    + destination.getClass().getName());
        }
        //Register this combination (so we can remove it later if an insert occurs before commit)
        FlattenedRelationshipInfo info =
            new FlattenedRelationshipInfo(source, destination, relationship);

        if (flattenedDeletes.contains(info)) {
            //If this combination has already been deleted, simply undelete it.
            logObj.debug(
                "This combination already deleted.. undeleting to simulate the insert");
            flattenedDeletes.remove(info);
        }
        else if (!flattenedInserts.contains(info)) {
            logObj.debug("This combination is not currently inserted... ok");
            flattenedInserts.add(info);
        }
    }

    public void registerFlattenedRelationshipDelete(
        DataObject source,
        ObjRelationship relationship,
        DataObject destination) {
        if (logObj.isDebugEnabled()) {
            logObj.debug(
                "registerFlattenedRelationshipDelete for source of class "
                    + source.getClass().getName()
                    + ", rel="
                    + relationship.getName()
                    + ", destination class="
                    + destination.getClass().getName());
        }
        //Register this combination (so we can remove it later if an insert occurs before commit)
        FlattenedRelationshipInfo info =
            new FlattenedRelationshipInfo(source, destination, relationship);

        if (flattenedInserts.contains(info)) {
            //If this combination has already been inserted, simply uninsert it.
            logObj.debug(
                "This combination already inserted..uninserting to simulate the delete");
            flattenedInserts.remove(info);
        }
        else if (!flattenedDeletes.contains(info)) { //Do not delete it twice
            logObj.debug(
                "This combination is not currently deleted... registering for deletes");
            flattenedDeletes.add(info);
        }
    }

    /**
     * Should be called once the queries returned by getFlattenedUpdateQueries
     * have been succesfully executed or reverted and are no longer needed.
     */
    protected void clearFlattenedUpdateQueries() {
        this.flattenedDeletes = new ArrayList();
        this.flattenedInserts = new ArrayList();
    }

    /**
     * Sets default for posting transaction events by new DataContexts.
     */
    public static void setTransactionEventsEnabledDefault(boolean flag) {
        transactionEventsEnabledDefault = flag;
    }

    /**
     * Enable/disable posting of transaction events by this DataContext.
     */
    public void setTransactionEventsEnabled(boolean flag) {
        this.transactionEventsEnabled = flag;
    }

    public boolean isTransactionEventsEnabled() {
        return this.transactionEventsEnabled;
    }

    public Collection getDataMaps() {
        return (parent != null) ? parent.getDataMaps() : Collections.EMPTY_LIST;
    }

    void fireWillCommit() {
        // post event: WILL_COMMIT
        if (this.transactionEventsEnabled) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.WILL_COMMIT);
        }
    }

    void fireTransactionRolledback() {
        // post event: DID_ROLLBACK
        if ((this.transactionEventsEnabled)) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.DID_ROLLBACK);
        }
    }

    void fireTransactionCommitted() {
        // post event: DID_COMMIT
        if ((this.transactionEventsEnabled)) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.DID_COMMIT);
        }
    }

    List getFlattenedInserts() {
        return flattenedInserts;
    }

    List getFlattenedDeletes() {
        return flattenedDeletes;
    }

    //Stores the information about a flattened relationship between two objects in a
    // canonical form, such that equals returns true if both objects refer to the same
    // pair of DataObjects connected by the same relationship (regardless of the
    // direction of the relationship used to construct this info object)
    static final class FlattenedRelationshipInfo extends Object {
        private DataObject source;
        private DataObject destination;
        private ObjRelationship baseRelationship;
        private String canonicalRelationshipName;

        public FlattenedRelationshipInfo(
            DataObject aSource,
            DataObject aDestination,
            ObjRelationship relationship) {
            super();
            this.source = aSource;
            this.destination = aDestination;
            this.baseRelationship = relationship;

            //Calculate canonical relationship name
            String relName1 = relationship.getName();
            ObjRelationship reverseRel = relationship.getReverseRelationship();
            if (reverseRel != null) {
                String relName2 = reverseRel.getName();
                //Find the lexically lesser name and use it first, then use the second.
                //If equal (the same name), it doesn't matter which order.. be arbitrary
                if (relName1.compareTo(relName2) <= 0) {
                    this.canonicalRelationshipName = relName1 + "." + relName2;
                }
                else {
                    this.canonicalRelationshipName = relName2 + "." + relName1;
                }
            }
            else {
                this.canonicalRelationshipName = relName1;
            }
        }

        /**
         * Does not care about the order of source/destination, only that the
         * pair and the canonical relationship name match
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof FlattenedRelationshipInfo)) {
                return false;
            }
            if (this == obj) {
                return true;
            }

            FlattenedRelationshipInfo otherObj = (FlattenedRelationshipInfo) obj;

            if (!this
                .canonicalRelationshipName
                .equals(otherObj.canonicalRelationshipName)) {
                return false;
            }
            //Check that either direct mapping matches (src=>src, dest=>dest), or that
            // cross mapping matches (src=>dest, dest=>src).
            if (((this.source.equals(otherObj.source))
                && (this.destination.equals(otherObj.destination)))
                || ((this.source.equals(otherObj.destination))
                    && (this.destination.equals(otherObj.source)))) {
                return true;
            }
            return false;
        }

        /**
         * Because equals effectively ignores the order of dataObject1/2,
         * summing the hashcodes is sufficient to fulfill the equals/hashcode
         * contract
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return source.hashCode()
                + destination.hashCode()
                + canonicalRelationshipName.hashCode();
        }
        /**
         * Returns the baseRelationship.
         * @return ObjRelationship
         */
        public ObjRelationship getBaseRelationship() {
            return baseRelationship;
        }

        /**
         * Returns the destination.
         * @return DataObject
         */
        public DataObject getDestination() {
            return destination;
        }

        /**
         * Returns the source.
         * @return DataObject
         */
        public DataObject getSource() {
            return source;
        }

    }

    private class FlattenedSelectObserver extends SelectObserver {
        FlattenedObjectId oid;

        public FlattenedSelectObserver(FlattenedObjectId anOid) {
            super();
            this.oid = anOid;
        }

        public void nextDataRows(Query query, List dataRows) {
            List result = new ArrayList();
            if (dataRows != null && dataRows.size() > 0) {
                //Really, we're only expecting one row, but lets be complete
                ObjEntity ent =
                    DataContext.this.getEntityResolver().lookupObjEntity(query);
                Iterator it = dataRows.iterator();
                while (it.hasNext()) {
                    //The next few lines are basically a cut down/modified 
                    // version of objectFromDataRow that handles the oid swapping
                    // properly 
                    Map dataRow = (Map) it.next();
                    DataObject obj = registeredObject(this.oid);
                    SnapshotManager.mergeObjectWithSnapshot(ent, obj, dataRow);
                    obj.fetchFinished();
                    //Swizzle object ids as the old "flattened" one isn't suitable for later
                    // identification of this object
                    ObjectId newOid = SnapshotManager.objectIdFromSnapshot(ent, dataRow);
                    obj.setObjectId(newOid);
                    DataContext.this.objectStore.changeObjectKey(this.oid, newOid);
                    result.add(obj);
                }
            }
            super.nextDataRows(query, result);
        }
    }
}
