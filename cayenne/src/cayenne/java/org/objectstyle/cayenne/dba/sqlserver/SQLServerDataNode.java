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
package org.objectstyle.cayenne.dba.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Iterator;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * SQLServer-specific DataNode implementation that handles certain issues like identity
 * columns, etc.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
public class SQLServerDataNode extends DataNode {

    public SQLServerDataNode() {
        super();
    }

    public SQLServerDataNode(String name) {
        super(name);
    }

    /**
     * Executes stored procedure with SQLServer specific action.
     */
    protected void runStoredProcedure(
            Connection con,
            Query query,
            OperationObserver observer) throws SQLException, Exception {

        new SQLServerProcedureAction(getAdapter(), getEntityResolver()).performAction(
                con,
                query,
                observer);
    }

    /**
     * Implements handling of identity PK columns.
     */
    protected void runBatchUpdate(
            Connection connection,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        // this condition checks if identity coilumns are present in the query and adapter
        // is not ready to process them... e.g. if we are using a MS driver...
        if (expectsToOverrideIdentityColumns(query)) {

            String configSQL = "SET IDENTITY_INSERT "
                    + query.getDbEntity().getFullyQualifiedName()
                    + " ON";

            QueryLogger.logQuery(
                    query.getLoggingLevel(),
                    configSQL,
                    Collections.EMPTY_LIST);

            Statement statement = connection.createStatement();
            try {
                statement.execute(configSQL);
            }
            finally {
                try {
                    statement.close();
                }
                catch (Exception e) {
                }
            }
        }

        super.runBatchUpdate(connection, query, delegate);
    }

    /**
     * Returns whether a table has identity columns.
     */
    protected boolean expectsToOverrideIdentityColumns(BatchQuery query) {
        // jTDS driver supports identity columns, no need for tricks...
        if (getAdapter().supportsGeneratedKeys()) {
            return false;
        }

        if (!(query instanceof InsertBatchQuery) || query.getDbEntity() == null) {
            return false;
        }

        // find identity attributes
        Iterator it = query.getDbEntity().getAttributes().iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            if (attribute.isGenerated()) {
                return true;
            }
        }

        return false;
    }
}