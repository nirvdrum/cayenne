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
package org.objectstyle.cayenne.dba.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.jdbc.SQLAction;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryWrapper;
import org.objectstyle.cayenne.access.util.BatchQueryUtils;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * DataNode subclass customized for Oracle database engine.
 * 
 * @author Andrei Adamchik
 */
public class OracleDataNode extends DataNode {

    public OracleDataNode() {
        super();
    }

    public OracleDataNode(String name) {
        super(name);
    }

    /**
     * @since 1.2
     */
    protected void runBatchUpdate(
            Connection connection,
            BatchQuery query,
            OperationObserver observer) throws SQLException, Exception {

        SQLAction action;

        // special handling for LOB updates
        if (OracleAdapter.isSupportsOracleLOB()
                && BatchQueryUtils.updatesLOBColumns(query)) {

            action = new OracleLOBBatchAction(getAdapter());
        }
        else {

            // optimistic locking is not supported in batches due to JDBC driver
            // limitations
            boolean useOptimisticLock = (query instanceof UpdateBatchQuery)
                    && ((UpdateBatchQuery) query).isUsingOptimisticLocking();

            boolean runningAsBatch = !useOptimisticLock && adapter.supportsBatchUpdates();
            action = new OracleBatchAction(
                    getAdapter(),
                    getEntityResolver(),
                    runningAsBatch);
        }

        action.performAction(connection, query, observer);
    }

    /**
     * @since 1.2
     */
    protected void runStoredProcedure(
            Connection con,
            Query query,
            OperationObserver observer) throws SQLException, Exception {

        new OracleProcedureAction(getAdapter(), getEntityResolver()).performAction(
                con,
                query,
                observer);
    }

    /**
     * Implements Oracle-specific handling of StoredProcedure OUT parameters reading.
     * 
     * @deprecated Since 1.2 this logic is moved to SQLExecutionPlans.
     */
    protected void readStoredProcedureOutParameters(
            CallableStatement statement,
            ResultDescriptor descriptor,
            Query query,
            OperationObserver delegate) throws SQLException, Exception {
        new OracleProcedureAction(getAdapter(), getEntityResolver())
                .readStoredProcedureOutParameters(statement, descriptor, query, delegate);
    }

    /**
     * Special update method that is called from OracleAdapter if LOB columns are to be
     * updated.
     * 
     * @deprecated Since 1.2 replaced with SQLAction.
     */
    public void runBatchUpdateWithLOBColumns(
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        new OracleLOBBatchAction(getAdapter()).performAction(con, query, delegate);
    }

    /**
     * Selects a LOB row and writes LOB values.
     * 
     * @deprecated since 1.2 OracleLOBBatchAction is used.
     */
    protected void processLOBRow(
            Connection con,
            LOBBatchQueryBuilder queryBuilder,
            LOBBatchQueryWrapper selectQuery,
            List qualifierAttributes) throws SQLException, Exception {

        new OracleLOBBatchAction(getAdapter()).processLOBRow(
                con,
                queryBuilder,
                selectQuery,
                qualifierAttributes);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes super
     * implementation.
     * 
     * @deprecated Since 1.2 super implementation is deprecated.
     */
    protected void runBatchUpdateAsBatch(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsBatch(con, query, queryBuilder, delegate);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes super
     * implementation.
     * 
     * @deprecated Since 1.2 super implementation is deprecated.
     */
    protected void runBatchUpdateAsIndividualQueries(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsIndividualQueries(con, query, queryBuilder, delegate);
    }
}