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
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.*;
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
	static Logger logObj = Logger.getLogger(OperationSorter.class.getName());

	private QueryComparator queryComparator;

	/** Creates new OperationSorter based on all entities in DataMap array */
	public OperationSorter(QueryEngine queryEngine, DataMap[] maps) {
		ArrayList entities = new ArrayList();
		int len = (maps == null) ? 0 : maps.length;

		// copy all entities to the list ignoring the order
		for (int i = 0; i < len; i++) {
			entities.addAll(maps[i].getDbEntitiesAsList());
		}

		// order the list
		Collections.sort(entities, new EntityComparator());

		// convert the list of DbEntities to the list of names
		ArrayList entNames = new ArrayList();
		int entLen = entities.size();
		for (int i = 0; i < entLen; i++) {
			entNames.add(((DbEntity) entities.get(i)).getName());
		}
		queryComparator = new QueryComparator(queryEngine, entNames);
	}

	public OperationSorter(QueryEngine queryEngine, List insOrderOfEntNames) {
		queryComparator = new QueryComparator(queryEngine, insOrderOfEntNames);
	}

	/** 
	  *  Sorts an unsorted array of DbEntities in the right 
	  *  insert order. 
	  */
	public static void sortEntitiesInInsertOrder(List entities) {
		Collections.sort(entities, new EntityComparator());
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
		Collections.sort(objects, new ObjectComparator());
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
		Object[] qs = unsortedQueries.toArray();
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

	/** Used as a comparator to derive entity ordering */
	static final class EntityComparator implements Comparator {

		public final int compare(Object o1, Object o2) {
			DbEntity e1 = (DbEntity) o1;
			DbEntity e2 = (DbEntity) o2;
			int result = compareEntities(e1, e2);

			// resort to very bad and dumb alphabetic ordering
			if (result == 0) {
				result = e1.getName().compareTo(e2.getName());
			}

			return result;
		}

		/** @return
		 *  <li> -1 when target depends on source
		 *  <li> 1 when source depends on target
		 *  <li> 0 when dependency is undefined..
		 *
		 */
		private final int checkSrcTargetDependancy(DbRelationship rel) {
			DbEntity src = (DbEntity) rel.getSourceEntity();
			DbEntity target = (DbEntity) rel.getTargetEntity();

			Iterator joinIt = rel.getJoins().iterator();
			while (joinIt.hasNext()) {
				DbAttributePair join = (DbAttributePair) joinIt.next();
				boolean srcPkFlag = join.getSource().isPrimaryKey();
				boolean targetPkFlag = join.getTarget().isPrimaryKey();

				if (srcPkFlag && !targetPkFlag)
					// src goes first
					return -1;
				else if (!srcPkFlag && targetPkFlag)
					// dest goes first
					return 1;
				else if (srcPkFlag && targetPkFlag && rel.isToDependentPK())
					// src goes first even though target has its PK attribute in a join
					return -1;
			}

			return 0;
		}

		/** Check if these 2 entities have a relationship with each other....
		 * if so, entity with PK in this relationship goes first..
		 * if they are both PK, "toDependentPK" attribute of relationship defines who goes first,
		 * otherwise try to see if one of the entities does not have dependent entities at all,
		 * then it should go second....
		 *
		 * @return
		 *  <li> -1 when e2 depends on e1
		 *  <li> 1 when e1 depends on e2
		 *  <li> 0 when dependency is undefined..
		 */
		private final int compareEntities(DbEntity e1, DbEntity e2) {

			boolean hasDependent1 = false;
			boolean hasDependent2 = false;

			Iterator it1 = e1.getRelationshipList().iterator();
			while (it1.hasNext()) {
				DbRelationship rel = (DbRelationship) it1.next();

				// non zero value would indicate that
				// there is a dependancy discovered
				int dep = checkSrcTargetDependancy(rel);
				if (dep != 0) {
					if (rel.getTargetEntity() == e2)
						return dep;
					else if (dep < 0)
						hasDependent1 = true;
					else
						hasDependent2 = true;
				}
			}

			// check if the opposite relationships have more information
			Iterator it2 = e2.getRelationshipList().iterator();
			while (it2.hasNext()) {
				DbRelationship rel = (DbRelationship) it2.next();

				// non zero value would indicate that
				// there is a dependancy discovered
				int dep = checkSrcTargetDependancy(rel);
				if (dep != 0) {
					if (rel.getTargetEntity() == e1)
						return -dep;
					else if (dep > 0)
						hasDependent1 = true;
					else
						hasDependent2 = true;
				}
			}

			// ok if 1 entity has a dependency and another does not, 
			// the first one goes first
			if (hasDependent1 && !hasDependent2)
				return -1;

			if (!hasDependent1 && hasDependent2)
				return 1;

			// do not know what to do...
			return 0;
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
	 * Used as a comparator to derive DataObject insert ordering. 
	 * Delegates its functions to internal EntityComparator.
	 */
	static final class ObjectComparator implements Comparator {
		private EntityComparator ecomp = new EntityComparator();

		public final int compare(Object o1, Object o2) {
			DbEntity e1 = lookupEntity((DataObject) o1);
			DbEntity e2 = lookupEntity((DataObject) o2);
			return ecomp.compare(e1, e2);
		}

		private DbEntity lookupEntity(DataObject o) {
			Class aClass = o.getObjectId().getObjClass();
			return o.getDataContext().getEntityResolver().lookupDbEntity(aClass);
		}
	}
}