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
import org.apache.commons.collections.*;
import org.objectstyle.ashwood.graph.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.dba.*;

/**
 * PrimaryKeyGenerationSupport resolves primary key dependencies for entities
 * related to the supported query engine via topological sorting. It is
 * directly based on ASHWOOD. In addition it provides means for primary key
 * generation relying on DbAdapter in this. PrimaryKeyGenerationSupport is
 * used by ContextCommit.
 *
 * @author Andriy Shapochka
 */

class PrimaryKeyGenerationSupport {
  private Map indexedDbEntities;
  private QueryEngine supportedQueryEngine;
  private DbEntityComparator dbEntityComparator;
  private ObjEntityComparator objEntityComparator;

  public PrimaryKeyGenerationSupport(QueryEngine queryEngineToSupport) {
    supportedQueryEngine = queryEngineToSupport;
    init();
    dbEntityComparator = new DbEntityComparator();
    objEntityComparator = new ObjEntityComparator();
  }

  public void reset() {
    init();
  }

  public Comparator getDbEntityComparator() {
    return dbEntityComparator;
  }

  public Comparator getObjEntityComparator() {
    return objEntityComparator;
  }

  public Object generatePrimaryKeyValue(ObjEntity objEntity) throws CayenneException {
    DbEntity dbEntity = objEntity.getDbEntity();
    DataNode owner = supportedQueryEngine.dataNodeForObjEntity(objEntity);
    try {
      PkGenerator gen = owner.getAdapter().getPkGenerator();
      Object pk = gen.generatePkForDbEntity(owner, dbEntity);
      return pk;
    } catch (Exception ex) {
      throw new CayenneException("Error generating PK: " + ex.getMessage(), ex);
    }
  }

  public void createPermIdsForObjEntity(List dataObjects) throws CayenneException {
    if (dataObjects.isEmpty()) return;
    TempObjectId tempId = (TempObjectId) ((DataObject)dataObjects.get(0)).getObjectId();
    ObjEntity objEntity = supportedQueryEngine.getEntityResolver().lookupObjEntity(tempId.getObjClass());
    DbEntity dbEntity = objEntity.getDbEntity();
    DataNode owner = supportedQueryEngine.dataNodeForObjEntity(objEntity);
    PkGenerator pkGenerator = owner.getAdapter().getPkGenerator();

    HashMap idMap = null;
    boolean pkFromMaster = true;
    for (Iterator i = dataObjects.iterator(); i.hasNext();) {
      idMap = new HashMap(idMap != null ? idMap.size() : 1);
      DataObject object = (DataObject)i.next();
      tempId = (TempObjectId)object.getObjectId();
      // first get values delivered via relationships
      if (pkFromMaster) pkFromMaster = appendPkFromMasterRelationships(idMap, object, objEntity, dbEntity);
      boolean autoPkDone = false;
      Iterator it = dbEntity.getPrimaryKey().iterator();
      while (it.hasNext()) {
        DbAttribute dbAttr = (DbAttribute) it.next();
        String dbAttrName = dbAttr.getName();
        if (idMap.containsKey(dbAttrName)) continue;
        ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);
        if (objAttr != null) {
          idMap.put(dbAttrName, object.readPropertyDirectly(objAttr.getName()));
          continue;
        }
        if (autoPkDone) throw new CayenneException("Primary Key autogeneration only works for a single attribute.");
        try {
          Object pkValue = pkGenerator.generatePkForDbEntity(owner, dbEntity);
          idMap.put(dbAttrName, pkValue);
          autoPkDone = true;
        } catch (Exception ex) {
          throw new CayenneException("Error generating PK: " + ex.getMessage(), ex);
        }
      }
      ObjectId permId = new ObjectId(objEntity.getClass(), idMap);
      // note that object registration did not changed (new id is not attached to context, only to temp. oid)
      tempId.setPermId(permId);
    }
  }

  public ObjectId createPermId(DataObject object) throws CayenneException {
    TempObjectId tempId = (TempObjectId) object.getObjectId();
    ObjEntity objEntity = supportedQueryEngine.getEntityResolver().lookupObjEntity(tempId.getObjClass());
    DbEntity dbEntity = objEntity.getDbEntity();

    HashMap idMap = new HashMap();
    // first get values delivered via relationships
    appendPkFromMasterRelationships(idMap, object, objEntity, dbEntity);
    boolean autoPkDone = false;
    Iterator it = dbEntity.getPrimaryKey().iterator();
    while (it.hasNext()) {
      DbAttribute dbAttr = (DbAttribute) it.next();
      String dbAttrName = dbAttr.getName();
      if (idMap.containsKey(dbAttrName)) continue;
      ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);
      if (objAttr != null) {
        idMap.put(dbAttrName, object.readPropertyDirectly(objAttr.getName()));
        continue;
      }
      if (autoPkDone) throw new CayenneException("Primary Key autogeneration only works for a single attribute.");
      Object pkValue = generatePrimaryKeyValue(objEntity);
      idMap.put(dbAttrName, pkValue);
      autoPkDone = true;
    }
    ObjectId permId = new ObjectId(objEntity.getClass(), idMap);
    // note that object registration did not changed (new id is not attached to context, only to temp. oid)
    tempId.setPermId(permId);
    return permId;
  }

  private boolean appendPkFromMasterRelationships(Map map, DataObject dataObject, ObjEntity objEntity, DbEntity dbEntity) throws CayenneException {
    boolean useful = false;
    Iterator it = dbEntity.getRelationshipMap().values().iterator();
    while (it.hasNext()) {
      DbRelationship dbRel = (DbRelationship) it.next();
      if (!dbRel.isToMasterPK()) continue;

      ObjRelationship rel = objEntity.getRelationshipForDbRelationship(dbRel);
      if (rel == null) continue;

      DataObject targetDo = (DataObject) dataObject.readPropertyDirectly(rel.getName());
      if (targetDo == null)
          throw new CayenneException("Null master object, can't create primary key.");

      ObjectId targetKey = targetDo.getObjectId();
      Map idMap = targetKey.getIdSnapshot();
      if (idMap == null) throw new CayenneException(noMasterPkMsg(objEntity.getName(), targetKey.getObjClass().toString(), dbRel.getName()));
      map.putAll(dbRel.srcFkSnapshotWithTargetSnapshot(idMap));
      useful = true;
    }
    return useful;
  }

  private String noMasterPkMsg(String src, String dst, String rel) {
    StringBuffer msg = new StringBuffer("Can't create primary key, master object has no PK snapshot.");
    msg.append("\nrelationship name: ").append(rel).append(", src object: ").append(src).append(", target obj: ").append(dst);
    return msg.toString();
  }

  private List collectAllDbEntities() {
    List entities = new ArrayList();
    for (Iterator i = supportedQueryEngine.dataMapIterator(); i.hasNext();) {
      DataMap dataMap = (DataMap)i.next();
      CollectionUtils.addAll(entities, dataMap.getDbEntities());
    }
    return entities;
  }

  private void init() {
    List dbEntitiesToResolve = collectAllDbEntities();
    Digraph pkDependencyGraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
    indexedDbEntities = new HashMap(dbEntitiesToResolve.size());
    for (Iterator i = dbEntitiesToResolve.iterator(); i.hasNext();) {
      DbEntity origin = (DbEntity)i.next();
      for (Iterator j = origin.getRelationshipMap().values().iterator(); j.hasNext();) {
        DbRelationship relation = (DbRelationship)j.next();
        if (relation.isToDependentPK()) {
          DbEntity dst = (DbEntity)relation.getTargetEntity();
          if (origin.equals(dst)) continue;
          pkDependencyGraph.putArc(origin, dst, Boolean.TRUE);
        }
      }
    }
    int index = 0;
    for (Iterator i = dbEntitiesToResolve.iterator(); i.hasNext();) {
      DbEntity entity = (DbEntity)i.next();
      if (!pkDependencyGraph.containsVertex(entity)) indexedDbEntities.put(entity, new Integer(index++));
    }
    boolean acyclic = GraphUtils.isAcyclic(pkDependencyGraph);
    if (acyclic) {
      IndegreeTopologicalSort sorter = new IndegreeTopologicalSort(pkDependencyGraph);
      while (sorter.hasNext()) indexedDbEntities.put(sorter.next(), new Integer(index++));
    } else {
      StrongConnection contractor = new StrongConnection(pkDependencyGraph, CollectionFactory.ARRAYLIST_FACTORY);
      Digraph contractedDigraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
      contractor.contract(contractedDigraph, CollectionFactory.ARRAYLIST_FACTORY);
      IndegreeTopologicalSort sorter = new IndegreeTopologicalSort(contractedDigraph);
      while (sorter.hasNext()) {
        Collection component = (Collection)sorter.next();
        for (Iterator i = component.iterator(); i.hasNext();) indexedDbEntities.put(i.next(), new Integer(index++));
      }
    }
  }

  private class DbEntityComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1.equals(o2)) return 0;
      Integer index1 = (Integer)indexedDbEntities.get((DbEntity)o1);
      Integer index2 = (Integer)indexedDbEntities.get((DbEntity)o2);
      return ComparatorUtils.NATURAL_COMPARATOR.compare(index1, index2);
    }
  }

  private class ObjEntityComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1.equals(o2)) return 0;
      DbEntity e1 = ((ObjEntity)o1).getDbEntity();
      DbEntity e2 = ((ObjEntity)o2).getDbEntity();
      return dbEntityComparator.compare(e1, e2);
    }
  }
}