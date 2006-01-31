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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.BaseResponse;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.query.RelationshipQuery;
import org.objectstyle.cayenne.query.SingleObjectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Performs query routing and execution. During execution phase intercepts callbacks to
 * the OperationObserver, remapping results to the original pre-routed queries.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainQueryAction implements QueryRouter, OperationObserver {

    static final boolean DONE = true;

    DataContext context;
    DataDomain domain;
    DataRowStore cache;
    Query query;
    QueryMetadata metadata;

    BaseResponse response;
    Map prefetchResultsByPath;
    Map queriesByNode;
    Map queriesByExecutedQueries;

    /*
     * A constructor for the "new" way of performing a query via 'execute' with
     * QueryResponse created internally.
     */
    DataDomainQueryAction(ObjectContext context, DataDomain domain, Query query) {
        if (context != null && !(context instanceof DataContext)) {
            throw new IllegalArgumentException(
                    "DataDomain can only work with DataContext. "
                            + "Unsupported context type: "
                            + context);
        }

        this.domain = domain;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());
        this.context = (DataContext) context;

        // cache may be shared or unique for the ObjectContext
        this.cache = this.context != null ? this.context
                .getObjectStore()
                .getDataRowCache() : domain.getSharedSnapshotCache();
    }

    QueryResponse execute() {

        // run chain...
        if (interceptOIDQuery() != DONE) {
            if (interceptRelationshipQuery() != DONE) {
                if (interceptSharedCache() != DONE) {
                    runQuery();
                }
            }
        }

        // turn results to objects
        interceptObjectConversion();

        return response;
    }

    private boolean interceptOIDQuery() {
        if (query instanceof SingleObjectQuery) {
            SingleObjectQuery oidQuery = (SingleObjectQuery) query;
            if (!oidQuery.isRefreshing()) {
                DataRow row = cache.getCachedSnapshot(oidQuery.getObjectId());
                if (row != null) {
                    this.response = new BaseResponse(Collections.singletonList(row));
                    return DONE;
                }
            }
        }

        return !DONE;
    }

    private boolean interceptRelationshipQuery() {

        if (query instanceof RelationshipQuery) {

            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
            if (relationshipQuery.isRefreshing()) {
                return !DONE;
            }

            ObjRelationship relationship = relationshipQuery.getRelationship(domain
                    .getEntityResolver());

            // check if we can derive target PK from FK... this implies that the
            // relationship is to-one
            if (relationship.isSourceIndependentFromTargetChange()) {
                return !DONE;
            }

            DataRow sourceRow = cache.getCachedSnapshot(relationshipQuery.getObjectId());

            if (sourceRow == null) {
                return !DONE;
            }

            // we can assume that there is one and only one DbRelationship as
            // we previously checked that
            // "!isSourceIndependentFromTargetChange"
            DbRelationship dbRelationship = (DbRelationship) relationship
                    .getDbRelationships()
                    .get(0);

            ObjectId targetId = sourceRow.createTargetObjectId(relationship
                    .getTargetEntityName(), dbRelationship);

            if (targetId == null) {
                return !DONE;
            }

            DataRow targetRow = cache.getCachedSnapshot(targetId);

            DataRow resultRow;

            if (targetRow != null) {
                resultRow = targetRow;
            }
            // if no inheritance involved, we can return a valid partial row made from
            // the target Id alone...
            else if (domain.getEntityResolver().lookupInheritanceTree(
                    (ObjEntity) relationship.getTargetEntity()) == null) {

                resultRow = new DataRow(targetId.getIdSnapshot());
            }
            else {
                // can't guess the right target...
                return !DONE;
            }

            this.response = new BaseResponse(Collections.singletonList(resultRow));
            return DONE;
        }

        return !DONE;
    }

    /*
     * Wraps execution in shared cache checks
     */
    private final boolean interceptSharedCache() {

        if (!QueryMetadata.SHARED_CACHE.equals(metadata.getCachePolicy())) {
            return !DONE;
        }

        if (query.getName() == null) {
            throw new CayenneRuntimeException(
                    "Caching of unnamed queries is not supported. Query: " + query);
        }

        if (!metadata.isRefreshingObjects()) {

            List cachedRows = cache.getCachedSnapshots(query.getName());

            if (cachedRows != null) {
                this.response = new BaseResponse();

                // decorate result immutable list to avoid messing up the cache
                response.addResultList(Collections.unmodifiableList(cachedRows));
                return DONE;
            }
        }

        runQuery();

        // TODO: Andrus, 1/22/2006 - cache QueryResponse object instead of a list!

        List list = response.firstList();
        if (list != null) {
            cache.cacheSnapshots(query.getName(), list);
        }

        return DONE;
    }

    /*
     * Gets response from the underlying DataNodes.
     */
    // the only reason why this method is non-private is to create mockup subclasses for
    // test cases that intercept this call.
    void runQuery() {

        // reset
        this.response = new BaseResponse();
        this.queriesByNode = null;
        this.queriesByExecutedQueries = null;

        // whether this is null or not will driver further decisions on how to process
        // prefetched rows
        this.prefetchResultsByPath = metadata.getPrefetchTree() != null
                && !metadata.isFetchingDataRows() ? new HashMap() : null;

        // categorize queries by node and by "executable" query...
        query.route(this, domain.getEntityResolver(), null);

        // run categorized queries
        if (queriesByNode != null) {
            Iterator nodeIt = queriesByNode.entrySet().iterator();
            while (nodeIt.hasNext()) {
                Map.Entry entry = (Map.Entry) nodeIt.next();
                QueryEngine nextNode = (QueryEngine) entry.getKey();
                Collection nodeQueries = (Collection) entry.getValue();
                nextNode.performQueries(nodeQueries, this);
            }
        }
    }

    private void interceptObjectConversion() {

        if (context != null && !metadata.isFetchingDataRows()) {

            List mainRows = response.firstList();
            if (mainRows != null && !mainRows.isEmpty()) {

                List objects;
                ObjEntity entity = metadata.getObjEntity();
                PrefetchTreeNode prefetchTree = metadata.getPrefetchTree();

                // take a shortcut when no prefetches exist...
                if (prefetchTree == null) {
                    objects = new ObjectResolver(context, entity, metadata
                            .isRefreshingObjects(), metadata.isResolvingInherited())
                            .synchronizedObjectsFromDataRows(mainRows);
                }
                else {

                    ObjectTreeResolver resolver = new ObjectTreeResolver(
                            context,
                            metadata);
                    objects = resolver.resolveObjectTree(
                            prefetchTree,
                            mainRows,
                            prefetchResultsByPath);
                }

                response.replaceResult(mainRows, objects);
            }
        }
    }

    public void route(QueryEngine engine, Query query, Query substitutedQuery) {

        List queries = null;
        if (queriesByNode == null) {
            queriesByNode = new HashMap();
        }
        else {
            queries = (List) queriesByNode.get(engine);
        }

        if (queries == null) {
            queries = new ArrayList(5);
            queriesByNode.put(engine, queries);
        }

        queries.add(query);

        // handle case when routing resuled in an "exectable" query different from the
        // original query.
        if (substitutedQuery != null && substitutedQuery != query) {

            if (queriesByExecutedQueries == null) {
                queriesByExecutedQueries = new HashMap();
            }

            queriesByExecutedQueries.put(query, substitutedQuery);
        }
    }

    public QueryEngine engineForDataMap(DataMap map) {
        if (map == null) {
            throw new NullPointerException("Null DataMap, can't determine DataNode.");
        }

        QueryEngine node = domain.lookupDataNode(map);

        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap " + map);
        }

        return node;
    }

    public void nextCount(Query query, int resultCount) {
        response.addUpdateCount(resultCount);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        response.addBatchUpdateCount(resultCount);
    }

    public void nextDataRows(Query query, List dataRows) {

        // exclude prefetched rows in the main result
        if (prefetchResultsByPath != null && query instanceof PrefetchSelectQuery) {
            PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) query;
            prefetchResultsByPath.put(prefetchQuery.getPrefetchPath(), dataRows);
        }
        else {
            response.addResultList(dataRows);
        }
    }

    public void nextDataRows(Query q, ResultIterator it) {
        throw new CayenneRuntimeException("Invalid attempt to fetch a cursor.");
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        if (keysIterator != null) {
            try {
                nextDataRows(query, keysIterator.dataRows(true));
            }
            catch (CayenneException ex) {
                // don't throw here....
                nextQueryException(query, ex);
            }
        }
    }

    public void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    public void nextGlobalException(Exception e) {
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(e));
    }

    /**
     * @deprecated since 1.2, as corresponding interface method is deprecated too.
     */
    public Level getLoggingLevel() {
        return QueryLogger.DEFAULT_LOG_LEVEL;
    }

    public boolean isIteratedResult() {
        return false;
    }
}
