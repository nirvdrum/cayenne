package org.objectstyle.cayenne.dba.sybase;
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
import java.util.HashMap;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationSorter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DbEntity;

/** 
 * DbAdapter implementation for 
 * <a href="http://www.sybase.com">Sybase RDBMS</a>.
 *
 * @author Andrei Adamchik
 */
public class SybaseAdapter extends JdbcAdapter {
    protected HashMap sorters = new HashMap();

    public OperationSorter getOpSorter(DataNode node) {
        synchronized (sorters) {
            OperationSorter sorter = (OperationSorter) sorters.get(node);
            if (sorter == null) {
                sorter = new OperationSorter(node, node.getDataMaps());
                sorters.put(node, sorter);
            }
            return sorter;
        }
    }

    /** Generates database objects to provide
     *  automatic primary key support. Method will execute the following
     *  SQL statements:
     * 
     * <p>1. Executed only if a corresponding table does not exist in the
     * database.</p>
     * 
     * <pre>
     *    CREATE TABLE AUTO_PK_SUPPORT (
     *       TABLE_NAME VARCHAR(32) NOT NULL,
     *       NEXT_ID INTEGER NOT NULL
     *    )
     * </pre>
     * 
     * <p>2. Executed under any circumstances. </p>
     * 
     * <pre> 
     * if (select count(*) from sysobjects where name = 'auto_pk_for_table') = 1
     * BEGIN
     *    DROP PROCEDURE auto_pk_for_table 
     * END
     * 
     * <p>3. Executed under any circumstances. </p>
     * CREATE PROCEDURE auto_pk_for_table @tname VARCHAR(32) AS
     * BEGIN
     *      BEGIN TRANSACTION
     *         UPDATE AUTO_PK_SUPPORT set NEXT_ID = NEXT_ID + 1 
     *         WHERE TABLE_NAME = @tname
     * 
     *         SELECT NEXT_ID from AUTO_PK_SUPPORT where NEXT_ID = @tname
     *      COMMIT
     * END
     * </pre>
     *
     *  @param node node that provides access to a DataSource.
     */
    public void createAutoPkSupport(DataNode node) throws Exception {

        // need to drop procedure first
        pkGen.runSchemaUpdate(node, safePkProcDrop());

        // create objects
        super.createAutoPkSupport(node);
        pkGen.runSchemaUpdate(node, unsafePkProcCreate());
    }

    /** 
     * Drops database objects related to automatic primary
     * key support. Method will execute the following SQL
     * statements:
     * 
     * <pre>
     * if exists (SELECT * FROM sysobjects WHERE name = 'AUTO_PK_SUPPORT')
     * BEGIN
     *    DROP TABLE AUTO_PK_SUPPORT
     * END
     * 
     * 
     * if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table')
     * BEGIN
     *    DROP PROCEDURE auto_pk_for_table 
     * END
     * </pre>
     *
     *  @param node node that provides access to a DataSource.
     */
    public void dropAutoPkSupport(DataNode node) throws Exception {
        pkGen.runSchemaUpdate(node, safePkProcDrop());
        pkGen.runSchemaUpdate(node, safePkTableDrop());
    }

    public Object generatePkForDbEntity(DataNode dataNode, DbEntity dbEntity)
        throws Exception {

        Connection con = dataNode.getDataSource().getConnection();

        try {
            CallableStatement st = con.prepareCall("{call auto_pk_for_table(?)}");

            try {
                st.setString(1, dbEntity.getName());
                ResultSet rs = st.executeQuery();

                Object pk = null;
                if (rs.next()) {
                    pk = new Integer(rs.getInt(1));
                }

                rs.close();

                if (pk == null) {
                    throw new CayenneRuntimeException(
                        "Error generating pk for DbEntity " + dbEntity.getName());
                }

                return pk;
            }
            finally {
                st.close();
            }
        }
        finally {
            con.close();
        }
        // return super.generatePkForDbEntity(dataNode, dbEntity);
    }

    private String safePkTableCreate() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("if (SELECT count(*) FROM sysobjects WHERE name = 'AUTO_PK_SUPPORT') = 0")
            .append(" BEGIN ")
            .append(" CREATE TABLE AUTO_PK_SUPPORT (")
            .append(" TABLE_NAME VARCHAR(32) NOT NULL,")
            .append(" NEXT_ID INTEGER NOT NULL")
            .append(" )")
            .append(" END");

        return buf.toString();
    }

    private String safePkTableDrop() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("if exists (SELECT * FROM sysobjects WHERE name = 'AUTO_PK_SUPPORT')")
            .append(" BEGIN ")
            .append(" DROP TABLE AUTO_PK_SUPPORT")
            .append(" END");

        return buf.toString();
    }

    private String unsafePkProcCreate() {
        StringBuffer buf = new StringBuffer();
        buf
            .append(" CREATE PROCEDURE auto_pk_for_table @tname VARCHAR(32) AS")
            .append(" BEGIN")
            .append(" BEGIN TRANSACTION")
            .append(" UPDATE AUTO_PK_SUPPORT set NEXT_ID = NEXT_ID + 1")
            .append(" WHERE TABLE_NAME = @tname")
            .append(" SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = @tname")
            .append(" COMMIT")
            .append(" END");
        return buf.toString();
    }

    private String safePkProcDrop() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table')")
            .append(" BEGIN")
            .append(" DROP PROCEDURE auto_pk_for_table")
            .append(" END");
        return buf.toString();
    }

}