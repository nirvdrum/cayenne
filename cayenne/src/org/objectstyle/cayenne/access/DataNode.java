package org.objectstyle.cayenne.access;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import org.objectstyle.cayenne.access.trans.SelectQueryAssembler;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.Query;


/** Wrapper class for javax.sql.DataSource. Links Cayenne framework
  * with JDBC layer, providing query execution facilities.
  *
  * @author Andrei Adamchik
  */
public class DataNode implements QueryEngine {
    static Logger logObj = Logger.getLogger(DataNode.class.getName());

    public static final String DEFAULT_ADAPTER_CLASS = "org.objectstyle.cayenne.dba.JdbcAdapter";

    private static final DataMap[] noDataMaps = new DataMap[0];


    protected String name;
    protected DataSource dataSource;
    protected DataMap[] dataMaps;
    protected DbAdapter adapter;
    protected String dataSourceLocation;
    protected String dataSourceFactory;


    /** Creates unnamed DataNode */
    public DataNode() {}

    /** Creates DataNode and assigns <code>name</code> to it. */
    public DataNode(String name) {
        this.name = name;

        // make sure it is not null - set to static immutable object
        this.dataMaps = noDataMaps;
    }


    // setters/getters

    /** Returns node "name" property. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /** Returns a location of DataSource of this node. */
    public String getDataSourceLocation() {
        return dataSourceLocation;
    }

    public void setDataSourceLocation(String dataSourceLocation) {
        this.dataSourceLocation = dataSourceLocation;
    }
    
    
    /** Returns a name of DataSourceFactory class for this node. */
    public String getDataSourceFactory() {
        return dataSourceFactory;
    }

    public void setDataSourceFactory(String dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }


    public DataMap[] getDataMaps() {
        return dataMaps;
    }

    public void setDataMaps(DataMap[] dataMaps) {
        this.dataMaps = dataMaps;
    }

    public void addDataMap(DataMap map) {
        // note to self - implement it as a List
        // to avoid this ugly resizing in the future
        if (dataMaps == null)
            dataMaps = new DataMap[] {map};
        else {
            DataMap[] newMaps = new DataMap[dataMaps.length + 1];
            System.arraycopy(dataMaps, 0, newMaps, 0, dataMaps.length);
            newMaps[dataMaps.length] = map;
            dataMaps = newMaps;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    /** Returns DbAdapter object. This is a plugin for
      * that handles RDBMS vendor-specific features. */
    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }


    // other methods

    /** Lookup an entity by name across all node maps. */
    public ObjEntity lookupEntity(String objEntityName) {
        DataMap[] maps = this.getDataMaps();
        int mapLen = maps.length;
        for (int i = 0; i < mapLen; i++) {
            ObjEntity anEntity = maps[i].getObjEntity(objEntityName);
            if (anEntity != null)
                return anEntity;
        }
        return null;
    }


    /** Run multiple queries using one of the pooled connections. */
    public void performQueries(List queries, OperationObserver opObserver) {
        Level logLevel = opObserver.queryLogLevel();

        int listSize = queries.size();
        QueryLogger.logQueryStart(logLevel, listSize);
        if (listSize == 0)
            return ;

        Connection con = null;
        boolean usesAutoCommit = opObserver.useAutoCommit();
        boolean rolledBackFlag = false;

        try {
            // check out connection, create statement
            con = this.getDataSource().getConnection();
            con.setAutoCommit(usesAutoCommit);

            // give a chance to order queries
            queries = opObserver.orderQueries(this, queries);

            // just in case recheck list size....
            listSize = queries.size();

            for (int i = 0; i < listSize; i++) {
                Query nextQuery = (Query)queries.get(i);

                // catch exceptions for each individual query
                try {
                    // 1. translate query
                    QueryTranslator queryTranslator = QueryTranslator.queryTranslator(this, con, getAdapter(), nextQuery);
                    PreparedStatement prepStmt = queryTranslator.createStatement(logLevel);
                    if (nextQuery.getQueryType() == Query.SELECT_QUERY) {
                        // 2.a execute query
                        ResultSet rs = prepStmt.executeQuery();
                        String[] snapshotLabels = ((SelectQueryAssembler)queryTranslator).getSnapshotLabels(rs);
                        String[] resultTypes = ((SelectQueryAssembler)queryTranslator).getResultTypes(rs);
                        List resultSnapshots = snapshotsFromResultSet(rs, snapshotLabels, resultTypes);
                        QueryLogger.logSelectCount(logLevel, resultSnapshots.size());
                        rs.close();

                        // 3.a send results back to consumer
                        opObserver.nextSnapshots(nextQuery, resultSnapshots);
                    } else {
                        // 2.b execute update
                        int count = prepStmt.executeUpdate();
                        QueryLogger.logUpdateCount(logLevel, count);

                        // 3.b send results back to consumer
                        opObserver.nextCount(nextQuery, count);
                    }
                } catch (Exception queryEx) {
                    QueryLogger.logQueryError(logLevel, queryEx);

                    // notify consumer of the exception,
                    // stop running further queries
                    opObserver.nextQueryException(nextQuery, queryEx);

                    if (!usesAutoCommit) {
                        // rollback transaction
                        try {
                            rolledBackFlag = true;
                            con.rollback();
                            QueryLogger.logRollbackTransaction(logLevel);
                            opObserver.transactionRolledback();
                        } catch (SQLException sqlEx) {
                            opObserver.nextQueryException(nextQuery, sqlEx);
                        }
                    }

                    break;
                }
            }

            // commit transaction if needed
            if (!rolledBackFlag && !usesAutoCommit) {
                con.commit();
                QueryLogger.logCommitTransaction(logLevel);
                opObserver.transactionCommitted();
            }

        }
        // catch stuff like connection allocation errors, etc...
        catch (Exception globalEx) {
            QueryLogger.logQueryError(logLevel, globalEx);
            
            if (!usesAutoCommit) {
                // rollback failed transaction
                rolledBackFlag = true;

                try {
                    con.rollback();
                    QueryLogger.logRollbackTransaction(logLevel);
                    opObserver.transactionRolledback();
                } catch (SQLException ex) {
                    // do nothing....
                }
            }

            opObserver.nextGlobalException(globalEx);
        } finally {
            try {
                // return connection to the pool if it was checked out
                if (con != null)
                    con.close();
            }
            // finally catch connection closing exceptions...
            catch (Exception finalEx) {
                opObserver.nextGlobalException(finalEx);
            }
        }
    }

    public void performQuery(Query query, OperationObserver opObserver) {
        ArrayList qWrapper = new ArrayList(1);
        qWrapper.add(query);
        this.performQueries(qWrapper, opObserver);
    }


    /** Creates primary key support for all node DbEntities.
     *  Will use its facilities provided by DbAdapter to generate
     *  any necessary database objects and data for primary
     *  key support. */
    public void createPkSupportForMapEntities() throws Exception {
        // generate common PK support
        adapter.createAutoPkSupport(this);
        
        // generate PK support for each indiv. entity.
        int len = dataMaps.length;
        for (int i = 0; i < len; i++) {
            DbEntity[] ents = dataMaps[i].getDbEntities();
            for (int j = 0; j < ents.length; j++) {
                adapter.createAutoPkSupportForDbEntity(this, ents[j]);
            }
        }
    }



    /** Will process a result set instantiating a list of maps with data. */
    public List snapshotsFromResultSet(ResultSet rs, String[] snapshotLabels, String[] resultTypes) throws Exception {
        ArrayList snapshots = new ArrayList();
        int len = snapshotLabels.length;
        ExtendedType[] converters = new ExtendedType[len];
        ExtendedTypeMap typeMap = ExtendedTypeMap.sharedInstance();
        for (int i = 0; i < len; i++) {
            converters[i] = typeMap.getRegisteredType(resultTypes[i]);
        }

        while (rs.next()) {
            HashMap map = new HashMap();
            snapshots.add(map);

            // process result row columns,
            // set object properties right away,
            // FK & PK columns will be stored in temp maps that will be converted to id's later
            Object fetchedValue = null;
            for (int i = 0; i < len; i++) {
                // note: jdbc column indexes start from 1 , not 0 as in arrays
                Object val = converters[i].materializeObject(rs, i + 1);
                map.put(snapshotLabels[i], val);
            }
        }

        return snapshots;
    }
}
