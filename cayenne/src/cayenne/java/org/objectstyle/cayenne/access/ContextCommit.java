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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.SequencedHashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.DataContext.FlattenedRelationshipInfo;
import org.objectstyle.cayenne.access.util.BatchQueryUtils;
import org.objectstyle.cayenne.access.util.ContextCommitObserver;
import org.objectstyle.cayenne.access.util.DataNodeCommitHelper;
import org.objectstyle.cayenne.access.util.DependencySorter;
import org.objectstyle.cayenne.access.util.PrimaryKeyHelper;
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
 * ContextCommit implements commit logic. It is used internally by
 * DataContext as a commit delegate.
 * Currently ContextCommit resolves primary key dependencies,
 * referential integrity dependencies including multi-reflexive entities,
 * generates primary keys, creates batches for massive data modifications,
 * assigns operations to data nodes. It indirectly relies on graph
 * algorithms provided by ASHWOOD.
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
    private Set writableObjEntities;
    private List objEntitiesToInsert;
    private List objEntitiesToDelete;
    private List objEntitiesToUpdate;
    private List nodeHelpers;
    private Map updatedIds;
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

        categorizeObjects();
        createPrimaryKeys();
        categorizeFlattenedInsertsAndCreateBatches();
        categorizeFlattenedDeletesAndCreateBatches();
        updatedIds = new SequencedHashMap();

        insObjects = new ArrayList();
        delObjects = new ArrayList();
        updObjects = new ArrayList();

        for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
            DataNodeCommitHelper nodeHelper = (DataNodeCommitHelper) i.next();
            prepareInsertQueries(nodeHelper);
            prepareFlattenedQueries(nodeHelper, nodeHelper.getFlattenedInsertQueries());

            prepareUpdateQueries(nodeHelper);

            //Side effect - fills updObjects
            prepareFlattenedQueries(nodeHelper, nodeHelper.getFlattenedDeleteQueries());

            prepareDeleteQueries(nodeHelper);
        }

        ContextCommitObserver observer =
            new ContextCommitObserver(logLevel, context, insObjects, updObjects, delObjects);

        if (context.isTransactionEventsEnabled()) {
            observer.registerForDataContextEvents();
        }

        try {
            context.fireWillCommit();

            for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
                DataNodeCommitHelper nodeHelper = (DataNodeCommitHelper) i.next();
                List queries = nodeHelper.getQueries();

                // Andrei: this check is needed, since if we run an empty query set,
                // commit will not be executed, and this method will blow below.
                if (queries.size() > 0) {
                    nodeHelper.getNode().performQueries(queries, observer);

                    // Andrei: should we reset observer commit status for each iteration?
                    // Also we may add real distributed transactions support by adding
                    // some kind of commit delegate that runs all queries, and only then
                    // commits...
                    if (observer.isTransactionRolledback()) {
                        context.fireTransactionRolledback();
                        throw new CayenneException("Transaction was rolledback.");
                    }
                    else if (!observer.isTransactionCommitted()) {
                        throw new CayenneException("Error committing transaction.");
                    }
                }

                postprocess(nodeHelper);
            }
            context.fireTransactionCommitted();
        }
        finally {
            if (context.isTransactionEventsEnabled()) {
                observer.unregisterFromDataContextEvents();
            }
        }
    }

    /**
     * Performs necessary changes to objects after they are committed for a
     * particular DataNode.
     */
    private void postprocess(DataNodeCommitHelper nodeHelper) {
        ObjectStore objectStore = context.getObjectStore();
        Collection entitiesForNode = nodeHelper.getObjEntitiesForInsert();

        // postprocess inserted objects in context
        for (Iterator i = entitiesForNode.iterator(); i.hasNext();) {
            ObjEntity entity = (ObjEntity) i.next();
            Collection objects =
                (Collection) newObjectsByObjEntity.get(entity.getClassName());
            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                TempObjectId tempId = (TempObjectId) o.getObjectId();
                ObjectId permId = tempId.getPermId();
                objectStore.changeObjectKey(tempId, permId);
                o.setObjectId(permId);
                Map snapshot = context.takeObjectSnapshot(o);
                objectStore.addSnapshot(permId, snapshot);
                o.setPersistenceState(PersistenceState.COMMITTED);
            }
        }

        // postprocess deleted objects in context
        entitiesForNode = nodeHelper.getObjEntitiesForDelete();
        for (Iterator i = entitiesForNode.iterator(); i.hasNext();) {
            ObjEntity entity = (ObjEntity) i.next();
            Collection objects =
                (Collection) objectsToDeleteByObjEntity.get(entity.getClassName());
            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                ObjectId anId = o.getObjectId();
                objectStore.removeObject(anId);
                o.setPersistenceState(PersistenceState.TRANSIENT);
                o.setDataContext(null);
            }
        }

        // postprocess updated objects in context
        entitiesForNode = nodeHelper.getObjEntitiesForUpdate();

        for (Iterator i = entitiesForNode.iterator(); i.hasNext();) {
            ObjEntity entity = (ObjEntity) i.next();
            Collection objects =
                (Collection) objectsToUpdateByObjEntity.get(entity.getClassName());
            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                ObjectId oldId = o.getObjectId();
                ObjectId newId = (ObjectId) updatedIds.get(oldId);
                Map snapshot = context.takeObjectSnapshot(o);
                objectStore.addSnapshot(oldId, snapshot);
                if (newId != null) {
                    objectStore.changeObjectKey(oldId, newId);
                    o.setObjectId(newId);
                }
                o.setPersistenceState(PersistenceState.COMMITTED);
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
        //        sorter.sortObjEntities(entities, false);
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

                if (isMasterDbEntity)
                    sorter.sortObjectsForEntity(entity, objects, true);

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
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
            Map batches = new SequencedHashMap();

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
                    (List) objectsToUpdateByObjEntity.get(entity.getClassName());

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    Map snapshot =
                        BatchQueryUtils.buildSnapshotForUpdate(
                            entity,
                            o,
                            masterDependentDbRel);
                    //                    Map snapshot = BatchUtils.buildSnapshotForUpdate(o);

                    if (snapshot.isEmpty()) {
                        o.setPersistenceState(PersistenceState.COMMITTED);
                        continue;
                    }

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
                    batch.add(idSnapshot, snapshot);
                    if (isMasterDbEntity) {
                        ObjectId updId =
                            updatedId(
                                o.getObjectId().getObjClass(),
                                idSnapshot,
                                snapshot);
                        if (updId != null) {
                            updatedIds.put(o.getObjectId(), updId);
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
        writableObjEntities = new HashSet();
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

    private ObjEntity classifyAsWritable(Class objEntityClass) {
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(objEntityClass);
        if (entity == null) {
            throw attemptToCommitUnmappedClass(objEntityClass);
        }
        else if (entity.isReadOnly()) {
            throw attemptToCommitReadOnlyEntity(
                objEntityClass,
                context.getEntityResolver().lookupObjEntity(objEntityClass));
        }
        else {
            writableObjEntities.add(objEntityClass);
            return entity;
        }
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

    private RuntimeException attemptToCommitUnmappedClass(Class objectClass) {
        String className = (objectClass != null) ? objectClass.getName() : "<null>";
        StringBuffer message = new StringBuffer();
        message.append("Class '").append(className).append(
            "' does not map to an ObjEntity and is therefore not persistent. Can't commit changes.");

        return new CayenneRuntimeException(message.toString());
    }

    /**
     * Performs classification of a DataObject for the DML operation.
     * Throws CayenneRuntimeException if an object can't be classified. 
     */
    private void classifyByEntityAndNode(
        DataObject o,
        Map objectsByObjEntity,
        List objEntities,
        int operationType) {

        Class objEntityClass = o.getObjectId().getObjClass();
        ObjEntity entity = null;

        if (!writableObjEntities.contains(objEntityClass)) {
            entity = classifyAsWritable(objEntityClass);
        }
        else {
            entity = context.getEntityResolver().lookupObjEntity(objEntityClass);
        }

        Collection objectsForObjEntity =
            (Collection) objectsByObjEntity.get(objEntityClass.getName());
        if (objectsForObjEntity == null) {
            objEntities.add(entity);
            DataNode responsibleNode = context.dataNodeForObjEntity(entity);

            DataNodeCommitHelper commitHelper =
                DataNodeCommitHelper.getHelperForNode(nodeHelpers, responsibleNode);

            commitHelper.addToEntityList(entity, operationType);
            objectsForObjEntity = new ArrayList();
            objectsByObjEntity.put(objEntityClass.getName(), objectsForObjEntity);
        }
        objectsForObjEntity.add(o);
    }

    private void categorizeFlattenedInsertsAndCreateBatches() {
        Iterator i = context.getFlattenedInserts().iterator();

        while (i.hasNext()) {
            DataContext.FlattenedRelationshipInfo info =
                (FlattenedRelationshipInfo) i.next();

            DataObject source = info.getSource();
            if (source.getPersistenceState() == PersistenceState.DELETED) {
                continue;
            }

            Map sourceId = source.getObjectId().getIdSnapshot();
            ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);

            DataNode responsibleNode = context.dataNodeForObjEntity(sourceEntity);
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
        Iterator i = context.getFlattenedDeletes().iterator();

        while (i.hasNext()) {
            DataContext.FlattenedRelationshipInfo info =
                (FlattenedRelationshipInfo) i.next();
            DataObject source = info.getSource();
            Map sourceId = source.getObjectId().getIdSnapshot();
            if (sourceId == null)
                continue;

            ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);
            DataNode responsibleNode = context.dataNodeForObjEntity(sourceEntity);
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
