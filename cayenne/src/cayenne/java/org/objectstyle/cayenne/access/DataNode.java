/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.InsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.ProcedureTranslator;
import org.objectstyle.cayenne.access.trans.SelectQueryTranslator;
import org.objectstyle.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.objectstyle.cayenne.access.util.DefaultSorter;
import org.objectstyle.cayenne.access.util.DependencySorter;
import org.objectstyle.cayenne.access.util.NullSorter;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * Describes a single physical data source. This can be a database server, LDAP server, etc.
 * When the underlying connection layer is based on JDBC, DataNode works as a Cayenne
 * wrapper of javax.sql.DataSource.
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public class DataNode implements QueryEngine {

    public static final Class DEFAULT_ADAPTER_CLASS = JdbcAdapter.class;

    protected String name;
    protected DataSource dataSource;
    protected DbAdapter adapter;
    protected String dataSourceLocation;
    protected String dataSourceFactory;
    protected EntityResolver entityResolver = new EntityResolver();
    protected DependencySorter dependencySorter = NullSorter.NULL_SORTER;

    /** Creates unnamed DataNode. */
    public DataNode() {
        this(null);
    }

    /** Creates DataNode and assigns <code>name</code> to it. */
    public DataNode(String name) {
        this.name = name;
    }

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
     * Returns an unmodifiable collection of DataMaps handled by this DataNode.
     */
    public Collection getDataMaps() {
        return entityResolver.getDataMaps();
    }

    public void setDataMaps(Collection dataMaps) {
        entityResolver.setDataMaps(dataMaps);
        dependencySorter.indexSorter(this);
    }

    /**
     * Adds a DataMap to be handled by this node.
     */
    public void addDataMap(DataMap map) {
        entityResolver.addDataMap(map);
        dependencySorter.indexSorter(this);
    }

    public void removeDataMap(String mapName) {
        DataMap map = entityResolver.getDataMap(mapName);
        if (map != null) {
            entityResolver.removeDataMap(map);
            dependencySorter.indexSorter(this);
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

        // update sorter

        // TODO: since sorting may be disabled even for databases
        // that enforce constraints, in cases when constraints are
        // defined as deferrable, this may need more fine grained
        // control from the user, maybe via ContextCommitObserver?
        if (adapter != null && adapter.supportsFkConstraints()) {
            this.dependencySorter = new DefaultSorter(this);
        }
        else {
            this.dependencySorter = NullSorter.NULL_SORTER;
        }
    }

    /**
     * Returns this object if it can handle queries for <code>objEntity</code>,
     * returns null otherwise.
     * 
     * @deprecated Since 1.1 use {@link #lookupDataNode(DataMap)} since
     * queries are not necessarily based on an ObjEntity.
     */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
        return (this.getEntityResolver().lookupObjEntity(objEntity.getName()) != null)
            ? this
            : null;
    }

    /**
     * Returns a DataNode that should hanlde queries for all
     * DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        return (getEntityResolver().getDataMap(dataMap.getName()) == dataMap)
            ? this
            : null;
    }

    /** 
     * Wraps queries in an internal transaction, and executes them via connection obtained from 
     * internal DataSource.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        Transaction transaction = Transaction.internalTransaction(null);
        transaction.performQueries(this, queries, observer);
    }

    /** 
     * Calls "performQueries()" wrapping a query argument into a list.
     * 
     * @deprecated Since 1.1 use performQueries(List, OperationObserver).
     * This method is redundant and doesn't add value.
     */
    public void performQuery(Query query, OperationObserver operationObserver) {
        this.performQueries(Collections.singletonList(query), operationObserver);
    }

    /** 
     * Runs queries using connection obtained from internal DataSource.
     * 
     * @since 1.1 
     */
    public void performQueries(
        Collection queries,
        OperationObserver resultConsumer,
        Transaction transaction) {

        Level logLevel = resultConsumer.getLoggingLevel();

        int listSize = queries.size();
        if (listSize == 0) {
            return;
        }
        QueryLogger.logQueryStart(logLevel, listSize);

        // since 1.1 Transaction object is required
        if (transaction == null) {
            throw new CayenneRuntimeException("No transaction associated with the queries.");
        }

        Connection connection = null;

        try {
            // check for invalid iterated query
            if (resultConsumer.isIteratedResult() && listSize > 1) {
                throw new CayenneException(
                    "Iterated queries are not allowed in a batch. Batch size: "
                        + listSize);
            }

            // check out connection, create statement
            connection = this.getDataSource().getConnection();
            transaction.addConnection(connection);
        }
        // catch stuff like connection allocation errors, etc...
        catch (Exception globalEx) {
            QueryLogger.logQueryError(logLevel, globalEx);

            if (connection != null) {
                // rollback failed transaction
                transaction.setRollbackOnly();
            }

            resultConsumer.nextGlobalException(globalEx);
            return;
        }

        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query nextQuery = (Query) it.next();

            // catch exceptions for each individual query
            try {

                // figure out query type and call appropriate worker method

                // 1. All kinds of SELECT
                if (nextQuery.getQueryType() == Query.SELECT_QUERY) {
                    runSelect(connection, nextQuery, resultConsumer);
                }
                // 2. All kinds of MODIFY - INSERT, DELETE, UPDATE, UNKNOWN
                else if (nextQuery instanceof BatchQuery) {
                    runBatchUpdate(connection, (BatchQuery) nextQuery, resultConsumer);
                }
                else if (nextQuery instanceof ProcedureQuery) {
                    runStoredProcedure(connection, nextQuery, resultConsumer);
                }
                else {
                    runUpdate(connection, nextQuery, resultConsumer);
                }
            }
            catch (Exception queryEx) {
                QueryLogger.logQueryError(logLevel, queryEx);

                // notify consumer of the exception,
                // stop running further queries
                resultConsumer.nextQueryException(nextQuery, queryEx);

                // rollback transaction
                transaction.setRollbackOnly();
                break;
            }
        }
    }

    /**
     * Executes select query.
     */
    protected void runSelect(Connection con, Query query, OperationObserver delegate)
        throws SQLException, Exception {

        long t1 = System.currentTimeMillis();

        QueryTranslator transl = getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(con);

        PreparedStatement prepStmt = transl.createStatement(query.getLoggingLevel());
        ResultSet rs = prepStmt.executeQuery();

        SelectQueryTranslator assembler = (SelectQueryTranslator) transl;
        DefaultResultIterator it =
            new DefaultResultIterator(
                con,
                prepStmt,
                rs,
                assembler.getResultDescriptor(rs),
                ((GenericSelectQuery) query).getFetchLimit());

        // TODO: Should do something about closing ResultSet and PreparedStatement in this method,
        // instead of relying on DefaultResultIterator to do that later

        if (!delegate.isIteratedResult()) {
            // note that we don't need to close ResultIterator
            // since "dataRows" will do it internally
            List resultRows = it.dataRows(true);
            QueryLogger.logSelectCount(
                query.getLoggingLevel(),
                resultRows.size(),
                System.currentTimeMillis() - t1);

            delegate.nextDataRows(query, resultRows);
        }
        else {
            try {
                it.setClosingConnection(true);
                delegate.nextDataRows(transl.getQuery(), it);
            }
            catch (Exception ex) {
                it.close();
                throw ex;
            }
        }
    }

    /**
     * Executes a non-batched update query (including UPDATE, DELETE, INSERT, etc.).
     */
    protected void runUpdate(Connection con, Query query, OperationObserver delegate)
        throws SQLException, Exception {

        QueryTranslator transl = getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(con);

        PreparedStatement prepStmt = transl.createStatement(query.getLoggingLevel());

        try {
            // execute update
            int count = prepStmt.executeUpdate();
            QueryLogger.logUpdateCount(query.getLoggingLevel(), count);

            // send results back to consumer
            delegate.nextCount(transl.getQuery(), count);
        }
        finally {
            prepStmt.close();
        }
    }

    /**
     * Executes a BatchQuery (including UPDATE, DELETE, INSERT, etc.).
     */
    protected void runBatchUpdate(
        Connection con,
        BatchQuery query,
        OperationObserver delegate)
        throws SQLException, Exception {

        // check if adapter wants to run the query itself
        if (!adapter.shouldRunBatchQuery(this, con, query, delegate)) {
            return;
        }

        // create BatchInterpreter
        // TODO: move all query translation logic to adapter.getQueryTranslator()
        BatchQueryBuilder queryBuilder;
        switch (query.getQueryType()) {
            case Query.INSERT_QUERY :
                queryBuilder = new InsertBatchQueryBuilder(getAdapter());
                break;
            case Query.UPDATE_QUERY :
                queryBuilder = new UpdateBatchQueryBuilder(getAdapter());
                break;
            case Query.DELETE_QUERY :
                queryBuilder = new DeleteBatchQueryBuilder(getAdapter());
                break;
            default :
                throw new CayenneException(
                    "Unsupported batch type: " + query.getQueryType());
        }

        // run batch

        // optimistic locking is not supported in batches due to JDBC driver limitations
        boolean useOptimisticLock =
            (query instanceof UpdateBatchQuery)
                && ((UpdateBatchQuery) query).isUsingOptimisticLocking();

        if (useOptimisticLock || !adapter.supportsBatchUpdates()) {
            runBatchUpdateAsIndividualQueries(con, query, queryBuilder, delegate);
        }
        else {
            runBatchUpdateAsBatch(con, query, queryBuilder, delegate);
        }
    }

    /**
     * Executes batch query using JDBC Statement batching features.
     */
    protected void runBatchUpdateAsBatch(
        Connection con,
        BatchQuery query,
        BatchQueryBuilder queryBuilder,
        OperationObserver delegate)
        throws SQLException, Exception {

        String queryStr = queryBuilder.createSqlString(query);
        Level logLevel = query.getLoggingLevel();
        boolean isLoggable = QueryLogger.isLoggable(logLevel);

        // log batch SQL execution
        QueryLogger.logQuery(logLevel, queryStr, Collections.EMPTY_LIST);
        List dbAttributes = query.getDbAttributes();

        // run batch
        query.reset();

        PreparedStatement statement = con.prepareStatement(queryStr);
        try {
            while (query.next()) {

                if (isLoggable) {
                    QueryLogger.logQueryParameters(
                        logLevel,
                        "batch bind",
                        query.getValuesForUpdateParameters());
                }

                queryBuilder.bindParameters(statement, query, dbAttributes);
                statement.addBatch();
            }

            // execute the whole batch
            int[] results = statement.executeBatch();
            delegate.nextBatchCount(query, results);

            if (isLoggable) {
                QueryLogger.logUpdateCount(logLevel, statement.getUpdateCount());
            }
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Executes batch query without using JDBC Statement batching features,
     * running individual statements in the batch one by one.
     */
    protected void runBatchUpdateAsIndividualQueries(
        Connection con,
        BatchQuery query,
        BatchQueryBuilder queryBuilder,
        OperationObserver delegate)
        throws SQLException, Exception {

        Level logLevel = query.getLoggingLevel();
        boolean isLoggable = QueryLogger.isLoggable(logLevel);
        boolean useOptimisticLock =
            (query instanceof UpdateBatchQuery)
                && ((UpdateBatchQuery) query).isUsingOptimisticLocking();

        String queryStr = queryBuilder.createSqlString(query);

        // log batch SQL execution
        QueryLogger.logQuery(logLevel, queryStr, Collections.EMPTY_LIST);
        List dbAttributes = query.getDbAttributes();

        // run batch queries one by one
        query.reset();

        PreparedStatement statement = con.prepareStatement(queryStr);
        try {
            while (query.next()) {
                if (isLoggable) {
                    QueryLogger.logQueryParameters(
                        logLevel,
                        "bind",
                        query.getValuesForUpdateParameters());
                }

                queryBuilder.bindParameters(statement, query, dbAttributes);

                int updated = statement.executeUpdate();
                if (useOptimisticLock && updated != 1) {

                    Map snapshot =
                        (query instanceof UpdateBatchQuery)
                            ? ((UpdateBatchQuery) query).getCurrentQualifier()
                            : Collections.EMPTY_MAP;

                    throw new OptimisticLockException(
                        query.getDbEntity(),
                        queryStr,
                        snapshot);
                }

                delegate.nextCount(query, updated);

                if (isLoggable) {
                    QueryLogger.logUpdateCount(logLevel, updated);
                }
            }
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception e) {
            }
        }
    }

    protected void runStoredProcedure(
        Connection con,
        Query query,
        OperationObserver delegate)
        throws SQLException, Exception {

        ProcedureTranslator transl =
            (ProcedureTranslator) getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(con);

        CallableStatement statement =
            (CallableStatement) transl.createStatement(query.getLoggingLevel());

        // stored procedure may contain a mixture of update counts and result sets,
        // and out parameters. Read out parameters first, then
        // iterate until we exhaust all results
        boolean hasResultSet = statement.execute();

        // read out parameters
        readStoredProcedureOutParameters(
            statement,
            transl.getProcedureResultDescriptor(),
            query,
            delegate);

        // read the rest of the query
        while (true) {
            if (hasResultSet) {
                ResultSet rs = statement.getResultSet();
                SelectQueryTranslator assembler = (SelectQueryTranslator) transl;

                readResultSet(
                    rs,
                    assembler.getResultDescriptor(rs),
                    (GenericSelectQuery) query,
                    delegate);
            }
            else {
                int updateCount = statement.getUpdateCount();
                if (updateCount == -1) {
                    break;
                }
                QueryLogger.logUpdateCount(query.getLoggingLevel(), updateCount);
                delegate.nextCount(query, updateCount);
            }

            hasResultSet = statement.getMoreResults();
        }
    }

    /**
     * Helper method that reads OUT parameters of a CallableStatement.
     */
    protected void readStoredProcedureOutParameters(
        CallableStatement statement,
        ResultDescriptor descriptor,
        Query query,
        OperationObserver delegate)
        throws SQLException, Exception {

        long t1 = System.currentTimeMillis();
        Map row = DefaultResultIterator.readProcedureOutParameters(statement, descriptor);

        if (!row.isEmpty()) {
            // treat out parameters as a separate data row set
            QueryLogger.logSelectCount(
                query.getLoggingLevel(),
                1,
                System.currentTimeMillis() - t1);
            delegate.nextDataRows(query, Collections.singletonList(row));
        }
    }

    /**
     * Helper method that reads a ResultSet.
     */
    protected void readResultSet(
        ResultSet resultSet,
        ResultDescriptor descriptor,
        GenericSelectQuery query,
        OperationObserver delegate)
        throws SQLException, Exception {

        long t1 = System.currentTimeMillis();
        DefaultResultIterator resultReader =
            new DefaultResultIterator(
                null,
                null,
                resultSet,
                descriptor,
                query.getFetchLimit());

        if (!delegate.isIteratedResult()) {
            List resultRows = resultReader.dataRows(false);
            QueryLogger.logSelectCount(
                query.getLoggingLevel(),
                resultRows.size(),
                System.currentTimeMillis() - t1);

            delegate.nextDataRows(query, resultRows);
        }
        else {
            try {
                resultReader.setClosingConnection(true);
                delegate.nextDataRows(query, resultReader);
            }
            catch (Exception ex) {
                resultReader.close();
                throw ex;
            }
        }
    }

    /**
     * Returns EntityResolver that handles DataMaps of this node.
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public DependencySorter getDependencySorter() {
        return dependencySorter;
    }

    /**
     * Tries to close JDBC connections opened by this node's data source.
     */
    public synchronized void shutdown() {
        DataSource ds = getDataSource();
        try {
            if (ds instanceof PoolManager) {
                ((PoolManager) ds).dispose();
            }
        }
        catch (SQLException ex) {
        }
    }
}