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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;

/**
 * A DataContext helper that handles select query execution.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataContextSelectAction {

    DataContext context;
    Query query;
    QueryMetadata metadata;

    DataContextSelectAction(DataContext context, Query query) {
        this.context = context;
        this.query = query;
        this.metadata = query.getMetaData(context.getEntityResolver());
    }

    /**
     * Selects an appropriate select execution strategy and runs the query.
     */
    List execute() {

        if (metadata.getPageSize() > 0) {
            return new IncrementalFaultList(context, query);
        }

        if (QueryMetadata.LOCAL_CACHE.equals(metadata.getCachePolicy())) {
            return getListViaCache();
        }
        else {
            return getList();
        }
    }

    /*
     * Wraps execution in local cache checks.
     */
    private List getListViaCache() {
        if (query.getName() == null) {
            throw new CayenneRuntimeException(
                    "Caching of unnamed queries is not supported.");
        }

        if (!metadata.isRefreshingObjects()) {
            List cachedResults = context.getObjectStore().getCachedQueryResult(
                    query.getName());
            if (cachedResults != null) {
                return cachedResults;
            }
        }

        List results = getList();
        context.getObjectStore().cacheQueryResult(query.getName(), results);
        return results;
    }

    /*
     * Fetches data directly from the channel.
     */
    private List getList() {
        if (context.getChannel() instanceof ObjectContext) {
            return getListFromObjectContext();
        }
        else {
            return getListFromChannel();
        }
    }

    private List getListFromObjectContext() {
        List parentObjects = context.getChannel().performQuery(query);

        // TODO: handle refreshing...

        return context.localObjects(parentObjects);
    }

    private List getListFromChannel() {
        QueryResponse response = context.getChannel().performGenericQuery(query);
        List mainRows = response.getFirstRows(query);

        if (metadata.isFetchingDataRows()) {
            return mainRows;
        }

        if (mainRows.isEmpty()) {
            return new ArrayList(1);
        }

        ObjEntity entity = metadata.getObjEntity();
        PrefetchTreeNode prefetchTree = metadata.getPrefetchTree();

        // take a shortcut when no prefetches exist...
        if (prefetchTree == null) {
            return new ObjectResolver(
                    context,
                    entity,
                    metadata.isRefreshingObjects(),
                    metadata.isResolvingInherited())
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

        ObjectTreeResolver resolver = new ObjectTreeResolver(context, metadata);

        // double-sync row processing
        synchronized (context.getObjectStore()) {
            synchronized (context.getObjectStore().getDataRowCache()) {
                return resolver.resolveObjectTree(prefetchTree, mainRows, rowsByPath);
            }
        }
    }
}