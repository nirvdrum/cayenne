/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.trans;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Class that serves as a translator of SELECT queries to JDBC statements.
 *
 * @author Andrei Adamchik
 */
public class SelectTranslator extends QueryAssembler implements SelectQueryTranslator {

    private final Map aliasLookup = new HashMap();
    private final List columnList = new ArrayList();
    private final List tableList = new ArrayList();
    private final List aliasList = new ArrayList();
    private final List dbRelList = new ArrayList();
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
    protected List getColumns() {
        return columnList;
    }

    /**
     * Returns query translated to SQL. This is a main work method of the SelectTranslator.
     */
    public String createSqlString() throws Exception {
        forceDistinct = false;

        // build column list
        buildColumnList();

        QualifierTranslator tr = adapter.getQualifierTranslator(this);

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

        // build ORDER BY
        OrderingTranslator orderingTranslator = new OrderingTranslator(this); 
        String orderByStr = orderingTranslator.doTranslation();

        // assemble
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT ");

        if (forceDistinct || getSelectQuery().isDistinct()) {
            queryBuf.append("DISTINCT ");
        }

        List selectColumnExpList = new ArrayList();
        
        for (int i = 0; i < columnList.size(); i++)
        {
            selectColumnExpList.add(getColumn(i));
        }
        
        // append any column expressions used in the order by if this query 
        // uses the DISTINCT modifier
        if (forceDistinct || getSelectQuery().isDistinct()) {
            List orderByColumnList = orderingTranslator.getOrderByColumnList();
            for (int i = 0; i < orderByColumnList.size(); i++) {
                String orderByColumnExp = (String) orderByColumnList.get(i);
                if (selectColumnExpList.contains(orderByColumnExp) == false)
                    selectColumnExpList.add(orderByColumnExp);
            }
        }
        
        // append columns (unroll the loop's first element)
        int columnCount = selectColumnExpList.size();
        queryBuf.append((String) selectColumnExpList.get(0)); // assume there is at least 1 element
        for (int i = 1; i < columnCount; i++) {
            queryBuf.append(", ");
            queryBuf.append((String) selectColumnExpList.get(i));
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
                queryBuf.append(" AND (");
                queryBuf.append(parentQualifierStr);
                queryBuf.append(")");
            } else {
                hasWhere = true;
                queryBuf.append(" WHERE ");
                queryBuf.append(parentQualifierStr);
            }
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
                queryBuf.append(qualifierStr);
            } else {
                if (hasWhere) {
                    queryBuf.append(" AND (");
                    queryBuf.append(qualifierStr);
                    queryBuf.append(")");
                } else {
                    hasWhere = true;
                    queryBuf.append(" WHERE ");
                    queryBuf.append(qualifierStr);
                }
            }
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
        newAliasForTable(getRootDbEntity());
        appendAttributes();
    }

    /**
     * Creates a list of columns used in the query's GROUP BY clause.
     */
    private void buildGroupByList() {
        DbEntity dbEntity = getRootDbEntity();
        if (dbEntity instanceof DerivedDbEntity) {
            groupByList = ((DerivedDbEntity) dbEntity).getGroupByAttributes();
        }
    }

    /**
     * Returns a list of DbAttributes used in query.
     */
    private void appendAttributes() {
        DbEntity dbe = getRootDbEntity();
        SelectQuery q = getSelectQuery();

        // extract custom attributes from the query
        if (q.isFetchingCustomAttributes()) {
            List custAttrNames = q.getCustomDbAttributes();
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
			ObjEntity oe = getRootEntity();
			
            // ObjEntity attrs
            Iterator attrs = oe.getAttributes().iterator();
            while (attrs.hasNext()) {
                ObjAttribute oa = (ObjAttribute) attrs.next();
                Iterator dbPathIterator = oa.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();
                    if (pathPart instanceof DbRelationship) {
                        DbRelationship rel = (DbRelationship) pathPart;
                        dbRelationshipAdded(rel);
                    } else if (pathPart instanceof DbAttribute) {
                        DbAttribute dbAttr = (DbAttribute) pathPart;
                        if (dbAttr == null) {
                            throw new CayenneRuntimeException(
                                "ObjAttribute has no DbAttribute: "
                                    + oa.getName());
                        }
                        columnList.add(dbAttr);
                    }
                }
            }

            // relationship keys
            Iterator rels = oe.getRelationships().iterator();
            while (rels.hasNext()) {
                ObjRelationship rel = (ObjRelationship) rels.next();
                DbRelationship dbRel =
                    (DbRelationship) rel.getDbRelationships().get(0);

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
            Iterator dbattrs = dbe.getAttributes().iterator();
            while (dbattrs.hasNext()) {
                DbAttribute dba = (DbAttribute) dbattrs.next();
                if (dba.isPrimaryKey()) {
                    if (!columnList.contains(dba)) {
                        columnList.add(dba);
                    }
                }
            }

            //May require some special handling for prefetch selects
            // if the prefetch is of a certain type
            if (q instanceof PrefetchSelectQuery) {
                PrefetchSelectQuery pq = (PrefetchSelectQuery) q;
                ObjRelationship r = pq.getSingleStepToManyRelationship();
                if ((r != null) && (r.getReverseRelationship() == null)) {
                    //Prefetching a single step toMany relationship which
                    // has no reverse obj relationship.  Add the FK attributes
                    // of the relationship (wouldn't otherwise be included)
                    DbRelationship dbRel =
                        (DbRelationship) r.getDbRelationships().get(0);

                    List joins = dbRel.getJoins();
                    int jLen = joins.size();
                    for (int j = 0; j < jLen; j++) {
                        DbAttributePair join = (DbAttributePair) joins.get(j);
                        DbAttribute target = join.getTarget();
                        if (!columnList.contains(target)) {
                            columnList.add(target);
                        }
                    }
                }
            }
        }
    }

    private String getColumn(int index) {
        DbAttribute attr = (DbAttribute) columnList.get(index);
        String alias = aliasForTable((DbEntity) attr.getEntity());
        return attr.getAliasedName(alias);
    }

    private void appendGroupBy(StringBuffer queryBuf, int index) {
        DbAttribute attr = (DbAttribute) groupByList.get(index);
        DbEntity ent = (DbEntity) attr.getEntity();
        queryBuf.append(attr.getAliasedName(aliasForTable(ent)));
    }

    private void appendTable(StringBuffer queryBuf, int index) {
        DbEntity ent = (DbEntity) tableList.get(index);
        queryBuf.append(ent.getFullyQualifiedName());
        //The alias should be the alias from the same index in aliasList, not that
        // returned by aliasForTable.
        queryBuf.append(' ').append((String) aliasList.get(index));
    }

    private void appendDbRelJoins(StringBuffer queryBuf, int index) {
        DbRelationship rel = (DbRelationship) dbRelList.get(index);
        String srcAlias = aliasForTable((DbEntity) rel.getSourceEntity());
        String targetAlias = (String) aliasLookup.get(rel);

        boolean andFlag = false;

        List joins = rel.getJoins();
        int len = joins.size();
        for (int i = 0; i < len; i++) {
            DbAttributePair join = (DbAttributePair) joins.get(i);

            if (andFlag) {
                queryBuf.append(" AND ");
            } else {
                andFlag = true;
            }

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

    public String aliasForTable(DbEntity ent, DbRelationship rel) {
        return (String) aliasLookup.get(rel);
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
                msg.append("\n").append(aliasList.get(i)).append(
                    " => ").append(
                    dbeName);
            }

            throw new CayenneRuntimeException(msg.toString());
        }
    }

    public boolean supportsTableAliases() {
        return true;
    }

    public ResultDescriptor getResultDescriptor(ResultSet rs) {
        if (columnList.size() == 0) {
            throw new CayenneRuntimeException("Call 'createStatement' first");
        }
        
		ResultDescriptor descriptor;
			
        if(getSelectQuery().isFetchingCustomAttributes()) {
			descriptor = new ResultDescriptor(getAdapter().getExtendedTypes());
        }
        else {
		    descriptor = new ResultDescriptor(
                getAdapter().getExtendedTypes(),
                getRootEntity());
        }
        
        descriptor.addColumns(columnList);
        descriptor.index();
        return descriptor;
    }

}