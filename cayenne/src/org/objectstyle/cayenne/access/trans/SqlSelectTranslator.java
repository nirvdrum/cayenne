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


import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SqlSelectQuery;


/** Class works as a translator of raw SELECT queries to JDBC statements.
  * 
  * @author Andrei Adamchik
  */
public class SqlSelectTranslator extends SelectQueryAssembler {
    static Logger logObj = Logger.getLogger(SqlSelectTranslator.class.getName());

    public String createSqlString() throws java.lang.Exception {
        return getRawQuery().getSqlString();
    }

    private final SqlSelectQuery getRawQuery() {
        return (SqlSelectQuery)query;
    }

    /** Returns ordered list of names that should be assigned as keys
      * for values fetched from the database. Uses ResultSet info. */
    public String[] getSnapshotLabels(ResultSet rs) {
        ObjAttribute[] attrs = getRawQuery().getResultDesc();
        if(attrs == null || attrs.length == 0)
            return getSnapshotLabelsFromMetadata(rs);

        int len = attrs.length;
        String[] labels = new String[len];
        for(int i = 0; i < len; i++) {
            labels[i] = attrs[i].getName();
        }
        return labels;
    }

    /** Returns ordered list of names that should be assigned as keys
      * for values fetched from the database. This is a failover method to
      * obtain result labels used when query has no such data.. */
    public String[] getSnapshotLabelsFromMetadata(ResultSet rs) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int len = md.getColumnCount();
            if(len == 0)
                throw new CayenneRuntimeException("No columns in ResultSet.");

            String[] labels = new String[len];
            for(int i = 0; i < len; i++) {

                String name = md.getColumnLabel(i + 1);
                if(name == null || name.length() == 0) {
                    name = md.getColumnName(i + 1);

                    if(name == null || name.length() == 0)
                        name = "column_" + (i + 1);
                }

                labels[i] = name;
            }
            return labels;
        } catch(SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }
    }

    /** Returns ordered list of Java class names that
      *  should be used for fetched values. */
    public String[] getResultTypes(ResultSet rs) {
        ObjAttribute[] attrs = getRawQuery().getResultDesc();
        if(attrs == null || attrs.length == 0)
            return getResultTypesFromMetadata(rs);

        int len = attrs.length;
        String[] types = new String[len];
        for(int i = 0; i < len; i++) {
            types[i] = attrs[i].getType();
        }
        return types;
    }


    /** Returns ordered list of Java class names that
      * should be used for fetched values according to default Java
      * class to JDBC type mapping. This is a failover method to
      * obtain Java types used when query has no such data. */
    public String[] getResultTypesFromMetadata(ResultSet rs) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int len = md.getColumnCount();
            if(len == 0)
                throw new CayenneRuntimeException("No columns in ResultSet.");

            String[] types = new String[len];
            for(int i = 0; i < len; i++) {
                int sqlType = md.getColumnType(i + 1);
                types[i] = TypesMapping.getJavaBySqlType(sqlType);
            }
            return types;
        } catch(SQLException sqex) {
            logObj.log(Level.SEVERE, "Error", sqex);
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }
    }

    public String aliasForTable(DbEntity dbEnt) {
        throw new RuntimeException("aliases not supported");
    }


    public void dbRelationshipAdded(DbRelationship dbRel) {
        throw new RuntimeException("db relationships not supported");
    }


    public boolean supportsTableAlases() {
        return false;
    }
}
