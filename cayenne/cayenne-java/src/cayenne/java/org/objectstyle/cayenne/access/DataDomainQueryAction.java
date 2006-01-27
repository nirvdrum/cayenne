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
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.query.RelationshipQuery;
import org.objectstyle.cayenne.query.SingleObjectQuery;

/**
 * Performs query routing and execution. During execution phase intercepts callbacks to
 * the OperationObserver, remapping results to the original pre-routed queries.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainQueryAction implements QueryRouter, OperationObserver {

    static final boolean DONE = true;

    DataDomain domain;
    OperationObserver callback;
    Query query;
    QueryMetadata metadata;
    QueryResponse result;

    Map queriesByNode;
    Map queriesByExecutedQueries;

    /*
     * A constructor for the "new" way of performing a query via 'execute' with
     * QueryResponse created internally.
     */
    DataDomainQueryAction(DataDomain domain, Query query) {
        this.domain = domain;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());
    }

    /*
     * A constructor for the "old" way of performing a query via performQuery() with
     * outside callback. Still needed for cursor results and such.
     */
    DataDomainQueryAction(DataDomain domain, Query query, OperationObserver callback) {
        this(domain, query);
        this.callback = callback;
    }

    QueryResponse execute() {

        if (domain.isSharedCacheEnabled()) {
            // intercept object and relationship queries that can be served from cache.
            if (interceptOIDQuery() == DONE) {
                return this.result;
            }

            if (interceptRelationshipQuery() == DONE) {
                return this.result;
            }

            // check cache BEFORE routing .. we may need to change that in the future
            if (QueryMetadata.SHARED_CACHE.equals(metadata.getCachePolicy())) {
                return getResponseViaCache();
            }
        }

        return getResponse();
    }

    private boolean interceptOIDQuery() {
        if (query instanceof SingleObjectQuery) {
            SingleObjectQuery oidQuery = (SingleObjectQuery) query;
            if (!oidQuery.isRefreshing()) {
                DataRow row = domain.getSharedSnapshotCache().getCachedSnapshot(
                        oidQuery.getObjectId());
                if (row != null) {
                    QueryResult result = new QueryResult();
                    result.nextDataRows(query, Collections.singletonList(row));
                    this.result = result;
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

            // target may be a subclass of a given entity, so we can't guess it here.
            if (domain.getEntityResolver().lookupInheritanceTree(
                    (ObjEntity) relationship.getTargetEntity()) != null) {
                return !DONE;
            }

            DataRow sourceRow = domain.getSharedSnapshotCache().getCachedSnapshot(
                    relationshipQuery.getObjectId());

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

            DataRow targetRow = domain.getSharedSnapshotCache().getCachedSnapshot(
                    targetId);

            // if we have a target object in the cache - return it, if not - return
            // partial snapshot that would allow callers to create a HOLLOW object.
            DataRow resultRow = targetRow != null ? targetRow : new DataRow(targetId
                    .getIdSnapshot());

            QueryResult result = new QueryResult();
            result.nextDataRows(query, Collections.singletonList(resultRow));
            this.result = result;
            return DONE;
        }

        return !DONE;
    }

    /*
     * Wraps execution in shared cache checks
     */
    private final QueryResponse getResponseViaCache() {

        if (query.getName() == null) {
            throw new CayenneRuntimeException(
                    "Caching of unnamed queries is not supported.");
        }

        DataRowStore cache = domain.getSharedSnapshotCache();

        if (!metadata.isRefreshingObjects()) {

            List cachedRows = cache.getCachedSnapshots(query.getName());

            if (cachedRows != null) {
                QueryResult cachedResult = new QueryResult();

                if (!cachedRows.isEmpty()) {
                    // decorate result immutable list to avoid messing up the cache
                    cachedResult.nextDataRows(query, Collections
                            .unmodifiableList(cachedRows));
                }

                return cachedResult;
            }
        }

        QueryResponse response = getResponse();

        // TODO: Andrus, 1/22/2006 - cache QueryResponse object instead of a list!
        cache.cacheSnapshots(query.getName(), response.getFirstRows(query));
        return response;
    }

    /*
     * Gets response from the underlying DataNodes.
     */
    private final QueryResponse getResponse() {
        // sanity check
        // TODO: Andrus, 1/22/2006 - we need to reconcile somehow external
        // and internal callback strategies...E.g. by using a callback wrapper...
        if (callback != null) {
            throw new CayenneRuntimeException(
                    "Invalid state, can't run this query with external callback set.");
        }

        QueryResult response = new QueryResult();
        this.callback = response;

        performQuery();
        return response;
    }

    void performQuery() {

        // reset
        queriesByNode = null;
        queriesByExecutedQueries = null;

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
        callback.nextCount(queryForExecutedQuery(query), resultCount);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        callback.nextBatchCount(queryForExecutedQuery(query), resultCount);
    }

    public void nextDataRows(Query query, List dataRows) {
        callback.nextDataRows(queryForExecutedQuery(query), dataRows);
    }

    public void nextDataRows(Query q, ResultIterator it) {
        callback.nextDataRows(queryForExecutedQuery(q), it);
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        callback.nextGeneratedDataRows(queryForExecutedQuery(query), keysIterator);
    }

    public void nextQueryException(Query query, Exception ex) {
        callback.nextQueryException(queryForExecutedQuery(query), ex);
    }

    public void nextGlobalException(Exception e) {
        callback.nextGlobalException(e);
    }

    /**
     * @deprecated since 1.2, as corresponding interface method is deprecated too.
     */
    public Level getLoggingLevel() {
        return callback.getLoggingLevel();
    }

    public boolean isIteratedResult() {
        return callback.isIteratedResult();
    }

    Query queryForExecutedQuery(Query executedQuery) {
        Query q = null;

        if (queriesByExecutedQueries != null) {
            q = (Query) queriesByExecutedQueries.get(executedQuery);
        }

        return q != null ? q : executedQuery;
    }
}
