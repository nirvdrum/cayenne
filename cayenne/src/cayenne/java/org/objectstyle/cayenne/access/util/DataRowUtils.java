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

package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.access.DataRow;
import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.access.ToManyListDataSource;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.Util;

/**
 * DataRowUtils contains a number of static methods to work with DataRows.
 * This is a helper class that works in conjunction with DataContext.
 *
 * @author Andrei Adamchik
 */
public class DataRowUtils {
    /**
     * A factory method of DataObjects. Uses Configuration ClassLoader to
     * instantiate a new instance of DataObject of a given class.
     */
    public static final CayenneDataObject newDataObject(String className)
        throws Exception {
        return (CayenneDataObject) Configuration
            .getResourceLoader()
            .loadClass(className)
            .newInstance();
    }

    /**
     * Creates an object id from the values in object snapshot.
     * If needed attributes are missing in a snapshot or if it is null,
     * CayenneRuntimeException is thrown.
     */
    public static ObjectId objectIdFromSnapshot(ObjEntity entity, Map snapshot) {

        // ... handle special case - PK.size == 1
        //     use some not-so-significant optimizations...

        List pk = entity.getDbEntity().getPrimaryKey();
        if (pk.size() == 1) {
            DbAttribute attr = (DbAttribute) pk.get(0);
            Object val = snapshot.get(attr.getName());
            return new ObjectId(entity.getJavaClass(), attr.getName(), val);
        }

        // ... handle generic case - PK.size > 1

        Map idMap = new HashMap(pk.size() * 2);
        Iterator it = pk.iterator();
        while (it.hasNext()) {
            DbAttribute attr = (DbAttribute) it.next();
            Object val = snapshot.get(attr.getName());
            if (val == null) {
                throw new CayenneRuntimeException(
                    "Null value for '" + attr.getName() + "'. Snapshot: " + snapshot);
            }

            idMap.put(attr.getName(), val);
        }

        return new ObjectId(entity.getJavaClass(), idMap);
    }

    /**
     * Returns an ObjectId of an object on the other side of the to-one relationship,
     * given a snapshot of the source object. Returns null if snapshot FK columns
     * indicate a null to-one relationship.
     */
    public static final ObjectId targetObjectId(
        Class targetClass,
        DbRelationship relationship,
        Map sourceSnapshot) {
        Map target = relationship.targetPkSnapshotWithSrcSnapshot(sourceSnapshot);
        return (target != null) ? new ObjectId(targetClass, target) : null;
    }

    /** 
     * Replaces all object attribute values with snapshot values. 
     * Sets object state to COMMITTED, unless the snapshot is partial
     * in which case the state is set to HOLLOW
     */
    public static void refreshObjectWithSnapshot(
        ObjEntity objEntity,
        DataObject object,
        Map snapshot,
        boolean invalidateToManyRelationships) {

        Map attrMap = objEntity.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        boolean isPartialSnapshot = false;
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);
            String dbAttrPath = attr.getDbAttributePath();
            object.writePropertyDirectly(attrName, snapshot.get(dbAttrPath));
            if (!snapshot.containsKey(dbAttrPath)) {
                //Note the distinction between
                // 1) the map returning null because there was no mapping
                // for that key and
                // 2) returning null because 'null' was the value mapped
                // for that key.
                // If the first case (this clause) then snapshot is only partial
                isPartialSnapshot = true;
            }
        }

        DataContext context = object.getDataContext();
        ToManyListDataSource relDataSource = context.getRelationshipDataSource();

        Iterator rit = objEntity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {

                // "to many" relationships have no information to collect from snapshot
                // initialize a new empty list if requested, but otherwise 
                // ignore snapshot data

                ToManyList toManyList =
                    (ToManyList) object.readPropertyDirectly(rel.getName());

                if (toManyList == null) {
                    object.writePropertyDirectly(
                        rel.getName(),
                        new ToManyList(
                            relDataSource,
                            object.getObjectId(),
                            rel.getName()));
                }
                else if (invalidateToManyRelationships) {
                    toManyList.invalidateObjectList();
                }

                continue;
            }

            ObjEntity targetEntity = (ObjEntity) rel.getTargetEntity();
            Class targetClass = targetEntity.getJavaClass();

            // handle toOne flattened relationship
            if (rel.isFlattened()) {
                // A flattened toOne relationship must be a series of
                // toOne dbRelationships.  Initialize fault for it, since 
                // creating a hollow object won't be right...
                RelationshipFault fault = new RelationshipFault(object, rel.getName());
                object.writePropertyDirectly(rel.getName(), fault);
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);

            // dependent to one relationship is optional 
            // use fault, since we do not know whether it is null or not...
            if (dbRel.isToDependentPK()) {
                RelationshipFault fault = new RelationshipFault(object, rel.getName());
                object.writePropertyDirectly(rel.getName(), fault);
                continue;
            }

            ObjectId id = targetObjectId(targetClass, dbRel, snapshot);
            DataObject targetObject = (id != null) ? context.registeredObject(id) : null;

            object.writePropertyDirectly(rel.getName(), targetObject);
        }

        if (isPartialSnapshot) {
            object.setPersistenceState(PersistenceState.HOLLOW);
        }
        else {
            object.setPersistenceState(PersistenceState.COMMITTED);
        }

    }

    public static void mergeObjectsWithSnapshotDiffs(
        ObjectStore objectStore,
        Map diffs) {

        if (diffs != null && !diffs.isEmpty()) {
            Iterator oids = diffs.keySet().iterator();

            while (oids.hasNext()) {
                ObjectId oid = (ObjectId) oids.next();
                DataObject object = objectStore.getObject(oid);

                // no object, or HOLLOW object require no processing
                if (object == null
                    || object.getPersistenceState() == PersistenceState.HOLLOW) {
                    continue;
                }

                // we are lazy, just turn COMMITTED object into HOLLOW instead of 
                // actually updating it
                if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                    object.setPersistenceState(PersistenceState.HOLLOW);
                    continue;
                }

                // merge modified and deleted
                if (object.getPersistenceState() == PersistenceState.DELETED
                    || object.getPersistenceState() == PersistenceState.MODIFIED) {

                    ObjEntity entity =
                        object.getDataContext().getEntityResolver().lookupObjEntity(
                            object);
                    forceMergeWithSnapshot(entity, object, (Map) diffs.get(oid));
                }
            }
        }
    }

    private static void forceMergeWithSnapshot(
        ObjEntity entity,
        DataObject anObject,
        Map snapshot) {

        DataContext context = anObject.getDataContext();
        Map oldSnap =
            context.getObjectStore().getSnapshot(anObject.getObjectId(), context);

        // attributes
        Map attrMap = entity.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);

            //processing compound attributes correctly
            String dbAttrPath = attr.getDbAttributePath();

            // supports merging of partial snapshots...
            // check for null is cheaper than double lookup 
            // for a key... so check for partial snapshot
            // only if the value is null
            Object newVal = snapshot.get(dbAttrPath);
            if (newVal == null && !snapshot.containsKey(dbAttrPath)) {
                continue;
            }

            Object curVal = anObject.readPropertyDirectly(attrName);
            Object oldVal = oldSnap.get(dbAttrPath);

            // if value not modified, update it from snapshot,
            // otherwise leave it alone
            if (Util.nullSafeEquals(curVal, oldVal)
                && !Util.nullSafeEquals(newVal, curVal)) {
                anObject.writePropertyDirectly(attrName, newVal);
            }
        }

        // merge to-one relationships
        Iterator rit = entity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {
                continue;
            }

            // TODO: will this work for flattened, how do we save snapshots for them?

            // if value not modified, update it from snapshot,
            // otherwise leave it alone
            if (!isToOneTargetModified(rel, anObject, oldSnap)
                && isJoinAttributesModified(rel, snapshot, oldSnap)) {

                DbRelationship dbRelationship =
                    (DbRelationship) rel.getDbRelationships().get(0);

                ObjectId id =
                    targetObjectId(
                        ((ObjEntity) rel.getTargetEntity()).getJavaClass(),
                        dbRelationship,
                        snapshot);
                DataObject target = (id != null) ? context.registeredObject(id) : null;

                anObject.writePropertyDirectly(rel.getName(), target);
            }
        }
    }

    /**
     * Merges changes reflected in snapshot map to the object. Changes
     * made to attributes and to-one relationships will be merged. 
     * In case an object is already modified, modified properties will
     * not be overwritten.
     */
    public static void mergeObjectWithSnapshot(
        ObjEntity entity,
        DataObject anObject,
        Map snapshot) {

        if (entity.isReadOnly()
            || anObject.getPersistenceState() == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(entity, anObject, snapshot, true);
        }
        else if (anObject.getPersistenceState() == PersistenceState.COMMITTED) {
            // do not invalidate to-many relationships, since they might have just been prefetched...
            refreshObjectWithSnapshot(entity, anObject, snapshot, false);
        }
        else {
            forceMergeWithSnapshot(entity, anObject, snapshot);
        }
    }

    /**
     * Checks if a new snapshot has a modified to-one relationship compared to
     * the cached snapshot.
     */
    protected static boolean isJoinAttributesModified(
        ObjRelationship relationship,
        Map newSnapshot,
        Map storedSnapshot) {

        Iterator it =
            ((DbRelationship) relationship.getDbRelationships().get(0))
                .getJoins()
                .iterator();
        while (it.hasNext()) {
            DbAttributePair join = (DbAttributePair) it.next();
            String propertyName = join.getSource().getName();

            // for equality to be true, snapshot must contain all matching pk values
            if (!Util
                .nullSafeEquals(
                    newSnapshot.get(propertyName),
                    storedSnapshot.get(propertyName))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an object has its to-one relationship target modified in memory.
     */
    protected static boolean isToOneTargetModified(
        ObjRelationship relationship,
        DataObject object,
        Map storedSnapshot) {

        if (object.getPersistenceState() != PersistenceState.MODIFIED) {
            return false;
        }

        DataObject toOneTarget =
            (DataObject) object.readPropertyDirectly(relationship.getName());
        ObjectId currentId = (toOneTarget != null) ? toOneTarget.getObjectId() : null;

        // check if ObjectId map is a subset of a stored snapshot;
        // this is an equality condition
        Iterator it =
            ((DbRelationship) relationship.getDbRelationships().get(0))
                .getJoins()
                .iterator();

        while (it.hasNext()) {
            DbAttributePair join = (DbAttributePair) it.next();
            String propertyName = join.getSource().getName();

            if (currentId == null) {
                // for equality to be true, snapshot must contain no pk values
                if (storedSnapshot.get(propertyName) != null) {
                    return true;
                }
            }
            else {
                // for equality to be true, snapshot must contain all matching pk values
                // note that we must use target entity names to extract id values.
                if (!Util
                    .nullSafeEquals(
                        currentId.getValueForAttribute(join.getTarget().getName()),
                        storedSnapshot.get(propertyName))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Takes a snapshot of current object state.
     */
    public static DataRow takeObjectSnapshot(ObjEntity ent, DataObject anObject) {

        // for a HOLLOW object return snapshot from cache
        if (anObject.getPersistenceState() == PersistenceState.HOLLOW
            && anObject.getDataContext() != null) {

            DataContext context = anObject.getDataContext();
            ObjectId id = anObject.getObjectId();
            return context.getObjectStore().getSnapshot(id, context);
        }

        DataRow snapshot = new DataRow(10);

        Map attrMap = ent.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute objAttr = (ObjAttribute) attrMap.get(attrName);
            //processing compound attributes correctly
            snapshot.put(
                objAttr.getDbAttributePath(),
                anObject.readPropertyDirectly(attrName));
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

            Map idParts = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects
            if (idParts.isEmpty()) {
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            snapshot.putAll(fk);
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
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, thisIdParts.get(nextKey));
                }
            }
        }
        return snapshot;
    }

    /**
     * Takes a list of "root" (or "source") objects,
     * a list of destination objects, and the relationship which relates them
     * (from root to destination).  It then merges the destination objects
     * into the toMany relationships of the relevant root objects, thus clearing
     * the toMany fault.  This method is typically only used internally by Cayenne
     * and is not intended for client use.
     * @param rootObjects
     * @param theRelationship
     * @param destinationObjects
     */
    public static void mergePrefetchResultsRelationships(
        List rootObjects,
        ObjRelationship relationship,
        List destinationObjects) {

        if (rootObjects.size() == 0) {
            // nothing to do
            return;
        }

        Class sourceObjectClass = ((DataObject) rootObjects.get(0)).getClass();
        ObjRelationship reverseRelationship = relationship.getReverseRelationship();
        //Might be used later on... obtain and cast only once
        DbRelationship dbRelationship =
            (DbRelationship) relationship.getDbRelationships().get(0);

        Factory listFactory = new Factory() {
            public Object create() {
                return new ArrayList();
            }
        };

        Map toManyLists = MapUtils.lazyMap(new HashMap(), listFactory);

        Iterator destIterator = destinationObjects.iterator();
        while (destIterator.hasNext()) {
            DataObject thisDestinationObject = (DataObject) destIterator.next();
            DataObject sourceObject = null;
            if (reverseRelationship != null) {
                sourceObject =
                    (DataObject) thisDestinationObject.readPropertyDirectly(
                        reverseRelationship.getName());
            }
            else {
                //Reverse relationship doesn't exist... match objects manually
                DataContext context = thisDestinationObject.getDataContext();
                Map sourcePk =
                    dbRelationship.srcPkSnapshotWithTargetSnapshot(
                        context.getObjectStore().getSnapshot(
                            thisDestinationObject.getObjectId(),
                            context));
                sourceObject =
                    context.registeredObject(new ObjectId(sourceObjectClass, sourcePk));
            }
            //Find the list so far for this sourceObject
            List thisList = (List) toManyLists.get(sourceObject);
            thisList.add(thisDestinationObject);

        }

        //destinationObjects has now been partitioned into a list per
        //source object...
        //Iterate over the source objects and fix up the relationship on
        //each
        Iterator rootIterator = rootObjects.iterator();
        while (rootIterator.hasNext()) {
            DataObject thisRoot = (DataObject) rootIterator.next();
            ToManyList toManyList =
                (ToManyList) thisRoot.readPropertyDirectly(relationship.getName());

            toManyList.setObjectList((List) toManyLists.get(thisRoot));
        }
    }

    /**
     * Instantiation is not allowed.
     */
    private DataRowUtils() {
        super();
    }

}
