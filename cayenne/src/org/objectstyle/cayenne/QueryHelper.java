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

import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertQuery;
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
	static Logger logObj = Logger.getLogger(QueryHelper.class.getName());

	/** Returns a qualifier that matches an all values in the
	 *  map as defined in ObjectId. Keys are DbAttribute names, values - database values. */
	private static Expression qualifierForDbMap(Map map) {
		ArrayList list = new ArrayList();

		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String attr = (String) it.next();
			Object value = map.get(attr);
			Expression dbPathExp = ExpressionFactory.unaryExp(Expression.DB_PATH, attr);
			list.add(ExpressionFactory.binaryExp(Expression.EQUAL_TO, dbPathExp, value));
		}
		return ExpressionFactory.joinExp(Expression.AND, list);
	}

	/** Returns an update query for the DataObject that can be used to commit
	 *  object state changes to the database. If no changes are found, null is returned.
	 *
	 *  @param dataObject data object that potentially can have changes that need
	 *  to be synchronized with the database.
	 */
	public static UpdateQuery updateQuery(DataObject dataObject) {
		UpdateQuery upd = new UpdateQuery();

		ObjectId id = dataObject.getObjectId();
		upd.setObjEntityName(id.getObjEntityName());

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

		// original snapshot can have extra keys that are missing in current snapshot
		// process those
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

		if (upd.getUpdAttributes().size() > 0) {
			// set qualifier
			upd.setQualifier(qualifierForDbMap(id.getIdSnapshot()));
			return upd;
		}

		return null;
	}

	/** Generates a delete query for a specified data object */
	public static DeleteQuery deleteQuery(DataObject dataObject) {
		DeleteQuery del = new DeleteQuery();
		ObjectId id = dataObject.getObjectId();
		del.setObjEntityName(id.getObjEntityName());
		del.setQualifier(qualifierForDbMap(id.getIdSnapshot()));
		return del;
	}

	/** Generates an insert query for a specified data object.
	 *
	 *  @param dataObject new data object that need to be inserted to the database.
	 *  @param permId permanent object id that will be assigned to this data object after
	 *  it is committed to the database.
	 */
	public static InsertQuery insertQuery(Map objectSnapshot, ObjectId permId) {
		InsertQuery ins = new InsertQuery();
		ins.setObjEntityName(permId.getObjEntityName());
		ins.setObjectSnapshot(objectSnapshot);
		ins.setObjectId(permId);
		return ins;
	}

	/** Generates a select query that can be used to fetch an object for object id. */
	public static SelectQuery selectObjectForId(ObjectId oid) {
		SelectQuery sel = new SelectQuery();
		sel.setObjEntityName(oid.getObjEntityName());
		sel.setQualifier(qualifierForDbMap(oid.getIdSnapshot()));
		return sel;
	}

	/** 
	 * Creates and returns SelectQuery for a given SelectQuery and
	 * relationship prefetching path.
	 */
	public static SelectQuery selectPrefetchPath(
		QueryEngine e,
		SelectQuery q,
		String prefetchPath) {
		ObjEntity ent = e.lookupEntity(q.getObjEntityName());
		SelectQuery newQ = new SelectQuery();

		Expression exp = ExpressionFactory.unaryExp(Expression.OBJ_PATH, prefetchPath);
		Iterator it = ent.resolvePathComponents(exp);
		Relationship r = null;
		while (it.hasNext()) {
			r = (Relationship) it.next();
		}

		newQ.setObjEntityName(r.getTargetEntity().getName());
		newQ.setQualifier(transformQualifier(ent, q.getQualifier(), prefetchPath));
		return newQ;
	}

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
		ObjectId oid,
		String relName) {
		SelectQuery sel = new SelectQuery();
		ObjEntity ent = e.lookupEntity(oid.getObjEntityName());
		ObjRelationship rel = (ObjRelationship) ent.getRelationship(relName);
		ObjEntity destEnt = (ObjEntity) rel.getTargetEntity();
		sel.setObjEntityName(destEnt.getName());

		// convert source PK into destination FK definition,
		// use it to build a qualifier that will be applied to the destination entity
		List dbRels = rel.getDbRelationshipList();

		// no support for flattened rels yet...
		if (dbRels == null || dbRels.size() != 1) {
			throw new CayenneRuntimeException("ObjRelationship has invalid/unsupported DbRelationships.");
		}

		DbRelationship dbRel = (DbRelationship) dbRels.get(0);
		Map fkAttrs =
			dbRel.getReverseRelationship().srcFkSnapshotWithTargetSnapshot(
				oid.getIdSnapshot());
		sel.setQualifier(qualifierForDbMap(fkAttrs));
		return sel;
	}

	static final class ExpressionTranslator extends TraversalHelper {
		protected HashMap expMap = new HashMap();
		protected HashMap expFill = new HashMap();
		protected String prependObjPath;
		protected String prependDbPath;

		public ExpressionTranslator(ObjEntity e, String relPath) {
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
			Expression exp = ExpressionFactory.unaryExp(Expression.OBJ_PATH, relPath);
			Iterator it = e.resolvePathComponents(exp);
			StringBuffer buf = new StringBuffer();
			boolean hasRels = false;

			while (it.hasNext()) {
				ObjRelationship rel = (ObjRelationship) it.next();
				ObjRelationship reverse = rel.getReverseRelationship();

				if (hasRels) {
					buf.insert(0, Entity.PATH_SEPARATOR);
				}

				buf.insert(0, reverse.getName());
				hasRels = true;
			}

			return buf.toString();
		}

		/**
		 * For a relationship path from source to target, builds a reverse path 
		 * from target to source.
		 */
		public String reverseDbPath(ObjEntity e, String relPath) {
			Expression exp = ExpressionFactory.unaryExp(Expression.DB_PATH, relPath);
			Iterator it = e.resolvePathComponents(exp);
			StringBuffer buf = new StringBuffer();
			boolean hasRels = false;

			while (it.hasNext()) {
				ObjRelationship rel = (ObjRelationship) it.next();

				Iterator dbRels = rel.getDbRelationshipList().iterator();
				while(dbRels.hasNext()) {
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
				logObj.log(Level.INFO, "Error instantiating expression.", ex);
				throw new ExpressionException("Error instantiating expression.", ex);
			}
		}

		private void processOperand(Object operand, Expression parentNode) {
			// attach operand to parent at index
			if (parentNode != null) {
				int ind = getOperandIndex(parentNode);
				Expression parentPeer = getPeer(parentNode);

				// operands of object expression need translation
				if (parentPeer.getType() == Expression.OBJ_PATH) {
					operand = prependObjPath + Entity.PATH_SEPARATOR + operand;
				}
				else if(parentPeer.getType() == Expression.DB_PATH) {
					operand = prependDbPath + Entity.PATH_SEPARATOR + operand;
				}

				parentPeer.setOperand(ind, operand);
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
	}
}