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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;

/**
 * A DataContext helper that handles select query execution.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
class DataContextSelectAction {

    DataContext context;

    DataContextSelectAction(DataContext context) {
        this.context = context;
    }

    List performQuery(Query query) {
        boolean refresh = (query instanceof GenericSelectQuery)
                ? ((GenericSelectQuery) query).isRefreshingObjects()
                : GenericSelectQuery.REFRESHING_OBJECTS_DEFAULT;

        return performQuery(query, query.getName(), refresh);
    }

    List performQuery(Query query, String cacheKey, boolean refreshCache) {

        String cachePolicy = GenericSelectQuery.CACHE_POLICY_DEFAULT;
        boolean dataRows = GenericSelectQuery.FETCHING_DATA_ROWS_DEFAULT;
        boolean inheritance = GenericSelectQuery.RESOLVING_INHERITED_DEFAULT;

        if (query instanceof GenericSelectQuery) {
            GenericSelectQuery select = (GenericSelectQuery) query;

            // check if result pagination is requested
            // let a list handle fetch in this case
            if (select.getPageSize() > 0) {
                return new IncrementalFaultList(context, select);
            }

            // init local select parameters
            cachePolicy = select.getCachePolicy();
            dataRows = select.isFetchingDataRows();
            inheritance = select.isResolvingInherited();
        }

        boolean localCache = GenericSelectQuery.LOCAL_CACHE.equals(cachePolicy);
        boolean sharedCache = GenericSelectQuery.SHARED_CACHE.equals(cachePolicy);
        boolean useCache = localCache || sharedCache;

        String name = query.getName();

        // sanity check
        if (useCache && name == null) {
            throw new CayenneRuntimeException(
                    "Caching of unnamed queries is not supported.");
        }

        // get results from cache...
        if (!refreshCache && useCache) {
            List results = null;

            if (localCache) {
                // results should have been stored as rows or objects when
                // they were originally cached... do no conversions now
                results = context.getObjectStore().getCachedQueryResult(cacheKey);
            }
            else if (sharedCache) {

                List rows = context
                        .getObjectStore()
                        .getDataRowCache()
                        .getCachedSnapshots(cacheKey);
                if (rows != null) {

                    // decorate shared cached lists with immutable list to avoid messing
                    // up the cache
                    if (rows.size() == 0) {
                        results = Collections.EMPTY_LIST;
                    }
                    else if (dataRows) {
                        results = Collections.unmodifiableList(rows);
                    }
                    else {
                        ObjEntity root = context.getEntityResolver().lookupObjEntity(
                                query);
                        results = context.objectsFromDataRows(
                                root,
                                rows,
                                refreshCache,
                                inheritance);
                    }
                }
            }

            if (results != null) {
                return results;
            }
        }

        // must fetch...
        QueryResult observer = new QueryResult();
        context.performQueries(Collections.singletonList(query), observer);

        List results;

        if (dataRows) {
            results = observer.getFirstRows(query);
        }
        else {
            results = getResultsAsObjects(query, observer, refreshCache, inheritance);
        }

        // cache results if needed
        if (useCache) {
            if (localCache) {
                context.getObjectStore().cacheQueryResult(cacheKey, results);
            }
            else if (sharedCache) {
                context.getObjectStore().getDataRowCache().cacheSnapshots(
                        cacheKey,
                        observer.getFirstRows(query));
            }
        }

        return results;
    }

    List getResultsAsObjects(
            Query rootQuery,
            QueryResponse response,
            boolean refresh,
            boolean resolveInheritanceTree) {

        List mainRows = response.getFirstRows(rootQuery);

        if (mainRows.isEmpty()) {
            return new ArrayList(1);
        }

        ObjEntity entity = context.getEntityResolver().lookupObjEntity(rootQuery);
        PrefetchTreeNode prefetchTree = (rootQuery instanceof GenericSelectQuery)
                ? ((GenericSelectQuery) rootQuery).getPrefetchTree()
                : null;

        // take a shortcut when no prefetches exist...
        if (prefetchTree == null) {
            return new ObjectResolver(context, entity, refresh, resolveInheritanceTree)
                    .synchronizedObjectsFromDataRows(mainRows);
        }

        // map results to prefetch paths
        Map rowsByPath = new HashMap();

        // find result set
        Iterator it = response.allQueries().iterator();

        while (it.hasNext()) {
            Query q = (Query) it.next();

            if (q instanceof PrefetchSelectQuery) {
                PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) q;
                rowsByPath.put(prefetchQuery.getPrefetchPath(), response.getFirstRows(q));
            }
        }

        ObjectTreeResolver resolver = new ObjectTreeResolver(
                context,
                entity,
                refresh,
                resolveInheritanceTree);

        // double-sync row processing
        synchronized (context.getObjectStore()) {
            synchronized (context.getObjectStore().getDataRowCache()) {
                return resolver.resolveObjectTree(prefetchTree, mainRows, rowsByPath);
            }
        }
    }
}