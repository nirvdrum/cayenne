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

package org.objectstyle.cayenne.access.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectFactory;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataContextObjectFactory;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.QualifiedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * OperationObserver that accumulates select query results provided by callback methods.
 * Later the results can be retrieved via different <code>getResults</code> methods.
 * Also supports instantiating DataObjects within a provided DataContext.
 * <p>
 * This class is used as a default OperationObserver by DataContext. Also it can serve as
 * a helper for classes that work with DataNode directly, bypassing DataContext.
 * </p>
 * <p>
 * If exceptions happen during the execution, they are immediately rethrown.
 * </p>
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class SelectObserver extends DefaultOperationObserver {

    protected Map results = new HashMap();
    protected int selectCount;

    public SelectObserver() {

    }

    /**
     * @deprecated since 1.2
     */
    public SelectObserver(Level logLevel) {
        super.setLoggingLevel(logLevel);
    }

    /**
     * Returns a count of select queries that returned results since the last time "clear"
     * was called, or since this object was created.
     */
    public int getSelectCount() {
        return selectCount;
    }

    /**
     * Returns a list of result snapshots for the specified query, or null if this query
     * has never produced any results.
     */
    public List getResults(Query q) {
        return (List) results.get(q);
    }

    /**
     * Returns query results accumulated during query execution with this object as an
     * operation observer.
     */
    public Map getResults() {
        return results;
    }

    /** Clears fetched objects stored in an internal list. */
    public void clear() {
        selectCount = 0;
        results.clear();
    }

    /**
     * Stores all objects in <code>dataRows</code> in an internal result list.
     */
    public void nextDataRows(Query query, List dataRows) {

        if (dataRows != null) {
            results.put(query, dataRows);
        }

        selectCount++;
    }

    /**
     * @since 1.1
     * @deprecated since 1.2 use ObjectFactory variety instead.
     */
    public List getResultsAsObjects(DataContext dataContext, Query rootQuery) {

        if (!(rootQuery instanceof GenericSelectQuery)) {
            throw new CayenneRuntimeException("Expected GenericSelectQuery, got: "
                    + rootQuery);
        }

        GenericSelectQuery selectQuery = (GenericSelectQuery) rootQuery;

        ObjectFactory factory = new DataContextObjectFactory(dataContext, selectQuery
                .isRefreshingObjects(), selectQuery.isResolvingInherited());
        ObjEntity rootEntity = dataContext.getEntityResolver().lookupObjEntity(rootQuery);
        return getResultsAsObjects(factory, rootEntity, selectQuery);
    }

    /**
     * Returns results for a given query object as DataObjects. <code>rootQuery</code>
     * argument is assumed to be the root query, and the rest are either independent
     * queries or queries prefetching relationships for the root query.
     * <p>
     * If no results are found, an empty immutable list is returned. Most common case for
     * this is when a delegate has blocked the query from execution.
     * </p>
     * <p>
     * Side effect of this method call is that all data rows currently stored in this
     * SelectObserver are loaded as objects to a given DataContext (thus resolving
     * prefetched to-one relationships). Any to-many relationships for the root query are
     * resolved as well.
     * </p>
     * 
     * @since 1.2
     */
    public List getResultsAsObjects(
            ObjectFactory factory,
            ObjEntity rootEntity,
            GenericSelectQuery rootQuery) {

        // sanity check
        if (rootEntity == null) {
            throw new CayenneRuntimeException(
                    "Can't instantiate DataObjects from resutls. ObjEntity is undefined for query: "
                            + rootQuery);
        }

        // prepare prefetch resolver ... it can be used in two different ways
        // depending on whether we also have joint prefetches.
        // TODO: this logic needs to be streamlined...
        PrefetchResolver tree = new PrefetchResolver();
        tree.buildTree(rootEntity, rootQuery, results);

        Collection jointPrefetches = rootQuery.getJointPrefetches();
        if (!jointPrefetches.isEmpty()) {

            // certain qualifiers conflict with joint prefetches, so we
            // might need to disable some prefetched to-many arrays from being
            // resolved. This is somewhat of a hack in search of a better solution.
            Expression qualifier = null;
            if (rootQuery instanceof QualifiedQuery) {
                qualifier = ((QualifiedQuery) rootQuery).getQualifier();
            }

            FlatPrefetchTreeNode flatPrefetchTree = new FlatPrefetchTreeNode(
                    rootEntity,
                    jointPrefetches,
                    qualifier);

            FlatPrefetchResolver flatPrefetchResolver = new FlatPrefetchResolver(factory);

            List objects = flatPrefetchResolver.resolveObjectTree(
                    flatPrefetchTree,
                    getResults(rootQuery));

            // attach normal prefetches to the list of main objects that is already
            // resolved...
            tree.resolveObjectTree(factory, objects, true);
            return objects;
        }

        return tree.resolveObjectTree(factory);
    }

    /**
     * Overrides super implementation to rethrow an exception immediately.
     */
    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    /**
     * Overrides superclass implementation to rethrow an exception immediately.
     */
    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(ex));
    }
}