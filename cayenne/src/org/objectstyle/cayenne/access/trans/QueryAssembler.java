package org.objectstyle.cayenne.access.trans;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.Query;

/** Abstract superclass of Query translators.
 *  Defines callback methods for helper classes 
 *  that are delegated tasks of building query parts.
 *
 * @author Andrei Adamchik 
 */
public abstract class QueryAssembler extends QueryTranslator {
    static Logger logObj = Logger.getLogger(QueryAssembler.class.getName());

    /** PreparedStatement values. */
    protected ArrayList values = new ArrayList();

    /** PreparedStatement attributes matching entries in <code>values</code> list.. */
    protected ArrayList attributes = new ArrayList();

    /** Query being translated. */
    protected Query query;

    /** JDBC database connection needed to create PreparedStatement. */
    protected Connection con;

    /** Used mainly for name resolution. */
    protected QueryEngine engine;

    /** Adapter helping to do SQL literal conversions, etc. */
    protected DbAdapter adapter;

    public QueryAssembler(
        QueryEngine engine,
        Connection con,
        DbAdapter adapter,
        Query query) {
        this.engine = engine;
        this.con = con;
        this.query = query;
        this.adapter = adapter;
    }

    /** Returns query object being processed. */
    public Query getQuery() {
        return query;
    }

    /** Processes a join being added. */
    public abstract void dbRelationshipAdded(DbRelationship dbRel);

    /** Translates query into sql string. This is a workhorse
    * method of QueryAssembler. It is called internally from
    * <code>createStatement</code>. Usually there is no need
    * to invoke it explicitly. */
    public abstract String createSqlString() throws java.lang.Exception;

    /** Returns a name that can be used as column alias.
     *  This can be one of the following:
     *  <ul>
     *  <li>an alias for this table, if it uses aliases
     *  <li>a fully qualified table name, if not
     *  <li> null if dbEnt is not in its list of tables
     *  </ul>
     *
     *  Default implementation throws RuntimeException
     */
    public abstract String aliasForTable(DbEntity dbEnt);


    /** Returns Connection object used by this assembler. */
    public Connection getCon() {
        return con;
    }


    /** Returns QueryEngine used by this assembler. */
    public QueryEngine getEngine() {
        return engine;
    }

    public ObjEntity getRootEntity() {
        return engine.lookupEntity(query.getObjEntityName());
    }

    /** Returns <code>true</code> if table aliases are supported.
      * Default implementation returns false. */
    public boolean supportsTableAlases() {
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

        String sqlStr = createSqlString();
        QueryLogger.logQuery(logLevel, sqlStr, values);
        PreparedStatement stmt = con.prepareStatement(sqlStr);

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
                }
                else {
                    int type = attr.getType();
                    int precision = attr.getPrecision();

                    if (val == null)
                        stmt.setNull(i + 1, type);
                    else {
                        ExtendedType map =
                            ExtendedTypeMap.sharedInstance().getRegisteredType(val.getClass().getName());
                        Object jdbcVal = (map == null) ? val : map.toJdbcObject(val, type);
                        stmt.setObject(i + 1, jdbcVal, type, precision);
                    }
                }
            }
        }
        return stmt;
    }
}