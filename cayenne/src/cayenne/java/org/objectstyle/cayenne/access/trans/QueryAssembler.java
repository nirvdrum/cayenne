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

import java.sql.PreparedStatement;
import java.util.ArrayList;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Abstract superclass of Query translators.
 *
 * @author Andrei Adamchik 
 */
public abstract class QueryAssembler extends QueryTranslator {
    static Logger logObj = Logger.getLogger(QueryAssembler.class.getName());

    /** PreparedStatement values. */
    protected ArrayList values = new ArrayList();

    /** 
     * PreparedStatement attributes matching entries in <code>values</code> 
     * list. 
     */
    protected ArrayList attributes = new ArrayList();

    /** Processes a join being added. */
    public abstract void dbRelationshipAdded(DbRelationship dbRel);

    /** 
     * Translates query into sql string. This is a workhorse
     * method of QueryAssembler. It is called internally from
     * <code>createStatement</code>. Usually there is no need
     * to invoke it explicitly. 
     */
    public abstract String createSqlString() throws java.lang.Exception;

    public String aliasForTable(DbEntity ent, DbRelationship rel) {
        return aliasForTable(ent); //Default implementation
    }
    
    /** 
     * Returns a name that can be used as column alias.
     * This can be one of the following:
     * <ul>
     *  <li>an alias for this table, if it uses aliases</li>
     *  <li>a fully qualified table name, if not.</li>
     * </ul>
     *  
     * CayenneRuntimeException is thrown if a table alias
     * can not be created.
     */
    public abstract String aliasForTable(DbEntity dbEnt);

    /** Returns <code>true</code> if table aliases are supported.
      * Default implementation returns false. */
    public boolean supportsTableAliases() {
        return false;
    }

    /** Registers <code>anObject</code> as a PreparedStatement paramter.
     *
     *  @param anObject object that represents a value of DbAttribute
     *  @param dbAttr DbAttribute being processed.
     */
    public void addToParamList(DbAttribute dbAttr, Object anObject) {
        attributes.add(dbAttr);
        values.add(anObject);
    }

    /** Translates internal query into PreparedStatement. */
    public PreparedStatement createStatement(Level logLevel) throws Exception {
        long t1 = System.currentTimeMillis();
        String sqlStr = createSqlString();
        QueryLogger.logQuery(logLevel, sqlStr, values, System.currentTimeMillis() - t1);
        PreparedStatement stmt = con.prepareStatement(sqlStr);
        initStatement(stmt);
        return stmt;
    }

    /** 
     * Initializes prepared statements with collected parameters. 
     * Called internally from "createStatement". Cayenne users
     * shouldn't normally call it directly.
     */
    protected void initStatement(PreparedStatement stmt) throws Exception {
        if (values != null && values.size() > 0) {
            int len = values.size();
            for (int i = 0; i < len; i++) {
                Object val = values.get(i);

                DbAttribute attr = (DbAttribute) attributes.get(i);

                // null DbAttributes are a result of inferior qualifier processing
                // (qualifier can't map parameters to DbAttributes and therefore
                // only supports standard java types now)
                // hence, a special moronic case here:
                if (attr == null) {
                    stmt.setObject(i + 1, val);
                } else {
                    int type = attr.getType();
                    int precision = attr.getPrecision();

                    if (val == null)
                        stmt.setNull(i + 1, type);
                    else {
                        ExtendedType map =
                            adapter.getTypeConverter().getRegisteredType(
                                val.getClass().getName());
                        Object jdbcVal =
                            (map == null) ? val : map.toJdbcObject(val, type);
                        stmt.setObject(i + 1, jdbcVal, type, precision);
                    }
                }
            }
        }
    }
}