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

package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.trans.SelectQueryAssembler;
import org.objectstyle.cayenne.dba.BatchInterpreter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.Query;

/** Wrapper class for javax.sql.DataSource. Links Cayenne framework
  * with JDBC layer, providing query execution facilities.
  *
  * <p><i>For more information see <a href="../../../../../../userguide/index.html"
  * target="_top">Cayenne User Guide.</a></i></p>
  *
  * @author Andrei Adamchik
  */
public class DataNode implements QueryEngine {
    private static Logger logObj = Logger.getLogger(DataNode.class);

    public static final Class DEFAULT_ADAPTER_CLASS = JdbcAdapter.class;

    protected String name;
    protected DataSource dataSource;
    protected DbAdapter adapter;
    protected String dataSourceLocation;
    protected String dataSourceFactory;
    protected EntityResolver entityResolver = new EntityResolver();
    protected RefIntegritySupport refIntegritySupport;

    /** Creates unnamed DataNode */
    public DataNode() {
    }

    /** Creates DataNode and assigns <code>name</code> to it. */
    public DataNode(String name) {
        this.name = name;
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

    /**
     * Returns a list of DataMaps handled by this DataNode.
     */
    public List getDataMapsAsList() {
        return entityResolver.getDataMapsList();
    }

    /**
     * Returns an array of DataMaps handled by this DataNode.
     * @deprecated since b1; use #getDataMapsAsList() instead.
     */
    public DataMap[] getDataMaps() {
        List maps = entityResolver.getDataMapsList();
        DataMap[] mapsArray = new DataMap[maps.size()];
        return (DataMap[]) maps.toArray(mapsArray);
    }

    public void setDataMaps(List dataMaps) {
        refIntegritySupport = null;
        entityResolver.setDataMaps(dataMaps);
    }

    /**
     * Adds a DataMap to be handled by this node.
     */
    public void addDataMap(DataMap map) {
        entityResolver.addDataMap(map);
        refIntegritySupport = null;
    }

    public void removeDataMap(String mapName) {
        DataMap map = entityResolver.getDataMap(mapName);
        if (map != null) {
            entityResolver.removeDataMap(map);
            refIntegritySupport = null;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns DbAdapter object. This is a plugin
     * that handles RDBMS vendor-specific features.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    // other methods

    /**
     * Returns this object if it can handle queries for <code>objEntity</code>,
     * returns null otherwise.
     */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
        return (
            this.getEntityResolver().lookupObjEntity(objEntity.getName())
                != null)
            ? this
            : null;
    }

    /** Run multiple queries using one of the pooled connections. */
    public void performQueries(List queries, OperationObserver opObserver) {
        Level logLevel = opObserver.getLoggingLevel();

        int listSize = queries.size();
        QueryLogger.logQueryStart(logLevel, listSize);
        if (listSize == 0) {
            return;
        }

        Connection con = null;
        boolean usesAutoCommit = opObserver.useAutoCommit();
        boolean rolledBackFlag = false;

        try {
            // check for invalid iterated query
            if (opObserver.isIteratedResult() && listSize > 1) {
                throw new CayenneException(
                    "Iterated queries are not allowed in a batch. Batch size: "
                        + listSize);
            }

            // check out connection, create statement
            con = this.getDataSource().getConnection();
            if (con.getAutoCommit() != usesAutoCommit) {
                con.setAutoCommit(usesAutoCommit);
            }

            // give a chance to order queries
            queries = opObserver.orderQueries(this, queries);

            // just in case, recheck list size....
            listSize = queries.size();

            for (int i = 0; i < listSize; i++) {
                Query nextQuery = (Query) queries.get(i);

                // catch exceptions for each individual query
                try {
                    BatchInterpreter interpreter = null;
                    switch (nextQuery.getQueryType()) {
                        case Query.INSERT_BATCH_QUERY :
                            interpreter =
                                getAdapter().getInsertBatchInterpreter();
                            break;
                        case Query.UPDATE_BATCH_QUERY :
                            interpreter =
                                getAdapter().getUpdateBatchInterpreter();
                            break;
                        case Query.DELETE_BATCH_QUERY :
                            interpreter =
                                getAdapter().getDeleteBatchInterpreter();
                            break;
                    }
                    if (interpreter != null) {
                        interpreter.execute((BatchQuery) nextQuery, con);
                        continue;
                    }
                    // translate query
                    QueryTranslator transl =
                        getAdapter().getQueryTranslator(nextQuery);
                    transl.setEngine(this);
                    transl.setCon(con);

                    PreparedStatement prepStmt =
                        transl.createStatement(logLevel);

                    // if ResultIterator is returned to the user,
                    // DataNode is not responsible for closing the connections
                    // exception handling, and other housekeeping
                    if (opObserver.isIteratedResult()) {
                        // trick "finally" to avoid closing connection here
                        // it will be closed by the ResultIterator
                        con = null;
                        runIteratedSelect(opObserver, prepStmt, transl);
                        return;
                    }

                    if (nextQuery.getQueryType() == Query.SELECT_QUERY) {
                        runSelect(opObserver, prepStmt, transl);
                    } else if (
                        nextQuery.getQueryType()
                            != Query.STORED_PROCEDURE_QUERY) {
                        runUpdate(opObserver, prepStmt, transl);
                    } else {
                        runProcedure(opObserver, prepStmt, transl);
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
                            QueryLogger.logQueryError(logLevel, sqlEx);
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

            if (!usesAutoCommit && con != null) {
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
                if (con != null) {
                    con.close();
                }
            }
            // finally catch connection closing exceptions...
            catch (Exception finalEx) {
                opObserver.nextGlobalException(finalEx);
            }
        }
    }

    /**
     * Executes prebuilt SELECT PreparedStatement.
     */
    protected void runSelect(
        OperationObserver observer,
        PreparedStatement prepStmt,
        QueryTranslator transl)
        throws Exception {

        long t1 = System.currentTimeMillis();

        SelectQueryAssembler assembler = (SelectQueryAssembler) transl;
        DefaultResultIterator it =
            (assembler.getFetchLimit() > 0)
                ? new LimitedResultIterator(
                    prepStmt,
                    this.getAdapter(),
                    assembler)
                : new DefaultResultIterator(
                    prepStmt,
                    this.getAdapter(),
                    assembler);

        // note that we don't need to close ResultIterator
        // since "dataRows" will do it internally
        List resultRows = it.dataRows();
        QueryLogger.logSelectCount(
            observer.getLoggingLevel(),
            resultRows.size(),
            System.currentTimeMillis() - t1);
        observer.nextDataRows(transl.getQuery(), resultRows);
    }

    /**
     * Executes prebuilt SELECT PreparedStatement and returns
     * result to an observer as a ResultIterator.
     */
    protected void runIteratedSelect(
        OperationObserver observer,
        PreparedStatement prepStmt,
        QueryTranslator transl)
        throws Exception {

        DefaultResultIterator it = null;

        try {
            SelectQueryAssembler assembler = (SelectQueryAssembler) transl;
            it =
                new DefaultResultIterator(
                    prepStmt,
                    this.getAdapter(),
                    assembler);

            it.setClosingConnection(true);
            observer.nextDataRows(transl.getQuery(), it);
        } catch (Exception ex) {
            if (it != null) {
                it.close();
            }

            throw ex;
        }
    }

    /**
     * Executes prebuilt UPDATE, DELETE or INSERT PreparedStatement.
     */
    protected void runUpdate(
        OperationObserver observer,
        PreparedStatement prepStmt,
        QueryTranslator transl)
        throws Exception {

        try {
            // execute update
            int count = prepStmt.executeUpdate();
            QueryLogger.logUpdateCount(observer.getLoggingLevel(), count);

            // send results back to consumer
            observer.nextCount(transl.getQuery(), count);
        } finally {
            prepStmt.close();
        }
    }

    /**
        * Executes StoredProcedure.
        */
    protected void runProcedure(
        OperationObserver observer,
        PreparedStatement prepStmt,
        QueryTranslator transl)
        throws Exception {

        try {
        	// ProcedureTranslator procTransl = (ProcedureTranslator)transl;
        	// Procedure proc = procTransl.getProcedure();
        	
            // execute procedure
            prepStmt.execute();
            
            // process result
            // List columns = proc.getResultAttributesList();
            
            
            // ignore OUT params for now...
            
        } finally {
            prepStmt.close();
        }
    }

    public void performQuery(Query query, OperationObserver opObserver) {
        List qWrapper = new ArrayList(1);
        qWrapper.add(query);
        this.performQueries(qWrapper, opObserver);
    }

    /**
     * Returns EntityResolver that handles DataMaps of this node.
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public Iterator dataMapIterator() {
        return this.getDataMapsAsList().iterator();
    }

    public void resetReferentialIntegritySupport() throws CayenneException {
        if (refIntegritySupport == null) {
            if (adapter.supportsFkConstraints())
                refIntegritySupport = new RefIntegritySupport(this);
        } else
            refIntegritySupport.reset(this);
    }

    public RefIntegritySupport getReferentialIntegritySupport()
        throws CayenneException {
        if (refIntegritySupport == null)
            resetReferentialIntegritySupport();
        return refIntegritySupport;
    }
}