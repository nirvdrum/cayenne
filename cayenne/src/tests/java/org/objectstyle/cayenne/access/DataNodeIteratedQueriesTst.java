/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.*;

/**
 * @author Andrei Adamchik
 */
public class DataNodeIteratedQueriesTst extends JDBCAccessTestCase {
    protected DataNode testNode;

    protected void init() throws Exception {
        super.init();
        testNode = getNode().getAdapter().createDataNode("dummy");
        testNode.setDataMaps(getNode().getDataMaps());
        testNode.setEntityResolver(getNode().getEntityResolver());
    }

    public void testRunIteratedSelect() throws Exception {
        IteratedObserver observer = new IteratedObserver();

        init();

        // first assert that created node is valid
        assertNotNull(testNode.getEntityResolver());
        assertNotNull(testNode.getEntityResolver().lookupObjEntity(query));

        testNode.runSelect(connection, query, observer);
        assertEquals(DataContextTst.artistCount, observer.getResultCount());

        // no cleanup is needed, since observer will close the iterator
    }

    public void testFailIterated() throws Exception {
        // must fail multiple queries when one of them is iterated
        IteratedObserver observer = new IteratedObserver();

        List queries = new ArrayList();
        queries.add(new SelectQuery("Artist"));
        queries.add(new SelectQuery("Artist"));

        getNode().performQueries(queries, observer);

        assertEquals(0, observer.getResultCount());
        assertTrue(
            "Iterated queries are not allowed in batches.",
            observer.hasExceptions());
    }

    /**
     * Checks that when an iterated query fails prior to returning
     * to the caller, connection is being closed properly. 
     */
    public void testEarlyIteratedFailure() throws Exception {
        IteratedFailingNode node = new IteratedFailingNode();
        node.setDataMaps(getNode().getDataMaps());
        node.setAdapter(getNode().getAdapter());
        node.setDataSource(getNode().getDataSource());

        SelectObserver observer = new SelectObserver() {
            public boolean isIteratedResult() {
                return true;
            }
        };

        SelectQuery query = new SelectQuery("Artist");

        try {
            node.performQueries(Collections.singletonList(query), observer);
            fail("SQLException expected.");
        }
        catch (CayenneRuntimeException ex) {
            // by now connection must be closed
            assertNotNull("Node connection is null.", node.connection);
            assertTrue("Node did not close connection.", node.connection.isClosed());
        }
        finally {
            // To avoid total meltdown if connection is not closed,
            // close it here again 
            try {
                if (node.connection != null) {
                    node.connection.close();
                }
            }
            catch (Throwable th) {
                // ignore it...
            }
        }
    }

    /**
      * Checks that when an iterated query fails prior to returning
      * to the caller, connection is being closed properly. This case
      * is being run with a transaction to emulate usual DataContext behavior.
      */
    public void testEarlyIteratedFailure2() throws Exception {
        IteratedFailingNode node = new IteratedFailingNode();
        node.setDataMaps(getNode().getDataMaps());
        node.setAdapter(getNode().getAdapter());
        node.setDataSource(getNode().getDataSource());

        SelectObserver observer = new SelectObserver() {
            public boolean isIteratedResult() {
                return true;
            }
        };

        SelectQuery query = new SelectQuery(Artist.class);
        Transaction transaction = Transaction.externalTransaction(null);

        try {
            transaction.performQueries(node, Collections.singletonList(query), observer);
            fail("SQLException expected.");
        }
        catch (CayenneRuntimeException ex) {
            // by now connection must be closed
            assertNotNull("Node connection is null.", node.connection);
            assertTrue("Node did not close connection.", node.connection.isClosed());
        }
        finally {
            // To avoid total meltdown if connection is not closed,
            // close it here again 
            try {
                if (node.connection != null) {
                    node.connection.close();
                }
            }
            catch (Throwable th) {
                // ignore it...
            }
        }
    }

    class IteratedFailingNode extends DataNode {
        Connection connection;

        IteratedFailingNode() {
            super("test");
        }

        protected void runSelect(
            Connection connection,
            Query query,
            OperationObserver delegate)
            throws SQLException, Exception {

            this.connection = connection;
            throw new SQLException("Test Exception");
        }
    }

    class IteratedObserver extends DefaultOperationObserver {
        protected int count;

        public boolean isIteratedResult() {
            return true;
        }

        public void nextDataRows(Query q, ResultIterator it) {
            try {
                while (it.hasNextRow()) {
                    it.nextDataRow();
                    count++;
                }
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error processing result iterator", ex);
            }
            finally {

                try {
                    it.close();
                }
                catch (Exception ex) {
                    throw new CayenneRuntimeException(
                        "Error closing result iterator",
                        ex);
                }
            }
        }

        public int getResultCount() {
            return count;
        }
    }
}
