package org.objectstyle.cayenne.access.trans;
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** Class works as a translator of SELECT queries to JDBC statements. */
public class SelectTranslator extends SelectQueryAssembler {
    static Logger logObj = Logger.getLogger(SelectTranslator.class.getName());

    private HashMap aliasLookup = new HashMap();
    private ArrayList columnList = new ArrayList();
    private ArrayList tableList = new ArrayList();
    private ArrayList aliasList = new ArrayList();
    private ArrayList dbRelList = new ArrayList();
    private int aliasCounter;

    /** If set to <code>true</code>, indicates that distinct
     *  select query is required no matter what the original query 
     *  settings where. This flag can be set when joins are created
     *  using "to-many" relationships. */
    private boolean forceDistinct;
    

    /** Returns ordered list of names that should be assigned as keys
     * for values fetched from the database. ResultSet column names are ignored, 
     * names specified in the query are used instead. */
    public String[] getSnapshotLabels(ResultSet rs) {
        int len = columnList.size();
        if (len == 0)
            throw new CayenneRuntimeException("Call 'createStatement' first");

        String[] labels = new String[len];
        for (int i = 0; i < len; i++) {
            labels[i] = ((DbAttribute) columnList.get(i)).getName();
        }
        return labels;
    }

    /** Returns ordered list of Java class names that should be used for fetched values.
      * ResultSet types are ignored, types specified in the query are used instead. */
    public String[] getResultTypes(ResultSet rs) {
        int len = columnList.size();
        if (len == 0) {
            throw new CayenneRuntimeException("Call 'createStatement' first.");
        }

        String[] types = new String[len];
        for (int i = 0; i < len; i++) {
            DbAttribute attr = (DbAttribute) columnList.get(i);
            ObjAttribute objAttr = getRootEntity().getAttributeForDbAttribute(attr);

            // use explicit type mapping specified in ObjAttribute,
            // or use default JDBC mapping if no ObjAttribute exists
            types[i] =
                (objAttr != null)
                    ? objAttr.getType()
                    : TypesMapping.getJavaBySqlType(attr.getType());
        }
        return types;
    }

    public String createSqlString() throws java.lang.Exception {
        forceDistinct = false;

        // build column list
        buildColumnList();

        // build WHERE
        String qualifierStr = new QualifierTranslator(this).doTranslation();

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
        int dbRelCount = dbRelList.size();
        if (dbRelCount > 0) {
            queryBuf.append(" WHERE ");
            appendDbRelJoins(queryBuf, 0);
            for (int i = 1; i < dbRelCount; i++) {
                queryBuf.append(" AND ");
                appendDbRelJoins(queryBuf, i);
            }
        }

        // append prebuilt qualifier
        if (qualifierStr != null) {
            if (dbRelCount > 0)
                queryBuf.append(" AND ");
            else
                queryBuf.append(" WHERE ");

            queryBuf.append(qualifierStr);
        }

        // append prebuilt ordering
        if (orderByStr != null)
            queryBuf.append(" ORDER BY ").append(orderByStr);

        return queryBuf.toString();
    }

    private SelectQuery getSelectQuery() {
        return (SelectQuery) getQuery();
    }

    private void buildColumnList() {
        DbEntity dbEntity = getRootEntity().getDbEntity();
        String newAlias = "t" + aliasCounter++;
        tableList.add(dbEntity);
        aliasList.add(newAlias);

        List dbAttrs = dbEntity.getAttributeList();
        columnList.addAll(dbAttrs);
    }

    private void appendColumn(StringBuffer queryBuf, int index) {
        DbAttribute attr = (DbAttribute) columnList.get(index);
        Entity ent = attr.getEntity();
        int aliasIndex = tableList.indexOf(ent);
        queryBuf.append(aliasList.get(aliasIndex)).append('.');
        queryBuf.append(attr.getName());
    }

    private void appendTable(StringBuffer queryBuf, int index) {
        DbEntity ent = (DbEntity) tableList.get(index);
        queryBuf.append(ent.getName());
        queryBuf.append(' ').append(aliasList.get(index));
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

            // add alias for the destination table
            // of the relationship
            String newAlias = "t" + aliasCounter++;
            tableList.add(rel.getTargetEntity());
            aliasList.add(newAlias);
            aliasLookup.put(rel, newAlias);
        }
    }

    /** Overrides superclass implementation. Will return an alias that
    *  should be used for a specified DbEntity in the query
    *  (or null if this DbEntity is not included in the FROM
    *  clause).
    */
    public String aliasForTable(DbEntity dbEnt) {
        int entIndex = tableList.indexOf(dbEnt);
        return (entIndex >= 0) ? (String) aliasList.get(entIndex) : null;
    }

    public boolean supportsTableAlases() {
        return true;
    }
}