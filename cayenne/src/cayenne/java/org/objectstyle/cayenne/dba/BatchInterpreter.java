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

package org.objectstyle.cayenne.dba;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;

/**
 * BatchInterpreter performs BatchQueries in a JDBC specific fashion
 * Its descendants may even employ some RDBS specific features for
 * the sake of batch efficiency. It is mostly used by DataNodes.
 *
 * @author Andriy Shapochka
 */

public class BatchInterpreter {
    private static Logger logObj = Logger.getLogger(BatchInterpreter.class);

    private DbAdapter adapter;
    private BatchQueryBuilder queryBuilder;

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }
    public DbAdapter getAdapter() {
        return adapter;
    }
    public BatchQueryBuilder getQueryBuilder() {
        return queryBuilder;
    }
    public void setQueryBuilder(BatchQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public int[] execute(BatchQuery batch, Connection connection) throws SQLException, CayenneException {
        List dbAttributes = batch.getDbAttributes();
        int attributeCount = dbAttributes.size();
        int[] attributeTypes = new int[attributeCount];
        int[] attributeScales = new int[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            DbAttribute attribute = (DbAttribute)dbAttributes.get(i);
            attributeTypes[i] = attribute.getType();
            attributeScales[i] = attribute.getPrecision();
        }
        String query = queryBuilder.query(batch);
        PreparedStatement st = null;
        ExtendedTypeMap typeConverter = adapter.getTypeConverter();
        try {
            st = connection.prepareStatement(query);
            batch.reset();
            while (batch.next()) {
                for (int i = 0; i < attributeCount; i++) {
                    Object value = batch.getObject(i);
                    int type = attributeTypes[i];
                    if (value == null) st.setNull(i + 1, type);
                    else {
                        ExtendedType map = typeConverter.getRegisteredType(value.getClass().getName());
                        Object jdbcValue = (map == null) ? value : map.toJdbcObject(value, type);
                        st.setObject(i + 1, jdbcValue, type, attributeScales[i]);
                    }
                }
                st.addBatch();
            }
            return st.executeBatch();
        } catch (SQLException e) {
            throw e;
        } catch (CayenneException e) {
            throw e;
        } catch (Exception e) {
            throw new CayenneException(e);
        } finally {
            try {if (st != null) st.close();}
            catch (Exception e) {}
        }
    }
}

