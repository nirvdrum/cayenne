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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.Query;

/**
 * Class provides a set of sorting utilities for Cayenne.
 * For instance it supports sorting of database operations to
 * satisfy database referential integrity consraints.
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrei Adamchik
 */
public class OperationSorter {
	private static Logger logObj = Logger.getLogger(OperationSorter.class);

	private QueryComparator queryComparator;
	private List sortedEntities;

	/** Creates new OperationSorter based on all entities in DataMap array*/
	public OperationSorter(QueryEngine queryEngine, List maps) {
		List entities = new ArrayList();

		if (maps != null) {
			// copy all entities to the list ignoring the order
			for (Iterator it = maps.iterator(); it.hasNext();) {
				entities.addAll(((DataMap)it.next()).getDbEntitiesAsList());
			}
		}

		List sortedEntityNames = InsertOrderSorter.sortedDbEntityNames(entities);
		queryComparator = new QueryComparator(queryEngine, sortedEntityNames);
	}

	/**
	 * Creates a new OperationSorted which will sort entities for insert in the order
	 * specified by the insOrderOfEntNames list
	 */
	/*
	 * TODO: if this method is still required both constructors should be made
	 * into appropriately named static factory methods, so that the method
	 * signatures do not conflict.
	 * 
	public OperationSorter(QueryEngine queryEngine, List insOrderOfEntNames) {
		queryComparator = new QueryComparator(queryEngine, insOrderOfEntNames);
	}
	*/

	/** 
	  *  Returns a new list containing all the DbEntities in <code>entities</code>,
	  *  in the correct order for inserting objects int,o or creating the tables of, those entities.
	  */
	public List sortedEntitiesInInsertOrder(List entities) {
		return InsertOrderSorter.sortedDbEntities(entities);
	}

	/** 
	  *  Returns a new list containing all the DbEntities in <code>entities</code>,
	  *  in the correct order for deleting objects from or removing the tables of, those entities.
	  */
	public List sortedEntitiesInDeleteOrder(List entities) {
		List result = InsertOrderSorter.sortedDbEntities(entities);
		Collections.reverse(result);
		return result;
	}

	/** 
	 *  Sorts an unsorted array of DataObjects in the right 
	 *  insert order for database constraints not to be violated, 
	 *  and so that master pk's for dependent relationships
	 *  are in place prior to being needed for the dependent
	 *  object, and reflexive relationships are correctly handled
	 */
	public static void sortObjectsInInsertOrder(List objects) {
		Collections.sort(objects, ObjectComparator.comparatorForInsertOrder(objects));
	}
	
	/** 
	 *  Sorts an unsorted array of DataObjects in the right 
	 *  delete order for database constraints not to be violated, 
	 *  and so that master pk's for dependent relationships
	 *  are in place prior to being needed for the dependent
	 *  object and reflexive relationships are correctly handled
	 */
	public static void sortObjectsInDeleteOrder(List objects) {
		Collections.sort(objects, ObjectComparator.comparatorForDeleteOrder(objects));
	}

	/**
	 * Creates and returns a List of queries from an unsorted List to make sure
	 * that database constraints will not be violated when a batch is executed.
	 * @param unsortedQueries a List of queries that need to be ordered.
	 */
	public List sortedQueries(List unsortedQueries) {
		Object[] qs = unsortedQueries.toArray();
		Arrays.sort(qs, queryComparator);
		return Arrays.asList(qs);
	}

	/** Used as a comparator to sort in place a list of entities into insert Order (based on a 
	 * sorted list that had to be created from scratch) */
	static final class PreSortedEntityComparator implements Comparator {
		private List sortedList;

		public PreSortedEntityComparator(List sortedList) {
			super();
			this.sortedList = sortedList;
		}

		public final int compare(Object o1, Object o2) {
			int index1 = sortedList.indexOf(o1);
			int index2 = sortedList.indexOf(o2);
			if (index1 < index2) {
				return -1;
			} else if (index1 > index2) {
				return 1;
			} else {
				return 0;
			}
		}

	}
	/** Used as a comparator to sort in place a list of objects into insert Order */
	static final class ObjectComparator implements Comparator {
		private List sortedDbEntities;
		private List dataObjects;
		private int direction;

		//Entities that have one or more reflexive relationships.  Key is the entity, 
		// value is a list of the reflexive relationships
		private HashMap reflexiveEntities = new HashMap();
		
		public static ObjectComparator comparatorForInsertOrder(List objects) {
			return new ObjectComparator(objects, true);
		}
		
		public static ObjectComparator comparatorForDeleteOrder(List objects) {
			return new ObjectComparator(objects, false);
		}
		/**
		 * Create a comparator that will be able to sort a list of objects.  Is private because
		 * the insert boolean flag is not totally clear, and there are only two uses for it.
		 * These users are provided for by the more clearly named static methods.
		 * 
		 * @param objects a List of DataObjects to be sorted into insert/delete order.
		 * This parameter is necessary to determine both the list of entities for
		 * gross sorting by entity, and to sort within an entity if there are any
		 * reflexive relationships
		 * @param insert A boolean indicating if the ordering is for insert or for delete.  
		 * true means insert order, false means delete
		 */
		private ObjectComparator(List objects, boolean insert) {
			super();
			this.direction=insert?1:-1; //The standard "direction" for this sort (returned by the compare method)
			ArrayList unsortedDbEntities = new ArrayList();
			ArrayList allObjEntities = new ArrayList();
			Iterator objIterator = objects.iterator();
			while (objIterator.hasNext()) {
				DataObject thisObj = (DataObject) objIterator.next();
				DbEntity thisDbEntity = thisObj.getDataContext().getEntityResolver().lookupDbEntity(thisObj.getClass());
				ObjEntity thisObjEntity =
					thisObj.getDataContext().getEntityResolver().lookupObjEntity(thisObj.getClass());
				//entity might be null in some odd cases, mainly testing,  Do not try and sort such entities
				if (thisDbEntity != null && !unsortedDbEntities.contains(thisDbEntity)) {
					unsortedDbEntities.add(thisDbEntity);
				}
				if (thisObjEntity != null && !allObjEntities.contains(thisObjEntity)) {
					allObjEntities.add(thisObjEntity);
				}
			}

			sortedDbEntities = InsertOrderSorter.sortedDbEntities(unsortedDbEntities);

			Iterator entityIterator = allObjEntities.iterator();
			while (entityIterator.hasNext()) {
				ObjEntity thisEntity = (ObjEntity) entityIterator.next();
				Iterator relIterator = thisEntity.getRelationshipList().iterator();
				Set handledRelationships = new HashSet();

				while (relIterator.hasNext()) {
					ObjRelationship thisRel = (ObjRelationship) relIterator.next();
					//Target is the source... we've found a reflexive relationship
					if (thisRel.getTargetEntity().equals(thisEntity) && !handledRelationships.contains(thisRel)) {
						List relList = (List) reflexiveEntities.get(thisEntity);
						if (relList == null) {
							relList = new ArrayList();
							reflexiveEntities.put(thisEntity, relList);
						}
						relList.add(new ReflexiveRelData(thisRel, objects));
						handledRelationships.add(thisRel);
						//Only handle the relationship from one direction at this level - 
						//ReflexiveRelData will handle both directions as needed
						ObjRelationship reverse = thisRel.getReverseRelationship();
						if (reverse != null) {
							handledRelationships.add(reverse);
						}
					}
				}
			}
			this.dataObjects = objects;
		}

		public final int compare(Object o1, Object o2) {
			DataObject do1 = (DataObject) o1;
			DataObject do2 = (DataObject) o2;
			EntityResolver er = do1.getDataContext().getEntityResolver();
			int index1 = sortedDbEntities.indexOf(er.lookupDbEntity(do1));
			int index2 = sortedDbEntities.indexOf(er.lookupDbEntity(do2));

			if (index1 < index2) {
				return -this.direction;
			} else if (index1 > index2) {
				return this.direction;
			} else {
				//Same dbentity - check if this entity has reflexive relationships
				List relList = (List) reflexiveEntities.get(er.lookupObjEntity(do1));
				int result = 0;
				if ((relList != null) && (relList.size() != 0)) {
					Iterator relIterator = relList.iterator();
					while (relIterator.hasNext()) {
						ReflexiveRelData thisData = (ReflexiveRelData) relIterator.next();
						List orderedList = thisData.getOrderedObjects();
						int thisOrder = orderedList.indexOf(do1) - orderedList.indexOf(do2);
						if ((result != 0) && (thisOrder != result)) {
							//Conflicting orders from two different relationships... crack up.
							//Future note:  this is only an issue with multiple reflexive relationships on the
							// same entity - rare indeed.  I'm not certain of any robust workaround.
							// An initial guess could be made based on whether the two objects are
							// 'connected' or not (and use a previous value if they aren't), but
							// that might completely foul up the "ordered" semantics.
							throw new CayenneRuntimeException(
								"Cannot determine correct order of insertion for objects :" + do1 + " and " + do2);
						}
						result = thisOrder;
					}
				}
				if(result<0) {
					return -this.direction;
				} else if (result>0) {
					return this.direction;
				}
				return 0;
			}
		}
		
		private final class ReflexiveRelData {
			private ObjRelationship rel;
			private List orderedObjects = new ArrayList();
      private Class targetClass;
      
			public ReflexiveRelData(ObjRelationship relationship, List allObjects) {
				super();
				this.rel = relationship;
    		ObjEntity targetEntity=(ObjEntity)this.rel.getTargetEntity();
				try {
				  this.targetClass =
						Class.forName(targetEntity.getClassName());
				} catch (Exception e) {
					throw new CayenneRuntimeException(
						"Unable to load class named "
							+ targetEntity.getClassName()
							+ " to handle entity "
							+ targetEntity);
				}
				this.createOrderedObjectsList(allObjects);
			}

			//Creates an ordered list based on obj (i.e. finds other objects
			// that must come before and after obj), and adds this list to the 
			// end of orderedList.  Uses recursion to find dependencies.
			private void addObjectToList(DataObject obj, List allObjects, List orderedList) {
				//Do toOne relationships first, because any objects on a toOne will need to be 
				//added prior to obj (but still at the end of the current list)
				ObjRelationship toOneRel;

				if (!this.rel.isToMany()) {
					toOneRel = this.rel;
				} else {
					toOneRel = this.rel.getReverseRelationship();
				}

				if (toOneRel != null) {
					DataObject dest = (DataObject) obj.readPropertyDirectly(toOneRel.getName());
					//In the case that a relationship had a NULLIFY delete rule, this property may be null
					// when in fact it used to point to an object that is being deleted (and is hence important
					// from the perspective of sorting the operations).  So if there is no object, 
					// we do a quick check at the dbrelationship/snapshot level just to be sure.  If
					// an object turns up, then it must have been the above situation, so we use that object
					if(dest==null) {
						DbRelationship finalRel= (DbRelationship) toOneRel.getDbRelationshipList().get(0);
						Map snapshot=obj.getCommittedSnapshot();
						if(snapshot==null) {
							snapshot=obj.getCurrentSnapshot();
						}
					
						Map pksnapshot=finalRel.targetPkSnapshotWithSrcSnapshot(snapshot);
						if(pksnapshot!=null) {

							ObjectId destId = new ObjectId(this.targetClass, pksnapshot);
							dest=obj.getDataContext().registeredObject(destId);
						}
					}
					if (allObjects.contains(dest)) {
						this.addObjectToList(dest, allObjects, orderedList);
					}
				}

				//Now add the current object, after everything that needs to go before it is done.
				//Note that it may have been already added by objects recursed on the to-One relationship
				if (!orderedList.contains(obj)) {
					orderedList.add(obj);
				}

				//And finally, add any objects along the toMany path (that must be definitely after
				// 'obj')
				ObjRelationship toManyRel;
				if (this.rel.isToMany()) {
					toManyRel = this.rel;
				} else {
					toManyRel = this.rel.getReverseRelationship();
				}
				if (toManyRel != null) {
					List targets = (List) obj.readPropertyDirectly(toManyRel.getName());
					if (targets != null) {
						Iterator targetIterator = targets.iterator();
						while (targetIterator.hasNext()) {
							DataObject thisTarget = (DataObject) targetIterator.next();
							if (allObjects.contains(thisTarget)) {
								orderedList.add(thisTarget);
							}
						}
					}
				}
			}

			//Finds the objects of interest (with the correct entity) from allObjects
			// and then iterates over that list, using addObjectToList to build an ordered list.
			// (ignoring objects that are added as the result of addObjectToList)
			private void createOrderedObjectsList(List allObjects) {
				//could use getSourceEntity, but they're the same in this case
				ObjEntity entity = (ObjEntity) this.rel.getTargetEntity();

				//The objects from allObjects that use the given entity
				List entityObjects = new ArrayList();

				//It's more convenient to iterate once and filter to get just the objects 
				// of interest... makes the relationship following code cleaner and 
				// possibly faster (but cleaner is the only intention, until and unless
				// this algorithm proves unviably slow)
				Iterator objectsIterator = allObjects.iterator();
				while (objectsIterator.hasNext()) {
					DataObject thisObject = (DataObject) objectsIterator.next();
					//Obtaining the resolver could be moved outside the loop for efficiency
					EntityResolver er = thisObject.getDataContext().getEntityResolver();
					ObjEntity db1 = er.lookupObjEntity(thisObject);
					if (entity.equals(db1)) {
						entityObjects.add(thisObject);
					}
				}
				//Now iterate over just the objects of interest
				objectsIterator = entityObjects.iterator();
				while (objectsIterator.hasNext()) {
					DataObject thisObject = (DataObject) objectsIterator.next();
					//Check if thisObject is already taken care of... it might already have been
					// due to relationships to other objects
					if (!orderedObjects.contains(thisObject)) {
						this.addObjectToList(thisObject, allObjects, orderedObjects);
					}
				}
			}

			public List getOrderedObjects() {
				return orderedObjects;
			}

			public ObjRelationship getRelationship() {
				return rel;
			}

		}

	}

	/** Used as a comparator in query ordering */
	final class QueryComparator implements Comparator {
		private List insOrderOfEntNames;
		private QueryEngine queryEngine;

		public QueryComparator(QueryEngine queryEngine, List insOrderOfEntNames) {
			this.insOrderOfEntNames = insOrderOfEntNames;
			this.queryEngine = queryEngine;
		}

		/** Insert operations go first, then update, then delete. */
		public int compare(Object o1, Object o2) {
			Query q1 = (Query) o1;
			Query q2 = (Query) o2;
			int opType1 = q1.getQueryType();
			int opType2 = q2.getQueryType();

			// sanity check
			if (opType1 == Query.SELECT_QUERY || opType2 == Query.SELECT_QUERY)
				throw new RuntimeException("Can not sort select queries...");

			if (opType1 == opType2) {
				if (opType1 == Query.INSERT_QUERY) {

					EntityResolver er = queryEngine.getEntityResolver();
					String ent1 = er.lookupDbEntity(q1).getName();
					String ent2 = er.lookupDbEntity(q2).getName();

					if (ent1.equals(ent2))
						return 0;
					else {
						int ind1 = insOrderOfEntNames.indexOf(ent1);
						int ind2 = insOrderOfEntNames.indexOf(ent2);

						if (ind1 < 0)
							return 1; //entity1 not found... do it last
						else if (ind2 < 0)
							return -1; //entity2 not found... do it last
						else
							return ind1 - ind2;
					}
				} else if (opType1 == Query.DELETE_QUERY) {

					// delete operation uses insert ordering in reverse order
					EntityResolver er = queryEngine.getEntityResolver();
					String ent1 = er.lookupDbEntity(q1).getName();
					String ent2 = er.lookupDbEntity(q2).getName();

					if (ent1.equals(ent2))
						return 0;
					else {
						int ind1 = insOrderOfEntNames.indexOf(ent1);
						int ind2 = insOrderOfEntNames.indexOf(ent2);

						if (ind1 < 0)
							return -1;
						else if (ind2 < 0)
							return 1;
						else
							return ind2 - ind1;
					}
				} else
					return 0;
			} else {
				// if two operation have different type, precedence is the following "insert -> update -> delete"
				int op1Val = (opType1 == Query.INSERT_QUERY) ? 1 : (opType1 == Query.UPDATE_QUERY) ? 2 : 3;
				int op2Val = (opType2 == Query.INSERT_QUERY) ? 1 : (opType1 == Query.UPDATE_QUERY) ? 2 : 3;
				return op1Val - op2Val;
			}
		}
	}

	/**
	 * Sorts a list of DBEntities into an appropriate insert order (sortedDbEntities returns a list of sorted names)
	 */
	static final class InsertOrderSorter {
		public static List sortedDbEntityNames(List dbEntities) {
			List names = new ArrayList();
			Iterator entityIt = sortedDbEntities(dbEntities).iterator();
			while (entityIt.hasNext()) {
				names.add(((DbEntity) entityIt.next()).getName());
			}
			return names;
		}

		/* The algorithm
		 *  Partition the entities into sets of connected entities.  All entities in a given set must have a relationship path to 
		 * 	all other entities in the set.
		 * 		While there are unassigned entities:
		 * 			Create a new set and put the first unassigned entity in it.
		 * 			Follow relationships of entities in the set and add entities that aren't already in the set, 
		 * 				until all relationships have been checked.
		 * 		   end while
		 *   For each set:
		 *   		Pick an entity, create a Structure entityDeps {entity, afterEntities, beforeEntities, dontCareEntities} 
		 * 			and populate for that entity.
		 * 		Put this structure into a "main" list (one object only to start with)
		 * 		While there exists a structure in the main list that has after/before/dontcare entities do
		 * 			Pick one such "current" structure
		 * 			From its beforeEntities list, pick one of the entities and create its structure, only using other entities from 
		 * 				the current before list in the new before/after/dontcare lists.  
		 * 				Insert this new structure into the main list directly before the current structure
		 * 			Do the same for the afterEntities, but insert the resulting structure directly after the current structure in the main list
		 * 			Do the same for the dontCareEntities list, and place the result directly after the current structure (before or
		 * 				after is an arbitrary choice and doesn't matter.  However, it must be between the current and the newly inserted
		 * 				after or before structure (the previous two steps) for consistency.
		 * 		End while
		 * 
		 * 	Finally, take the lists from each set and concatenate the top level entities into one big list.  
		 * 		The order of sets doesn't matter as by creation they are disjoint
		 */
		public static List sortedDbEntities(List dbEntities) {
			List finalResult = new ArrayList();

			List entitySets = new ArrayList();
			List unassignedEntities = new ArrayList(dbEntities);
			//Create "sets" of entities
			while (unassignedEntities.size() != 0) {
				//Create and add the set
				List thisSet = new ArrayList();
				entitySets.add(thisSet);

				//seed the set
				thisSet.add(unassignedEntities.get(0));
				unassignedEntities.remove(0);
				int i;

				//As thisSet is modified, the size will keep on growing.  Because entities are
				// only added to the end, all will be processed eventually
				for (i = 0; i < thisSet.size(); i++) {
					DbEntity thisEntity = (DbEntity) thisSet.get(i);
					Iterator relIterator = thisEntity.getRelationshipList().iterator();
					while (relIterator.hasNext()) {
						DbRelationship thisRel = (DbRelationship) relIterator.next();
						DbEntity target = (DbEntity) thisRel.getTargetEntity();
						//Add it to thisSet, if it's not already there
						if (dbEntities.contains(target) && !thisSet.contains(target)) {
							thisSet.add(target);
							unassignedEntities.remove(target);
						}
					}

					//And check unassigned entities to see if any have a direct relationship to thisEntity (maybe one way)
					for (int j = unassignedEntities.size() - 1; j >= 0; j--) {
						DbEntity unassignedEntity = (DbEntity) unassignedEntities.get(j);
						Iterator reverseRelIterator = unassignedEntity.getRelationshipList().iterator();
						while (reverseRelIterator.hasNext()) {
							DbRelationship thisRel = (DbRelationship) reverseRelIterator.next();
							DbEntity target = (DbEntity) thisRel.getTargetEntity();
							//Target is the entity we are looking at.
							if (target == thisEntity) {
								thisSet.add(unassignedEntity);
								unassignedEntities.remove(unassignedEntity);
							}
						}
					}

				}
			}

			//Sets have been created - now process them one by one
			Iterator setIterator = entitySets.iterator();
			while (setIterator.hasNext()) {
				List thisSet = (List) setIterator.next();
				List mainList = new ArrayList();

				//Seed the mainList with the first entity in thisSet
				EntityDep firstDep = new EntityDep((DbEntity) thisSet.get(0), thisSet);
				mainList.add(firstDep);
				int i;
				int entryIndex = 0;
				while (entryIndex != -1) {
					EntityDep thisDep = (EntityDep) mainList.get(entryIndex);
					List befores = thisDep.getBeforeEntities();
					List afters = thisDep.getAfterEntities();
					List dontCares = thisDep.getDontCareEntities();

					//Insert the afters first (while entryIndex+1 is still the correct place)
					if (afters != null && afters.size() != 0) {
						EntityDep newDep = new EntityDep((DbEntity) afters.get(0), afters);
						mainList.add(entryIndex + 1, newDep);
						thisDep.clearAfterEntities(); //Remove them
					}

					//Same for the dontcares, which will put them between the current and the recently inserted "afters"
					if (dontCares != null && dontCares.size() != 0) {
						EntityDep newDep = new EntityDep((DbEntity) dontCares.get(0), dontCares);
						mainList.add(entryIndex + 1, newDep);
						thisDep.clearDontCareEntities(); //Remove them
					}

					//Now insert the befores, at the entry index, shifting the "current" structure to entryIndex+1
					if (befores != null && befores.size() != 0) {
						EntityDep newDep = new EntityDep((DbEntity) befores.get(0), befores);
						mainList.add(entryIndex, newDep);
						thisDep.clearBeforeEntities();
					}
					//Check to see if there's another structure to process
					entryIndex = -1; //Reset to "stop" flag
					for (i = 0; i < mainList.size(); i++) {
						if (((EntityDep) mainList.get(i)).hasDependencies()) {
							entryIndex = i; //found one - use it next
							break; // out of the for loop
						}
					}
				}

				//Processed this set - shove the results into finalResult		
				Iterator mainListIterator = mainList.iterator();
				while (mainListIterator.hasNext()) {
					//Add the root entity to the final result
					finalResult.add(((EntityDep) mainListIterator.next()).getRootEntity());
				}

			}
			return finalResult;
		}

		/**Contains an entity and it's known dependencies.  Constructor can create the dependencies **/
		static final class EntityDep {
			private DbEntity rootEntity;
			private List beforeEntities;
			private List afterEntities;
			private List dontCareEntities;

			private EntityDep() {
				super();
			}

			public EntityDep(DbEntity rootEntity, List otherEntities) {
				super();
				this.rootEntity = rootEntity;
				this.beforeEntities = this.findBeforeEntities(this.rootEntity, otherEntities, null);
				this.afterEntities = this.findAfterEntities(this.rootEntity, otherEntities, null);
				this.dontCareEntities = new ArrayList();
				Iterator othersIterator = otherEntities.iterator();
				while (othersIterator.hasNext()) {
					DbEntity thisEntity = (DbEntity) othersIterator.next();
					boolean inBefore = beforeEntities.contains(thisEntity);
					boolean inAfter = afterEntities.contains(thisEntity);
					if (inBefore && inAfter) {
						throw new CayenneRuntimeException(
							"Cannot handle circular db dependencies properly yet.  DbEntity "
								+ thisEntity.getName()
								+ " needs to be inserted both before and after "
								+ rootEntity.getName());
					}
					if (!inBefore && !inAfter && (thisEntity != rootEntity)) {
						dontCareEntities.add(thisEntity);
					}
				}
			}

			private List findAfterEntities(DbEntity start, List relevantEntities, Set foundEntities) {
				return this.findRelatedEntities(start, relevantEntities, foundEntities, true);
			}

			private List findBeforeEntities(DbEntity start, List relevantEntities, Set foundEntities) {
				return this.findRelatedEntities(start, relevantEntities, foundEntities, false);
			}

			private List findRelatedEntities(
				DbEntity start,
				List relevantEntities,
				Set foundEntities,
				boolean findAfterEntities) {
				List result = new ArrayList();
				if (foundEntities == null) {
					foundEntities = new HashSet();
				}
				foundEntities.add(start);
				Iterator relIterator = start.getRelationshipList().iterator();
				while (relIterator.hasNext()) {
					DbRelationship thisRel = (DbRelationship) relIterator.next();
					//When looking for entities to do after 'start', then if the rel is toMany *OR* it's dependent, 
					//  then it's one we want
					//When looking for entities to do before 'start', then if it's toOne and it's NOT dependent then 
					// it's one we want.  If it's toOne and dependent, then the it's not a "before" situation
					if ((findAfterEntities && (thisRel.isToMany() || thisRel.isToDependentPK()))
						|| (!findAfterEntities && !thisRel.isToMany() && !thisRel.isToDependentPK())) {
						DbEntity target = (DbEntity) thisRel.getTargetEntity();
						//Only add this target if it's in the list of relevant entities, and it's not already found
						if (relevantEntities.contains(target) && (!foundEntities.contains(target))) {
							//Add the target..
							result.add(target);
							//.. and any further "beforeEntities" from that entity
							result.addAll(
								this.findRelatedEntities(target, relevantEntities, foundEntities, findAfterEntities));
						}
					}
				}

				return result;
			}

			public List getAfterEntities() {
				return afterEntities;
			}

			public void clearAfterEntities() {
				afterEntities = null;
			}

			public List getBeforeEntities() {
				return beforeEntities;
			}

			public void clearBeforeEntities() {
				beforeEntities = null;
			}

			public List getDontCareEntities() {
				return dontCareEntities;
			}

			public void clearDontCareEntities() {
				dontCareEntities = null;
			}

			public DbEntity getRootEntity() {
				return rootEntity;
			}

			public boolean hasDependencies() {
				boolean hasAfter = ((afterEntities != null) && (afterEntities.size() != 0));
				boolean hasBefore = ((beforeEntities != null) && (beforeEntities.size() != 0));
				boolean hasDontCare = ((dontCareEntities != null) && (dontCareEntities.size() != 0));
				return hasAfter || hasBefore || hasDontCare;
			}

			//Convenience for the toString method
			private String entityNames(List dbEntities) {
				if (dbEntities != null && dbEntities.size() > 0) {
					StringBuffer result = new StringBuffer("*" + ((DbEntity) dbEntities.get(0)).getName());
					for (int i = 1; i < dbEntities.size(); i++) {
						result.append(", ");
						result.append(((DbEntity) dbEntities.get(i)).getName());
					}
					result.append("*");
					return result.toString();
				}
				return "none";
			}

			public String toString() {
				return "root:"
					+ rootEntity.getName()
					+ " befores :"
					+ entityNames(beforeEntities)
					+ " afters:"
					+ entityNames(afterEntities)
					+ " dontCares:"
					+ entityNames(dontCareEntities);
			}
		}
	}
}
