/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.query.SqlSelectQuery;

/**
 * Class works as a translator of raw SELECT queries to JDBC statements.
 *
 * @author Andrei Adamchik
 */
public class SqlSelectTranslator
    extends QueryAssembler
    implements SelectQueryTranslator {
        
    private static Logger logObj = Logger.getLogger(SqlSelectTranslator.class);

    /**
     * Initializes the types of columns in ResultDescriptor from the ResultSet metadata.
     */
    public static void appendResultTypesFromMetadata(
        ResultSet rs,
        ResultDescriptor desc) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int len = md.getColumnCount();
            if (len == 0) {
                throw new CayenneRuntimeException("No columns in ResultSet.");
            }

            for (int i = 0; i < len; i++) {
                int sqlType = md.getColumnType(i + 1);
                desc.addJavaType(TypesMapping.getJavaBySqlType(sqlType));
            }
        } catch (SQLException sqex) {
            logObj.error("Error", sqex);
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }
    }

    /**
     * Initializes the names of columns in ResultDescriptor from the ResultSet metadata.
     */
    public static void appendSnapshotLabelsFromMetadata(
        ResultSet rs,
        ResultDescriptor descriptor) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int len = md.getColumnCount();
            if (len == 0) {
                throw new CayenneRuntimeException("No columns in ResultSet.");
            }

            for (int i = 0; i < len; i++) {

                // figure out column name
                String name = md.getColumnLabel(i + 1);
                if (name == null || name.length() == 0) {
                    name = md.getColumnName(i + 1);

                    if (name == null || name.length() == 0) {
                        name = "column_" + (i + 1);
                    }
                }

                DbAttribute desc = new DbAttribute();
                desc.setName(name);
                desc.setType(md.getColumnType(i + 1));
                descriptor.addDbAttribute(desc);
            }
        } catch (SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }
    }

    public String aliasForTable(DbEntity dbEnt) {
        throw new RuntimeException("aliases not supported");
    }

    public String createSqlString() throws Exception {
        return getRawQuery().getSqlString();
    }

    public void dbRelationshipAdded(DbRelationship dbRel) {
        throw new RuntimeException("db relationships not supported");
    }

    private SqlSelectQuery getRawQuery() {
        return (SqlSelectQuery) query;
    }

    /**
     * Creates and returns a ResultDescriptor instance for the raw SQL
     * query. Uses information provided by the query itself if possible. 
     * If not, result information is deduced from the ResultSetMetadata.
     */
    public ResultDescriptor getResultDescriptor(ResultSet rs) {
        ResultDescriptor descriptor =
            new ResultDescriptor(getAdapter().getExtendedTypes(), null);

        DbAttribute[] attrs = getRawQuery().getResultDescriptors();
        if (attrs == null || attrs.length == 0) {
            SqlSelectTranslator.appendSnapshotLabelsFromMetadata(rs, descriptor);
        } else {
            for (int i = 0; i < attrs.length; i++) {
                descriptor.addDbAttribute(attrs[i]);
            }
        }

        ObjAttribute[] objAttrs = getRawQuery().getObjDescriptors();
        if (objAttrs == null || objAttrs.length == 0) {
            SqlSelectTranslator.appendResultTypesFromMetadata(rs, descriptor);
        } else {
            for (int i = 0; i < objAttrs.length; i++) {
                descriptor.addJavaType(objAttrs[i].getType());
            }
        }

        descriptor.index();
        return descriptor;
    }

    /**
     * Returns <code>false</code>.  Since this translator deals
     * with raw SQL queries, it is the responsibility of the 
     * caller to format SQL appropriately. The translator itself
     * will not modify the query.
     */
    public boolean supportsTableAliases() {
        return false;
    }
}