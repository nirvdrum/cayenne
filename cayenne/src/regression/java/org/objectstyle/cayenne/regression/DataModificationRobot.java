package org.objectstyle.cayenne.regression;

import java.util.*;
import org.apache.commons.collections.*;
import org.objectstyle.ashwood.random.*;
import org.objectstyle.ashwood.dbutil.*;
import org.objectstyle.ashwood.graph.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;

public class DataModificationRobot {
  private DataContext context;
  private Roulette insertionRandomizer;
  private Roulette deletionRandomizer;
  private List objEntities;
  private Map objectsByEntity = new SequencedHashMap();
  private Random randomizer;

  public DataModificationRobot(DataContext context, Random randomizer, int newObjectPerEntityCount, int deleteObjectPerEntityCount) {
    this.context = context;
    this.randomizer = randomizer;
    objEntities = new ArrayList();
    for (Iterator i = context.dataMapIterator(); i.hasNext();) {
      DataMap dataMap = (DataMap)i.next();
      objEntities.addAll(dataMap.getObjEntitiesAsList(false));
    }
    insertionRandomizer = new Roulette(objEntities.size(), newObjectPerEntityCount, randomizer);
    deletionRandomizer = new Roulette(objEntities.size(), deleteObjectPerEntityCount, randomizer);
  }

  public void generate() {
    insertRandomData();
    deleteRandomData();
  }

  private void insertRandomData() {
    while (insertionRandomizer.hasNext()) {
      Integer entityIndex = (Integer)insertionRandomizer.next();
      ObjEntity entity = (ObjEntity)objEntities.get(entityIndex.intValue());
      DataObject o = context.createAndRegisterNewObject(entity.getName());
      List objects = (List)objectsByEntity.get(entity);
      if (objects == null) {
        objects = new ArrayList();
        objectsByEntity.put(entity, objects);
      }
      objects.add(o);
    }
    for (Iterator i = objectsByEntity.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry)i.next();
      ObjEntity entity = (ObjEntity)entry.getKey();
      List objects = (List)entry.getValue();
      List relationships = entity.getRelationshipList();
      ObjRelationship dependentPkRelation = null;
      for (Iterator k = relationships.iterator(); k.hasNext();) {
        ObjRelationship r = (ObjRelationship)k.next();
        if (!r.isToMany() && r.getReverseRelationship().isToDependentEntity()) {
          dependentPkRelation = r;
          List targetObjects = (List)objectsByEntity.get(dependentPkRelation.getTargetEntity());
          Roulette masterPkEntitySelector = new Roulette(targetObjects.size(), 1, randomizer);
          for (Iterator j = objects.iterator(); j.hasNext();) {
            DataObject o = (DataObject)j.next();
            int targetIndex = ((Integer)masterPkEntitySelector.next()).intValue();
            o.setToOneTarget(dependentPkRelation.getName(), (DataObject)targetObjects.get(targetIndex), true);
          }
          break;
        }
      }
      for (Iterator j = objects.iterator(); j.hasNext();) {
        DataObject o = (DataObject)j.next();
        for (Iterator k = relationships.iterator(); k.hasNext();) {
          ObjRelationship r = (ObjRelationship)k.next();
          if (r == dependentPkRelation || r.isToMany()) continue;
          List targetObjects = (List)objectsByEntity.get(r.getTargetEntity());
          int targetIndex = randomizer.nextInt(targetObjects.size());
          o.setToOneTarget(r.getName(), (DataObject)targetObjects.get(targetIndex), true);
        }
      }
    }
  }

  private void deleteRandomData() {
    Map objectsByObjEntity = new HashMap();
    Iterator it = context.getObjectStore().getObjectIterator();
    while (it.hasNext()) {
      DataObject o = (DataObject) it.next();
      Class objEntityClass = o.getObjectId().getObjClass();
      ObjEntity entity = context.getEntityResolver().lookupObjEntity(objEntityClass);
      List objectsForObjEntity = (List)objectsByObjEntity.get(entity);
      if (objectsForObjEntity == null) {
        objectsForObjEntity = new LinkedList();
        objectsByObjEntity.put(entity, objectsForObjEntity);
      }
      objectsForObjEntity.add(o);
    }

    while (deletionRandomizer.hasNext()) {
      Integer entityIndex = (Integer)deletionRandomizer.next();
      ObjEntity entity = (ObjEntity)objEntities.get(entityIndex.intValue());
      List objects = (List)objectsByObjEntity.get(entity);
      if (objects.size() <= 1) continue;
      int objectIndex = randomizer.nextInt(objects.size());
      DataObject objectToDelete = (DataObject)objects.remove(objectIndex);
      DataObject dependentsTakeOver = (DataObject)objects.get(0);
      List relationships = entity.getRelationshipList();
      for (Iterator i = relationships.iterator(); i.hasNext();) {
        ObjRelationship relation = (ObjRelationship)i.next();
        if (!relation.isToMany()) continue;
        List dependentObjects = (List)objectToDelete.readPropertyDirectly(relation.getName());
        if (dependentObjects == null || dependentObjects.isEmpty()) continue;
        if (relation.isToDependentEntity()) {
          for (Iterator j = new ArrayList(dependentObjects).iterator(); j.hasNext();) {
            DataObject dependent = (DataObject)j.next();
            context.deleteObject(dependent);
          }
        } else {
          for (Iterator j = new ArrayList(dependentObjects).iterator(); j.hasNext();) {
            DataObject dependent = (DataObject)j.next();
            objectToDelete.removeToManyTarget(relation.getName(), dependent, true);
            dependentsTakeOver.addToManyTarget(relation.getName(), dependent, true);
          }
        }
      }
      context.deleteObject(objectToDelete);
    }
  }
}