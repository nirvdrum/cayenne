package org.objectstyle.cayenne.dba.oracle;
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
import java.util.*;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationSorter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;

/** DbAdapter implementation for <a href="http://www.oracle.com">Oracle RDBMS</a>. */
public class OracleAdapter extends JdbcAdapter {
    static Logger logObj = Logger.getLogger(OracleAdapter.class.getName());

    private static final String _SEQUENCE_PREFIX = "pk_";

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

    /** Returns a query string to drop a table corresponding
      * to <code>ent</code> DbEntity. Changes superclass behavior
      * to drop all related foreign key constraints. */
    public String dropTable(DbEntity ent) {
        return "DROP TABLE " + ent.getName() + " CASCADE CONSTRAINTS";
    }

    /** 
     * This is a noop for Oracle. Primary keys are generated from sequences
     * created for each individual table. Therefore  no common setup required.    *  
     *
     *  @param node node that provides access to a DataSource.
     */
    public void createAutoPkSupport(DataNode node) throws Exception {
        // do nothing
    }

    /** 
     * Creates sequences for each DbEntity. First checks if a corresponding
     * sequence exists in the database:
     * 
     * <pre>
     * SELECT SEQUENCE_NAME FROM USER_SEQUENCES 
     * </pre>
     * 
     * If a DbEntity doesn't have PK support yet, executes the 
     * following SQL:
     * 
     * <pre>
     * CREATE SEQUENCE pk_table_name
     * </pre>
     *
     *  @param node node that provides access to a DataSource.
     */
    public void createAutoPkSupportForDbEntity(DataNode node, DbEntity dbEntity)
        throws Exception {

        Connection con = node.getDataSource().getConnection();
        try {
            List existing = getExistingSequences(con, dbEntity);
            if (existing == null || existing.size() == 0) {
                Statement upd = con.createStatement();
                try {
                    upd.executeUpdate("CREATE SEQUENCE " + sequenceName(dbEntity));
                }
                finally {
                    upd.close();
                }
            }
        }
        finally {
            con.close();
        }
    }

    /** 
     * Drops all sequences for DataMap tables. First checks if a corresponding
     * sequence exists in the database:
     * 
     * <pre>
     * SELECT SEQUENCE_NAME FROM USER_SEQUENCES 
     * </pre>
     * 
     * For each DbEntity, that has PK sequence (named like "pk_table_name", 
     * executes the following SQL (drop statements are executed in a single batch):
     * 
     * <pre>
     * DROP SEQUENCE pk_table_name
     * </pre>
     *
     *  @param node node that provides access to a DataSource.
     */
    public void dropAutoPkSupport(DataNode node) throws Exception {
        DataMap[] maps = node.getDataMaps();

        Connection con = node.getDataSource().getConnection();
        try {
            List existing = getExistingSequences(con, null);
            Statement upd = con.createStatement();
            try {
                boolean needToDrop = false;
                for (int i = 0; i < maps.length; i++) {
                    
                    DbEntity[] ents = maps[i].getDbEntities();
                    for (int j = 0; j < ents.length; j++) {
                        
                        if (existing.contains(sequenceName(ents[j]))) {
                            upd.addBatch("DROP SEQUENCE " + sequenceName(ents[j]));
                            needToDrop = true;
                        }
                    }
                }

                if (needToDrop) {
                    upd.executeBatch();
                }
            }
            finally {
                upd.close();
            }
        }
        finally {
            con.close();
        }
    }

    /** 
     * Generates primary key by calling Oracle sequence corresponding to the
     * <code>dbEntity</code>. Executed SQL looks like this:
     * 
     * <pre>
     * SELECT pk_table_name.nextval FROM DUAL
     * </pre>
     */
    public Object generatePkForDbEntity(DataNode dataNode, DbEntity dbEntity)
        throws Exception {

        Connection con = dataNode.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {
                ResultSet rs =
                    st.executeQuery("SELECT " + sequenceName(dbEntity) + ".nextval FROM DUAL");

                try {
                    Object pk = null;
                    if (rs.next()) {
                        pk = new Integer(rs.getInt(1));
                    }

                    if (pk == null) {
                        throw new CayenneRuntimeException(
                            "Error generating pk for DbEntity " + dbEntity.getName());
                    }

                    return pk;
                }
                finally {
                    rs.close();
                }
            }
            finally {
                st.close();
            }
        }
        finally {
            con.close();
        }
    }

    /** Returns expected primary key sequence name for a DbEntity. */
    protected String sequenceName(DbEntity ent) {
        return _SEQUENCE_PREFIX + ent.getName().toLowerCase();
    }

    /** 
     * Fetches a list of existing sequences that might match Cayenne
     * generated ones. If <code>dbEnt</code> parameter is not null,
     * only tries to retrieve sequence related to this entity.
     */
    protected List getExistingSequences(Connection con, DbEntity ent)
        throws SQLException {
        // check existsing sequences
        Statement sel = con.createStatement();
        try {
            StringBuffer q = new StringBuffer();
            q.append(
                "SELECT LOWER(SEQUENCE_NAME) FROM USER_SEQUENCES WHERE LOWER(SEQUENCE_NAME)");

            if (ent != null) {
                q.append(" = '").append(sequenceName(ent)).append('\'');
            }
            else {
                q.append(" LIKE '").append(_SEQUENCE_PREFIX).append("%'");
            }

            ResultSet rs = sel.executeQuery(q.toString());

            try {
                List sequenceList = new ArrayList();
                while (rs.next()) {
                    sequenceList.add(rs.getString(1));
                }
                return sequenceList;
            }
            finally {
                rs.close();
            }
        }
        finally {
            sel.close();
        }
    }
}