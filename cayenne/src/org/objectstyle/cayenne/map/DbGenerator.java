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

package org.objectstyle.cayenne.map;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.TypesMapping;

/** Utility class that does forward engineering of the database.
  * It can generate database schema using the data map. It is a 
  * counterpart of DbLoader class. 
  * 
  * <p>It is a responsibility of calling code to close connection
  * DbGenerator was initialized with or perform any other cleanup.
  *
  * @author Andrei Adamchik
 */
public class DbGenerator {

    private Connection con;
    private DbAdapter adapter;

    /** Creates and initializes new DbGenerator. */
    public DbGenerator(Connection con, DbAdapter adapter) {
        this.con = con;
        this.adapter = adapter;
    }

    /** Returns DbAdapter associated with this DbGenerator. */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /** Returns JDBC connection object associated with this DbGenerator. */
    public Connection getCon() {
        return con;
    }

    /** Creates database tables using the information from the
      * <code>map</code>. Does not drop any existsing tables. */
    public void createTables(DataMap map) throws SQLException {
        createTables(map, false);
    }

    /** 
     * Creates database tables using the information from the
     * <code>map</code>. Depending on <code>drop</code> flag
     * value may also drop existing tables. 
     */
    public void createTables(DataMap map, boolean drop) throws SQLException {
        Statement stmt = con.createStatement();
        DatabaseMetaData meta = con.getMetaData();

        try {
            // DROP TABLE
            if (drop) {
                Iterator it = map.getDbEntitiesAsList().iterator();
                while (it.hasNext()) {
                    ResultSet rs =
                        meta.getTables(null, null, ((DbEntity) it.next()).getName(), null);
                    // If no such table, don't try to delete, just continue
                    if (!rs.next())
                        continue;
                    String q = adapter.dropTable((DbEntity) it.next());
                    QueryLogger.logQuery(Level.INFO, q, null);
                    stmt.execute(q);
                }
            }

            // CREATE TABLE
            // Note: if drop was requested, we should recreate all
            // tables in the map, if not - just those that are missing
            List createThese =
                (drop) ? map.getDbEntitiesAsList() : filterNonExistentTables(map);

            Iterator it = createThese.iterator();
            while (it.hasNext()) {
                String q = createTableQuery((DbEntity) it.next());
                QueryLogger.logQuery(Level.INFO, q, null);
                stmt.execute(q);
            }

            // now see if we need FK constraints
            if (adapter.supportsFkConstraints()) {
                Iterator it2 = createThese.iterator();
                while (it2.hasNext()) {
                    List list = createFkConstraintsQueries((DbEntity) it2.next());

                    Iterator cit = list.iterator();
                    while (cit.hasNext()) {
                        String cq = (String) cit.next();
                        QueryLogger.logQuery(Level.INFO, cq, null);
                        stmt.execute(cq);
                    }
                }
            }
        }
        finally {
            stmt.close();
        }
    }

    /** Returns a query that can be used to create database table
      * corresponding to <code>ent</code> parameter. */
    public String createTableQuery(DbEntity ent) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(ent.getName()).append(" (");

        // columns
        Iterator it = ent.getAttributeList().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            }
            else {
                buf.append(", ");
            }

            DbAttribute at = (DbAttribute) it.next();
            String type = adapter.externalTypesForJdbcType(at.getType())[0];

            buf.append(at.getName()).append(' ').append(type);

            // append size and precision (if applicable)
            if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int prec = TypesMapping.isDecimal(at.getType()) ? at.getPrecision() : -1;

                // sanity check
                if (prec > len) {
                    prec = -1;
                }

                if (len > 0) {
                    buf.append('(').append(len);
                    
                    if (prec >= 0) {
                        buf.append(", ").append(prec);
                    }
                    
                    buf.append(')');
                }
            }

            if (at.isMandatory())
                buf.append(" NOT");

            buf.append(" NULL");
        }

        // primary key clause
        Iterator pkit = ent.getPrimaryKey().iterator();
        if (pkit.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    buf.append(", ");

                DbAttribute at = (DbAttribute) pkit.next();
                buf.append(at.getName());
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
    }

    /** Returns an array of queries to create foreign key constraints
     * for a particular DbEntity. Throws CayenneRuntimeException, if called
     * for adapter that does not support FK constraints. */
    public List createFkConstraintsQueries(DbEntity dbEnt) {
        if (!adapter.supportsFkConstraints())
            throw new CayenneRuntimeException("FK constraints are not supported by adapter.");
        ArrayList list = new ArrayList();
        Iterator it = dbEnt.getRelationshipList().iterator();
        while (it.hasNext()) {
            DbRelationship rel = (DbRelationship) it.next();
            if (!rel.isToMany() && !rel.isToDependentPK())
                list.add(adapter.createFkConstraint(rel));
        }
        return list;
    }

    /** Returns a subset of DbEntities from the <code>map</code>
     *  that have no corresponding database tables. 
     * 
     * @throws SQLException if an error occurred while processing
     * a list of database tables. 
     */
    private List filterNonExistentTables(DataMap map) throws SQLException {
        // read a list of tables
        DatabaseMetaData md = con.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", null);
        ArrayList tables = new ArrayList();
        while (rs.next()) {
            tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        rs.close();

        // find tables that are in the map but not in the database
        ArrayList missing = new ArrayList();
        Iterator it = map.getDbEntitiesAsList().iterator();
        while (it.hasNext()) {
            DbEntity e = (DbEntity) it.next();
            if (!tables.contains(e.getName().toLowerCase())) {
                missing.add(e);
            }
        }
        return missing;
    }
}