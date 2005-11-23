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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;

/**
 * @author Andrei Adamchik
 */
public class QueryResultTst extends TestCase {

    protected QueryResult result;
    protected Query[] queries;

    protected void setUp() throws Exception {
        super.setUp();

        result = new QueryResult();
        queries = new Query[] {
                new SelectQuery(), new UpdateQuery(), new DeleteQuery()
        };
    }

    public void testQueries() {

        for (int i = 0; i < queries.length; i++) {
            result.nextCount(queries[i], i);
        }
        Iterator it = result.getQueries();
        assertNotNull(it);

        int ind = 0;
        while (it.hasNext()) {
            assertSame(queries[ind], it.next());
            ind++;
        }

        assertEquals(queries.length, ind);
    }

    public void testQueriesIterationOrder() {

        Query q1 = new MockQuery();
        Query q2 = new MockQuery();
        Query q3 = new MockQuery();
        Query q4 = new MockQuery();
        Query q5 = new MockQuery();
        Query q6 = new MockQuery();

        QueryResult result = new QueryResult();

        result.nextCount(q1, 1);
        result.nextCount(q2, 1);
        result.nextBatchCount(q3, new int[] {1});
        result.nextDataRows(q4, new ArrayList());
        result.nextCount(q5, 1);
        result.nextCount(q6, 1);

        Query[] orderedArray = new Query[] {
                q1, q2, q3, q4, q5, q6
        };

        Iterator it = result.getQueries();
        for (int i = 0; i < orderedArray.length; i++) {
            assertTrue(it.hasNext());
            assertSame("Unexpected query at index " + i, orderedArray[i], it.next());
        }

        assertFalse(it.hasNext());
    }

    public void testResults() throws Exception {
        // add a mix of counts and rows
        result.nextCount(queries[0], 1);
        result.nextDataRows(queries[0], Collections.EMPTY_LIST);
        result.nextDataRows(queries[0], Collections.EMPTY_LIST);
        result.nextDataRows(queries[0], Collections.EMPTY_LIST);
        result.nextCount(queries[0], 5);

        Iterator it = result.getQueries();
        Query q = (Query) it.next();

        List rows = result.getRows(q);
        assertNotNull(rows);
        assertEquals(3, rows.size());

        List counts = result.getUpdates(q);
        assertNotNull(counts);
        assertEquals(2, counts.size());

        List all = result.getResults(q);
        assertNotNull(all);
        assertEquals(5, all.size());
    }
}