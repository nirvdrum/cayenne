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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.NodeIdChangeOperation;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * A stateful commit handler used by DataContext to perform commit operation.
 * DataContextCommitAction resolves primary key dependencies, referential integrity
 * dependencies (including multi-reflexive entities), generates primary keys, creates
 * batches for massive data modifications, assigns operations to data nodes.
 * 
 * @author Andriy Shapochka,
 * @author Andrus Adamchik
 * @since 1.2
 */
class DataDomainFlushAction {

    private DataDomain domain;
    private DataContext context;
    private Map newObjectsByObjEntity;
    private Map objectsToDeleteByObjEntity;
    private Map objectsToUpdateByObjEntity;
    private List objEntitiesToInsert;
    private List objEntitiesToDelete;
    private List objEntitiesToUpdate;
    private List nodeHelpers;
    private List newObjects;
    private List deletedObjects;
    private List updatedObjects;

    DataDomainFlushAction(DataDomain domain) {
        this.domain = domain;
    }

    GraphDiff flush(DataContext context, GraphDiff diff) {

        this.context = context;

        // note that there is no syncing on the object store itself. This is caller's
        // responsibility.
        synchronized (context.getObjectStore().getDataRowCache()) {

            categorizeObjects();
            categorizeFlattenedInsertsAndCreateBatches();
            categorizeFlattenedDeletesAndCreateBatches();

            newObjects = new ArrayList();
            deletedObjects = new ArrayList();
            updatedObjects = new ArrayList();

            for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
                DataNodeCommitAction nodeHelper = (DataNodeCommitAction) i.next();
                prepareInsertQueries(nodeHelper);
                prepareFlattenedQueries(nodeHelper, nodeHelper
                        .getFlattenedInsertQueries());

                prepareUpdateQueries(nodeHelper);

                prepareFlattenedQueries(nodeHelper, nodeHelper
                        .getFlattenedDeleteQueries());

                prepareDeleteQueries(nodeHelper);
            }

            CommitObserver observer = new CommitObserver(
                    context,
                    newObjects,
                    updatedObjects,
                    deletedObjects);

            if (context.isTransactionEventsEnabled()) {
                observer.registerForDataContextEvents();
            }

            try {
                context.fireWillCommit();

                Transaction transaction = Transaction.getThreadTransaction();

                try {
                    Iterator i = nodeHelpers.iterator();
                    while (i.hasNext()) {
                        DataNodeCommitAction nodeHelper = (DataNodeCommitAction) i.next();
                        List queries = nodeHelper.getQueries();

                        if (queries.size() > 0) {
                            nodeHelper.getNode().performQueries(queries, observer);
                        }
                    }
                }
                catch (Throwable th) {
                    transaction.setRollbackOnly();

                    // TODO: Andrus, 2/14/2006 - as transaction is external to this
                    // method, it is wrong to notify the listeners here... likely need to
                    // move to the DataContext.
                    context.fireTransactionRolledback();
                    throw new CayenneRuntimeException("Transaction was rolledback.", th);
                }

                GraphDiff changes = postprocessAfterCommit();

                // TODO: Andrus, 2/14/2006 - as transaction is external to this method, it
                // is wrong to notify the listeners here... likely need to move to the
                // DataContext.
                context.fireTransactionCommitted();
                return changes;
            }
            finally {
                if (context.isTransactionEventsEnabled()) {
                    observer.unregisterFromDataContextEvents();
                }
            }
        }
    }

    /*
     * Sends notification of changes to the DataRowStore, returns GraphDiff with replaced
     * ObjectIds.
     */
    private GraphDiff postprocessAfterCommit() {

        final Collection deletedIds = new ArrayList(deletedObjects.size());

        int modifiedSize = 0;
        if (updatedObjects != null) {
            modifiedSize += updatedObjects.size();
        }
        if (newObjects != null) {
            modifiedSize += newObjects.size();
        }

        int mapCapacity = (int) (modifiedSize / 0.75);
        final Map modifiedSnapshots = new HashMap(mapCapacity);
        final CompoundDiff diff = new CompoundDiff();

        // process deleted
        if (!deletedObjects.isEmpty()) {

            Iterator it = deletedObjects.iterator();
            while (it.hasNext()) {
                Persistent object = (Persistent) it.next();
                deletedIds.add(object.getObjectId());
            }
        }

        // process new and modified
        if (modifiedSize > 0) {

            // new and modified are processed in essentially the same way, so define a
            // Closure with access to local vars.
            Closure op = new Closure() {

                public void execute(Object input) {
                    if (!(input instanceof Collection)) {
                        return;
                    }

                    Collection c = (Collection) input;
                    if (c.isEmpty()) {
                        return;
                    }

                    Iterator it = c.iterator();
                    while (it.hasNext()) {
                        DataObject object = (DataObject) it.next();
                        ObjectId id = object.getObjectId();

                        DataRow dataRow = context.currentSnapshot(object);
                        dataRow.setReplacesVersion(object.getSnapshotVersion());
                        object.setSnapshotVersion(dataRow.getVersion());

                        // record id change
                        if (id.isReplacementIdAttached()) {
                            ObjectId replacementId = id.createReplacementId();
                            diff.add(new NodeIdChangeOperation(id, replacementId));

                            // classify replaced permanent ids as "deleted", as
                            // DataRowCache has no notion of replaced id...
                            if (!id.isTemporary()) {
                                deletedIds.add(id);
                            }

                            modifiedSnapshots.put(replacementId, dataRow);
                        }
                        else if (id.isTemporary()) {
                            throw new CayenneRuntimeException(
                                    "Temporary ID hasn't been replaced on commit: "
                                            + object);
                        }
                        else {
                            modifiedSnapshots.put(id, dataRow);
                        }
                    }
                }
            };

            op.execute(updatedObjects);
            op.execute(newObjects);
        }

        // notify cache...
        if (!deletedIds.isEmpty() || !modifiedSnapshots.isEmpty()) {
            // create a copy to avoid underlying ObjectStore modification
            Collection indirectlyModified = new ArrayList(
                    context.getObjectStore().indirectlyModifiedIds);

            context.getObjectStore().getDataRowCache().processSnapshotChanges(
                    context.getObjectStore(),
                    modifiedSnapshots,
                    deletedIds,
                    Collections.EMPTY_LIST,
                    indirectlyModified);
        }

        return diff;
    }

    private void prepareInsertQueries(DataNodeCommitAction commitHelper) {

        List entities = commitHelper.getObjEntitiesForInsert();
        if (entities.isEmpty()) {
            return;
        }

        boolean supportsGeneratedKeys = commitHelper
                .getNode()
                .getAdapter()
                .supportsGeneratedKeys();
        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        EntitySorter sorter = commitHelper.getNode().getEntitySorter();
        sorter.sortDbEntities(dbEntities, false);

        Iterator i = dbEntities.iterator();
        while (i.hasNext()) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);

            InsertBatchQuery batch = new InsertBatchQuery(dbEntity, 27);

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();
                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);
                DbRelationship masterDependentDbRel = (isMasterDbEntity
                        ? null
                        : findMasterToDependentDbRelationship(
                                entity.getDbEntity(),
                                dbEntity));

                List objects = (List) newObjectsByObjEntity.get(entity.getName());

                // throw an exception - an attempt to modify read-only entity
                if (entity.isReadOnly() && objects.size() > 0) {
                    throw attemptToCommitReadOnlyEntity(objects.get(0).getClass(), entity);
                }

                if (isMasterDbEntity) {
                    sorter.sortObjectsForEntity(entity, objects, false);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    Map snapshot = BatchQueryUtils.buildSnapshotForInsert(
                            entity,
                            o,
                            masterDependentDbRel,
                            supportsGeneratedKeys);
                    batch.add(snapshot, o.getObjectId());
                }

                if (isMasterDbEntity) {
                    newObjects.addAll(objects);
                }
            }
            commitHelper.getQueries().add(batch);
        }
    }

    private void prepareDeleteQueries(DataNodeCommitAction commitHelper) {

        List entities = commitHelper.getObjEntitiesForDelete();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        EntitySorter sorter = commitHelper.getNode().getEntitySorter();
        sorter.sortDbEntities(dbEntities, true);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            Map batches = new LinkedMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                // Per-objEntity optimistic locking conditional
                boolean optimisticLocking = (ObjEntity.LOCK_TYPE_OPTIMISTIC == entity
                        .getLockType());

                List qualifierAttributes = qualifierAttributes(entity, optimisticLocking);

                boolean isRootDbEntity = (entity.getDbEntity() == dbEntity);
                DbRelationship masterDependentDbRel = (isRootDbEntity
                        ? null
                        : findMasterToDependentDbRelationship(
                                entity.getDbEntity(),
                                dbEntity));

                List objects = (List) objectsToDeleteByObjEntity.get(entity.getName());

                // throw an exception - an attempt to delete read-only entity
                if (entity.isReadOnly() && objects.size() > 0) {
                    throw attemptToCommitReadOnlyEntity(objects.get(0).getClass(), entity);
                }

                if (isRootDbEntity) {
                    sorter.sortObjectsForEntity(entity, objects, true);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    // build qualifier snapshot
                    Map idSnapshot = o.getObjectId().getIdSnapshot();

                    if (idSnapshot == null || idSnapshot.isEmpty()) {
                        // skip this one
                        continue;
                    }

                    if (!isRootDbEntity && masterDependentDbRel != null) {
                        idSnapshot = masterDependentDbRel
                                .targetPkSnapshotWithSrcSnapshot(idSnapshot);
                    }

                    Map qualifierSnapshot = idSnapshot;
                    if (optimisticLocking) {
                        // clone snapshot and add extra keys...
                        qualifierSnapshot = new HashMap(qualifierSnapshot);
                        appendOptimisticLockingAttributes(
                                qualifierSnapshot,
                                o,
                                qualifierAttributes);
                    }

                    // organize batches by the nulls in qualifier
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                        nullQualifierNames
                    });

                    DeleteBatchQuery batch = (DeleteBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new DeleteBatchQuery(
                                dbEntity,
                                qualifierAttributes,
                                nullQualifierNames,
                                27);
                        batch.setUsingOptimisticLocking(optimisticLocking);
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot);

                }

                if (isRootDbEntity)
                    deletedObjects.addAll(objects);

            }
            commitHelper.getQueries().addAll(batches.values());
        }
    }

    private void prepareUpdateQueries(DataNodeCommitAction commitHelper) {
        List entities = commitHelper.getObjEntitiesForUpdate();
        if (entities.isEmpty()) {
            return;
        }

        boolean supportsGeneratedKeys = commitHelper
                .getNode()
                .getAdapter()
                .supportsGeneratedKeys();

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
                boolean optimisticLocking = (ObjEntity.LOCK_TYPE_OPTIMISTIC == entity
                        .getLockType());

                List qualifierAttributes = qualifierAttributes(entity, optimisticLocking);

                boolean isRootDbEntity = entity.getDbEntity() == dbEntity;

                DbRelationship masterDependentDbRel = (isRootDbEntity)
                        ? null
                        : findMasterToDependentDbRelationship(
                                entity.getDbEntity(),
                                dbEntity);
                List objects = (List) objectsToUpdateByObjEntity.get(entity.getName());

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    Map snapshot = BatchQueryUtils.buildSnapshotForUpdate(
                            entity,
                            o,
                            masterDependentDbRel,
                            supportsGeneratedKeys);

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

                    // build qualifier snapshot
                    Map idSnapshot = o.getObjectId().getIdSnapshot();

                    if (!isRootDbEntity && masterDependentDbRel != null) {
                        idSnapshot = masterDependentDbRel
                                .targetPkSnapshotWithSrcSnapshot(idSnapshot);
                    }

                    Map qualifierSnapshot = idSnapshot;
                    if (optimisticLocking) {
                        // clone snapshot and add extra keys...
                        qualifierSnapshot = new HashMap(qualifierSnapshot);
                        appendOptimisticLockingAttributes(
                                qualifierSnapshot,
                                o,
                                qualifierAttributes);
                    }

                    // organize batches by the updated columns + nulls in qualifier
                    Set snapshotSet = snapshot.keySet();
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                            snapshotSet, nullQualifierNames
                    });

                    UpdateBatchQuery batch = (UpdateBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new UpdateBatchQuery(
                                dbEntity,
                                qualifierAttributes,
                                updatedAttributes(dbEntity, snapshot),
                                nullQualifierNames,
                                10);

                        batch.setUsingOptimisticLocking(optimisticLocking);
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot, snapshot, o.getObjectId());

                    if (isRootDbEntity) {
                        updateId(
                                idSnapshot,
                                o.getObjectId().getReplacementIdMap(),
                                snapshot);
                        updatedObjects.add(o);
                    }
                }
            }
            commitHelper.getQueries().addAll(batches.values());
        }
    }

    /**
     * Creates a list of DbAttributes that should be used in update WHERE clause.
     */
    private List qualifierAttributes(ObjEntity entity, boolean optimisticLocking) {
        if (!optimisticLocking) {
            return entity.getDbEntity().getPrimaryKey();
        }

        List attributes = new ArrayList(entity.getDbEntity().getPrimaryKey());

        Iterator attributeIt = entity.getAttributes().iterator();
        while (attributeIt.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) attributeIt.next();

            if (attribute.isUsedForLocking()) {
                // only care about first step in a flattened attribute
                DbAttribute dbAttribute = (DbAttribute) attribute
                        .getDbPathIterator()
                        .next();

                if (!attributes.contains(dbAttribute)) {
                    attributes.add(dbAttribute);
                }
            }
        }

        Iterator relationshipIt = entity.getRelationships().iterator();
        while (relationshipIt.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) relationshipIt.next();

            if (relationship.isUsedForLocking()) {
                // only care about the first DbRelationship
                DbRelationship dbRelationship = (DbRelationship) relationship
                        .getDbRelationships()
                        .get(0);

                Iterator joinsIterator = dbRelationship.getJoins().iterator();
                while (joinsIterator.hasNext()) {
                    DbJoin dbAttrPair = (DbJoin) joinsIterator.next();
                    DbAttribute dbAttribute = dbAttrPair.getSource();
                    if (!attributes.contains(dbAttribute)) {
                        attributes.add(dbAttribute);
                    }
                }
            }
        }

        return attributes;
    }

    /**
     * Creates a list of DbAttributes that are updated in a snapshot
     * 
     * @param entity
     * @return
     */
    private List updatedAttributes(DbEntity entity, Map updatedSnapshot) {
        List attributes = new ArrayList(updatedSnapshot.size());
        Map entityAttributes = entity.getAttributeMap();

        Iterator it = updatedSnapshot.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            attributes.add(entityAttributes.get(name));
        }

        return attributes;
    }

    /**
     * Appends values used for optimistic locking to a given snapshot.
     */
    private void appendOptimisticLockingAttributes(
            Map qualifierSnapshot,
            DataObject dataObject,
            List qualifierAttributes) {

        Map snapshot = null;

        Iterator it = qualifierAttributes.iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            String name = attribute.getName();
            if (!qualifierSnapshot.containsKey(name)) {

                // get cached snapshot on demand ...
                if (snapshot == null) {
                    snapshot = dataObject
                            .getDataContext()
                            .getObjectStore()
                            .getCachedSnapshot(dataObject.getObjectId());

                    // sanity check...
                    if (snapshot == null) {
                        throw new CayenneRuntimeException(
                                "Can't build qualifier for optimistic locking, "
                                        + "no snapshot for id "
                                        + dataObject.getObjectId());
                    }
                }

                qualifierSnapshot.put(name, snapshot.get(name));
            }
        }
    }

    /**
     * Organizes committed objects by node, performs sorting operations.
     */
    private void categorizeObjects() {
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
                case PersistenceState.NEW:
                    objectToInsert(nextObject);
                    break;
                case PersistenceState.DELETED:
                    objectToDelete(nextObject);
                    break;
                case PersistenceState.MODIFIED:
                    objectToUpdate(nextObject);
                    break;
            }
        }
    }

    private void objectToInsert(DataObject o) {
        classifyByEntityAndNode(
                o,
                newObjectsByObjEntity,
                objEntitiesToInsert,
                DataNodeCommitAction.INSERT);
    }

    private void objectToDelete(DataObject o) {
        classifyByEntityAndNode(
                o,
                objectsToDeleteByObjEntity,
                objEntitiesToDelete,
                DataNodeCommitAction.DELETE);
    }

    private void objectToUpdate(DataObject o) {
        classifyByEntityAndNode(
                o,
                objectsToUpdateByObjEntity,
                objEntitiesToUpdate,
                DataNodeCommitAction.UPDATE);
    }

    private RuntimeException attemptToCommitReadOnlyEntity(
            Class objectClass,
            ObjEntity entity) {
        String className = (objectClass != null) ? objectClass.getName() : "<null>";
        StringBuffer message = new StringBuffer();
        message
                .append("Class '")
                .append(className)
                .append("' maps to a read-only entity");

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

        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                o.getObjectId().getEntityName());
        Collection objectsForObjEntity = (Collection) objectsByObjEntity.get(entity
                .getName());
        if (objectsForObjEntity == null) {
            objEntities.add(entity);
            DataNode responsibleNode = domain.lookupDataNode(entity.getDataMap());

            DataNodeCommitAction commitHelper = nodeHelper(responsibleNode);

            commitHelper.addToEntityList(entity, operationType);
            objectsForObjEntity = new ArrayList();
            objectsByObjEntity.put(entity.getName(), objectsForObjEntity);
        }
        objectsForObjEntity.add(o);
    }

    private void categorizeFlattenedInsertsAndCreateBatches() {
        Iterator i = context.getObjectStore().getFlattenedInserts().iterator();

        while (i.hasNext()) {
            FlattenedRelationshipUpdate info = (FlattenedRelationshipUpdate) i.next();

            DataObject source = info.getSource();

            // TODO: does it ever happen? How?
            if (source.getPersistenceState() == PersistenceState.DELETED) {
                continue;
            }

            if (info.getDestination().getPersistenceState() == PersistenceState.DELETED) {
                continue;
            }

            DbEntity flattenedEntity = info.getJoinEntity();
            DataNode responsibleNode = domain
                    .lookupDataNode(flattenedEntity.getDataMap());
            DataNodeCommitAction commitHelper = nodeHelper(responsibleNode);
            Map batchesByDbEntity = commitHelper.getFlattenedInsertQueries();

            InsertBatchQuery relationInsertQuery = (InsertBatchQuery) batchesByDbEntity
                    .get(flattenedEntity);

            if (relationInsertQuery == null) {
                relationInsertQuery = new InsertBatchQuery(flattenedEntity, 50);
                batchesByDbEntity.put(flattenedEntity, relationInsertQuery);
            }

            Map flattenedSnapshot = info.buildJoinSnapshotForInsert();
            relationInsertQuery.add(flattenedSnapshot);
        }
    }

    private void categorizeFlattenedDeletesAndCreateBatches() {
        Iterator i = context.getObjectStore().getFlattenedDeletes().iterator();

        while (i.hasNext()) {
            FlattenedRelationshipUpdate info = (FlattenedRelationshipUpdate) i.next();

            // TODO: does it ever happen?
            Map sourceId = info.getSource().getObjectId().getIdSnapshot();

            if (sourceId == null)
                continue;

            Map dstId = info.getDestination().getObjectId().getIdSnapshot();
            if (dstId == null)
                continue;

            DbEntity flattenedEntity = info.getJoinEntity();

            DataNode responsibleNode = domain
                    .lookupDataNode(flattenedEntity.getDataMap());
            DataNodeCommitAction commitHelper = nodeHelper(responsibleNode);
            Map batchesByDbEntity = commitHelper.getFlattenedDeleteQueries();

            DeleteBatchQuery relationDeleteQuery = (DeleteBatchQuery) batchesByDbEntity
                    .get(flattenedEntity);
            if (relationDeleteQuery == null) {
                boolean optimisticLocking = false;
                relationDeleteQuery = new DeleteBatchQuery(flattenedEntity, 50);
                relationDeleteQuery.setUsingOptimisticLocking(optimisticLocking);
                batchesByDbEntity.put(flattenedEntity, relationDeleteQuery);
            }

            List flattenedSnapshots = info.buildJoinSnapshotsForDelete();
            if (!flattenedSnapshots.isEmpty()) {
                Iterator snapsIt = flattenedSnapshots.iterator();
                while (snapsIt.hasNext()) {
                    relationDeleteQuery.add((Map) snapsIt.next());
                }
            }
        }
    }

    private void prepareFlattenedQueries(
            DataNodeCommitAction commitHelper,
            Map flattenedBatches) {

        for (Iterator i = flattenedBatches.values().iterator(); i.hasNext();) {
            commitHelper.addToQueries((Query) i.next());
        }
    }

    // 
    private void updateId(Map oldID, Map replacementID, Map updatedKeys) {
        Iterator it = updatedKeys.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            // id values can be deferred ID factories... we are ignoring this fact here,
            // hoping they will be replaced by real values on update...

            Object key = entry.getKey();

            if (oldID.containsKey(key) && !replacementID.containsKey(key)) {
                replacementID.put(key, entry.getValue());
            }
        }
    }

    private void groupObjEntitiesBySpannedDbEntities(
            List dbEntities,
            Map objEntitiesByDbEntity,
            List objEntities) {

        Iterator i = objEntities.iterator();
        while (i.hasNext()) {
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
            for (Iterator j = objEntity.getAttributeMap().values().iterator(); j
                    .hasNext();) {
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
        for (Iterator i = masterDbEntity.getRelationshipMap().values().iterator(); i
                .hasNext();) {
            DbRelationship rel = (DbRelationship) i.next();
            if (dependentDbEntity.equals(rel.getTargetEntity()) && rel.isToDependentPK())
                return rel;
        }
        return null;
    }

    /**
     * Finds an existing helper for DataNode, creates a new one if no matching helper is
     * found.
     */
    private DataNodeCommitAction nodeHelper(DataNode node) {

        DataNodeCommitAction helper = null;
        Iterator it = nodeHelpers.iterator();
        while (it.hasNext()) {
            DataNodeCommitAction itHelper = (DataNodeCommitAction) it.next();
            if (itHelper.getNode() == node) {
                helper = itHelper;
                break;
            }
        }

        if (helper == null) {
            helper = new DataNodeCommitAction(node);
            nodeHelpers.add(helper);
        }

        return helper;
    }
}