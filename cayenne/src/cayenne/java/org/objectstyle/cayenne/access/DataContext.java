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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.QueryHelper;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.util.ContextCommitObserver;
import org.objectstyle.cayenne.access.util.ContextSelectObserver;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.access.util.RelationshipDataSource;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;
import org.objectstyle.cayenne.util.Util;

/** User-level Cayenne access class. Provides isolated object view of 
  * the datasource to the application code. Normal use pattern is to 
  * create DataContext in a session scope.
  *
  * <p><i>For more information see <a href="../../../../../userguide/index.html"
  * target="_top">Cayenne User Guide.</a></i></p>
  * 
  * @author Andrei Adamchik
  */
public class DataContext implements QueryEngine, Serializable {
    static Logger logObj = Logger.getLogger(DataContext.class.getName());

    //Will not be directly serialized - see read/writeObject for details
    protected transient QueryEngine parent;
    protected transient ObjectStore objectStore = new ObjectStore();

    //Must be deserialized slightly differently - see read/writeObject
    // protected transient Map registeredMap = Collections.synchronizedMap(new HashMap());
    // protected Map committedSnapshots = Collections.synchronizedMap(new HashMap());
    protected RelationshipDataSource relDataSource = new RelationshipDataSource(this);

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
        this.parent = parent;
    }

    /** Returns parent QueryEngine object. */
    public QueryEngine getParent() {
        return parent;
    }

    /**
     * Sets parent QueryEngine of this DataContext.
     */
    public void setParent(QueryEngine parent) {
        this.parent = parent;
    }
    
    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
    	return objectStore;
    }

    /** 
     * Returns a collection of objects that are registered
     * with this DataContext, regardless of their persistence state.
     * Collection is returned by copy and can be modified by caller.
     * 
     * @deprecated (Since 1.0 Alpha 4) 
     * Use DataContext.getObjectStore().getObjects() instead
     */
    public Collection registeredObjects() {
        return objectStore.getObjects();
    }

    /**
     * Returns <code>true</code> if there are any modified,
     * deleted or new objects registered with this DataContext,
     * <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return objectStore.hasChanges();
    }

    /** 
     * Returns a subset of registered objects that are in a 
     * certian persistence state. Collection is returned by
     * copy.
     * 
     * @deprecated (Since 1.0 Alpha 4) 
     * Use DataContext.getObjectStore().objectsInState(int) instead
     */
    public Collection objectsInState(int state) {
        return objectStore.objectsInState(state);
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
     * it is being fetched. If no such object exists,
     * a <code>CayenneRuntimeException</code> is thrown. 
     * 
     * @deprecated Use refecthObject instead.
     */
    public DataObject registeredExistingObject(ObjectId oid) {
        DataObject obj = objectStore.getObject(oid);
        if (obj == null) {
            obj = refetchObject(oid);
        }

        return obj;
    }

    /** 
     * Returns an object for a given ObjectId. 
     * If object is not registered with this context, 
     * a "hollow" object fault is created, registered and returned to the caller. 
     */
    public DataObject registeredObject(ObjectId oid) {
        // must synchronize on ObjectStore since we must read and write atomically
        synchronized (objectStore) {
            DataObject obj = objectStore.getObject(oid);
            if (obj == null) {
                try {
                    obj =
                        newDataObject(
                            lookupEntity(oid.getObjEntityName()).getClassName());
                } catch (Exception ex) {
                    String entity = (oid != null) ? oid.getObjEntityName() : null;
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

    private final DataObject newDataObject(String className) throws Exception {
        return (DataObject) Configuration
            .getResourceLoader()
            .loadClass(className)
            .newInstance();
    }

    /** 
     * Replaces all object attribute values with snapshot values. 
     * Sets object state to COMMITTED.
     */
    protected void refreshObjectWithSnapshot(
        ObjEntity ent,
        DataObject anObject,
        Map snapshot) {

        Map attrMap = ent.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);
            anObject.writePropertyDirectly(
                attrName,
                snapshot.get(attr.getDbAttribute().getName()));
        }

        Iterator rit = ent.getRelationshipList().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {
                // "to many" relationships have no information to collect from snapshot
                // rather we need to check if a relationship list exists, if not -
                // create an empty one.

                ToManyList relList =
                    new ToManyList(relDataSource, anObject.getObjectId(), rel.getName());
                anObject.writePropertyDirectly(rel.getName(), relList);
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationshipList().get(0);

            // dependent to one relationship is optional and can be null.
            if (dbRel.isToDependentPK()) {
                continue;
            }

            Map destMap = dbRel.targetPkSnapshotWithSrcSnapshot(snapshot);
            if (destMap == null) {
                continue;
            }

            ObjectId destId = new ObjectId(rel.getTargetEntity().getName(), destMap);
            anObject.writePropertyDirectly(rel.getName(), registeredObject(destId));
        }
        anObject.setPersistenceState(PersistenceState.COMMITTED);
    }

    protected void mergeObjectWithSnapshot(
        ObjEntity ent,
        DataObject anObject,
        Map snapshot) {
        if (anObject.getPersistenceState() == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(ent, anObject, snapshot);
            return;
        }

        Map oldSnap = getCommittedSnapshot(anObject);

        Map attrMap = ent.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);
            String dbAttrName = attr.getDbAttribute().getName();

            Object curVal = anObject.readPropertyDirectly(attrName);
            Object oldVal = oldSnap.get(dbAttrName);
            Object newVal = snapshot.get(dbAttrName);

            // if value not modified, update it from snapshot, 
            // otherwise leave it alone
            if (Util.nullSafeEquals(curVal, oldVal)
                && !Util.nullSafeEquals(newVal, curVal)) {
                anObject.writePropertyDirectly(attrName, newVal);
            }
        }
    }

    /** Takes a snapshot of current object state. */
    public Map takeObjectSnapshot(DataObject anObject) {
        HashMap map = new HashMap();

        ObjEntity ent = lookupEntity(anObject.getObjectId().getObjEntityName());
        Map attrMap = ent.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            DbAttribute dbAttr = ((ObjAttribute) attrMap.get(attrName)).getDbAttribute();
            map.put(dbAttr.getName(), anObject.readPropertyDirectly(attrName));
        }

        Map relMap = ent.getRelationshipMap();
        Iterator itr = relMap.keySet().iterator();
        while (itr.hasNext()) {
            String relName = (String) itr.next();
            ObjRelationship rel = (ObjRelationship) relMap.get(relName);

            // to-many will be handled on the other side
            if (rel.isToMany()) {
                continue;
            }

            if (rel.isToDependentEntity()) {
                continue;
            }

            DataObject target = (DataObject) anObject.readPropertyDirectly(relName);
            if (target == null) {
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationshipList().get(0);
            Map idParts = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects
            if (idParts == null) {
                continue;
            }

            Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            map.putAll(fk);
        }

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been 
        // set above when db relationships where processed.                
        Map thisIdParts = anObject.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {
            // put only thise that do not exist in the map
            Iterator itm = thisIdParts.keySet().iterator();
            while (itm.hasNext()) {
                Object nextKey = itm.next();
                if (!map.containsKey(nextKey))
                    map.put(nextKey, thisIdParts.get(nextKey));
            }
        }
        return map;
    }

    /** 
     * Creates and returns a DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     * 
     * <p>Internally this method calls 
     * <code>objectFromDataRow(ObjEntity, Map, boolean)</code>
     * with <code>false</code> "refersh" parameter.</p>
     */
    public DataObject objectFromDataRow(String entityName, Map dataRow) {
        ObjEntity ent = this.lookupEntity(entityName);
        return (ent.isReadOnly())
            ? readOnlyObjectFromDataRow(ent, dataRow, false)
            : objectFromDataRow(ent, dataRow, false);
    }

    /** 
     * Creates and returns a DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     */
    public DataObject objectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {
        ObjectId anId = objEntity.objectIdFromSnapshot(dataRow);

        // synchronized on objectstore, since read/write
        // must be performed atomically
        synchronized (objectStore) {
            // this will create a HOLLOW object if it is not registered yet
            DataObject obj = registeredObject(anId);

            if (refresh || obj.getPersistenceState() == PersistenceState.HOLLOW) {
                // we are asked to refresh an existing object with new values
                mergeObjectWithSnapshot(objEntity, obj, dataRow);
                objectStore.addSnapshot(anId, dataRow);

                // notify object that it was fetched
                obj.fetchFinished();
            }

            return obj;
        }
    }

    /** 
     * Creates and returns a read-only DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     */
    protected DataObject readOnlyObjectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {
        ObjectId anId = objEntity.objectIdFromSnapshot(dataRow);

        // this will create a HOLLOW object if it is not registered yet
        DataObject obj = registeredObject(anId);

        if (refresh || obj.getPersistenceState() == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(objEntity, obj, dataRow);

            // notify object that it was fetched
            obj.fetchFinished();
        }

        return obj;
    }

    /** 
     * Return a snapshot of all object persistent field values as of last
     * commit or fetch operation.
     *
     * @return a map of object values with DbAttribute names as keys 
     * corresponding to the latest value read from or committed to the database. */
    public Map getCommittedSnapshot(DataObject dataObject) {
        return objectStore.getSnapshot(dataObject.getObjectId());
    }

    /** 
     * Instantiates new object and registers it with itself. Object class
     * is determined from ObjEntity. Object class must have a default constructor. 
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        String objClassName = lookupEntity(objEntityName).getClassName();
        DataObject dobj = null;
        try {
            dobj = newDataObject(objClassName);
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObject(dobj, objEntityName);
        return dobj;
    }

    /** Registers new object (that is not persistent yet) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     * @param objEntityName a name of the ObjEntity in the map used to get 
     *  persistence information for this object.
     */
    public void registerNewObject(DataObject dataObject, String objEntityName) {
        // set "to many" relationship arrays

        TempObjectId tempId = new TempObjectId(objEntityName);
        dataObject.setObjectId(tempId);

        ObjEntity ent = lookupEntity(objEntityName);
        Iterator it = ent.getRelationshipList().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.isToMany()) {
                ToManyList relList = new ToManyList(relDataSource, tempId, rel.getName());
                dataObject.writePropertyDirectly(rel.getName(), relList);
            }
        }

        dataObject.setPersistenceState(PersistenceState.NEW);
        objectStore.addObject(dataObject);
        dataObject.setDataContext(this);
    }

    /**
     * Unregisters a DataObject from the context.
     * This would remove object from the internal cache,
     * remove its snapshot, unset object's DataContext and ObjectId
     * and change its state to TRANSIENT.
     */
    public void unregisterObject(DataObject dataObj) {
        // we don't care about objects that are not ours    		
        if (dataObj.getDataContext() != this) {
            return;
        }

        ObjectId oid = dataObj.getObjectId();
        objectStore.removeObject(oid);

        dataObj.setDataContext(null);
        dataObj.setObjectId(null);
        dataObj.setPersistenceState(PersistenceState.TRANSIENT);
    }

    /**
     * "Invalidates" a DataObject, changing it to a HOLLOW state.
     * This would remove object's snapshot
     * and change its state to HOLLOW. 
     * On the next access to this object, it will be refeched.
     */
    public void invalidateObject(DataObject dataObj) {
        // we don't care about objects that are not ours    
        // we don't care about uncomitted objects		
        if (dataObj.getDataContext() != this
            || dataObj.getPersistenceState() == PersistenceState.NEW) {
            return;
        }
        objectStore.removeSnapshot(dataObj.getObjectId());
        dataObj.setPersistenceState(PersistenceState.HOLLOW);
    }

    /** 
     * Notifies data context that a registered object need to be deleted on
     * next commit.
     *
     * @param deleteObject data object that we want to delete.
     */
    public void deleteObject(DataObject deleteObject) {
        deleteObject.setPersistenceState(PersistenceState.DELETED);
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
        SelectQuery sel = QueryHelper.selectObjectForId(oid);
        List results = this.performQuery(sel);
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

    /** Check what objects have changed in the context. Generate appropriate
     *  insert, update and delete queries to commit their state to the database. */
    public void commitChanges() throws CayenneRuntimeException {
        commitChanges((Level) null);
    }

    /** 
     * Commits changes of the object graph to the database.
     * Checks what objects have changed in the context. 
     * Generates appropriate insert, update and delete queries to 
     * commit their state to the database. 
     * 
     * @param logLevel if logLevel is higher or equals to the level 
     * set for QueryLogger, statements execution will be logged. 
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {
        ArrayList queryList = new ArrayList();
        ArrayList rawUpdObjects = new ArrayList();
        ArrayList updObjects = new ArrayList();
        ArrayList delObjects = new ArrayList();
        ArrayList insObjects = new ArrayList();
        HashMap updatedIds = new HashMap();

        synchronized (objectStore) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject nextObject = (DataObject) it.next();
                int objectState = nextObject.getPersistenceState();

                // 1. deal with inserts
                if (objectState == PersistenceState.NEW) {
                    filterReadOnly(nextObject);
                    insObjects.add(nextObject);
                }
                // 2. deal with deletes
                else if (objectState == PersistenceState.DELETED) {
                    filterReadOnly(nextObject);
                    queryList.add(QueryHelper.deleteQuery(nextObject));
                    delObjects.add(nextObject);
                }
                // 3. deal with updates
                else if (objectState == PersistenceState.MODIFIED) {
                    filterReadOnly(nextObject);
                    rawUpdObjects.add(nextObject);
                }
            }
        }

        // prepare inserts (create id's, build queries) 
        if (insObjects.size() > 0) {
            // create permanent id's
            createPermIds(insObjects);

            // create insert queries
            Iterator insIt = insObjects.iterator();
            while (insIt.hasNext()) {
                DataObject nextObject = (DataObject) insIt.next();
                queryList.add(
                    QueryHelper.insertQuery(
                        takeObjectSnapshot(nextObject),
                        nextObject.getObjectId()));
            }
        }

        // prepare updates (filter "fake" updates, update id's, build queries)
        if (rawUpdObjects.size() > 0) {
            Iterator updIt = rawUpdObjects.iterator();
            while (updIt.hasNext()) {
                DataObject nextObject = (DataObject) updIt.next();
                UpdateQuery updateQuery = QueryHelper.updateQuery(nextObject);
                if (updateQuery != null) {
                    queryList.add(updateQuery);
                    updObjects.add(nextObject);

                    ObjectId updId = updatedId(nextObject.getObjectId(), updateQuery);
                    if (updId != null) {
                        updatedIds.put(nextObject.getObjectId(), updId);
                    }
                } else {
                    // object was not really modified,
                    // put this object back in unmodified state right away
                    nextObject.setPersistenceState(PersistenceState.COMMITTED);
                }
            }
        }

        if (queryList.size() > 0) {
            ContextCommitObserver result =
                new ContextCommitObserver(logLevel, this, insObjects, updObjects, delObjects);
            parent.performQueries(queryList, result);
            if (!result.isTransactionCommitted())
                throw new CayenneRuntimeException("Error committing transaction.");
            else if (result.isTransactionRolledback())
                throw new CayenneRuntimeException("Transaction was rolledback.");

            // re-register objects whose id's where updated
            Iterator idIt = updatedIds.keySet().iterator();
            while (idIt.hasNext()) {
                ObjectId oldId = (ObjectId) idIt.next();
                ObjectId newId = (ObjectId) updatedIds.get(oldId);

                DataObject obj = objectStore.changeObjectKey(oldId, newId);
                if (obj != null) {
                    obj.setObjectId(newId);
                }
            }
        }
    }

    /**
     * Throws an exception if <code>dataObj</code> parameter is
     * mapped to a "read-only" entity.
     */
    private void filterReadOnly(DataObject dataObj) throws CayenneRuntimeException {
        String name = dataObj.getObjectId().getObjEntityName();
        if (lookupEntity(name).isReadOnly()) {
            throw new CayenneRuntimeException(
                "Attempt to commit a read-only object, " + name + ".");
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

        // Fetch either DataObjects or data rows.
        SelectObserver observer =
            (query.isFetchingDataRows())
                ? new SelectObserver(query.getLoggingLevel())
                : new ContextSelectObserver(this, query.getLoggingLevel());

        performQuery((Query) query, observer);
        return observer.getResults((Query) query);
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
        performQuery((Query) query, observer);
        return observer.getResultIterator();
    }

    /** Delegates node lookup to parent QueryEngine. */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
        return parent.dataNodeForObjEntity(objEntity);
    }

    /** 
     * Delegates queries execution to parent QueryEngine. If there are select
     * queries that require prefetching relationships, will create additional
     * queries to perform necessary prefetching. 
     */
    public void performQueries(List queries, OperationObserver resultConsumer) {

        // find queries that require prefetching
        List prefetch = new ArrayList();

        // if we expect iterated queries, ignore prefetching
        if (!resultConsumer.isIteratedResult()) {
            Iterator it = queries.iterator();
            while (it.hasNext()) {
                Object q = it.next();
                if (q instanceof SelectQuery) {
                    SelectQuery sel = (SelectQuery) q;
                    List prefetchRels = sel.getPrefetchList();
                    if (prefetchRels != null && prefetchRels.size() > 0) {
                        Iterator prIt = prefetchRels.iterator();
                        while (prIt.hasNext()) {
                            prefetch.add(
                                QueryHelper.selectPrefetchPath(
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
        } else {
            prefetch.addAll(0, queries);
            finalQueries = prefetch;
        }

        parent.performQueries(finalQueries, resultConsumer);
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships
     * for a list of DataObjects in the most optimized way (preferrably in 
     * a single query per relationship).
     * 
     * <p><i>Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed soon.</i></p>
     */
    public void prefetchRelationships(SelectQuery query, List objects) {
        List prefetches = query.getPrefetchList();

        if (objects == null
            || prefetches == null
            || objects.size() == 0
            || prefetches.size() == 0) {
            return;
        }

        int prefetchSize = prefetches.size();
        int objectsSize = objects.size();
        ArrayList queries = new ArrayList(prefetchSize);
        ObjEntity oe = lookupEntity(query.getObjEntityName());

        for (int i = 0; i < prefetchSize; i++) {
            String prefetchKey = (String) prefetches.get(i);
            if (prefetchKey.indexOf(Entity.PATH_SEPARATOR) >= 0) {
                throw new CayenneRuntimeException(
                    "Only one-step relationships are "
                        + "supported at the moment, this will be fixed soon. Unsupported path : "
                        + prefetchKey);
            }

            ArrayList needPrefetch = new ArrayList();
            for (int j = 0; j < objectsSize; j++) {
                CayenneDataObject obj = (CayenneDataObject) objects.get(j);
                Object dest = obj.readNestedProperty(prefetchKey);

                if (dest == null) {
                    continue;
                } else if (dest instanceof DataObject) {
                    DataObject destDO = (DataObject) dest;
                    if (destDO.getPersistenceState() == PersistenceState.HOLLOW) {
                        needPrefetch.add(destDO);
                    }
                } else {
                    throw new CayenneRuntimeException(
                        "Invalid/unsupported prefetch key '"
                            + prefetchKey
                            + "'. Resulting object must be a DataObject, instead it was "
                            + dest.getClass().getName());
                }
            }

            if (needPrefetch.size() > 0) {

                ObjRelationship r = (ObjRelationship) oe.getRelationship(prefetchKey);
                if (r == null) {
                    throw new CayenneRuntimeException(
                        "Invalid prefetch key '"
                            + prefetchKey
                            + "'. No relationship found with this name in "
                            + oe.getName());
                }

                ObjRelationship rev = r.getReverseRelationship();

                Expression inExp =
                    ExpressionFactory.binaryPathExp(
                        Expression.IN,
                        rev.getName(),
                        needPrefetch);
                queries.add(new SelectQuery(r.getTargetEntity().getName(), inExp));
            }
        }

        if (queries.size() > 0) {
            this.performQueries(
                queries,
                new ContextSelectObserver(this, query.getLoggingLevel()));
        }
    }

    /** Delegates query execution to parent QueryEngine. */
    public void performQuery(Query query, OperationObserver resultConsumer) {
        ArrayList qWrapper = new ArrayList(1);
        qWrapper.add(query);
        this.performQueries(qWrapper, resultConsumer);
    }

    /** Delegates entity name resolution to parent QueryEngine. */
    public ObjEntity lookupEntity(String objEntityName) {
        return parent.lookupEntity(objEntityName);
    }

    /**
     * Returns ObjectId if id needs to be updated 
     * after UpdateQuery is committed,
     * or null, if current id is good enough. 
     */
    private ObjectId updatedId(ObjectId id, UpdateQuery upd) {
        Map idMap = id.getIdSnapshot();

        Map updAttrs = upd.getUpdAttributes();
        Iterator it = updAttrs.keySet().iterator();

        HashMap newIdMap = null;
        while (it.hasNext()) {
            Object key = it.next();
            if (!idMap.containsKey(key))
                continue;

            if (newIdMap == null)
                newIdMap = new HashMap(idMap);

            newIdMap.put(key, updAttrs.get(key));
        }

        return (newIdMap != null) ? new ObjectId(id.getObjEntityName(), newIdMap) : null;
    }

    /** 
     *  Populates the <code>map</code> with ObjectId values from master objects 
     *  related to this object. 
     */
    private void appendPkFromMasterRelationships(Map map, DataObject dataObject) {
        ObjEntity objEntity =
            parent.lookupEntity(dataObject.getObjectId().getObjEntityName());
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
                    .append(dataObject.getObjectId().getObjEntityName())
                    .append(", target obj: ")
                    .append(targetDo.getObjectId().getObjEntityName());
                throw new CayenneRuntimeException(msg.toString());
            }

            map.putAll(dbRel.srcFkSnapshotWithTargetSnapshot(idMap));
        }
    }

    /** Creates permanent ObjectId's for the list of new objects. */
    private void createPermIds(List objects) {
        OperationSorter.sortObjectsInInsertOrder(objects);
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            createPermId((DataObject) it.next());
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
        TempObjectId tempId = (TempObjectId) anObject.getObjectId();
        ObjEntity objEntity = parent.lookupEntity(tempId.getObjEntityName());
        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode aNode = parent.dataNodeForObjEntity(objEntity);

        HashMap idMap = new HashMap();
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
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK", ex);
            }
        }

        ObjectId permId = new ObjectId(objEntity.getName(), idMap);

        // note that object registration did not changed (new id is not attached to context, only to temp. oid)
        tempId.setPermId(permId);
        return permId;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it.  Then when deser happens, we can get back the DataDomain by name, 
        // from the shared configuration (which will either load it if need be, or return 
        // an existing one.
        out.defaultWriteObject();
        if (this.parent instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.parent;
            out.writeObject(domain.getName());
        } else {
            out.writeObject(this.parent);
            //Hope that whatever this.parent is, that it is Serializable
        }

        //For writing, just write the objects.  They will be serialized possibly
        // as just objectIds... it's up to the object itself.  Reading will do magic
        out.writeObject(objectStore);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        boolean failed = false;
        in.defaultReadObject();
        Object value = in.readObject();
        if (value instanceof QueryEngine) {
            //Must be a real QueryEngine object - use it
            this.parent = (QueryEngine) value;
        } else if (value instanceof String) {
            //Must be the name of a DataDomain - use it
            this.parent = Configuration.getSharedConfig().getDomain((String) value);
            if (this.parent == null) {
                failed = true;
            }
        } else {
            failed = true;
        }
        if (failed) {
            throw new IOException(
                "Parent attribute of DataContext was neither a QueryEngine nor "
                    + "the name of a valid DataDomain:"
                    + value);
        }

        //CayenneDataObjects have a transient datacontext
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
}