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

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.trans.SelectTranslator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.ObjAttribute;

/** 
 * Select translator that implements Oracle-specific optimizations.
 * 
 * @author Andrei Adamchik
 */
public class OracleSelectTranslator extends SelectTranslator {
    static Logger logObj = Logger.getLogger(OracleSelectTranslator.class.getName());

    private static boolean testedDriver;
    private static boolean useOptimizations;
    private static Method statementDefineColumnType;
    private static Method statementSetRowPrefetch;
    private static final Object[] rowPrefetchArgs =
        new Object[] { new Integer(100)};

    /** 
     * Determines if we can use Oracle optimizations.
     * If yes, configure this object to use them via reflection.
     */
    private static final synchronized void testDriver(Statement st) {
        if (testedDriver) {
            return;
        }

        // invalid call.. give it another chance later
        if (st == null) {
            return;
        }

        testedDriver = true;

        try {
            // search for matching methods in class and its superclasses
            Class[] args1 = new Class[] { Integer.TYPE, Integer.TYPE };
            statementDefineColumnType = st.getClass().getMethod("defineColumnType", args1);

            Class[] args2 = new Class[] { Integer.TYPE };
            statementSetRowPrefetch = st.getClass().getMethod("setRowPrefetch", args2);

            useOptimizations = true;
        }
        catch (Exception ex) {
            useOptimizations = false;
            statementSetRowPrefetch = null;
            statementDefineColumnType = null;

            StringBuffer buf = new StringBuffer();
            buf
                .append("Unknown Oracle statement type: [")
                .append(st.getClass().getName())
                .append("]. No Oracle optimizations applied.");

            logObj.info(buf.toString());
        }
    }

    /** 
     * Translates internal query into PreparedStatement,
     * applying Oracle optimizations if possible.
     */
    public PreparedStatement createStatement(Level logLevel) throws Exception {
        String sqlStr = createSqlString();
        QueryLogger.logQuery(logLevel, sqlStr, values);
        PreparedStatement stmt = con.prepareStatement(sqlStr);

        initStatement(stmt);

        if (!testedDriver) {
            testDriver(stmt);
        }

        if (useOptimizations) {
            // apply Oracle optimization of the statement

            // 1. name result columns
            List columns = getColumnList();
            int len = columns.size();
            Object[] args = new Object[2];
            for (int i = 0; i < len; i++) {
                DbAttribute attr = (DbAttribute) columns.get(i);
                args[0] = new Integer(i + 1);
                args[1] = new Integer(attr.getType());
                statementDefineColumnType.invoke(stmt, args);
            }

            // 2. prefetch bigger batches of rows
            // [This optimization didn't give any measurable performance
            // increase. Keeping it for the future research]
           
            // Note that this is done by statement,
            // instead of Connection, since we do not want to mess 
            // with Connection that is potentially used by
            // other people.
            statementSetRowPrefetch.invoke(stmt, rowPrefetchArgs);
        }
        
        return stmt;
    }
}