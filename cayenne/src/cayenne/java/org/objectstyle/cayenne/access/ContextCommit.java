/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.ObjectStore.FlattenedRelationshipInfo;
import org.objectstyle.cayenne.access.util.BatchQueryUtils;
import org.objectstyle.cayenne.access.util.ContextCommitObserver;
import org.objectstyle.cayenne.access.util.DataNodeCommitHelper;
import org.objectstyle.cayenne.access.util.DependencySorter;
import org.objectstyle.cayenne.access.util.PrimaryKeyHelper;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateBatchQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * ContextCommit implements DataContext commit logic. DataContext internally
 * delegates commit operations to an instance of ContextCommit. ContextCommit
 * resolves primary key dependencies, referential integrity dependencies
 * (including multi-reflexive entities), generates primary keys, creates
 * batches for massive data modifications, assigns operations to data nodes. It
 * indirectly relies on graph algorithms provided by ASHWOOD library.
 * 
 * @author Andriy Shapochka
 */

class ContextCommit {
    private static Logger logObj = Logger.getLogger(ContextCommit.class);

    private DataContext context;
    private Level logLevel;
    private Map newObjectsByObjEntity;
    private Map objectsToDeleteByObjEntity;
    private Map objectsToUpdateByObjEntity;
    private List objEntitiesToInsert;
    private List objEntitiesToDelete;
    private List objEntitiesToUpdate;
    private List nodeHelpers;
    private List insObjects; //event support
    private List delObjects; //event support
    private List updObjects; //event support

    ContextCommit(DataContext contextToCommit) {
        context = contextToCommit;
    }

    /**
     * Commits changes in the enclosed DataContext.
     */
    void commit(Level logLevel) throws CayenneException {
        if (logLevel == null) {
            logLevel = QueryLogger.DEFAULT_LOG_LEVEL;
        }

        this.logLevel = logLevel;

        // synchronize on both object store and underlying DataRowStore
        synchronized (context.getObjectStore()) {
            synchronized (context.getObjectStore().getDataRowCache()) {

                categorizeObjects();
                createPrimaryKeys();
                categorizeFlattenedInsertsAndCreateBatches();
                categorizeFlattenedDeletesAndCreateBatches();

                insObjects = new ArrayList();
                delObjects = new ArrayList();
                updObjects = new ArrayList();

                for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
                    DataNodeCommitHelper nodeHelper = (DataNodeCommitHelper) i.next();
                    prepareInsertQueries(nodeHelper);
                    prepareFlattenedQueries(
                        nodeHelper,
                        nodeHelper.getFlattenedInsertQueries());

                    prepareUpdateQueries(nodeHelper);

                    //Side effect - fills updObjects
                    prepareFlattenedQueries(
                        nodeHelper,
                        nodeHelper.getFlattenedDeleteQueries());

                    prepareDeleteQueries(nodeHelper);
                }

                ContextCommitObserver observer =
                    new ContextCommitObserver(
                        logLevel,
                        context,
                        insObjects,
                        updObjects,
                        delObjects);

                if (context.isTransactionEventsEnabled()) {
                    observer.registerForDataContextEvents();
                }

                try {
                    context.fireWillCommit();

                    Transaction transaction =
                        context.getParentDataDomain().createTransaction();
                    transaction.begin();

                    try {
                        Iterator i = nodeHelpers.iterator();
                        while (i.hasNext()) {
                            DataNodeCommitHelper nodeHelper =
                                (DataNodeCommitHelper) i.next();
                            List queries = nodeHelper.getQueries();

                            if (queries.size() > 0) {
                                // note: observer throws on error
                                nodeHelper.getNode().performQueries(
                                    queries,
                                    observer,
                                    transaction);
                            }
                        }

                        // commit
                        transaction.commit();
                    }
                    catch (Throwable th) {
                        try {
                            // rollback
                            transaction.rollback();
                        }
                        catch (Throwable rollbackTh) {
                            // ignoring...
                        }

                        context.fireTransactionRolledback();
                        throw new CayenneException(
                            "Transaction was rolledback.",
                            Util.unwindException(th));
                    }

                    context.getObjectStore().objectsCommitted();
                    context.fireTransactionCommitted();
                }
                finally {
                    if (context.isTransactionEventsEnabled()) {
                        observer.unregisterFromDataContextEvents();
                    }
                }
            }
        }
    }

    private void prepareInsertQueries(DataNodeCommitHelper commitHelper)
        throws CayenneException {

        List entities = commitHelper.getObjEntitiesForInsert();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        DependencySorter sorter = commitHelper.getNode().getDependencySorter();
        sorter.sortDbEntities(dbEntities, false);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            InsertBatchQuery batch = new InsertBatchQuery(dbEntity, 27);
            batch.setLoggingLevel(logLevel);
            if (logObj.isDebugEnabled()) {
                logObj.debug(
                    "Creating InsertBatchQuery for DbEntity " + dbEntity.getName());
            }
            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();
                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);
                DbRelationship masterDependentDbRel =
                    (isMasterDbEntity
                        ? null
                        : findMasterToDependentDbRelationship(
                            entity.getDbEntity(),
                            dbEntity));

                List objects = (List) newObjectsByObjEntity.get(entity.getClassName());

                // throw an exception - an attempt to modify read-only entity
                if (entity.isReadOnly() && objects.size() > 0) {
                    throw attemptToCommitReadOnlyEntity(
                        objects.get(0).getClass(),
                        entity);
                }

                if (isMasterDbEntity)
                    sorter.sortObjectsForEntity(entity, objects, false);

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    //                    batch.add(context.takeObjectSnapshot(o));
                    Map snapshot =
                        BatchQueryUtils.buildSnapshotForInsert(
                            entity,
                            o,
                            masterDependentDbRel);
                    batch.add(snapshot);
                }

                if (isMasterDbEntity)
                    insObjects.addAll(objects);
            }
            commitHelper.getQueries().add(batch);
        }
    }

    private void prepareDeleteQueries(DataNodeCommitHelper commitHelper)
        throws CayenneException {

        List entities = commitHelper.getObjEntitiesForDelete();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        DependencySorter sorter = commitHelper.getNode().getDependencySorter();
        //        sorter.sortObjEntities(entities, true);
        sorter.sortDbEntities(dbEntities, true);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            DeleteBatchQuery batch = new DeleteBatchQuery(dbEntity, 27);
            batch.setLoggingLevel(logLevel);

            if (logObj.isDebugEnabled())
                logObj.debug(
                    "Creating DeleteBatchQuery for DbEntity " + dbEntity.getName());

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);
                DbRelationship masterDependentDbRel =
                    (isMasterDbEntity
                        ? null
                        : findMasterToDependentDbRelationship(
                            entity.getDbEntity(),
                            dbEntity));

                List objects =
                    (List) objectsToDeleteByObjEntity.get(entity.getClassName());

                // throw an exception - an attempt to delete read-only entity
                if (entity.isReadOnly() && objects.size() > 0) {
                    throw attemptToCommitReadOnlyEntity(
                        objects.get(0).getClass(),
                        entity);
                }

                if (isMasterDbEntity) {
                    sorter.sortObjectsForEntity(entity, objects, true);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    // check if object was modified from underneath and consult the delegate
                    // if this is the case...
                    // checkConcurrentModifications(o);

                    Map id = o.getObjectId().getIdSnapshot();
                    if (id != null && !id.isEmpty()) {
                        if (!isMasterDbEntity && masterDependentDbRel != null)
                            id = masterDependentDbRel.targetPkSnapshotWithSrcSnapshot(id);
                        batch.add(id);
                    }
                }

                if (isMasterDbEntity)
                    delObjects.addAll(objects);
            }
            commitHelper.getQueries().add(batch);
        }
    }

    private List createOptimisticLockingIdSnapshotKeys(ObjEntity objEntity) {
        List newLockingAttributeList = new ArrayList();

        Iterator dbAttributeIterator = objEntity.getDbEntity().getAttributes().iterator();
        while (dbAttributeIterator.hasNext()) {
            DbAttribute dbAttr = (DbAttribute) dbAttributeIterator.next();

            // always lock on primary key(s)
            if (dbAttr.isPrimaryKey()) {
                newLockingAttributeList.add(dbAttr);
                continue;
            }

            ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);

            if (objAttr == null)
                continue;

            // Per-objAtttribute optimistic locking conditional
            if (!objAttr.isUsedForLocking())
                continue;

            newLockingAttributeList.add(dbAttr);
        }

        Iterator dbRelationshipIterator =
            objEntity.getDbEntity().getRelationships().iterator();
        while (dbRelationshipIterator.hasNext()) {
            DbRelationship dbRel = (DbRelationship) dbRelationshipIterator.next();

            ObjRelationship objRel = objEntity.getRelationshipForDbRelationship(dbRel);

            if (objRel == null)
                continue;

            // Per-objAtttribute optimistic locking conditional
            if (!objRel.isUsedForLocking())
                continue;

            Iterator joinsIterator = dbRel.getJoins().iterator();
            while (joinsIterator.hasNext()) {
                DbAttributePair dbAttrPair = (DbAttributePair) joinsIterator.next();
                DbAttribute dbAttr = dbAttrPair.getSource();
                newLockingAttributeList.add(dbAttr);
            }
        }

        return newLockingAttributeList;
    }

    private Map createOptimisticLockingIdSnapshot(
        ObjEntity objEntity,
        DataObject dataObject,
        List dbAttrList,
        Map srcIdSnapshotMap)
        throws CayenneException {
            
        // Unclear to me if srcIdSnapshotMap is necessary as a starting point, but it seems safest to leave it
        Map newLockingAttributeMap = new HashMap(srcIdSnapshotMap);

        // Use this to insure we're not fetching it from the db.
        DataRow commitedSnapshot =
            dataObject.getDataContext().getObjectStore().getRetainedSnapshot(
                dataObject.getObjectId());

        if (null == commitedSnapshot) {
            throw new CayenneException(
                "getRetainedSnapshot() is null for " + dataObject.getObjectId());
        }

        Iterator dbAttributeIterator = dbAttrList.iterator();
        while (dbAttributeIterator.hasNext()) {
            DbAttribute dbAttr = (DbAttribute) dbAttributeIterator.next();

            newLockingAttributeMap.put(
                dbAttr.getName(),
                commitedSnapshot.get(dbAttr.getName()));
        }

        return newLockingAttributeMap;
    }

    private void prepareUpdateQueries(DataNodeCommitHelper commitHelper)
        throws CayenneException {
        List entities = commitHelper.getObjEntitiesForUpdate();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);
        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            Map batches = new LinkedMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                // Per-objEntity optimistic locking conditional
                boolean shouldUseOptimisticLocking =
                    (ObjEntity.LOCK_TYPE_OPTIMISTIC == entity.getLockType());
                List lockingIdSnapshotKeys = null;
                if (shouldUseOptimisticLocking) {
                    lockingIdSnapshotKeys = createOptimisticLockingIdSnapshotKeys(entity);
                }

                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);

                DbRelationship masterDependentDbRel =
                    (isMasterDbEntity)
                        ? null
                        : findMasterToDependentDbRelationship(
                            entity.getDbEntity(),
                            dbEntity);
                List objects =
                    (List) objectsToUpdateByObjEntity.get(entity.getClassName());

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    // check if object was modified from underneath and consult the delegate
                    // if this is the case...
                    // checkConcurrentModifications(o);

                    Map snapshot =
                        BatchQueryUtils.buildSnapshotForUpdate(
                            entity,
                            o,
                            masterDependentDbRel);

                    // check whether MODIFIED object has real db-level
                    // modifications
                    if (snapshot.isEmpty()) {
                        o.setPersistenceState(PersistenceState.COMMITTED);
                        continue;
                    }

                    // after we filtered out "fake" modifications, check if an
                    // attempt is made to modify a read only entity
                    if (entity.isReadOnly()) {
                        throw attemptToCommitReadOnlyEntity(o.getClass(), entity);
                    }

                    // Need to wrap snapshot keys to a TreeSet to ensure
                    // automatic ordering so that we can build a valid hashcode
                    TreeSet updatedAttributeNames = new TreeSet(snapshot.keySet());
                    Integer hashCode = new Integer(Util.hashCode(updatedAttributeNames));

                    UpdateBatchQuery batch = (UpdateBatchQuery) batches.get(hashCode);
                    if (batch == null) {
                        batch =
                            new UpdateBatchQuery(
                                dbEntity,
                                new ArrayList(snapshot.keySet()),
                                10);
                        batch.setLoggingLevel(logLevel);
                        batches.put(hashCode, batch);
                    }
                    Map idSnapshot = o.getObjectId().getIdSnapshot();
                    if (!isMasterDbEntity && masterDependentDbRel != null)
                        idSnapshot =
                            masterDependentDbRel.targetPkSnapshotWithSrcSnapshot(
                                idSnapshot);

                    if (shouldUseOptimisticLocking) {
                        Map lockingIdSnapshot =
                            createOptimisticLockingIdSnapshot(
                                entity,
                                o,
                                lockingIdSnapshotKeys,
                                idSnapshot);
                        batch.setIdDbAttributes(lockingIdSnapshotKeys);
                        batch.setUsingOptimisticLocking(true);
                        batch.add(lockingIdSnapshot, snapshot);
                    }
                    else {
                        batch.add(idSnapshot, snapshot);
                    }

                    if (isMasterDbEntity) {
                        ObjectId updId =
                            updatedId(
                                o.getObjectId().getObjClass(),
                                idSnapshot,
                                snapshot);
                        if (updId != null) {
                            o.getObjectId().setReplacementId(updId);
                        }

                        updObjects.add(o);
                    }
                }
            }
            commitHelper.getQueries().addAll(batches.values());
        }
    }

    private void createPrimaryKeys() throws CayenneException {

        // TODO: casting here may not work in the future if
        // DataContexts are allowed to have parents other than DataDomain
        DataDomain domain = (DataDomain) context.getParent();
        PrimaryKeyHelper pkHelper = domain.getPrimaryKeyHelper();

        Collections.sort(objEntitiesToInsert, pkHelper.getObjEntityComparator());
        for (Iterator i = objEntitiesToInsert.iterator(); i.hasNext();) {
            ObjEntity currentEntity = (ObjEntity) i.next();
            List dataObjects =
                (List) newObjectsByObjEntity.get(currentEntity.getClassName());
            pkHelper.createPermIdsForObjEntity(currentEntity, dataObjects);
        }
    }

    /**
     * Organizes committed objects by node, performs sorting operations.
     */
    private void categorizeObjects() throws CayenneException {
        this.nodeHelpers = new ArrayList();

        Iterator it = context.getObjectStore().getObjectIterator();
        newObjectsByObjEntity = new HashMap();
        objectsToDeleteByObjEntity = new HashMap();
        objectsToUpdateByObjEntity = new HashMap();
        objEntitiesToInsert = new ArrayList();
        objEntitiesToDelete = new ArrayList();
        objEntitiesToUpdate = new ArrayList();

        while (it.hasNext()) {
            DataObject nextObject = (DataObject) it.next();
            int objectState = nextObject.getPersistenceState();
            switch (objectState) {
                case PersistenceState.NEW :
                    objectToInsert(nextObject);
                    break;
                case PersistenceState.DELETED :
                    objectToDelete(nextObject);
                    break;
                case PersistenceState.MODIFIED :
                    objectToUpdate(nextObject);
                    break;
            }
        }
    }

    private void objectToInsert(DataObject o) throws CayenneException {
        classifyByEntityAndNode(
            o,
            newObjectsByObjEntity,
            objEntitiesToInsert,
            DataNodeCommitHelper.INSERT);
    }

    private void objectToDelete(DataObject o) throws CayenneException {
        classifyByEntityAndNode(
            o,
            objectsToDeleteByObjEntity,
            objEntitiesToDelete,
            DataNodeCommitHelper.DELETE);
    }

    private void objectToUpdate(DataObject o) throws CayenneException {
        classifyByEntityAndNode(
            o,
            objectsToUpdateByObjEntity,
            objEntitiesToUpdate,
            DataNodeCommitHelper.UPDATE);
    }

    private RuntimeException attemptToCommitReadOnlyEntity(
        Class objectClass,
        ObjEntity entity) {
        String className = (objectClass != null) ? objectClass.getName() : "<null>";
        StringBuffer message = new StringBuffer();
        message.append("Class '").append(className).append(
            "' maps to a read-only entity");

        if (entity != null) {
            message.append(" '").append(entity.getName()).append("'");
        }

        message.append(". Can't commit changes.");
        return new CayenneRuntimeException(message.toString());
    }

    /**
     * Performs classification of a DataObject for the DML operation. Throws
     * CayenneRuntimeException if an object can't be classified.
     */
    private void classifyByEntityAndNode(
        DataObject o,
        Map objectsByObjEntity,
        List objEntities,
        int operationType) {

        Class objEntityClass = o.getObjectId().getObjClass();
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(objEntityClass);
        Collection objectsForObjEntity =
            (Collection) objectsByObjEntity.get(objEntityClass.getName());
        if (objectsForObjEntity == null) {
            objEntities.add(entity);
            DataNode responsibleNode = context.lookupDataNode(entity.getDataMap());

            DataNodeCommitHelper commitHelper =
                DataNodeCommitHelper.getHelperForNode(nodeHelpers, responsibleNode);

            commitHelper.addToEntityList(entity, operationType);
            objectsForObjEntity = new ArrayList();
            objectsByObjEntity.put(objEntityClass.getName(), objectsForObjEntity);
        }
        objectsForObjEntity.add(o);
    }

    private void categorizeFlattenedInsertsAndCreateBatches() {
        Iterator i = context.getObjectStore().getFlattenedInserts().iterator();

        while (i.hasNext()) {
            FlattenedRelationshipInfo info = (FlattenedRelationshipInfo) i.next();

            DataObject source = info.getSource();
            if (source.getPersistenceState() == PersistenceState.DELETED) {
                continue;
            }

            Map sourceId = source.getObjectId().getIdSnapshot();
            ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);

            DataNode responsibleNode = context.lookupDataNode(sourceEntity.getDataMap());
            DataNodeCommitHelper commitHelper =
                DataNodeCommitHelper.getHelperForNode(nodeHelpers, responsibleNode);
            Map batchesByDbEntity = commitHelper.getFlattenedInsertQueries();

            ObjRelationship flattenedRel = info.getBaseRelationship();
            List relList = flattenedRel.getDbRelationships();
            DbRelationship firstDbRel = (DbRelationship) relList.get(0);
            DbRelationship secondDbRel = (DbRelationship) relList.get(1);
            DbEntity flattenedEntity = (DbEntity) firstDbRel.getTargetEntity();
            InsertBatchQuery relationInsertQuery =
                (InsertBatchQuery) batchesByDbEntity.get(flattenedEntity);

            if (relationInsertQuery == null) {
                relationInsertQuery = new InsertBatchQuery(flattenedEntity, 50);
                relationInsertQuery.setLoggingLevel(logLevel);
                if (logObj.isDebugEnabled())
                    logObj.debug(
                        "Creating InsertBatchQuery for DbEntity "
                            + flattenedEntity.getName());
                batchesByDbEntity.put(flattenedEntity, relationInsertQuery);
            }
            DataObject destination = info.getDestination();
            if (destination.getPersistenceState() == PersistenceState.DELETED)
                continue;
            Map dstId = destination.getObjectId().getIdSnapshot();
            Map flattenedSnapshot =
                BatchQueryUtils.buildFlattenedSnapshot(
                    sourceId,
                    dstId,
                    firstDbRel,
                    secondDbRel);
            relationInsertQuery.add(flattenedSnapshot);
        }
    }

    private void categorizeFlattenedDeletesAndCreateBatches() {
        Iterator i = context.getObjectStore().getFlattenedDeletes().iterator();

        while (i.hasNext()) {
            FlattenedRelationshipInfo info = (FlattenedRelationshipInfo) i.next();
            DataObject source = info.getSource();
            Map sourceId = source.getObjectId().getIdSnapshot();
            if (sourceId == null)
                continue;

            ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);
            DataNode responsibleNode = context.lookupDataNode(sourceEntity.getDataMap());
            DataNodeCommitHelper commitHelper =
                DataNodeCommitHelper.getHelperForNode(nodeHelpers, responsibleNode);
            Map batchesByDbEntity = commitHelper.getFlattenedDeleteQueries();

            ObjRelationship flattenedRel = info.getBaseRelationship();
            List relList = flattenedRel.getDbRelationships();
            DbRelationship firstDbRel = (DbRelationship) relList.get(0);
            DbRelationship secondDbRel = (DbRelationship) relList.get(1);
            DbEntity flattenedEntity = (DbEntity) firstDbRel.getTargetEntity();
            DeleteBatchQuery relationDeleteQuery =
                (DeleteBatchQuery) batchesByDbEntity.get(flattenedEntity);
            if (relationDeleteQuery == null) {
                relationDeleteQuery = new DeleteBatchQuery(flattenedEntity, 50);
                relationDeleteQuery.setLoggingLevel(logLevel);
                if (logObj.isDebugEnabled())
                    logObj.debug(
                        "Creating DeleteBatchQuery for DbEntity "
                            + flattenedEntity.getName());
                batchesByDbEntity.put(flattenedEntity, relationDeleteQuery);
            }

            DataObject destination = info.getDestination();
            Map dstId = destination.getObjectId().getIdSnapshot();
            if (dstId == null)
                continue;
            Map flattenedSnapshot =
                BatchQueryUtils.buildFlattenedSnapshot(
                    sourceId,
                    dstId,
                    firstDbRel,
                    secondDbRel);
            relationDeleteQuery.add(flattenedSnapshot);
        }
    }

    private void prepareFlattenedQueries(
        DataNodeCommitHelper commitHelper,
        Map flattenedBatches) {

        for (Iterator i = flattenedBatches.values().iterator(); i.hasNext();) {
            commitHelper.addToQueries((Query) i.next());
        }
    }

    private ObjectId updatedId(Class objEntityClass, Map idMap, Map updAttrs) {
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
        return (newIdMap != null) ? new ObjectId(objEntityClass, newIdMap) : null;
    }

    private void groupObjEntitiesBySpannedDbEntities(
        List dbEntities,
        Map objEntitiesByDbEntity,
        List objEntities) {
        for (Iterator i = objEntities.iterator(); i.hasNext();) {
            ObjEntity objEntity = (ObjEntity) i.next();
            DbEntity dbEntity = objEntity.getDbEntity();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            if (objEntitiesForDbEntity == null) {
                objEntitiesForDbEntity = new ArrayList(1);
                dbEntities.add(dbEntity);
                objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
            }
            if (!objEntitiesForDbEntity.contains(objEntity))
                objEntitiesForDbEntity.add(objEntity);
            for (Iterator j = objEntity.getAttributeMap().values().iterator();
                j.hasNext();
                ) {
                ObjAttribute objAttribute = (ObjAttribute) j.next();
                if (!objAttribute.isCompound())
                    continue;
                dbEntity = (DbEntity) objAttribute.getDbAttribute().getEntity();
                objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
                if (objEntitiesForDbEntity == null) {
                    objEntitiesForDbEntity = new ArrayList(1);
                    dbEntities.add(dbEntity);
                    objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
                }
                if (!objEntitiesForDbEntity.contains(objEntity))
                    objEntitiesForDbEntity.add(objEntity);
            }
        }
    }

    private DbRelationship findMasterToDependentDbRelationship(
        DbEntity masterDbEntity,
        DbEntity dependentDbEntity) {
        if (masterDbEntity.equals(dependentDbEntity))
            return null;
        for (Iterator i = masterDbEntity.getRelationshipMap().values().iterator();
            i.hasNext();
            ) {
            DbRelationship rel = (DbRelationship) i.next();
            if (dependentDbEntity.equals(rel.getTargetEntity()) && rel.isToDependentPK())
                return rel;
        }
        return null;
    }
}
