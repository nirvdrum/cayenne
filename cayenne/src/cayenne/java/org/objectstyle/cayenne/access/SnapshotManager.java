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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.Util;

/**
 * SnapshotManager handles snapshot (data row) operations on objects.
 * This is a helper class that works in conjunction with DataContext.
 *
 * @author Andrei Adamchik
 */
public class SnapshotManager {

	protected ToManyListDataSource relDataSource;

	/**
	 * Constructor for SnapshotManager.
	 */
	public SnapshotManager(ToManyListDataSource relDataSource) {
		this.relDataSource = relDataSource;
	}

	/**
	 * Replaces all object attribute values with snapshot values.
	 * Sets object state to COMMITTED.
	 */
	public void refreshObjectWithSnapshot(
		ObjEntity ent,
		DataObject anObject,
		Map snapshot) {

		DataContext context = anObject.getDataContext();

		Map attrMap = ent.getAttributeMap();
		Iterator it = attrMap.keySet().iterator();
		boolean isPartialSnapshot=false;
		while (it.hasNext()) {
			String attrName = (String) it.next();
			ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);
//			String dbAttrName=attr.getDbAttribute().getName();

            String dbAttrPath = attr.getDbAttributePath();
			anObject.writePropertyDirectly(
				attrName,
				snapshot.get(dbAttrPath));
			if(!snapshot.containsKey(dbAttrPath)) {
				//Note the distinction between
				// 1) the map returning null because there was no mapping
				// for that key and
				// 2) returning null because 'null' was the value mapped
				// for that key.
				// If the first case (this clause) then snapshot is only partial
				isPartialSnapshot=true;
			}
		}

		Iterator rit = ent.getRelationshipList().iterator();
		while (rit.hasNext()) {
			ObjRelationship rel = (ObjRelationship) rit.next();
			if (rel.isToMany()) {
				// "to many" relationships have no information to collect from snapshot
				// rather we need to check if a relationship list exists, if not -
				// create an empty one.

				ToManyList relList =
					new ToManyList(
						relDataSource,
						anObject.getObjectId(),
						rel.getName());
				anObject.writePropertyDirectly(rel.getName(), relList);
				continue;
			}

			DbRelationship dbRel =
				(DbRelationship) rel.getDbRelationshipList().get(0);

			// dependent to one relationship is optional and can be null.
			if (dbRel.isToDependentPK()) {
				continue;
			}

			Map destMap = dbRel.targetPkSnapshotWithSrcSnapshot(snapshot);
			if (destMap == null) {
				continue;
			}

			ObjEntity targetEntity = (ObjEntity) rel.getTargetEntity();
			Class targetClass;
			try {
				targetClass = Class.forName(targetEntity.getClassName());
			} catch (ClassNotFoundException e) {
				throw new CayenneRuntimeException(
					"Failed to load class for name "
						+ targetEntity.getClassName()
						+ " because "
						+ e.getMessage());
			}
			ObjectId destId = new ObjectId(targetClass, destMap);
			anObject.writePropertyDirectly(
				rel.getName(),
				context.registeredObject(destId));
		}
		if(isPartialSnapshot) {
			anObject.setPersistenceState(PersistenceState.HOLLOW);
		} else {
			anObject.setPersistenceState(PersistenceState.COMMITTED);
		}

	}

	public void mergeObjectWithSnapshot(
		ObjEntity ent,
		DataObject anObject,
		Map snapshot) {

		if (anObject.getPersistenceState() == PersistenceState.HOLLOW) {
			refreshObjectWithSnapshot(ent, anObject, snapshot);
			return;
		}

		DataContext context = anObject.getDataContext();
		Map oldSnap =
			context.getObjectStore().getSnapshot(anObject.getObjectId());

		Map attrMap = ent.getAttributeMap();
		Iterator it = attrMap.keySet().iterator();
		while (it.hasNext()) {
			String attrName = (String) it.next();
			ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);
//			String dbAttrName = attr.getDbAttribute().getName();
//
//			Object curVal = anObject.readPropertyDirectly(attrName);
//			Object oldVal = oldSnap.get(dbAttrName);
//			Object newVal = snapshot.get(dbAttrName);

            //processing compound attributes correctly
            String dbAttrPath = attr.getDbAttributePath();
            Object curVal = anObject.readPropertyDirectly(attrName);
			Object oldVal = oldSnap.get(dbAttrPath);
			Object newVal = snapshot.get(dbAttrPath);

			// if value not modified, update it from snapshot,
			// otherwise leave it alone
			if (Util.nullSafeEquals(curVal, oldVal)
				&& !Util.nullSafeEquals(newVal, curVal)) {
				anObject.writePropertyDirectly(attrName, newVal);
			}
		}
	}

	/**
	 * Initializes to-many relationships of a DataObject
	 * with empty lists.
	 */
	public void prepareForInsert(ObjEntity ent, DataObject anObject) {
		Iterator it = ent.getRelationshipList().iterator();
		while (it.hasNext()) {
			ObjRelationship rel = (ObjRelationship) it.next();
			if (rel.isToMany()) {
				ToManyList relList =
					new ToManyList(
						relDataSource,
						anObject.getObjectId(),
						rel.getName());
				anObject.writePropertyDirectly(rel.getName(), relList);
			}
		}
	}

	/**
	 * Takes a snapshot of current object state.
	 */
	public Map takeObjectSnapshot(ObjEntity ent, DataObject anObject) {
		Map map = new HashMap();

		Map attrMap = ent.getAttributeMap();
		Iterator it = attrMap.keySet().iterator();
		while (it.hasNext()) {
			String attrName = (String) it.next();
            ObjAttribute objAttr = (ObjAttribute) attrMap.get(attrName);
//			DbAttribute dbAttr = objAttr.getDbAttribute();
//			map.put(dbAttr.getName(), anObject.readPropertyDirectly(attrName));

            //processing compound attributes correctly
            map.put(objAttr.getDbAttributePath(), anObject.readPropertyDirectly(attrName));
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

			DataObject target =
				(DataObject) anObject.readPropertyDirectly(relName);
			if (target == null) {
				continue;
			}

			DbRelationship dbRel =
				(DbRelationship) rel.getDbRelationshipList().get(0);
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
	public void mergePrefetchResultsRelationships(
		List rootObjects,
		ObjRelationship theRelationship,
		List destinationObjects) {

		if(rootObjects.size()==0) {
			return; //Nothing to do... avoid array index exceptions
		}

		Class sourceObjectClass=((DataObject)rootObjects.get(0)).getClass();

		ObjRelationship reverseRelationship =
			theRelationship.getReverseRelationship();
		//Might be used later on... obtain and cast only once
		DbRelationship dbRelationship =
			(DbRelationship) theRelationship.getDbRelationshipList().get(0);

		Factory listFactory = new Factory() {
			public Object create() {
				return new ArrayList();
			}
		};
		Map toManyLists = MapUtils.lazyMap(new HashMap(), listFactory);

		Iterator destIterator = destinationObjects.iterator();
		while (destIterator.hasNext()) {
			DataObject thisDestinationObject = (DataObject) destIterator.next();
			DataObject sourceObject=null;
			if (reverseRelationship != null) {
				sourceObject =
					(DataObject) thisDestinationObject.readPropertyDirectly(
						reverseRelationship.getName());
			} else {
				//Reverse relationship doesn't exist... match objects manually
				DataContext context = thisDestinationObject.getDataContext();
				Map sourcePk =
					dbRelationship.srcPkSnapshotWithTargetSnapshot(
						context.getObjectStore().getSnapshot(
							thisDestinationObject.getObjectId()));
				sourceObject =
					context.registeredObject(
						new ObjectId(sourceObjectClass, sourcePk));
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
				(ToManyList) thisRoot.readPropertyDirectly(
					theRelationship.getName());

			toManyList.setObjectList((List) toManyLists.get(thisRoot));
		}
	}

}
