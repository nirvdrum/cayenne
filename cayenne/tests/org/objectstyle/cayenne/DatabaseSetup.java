package org.objectstyle.cayenne;
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

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.query.*;
import org.objectstyle.util.*;
import org.objectstyle.cayenne.gui.*;
import org.objectstyle.cayenne.conn.*;
import org.objectstyle.cayenne.map.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.sql.*;
import org.objectstyle.cayenne.dba.*;


/** Setup database connection info to run tests. */
public class DatabaseSetup {
    static Logger logObj = Logger.getLogger(DatabaseSetup.class.getName());


    public static final String[] TEST_TABLES = new String[] {
                "AUTO_PK_SUPPORT", "ARTIST", "GALLERY", "EXHIBIT", "ARTIST_EXHIBIT",
                "PAINTING", "PAINTING_INFO"
            };


    protected DataMap map;

    public DatabaseSetup(DataMap map) throws Exception {
        this.map = map;
    }


    public void dropTestTables() throws Exception {
        Connection conn = org.objectstyle.TestMain.getSharedConnection();
        DatabaseMetaData md = conn.getMetaData();
        ResultSet tables = md.getTables(null, null, "%", null);
        ArrayList allTables = new ArrayList();
        while(tables.next()) {
            // 'toUpperCase' is needed since most databases
            // are case insensitive, and some will convert names to lower case (PostgreSQL)
            String name = tables.getString("TABLE_NAME");
            if(name != null)
                allTables.add(name.toUpperCase());
        }
        tables.close();


        // drop tables in reverse order of insert. this will take care of
        // referential constraints...
        Statement stmt = conn.createStatement();
        for(int i = TEST_TABLES.length - 1; i >= 0; i--) {
            if(!allTables.contains(TEST_TABLES[i]))
                continue;

            try {
                String dropSql = "DROP TABLE " + TEST_TABLES[i];
                stmt.execute(dropSql);
                logObj.fine("Dropped table " + TEST_TABLES[i]);
            } catch(SQLException sqe) {
                logObj.log(Level.FINE, "Can't drop table " + TEST_TABLES[i], sqe);
            }
        }
    }



    public void setupTestTables() throws Exception {
        Connection conn = org.objectstyle.TestMain.getSharedConnection();

        Statement stmt = conn.createStatement();


        Iterator it = tableCreateQueries();
        while(it.hasNext()) {
            SqlModifyQuery query = (SqlModifyQuery)it.next();
            logObj.warning("Create table: " + query.getSqlString());
            stmt.execute(query.getSqlString());
        }
    }


    private void applyOracleHack() {
        DbEntity paintingInfo = map.getDbEntity("PAINTING_INFO");
        DbAttribute textReview = (DbAttribute)paintingInfo.getAttribute("TEXT_REVIEW");
        textReview.setType(Types.VARCHAR);
        textReview.setMaxLength(255);
    }

    /** Return iterator of preprocessed table create queries */
    public Iterator tableCreateQueries() throws Exception {
        ArrayList queries = new ArrayList();
        DbAdapter adapter = org.objectstyle.TestMain.getSharedNode().getAdapter();
        DbGenerator gen = new DbGenerator(org.objectstyle.TestMain.getSharedConnection(), adapter);

        // Oracle does not support more then 1 "LONG xx" column per table
        // PAINTING_INFO need to be fixed
        if(adapter instanceof org.objectstyle.cayenne.dba.oracle.OracleAdapter) {
            applyOracleHack();
        }

        for(int i = 0; i < TEST_TABLES.length; i++) {
            DbEntity ent = ("AUTO_PK_SUPPORT".equals(TEST_TABLES[i]))
                           ? pkEntity()
                           : map.getDbEntity(TEST_TABLES[i]);
            queries.add(gen.createTableQuery(ent));
        }

        // add FK constraints
        if(adapter.supportsFkConstraints()) {
            for(int i = 0; i < TEST_TABLES.length; i++) {
                if("AUTO_PK_SUPPORT".equals(TEST_TABLES[i]))
                    continue;

                DbEntity ent = map.getDbEntity(TEST_TABLES[i]);
                List qs = gen.createFkConstraintsQueries(ent);
                queries.addAll(qs);
            }
        }

        return queries.iterator();
    }

    // temp hack
    private DbEntity pkEntity() {
        DbEntity ent = new DbEntity("AUTO_PK_SUPPORT");
        DbAttribute at1 = new DbAttribute("TABLE_NAME", Types.VARCHAR, null);
        at1.setMandatory(true);
        at1.setMaxLength(100);
        ent.addAttribute(at1);
        at1.setPrimaryKey(true);

        DbAttribute at2 = new DbAttribute("NEXT_ID", Types.INTEGER, null);
        at2.setMandatory(true);
        ent.addAttribute(at2);
        return ent;
    }
}
