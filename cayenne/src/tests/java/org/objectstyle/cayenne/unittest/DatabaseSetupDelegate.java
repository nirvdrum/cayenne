/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
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

package org.objectstyle.cayenne.unittest;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.db2.DB2Adapter;
import org.objectstyle.cayenne.dba.firebird.FirebirdAdapter;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.objectstyle.cayenne.dba.mysql.MySQLAdapter;
import org.objectstyle.cayenne.dba.openbase.OpenBaseAdapter;
import org.objectstyle.cayenne.dba.oracle.OracleAdapter;
import org.objectstyle.cayenne.dba.postgres.PostgresAdapter;
import org.objectstyle.cayenne.dba.sybase.SybaseAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.util.Util;

/**
 * Defines API and a common superclass for testing various datbase features.
 * Different databases support different feature sets that need to be tested
 * differently. Many things implemented in subclasses may become future
 * candidates for inclusin in the corresponding adapter code.
 * 
 * @author Andrei Adamchik
 */
public class DatabaseSetupDelegate {
    private static Logger logObj =
        Logger.getLogger(DatabaseSetupDelegate.class);

    protected static Map delegates = new HashMap();

    static {
        delegates.put(OracleAdapter.class, OracleDelegate.class);
        delegates.put(SybaseAdapter.class, SybaseDelegate.class);
        delegates.put(FirebirdAdapter.class, FirebirdDelegate.class);
        delegates.put(PostgresAdapter.class, PostgresDelegate.class);
        delegates.put(MySQLAdapter.class, MySQLDelegate.class);
        delegates.put(HSQLDBAdapter.class, HSQLDBDelegate.class);
        delegates.put(OpenBaseAdapter.class, OpenBaseDelegate.class);
        delegates.put(DB2Adapter.class, DB2SetupDelegate.class);
    }

    protected DbAdapter adapter;

    public static DatabaseSetupDelegate createDelegate(DbAdapter adapter) {
        Class delegateClass = (Class) delegates.get(adapter.getClass());
        if (delegateClass != null) {
            try {
                Constructor c =
                    delegateClass.getConstructor(
                        new Class[] { DbAdapter.class });
                return (DatabaseSetupDelegate) c.newInstance(
                    new Object[] { adapter });
            } catch (Exception ex) {
                throw new CayenneRuntimeException(
                    "Error instantiating delegate.",
                    ex);
            }
        }
        return new DatabaseSetupDelegate(adapter);
    }

    protected DatabaseSetupDelegate(DbAdapter adapter) {
        this.adapter = adapter;
    }

    public void willDropTables(Connection con, DataMap map) throws Exception {

    }

    public void droppedTables(Connection con, DataMap map) throws Exception {

    }
    
    /**
     * Callback method that allows Delegate to customize
     * test procedure.
     */
    public void tweakProcedure(Procedure proc) {
    }

    public void willCreateTables(Connection con, DataMap map)
        throws Exception {
    }

    public void createdTables(Connection con, DataMap map) throws Exception {

    }

    public boolean supportsStoredProcedures() {
        return false;
    }
    
    /**
     * Returns true if the target database has support for large objects (BLOB,
     * CLOB).
     */
    public boolean supportsLobs() {
    	return false;
    }
    
    public boolean supportsBinaryPK() {
        return true;
    }
    
    public boolean supportsHaving() {
        return true;
    }
    
    public boolean supportsDroppingPK() {
        return true;
    }
    
    protected void executeDDL(Connection con, String ddl) throws Exception {
        logObj.info(ddl);
        Statement st = con.createStatement();

        try {
            st.execute(ddl);
        } finally {
            st.close();
        }
    }

    protected void executeDDL(Connection con, File sourceFile)
        throws Exception {
        // not sure if all JDBC adapters will like multiline statements
        // separated with '\n'. Oracle & Sybase seem OK, though
        // joining with space is probably safer, though produces agly code
        String ddl = Util.stringFromFile(sourceFile, "\n");
        executeDDL(con, ddl);
    }

    /**
     * Returns a file under test resources DDL directory for the specified
     * database.
     */
    protected File ddlFile(String database, String name) {
        return new File(
            new File(
                new File(CayenneTestCase.getDefaultTestResourceDir(), "ddl"),
                database),
            name);
    }

    public boolean handlesNullVsEmptyLOBs() {
        return supportsLobs();
    }
}
