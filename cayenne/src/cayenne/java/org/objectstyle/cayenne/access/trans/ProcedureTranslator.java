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
package org.objectstyle.cayenne.access.trans;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParam;
import org.objectstyle.cayenne.query.ProcedureQuery;

/**
 * Stored procedure query translator.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureTranslator extends QueryTranslator {
    protected List values;

    public PreparedStatement createStatement(Level logLevel) throws Exception {
        long t1 = System.currentTimeMillis();

        initValues();
        String sqlStr = createSqlString();

        QueryLogger.logQuery(
            logLevel,
            sqlStr,
            values,
            System.currentTimeMillis() - t1);
        CallableStatement stmt = con.prepareCall(sqlStr);
        initStatement(stmt);
        return stmt;
    }

    /**
     * Creates an SQL String for the stored procedure call.
     */
    protected String createSqlString() {
        Procedure proc = getProcedure();
        List params = proc.getCallParamsList();

        StringBuffer buf = new StringBuffer();
        buf.append("{call ").append(proc.getName());

        if (params.size() > 0) {
            // unroll the loop
            buf.append("(?");

            for (int i = 1; i < params.size(); i++) {
                buf.append(", ?");
            }

            buf.append(")");
        }

        buf.append("}");
        return buf.toString();
    }

    protected void initValues() {
        List params = getProcedure().getCallParamsList();
        Map queryValues = getProcedureQuery().getParams();

        // match values with parameters in the correct order.
        // make an assumption that a missing value is NULL
        // Any reason why this is bad?

        values = new ArrayList(params.size());
        Iterator it = params.iterator();
        while (it.hasNext()) {
            ProcedureParam param = (ProcedureParam)it.next();
            values.add(queryValues.get(param.getName()));
        }
    }

    protected void initStatement(CallableStatement stmt) throws Exception {
        if (values != null && values.size() > 0) {
            List params = getProcedure().getCallParamsList();
            
            int len = values.size();
            for (int i = 0; i < len; i++) {
                Object val = values.get(i);

                ProcedureParam attr = (ProcedureParam) params.get(i);

                // null DbAttributes are a result of inferior qualifier processing
                // (qualifier can't map parameters to DbAttributes and therefore
                // only supports standard java types now)
                // hence, a special moronic case here:
                if (attr == null) {
                    stmt.setObject(i + 1, val);
                } else {
                    int type = attr.getType();

                    if (val == null)
                        stmt.setNull(i + 1, type);
                    else {
                        ExtendedType map =
                            adapter.getTypeConverter().getRegisteredType(
                                val.getClass().getName());
                        Object jdbcVal =
                            (map == null) ? val : map.toJdbcObject(val, type);
                        stmt.setObject(i + 1, jdbcVal, type);
                    }
                }
            }
        }
    }

    public ProcedureQuery getProcedureQuery() {
        return (ProcedureQuery) query;
    }

    public Procedure getProcedure() {
        return getProcedureQuery().getProcedure();
    }
}
