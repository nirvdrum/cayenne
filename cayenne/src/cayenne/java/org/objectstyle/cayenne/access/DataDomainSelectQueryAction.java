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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectFactory;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.ParameterizedQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Executes non-selecting queries on behalf of DataDomain.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// Differences with DataContextSelectAction:
// * no support for IncrementalFaultList yet (as it relies on DataContext)
// * local cache is not checked ... caller must do it
// * if caller uses its own "shared" cache, this action won't have access to it and will
// use real shared cache instead...
class DataDomainSelectQueryAction {

    DataDomain domain;

    DataDomainSelectQueryAction(DataDomain domain) {
        this.domain = domain;
    }

    List performQuery(
            ObjectContext context,
            String queryName,
            Map parameters,
            boolean refresh) {

        // find query...
        Query query = domain.getEntityResolver().getQuery(queryName);
        if (query == null) {
            throw new CayenneRuntimeException("There is no saved query for name '"
                    + queryName
                    + "'.");
        }

        // for SelectQuery we must always run parameter substitution as the query
        // in question might have unbound values in the qualifier... that's a bit
        // inefficient... any better ideas to determine whether we can skip parameter
        // processing?

        // another side effect from NOT substituting parameters is that caching key of the
        // final query will be that of the original query... thus parameters vs. no
        // paramete will result in inconsistent caching behavior.

        if (query instanceof SelectQuery) {
            SelectQuery select = (SelectQuery) query;
            if (select.getQualifier() != null) {
                query = select.createQuery(parameters != null
                        ? parameters
                        : Collections.EMPTY_MAP);
            }
        }
        else if (parameters != null
                && !parameters.isEmpty()
                && query instanceof ParameterizedQuery) {
            query = ((ParameterizedQuery) query).createQuery(parameters);
        }

        if (!(query instanceof GenericSelectQuery)) {
            throw new CayenneRuntimeException("Query for name '"
                    + queryName
                    + "' is not a GenericSelectQuery: "
                    + query);
        }

        return performQuery(context, (GenericSelectQuery) query, query.getName(), refresh);
    }

    List performQuery(ObjectContext context, GenericSelectQuery query) {
        return performQuery(context, query, query.getName(), query.isRefreshingObjects());
    }

    List performQuery(
            ObjectContext context,
            GenericSelectQuery query,
            String cacheKey,
            boolean refreshCache) {

        // check if result pagination is requested
        // let a list handle fetch in this case
        if (query.getPageSize() > 0) {
            throw new CayenneRuntimeException("Paginated queries will be supported soon");
        }

        boolean sharedCache = GenericSelectQuery.SHARED_CACHE.equals(query
                .getCachePolicy());

        String name = query.getName();

        // sanity check
        if (sharedCache && name == null) {
            throw new CayenneRuntimeException(
                    "Caching of unnamed queries is not supported.");
        }

        // get results from cache...
        if (!refreshCache && sharedCache) {

            List rows = domain.getSharedSnapshotCache().getCachedSnapshots(cacheKey);
            if (rows != null) {

                // decorate shared cached lists with immutable list to avoid messing
                // up the cache

                if (rows.size() == 0) {
                    return Collections.EMPTY_LIST;
                }
                else if (query.isFetchingDataRows()) {
                    return Collections.unmodifiableList(rows);
                }
                else {
                    ObjEntity root = domain.getEntityResolver().lookupObjEntity(query);
                    return new EntityObjectFactory(context, query)
                            .objectsFromDataRows(root, rows);
                }
            }

        }

        // TODO: even with the move to ObjectFactory, SelectObserver still won't support
        // ObjectContext prefetches as it relies on DataObject/DataContext internally...
        // so need a different observer.
        SelectObserver observer = new SelectObserver(query.getLoggingLevel());
        ObjectFactory factory = new EntityObjectFactory(context, query);
        ObjEntity rootEntity = domain.getEntityResolver().lookupObjEntity(query);
        domain.performQueries(queryWithPrefetches(query), observer);

        List results = (query.isFetchingDataRows())
                ? observer.getResults(query)
                : observer.getResultsAsObjects(factory, rootEntity, query);

        // cache results if needed (note - if caller needs to set a local cache it is
        // caller's responsibility...
        if (sharedCache) {
            domain.getSharedSnapshotCache().cacheSnapshots(cacheKey,
                    observer.getResults(query));
        }

        return results;
    }

    /**
     * Expands a SelectQuery into a collection of queries, including prefetch queries if
     * needed.
     */
    Collection queryWithPrefetches(GenericSelectQuery query) {

        // check conditions for prefetch...
        if (query.isFetchingDataRows() || !(query instanceof SelectQuery)) {
            return Collections.singletonList(query);
        }

        SelectQuery selectQuery = (SelectQuery) query;

        Collection prefetchKeys = selectQuery.getPrefetches();
        if (prefetchKeys.isEmpty()) {
            return Collections.singletonList(query);
        }

        List queries = new ArrayList(prefetchKeys.size() + 1);
        queries.add(query);

        Iterator prefetchIt = prefetchKeys.iterator();
        while (prefetchIt.hasNext()) {
            PrefetchSelectQuery prefetchQuery = new PrefetchSelectQuery(domain
                    .getEntityResolver(), selectQuery, (String) prefetchIt.next());
            queries.add(prefetchQuery);
        }

        return queries;
    }

}
