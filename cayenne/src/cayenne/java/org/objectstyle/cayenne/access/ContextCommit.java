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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.SequencedHashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.DataContext.FlattenedRelationshipInfo;
import org.objectstyle.cayenne.access.util.ContextCommitObserver;
import org.objectstyle.cayenne.access.util.DataNodeCommitHelper;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.BatchUtils;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.UpdateBatchQuery;
import org.objectstyle.cayenne.query.UpdateQuery;

/**
 * ContextCommit implements commit logic. It is used internally by
 * DataContext as a commit delegate.
 * Currently ContextCommit resolves primary key dependencies,
 * referential integrity dependencies including multi-reflexive entities,
 * generates primary keys, creates batches for massive data modifications,
 * assignes operations to data nodes. It indirectly relies on graph
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
    private Set readOnlyObjEntities;
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
        this.logLevel = logLevel;

        categorizeObjects();
        createPrimaryKeys();
        Map flattenedInsertBatches =
            categorizeFlattenedInsertsAndCreateBatches();
        Map flattenedDeleteBatches =
            categorizeFlattenedDeletesAndCreateBatches();
        updatedIds = new SequencedHashMap();

        insObjects = new ArrayList();
        delObjects = new ArrayList();
        updObjects = new ArrayList();

        for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
            DataNodeCommitHelper nodeHelper = (DataNodeCommitHelper) i.next();
            DataNode node = nodeHelper.getNode();
            List queries = nodeHelper.getQueries();

            prepareInsertQueries(nodeHelper);
            //Side effect - fills insObjects
            prepareFlattenedQueries(node, queries, flattenedInsertBatches);
            prepareUpdateQueries(nodeHelper);
            //Side effect - fills updObjects
            prepareFlattenedQueries(node, queries, flattenedDeleteBatches);
            prepareDeleteQueries(nodeHelper);
        }

        CommitObserver observer =
            new CommitObserver(
                logLevel,
                context,
                insObjects,
                updObjects,
                delObjects);
        if (context.isTransactionEventsEnabled())
            observer.registerForDataContextEvents();
        try {
            context.fireWillCommit();
            for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
                DataNodeCommitHelper nodeHelper =
                    (DataNodeCommitHelper) i.next();
                List queries = nodeHelper.getQueries();
                nodeHelper.getNode().performQueries(queries, observer);

                if (observer.isTransactionRolledback()) {
                    context.fireTransactionRolledback();
                    throw new CayenneException("Transaction was rolledback.");
                } else if (!observer.isTransactionCommitted()) {
                    throw new CayenneException("Error committing transaction.");
                }

                postprocess(nodeHelper);
            }
            context.clearFlattenedUpdateQueries();
            context.fireTransactionCommitted();
        } finally {
            if (context.isTransactionEventsEnabled())
                observer.unregisterFromDataContextEvents();
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
                (Collection) objectsToDeleteByObjEntity.get(
                    entity.getClassName());
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
                (Collection) objectsToUpdateByObjEntity.get(
                    entity.getClassName());
            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                ObjectId oldId = (ObjectId) o.getObjectId();
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

        RefIntegritySupport sorter =
            commitHelper.getNode().getReferentialIntegritySupport();
        if (sorter != null)
            Collections.sort(entities, sorter.getObjEntityComparator());
        for (Iterator i = entities.iterator(); i.hasNext();) {
            ObjEntity entity = (ObjEntity) i.next();
            List objects =
                (List) newObjectsByObjEntity.get(entity.getClassName());
            if (sorter != null)
                objects = sorter.sort(objects, entity);
            InsertBatchQuery batch =
                new InsertBatchQuery(entity.getDbEntity(), objects.size());
            if (logObj.isDebugEnabled())
                logObj.debug(
                    "Creating InsertBatchQuery for DbEntity "
                        + entity.getDbEntity().getName());

            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                batch.add(context.takeObjectSnapshot(o));
                //queries.add(QueryHelper.insertQuery(context.takeObjectSnapshot(o), o.getObjectId()));
            }

            commitHelper.getQueries().add(batch);
            insObjects.addAll(objects);
        }
    }

    private void prepareDeleteQueries(DataNodeCommitHelper commitHelper)
        throws CayenneException {

        List entities = commitHelper.getObjEntitiesForDelete();
        if (entities.isEmpty()) {
            return;
        }

        RefIntegritySupport sorter =
            commitHelper.getNode().getReferentialIntegritySupport();
        if (sorter != null)
            Collections.sort(
                entities,
                ComparatorUtils.reversedComparator(
                    sorter.getObjEntityComparator()));
        for (Iterator i = entities.iterator(); i.hasNext();) {
            ObjEntity entity = (ObjEntity) i.next();
            List objects =
                (List) objectsToDeleteByObjEntity.get(entity.getClassName());
            if (sorter != null)
                objects = sorter.sort(objects, entity);
            DeleteBatchQuery batch =
                new DeleteBatchQuery(entity.getDbEntity(), objects.size());
            if (logObj.isDebugEnabled())
                logObj.debug(
                    "Creating DeleteBatchQuery for DbEntity "
                        + entity.getDbEntity().getName());
            for (ListIterator j = objects.listIterator(objects.size());
                j.hasPrevious();
                ) {
                DataObject o = (DataObject) j.previous();
                Map id = o.getObjectId().getIdSnapshot();
                if (id != null && !id.isEmpty())
                    batch.add(id);
                //queries.add(QueryHelper.deleteQuery(o));
            }

            commitHelper.getQueries().add(batch);
            delObjects.addAll(objects);
        }
    }

    private void prepareUpdateQueries(DataNodeCommitHelper commitHelper)
        throws CayenneException {
        List entities = commitHelper.getObjEntitiesForUpdate();
        if (entities.isEmpty()) {
            return;
        }

        for (Iterator i = entities.iterator(); i.hasNext();) {
            ObjEntity entity = (ObjEntity) i.next();
            List objects =
                (List) objectsToUpdateByObjEntity.get(entity.getClassName());
            Map batches = new SequencedHashMap();
            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                Map snapshot = BatchUtils.buildSnapshotForUpdate(o);
                if (snapshot.isEmpty()) {
                    o.setPersistenceState(PersistenceState.COMMITTED);
                    continue;
                }
                TreeSet updatedAttributeNames = new TreeSet(snapshot.keySet());
                Integer hashCode =
                    new Integer(BatchUtils.hashCode(updatedAttributeNames));
                UpdateBatchQuery batch =
                    (UpdateBatchQuery) batches.get(hashCode);
                if (batch == null) {
                    batch =
                        new UpdateBatchQuery(
                            entity.getDbEntity(),
                            new ArrayList(snapshot.keySet()),
                            10);
                    if (logObj.isDebugEnabled())
                        logObj.debug(
                            "Creating UpdateBatchQuery for DbEntity "
                                + entity.getDbEntity().getName());
                    batches.put(hashCode, batch);
                }
                Map idSnapshot = o.getObjectId().getIdSnapshot();
                batch.add(idSnapshot, snapshot);
                ObjectId updId =
                    updatedId(
                        o.getObjectId().getObjClass(),
                        idSnapshot,
                        snapshot);
                if (updId != null)
                    updatedIds.put(o.getObjectId(), updId);
                updObjects.add(o);
                /*
                UpdateQuery query = QueryHelper.updateQuery(o);
                if (query == null) {
                  o.setPersistenceState(PersistenceState.COMMITTED);
                  continue;
                }
                queries.add(query);
                ObjectId updId = updatedId(o.getObjectId(), query);
                if (updId != null) updatedIds.put(o.getObjectId(), updId);
                */

            }

            commitHelper.getQueries().addAll(batches.values());
        }
    }

    private void createPrimaryKeys() throws CayenneException {
        Collections.sort(
            objEntitiesToInsert,
            context.getKeyGenerator().getObjEntityComparator());
        for (Iterator i = objEntitiesToInsert.iterator(); i.hasNext();) {
            ObjEntity currentEntity = (ObjEntity) i.next();
            List dataObjects =
                (List) newObjectsByObjEntity.get(currentEntity.getClassName());
            context.getKeyGenerator().createPermIdsForObjEntity(dataObjects);
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
        readOnlyObjEntities = new HashSet();
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
        if (!classifyByEntityAndNode(o,
            newObjectsByObjEntity,
            objEntitiesToInsert,
            DataNodeCommitHelper.INSERT))
            throw new CayenneException("Classification failed");
    }

    private void objectToDelete(DataObject o) throws CayenneException {
        if (!classifyByEntityAndNode(o,
            objectsToDeleteByObjEntity,
            objEntitiesToDelete,
            DataNodeCommitHelper.DELETE))
            throw new CayenneException("Classification failed");
    }

    private void objectToUpdate(DataObject o) throws CayenneException {
        if (!classifyByEntityAndNode(o,
            objectsToUpdateByObjEntity,
            objEntitiesToUpdate,
            DataNodeCommitHelper.UPDATE))
            throw new CayenneException("Classification failed");
    }

    private ObjEntity classifyAsWritable(Class objEntityClass) {
        ObjEntity entity =
            context.getEntityResolver().lookupObjEntity(objEntityClass);
        if (entity == null || entity.isReadOnly()) {
            readOnlyObjEntities.add(objEntityClass);
            return null;
        } else {
            writableObjEntities.add(objEntityClass);
            return entity;
        }
    }

    private boolean classifyByEntityAndNode(
        DataObject o,
        Map objectsByObjEntity,
        List objEntities,
        int operationType) {

        Class objEntityClass = o.getObjectId().getObjClass();
        ObjEntity entity = null;

        if (readOnlyObjEntities.contains(objEntityClass)) {
            return false;
        }

        if (!writableObjEntities.contains(objEntityClass)) {
            entity = classifyAsWritable(objEntityClass);
            if (entity == null) {
                return false;
            }
        } else {
            entity =
                context.getEntityResolver().lookupObjEntity(objEntityClass);
        }

        Collection objectsForObjEntity =
            (Collection) objectsByObjEntity.get(objEntityClass.getName());
        if (objectsForObjEntity == null) {
            objEntities.add(entity);
            DataNode responsibleNode = context.dataNodeForObjEntity(entity);

            DataNodeCommitHelper commitHelper =
                DataNodeCommitHelper.getHelperForNode(
                    nodeHelpers,
                    responsibleNode);

            commitHelper.addToEntityList(entity, operationType);
            objectsForObjEntity = new ArrayList();
            objectsByObjEntity.put(
                objEntityClass.getName(),
                objectsForObjEntity);
        }
        objectsForObjEntity.add(o);
        return true;
    }

    private Map categorizeFlattenedInsertsAndCreateBatches() {
        List flattenedInsertsList = context.getFlattenedInserts();
        Map flattenedBatches = new HashMap();
        for (Iterator i = flattenedInsertsList.iterator(); i.hasNext();) {
            DataContext.FlattenedRelationshipInfo info =
                (FlattenedRelationshipInfo) i.next();
            DataObject source = info.getSource();
            if (source.getPersistenceState() == PersistenceState.DELETED)
                continue;

            Map sourceId = source.getObjectId().getIdSnapshot();
            ObjEntity sourceEntity =
                context.getEntityResolver().lookupObjEntity(source);
            DataNode responsibleNode =
                context.dataNodeForObjEntity(sourceEntity);
            Map batchesByDbEntity = (Map) flattenedBatches.get(responsibleNode);
            if (batchesByDbEntity == null) {
                batchesByDbEntity = new HashMap();
                flattenedBatches.put(responsibleNode, batchesByDbEntity);
            }

            ObjRelationship flattenedRel = info.getBaseRelationship();
            List relList = flattenedRel.getDbRelationshipList();
            DbRelationship firstDbRel = (DbRelationship) relList.get(0);
            DbRelationship secondDbRel = (DbRelationship) relList.get(1);
            DbEntity flattenedEntity = (DbEntity) firstDbRel.getTargetEntity();
            InsertBatchQuery relationInsertQuery =
                (InsertBatchQuery) batchesByDbEntity.get(flattenedEntity);
            if (relationInsertQuery == null) {
                relationInsertQuery = new InsertBatchQuery(flattenedEntity, 50);
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
                BatchUtils.buildFlattenedSnapshot(
                    sourceId,
                    dstId,
                    firstDbRel,
                    secondDbRel);
            relationInsertQuery.add(flattenedSnapshot);
        }
        return flattenedBatches;
    }

    private Map categorizeFlattenedDeletesAndCreateBatches() {
        List flattenedDeletes = context.getFlattenedDeletes();
        Map flattenedBatches = new HashMap();
        for (Iterator i = flattenedDeletes.iterator(); i.hasNext();) {
            DataContext.FlattenedRelationshipInfo info =
                (FlattenedRelationshipInfo) i.next();
            DataObject source = info.getSource();
            Map sourceId = source.getObjectId().getIdSnapshot();
            if (sourceId == null)
                continue;

            ObjEntity sourceEntity =
                context.getEntityResolver().lookupObjEntity(source);
            DataNode responsibleNode =
                context.dataNodeForObjEntity(sourceEntity);
            Map batchesByDbEntity = (Map) flattenedBatches.get(responsibleNode);
            if (batchesByDbEntity == null) {
                batchesByDbEntity = new HashMap();
                flattenedBatches.put(responsibleNode, batchesByDbEntity);
            }

            ObjRelationship flattenedRel = info.getBaseRelationship();
            List relList = flattenedRel.getDbRelationshipList();
            DbRelationship firstDbRel = (DbRelationship) relList.get(0);
            DbRelationship secondDbRel = (DbRelationship) relList.get(1);
            DbEntity flattenedEntity = (DbEntity) firstDbRel.getTargetEntity();
            DeleteBatchQuery relationDeleteQuery =
                (DeleteBatchQuery) batchesByDbEntity.get(flattenedEntity);
            if (relationDeleteQuery == null) {
                relationDeleteQuery = new DeleteBatchQuery(flattenedEntity, 50);
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
                BatchUtils.buildFlattenedSnapshot(
                    sourceId,
                    dstId,
                    firstDbRel,
                    secondDbRel);
            relationDeleteQuery.add(flattenedSnapshot);
        }
        return flattenedBatches;
    }

    private void prepareFlattenedQueries(
        DataNode node,
        List queries,
        Map flattenedBatches) {
        Map batchesByDbEntity = (Map) flattenedBatches.get(node);
        if (batchesByDbEntity == null)
            return;
        for (Iterator i = batchesByDbEntity.values().iterator();
            i.hasNext();
            ) {
            queries.add((BatchQuery) i.next());
        }
    }

    private ObjectId updatedId(ObjectId id, UpdateQuery query) {
        Map idMap = id.getIdSnapshot();
        Map updAttrs = query.getUpdAttributes();
        return updatedId(id.getObjClass(), idMap, updAttrs);
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
        return (newIdMap != null)
            ? new ObjectId(objEntityClass, newIdMap)
            : null;
    }

    private class CommitObserver extends ContextCommitObserver {
        private CommitObserver(
            Level logLevel,
            DataContext context,
            List insObjects,
            List updObjects,
            List delObjects) {
            super(logLevel, context, insObjects, updObjects, delObjects);
        }
        public boolean useAutoCommit() {
            return false;
        }
        public void transactionCommitted() {
            transactionCommittedImpl();
        }
        public List orderQueries(DataNode aNode, List queryList) {
            return queryList;
        }
    }
}
