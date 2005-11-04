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

package org.objectstyle.cayenne.access.util;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.UpdateBatchQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Collection of utility methods to work with BatchQueries.
 * 
 * @author Andriy Shapochka
 */
public class BatchQueryUtils {

    private BatchQueryUtils() {
    }

    /**
     * Utility method that returns <code>true</code> if the query will update at least
     * one BLOB or CLOB DbAttribute.
     */
    public static boolean updatesLOBColumns(BatchQuery query) {
        boolean isInsert = query instanceof InsertBatchQuery;
        boolean isUpdate = query instanceof UpdateBatchQuery;

        if (!isInsert && !isUpdate) {
            return false;
        }

        List updatedAttributes = (isInsert)
                ? query.getDbAttributes()
                : ((UpdateBatchQuery) query).getUpdatedAttributes();

        Iterator it = updatedAttributes.iterator();
        while (it.hasNext()) {
            int type = ((DbAttribute) it.next()).getType();
            if (type == Types.CLOB || type == Types.BLOB) {
                return true;
            }
        }

        return false;
    }

    public static Map buildSnapshotForUpdate(DataObject o) {
        DataContext context = o.getDataContext();
        Map committedSnapshot = context.getObjectStore().getSnapshot(
                o.getObjectId(),
                context);
        Map currentSnapshot = o.getDataContext().currentSnapshot(o);
        Map snapshot = null;

        if (committedSnapshot == null || committedSnapshot.isEmpty()) {
            snapshot = Collections.unmodifiableMap(currentSnapshot);
            return snapshot;
        }
        else
            snapshot = new HashMap(currentSnapshot.size());

        Iterator it = currentSnapshot.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String attrName = (String) entry.getKey();
            Object newValue = entry.getValue();
            // if snapshot exists, compare old values and new values,
            // only add attribute to the update clause if the value has changed
            Object oldValue = committedSnapshot.get(attrName);
            if (!Util.nullSafeEquals(oldValue, newValue))
                snapshot.put(attrName, newValue);
        }

        // original snapshot can have extra keys that are missing in current snapshot
        // process those
        Iterator origit = committedSnapshot.entrySet().iterator();
        while (origit.hasNext()) {
            Map.Entry entry = (Map.Entry) origit.next();
            String attrName = (String) entry.getKey();
            Object oldValue = entry.getValue();
            if (oldValue == null || currentSnapshot.containsKey(attrName))
                continue;
            snapshot.put(attrName, null);
        }

        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Creates a snapshot of inserted columns for a given object.
     * 
     * @deprecated since 1.2
     */
    public static Map buildSnapshotForInsert(
            ObjEntity entity,
            DataObject o,
            DbRelationship masterDependentRel) {
        return buildSnapshotForInsert(entity, o, masterDependentRel, false);
    }

    /**
     * Creates a snapshot of inserted columns for a given object. Supports deferring value
     * resolution by setting adding factories to the snapshot instead of rela values if
     * the value is not known yet.
     * 
     * @since 1.2
     */
    public static Map buildSnapshotForInsert(
            ObjEntity entity,
            DataObject o,
            DbRelationship masterDependentRel,
            boolean supportsGeneratedKeys) {

        boolean isMasterDbEntity = (masterDependentRel == null);
        Map map = new HashMap();

        // add object attributes
        Map attrMap = entity.getAttributeMap();
        Iterator attributes = attrMap.entrySet().iterator();
        while (attributes.hasNext()) {
            Map.Entry entry = (Map.Entry) attributes.next();
            String attrName = (String) entry.getKey();
            ObjAttribute objAttr = (ObjAttribute) entry.getValue();

            if (isMasterDbEntity && !objAttr.isCompound()) {
                map.put(objAttr.getDbAttributePath(), o.readPropertyDirectly(attrName));
            }
            else if (!isMasterDbEntity && objAttr.isCompound()) {
                DbAttribute dbAttr = objAttr.getDbAttribute();
                if (dbAttr.getEntity() == masterDependentRel.getTargetEntity())
                    map.put(dbAttr.getName(), o.readPropertyDirectly(attrName));
            }
        }

        // infer keys from relationships
        Map relMap = entity.getRelationshipMap();
        Iterator relationships = relMap.entrySet().iterator();
        while (relationships.hasNext()) {

            Map.Entry entry = (Map.Entry) relationships.next();
            String relName = (String) entry.getKey();
            ObjRelationship rel = (ObjRelationship) entry.getValue();

            if (rel.isSourceIndependentFromTargetChange()) {
                continue;
            }

            DataObject target = (DataObject) o.readPropertyDirectly(relName);
            if (target == null) {
                continue;
            }

            Map targetKeyMap = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects
            if (targetKeyMap == null) {
                continue;
            }

            DbRelationship dbRel;
            if (isMasterDbEntity) {
                dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            }
            else {
                dbRel = (DbRelationship) rel.getDbRelationships().get(1);
                if (dbRel.getSourceEntity() != masterDependentRel.getTargetEntity()) {
                    continue;
                }
            }

            // support deferred propagated values...
            // stick a Factory in the snapshot if the value is not available yet.
            Iterator joins = dbRel.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();
                Object value = targetKeyMap.get(join.getTargetName());
                if (value == null) {
                    if (supportsGeneratedKeys && join.getTarget().isGenerated()) {
                        // setup a factory
                        value = new PropagatedValueFactory(target.getObjectId(), join
                                .getTargetName());
                    }
                    else {
                        throw new CayenneRuntimeException(
                                "Some parts of FK are missing in snapshot, join: " + join);
                    }
                }

                map.put(join.getSourceName(), value);
            }
        }

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map thisIdParts = o.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {
            if (!isMasterDbEntity) {
                thisIdParts = masterDependentRel
                        .targetPkSnapshotWithSrcSnapshot(thisIdParts);
            }
            // put only thise that do not exist in the map
            Iterator itm = thisIdParts.entrySet().iterator();
            while (itm.hasNext()) {
            	Map.Entry entry = (Map.Entry) itm.next();
                Object nextKey = entry.getKey();
                if (!map.containsKey(nextKey))
                    map.put(nextKey, entry.getValue());
            }
        }
        return map;
    }

    private static String getTargetDbAttributeName(
            String srcDbAttributeName,
            DbRelationship masterDependentRel) {
        for (Iterator i = masterDependentRel.getJoins().iterator(); i.hasNext();) {
            DbJoin join = (DbJoin) i.next();
            if (srcDbAttributeName.equals(join.getSourceName()))
                return join.getTargetName();
        }
        return null;
    }

    /**
     * Creates a snapshot of updated columns for a given object.
     */
    public static Map buildSnapshotForUpdate(
            ObjEntity entity,
            DataObject o,
            DbRelationship masterDependentRel) {

        boolean isRootDbEntity = (masterDependentRel == null);
        DataContext context = o.getDataContext();
        DataRow committedSnapshot = context.getObjectStore().getSnapshot(
                o.getObjectId(),
                context);
        DataRow currentSnapshot = o.getDataContext().currentSnapshot(o);
        Map snapshot = new HashMap(currentSnapshot.size());

        // no committed snapshot (why?) - just use values from current snapshot
        if (committedSnapshot == null || committedSnapshot.isEmpty()) {
            Iterator i = currentSnapshot.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                String dbAttrPath = (String) entry.getKey();
                boolean compoundDbAttr = dbAttrPath.indexOf(Entity.PATH_SEPARATOR) > 0;
                Object newValue = entry.getValue();
                if (isRootDbEntity && !compoundDbAttr) {
                    snapshot.put(dbAttrPath, newValue);
                }
                else if (!isRootDbEntity && compoundDbAttr) {
                    Iterator pathIterator = entity.getDbEntity().resolvePathComponents(
                            dbAttrPath);
                    if (pathIterator.hasNext()
                            && masterDependentRel.equals(pathIterator.next())) {
                        DbAttribute dbAttr = (DbAttribute) pathIterator.next();
                        snapshot.put(dbAttr.getName(), newValue);
                    }
                }
                else if (!isRootDbEntity && !compoundDbAttr) {
                    String pkAttrName = getTargetDbAttributeName(
                            dbAttrPath,
                            masterDependentRel);
                    if (pkAttrName != null)
                        snapshot.put(pkAttrName, newValue);
                }
            }
            return Collections.unmodifiableMap(snapshot);
        }

        Iterator it = currentSnapshot.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String dbAttrPath = (String) entry.getKey();
            boolean compoundDbAttr = dbAttrPath.indexOf(Entity.PATH_SEPARATOR) > 0;
            Object newValue = entry.getValue();

            // ... if not for flattened attributes, we could've used
            // DataRow.createDiff()..

            // if snapshot exists, compare old values and new values,
            // only add attribute to the update clause if the value has changed
            Object oldValue = committedSnapshot.get(dbAttrPath);
            if (!Util.nullSafeEquals(oldValue, newValue)) {

                if (!isRootDbEntity) {
                    if (compoundDbAttr) {
                        Iterator pathIterator = entity
                                .getDbEntity()
                                .resolvePathComponents(dbAttrPath);
                        if (pathIterator.hasNext()
                                && masterDependentRel.equals(pathIterator.next())) {
                            DbAttribute dbAttr = (DbAttribute) pathIterator.next();
                            snapshot.put(dbAttr.getName(), newValue);
                        }
                    }
                    else {
                        String pkAttrName = getTargetDbAttributeName(
                                dbAttrPath,
                                masterDependentRel);
                        if (pkAttrName != null)
                            snapshot.put(pkAttrName, newValue);
                    }
                }
                else if (!compoundDbAttr) {
                    snapshot.put(dbAttrPath, newValue);
                }
            }
        }

        // original snapshot can have extra keys that are missing in current snapshot
        // process those
        Iterator origit = committedSnapshot.entrySet().iterator();
        while (origit.hasNext()) {
            Map.Entry entry = (Map.Entry) origit.next();
            String dbAttrPath = (String) entry.getKey();
            if (entry.getValue() == null || currentSnapshot.containsKey(dbAttrPath)) {
                continue;
            }

            boolean compoundDbAttr = dbAttrPath.indexOf(Entity.PATH_SEPARATOR) > 0;

            if (isRootDbEntity && !compoundDbAttr) {
                snapshot.put(dbAttrPath, null);
            }
            else if (!isRootDbEntity && compoundDbAttr) {
                Iterator pathIterator = entity.getDbEntity().resolvePathComponents(
                        dbAttrPath);
                if (pathIterator.hasNext()
                        && masterDependentRel.equals(pathIterator.next())) {
                    DbAttribute dbAttr = (DbAttribute) pathIterator.next();
                    snapshot.put(dbAttr.getName(), null);
                }
            }
            else if (!isRootDbEntity && !compoundDbAttr) {
                String pkAttrName = getTargetDbAttributeName(
                        dbAttrPath,
                        masterDependentRel);
                if (pkAttrName != null)
                    snapshot.put(pkAttrName, null);
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    final static class PropagatedValueFactory implements Factory {

        ObjectId masterID;
        String masterKey;

        PropagatedValueFactory(ObjectId masterID, String masterKey) {
            this.masterID = masterID;
            this.masterKey = masterKey;
        }

        public Object create() {
            if (!masterID.isReplacementIdAttached()) {
                throw new CayenneRuntimeException("Deferred propagated key ("
                        + masterKey
                        + ") wasn't generated for object with ID "
                        + masterID);
            }

            Map replacementId = masterID.getReplacementIdMap();
            Object value = replacementId.get(masterKey);
            if (value == null) {
                throw new CayenneRuntimeException("Deferred propagated key ("
                        + masterKey
                        + ") wasn't generated for object with ID "
                        + masterID);
            }

            return value;
        }
    }
}