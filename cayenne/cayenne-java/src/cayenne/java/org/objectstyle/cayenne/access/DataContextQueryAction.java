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
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.BaseResponse;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.RelationshipQuery;
import org.objectstyle.cayenne.query.SingleObjectQuery;

/**
 * A DataContext helper that handles query execution.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataContextQueryAction {

    static final boolean DONE = true;

    ObjectContext queryContext;
    DataContext context;
    Query query;
    QueryMetadata metadata;

    QueryResponse response;

    DataContextQueryAction(ObjectContext queryContext, DataContext context, Query query) {
        this.context = context;
        this.queryContext = queryContext != context ? queryContext : null;
        this.query = query;
        this.metadata = query.getMetaData(context.getEntityResolver());
    }

    /**
     * Selects an appropriate select execution strategy and runs the query.
     */
    QueryResponse execute() {

        if (interceptPaginatedQuery() != DONE) {
            if (interceptOIDQuery() != DONE) {
                if (interceptRelationshipQuery() != DONE) {
                    if (interceptLocalCache() != DONE) {
                        runQuery();
                    }
                }
            }
        }

        interceptObjectConversion();

        return response;
    }

    private void interceptObjectConversion() {

        if (queryContext != null && !metadata.isFetchingDataRows()) {

            // rewrite response to contain objects from the query context

            BaseResponse childResponse = new BaseResponse();

            for (response.reset(); response.next();) {
                if (response.isList()) {

                    List objects = response.currentList();
                    if (objects.isEmpty()) {
                        childResponse.addResultList(objects);
                    }
                    else {

                        // TODO: Andrus 1/31/2006 - InrementalFaultList is not properly
                        // transferred between contexts....

                        List childObjects = new ArrayList(objects.size());
                        Iterator it = objects.iterator();
                        while (it.hasNext()) {
                            Persistent object = (Persistent) it.next();
                            childObjects.add(queryContext.localObject(object
                                    .getObjectId(), object));
                        }

                        childResponse.addResultList(childObjects);
                    }
                }
                else {
                    childResponse.addBatchUpdateCount(response.currentUpdateCount());
                }
            }

            response = childResponse;
        }

    }

    private boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            response = new BaseResponse(new IncrementalFaultList(context, query));
            return DONE;
        }

        return !DONE;
    }

    private boolean interceptOIDQuery() {
        if (query instanceof SingleObjectQuery) {
            SingleObjectQuery oidQuery = (SingleObjectQuery) query;
            if (!oidQuery.isRefreshing()) {
                Object object = context.getGraphManager().getNode(oidQuery.getObjectId());
                if (object != null) {
                    List result = new ArrayList(1);
                    result.add(object);
                    this.response = new BaseResponse(result);
                    return DONE;
                }
            }
        }

        return !DONE;
    }

    private boolean interceptRelationshipQuery() {

        if (query instanceof RelationshipQuery) {
            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
            if (!relationshipQuery.isRefreshing()) {

                // don't intercept to-many relationships if fetch is done to the same
                // context as the root context of this action - this will result in an
                // infinite loop.

                if (queryContext == null
                        && relationshipQuery
                                .getRelationship(context.getEntityResolver())
                                .isToMany()) {
                    return !DONE;
                }

                ObjectId id = relationshipQuery.getObjectId();

                DataObject object = (DataObject) context.getGraphManager().getNode(id);

                if (object != null) {

                    // can't do 'readProperty' (or use ClassDescriptor at this point) as
                    // this will result in an infinite loop...
                    Object related = object.readPropertyDirectly(relationshipQuery
                            .getRelationshipName());

                    if (!(related instanceof Fault)) {
                        List result;

                        // null to-one
                        if (related == null) {
                            result = new ArrayList(1);
                        }
                        // to-many
                        else if (related instanceof List) {
                            result = (List) related;
                        }
                        // non-null to-one
                        else {
                            result = new ArrayList(1);
                            result.add(related);
                        }

                        this.response = new BaseResponse(result);

                        return DONE;
                    }
                }
            }
        }

        return !DONE;
    }

    /*
     * Wraps execution in local cache checks.
     */
    private boolean interceptLocalCache() {
        if (!QueryMetadata.LOCAL_CACHE.equals(metadata.getCachePolicy())) {
            return !DONE;
        }

        if (query.getName() == null) {
            throw new CayenneRuntimeException(
                    "Caching of unnamed queries is not supported.");
        }

        if (!metadata.isRefreshingObjects()) {
            List cachedResults = context.getObjectStore().getCachedQueryResult(
                    query.getName());
            if (cachedResults != null) {
                response = new BaseResponse(cachedResults);
                return DONE;
            }
        }

        runQuery();
        context.getObjectStore().cacheQueryResult(query.getName(), response.firstList());
        return DONE;
    }

    /*
     * Fetches data from the channel.
     */
    private void runQuery() {
        this.response = context.getChannel().onQuery(context, query);
    }
}