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
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.BatchUtils;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.Query;
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
    private Map objEntitiesToInsertByNode;
    private Map objEntitiesToDeleteByNode;
    private Map objEntitiesToUpdateByNode;
    private Map updatedIds;

    ContextCommit(DataContext contextToCommit) {
        context = contextToCommit;
    }

    void commit(Level logLevel) throws CayenneException {
        this.logLevel = logLevel;
        categorizeObjects();
        createPrimaryKeys();
        updatedIds = new SequencedHashMap();
        Set nodes = new HashSet(objEntitiesToInsertByNode.keySet());
        nodes.addAll(objEntitiesToDeleteByNode.keySet());
        nodes.addAll(objEntitiesToUpdateByNode.keySet());
        Map queriesByNode = new SequencedHashMap(nodes.size());
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            DataNode node = (DataNode) i.next();
            List queries = new ArrayList();
            prepareInsertQueries(node, queries);
            prepareUpdateQueries(node, queries);
            prepareDeleteQueries(node, queries);
            if (!queries.isEmpty())
                queriesByNode.put(node, queries);
        }

        CommitObserver observer = new CommitObserver();
        observer.setLoggingLevel(logLevel);
        for (Iterator i = queriesByNode.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            DataNode nodeToCommit = (DataNode) entry.getKey();
            List queries = (List) entry.getValue();
            nodeToCommit.performQueries(queries, observer);
            if (!observer.isTransactionCommitted())
                throw new CayenneException("Error committing transaction.");
            else if (observer.isTransactionRolledback())
                throw new CayenneException("Transaction was rolledback.");
            postprocess(nodeToCommit);
        }
    }

    private void postprocess(DataNode committedNode) {
        ObjectStore objectStore = context.getObjectStore();
        Collection entitiesForNode =
            (Collection) objEntitiesToInsertByNode.get(committedNode);
        entitiesForNode =
            (entitiesForNode != null
                ? entitiesForNode
                : Collections.EMPTY_LIST);
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

        entitiesForNode =
            (Collection) objEntitiesToDeleteByNode.get(committedNode);
        entitiesForNode =
            (entitiesForNode != null
                ? entitiesForNode
                : Collections.EMPTY_LIST);
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

        entitiesForNode =
            (Collection) objEntitiesToUpdateByNode.get(committedNode);
        entitiesForNode =
            (entitiesForNode != null
                ? entitiesForNode
                : Collections.EMPTY_LIST);
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

    private void prepareInsertQueries(DataNode node, List queries)
        throws CayenneException {
        List entities = (List) objEntitiesToInsertByNode.get(node);
        if (entities == null)
            return;
        RefIntegritySupport sorter = node.getReferentialIntegritySupport();
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
            for (Iterator j = objects.iterator(); j.hasNext();) {
                DataObject o = (DataObject) j.next();
                batch.add(context.takeObjectSnapshot(o));
                //queries.add(QueryHelper.insertQuery(context.takeObjectSnapshot(o), o.getObjectId()));
            }
            queries.add(batch);
        }
    }

    private void prepareDeleteQueries(DataNode node, List queries)
        throws CayenneException {
        List entities = (List) objEntitiesToDeleteByNode.get(node);
        if (entities == null)
            return;
        RefIntegritySupport sorter = node.getReferentialIntegritySupport();
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
            for (ListIterator j = objects.listIterator(objects.size());
                j.hasPrevious();
                ) {
                DataObject o = (DataObject) j.previous();
                Map id = o.getObjectId().getIdSnapshot();
                if (id != null && !id.isEmpty())
                    batch.add(id);
                //queries.add(QueryHelper.deleteQuery(o));
            }
            queries.add(batch);
        }
    }

    private void prepareUpdateQueries(DataNode node, List queries)
        throws CayenneException {
        List entities = (List) objEntitiesToUpdateByNode.get(node);
        if (entities == null)
            return;
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
            queries.addAll(batches.values());
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

    private void categorizeObjects() {
        Iterator it = context.getObjectStore().getObjectIterator();
        newObjectsByObjEntity = new HashMap();
        objectsToDeleteByObjEntity = new HashMap();
        objectsToUpdateByObjEntity = new HashMap();
        writableObjEntities = new HashSet();
        readOnlyObjEntities = new HashSet();
        objEntitiesToInsert = new ArrayList();
        objEntitiesToDelete = new ArrayList();
        objEntitiesToUpdate = new ArrayList();
        objEntitiesToInsertByNode = new HashMap();
        objEntitiesToDeleteByNode = new HashMap();
        objEntitiesToUpdateByNode = new HashMap();
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

    private void objectToInsert(DataObject o) {
        classifyByEntityAndNode(
            o,
            newObjectsByObjEntity,
            objEntitiesToInsertByNode,
            objEntitiesToInsert);
    }

    private void objectToDelete(DataObject o) {
        classifyByEntityAndNode(
            o,
            objectsToDeleteByObjEntity,
            objEntitiesToDeleteByNode,
            objEntitiesToDelete);
    }

    private void objectToUpdate(DataObject o) {
        classifyByEntityAndNode(
            o,
            objectsToUpdateByObjEntity,
            objEntitiesToUpdateByNode,
            objEntitiesToUpdate);
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

    private void classifyByEntityAndNode(
        DataObject o,
        Map objectsByObjEntity,
        Map objEntitiesByNode,
        List objEntities) {
        Class objEntityClass = o.getObjectId().getObjClass();
        ObjEntity entity = null;
        if (readOnlyObjEntities.contains(objEntityClass))
            return;
        if (!writableObjEntities.contains(objEntityClass)) {
            entity = classifyAsWritable(objEntityClass);
            if (entity == null)
                return;
        } else {
            entity =
                context.getEntityResolver().lookupObjEntity(objEntityClass);
        }
        Collection objectsForObjEntity =
            (Collection) objectsByObjEntity.get(objEntityClass.getName());
        if (objectsForObjEntity == null) {
            objEntities.add(entity);
            DataNode responsibleNode = context.dataNodeForObjEntity(entity);
            Collection entitiesForNode =
                (Collection) objEntitiesByNode.get(responsibleNode);
            if (entitiesForNode == null) {
                entitiesForNode = new ArrayList();
                objEntitiesByNode.put(responsibleNode, entitiesForNode);
            }
            entitiesForNode.add(entity);
            objectsForObjEntity = new ArrayList();
            objectsByObjEntity.put(
                objEntityClass.getName(),
                objectsForObjEntity);
        }
        objectsForObjEntity.add(o);
    }

    private void categorizeFlattenedInserts() {
        Map flattenedInserts = context.getFlattenedInserts();
        for (Iterator i = flattenedInserts.entrySet().iterator(); i.hasNext();) {
            Map.Entry fientry = (Map.Entry)i.next();
            DataObject source = (DataObject)fientry.getKey();
            Map insertsForObject = (Map)fientry.getValue();
            if (insertsForObject == null || insertsForObject.isEmpty()) continue;
            ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);
            DataNode responsibleNode = context.dataNodeForObjEntity(sourceEntity);
            for (Iterator j = insertsForObject.entrySet().iterator(); j.hasNext();) {
                Map.Entry ioentry = (Map.Entry)i.next();
                String relName = (String)ioentry.getKey();
                List insertedObjectsForRel = (List)ioentry.getValue();
                if (insertedObjectsForRel == null || insertedObjectsForRel.isEmpty()) continue;
                ObjRelationship flattenedRel = (ObjRelationship)sourceEntity.getRelationship(relName);
                DbRelationship firstDbRel = (DbRelationship)flattenedRel.getDbRelationshipList().get(0);
                DbEntity flattenedEntity = (DbEntity)firstDbRel.getTargetEntity();
                for (Iterator k = insertedObjectsForRel.iterator(); k.hasNext();) {
                    DataObject destination = (DataObject)k.next();
                }
            }
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

    private class CommitObserver extends DefaultOperationObserver {
        public boolean useAutoCommit() {
            return false;
        }
        public void transactionCommitted() {
            super.transactionCommitted();
        }
        public void nextQueryException(Query query, Exception ex) {
            super.nextQueryException(query, ex);
            throw new CayenneRuntimeException(
                "Raising from query exception.",
                ex);
        }
        public void nextGlobalException(Exception ex) {
            super.nextGlobalException(ex);
            throw new CayenneRuntimeException(
                "Raising from underlyingQueryEngine exception.",
                ex);
        }
        public List orderQueries(DataNode aNode, List queryList) {
            return queryList;
        }
    }
}
