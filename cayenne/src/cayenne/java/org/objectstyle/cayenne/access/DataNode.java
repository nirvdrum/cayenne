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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.jdbc.BatchAction;
import org.objectstyle.cayenne.access.jdbc.ProcedureAction;
import org.objectstyle.cayenne.access.jdbc.RowDescriptor;
import org.objectstyle.cayenne.access.jdbc.SQLAction;
import org.objectstyle.cayenne.access.jdbc.SQLTemplateAction;
import org.objectstyle.cayenne.access.jdbc.SQLTemplateSelectAction;
import org.objectstyle.cayenne.access.jdbc.SelectAction;
import org.objectstyle.cayenne.access.jdbc.UpdateAction;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.AshwoodEntitySorter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * Describes a single physical data source. This can be a database server, LDAP server,
 * etc. When the underlying connection layer is based on JDBC, DataNode works as a Cayenne
 * wrapper of javax.sql.DataSource.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
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
    protected EntityResolver entityResolver;
    protected EntitySorter entitySorter;

    // ====================================================
    // DataMaps
    // ====================================================
    protected Map dataMaps = new HashMap();
    private Collection dataMapsValuesRef = Collections.unmodifiableCollection(dataMaps
            .values());

    /** Creates unnamed DataNode. */
    public DataNode() {
        this(null);
    }

    /** Creates DataNode and assigns <code>name</code> to it. */
    public DataNode(String name) {
        this.name = name;

        // since 1.2 we always implement entity sorting, regardless of the underlying DB
        // as the right order is needed for deferred PK propagation (and maybe other
        // things too?)
        this.entitySorter = new AshwoodEntitySorter(Collections.EMPTY_LIST);
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
        return dataMapsValuesRef;
    }

    public void setDataMaps(Collection dataMaps) {
        Iterator it = dataMaps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            this.dataMaps.put(map.getName(), map);
        }

        entitySorter.setDataMaps(dataMaps);
    }

    /**
     * Adds a DataMap to be handled by this node.
     */
    public void addDataMap(DataMap map) {
        this.dataMaps.put(map.getName(), map);

        entitySorter.setDataMaps(getDataMaps());
    }

    public void removeDataMap(String mapName) {
        DataMap map = (DataMap) dataMaps.remove(mapName);
        if (map != null) {
            entitySorter.setDataMaps(getDataMaps());
        }
    }

    /**
     * Returns DataSource used by this DataNode to obtain connections.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns DbAdapter object. This is a plugin that handles RDBMS vendor-specific
     * features.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns a DataNode that should hanlde queries for all DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        // we don't know any better than to return ourselves...
        return this;
    }

    /**
     * Wraps queries in an internal transaction, and executes them via connection obtained
     * from internal DataSource.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        Transaction transaction = Transaction.internalTransaction(null);
        transaction.performQueries(this, queries, observer);
    }

    /**
     * Runs queries using Connection obtained from internal DataSource. Once Connection is
     * obtained internally, it is added to the Transaction that will handle its closing.
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
            throw new CayenneRuntimeException(
                    "No transaction associated with the queries.");
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

                // TODO: need to externalize this big switch into maybe a config file
                // to allow easy plugging of alt. actions for nodes

                // figure out query type and call appropriate worker method
                if (nextQuery instanceof SQLTemplate) {
                    runSQLTemplate(connection, (SQLTemplate) nextQuery, resultConsumer);
                }
                else if (nextQuery instanceof ProcedureQuery) {
                    runStoredProcedure(connection, nextQuery, resultConsumer);
                }
                // Important: check for GenericSelectQuery AFTER all specific
                // implementations are checked...
                else if (nextQuery instanceof GenericSelectQuery) {
                    runSelect(connection, nextQuery, resultConsumer);
                }
                else if (nextQuery instanceof BatchQuery) {
                    runBatchUpdate(connection, (BatchQuery) nextQuery, resultConsumer);
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
     * Executes a SQLTemplate query.
     * 
     * @since 1.2
     */
    protected void runSQLTemplate(
            Connection connection,
            SQLTemplate sqlTemplate,
            OperationObserver resultConsumer) throws SQLException, Exception {

        SQLAction executionPlan = (sqlTemplate.isSelecting())
                ? new SQLTemplateSelectAction(getAdapter())
                : new SQLTemplateAction(getAdapter());
        executionPlan.performAction(connection, sqlTemplate, resultConsumer);
    }
    

    /**
     * Executes a generic select query.
     */
    protected void runSelect(
            Connection connection,
            Query query,
            OperationObserver observer) throws SQLException, Exception {

        new SelectAction(getAdapter(), getEntityResolver()).performAction(
                connection,
                query,
                observer);
    }

    /**
     * Executes a non-batched updating query.
     */
    protected void runUpdate(Connection con, Query query, OperationObserver delegate)
            throws SQLException, Exception {

        new UpdateAction(getAdapter(), getEntityResolver()).performAction(
                con,
                query,
                delegate);
    }

    /**
     * Executes a batch updating query.
     */
    protected void runBatchUpdate(
            Connection connection,
            BatchQuery query,
            OperationObserver observer) throws SQLException, Exception {

        // check run strategy...

        // optimistic locking is not supported in batches due to JDBC driver limitations
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        boolean runningAsBatch = !useOptimisticLock && adapter.supportsBatchUpdates();
        BatchAction action = new BatchAction(getAdapter(), getEntityResolver());
        action.setBatch(runningAsBatch);
        action.performAction(connection, query, observer);
    }

    /**
     * Executes batch query using JDBC Statement batching features.
     * 
     * @deprecated since 1.2 SQLActions are used.
     */
    protected void runBatchUpdateAsBatch(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {
        new TempBatchAction(true).runAsBatch(con, query, queryBuilder, delegate);
    }

    /**
     * Executes batch query without using JDBC Statement batching features, running
     * individual statements in the batch one by one.
     * 
     * @deprecated since 1.2 SQLActions are used.
     */
    protected void runBatchUpdateAsIndividualQueries(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        new TempBatchAction(false).runAsBatch(con, query, queryBuilder, delegate);
    }

    protected void runStoredProcedure(
            Connection con,
            Query query,
            OperationObserver delegate) throws SQLException, Exception {

        new ProcedureAction(getAdapter(), getEntityResolver()).performAction(
                con,
                query,
                delegate);
    }

    /**
     * Helper method that reads OUT parameters of a CallableStatement.
     * 
     * @deprecated Since 1.2 this logic is moved to SQLAction.
     */
    protected void readStoredProcedureOutParameters(
            CallableStatement statement,
            org.objectstyle.cayenne.access.util.ResultDescriptor descriptor,
            Query query,
            OperationObserver delegate) throws SQLException, Exception {

        // method is deprecated, so keep this ugly piece here as a placeholder
        Procedure procedure = (Procedure) query.getRoot();
        new TempProcedureAction().readProcedureOutParameters(
                statement,
                procedure,
                query,
                delegate);
    }

    /**
     * Helper method that reads a ResultSet.
     * 
     * @deprecated Since 1.2 this logic is moved to SQLAction.
     */
    protected void readResultSet(
            ResultSet resultSet,
            org.objectstyle.cayenne.access.util.ResultDescriptor descriptor,
            GenericSelectQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        // method is deprecated, so keep this ugly piece here as a placeholder
        RowDescriptor rowDescriptor = new RowDescriptor(resultSet, getAdapter()
                .getExtendedTypes());
        new TempProcedureAction()
                .readResultSet(resultSet, rowDescriptor, query, delegate);
    }

    /**
     * Returns EntityResolver that handles DataMaps of this node.
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * Sets EntityResolver. DataNode relies on externally set EntityResolver, so if the
     * node is created outside of DataDomain stack, a valid EntityResolver must be
     * provided explicitly.
     * 
     * @since 1.1
     */
    public void setEntityResolver(
            org.objectstyle.cayenne.map.EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
     * Returns EntitySorter used by the DataNode.
     */
    public EntitySorter getEntitySorter() {
        return entitySorter;
    }

    /**
     * Tries to close JDBC connections opened by this node's data source.
     */
    public synchronized void shutdown() {
        DataSource ds = getDataSource();
        try {
            // TODO: theoretically someone maybe using our PoolManager as a container
            // mapped DataSource, so we should use some other logic to determine whether
            // this is a DataNode-managed DS.
            if (ds instanceof PoolManager) {
                ((PoolManager) ds).dispose();
            }
        }
        catch (SQLException ex) {
        }
    }

    // this class exists to provide deprecated DataNode methods with access to
    // various SQLAction implementations. It will be removed once corresponding
    // DataNode methods are removed
    final class TempProcedureAction extends ProcedureAction {

        public TempProcedureAction() {
            super(DataNode.this.adapter, DataNode.this.entityResolver);
        }

        // changing access to public
        public void readProcedureOutParameters(
                CallableStatement statement,
                Procedure procedure,
                Query query,
                OperationObserver delegate) throws SQLException, Exception {
            super.readProcedureOutParameters(statement, procedure, query, delegate);
        }

        // changing access to public
        public void readResultSet(
                ResultSet resultSet,
                RowDescriptor descriptor,
                GenericSelectQuery query,
                OperationObserver delegate) throws SQLException, Exception {
            super.readResultSet(resultSet, descriptor, query, delegate);
        }
    }

    // this class exists to provide deprecated DataNode methods with access to
    // various SQLAction implementations. It will be removed once corresponding
    // DataNode methods are removed
    final class TempBatchAction extends BatchAction {

        public TempBatchAction(boolean runningAsBatch) {
            super(DataNode.this.adapter, DataNode.this.entityResolver);
            setBatch(runningAsBatch);
        }

        // making public to access from DataNode
        protected void runAsBatch(
                Connection con,
                BatchQuery query,
                BatchQueryBuilder queryBuilder,
                OperationObserver delegate) throws SQLException, Exception {
            super.runAsBatch(con, query, queryBuilder, delegate);
        }

        // making public to access from DataNode
        public void runAsIndividualQueries(
                Connection con,
                BatchQuery query,
                BatchQueryBuilder queryBuilder,
                OperationObserver delegate) throws SQLException, Exception {
            super.runAsIndividualQueries(con, query, queryBuilder, delegate, false);
        }
    }

}