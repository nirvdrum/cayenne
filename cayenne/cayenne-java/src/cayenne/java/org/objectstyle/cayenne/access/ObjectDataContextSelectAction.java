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

import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryExecutionPlan;

/**
 * Executes non-selecting queries on behalf of DataDomain.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// Differences with DataContextSelectAction:
// * shared cache handling is delegated to the underlying PersistenceContext.
class ObjectDataContextSelectAction extends DataContextSelectAction {

    ObjectDataContext context;

    ObjectDataContextSelectAction(ObjectDataContext context) {
        super(context);
        this.context = context;
    }

    List performQuery(QueryExecutionPlan queryPlan) {
        GenericSelectQuery selectQuery = resolveQuery(queryPlan);

        // check if result pagination is requested
        // let a list handle fetch in this case
        if (selectQuery.getPageSize() > 0) {
            return new IncrementalFaultList(context, selectQuery);
        }

        String cacheKey = selectQuery.getName();
        boolean cacheResults = GenericSelectQuery.LOCAL_CACHE.equals(selectQuery
                .getCachePolicy());

        // get results from cache...
        if (cacheResults) {

            // sanity check
            if (cacheKey == null) {
                throw new CayenneRuntimeException(
                        "Caching of unnamed queries is not supported.");
            }

            // results should have been stored as rows or objects when
            // they were originally cached... do no conversions now
            List results = context.getObjectStore().getCachedQueryResult(cacheKey);
            if (results != null) {
                return results;
            }
        }

        // must fetch...
        QueryResult observer = new QueryResult();
        context.getParentContext().performQuery(selectQuery, observer);

        List results;

        if (selectQuery.isFetchingDataRows()) {
            results = observer.getFirstRows(selectQuery);
        }
        else {
            results = getResultsAsObjects(selectQuery, observer);
        }

        // cache results if needed
        if (cacheResults) {
            context.getObjectStore().cacheQueryResult(cacheKey, results);
        }

        return results;
    }

    /**
     * Executes query resolving phase...
     */
    GenericSelectQuery resolveQuery(QueryExecutionPlan queryPlan) {
        Query resolved = queryPlan.resolve(context.getEntityResolver());

        if (!(resolved instanceof GenericSelectQuery)) {
            throw new CayenneRuntimeException(
                    "QueryExecutionPlan was resolved to a query that is not a GenericSelectQuery: "
                            + resolved);
        }

        return (GenericSelectQuery) resolved;
    }
}
