/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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
package org.objectstyle.cayenne.dba.oracle;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryWrapper;
import org.objectstyle.cayenne.access.trans.LOBInsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBUpdateBatchQueryBuilder;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.util.ResultDescriptor;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

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
     * Implements Oracle-specific handling of StoredProcedure OUT parameters reading.
     */
    protected void readStoredProcedureOutParameters(
        CallableStatement statement,
        ResultDescriptor descriptor,
        Query query,
        OperationObserver delegate)
        throws SQLException, Exception {

        long t1 = System.currentTimeMillis();

        int resultSetType = OracleAdapter.getOracleCursorType();
        int resultWidth = descriptor.getResultWidth();
        if (resultWidth > 0) {
            Map dataRow = new HashMap(resultWidth * 2, 0.75f);
            ExtendedType[] converters = descriptor.getConverters();
            int[] jdbcTypes = descriptor.getJdbcTypes();
            String[] names = descriptor.getNames();
            int[] outParamIndexes = descriptor.getOutParamIndexes();

            // process result row columns,
            for (int i = 0; i < outParamIndexes.length; i++) {
                int index = outParamIndexes[i];

                if (jdbcTypes[index] == resultSetType) {
                    // note: jdbc column indexes start from 1, not 0 unlike everywhere else
                    ResultSet rs = (ResultSet) statement.getObject(index + 1);
                    ResultDescriptor nextDesc =
                        ResultDescriptor.createDescriptor(
                            rs,
                            getAdapter().getExtendedTypes());

                    readResultSet(rs, nextDesc, (GenericSelectQuery) query, delegate);
                }
                else {
                    // note: jdbc column indexes start from 1, not 0 unlike everywhere else
                    Object val =
                        converters[index].materializeObject(
                            statement,
                            index + 1,
                            jdbcTypes[index]);
                    dataRow.put(names[index], val);
                }
            }

            if (!dataRow.isEmpty()) {
                QueryLogger.logSelectCount(
                    query.getLoggingLevel(),
                    1,
                    System.currentTimeMillis() - t1);
                delegate.nextDataRows(query, Collections.singletonList(dataRow));
            }
        }
    }

    public void runBatchUpdateWithLOBColumns(
        Connection con,
        BatchQuery query,
        OperationObserver delegate)
        throws SQLException, Exception {

        LOBBatchQueryBuilder queryBuilder;
        switch (query.getQueryType()) {
            case Query.INSERT_QUERY :
                queryBuilder = new LOBInsertBatchQueryBuilder(getAdapter());
                break;
            case Query.UPDATE_QUERY :
                queryBuilder = new LOBUpdateBatchQueryBuilder(getAdapter());
                break;
            default :
                throw new CayenneException(
                    "Unsupported batch type for special LOB processing: "
                        + query.getQueryType());
        }

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        queryBuilder.setNewBlobFunction(OracleAdapter.NEW_BLOB_FUNCTION);
        queryBuilder.setNewClobFunction(OracleAdapter.NEW_CLOB_FUNCTION);

        // no batching is done, queries are translated 
        // for each batch set, since prepared statements
        // may be different depending on whether LOBs are NULL or not..

        List dbAttributes = query.getDbAttributes();
        LOBBatchQueryWrapper selectQuery = new LOBBatchQueryWrapper(query);
        List qualifierAttributes = selectQuery.getDbAttributesForLOBSelectQualifier();

        query.reset();
        while (selectQuery.next()) {
            int updated = 0;
            String updateStr = queryBuilder.createSqlString(query);

            // 1. run row update
            QueryLogger.logQuery(
                query.getLoggingLevel(),
                updateStr,
                Collections.EMPTY_LIST);

            PreparedStatement statement = con.prepareStatement(updateStr);
            try {
                queryBuilder.bindParameters(statement, query, dbAttributes);
                updated = statement.executeUpdate();
                QueryLogger.logUpdateCount(query.getLoggingLevel(), updated);
            }
            finally {
                try {
                    statement.close();
                }
                catch (Exception e) {
                }
            }

            // 2. run row LOB update (SELECT...FOR UPDATE and writing out LOBs)

            List lobAttributes = selectQuery.getDbAttributesForUpdatedLOBColumns();
            if (lobAttributes.size() > 0) {
                String selectStr =
                    queryBuilder.createLOBSelectString(
                        query,
                        lobAttributes,
                        qualifierAttributes);

                QueryLogger.logQuery(
                    query.getLoggingLevel(),
                    selectStr,
                    Collections.EMPTY_LIST);

                PreparedStatement selectStatement = con.prepareStatement(selectStr);

                try {
                    queryBuilder.bindLOBParameters(
                        selectStatement,
                        selectQuery,
                        qualifierAttributes);

                    ResultSet result = selectStatement.executeQuery();

                    try {
                        if (!result.next()) {
                            throw new CayenneRuntimeException("Missing LOB row.");
                        }

                        // read the only expected row
                        int size = lobAttributes.size();
                        for (int i = 0; i < size; i++) {
                            DbAttribute attribute = (DbAttribute) lobAttributes.get(i);
                            int type = attribute.getType();

                            if (type == Types.CLOB) {
                                Clob clob = result.getClob(i + 1);
                                Object clobVal = selectQuery.getUpdatedClobValue(i);

                                if (clobVal instanceof char[]) {
                                    writeClob(clob, (char[]) clobVal);
                                }
                                else {
                                    writeClob(clob, clobVal.toString());
                                }
                            }
                            else if (type == Types.BLOB) {
                                Blob blob = result.getBlob(i + 1);
                                writeBlob(blob, selectQuery.getUpdatedBlobValue(i));
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

            // finally, notify delegate that the row was updated
            delegate.nextCount(query, updated);
        }
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes
     * super implementation.
     */
    protected void runBatchUpdateAsBatch(
        Connection con,
        BatchQuery query,
        BatchQueryBuilder queryBuilder,
        OperationObserver delegate)
        throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsBatch(con, query, queryBuilder, delegate);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes
     * super implementation.
     */
    protected void runBatchUpdateAsIndividualQueries(
        Connection con,
        BatchQuery query,
        BatchQueryBuilder queryBuilder,
        OperationObserver delegate)
        throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsIndividualQueries(con, query, queryBuilder, delegate);
    }

    /**
     * TODO: this may become an adapter method eventually. Writing of LOBs
     * is not supported prior to JDBC 3.0 and has to be done using Oracle driver 
     * utilities, using reflection.
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
            throw new CayenneRuntimeException(
                "Error processing BLOB.",
                Util.unwindException(e));
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                "Error processing BLOB.",
                Util.unwindException(e));
        }
    }

    /**
      * TODO: this may become an adapter method eventually. Writing of LOBs
      * is not supported prior to JDBC 3.0 and has to be done using Oracle driver 
      * utilities.
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
            throw new CayenneRuntimeException(
                "Error processing BLOB.",
                Util.unwindException(e));
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                "Error processing BLOB.",
                Util.unwindException(e));
        }
    }

    /**
     * TODO: this may become an adapter method eventually. Writing of LOBs
     * is not supported prior to JDBC 3.0 and has to be done using Oracle driver 
     * utilities.
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
            throw new CayenneRuntimeException(
                "Error processing BLOB.",
                Util.unwindException(e));
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                "Error processing BLOB.",
                Util.unwindException(e));
        }
    }
}
