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
package org.objectstyle.cayenne.dba.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.InsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * DataNode subclass customized for Oracle database engine.
 * 
 * @author Andrei Adamchik
 */
public class OracleDataNode extends DataNode {

    /**
     * 
     */
    public OracleDataNode() {
        super();
    }

    /**
     * @param name
     */
    public OracleDataNode(String name) {
        super(name);
    }

    /**
     *
     */
    protected void runBatchUpdate(
        Connection con,
        BatchQuery query,
        OperationObserver delegate)
        throws SQLException, Exception {

        // TODO: refactoring super implementation  shoild make this method smaller
        // the only difference with super is using "addBatch" instead of "executeUpdate"

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

                // this line differs from super
                st.addBatch();
            }

            // this differs from super
            int[] results = st.executeBatch();
            delegate.nextBatchCount(query, results);
            
            // TODO: Create QUeryLogger method to log batch counts
        } finally {
            try {
                st.close();
            } catch (Exception e) {
            }
        }
    }

}
