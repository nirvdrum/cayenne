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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.InsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.SelectQueryAssembler;
import org.objectstyle.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.access.util.DefaultSorter;
import org.objectstyle.cayenne.access.util.DependencySorter;
import org.objectstyle.cayenne.access.util.NullSorter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.ProcedureSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * Describes a single physical data source, e.g. a database server.
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
     * @deprecated Since 1.0 Beta1; use #getDataMaps() instead.
     */
    public List getDataMapsAsList() {
        return entityResolver.getDataMapsList();
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
        } else {
            this.dependencySorter = NullSorter.NULL_SORTER;
        }
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

    public void performQuery(Query query, OperationObserver opObserver) {
        this.performQueries(Collections.singletonList(query), opObserver);
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

            for (int i = 0; i < listSize; i++) {
                Query nextQuery = (Query) queries.get(i);

                // catch exceptions for each individual query
                try {

                    // figure out query type and call appropriate worker method

                    // 1. All kinds of SELECT
                    if (nextQuery.getQueryType() == Query.SELECT_QUERY) {
                        boolean isIterated = opObserver.isIteratedResult();
                        Connection localCon = con;

                        if (isIterated) {
                            // if ResultIterator is returned to the user,
                            // DataNode is not responsible for closing the connections
                            // exception handling, and other housekeeping.
                            // trick "finally" to avoid closing connection here
                            // it will be closed by the ResultIterator
                            con = null;
                        }

                        if (nextQuery instanceof ProcedureSelectQuery) {
                            runStoredProcedureSelect(
                                localCon,
                                nextQuery,
                                opObserver,
                                !isIterated);
                        } else {
                            runSelect(
                                localCon,
                                nextQuery,
                                opObserver,
                                !isIterated);
                        }
                    }
                    // 2. All kinds of MODIFY - INSERT, DELETE, UPDATE, UNKNOWN
                    else {

                        if (nextQuery instanceof BatchQuery) {
                            runBatchUpdate(
                                con,
                                (BatchQuery) nextQuery,
                                opObserver);
                        } else if (nextQuery instanceof ProcedureQuery) {
                            runStoredProcedureUpdate(
                                con,
                                nextQuery,
                                opObserver);
                        } else {
                            runUpdate(con, nextQuery, opObserver);
                        }
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
     * Executes select query.
     */
    protected void runSelect(
        Connection con,
        Query query,
        OperationObserver delegate,
        boolean readAll)
        throws SQLException, Exception {

        long t1 = System.currentTimeMillis();

        QueryTranslator transl = getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(con);

        PreparedStatement prepStmt =
            transl.createStatement(query.getLoggingLevel());
        ResultSet rs = prepStmt.executeQuery();

        SelectQueryAssembler assembler = (SelectQueryAssembler) transl;
        DefaultResultIterator it =
            new DefaultResultIterator(
                con,
                prepStmt,
                rs,
                assembler.getResultDescriptor(rs),
                assembler.getFetchLimit());

        // TODO: Should do something about closing ResultSet and PreparedStatement in this method, 
        // instead of replying on DefaultResultIterator to do that later

        if (readAll) {
            // note that we don't need to close ResultIterator
            // since "dataRows" will do it internally
            List resultRows = it.dataRows();
            QueryLogger.logSelectCount(
                query.getLoggingLevel(),
                resultRows.size(),
                System.currentTimeMillis() - t1);

            delegate.nextDataRows(query, resultRows);
        } else {
            try {
                it.setClosingConnection(true);
                delegate.nextDataRows(transl.getQuery(), it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        }
    }

    /**
     * Executes a non-batched update query (including UPDATE, DELETE, INSERT, etc.).
     */
    protected void runUpdate(
        Connection con,
        Query query,
        OperationObserver delegate)
        throws SQLException, Exception {

        QueryTranslator transl = getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(con);

        PreparedStatement prepStmt =
            transl.createStatement(query.getLoggingLevel());

        try {
            // execute update
            int count = prepStmt.executeUpdate();
            QueryLogger.logUpdateCount(query.getLoggingLevel(), count);

            // send results back to consumer
            delegate.nextCount(transl.getQuery(), count);
        } finally {
            prepStmt.close();
        }
    }

    /**
     * Executes a batched update query (including UPDATE, DELETE, INSERT, etc.).
     */
    protected void runBatchUpdate(
        Connection con,
        BatchQuery query,
        OperationObserver delegate)
        throws SQLException, Exception {

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
                ;
                break;
            default :
                throw new CayenneException(
                    "Unsupported batch type: " + query.getQueryType());
        }

        // translate batch
        List dbAttributes = query.getDbAttributes();
        int attributeCount = dbAttributes.size();
        int[] attributeTypes = new int[attributeCount];
        int[] attributeScales = new int[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            DbAttribute attribute = (DbAttribute) dbAttributes.get(i);
            attributeTypes[i] = attribute.getType();
            attributeScales[i] = attribute.getPrecision();
        }
        String queryStr = queryBuilder.query(query);
        ExtendedTypeMap typeConverter = adapter.getExtendedTypes();

        // log batch execution
        QueryLogger.logQuery(
            query.getLoggingLevel(),
            queryStr,
            Collections.EMPTY_LIST);

        PreparedStatement st = con.prepareStatement(queryStr);
        try {

            query.reset();
            while (query.next()) {
                // log next batch parameters
                QueryLogger.logBatchQueryParameters(
                    query.getLoggingLevel(),
                    query);

                for (int i = 0; i < attributeCount; i++) {
                    Object value = query.getObject(i);
                    int type = attributeTypes[i];
                    if (value == null)
                        st.setNull(i + 1, type);
                    else {
                        ExtendedType typeProcessor =
                            typeConverter.getRegisteredType(value.getClass());
                        typeProcessor.setJdbcObject(
                            st,
                            value,
                            i + 1,
                            type,
                            attributeScales[i]);
                    }
                }
                int updated = st.executeUpdate();
                delegate.nextCount(query, updated);
                QueryLogger.logUpdateCount(query.getLoggingLevel(), updated);
            }
        } finally {
            try {
                st.close();
            } catch (Exception e) {
            }
        }
    }

    protected void runStoredProcedureUpdate(
        Connection con,
        Query query,
        OperationObserver delegate)
        throws SQLException, Exception {

        QueryTranslator transl = getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(con);

        PreparedStatement prepStmt =
            transl.createStatement(query.getLoggingLevel());

        try {

            // execute procedure
            prepStmt.execute();

        } finally {
            prepStmt.close();
        }
    }

    protected void runStoredProcedureSelect(
        Connection con,
        Query query,
        OperationObserver delegate,
        boolean readAll)
        throws SQLException, Exception {

        /*    QueryTranslator transl = getAdapter().getQueryTranslator(query);
            transl.setEngine(this);
            transl.setCon(con);
        
            PreparedStatement prepStmt =
                transl.createStatement(query.getLoggingLevel());
        
            DefaultResultIterator it =
                new DefaultResultIterator(prepStmt, this.getAdapter(), assembler);
        
            // note that we don't need to close ResultIterator
            // since "dataRows" will do it internally
            List resultRows = it.dataRows();
            QueryLogger.logSelectCount(
                query.getLoggingLevel(),
                resultRows.size(),
                System.currentTimeMillis() - t1);
        
            delegate.nextDataRows(query, resultRows); */
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
}