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

import java.util.*;

import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.*;

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

			ObjEntity targetEntity=(ObjEntity)rel.getTargetEntity();
			Class targetClass;
			try {
				targetClass = Class.forName(targetEntity.getClassName());
			} catch (ClassNotFoundException e) {
				throw new CayenneRuntimeException("Failed to load class for name "+targetEntity.getClassName()+" because "+e.getMessage());
			}
            ObjectId destId = new ObjectId(targetClass, destMap);
            anObject.writePropertyDirectly(
                rel.getName(),
                context.registeredObject(destId));
        }
        anObject.setPersistenceState(PersistenceState.COMMITTED);
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
        Map oldSnap = context.getObjectStore().getSnapshot(anObject.getObjectId());

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
                    new ToManyList(relDataSource, anObject.getObjectId(), rel.getName());
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
}
