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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Entity;

/**
 * Collection of utility methods to work with BatchQueries.
 *
 * @author Andriy Shapochka
 */

public class BatchQueryUtils {

    private BatchQueryUtils() {
    }

    public static Map buildSnapshotForUpdate(DataObject o) {
        Map committedSnapshot = o.getCommittedSnapshot();
        Map currentSnapshot = o.getCurrentSnapshot();
        Map snapshot = null;

        if (committedSnapshot == null || committedSnapshot.isEmpty()) {
            snapshot = Collections.unmodifiableMap(currentSnapshot);
            return snapshot;
        } else snapshot = new HashMap(currentSnapshot.size());

        Iterator it = currentSnapshot.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String attrName = (String)entry.getKey();
            Object newValue = entry.getValue();
            // if snapshot exists, compare old values and new values,
            // only add attribute to the update clause if the value has changed
            Object oldValue = committedSnapshot.get(attrName);
            if (valueChanged(oldValue, newValue)) snapshot.put(attrName, newValue);
        }

        // original snapshot can have extra keys that are missing in current snapshot
        // process those
        Iterator origit = committedSnapshot.entrySet().iterator();
        while (origit.hasNext()) {
            Map.Entry entry = (Map.Entry)origit.next();
            String attrName = (String) entry.getKey();
            Object oldValue = entry.getValue();
            if (oldValue == null || currentSnapshot.containsKey(attrName)) continue;
            snapshot.put(attrName, null);
        }

        return Collections.unmodifiableMap(snapshot);
    }

    public static int hashCode(Collection c) {
        HashCodeBuilder builder = new HashCodeBuilder();
        for (Iterator i = c.iterator(); i.hasNext();) builder.append(i.next());
        return builder.toHashCode();
    }

    public static Map buildFlattenedSnapshot(Map sourceId,
											    Map destinationId,
											    DbRelationship firstRelationship,
											    DbRelationship secondRelationship) {
        Map snapshot = new HashMap(sourceId.size() + destinationId.size());
        List joins = firstRelationship.getJoins();
        for (int i = 0, numJoins = joins.size(); i < numJoins; i++) {
            DbAttributePair thisJoin = (DbAttributePair) joins.get(i);
            DbAttribute sourceAttribute = thisJoin.getSource();
            DbAttribute targetAttribute = thisJoin.getTarget();
            snapshot.put(targetAttribute.getName(), sourceId.get(sourceAttribute.getName()));
        }

        joins = secondRelationship.getJoins();
		for (int i = 0, numJoins = joins.size(); i < numJoins; i++) {
            DbAttributePair thisJoin = (DbAttributePair) joins.get(i);
            DbAttribute sourceAttribute = thisJoin.getSource();
            DbAttribute targetAttribute = thisJoin.getTarget();
            snapshot.put(sourceAttribute.getName(), destinationId.get(targetAttribute.getName()));
        }

        return snapshot;
    }

    public static Map buildSnapshotForInsert(ObjEntity ent,
            DataObject o,
            DbRelationship masterDependentRel) {
        boolean isMasterDbEntity = (masterDependentRel == null);
        Map map = new HashMap();

        Map attrMap = ent.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute objAttr = (ObjAttribute) attrMap.get(attrName);

            if (isMasterDbEntity && !objAttr.isCompound()) {
                map.put(objAttr.getDbAttributePath(),
                        o.readPropertyDirectly(attrName));
            } else if (!isMasterDbEntity && objAttr.isCompound()) {
                DbAttribute dbAttr = objAttr.getDbAttribute();
                if (dbAttr.getEntity() == masterDependentRel.getTargetEntity())
                    map.put(dbAttr.getName(),
                            o.readPropertyDirectly(attrName));
            }
        }

        if (isMasterDbEntity) {
            Map relMap = ent.getRelationshipMap();
            Iterator itr = relMap.keySet().iterator();
            while (itr.hasNext()) {
                String relName = (String) itr.next();
                ObjRelationship rel = (ObjRelationship)relMap.get(relName);
                // to-many will be handled on the other side
                if (rel.isToMany()) continue;
                if (rel.isToDependentEntity()) continue;
                DataObject target = (DataObject) o.readPropertyDirectly(relName);
                if (target == null) continue;
                DbRelationship dbRel = (DbRelationship)rel.getDbRelationships().get(0);
                Map idParts = target.getObjectId().getIdSnapshot();
                // this may happen in uncommitted objects
                if (idParts == null) continue;
                Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
                map.putAll(fk);
            }
        } else {
            Map relMap = ent.getRelationshipMap();
            Iterator itr = relMap.keySet().iterator();
            while (itr.hasNext()) {
                String relName = (String) itr.next();
                ObjRelationship rel = (ObjRelationship)relMap.get(relName);
                DbRelationship dbRel = (DbRelationship)rel.getDbRelationships().get(1);
                if (rel.isToMany()) continue;
                if (dbRel.isToDependentPK()) continue;
                DataObject target = (DataObject) o.readPropertyDirectly(relName);
                if (target == null) continue;
                if (dbRel.getSourceEntity() != masterDependentRel.getTargetEntity()) continue;
                Map idParts = target.getObjectId().getIdSnapshot();
                // this may happen in uncommitted objects
                if (idParts == null) continue;
                Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
                map.putAll(fk);
            }
        }

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map thisIdParts = o.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {
            if (!isMasterDbEntity) {
                thisIdParts = masterDependentRel.
                              targetPkSnapshotWithSrcSnapshot(thisIdParts);
            }
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

//    public static Map buildSnapshotForUpdate(ObjEntity ent,
//            DataObject o,
//            DbRelationship masterDependentRel) {
//        boolean isMasterDbEntity = (masterDependentRel == null);
//        Map committedSnapshot = o.getCommittedSnapshot();
//        committedSnapshot = (committedSnapshot == null ||
//                             committedSnapshot.isEmpty() ?
//                             Collections.EMPTY_MAP :
//                             committedSnapshot);
//        Map map = new HashMap();
//
//        Map attrMap = ent.getAttributeMap();
//        Iterator it = attrMap.keySet().iterator();
//        while (it.hasNext()) {
//            String attrName = (String) it.next();
//            ObjAttribute objAttr = (ObjAttribute) attrMap.get(attrName);
//
//            Object oldValue = committedSnapshot.get(objAttr.getDbAttributePath());
//            Object newValue = o.readPropertyDirectly(attrName);
//
//            // if snapshot exists, compare old values and new values,
//            // only add attribute to the update clause if the value has changed
//            if (!valueChanged(oldValue, newValue)) continue;
//
//            if (isMasterDbEntity && !objAttr.isCompound()) {
//                map.put(objAttr.getDbAttributePath(), newValue);
//            } else if (!isMasterDbEntity && objAttr.isCompound()) {
//                DbAttribute dbAttr = objAttr.getDbAttribute();
//                if (dbAttr.getEntity() == masterDependentRel.getTargetEntity())
//                    map.put(dbAttr.getName(), newValue);
//            }
//        }
//
//        if (isMasterDbEntity) {
//            Map relMap = ent.getRelationshipMap();
//            Iterator itr = relMap.keySet().iterator();
//            while (itr.hasNext()) {
//                String relName = (String) itr.next();
//                ObjRelationship rel = (ObjRelationship) relMap.get(relName);
//
//                // to-many will be handled on the other side
//                if (rel.isToMany() || rel.isToDependentEntity()) continue;
//
//                DbRelationship dbRel =
//                        (DbRelationship) rel.getDbRelationshipList().get(0);
//
//                DataObject target = (DataObject) o.readPropertyDirectly(relName);
//
//                if (target == null) {
//                    for (Iterator i = dbRel.getJoins().iterator(); i.hasNext(); ) {
//                        DbAttributePair join = (DbAttributePair)i.next();
//                        String dbAttrName = join.getSource().getName();
//                        if (committedSnapshot.get(dbAttrName) != null) {
//                            map.put(dbAttrName, null);
//                        }
//                    }
//                    continue;
//                }
//
//                Map idParts = target.getObjectId().getIdSnapshot();
//
//                // this may happen in uncommitted objects
//                if (idParts == null) {
//                    continue;
//                }
//
//                Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
//                for (Iterator i = fk.entrySet().iterator(); i.hasNext(); ) {
//                    Map.Entry entry = (Map.Entry)i.next();
//                    Object key = entry.getKey();
//                    Object oldValue = committedSnapshot.get(key);
//                    Object newValue = entry.getValue();
//
//                    // if snapshot exists, compare old values and new values,
//                    // only add attribute to the update clause if the value has changed
//                    if (!valueChanged(oldValue, newValue)) continue;
//
//                    map.put(key, newValue);
//                }
//            }
//        }
//
//        // process object id map
//        // we should ignore any object id values if a corresponding attribute
//        // is a part of relationship "toMasterPK", since those values have been
//        // set above when db relationships where processed.
//        Map thisIdParts = o.getObjectId().getIdSnapshot();
//        if (thisIdParts != null) {
//            Map id = (isMasterDbEntity ?
//                      thisIdParts :
//                      masterDependentRel.
//                      targetPkSnapshotWithSrcSnapshot(thisIdParts));
//            // put only thise that do not exist in the map
//            Iterator itm = id.keySet().iterator();
//            while (itm.hasNext()) {
//                Object nextKey = itm.next();
//                Object newValue = id.get(nextKey);
//                if (!map.containsKey(nextKey)) {
//                    Object committedKey = (isMasterDbEntity ?
//                            nextKey :
//                            getSrcDbAttributeName((String)nextKey, masterDependentRel));
//                    Object oldValue = committedSnapshot.get(committedKey);
//                    if (!valueChanged(oldValue, newValue)) continue;
//                    map.put(nextKey, newValue);
//                }
//            }
//        }
//        return map;
//	}

    private static boolean valueChanged(Object oldValue, Object newValue) {
        return ((newValue == null && oldValue != null) ||
                (newValue != null && !newValue.equals(oldValue)));
    }

/*
    private static String getSrcDbAttributeName(
            String targetDbAttributeName,
            DbRelationship masterDependentRel) {
        for (Iterator i = masterDependentRel.getJoins().iterator(); i.hasNext(); ) {
            DbAttributePair join = (DbAttributePair)i.next();
            if (targetDbAttributeName.equals(join.getTarget().getName()))
                return join.getSource().getName();
        }
        return null;
    }
*/

    private static String getTargetDbAttributeName(
            String srcDbAttributeName,
            DbRelationship masterDependentRel) {
        for (Iterator i = masterDependentRel.getJoins().iterator(); i.hasNext(); ) {
            DbAttributePair join = (DbAttributePair)i.next();
            if (srcDbAttributeName.equals(join.getSource().getName()))
                return join.getTarget().getName();
        }
        return null;
    }

    public static Map buildSnapshotForUpdate(ObjEntity entity,
            DataObject o,
            DbRelationship masterDependentRel) {
        boolean isMasterDbEntity = (masterDependentRel == null);
        Map committedSnapshot = o.getCommittedSnapshot();
        Map currentSnapshot = o.getCurrentSnapshot();
        Map snapshot = new HashMap(currentSnapshot.size());

        if (committedSnapshot == null || committedSnapshot.isEmpty()) {
            for (Iterator i = currentSnapshot.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                String dbAttrPath = (String)entry.getKey();
                boolean compoundDbAttr = dbAttrPath.indexOf(Entity.PATH_SEPARATOR) > 0;
                Object newValue = entry.getValue();
                if (isMasterDbEntity && !compoundDbAttr) {
                    snapshot.put(dbAttrPath, newValue);
                } else if (!isMasterDbEntity && compoundDbAttr) {
                    Iterator pathIterator =
                            entity.getDbEntity().resolvePathComponents(dbAttrPath);
                    if (pathIterator.hasNext() &&
                        masterDependentRel.equals(pathIterator.next())) {
                        DbAttribute dbAttr = (DbAttribute)pathIterator.next();
                        snapshot.put(dbAttr.getName(), newValue);
                    }
                } else if (!isMasterDbEntity && !compoundDbAttr) {
                    String pkAttrName = getTargetDbAttributeName(dbAttrPath,
                            masterDependentRel);
                    if (pkAttrName != null)
                        snapshot.put(pkAttrName, newValue);
                }
            }
            return Collections.unmodifiableMap(snapshot);
        }

        Iterator it = currentSnapshot.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String dbAttrPath = (String)entry.getKey();
            boolean compoundDbAttr = dbAttrPath.indexOf(Entity.PATH_SEPARATOR) > 0;
            Object newValue = entry.getValue();
            // if snapshot exists, compare old values and new values,
            // only add attribute to the update clause if the value has changed
            Object oldValue = committedSnapshot.get(dbAttrPath);
            if (valueChanged(oldValue, newValue)) {
                if (isMasterDbEntity && !compoundDbAttr) {
                    snapshot.put(dbAttrPath, newValue);
                } else if (!isMasterDbEntity && compoundDbAttr) {
                    Iterator pathIterator =
                            entity.getDbEntity().resolvePathComponents(dbAttrPath);
                    if (pathIterator.hasNext() &&
                        masterDependentRel.equals(pathIterator.next())) {
                        DbAttribute dbAttr = (DbAttribute)pathIterator.next();
                        snapshot.put(dbAttr.getName(), newValue);
                    }
                } else if (!isMasterDbEntity && !compoundDbAttr) {
                    String pkAttrName = getTargetDbAttributeName(dbAttrPath,
                            masterDependentRel);
                    if (pkAttrName != null)
                        snapshot.put(pkAttrName, newValue);
                }
            }
        }

        // original snapshot can have extra keys that are missing in current snapshot
        // process those
        Iterator origit = committedSnapshot.entrySet().iterator();
        while (origit.hasNext()) {
            Map.Entry entry = (Map.Entry)origit.next();
            String dbAttrPath = (String) entry.getKey();
            boolean compoundDbAttr = dbAttrPath.indexOf(Entity.PATH_SEPARATOR) > 0;
            Object oldValue = entry.getValue();
            if (oldValue == null || currentSnapshot.containsKey(dbAttrPath)) continue;
            if (isMasterDbEntity && !compoundDbAttr) {
                snapshot.put(dbAttrPath, null);
            } else if (!isMasterDbEntity && compoundDbAttr) {
                Iterator pathIterator =
                        entity.getDbEntity().resolvePathComponents(dbAttrPath);
                if (pathIterator.hasNext() &&
                    masterDependentRel.equals(pathIterator.next())) {
                    DbAttribute dbAttr = (DbAttribute)pathIterator.next();
                    snapshot.put(dbAttr.getName(), null);
                }
            } else if (!isMasterDbEntity && !compoundDbAttr) {
                String pkAttrName = getTargetDbAttributeName(dbAttrPath,
                        masterDependentRel);
                if (pkAttrName != null)
                    snapshot.put(pkAttrName, null);
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }
}