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
package org.objectstyle.cayenne.dba.db2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DefaultResultIterator;
import org.objectstyle.cayenne.access.DistinctResultIterator;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.ResultIterator;
import org.objectstyle.cayenne.access.trans.SelectQueryTranslator;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * Custom DataNode implementation for DB2.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class DB2DataNode extends DataNode {

    public DB2DataNode() {
        super();
    }

    public DB2DataNode(String name) {
        super(name);
    }

    /**
     * Overrides generic behavior to trap queries with DISTINCT that also contain TEXT or
     * IMAGE columns in result set. Such query results are filtered in-memory as SQLServer
     * does not support DISTINCT in this case.
     */
    protected void runSelect(
            Connection connection,
            Query query,
            OperationObserver delegate) throws SQLException, Exception {
        long t1 = System.currentTimeMillis();

        QueryTranslator transl = getAdapter().getQueryTranslator(query);
        transl.setEngine(this);
        transl.setCon(connection);

        PreparedStatement prepStmt = transl.createStatement(query.getLoggingLevel());
        ResultSet rs = prepStmt.executeQuery();

        SelectQueryTranslator assembler = (SelectQueryTranslator) transl;
        DefaultResultIterator workerIterator = new DefaultResultIterator(
                connection,
                prepStmt,
                rs,
                assembler.getResultDescriptor(rs),
                ((GenericSelectQuery) query).getFetchLimit());

        ResultIterator it = workerIterator;

        // CUSTOMIZATION: wrap result iterator if distinct has to be suppressed
        if (assembler instanceof DB2SelectTranslator) {
            DB2SelectTranslator customTranslator = (DB2SelectTranslator) assembler;
            if (customTranslator.isSuppressingDistinct()) {
                it = new DistinctResultIterator(workerIterator, customTranslator
                        .getRootDbEntity());
            }
        }

        if (!delegate.isIteratedResult()) {
            // note that we don't need to close ResultIterator
            // since "dataRows" will do it internally
            List resultRows = it.dataRows(true);
            QueryLogger.logSelectCount(query.getLoggingLevel(), resultRows.size(), System
                    .currentTimeMillis()
                    - t1);

            delegate.nextDataRows(query, resultRows);
        }
        else {
            try {
                workerIterator.setClosingConnection(true);
                delegate.nextDataRows(transl.getQuery(), it);
            }
            catch (Exception ex) {
                it.close();
                throw ex;
            }
        }
    }
}