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
package org.objectstyle.cayenne;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.exp.ExpressionTraversal;
import org.objectstyle.cayenne.exp.TraversalHelper;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;

/**
 * Implements helper methods that perform different query-related operations.
 * <i>May be deprecated in the future, after its functionality is moved to the 
 * places where it is used now.</i>
 * 
 * @author Andrei Adamchik
 *
 */
public final class QueryHelper {
	private static Logger logObj = Logger.getLogger(QueryHelper.class);

	/** Returns an update query for the DataObject that can be used to commit
	 *  object state changes to the database. If no changes are found, null is returned.
	 *
	 *  @param dataObject data object that potentially can have changes that need
	 *  to be synchronized with the database.
	 */
	public static UpdateQuery updateQuery(DataObject dataObject) {
		UpdateQuery upd = new UpdateQuery();

		ObjectId id = dataObject.getObjectId();
		upd.setRoot(dataObject.getClass());

		Map committedSnapshot = dataObject.getCommittedSnapshot();
		Map currentSnapshot = dataObject.getCurrentSnapshot();

		Iterator it = currentSnapshot.keySet().iterator();
		while (it.hasNext()) {
			String attrName = (String) it.next();
			Object newValue = currentSnapshot.get(attrName);

			// if snapshot exists, compare old values and new values,
			// only add attribute to the update clause if the value has changed
			if (committedSnapshot != null) {
				Object oldValue = committedSnapshot.get(attrName);
				if (oldValue != null && !oldValue.equals(newValue))
					upd.addUpdAttribute(attrName, newValue);
				else if (oldValue == null && newValue != null)
					upd.addUpdAttribute(attrName, newValue);
			}
			// if no snapshot exists, just add the fresh value to update clause
			else
				upd.addUpdAttribute(attrName, newValue);
		}

		// original snapshot can have extra keys that are missing in the
		// current snapshot; process those
		if (committedSnapshot != null) {
			Iterator origit = committedSnapshot.keySet().iterator();
			while (origit.hasNext()) {
				String attrName = (String) origit.next();
				if (currentSnapshot.containsKey(attrName))
					continue;

				Object oldValue = committedSnapshot.get(attrName);
				if (oldValue == null)
					continue;

				upd.addUpdAttribute(attrName, null);
			}
		}

		if (upd.getUpdAttributes().size() > 0) {
			// set qualifier
			upd.setQualifier(
				ExpressionFactory.matchAllDbExp(
					id.getIdSnapshot(),
					Expression.EQUAL_TO));
			return upd;
		}

		return null;
	}

	/** Generates a delete query for a specified data object */
	public static DeleteQuery deleteQuery(DataObject dataObject) {
		DeleteQuery del = new DeleteQuery();
		ObjectId id = dataObject.getObjectId();
		del.setRoot(dataObject.getClass());
		del.setQualifier(
			ExpressionFactory.matchAllDbExp(
				id.getIdSnapshot(),
				Expression.EQUAL_TO));
		return del;
	}

	/** Generates an insert query for a specified data object.
	 *
	 *  @param dataObject new data object that need to be inserted to the database.
	 *  @param permId permanent object id that will be assigned to this data object after
	 *  it is committed to the database.
	 */
	public static InsertQuery insertQuery(
		Map objectSnapshot,
		ObjectId permId) {
		InsertQuery ins = new InsertQuery();
		ins.setRoot(permId.getObjClass());
		ins.setObjectSnapshot(objectSnapshot);
		ins.setObjectId(permId);
		return ins;
	}

	/** 
	 * Creates and returns a select query that can be used to 
	 * fetch an object given an ObjectId.
	 */
	public static SelectQuery selectObjectForId(ObjectId oid) {
		SelectQuery sel = new SelectQuery();
		sel.setRoot(oid.getObjClass());
		sel.setQualifier(
			ExpressionFactory.matchAllDbExp(
				oid.getIdSnapshot(),
				Expression.EQUAL_TO));
		return sel;
	}

	/** 
	 * Creates and returns a select query that can be used to 
	 * fetch a list of objects given a list of ObjectIds.
	 * All ObjectIds must belong to the same entity.
	 */
	public static SelectQuery selectQueryForIds(List oids) {
		if (oids == null || oids.size() == 0) {
			throw new IllegalArgumentException("List must contain at least one ObjectId");
		}

		SelectQuery sel = new SelectQuery();
		sel.setRoot(((ObjectId) oids.get(0)).getObjClass());

		Iterator it = oids.iterator();

		ObjectId firstId = (ObjectId) it.next();
		Expression exp =
			ExpressionFactory.matchAllDbExp(
				firstId.getIdSnapshot(),
				Expression.EQUAL_TO);

		while (it.hasNext()) {
			ObjectId anId = (ObjectId) it.next();
			exp =
				exp.orExp(
					ExpressionFactory.matchAllDbExp(
						anId.getIdSnapshot(),
						Expression.EQUAL_TO));
		}

		sel.setQualifier(exp);
		return sel;
	}

	/** 
	 * Creates and returns SelectQuery for a given SelectQuery and
	 * relationship prefetching path.
	 */
	public static PrefetchSelectQuery selectPrefetchPath(
		QueryEngine e,
		SelectQuery q,
		String prefetchPath) {
		ObjEntity ent = e.getEntityResolver().lookupObjEntity(q);
		PrefetchSelectQuery newQ = new PrefetchSelectQuery();

		newQ.setPrefetchPath(prefetchPath);
		newQ.setRootQuery(q);
		Expression exp =
			ExpressionFactory.unaryExp(Expression.OBJ_PATH, prefetchPath);
		Iterator it = ent.resolvePathComponents(exp);
		Relationship r = null;
		int relCount = 0;
		while (it.hasNext()) {
			r = (Relationship) it.next();
			relCount++;
		}

		if (r != null) {
			ObjRelationship objR = (ObjRelationship) r;
			newQ.setRoot(objR.getTargetEntity());
			newQ.setQualifier(
				transformQualifier(ent, q.getQualifier(), prefetchPath));
			if ((relCount == 1) && objR.isToMany() && !objR.isFlattened()) {
				//A one step toMany relationship needs the special handling
				newQ.setSingleStepToManyRelationship(objR);
			}
			return newQ;
		} else {
			// TODO: what else could we do here?
			return null;
		}
	}

	/**
	 * Translates qualifier applicable for one ObjEntity into a
	 * qualifier for a related ObjEntity.
	 * @param ent the entity to which the original qualifier (<code>qual</code>) 
	 * applies
	 * @param qual the qualifier on <code>ent</code>
	 * @param relPath a relationship path from <code>ent</code> to some target entity
	 * @return Expression which, when applied to the target entity of relPath, 
	 * will give the union of the objects that would be obtained by following 
	 * relPath from all of the objects in <code>ent</code> that match <code>qual</code>
	 */
	public static Expression transformQualifier(
		ObjEntity ent,
		Expression qual,
		String relPath) {
		if (qual == null) {
			return null;
		}

		ExpressionTranslator trans = new ExpressionTranslator(ent, relPath);
		ExpressionTraversal parser = new ExpressionTraversal();
		parser.setHandler(trans);
		parser.traverseExpression(qual);

		return trans.getPeer(qual);
	}

	/**
	 * Generates a SelectQuery that can be used to fetch 
	 * relationship destination objects given a source object
	 * of a to-many relationship. 
	 */
	public static SelectQuery selectRelationshipObjects(
		QueryEngine e,
		DataObject source,
		String relName) {

		ObjEntity ent = e.getEntityResolver().lookupObjEntity(source);
		ObjRelationship rel = (ObjRelationship) ent.getRelationship(relName);
		ObjEntity destEnt = (ObjEntity) rel.getTargetEntity();

		List dbRels = rel.getDbRelationshipList();

		// sanity check
		if (dbRels == null || dbRels.size() == 0) {
			throw new CayenneRuntimeException(
				"ObjRelationship '" + rel.getName() + "' is unmapped.");
		}

		// build a reverse DB path
		// ...while reverse ObjRelationship may be absent,
		// reverse DB must always be there...
		StringBuffer buf = new StringBuffer();
		ListIterator it = dbRels.listIterator(dbRels.size());
		while (it.hasPrevious()) {
			if (buf.length() > 0) {
				buf.append(".");
			}
			DbRelationship dbRel = (DbRelationship) it.previous();
			DbRelationship reverse = dbRel.getReverseRelationship();

			// another sanity check
			if (reverse == null) {
				throw new CayenneRuntimeException(
					"DbRelatitionship '"
						+ dbRel.getName()
						+ "' has no reverse relationship");
			}

			buf.append(reverse.getName());
		}

		SelectQuery sel = new SelectQuery(destEnt);
		sel.setQualifier(
			ExpressionFactory.binaryDbPathExp(
				Expression.EQUAL_TO,
				buf.toString(),
				source));

		return sel;
	}

	static final class ExpressionTranslator extends TraversalHelper {
		protected Map expMap = new HashMap();
		protected Map expFill = new HashMap();
		protected ObjEntity ent;
		protected String relationshipPath;
		protected String relationshipDbPath;
		protected String prependObjPath;
		protected String prependDbPath;

		public ExpressionTranslator(ObjEntity e, String relPath) {
			System.out.println(
				"Expression translator for e "
					+ e.getName()
					+ " and path "
					+ relPath);
			this.ent = e;
			this.relationshipPath = relPath;
			this.relationshipDbPath = forwardDbPath(e, relPath);
			this.prependObjPath = reversePath(e, relPath);
			this.prependDbPath = reverseDbPath(e, relPath);
		}

		public Expression getPeer(Expression orig) {
			return (Expression) expMap.get(orig);
		}

		private int getOperandIndex(Expression orig) {
			Integer indObj = (Integer) expFill.get(orig);
			int ind = (indObj != null) ? indObj.intValue() + 1 : 0;
			expFill.put(orig, new Integer(ind));

			return ind;
		}

		/**
		 * For a relationship path from source to target, builds a reverse path 
		 * from target to source.
		 */
		public String reversePath(ObjEntity e, String relPath) {
			Expression exp =
				ExpressionFactory.unaryExp(Expression.OBJ_PATH, relPath);
			Iterator it = e.resolvePathComponents(exp);
			StringBuffer buf = new StringBuffer();
			boolean hasRels = false;

			while (it.hasNext()) {
				ObjRelationship rel = (ObjRelationship) it.next();
				ObjRelationship reverse = rel.getReverseRelationship();
				if (reverse == null) {
					//Couldn't create reverse obj path because of a missing reverse
					// relationship
					return null;
				}
				if (hasRels) {
					buf.insert(0, Entity.PATH_SEPARATOR);
				}

				buf.insert(0, reverse.getName());
				hasRels = true;
			}

			return buf.toString();
		}

		public String forwardDbPath(ObjEntity e, String relPath) {
			Expression exp =
				ExpressionFactory.unaryExp(Expression.OBJ_PATH, relPath);
			Iterator it = e.resolvePathComponents(exp);
			StringBuffer buf = new StringBuffer();

			while (it.hasNext()) {
				ObjRelationship rel = (ObjRelationship) it.next();
				Iterator dbRels = rel.getDbRelationshipList().iterator();
				while (dbRels.hasNext()) {
					DbRelationship r = (DbRelationship) dbRels.next();
					if (buf.length() > 0) {
						buf.append(Entity.PATH_SEPARATOR);
					}

					buf.append(r.getName());
				}
			}

			return buf.toString();
		}

		/**
		 * For a relationship path from source to target, builds a reverse path 
		 * from target to source.
		 */
		public String reverseDbPath(ObjEntity e, String relPath) {
			Expression exp =
				ExpressionFactory.unaryExp(Expression.DB_PATH, relPath);
			Iterator it = e.resolvePathComponents(exp);
			StringBuffer buf = new StringBuffer();
			boolean hasRels = false;

			while (it.hasNext()) {
				ObjRelationship rel = (ObjRelationship) it.next();

				Iterator dbRels = rel.getDbRelationshipList().iterator();
				while (dbRels.hasNext()) {
					DbRelationship dbRel = (DbRelationship) dbRels.next();
					DbRelationship reverse = dbRel.getReverseRelationship();

					if (hasRels) {
						buf.insert(0, Entity.PATH_SEPARATOR);
					}

					buf.insert(0, reverse.getName());
					hasRels = true;
				}
			}

			return buf.toString();
		}

		/** 
		 * Creates expression of the same type and same operands 
		 * as the original expression. Operands of the new expression
		 * are set to null.
		 */
		public Expression createExpressionOfType(Expression e)
			throws ExpressionException {
			try {
				Expression exp = (Expression) e.getClass().newInstance();
				exp.setType(e.getType());
				return exp;
			} catch (Exception ex) {
				throw new ExpressionException(
					"Error instantiating expression.",
					ex);
			}
		}

		private void processOperand(Object operand, Expression parentNode) {
			// attach operand to parent at index
			if (parentNode != null) {
				int ind = getOperandIndex(parentNode);
				Expression parentPeer = getPeer(parentNode);
				// operands of object expression need translation
				if (parentPeer.getType() == Expression.OBJ_PATH) {
					if (prependObjPath == null) {
						//Change to a db path type expression,
						// because there is no ObjPath
						parentPeer.setType(Expression.DB_PATH);

						Iterator it = ent.resolvePathComponents(parentNode);
						Object lastComponent = null;
						while (it.hasNext()) {
							lastComponent = it.next();
						}
						if (lastComponent instanceof ObjAttribute) {
							ObjAttribute objAttr = (ObjAttribute) lastComponent;
							operand =
								processPath(
									objAttr.getDbAttribute().getName(),
									relationshipDbPath,
									prependDbPath);
						} else {
							//Not sure what to do... just hope that this
							// will work I guess?
							operand =
								processPath(
									(String) operand,
									relationshipDbPath,
									prependDbPath);

						}

					} else {
						//There is a reverse obj path.. use it
						operand =
							processPath(
								(String) operand,
								relationshipPath,
								prependObjPath);
					}
				} else if (parentPeer.getType() == Expression.DB_PATH) {
					operand =
						processPath(
							(String) operand,
							relationshipDbPath,
							prependDbPath);
				}
				parentPeer.setOperand(ind, operand);
			}
		}

		private String processPath(
			String path,
			String toPrefix,
			String fromPrefix) {
			if (path.equals(toPrefix)) {
				// 1. Path ends with prefetch entity - match PK
				throw new CayenneRuntimeException(
					"Prefetching with path ending on "
						+ "prefetch entity is not supported yet.");
			} else if (path.startsWith(toPrefix + Entity.PATH_SEPARATOR)) {
				// 2. Path starts with prefetch entity - strip it.
				return path.substring(
					(toPrefix + Entity.PATH_SEPARATOR).length());
			} else {
				// 3. Path has nothing to do with prefetch entity - prepend rel from prefetch
				return fromPrefix + Entity.PATH_SEPARATOR + path;
			}
		}

		private void processNode(Expression node, Expression parentNode) {
			Expression e = createExpressionOfType(node);
			expMap.put(node, e);
			processOperand(e, parentNode);
		}

		public void startUnaryNode(Expression node, Expression parentNode) {
			processNode(node, parentNode);
		}

		public void startBinaryNode(Expression node, Expression parentNode) {
			processNode(node, parentNode);
		}

		public void startTernaryNode(Expression node, Expression parentNode) {
			processNode(node, parentNode);
		}

		public void objectNode(Object leaf, Expression parentNode) {
			processOperand(leaf, parentNode);
		}

		public void startListNode(Expression node, Expression parentNode) {
			processNode(node, parentNode);
		}

	}
}