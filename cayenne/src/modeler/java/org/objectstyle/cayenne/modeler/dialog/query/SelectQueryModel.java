/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.dialog.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.CayenneMapEntry;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.core.Selector;
import org.scopemvc.model.collection.ListModel;

/**
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SelectQueryModel extends QueryModel {
    public static final Selector QUALIFIER_SELECTOR = Selector.fromString("qualifier");
    public static final Selector FETCH_LIMIT_SELECTOR = Selector.fromString("fetchLimit");
    public static final Selector PAGE_SIZE_SELECTOR = Selector.fromString("pageSize");
    public static final Selector REFRESHING_OBJECTS_SELECTOR =
        Selector.fromString("refreshingObjects");
    public static final Selector FETCHING_DATA_ROWS_SELECTOR =
        Selector.fromString("fetchingDataRows");
    public static final Selector DISTINCT_SELECTOR = Selector.fromString("distinct");

    public static final Selector NAVIGATION_PATH_SELECTOR =
        Selector.fromString("navigationPath");

    public static final Selector ORDERINGS_SELECTOR = Selector.fromString("orderings");
    public static final Selector SELECTED_ORDERING_SELECTOR =
        Selector.fromString("selectedOrdering");

    public static final Selector PREFETCHES_SELECTOR = Selector.fromString("prefetches");
    public static final Selector SELECTED_PREFETCH_SELECTOR =
        Selector.fromString("selectedPrefetch");

    // navigation path from the root entity
    // used for ordering or prefetches picking
    protected Object[] navigationPath;

    // Main query panel stuff
    protected Expression qualifier;
    protected int fetchLimit;
    protected int pageSize;
    protected boolean refreshingObjects;
    protected boolean fetchingDataRows;
    protected boolean distinct;

    // prefetch related
    protected ListModel prefetches;
    protected PrefetchModel selectedPrefetch;

    // orderings-related
    protected OrderingModel selectedOrdering;
    protected ListModel orderings;

    public SelectQueryModel(Query query) {
        super(query);
    }

    protected void initWithQuery(Query query) {
        if (!(query instanceof SelectQuery)) {
            throw new IllegalArgumentException("SelectQuery is expected, got: " + query);
        }

        super.initWithQuery(query);

        SelectQuery selectQuery = (SelectQuery) query;

        this.distinct = selectQuery.isDistinct();
        this.fetchLimit = selectQuery.getFetchLimit();
        this.pageSize = selectQuery.getPageSize();
        this.refreshingObjects = selectQuery.isRefreshingObjects();
        this.fetchingDataRows = selectQuery.isFetchingDataRows();
        this.qualifier = selectQuery.getQualifier();

        initOrderings(selectQuery);
        initPrefetches(selectQuery);
    }

    protected void initOrderings(SelectQuery query) {
        List originalOrderings = query.getOrderings();
        List modelOrderings = new ArrayList(originalOrderings.size());

        Iterator it = originalOrderings.iterator();
        while (it.hasNext()) {
            modelOrderings.add(new OrderingModel((Ordering) it.next()));
        }

        setOrderings(new ListModel(modelOrderings));
    }

    protected void initPrefetches(SelectQuery query) {
        Entity root = (Entity) getRoot();
        Collection originalPrefetches = query.getPrefetches();
        List modelPrefetches = new ArrayList(originalPrefetches.size());

        Iterator it = originalPrefetches.iterator();
        while (it.hasNext()) {
            String prefetch = (String) it.next();
            modelPrefetches.add(new PrefetchModel(root, prefetch));
        }

        setPrefetches(new ListModel(modelPrefetches));
    }

    public void updateQuery() {
        SelectQuery selectQuery = (SelectQuery) query;
        selectQuery.setName(name);

        selectQuery.setQualifier(qualifier);
        selectQuery.setFetchLimit(fetchLimit);
        selectQuery.setPageSize(pageSize);
        selectQuery.setRefreshingObjects(refreshingObjects);
        selectQuery.setFetchingDataRows(fetchingDataRows);
        selectQuery.setDistinct(distinct);

        selectQuery.clearPrefetches();
        selectQuery.addPrefetches(prefetches);

        // update with submodel changes
        selectQuery.clearOrderings();
        Iterator orderingsIt = orderings.iterator();
        while (orderingsIt.hasNext()) {
            OrderingModel orderingModel = (OrderingModel) orderingsIt.next();
            selectQuery.addOrdering(orderingModel.createOrdering());
        }

        selectQuery.clearPrefetches();
        Iterator prefetchesIt = prefetches.iterator();
        while (prefetchesIt.hasNext()) {
            PrefetchModel prefetchModel = (PrefetchModel) prefetchesIt.next();
            selectQuery.addPrefetch(prefetchModel.getPath());
        }
    }

    /**
     * Helper method to create a new ordering model from the current 
     * navigation path. Returns null if there is no valid selection.
     */
    public OrderingModel createOrderingFromNavigationPath() {
        String path = navigationPathString();
        return (path != null) ? new OrderingModel(path) : null;
    }

    /**
     * Helper method to create a new ordering model from the current 
     * navigation path. Returns null if there is no valid selection.
     */
    public PrefetchModel createPrefetchFromNavigationPath() {
        String path = navigationPathString();
        return (path != null) ? new PrefetchModel((Entity) getRoot(), path) : null;
    }

    private String navigationPathString() {
        // first item in the path is Entity, so we must have
        // at least two elements to constitute a valid ordering path

        if (navigationPath == null || navigationPath.length < 2) {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        // attribute or relationships
        CayenneMapEntry first = (CayenneMapEntry) navigationPath[1];
        buffer.append(first.getName());

        for (int i = 2; i < navigationPath.length; i++) {
            CayenneMapEntry pathEntry = (CayenneMapEntry) navigationPath[i];
            buffer.append(".").append(pathEntry.getName());
        }

        return buffer.toString();
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        if (this.distinct != distinct) {
            this.distinct = distinct;
            fireModelChange(VALUE_CHANGED, DISTINCT_SELECTOR);
        }
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    public void setFetchingDataRows(boolean fetchingDataRows) {
        if (this.fetchingDataRows != fetchingDataRows) {
            this.fetchingDataRows = fetchingDataRows;
            fireModelChange(VALUE_CHANGED, FETCHING_DATA_ROWS_SELECTOR);
        }
    }

    public int getFetchLimit() {
        return fetchLimit;
    }

    public void setFetchLimit(int fetchLimit) {
        if (this.fetchLimit != fetchLimit) {
            this.fetchLimit = fetchLimit;
            fireModelChange(VALUE_CHANGED, FETCH_LIMIT_SELECTOR);
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if (this.pageSize != pageSize) {
            this.pageSize = pageSize;
            fireModelChange(VALUE_CHANGED, PAGE_SIZE_SELECTOR);
        }
    }

    public Expression getQualifier() {
        return qualifier;
    }

    public void setQualifier(Expression qualifier) {
        if (!Util.nullSafeEquals(qualifier, this.qualifier)) {
            this.qualifier = qualifier;
            fireModelChange(VALUE_CHANGED, QUALIFIER_SELECTOR);
        }
    }

    public boolean isRefreshingObjects() {
        return refreshingObjects;
    }

    public void setRefreshingObjects(boolean refreshingObjects) {
        if (this.refreshingObjects != refreshingObjects) {
            this.refreshingObjects = refreshingObjects;
            fireModelChange(VALUE_CHANGED, REFRESHING_OBJECTS_SELECTOR);
        }
    }

    public ListModel getOrderings() {
        return orderings;
    }

    public void setOrderings(ListModel orderings) {
        if (this.orderings != orderings) {
            unlistenOldSubmodel(ORDERINGS_SELECTOR);
            this.orderings = orderings;
            listenNewSubmodel(ORDERINGS_SELECTOR);
            fireModelChange(VALUE_CHANGED, ORDERINGS_SELECTOR);
        }
    }

    public ListModel getPrefetches() {
        return prefetches;
    }

    public void setPrefetches(ListModel prefetches) {
        if (this.prefetches != prefetches) {
            unlistenOldSubmodel(PREFETCHES_SELECTOR);
            this.prefetches = prefetches;
            listenNewSubmodel(PREFETCHES_SELECTOR);
            fireModelChange(VALUE_CHANGED, PREFETCHES_SELECTOR);
        }
    }

    public Object[] getNavigationPath() {
        return navigationPath;
    }

    public void setNavigationPath(Object[] navigationPath) {
        this.navigationPath = navigationPath;
        // don't fire an event - navigation path only concerns
        // this instance.... 
    }

    public OrderingModel getSelectedOrdering() {
        return selectedOrdering;
    }

    public void setSelectedOrdering(OrderingModel selectedOrdering) {
        if (this.selectedOrdering != selectedOrdering) {
            this.selectedOrdering = selectedOrdering;
            fireModelChange(VALUE_CHANGED, SELECTED_ORDERING_SELECTOR);
        }
    }

    public PrefetchModel getSelectedPrefetch() {
        return selectedPrefetch;
    }

    public void setSelectedPrefetch(PrefetchModel selectedPrefetch) {
        if (this.selectedPrefetch != selectedPrefetch) {
            this.selectedPrefetch = selectedPrefetch;
            fireModelChange(VALUE_CHANGED, SELECTED_PREFETCH_SELECTOR);
        }
    }
}
