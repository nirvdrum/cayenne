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
package org.objectstyle.cayenne.access.trans;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * Class that serves as a translator of SELECT queries to JDBC statements.
 * 
 * @author Andrei Adamchik
 */
public class SelectTranslator extends SelectQueryAssembler {
	static Logger logObj = Logger.getLogger(SelectTranslator.class.getName());

	private final HashMap aliasLookup = new HashMap();
	private final ArrayList columnList = new ArrayList();
	private final ArrayList tableList = new ArrayList();
	private final ArrayList aliasList = new ArrayList();
	private final ArrayList dbRelList = new ArrayList();
	private List groupByList;
	private int aliasCounter;

	/** 
	 * If set to <code>true</code>, indicates that distinct
	 * select query is required no matter what the original query 
	 * settings where. This flag can be set when joins are created
	 * using "to-many" relationships. 
	 */
	private boolean forceDistinct;

	/** 
	 * Returns a list of DbAttributes representing columns
	 * in this query.
	 */
	protected List getColumnList() {
		return columnList;
	}

	public int getFetchLimit() {
		return getSelectQuery().getFetchLimit();
	}

	/** 
	 * Returns an ordered list of DbAttributes that describe the
	 * result columns in the in the ResultSet. ResultSet column names are ignored, 
	 * names specified in the query are used instead. */
	public DbAttribute[] getSnapshotDesc(ResultSet rs) {
		int len = columnList.size();
		if (len == 0) {
			throw new CayenneRuntimeException("Call 'createStatement' first");
		}

		DbAttribute[] desc = new DbAttribute[len];
		columnList.toArray(desc);
		return desc;
	}

	/** 
	 * Returns ordered list of Java class names that should be used for fetched values.
	 * ResultSet types are ignored, types specified in the query are used instead. 
	 */
	public String[] getResultTypes(ResultSet rs) {
		int len = columnList.size();
		if (len == 0) {
			throw new CayenneRuntimeException("Call 'createStatement' first.");
		}

		String[] types = new String[len];
		for (int i = 0; i < len; i++) {
			DbAttribute attr = (DbAttribute) columnList.get(i);
			ObjAttribute objAttr =
				getRootEntity().getAttributeForDbAttribute(attr);

			// use explicit type mapping specified in ObjAttribute,
			// or use default JDBC mapping if no ObjAttribute exists
			types[i] =
				(objAttr != null)
					? objAttr.getType()
					: TypesMapping.getJavaBySqlType(attr.getType());
		}
		return types;
	}

	/**
	 * Returns query translated to SQL. This is a main work method of the SelectTranslator.
	 */
	public String createSqlString() throws java.lang.Exception {
		forceDistinct = false;

		// build column list
		buildColumnList();

		QualifierTranslator tr =
			adapter.getQualifierFactory().createTranslator(this);

		// build parent qualifier
		// Parent qualifier translation must PRECEED main qualifier
		// since it will be appended first and its parameters must
		// go first as well
		String parentQualifierStr = null;
		if (getSelectQuery().isQualifiedOnParent()) {
			tr.setTranslateParentQual(true);
			parentQualifierStr = tr.doTranslation();
		}

		// build main qualifier
		tr.setTranslateParentQual(false);
		String qualifierStr = tr.doTranslation();

		// build GROUP BY
		buildGroupByList();

		// build ORDER BY,
		String orderByStr = new OrderingTranslator(this).doTranslation();

		// assemble
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append("SELECT ");

		if (forceDistinct || getSelectQuery().isDistinct()) {
			queryBuf.append("DISTINCT ");
		}

		// append columns (unroll the loop's first element)
		int columnCount = columnList.size();
		appendColumn(queryBuf, 0); // assume there is at least 1 element
		for (int i = 1; i < columnCount; i++) {
			queryBuf.append(", ");
			appendColumn(queryBuf, i);
		}

		// append from clause
		queryBuf.append(" FROM ");

		// append table list (unroll loop's 1st element)
		int tableCount = tableList.size();
		appendTable(queryBuf, 0); // assume there is at least 1 table
		for (int i = 1; i < tableCount; i++) {
			queryBuf.append(", ");
			appendTable(queryBuf, i);
		}

		// append db relationship joins if any
		boolean hasWhere = false;
		int dbRelCount = dbRelList.size();
		if (dbRelCount > 0) {
			hasWhere = true;
			queryBuf.append(" WHERE ");

			appendDbRelJoins(queryBuf, 0);
			for (int i = 1; i < dbRelCount; i++) {
				queryBuf.append(" AND ");
				appendDbRelJoins(queryBuf, i);
			}
		}

		// append parent qualifier if any
		if (parentQualifierStr != null) {
			if (hasWhere) {
				queryBuf.append(" AND ");
			} else {
				hasWhere = true;
				queryBuf.append(" WHERE ");
			}

			queryBuf.append(parentQualifierStr);
		}

		// append group by
		boolean hasGroupBy = false;
		if (groupByList != null) {
			int groupByCount = groupByList.size();
			if (groupByCount > 0) {
				hasGroupBy = true;
				queryBuf.append(" GROUP BY ");
				appendGroupBy(queryBuf, 0);
				for (int i = 1; i < groupByCount; i++) {
					queryBuf.append(", ");
					appendGroupBy(queryBuf, i);
				}
			}
		}

		// append qualifier
		if (qualifierStr != null) {
			if (hasGroupBy) {
				queryBuf.append(" HAVING ");
			} else {
				if (hasWhere) {
					queryBuf.append(" AND ");
				} else {
					hasWhere = true;
					queryBuf.append(" WHERE ");
				}
			}

			queryBuf.append(qualifierStr);
		}

		// append prebuilt ordering
		if (orderByStr != null) {
			queryBuf.append(" ORDER BY ").append(orderByStr);
		}

		return queryBuf.toString();
	}

	private SelectQuery getSelectQuery() {
		return (SelectQuery) getQuery();
	}

	/**
	 * Creates a list of columns used in the query.
	 */
	private void buildColumnList() {
		newAliasForTable(getRootEntity().getDbEntity());
		appendAttributes();
	}

	/**
	 * Creates a list of columns used in the query's GROUP BY clause.
	 */
	private void buildGroupByList() {
		DbEntity dbEntity = getRootEntity().getDbEntity();
		if (dbEntity instanceof DerivedDbEntity) {
			groupByList = ((DerivedDbEntity) dbEntity).getGroupByAttributes();
		}
	}

	/** 
	 * Returns a list of DbAttributes used in query.
	 */
	private void appendAttributes() {
		ObjEntity oe = getRootEntity();
		DbEntity dbe = oe.getDbEntity();
		SelectQuery q = getSelectQuery();

		// extract custom attributes from the query
		if (q.isFetchingCustAttributes()) {
			List custAttrNames = q.getCustDbAttributes();
			int len = custAttrNames.size();
			for (int i = 0; i < len; i++) {
				Attribute attr =
					dbe.getAttribute((String) custAttrNames.get(i));
				if (attr == null) {
					throw new CayenneRuntimeException(
						"Attribute does not exist: " + custAttrNames.get(i));
				}
				columnList.add(attr);
			}
		} else {
			// build a list of attributes mentioned in ObjEntity + PK's + FK's + GROUP BY's

			// ObjEntity attrs
			List attrs = oe.getAttributeList();
			int len = attrs.size();
			for (int i = 0; i < len; i++) {
				ObjAttribute oa = (ObjAttribute) attrs.get(i);
				Attribute attr = oa.getDbAttribute();
				if (attr == null) {
					throw new CayenneRuntimeException(
						"ObjAttribute has no DbAttribute: " + oa.getName());
				}
				columnList.add(attr);
			}

			// relationship keys
			List rels = oe.getRelationshipList();
			int rLen = rels.size();
			for (int i = 0; i < rLen; i++) {
				ObjRelationship rel = (ObjRelationship) rels.get(i);
				DbRelationship dbRel =
					(DbRelationship) rel.getDbRelationshipList().get(0);

				List joins = dbRel.getJoins();
				int jLen = joins.size();
				for (int j = 0; j < jLen; j++) {
					DbAttributePair join = (DbAttributePair) joins.get(j);
					DbAttribute src = join.getSource();
					if (!columnList.contains(src)) {
						columnList.add(src);
					}
				}
			}

			// add remaining needed attrs from DbEntity
			List dbattrs = dbe.getAttributeList();
			int dLen = dbattrs.size();
			for (int i = 0; i < dLen; i++) {
				DbAttribute dba = (DbAttribute) dbattrs.get(i);
				if (dba.isPrimaryKey()) {
					if (!columnList.contains(dba)) {
						columnList.add(dba);
					}
				}
			}
		}
	}

	private void appendColumn(StringBuffer queryBuf, int index) {
		DbAttribute attr = (DbAttribute) columnList.get(index);
		String alias = aliasForTable((DbEntity) attr.getEntity());
		queryBuf.append(attr.getAliasedName(alias));
	}


	private void appendGroupBy(StringBuffer queryBuf, int index) {
		DbAttribute attr = (DbAttribute) groupByList.get(index);
		DbEntity ent = (DbEntity)attr.getEntity();
		queryBuf.append(
			attr.getAliasedName(aliasForTable(ent)));
	}


	private void appendTable(StringBuffer queryBuf, int index) {
		DbEntity ent = (DbEntity) tableList.get(index);
		queryBuf.append(ent.getFullyQualifiedName());
		queryBuf.append(' ').append(aliasForTable(ent));
	}


	private void appendDbRelJoins(StringBuffer queryBuf, int index) {
		DbRelationship rel = (DbRelationship) dbRelList.get(index);
		String srcAlias = aliasForTable((DbEntity) rel.getSourceEntity());
		String targetAlias = aliasForTable((DbEntity) rel.getTargetEntity());

		boolean andFlag = false;

		List joins = rel.getJoins();
		int len = joins.size();
		for (int i = 0; i < len; i++) {
			if (andFlag)
				queryBuf.append(" AND ");
			else
				andFlag = true;

			DbAttributePair join = (DbAttributePair) joins.get(i);
			DbAttribute src = join.getSource();
			queryBuf
				.append(srcAlias)
				.append('.')
				.append(join.getSource().getName())
				.append(" = ")
				.append(targetAlias)
				.append('.')
				.append(join.getTarget().getName());
		}
	}

	/** 
	 * Stores a new relationship in an internal list.
	 * Later it will be used to create joins to relationship 
	 * destination table.
	 */
	public void dbRelationshipAdded(DbRelationship rel) {
		if (rel.isToMany()) {
			forceDistinct = true;
		}

		String existAlias = (String) aliasLookup.get(rel);

		if (existAlias == null) {
			dbRelList.add(rel);

			// add alias for the destination table of the relationship
			String newAlias =
				newAliasForTable((DbEntity) rel.getTargetEntity());
			aliasLookup.put(rel, newAlias);
		}
	}

	/**
	 * Sets up and returns a new alias for a speciafied table.
	 */
	protected String newAliasForTable(DbEntity ent) {
		if (ent instanceof DerivedDbEntity) {
			ent = ((DerivedDbEntity) ent).getParentEntity();
		}

		String newAlias = "t" + aliasCounter++;
		tableList.add(ent);
		aliasList.add(newAlias);
		return newAlias;
	}

	/** 
	 * Overrides superclass implementation. Will return an alias that
	 * should be used for a specified DbEntity in the query
	 * (or null if this DbEntity is not included in the FROM clause).
	 */
	public String aliasForTable(DbEntity ent) {
		if (ent instanceof DerivedDbEntity) {
			ent = ((DerivedDbEntity) ent).getParentEntity();
		}

		int entIndex = tableList.indexOf(ent);
		if (entIndex >= 0) {
			return (String) aliasList.get(entIndex);
		} else {
			StringBuffer msg = new StringBuffer();
			msg
				.append("Alias not found, DbEntity: '")
				.append(ent != null ? ent.getName() : "<null entity>")
				.append("'\nExisting aliases:");

			int len = aliasList.size();
			for (int i = 0; i < len; i++) {
				String dbeName =
					(tableList.get(i) != null)
						? ((DbEntity) tableList.get(i)).getName()
						: "<null entity>";
				msg.append("\n").append(aliasList.get(0)).append(
					" => ").append(
					dbeName);
			}

			throw new CayenneRuntimeException(msg.toString());
		}
	}

	public boolean supportsTableAliases() {
		return true;
	}
}