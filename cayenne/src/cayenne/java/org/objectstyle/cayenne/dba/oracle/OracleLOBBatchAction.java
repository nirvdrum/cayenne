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

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.jdbc.SQLAction;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryWrapper;
import org.objectstyle.cayenne.access.trans.LOBInsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBUpdateBatchQueryBuilder;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateBatchQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * @since 1.2
 * @author Andrei Adamchik
 */
class OracleLOBBatchAction implements SQLAction {

    DbAdapter adapter;

    OracleLOBBatchAction(DbAdapter adapter) {
        this.adapter = adapter;
    }

    DbAdapter getAdapter() {
        return adapter;
    }

    public void performAction(
            Connection connection,
            Query query,
            OperationObserver observer) throws SQLException, Exception {

        // sanity check
        if (!(query instanceof BatchQuery)) {
            throw new CayenneException("Query unsupported by the execution plan: "
                    + query);
        }

        BatchQuery batch = (BatchQuery) query;

        LOBBatchQueryBuilder queryBuilder;
        if (batch instanceof InsertBatchQuery) {
            queryBuilder = new LOBInsertBatchQueryBuilder(getAdapter());
        }
        else if (batch instanceof UpdateBatchQuery) {
            queryBuilder = new LOBUpdateBatchQueryBuilder(getAdapter());
        }
        else {
            throw new CayenneException(
                    "Unsupported batch type for special LOB processing: " + batch);
        }

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        queryBuilder.setNewBlobFunction(OracleAdapter.NEW_BLOB_FUNCTION);
        queryBuilder.setNewClobFunction(OracleAdapter.NEW_CLOB_FUNCTION);

        // no batching is done, queries are translated
        // for each batch set, since prepared statements
        // may be different depending on whether LOBs are NULL or not..

        LOBBatchQueryWrapper selectQuery = new LOBBatchQueryWrapper(batch);
        List qualifierAttributes = selectQuery.getDbAttributesForLOBSelectQualifier();

        Level logLevel = query.getLoggingLevel();
        boolean isLoggable = QueryLogger.isLoggable(logLevel);

        batch.reset();
        while (selectQuery.next()) {
            int updated = 0;
            String updateStr = queryBuilder.createSqlString(batch);

            // 1. run row update
            QueryLogger.logQuery(logLevel, updateStr, Collections.EMPTY_LIST);
            PreparedStatement statement = connection.prepareStatement(updateStr);
            try {

                if (isLoggable) {
                    List bindings = queryBuilder.getValuesForLOBUpdateParameters(batch);
                    QueryLogger.logQueryParameters(logLevel, "bind", bindings);
                }

                queryBuilder.bindParameters(statement, batch);
                updated = statement.executeUpdate();
                QueryLogger.logUpdateCount(logLevel, updated);
            }
            finally {
                try {
                    statement.close();
                }
                catch (Exception e) {
                }
            }

            // 2. run row LOB update (SELECT...FOR UPDATE and writing out LOBs)
            processLOBRow(connection, queryBuilder, selectQuery, qualifierAttributes);

            // finally, notify delegate that the row was updated
            observer.nextCount(query, updated);
        }
    }

    void processLOBRow(
            Connection con,
            LOBBatchQueryBuilder queryBuilder,
            LOBBatchQueryWrapper selectQuery,
            List qualifierAttributes) throws SQLException, Exception {

        List lobAttributes = selectQuery.getDbAttributesForUpdatedLOBColumns();
        if (lobAttributes.size() == 0) {
            return;
        }

        Level logLevel = selectQuery.getLoggingLevel();
        boolean isLoggable = QueryLogger.isLoggable(logLevel);

        List qualifierValues = selectQuery.getValuesForLOBSelectQualifier();
        List lobValues = selectQuery.getValuesForUpdatedLOBColumns();
        int parametersSize = qualifierValues.size();
        int lobSize = lobAttributes.size();

        String selectStr = queryBuilder.createLOBSelectString(
                selectQuery.getQuery(),
                lobAttributes,
                qualifierAttributes);

        if (isLoggable) {
            QueryLogger.logQuery(logLevel, selectStr, qualifierValues);
            QueryLogger.logQueryParameters(logLevel, "write LOB", lobValues);
        }

        PreparedStatement selectStatement = con.prepareStatement(selectStr);
        try {
            for (int i = 0; i < parametersSize; i++) {
                Object value = qualifierValues.get(i);
                DbAttribute attribute = (DbAttribute) qualifierAttributes.get(i);

                adapter.bindParameter(
                        selectStatement,
                        value,
                        i + 1,
                        attribute.getType(),
                        attribute.getPrecision());
            }

            ResultSet result = selectStatement.executeQuery();

            try {
                if (!result.next()) {
                    throw new CayenneRuntimeException("Missing LOB row.");
                }

                // read the only expected row

                for (int i = 0; i < lobSize; i++) {
                    DbAttribute attribute = (DbAttribute) lobAttributes.get(i);
                    int type = attribute.getType();

                    if (type == Types.CLOB) {
                        Clob clob = result.getClob(i + 1);
                        Object clobVal = lobValues.get(i);

                        if (clobVal instanceof char[]) {
                            writeClob(clob, (char[]) clobVal);
                        }
                        else {
                            writeClob(clob, clobVal.toString());
                        }
                    }
                    else if (type == Types.BLOB) {
                        Blob blob = result.getBlob(i + 1);

                        Object blobVal = lobValues.get(i);
                        if (blobVal instanceof byte[]) {
                            writeBlob(blob, (byte[]) blobVal);
                        }
                        else {
                            String className = (blobVal != null) ? blobVal
                                    .getClass()
                                    .getName() : null;
                            throw new CayenneRuntimeException(
                                    "Unsupported class of BLOB value: " + className);
                        }
                    }
                    else {
                        throw new CayenneRuntimeException(
                                "Only BLOB or CLOB is expected here, got: " + type);
                    }
                }

                if (result.next()) {
                    throw new CayenneRuntimeException("More than one LOB row found.");
                }
            }
            finally {
                try {
                    result.close();
                }
                catch (Exception e) {
                }
            }
        }
        finally {
            try {
                selectStatement.close();
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Writing of LOBs is not supported prior to JDBC 3.0 and has to be done using Oracle
     * driver utilities, using reflection.
     */
    private void writeBlob(Blob blob, byte[] value) {

        Method getBinaryStreamMethod = OracleAdapter.getOutputStreamFromBlobMethod();
        try {
            OutputStream out = (OutputStream) getBinaryStreamMethod.invoke(blob, null);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }
        }
        catch (InvocationTargetException e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
    }

    /**
     * Writing of LOBs is not supported prior to JDBC 3.0 and has to be done using Oracle
     * driver utilities.
     */
    private void writeClob(Clob clob, char[] value) {
        // obtain Writer and write CLOB
        Method getWriterMethod = OracleAdapter.getWriterFromClobMethod();
        try {

            Writer out = (Writer) getWriterMethod.invoke(clob, null);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }

        }
        catch (InvocationTargetException e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
    }

    /**
     * Writing of LOBs is not supported prior to JDBC 3.0 and has to be done using Oracle
     * driver utilities.
     */
    private void writeClob(Clob clob, String value) {
        // obtain Writer and write CLOB
        Method getWriterMethod = OracleAdapter.getWriterFromClobMethod();
        try {

            Writer out = (Writer) getWriterMethod.invoke(clob, null);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }

        }
        catch (InvocationTargetException e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
    }
}