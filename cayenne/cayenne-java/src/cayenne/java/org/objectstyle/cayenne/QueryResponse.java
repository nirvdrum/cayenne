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
package org.objectstyle.cayenne;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.objectstyle.cayenne.query.QueryExecutionPlan;

/**
 * Encapsulates results of query execution. A caller can use QueryResponse to inspect and
 * process <i>full</i> results of the query. QueryResponse can contain a mix of object or
 * data row collections and update counts. Such complex results are common when using
 * stored procedures or batches.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface QueryResponse extends Serializable {

    /**
     * Returns all queries whose results weere included in response.
     */
    Collection allQueries();

    /**
     * Returns a collection of results for a single Query in the order they were provided
     * by Cayenne stack. Collection can contain List objects (for selecting queries) and
     * java.lang.Number values for the update counts.
     * <p>
     * This is the most extreme case. Usually queries return a single result or a single
     * update count, so consider using <code>getFirstRows(Query)</code> or
     * <code>getFirstUpdateCount(Query)</code> instead.
     * </p>
     */
    List getResults(QueryExecutionPlan query);

    /**
     * Returns the first batch of update counts for the query. If the first update is not
     * a batch, it is still returned as an int[1] array. Returns an int[0] if no updates
     * were executed for the query. This is a shortcut to simplify extracting update count
     * in the most common case when it is known that the query resulted only one update.
     */
    int[] getFirstUpdateCounts(QueryExecutionPlan query);

    /**
     * Returns the first results for the query. This is a shortcut to simplify extracting
     * a result in the most common case when it is known that the query resulted only one
     * update.
     */
    public List getFirstRows(QueryExecutionPlan query);
}
