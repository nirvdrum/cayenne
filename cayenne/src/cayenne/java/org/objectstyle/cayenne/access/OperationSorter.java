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

import org.apache.log4j.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.*;

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
	static Logger logObj = Logger.getLogger(OperationSorter.class.getName());

	private QueryComparator queryComparator;
	private List sortedEntities;
	
	/** Creates new OperationSorter based on all entities in DataMap array*/
	public OperationSorter(QueryEngine queryEngine, DataMap[] maps) {
		List entities = new ArrayList();
		int len = (maps == null) ? 0 : maps.length;

		// copy all entities to the list ignoring the order
		for (int i = 0; i < len; i++) {
			entities.addAll(maps[i].getDbEntitiesAsList());
		}
		List sortedEntityNames=InsertOrderSorter.sortedDbEntityNames(entities);
		queryComparator = new QueryComparator(queryEngine, sortedEntityNames);
	}

	/**
	 * Creates a new OperationSorted which will sort entities for insert in the order
	 * specified by the insOrderOfEntNames list
	 */
	public OperationSorter(QueryEngine queryEngine, List insOrderOfEntNames) {
		queryComparator = new QueryComparator(queryEngine, insOrderOfEntNames);
	}

	/** 
	  *  Sorts an unsorted array of DbEntities in the right 
	  *  insert order. 
	  * @deprecated use OperationSorter.sortedEntitiesInInsertOrder instead.  This method is now rather inefficient
	  */
	public static void sortEntitiesInInsertOrder(List entities) {
		List newList=InsertOrderSorter.sortedDbEntities(entities);
		Collections.sort(entities, new PreSortedEntityComparator(newList));
	}

	public static List sortedEntitiesInInsertOrder(List entities) {
		return InsertOrderSorter.sortedDbEntities(entities);
	}
	
	/** 
	 * Sorts an array of DataMaps in the right save order 
	 * to satisfy inter-map dependencies. 
	 */
	public static void sortMaps(List maps) {
		Collections.sort(maps, new MapComparator());
	}
	
	/** 
	 *  Sorts an unsorted array of DataObjects in the right 
	 *  insert order. 
	 */
	public static void sortObjectsInInsertOrder(List objects) {
		List dbEntities=new ArrayList();
		Iterator objIterator=objects.iterator();
		while(objIterator.hasNext()) {
			DataObject thisObj = (DataObject)objIterator.next();
			DbEntity thisEntity=thisObj.getDataContext().getEntityResolver().lookupDbEntity(thisObj.getClass());
			//entity might be null in some odd cases, mainly testing,  Do not try and sort them
			if(thisEntity!=null && !dbEntities.contains(thisEntity)) {
				dbEntities.add(thisEntity);
			}
		}
		Collections.sort(objects, new PreSortedObjectComparator(InsertOrderSorter.sortedDbEntities(dbEntities)));
	}

	/** Sorts queries to make sure that database constraints will not be
	 *  violated when a batch is executed.
	 *
	 * @param queries an array of queries that need to be ordered.
	 */
	public void sortQueries(Object[] queries) {
		Arrays.sort(queries, queryComparator);
	}

	/** 
	 *  Creates and returns an array of queries
	 *  in the right sorting order from an unsorted array. 
	 */
	public List sortedQueries(List unsortedQueries) {
		Object[] qs = unsortedQueries.toArray();;
		Arrays.sort(qs, queryComparator);
		return Arrays.asList(qs);
	}

	/** Used as a comparator to infer DataMap ordering */
	static final class MapComparator implements Comparator {

		public final int compare(Object o1, Object o2) {
			DataMap m1 = (DataMap) o1;
			DataMap m2 = (DataMap) o2;
			int result = compareMaps(m1, m2);

			// resort to very bad and dumb alphabetic ordering
			if (result == 0) {
				result = m1.getName().compareTo(m2.getName());
			}

			return result;
		}

		/**
		 * Checks if these 2 DataMaps have a dependency on each other.
		 *
		 * @return
		 *  <ul>
		 *  <li> -1 when m2 depends on m1
		 *  <li> 1 when m1 depends on m2
		 *  <li> 0 when dependency is undefined
		 * </ul>
		 */
		private final int compareMaps(DataMap m1, DataMap m2) {

			boolean hasDependent1 = m1.isDependentOn(m2);
			boolean hasDependent2 = m2.isDependentOn(m1);

			// ok if 1 map has a dependency and another does not, 
			// the first one goes first
			if (hasDependent1 && !hasDependent2)
				return 1;

			if (!hasDependent1 && hasDependent2)
				return -1;

			// do not know what to do...
			return 0;
		}
	}

	/** Used as a comparator to sort in place a list of entities into insert Order (based on a 
	 * sorted list that had to be created from scratch) */
	static final class PreSortedEntityComparator implements Comparator {
		private List sortedList;
		
		public PreSortedEntityComparator(List sortedList) {
			super();
			this.sortedList=sortedList;
		}
		
		public final int compare(Object o1, Object o2) {
			int index1=sortedList.indexOf(o1);
			int index2=sortedList.indexOf(o2);
			if(index1<index2) {
				return -1;
			} else if (index1 > index2) {
				return 1;
			} else {
				return 0; 
			}
		}

	}
	/** Used as a comparator to sort in place a list of objects into insert Order (based on a 
	 * sorted list pf dbEntities) */
	static final class PreSortedObjectComparator implements Comparator {
		private List sortedDbEntities;
		
		public PreSortedObjectComparator(List sortedDbEntities) {
			super();
			this.sortedDbEntities=sortedDbEntities;
		}
		
		public final int compare(Object o1, Object o2) {
			DataObject do1=(DataObject)o1;
			DataObject do2=(DataObject)o2;
			EntityResolver er=do1.getDataContext().getEntityResolver();

			int index1=sortedDbEntities.indexOf(er.lookupDbEntity(do1));
			int index2=sortedDbEntities.indexOf(er.lookupDbEntity(do2));
			
			if(index1<index2) {
				return -1;
			} else if (index1 > index2) {
				return 1;
			} else {
				return 0; 
			}
		}

	}

	/** Used as a comparator in query ordering */
	final class QueryComparator implements Comparator {
		private List insOrderOfEntNames;
		private QueryEngine queryEngine;

		public QueryComparator(
			QueryEngine queryEngine,
			List insOrderOfEntNames) {
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
			if (opType1 == Query.SELECT_QUERY || opType1 == Query.SELECT_QUERY)
				throw new RuntimeException("Can not sort select queries...");

			if (opType1 == opType2) {
				if (opType1 == Query.INSERT_QUERY) {

					EntityResolver er=queryEngine.getEntityResolver();
					String ent1 = er.lookupDbEntity(q1).getName();
					String ent2 = er.lookupDbEntity(q2).getName();
							
					if (ent1.equals(ent2))
						return 0;
					else {
						int ind1 = insOrderOfEntNames.indexOf(ent1);
						int ind2 = insOrderOfEntNames.indexOf(ent2);

						if (ind1 < 0)
							return 1;
						else if (ind2 < 0)
							return -1;
						else
							return ind1 - ind2;
					}
				} else if (opType1 == Query.DELETE_QUERY) {

					// delete operation uses insert ordering in reverse order
					EntityResolver er=queryEngine.getEntityResolver();
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
				int op1Val =
					(opType1 == Query.INSERT_QUERY)
						? 1
						: (opType1 == Query.UPDATE_QUERY)
						? 2
						: 3;
				int op2Val =
					(opType2 == Query.INSERT_QUERY)
						? 1
						: (opType1 == Query.UPDATE_QUERY)
						? 2
						: 3;
				return op1Val - op2Val;
			}
		}
	}

	/**
	 * Sorts a list of DBEntities into an appropriate insert order (sortedDbEntities returns a list of sorted names)
	 */
	static final class InsertOrderSorter {
		public static List sortedDbEntityNames(List dbEntities) {
			List names=new ArrayList();
			Iterator entityIt=sortedDbEntities(dbEntities).iterator();
			while(entityIt.hasNext()) {
				names.add(((DbEntity)entityIt.next()).getName());
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
			List finalResult=new ArrayList();
			
			List entitySets=new ArrayList();
			List unassignedEntities=new ArrayList(dbEntities);
			//Create "sets" of entities
			while(unassignedEntities.size()!=0) {
				//Create and add the set
				List thisSet=new ArrayList();
				entitySets.add(thisSet);
				
				//seed the set
				thisSet.add(unassignedEntities.get(0));
				unassignedEntities.remove(0);
				int i;
				
				//As thisSet is modified, the size will keep on growing.  Because entities are
				// only added to the end, all will be processed eventually
				for(i=0; i<thisSet.size(); i++) {
					DbEntity thisEntity=(DbEntity)thisSet.get(i);
					Iterator relIterator=thisEntity.getRelationshipList().iterator();
					while(relIterator.hasNext()) {
						DbRelationship thisRel=(DbRelationship) relIterator.next();
						DbEntity target=(DbEntity)thisRel.getTargetEntity();
						//Add it to thisSet, if it's not already there
						if(dbEntities.contains(target) && !thisSet.contains(target)) {
							thisSet.add(target);
							unassignedEntities.remove(target);
						}
					}
					
					//And check unassigned entities to see if any have a direct relationship to thisEntity (maybe one way)
					for(int j=unassignedEntities.size()-1; j>=0; j--) {
						DbEntity unassignedEntity=(DbEntity)unassignedEntities.get(j);
						Iterator reverseRelIterator=unassignedEntity.getRelationshipList().iterator();
						while(reverseRelIterator.hasNext()) {
							DbRelationship thisRel=(DbRelationship) reverseRelIterator.next();
							DbEntity target=(DbEntity)thisRel.getTargetEntity();
							//Target is the entity we are looking at.
							if(target==thisEntity) {
								thisSet.add(unassignedEntity);
								unassignedEntities.remove(unassignedEntity);
							}
						}
					}

				}
			}
			
			//Sets have been created - now process them one by one
			Iterator setIterator=entitySets.iterator();
			while(setIterator.hasNext()) {
				List thisSet=(List)setIterator.next();
				List mainList=new ArrayList();
				
				//Seed the mainList with the first entity in thisSet
				EntityDep firstDep=new EntityDep((DbEntity)thisSet.get(0), thisSet);
				mainList.add(firstDep);
				int i;
				int entryIndex=0;
				while(entryIndex!=-1) {
					EntityDep thisDep=(EntityDep) mainList.get(entryIndex);
					List befores=thisDep.getBeforeEntities();
					List afters=thisDep.getAfterEntities();
					List dontCares=thisDep.getDontCareEntities();
					
					//Insert the afters first (while entryIndex+1 is still the correct place)
					if(afters!=null && afters.size()!=0) {
						EntityDep newDep=new EntityDep((DbEntity)afters.get(0), afters);
						mainList.add(entryIndex+1, newDep );
						thisDep.clearAfterEntities(); //Remove them
					}
					
					//Same for the dontcares, which will put them between the current and the recently inserted "afters"
					if(dontCares!=null && dontCares.size()!=0) {
						EntityDep newDep=new EntityDep((DbEntity)dontCares.get(0), dontCares);
						mainList.add(entryIndex+1, newDep);
						thisDep.clearDontCareEntities(); //Remove them
					}
					
					//Now insert the befores, at the entry index, shifting the "current" structure to entryIndex+1
					if(befores !=null && befores.size()!=0) {
						EntityDep newDep=new EntityDep((DbEntity)befores.get(0), befores);
						mainList.add(entryIndex, newDep);
						thisDep.clearBeforeEntities();
					}
					//Check to see if there's another structure to process
					entryIndex=-1; //Reset to "stop" flag
					for(i=0; i<mainList.size(); i++) {
						if(((EntityDep)mainList.get(i)).hasDependencies()) {
							entryIndex=i; //found one - use it next
							break; // out of the for loop
						}
					}
				}
		
				//Processed this set - shove the results into finalResult		
				Iterator mainListIterator=mainList.iterator();
				while(mainListIterator.hasNext()) {
					//Add the root entity to the final result
					finalResult.add(((EntityDep)mainListIterator.next()).getRootEntity());
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
				this.rootEntity=rootEntity;
				this.beforeEntities=this.findBeforeEntities(this.rootEntity, otherEntities, null);
				this.afterEntities=this.findAfterEntities(this.rootEntity, otherEntities, null);
				this.dontCareEntities=new ArrayList();
				Iterator othersIterator=otherEntities.iterator();
				while(othersIterator.hasNext()) {
					DbEntity thisEntity=(DbEntity)othersIterator.next();
					boolean inBefore=beforeEntities.contains(thisEntity);
					boolean inAfter=afterEntities.contains(thisEntity);
					if(inBefore && inAfter) {
						throw new CayenneRuntimeException("Cannot handle circular db dependencies properly yet.  DbEntity "+
							thisEntity.getName()+" needs to be inserted both before and after "+rootEntity.getName());
					}
					if(!inBefore && !inAfter && (thisEntity!=rootEntity)) {
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
			
			private List findRelatedEntities(DbEntity start, List relevantEntities, Set foundEntities, boolean findAfterEntities) {
				List result=new ArrayList();
				if(foundEntities==null) {
					foundEntities=new HashSet();
				}
				foundEntities.add(start);
				Iterator relIterator=start.getRelationshipList().iterator();
				while(relIterator.hasNext()) {
					DbRelationship thisRel=(DbRelationship) relIterator.next();
					//When looking for entities to do after 'start', then if the rel is toMany *OR* it's dependent, 
					//  then it's one we want
					//When looking for entities to do before 'start', then if it's toOne and it's NOT dependent then 
					// it's one we want.  If it's toOne and dependent, then the it's not a "before" situation
					if((findAfterEntities && (thisRel.isToMany() || thisRel.isToDependentPK())) ||
						(!findAfterEntities && !thisRel.isToMany() && !thisRel.isToDependentPK())) {
						DbEntity target=(DbEntity)thisRel.getTargetEntity();
						//Only add this target if it's in the list of relevant entities, and it's not already found
						if(relevantEntities.contains(target) && (!foundEntities.contains(target))) {
							//Add the target..
							result.add(target); 
							//.. and any further "beforeEntities" from that entity
							result.addAll(this.findRelatedEntities(target, relevantEntities, foundEntities, findAfterEntities)); 
						}
					}
				}
				
				return result;
			}

			public List getAfterEntities() {
				return afterEntities;
			}
			
			public void clearAfterEntities() {
				afterEntities=null;
			}

			public List getBeforeEntities() {
				return beforeEntities;
			}
			
			public void clearBeforeEntities() {
				beforeEntities=null;
			}
			
			public List getDontCareEntities() {
				return dontCareEntities;
			}

			public void clearDontCareEntities() {
				dontCareEntities=null;
			}

			public DbEntity getRootEntity() {
				return rootEntity;
			}
			
			public boolean hasDependencies() {
				boolean hasAfter=((afterEntities!=null) && (afterEntities.size()!=0));
				boolean hasBefore=((beforeEntities!=null) && (beforeEntities.size()!=0));
				boolean hasDontCare=((dontCareEntities!=null) && (dontCareEntities.size()!=0));
				return hasAfter || hasBefore || hasDontCare;
			}

			//Convenience for the toString method
			private String entityNames(List dbEntities) {
				if(dbEntities!=null && dbEntities.size()>0) {
					StringBuffer result=new StringBuffer("*"+((DbEntity)dbEntities.get(0)).getName());
					for(int i=1; i<dbEntities.size(); i++) {
						result.append(", ");
						result.append(((DbEntity)dbEntities.get(i)).getName());
					}
					result.append("*");
					return result.toString();
				}
				return "none";			
			}
			
			public String toString() {
				return "root:"+rootEntity.getName()+" befores :"+entityNames(beforeEntities)+
					" afters:"+entityNames(afterEntities)+" dontCares:"+entityNames(dontCareEntities);
			}
		}
	}
}

