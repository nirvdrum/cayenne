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

package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/** 
 * OperationObserver that accumulates select query results provided 
 * by callback methods. Later the results can be retrieved
 * via different <code>getResults</code> methods. Also supports instantiating
 * DataObjects within a provided DataContext.
 * 
 * <p>Thsi class is used as a default OperationObserver by DataContext.
 * Also it can serve as a helper for classes that work with 
 * DataNode directly, bypassing DataContext.
 * </p>
 * 
 * <p>If exceptions happen during the execution, they are immediately rethrown.
 * </p>
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 *  @author Andrei Adamchik
 */
public class SelectObserver extends DefaultOperationObserver {
    private static Logger logObj = Logger.getLogger(SelectObserver.class);

    protected Map results = new HashMap();
    protected int selectCount;

    public SelectObserver() {
        this(QueryLogger.DEFAULT_LOG_LEVEL);
    }

    public SelectObserver(Level logLevel) {
        super.setLoggingLevel(logLevel);
    }

    /** 
     * Returns a count of select queries that returned results
     * since the last time "clear" was called, or since this object
     * was created.
     */
    public int getSelectCount() {
        return selectCount;
    }

    /** 
     * Returns a list of result snapshots for the specified query,
     * or null if this query has never produced any results.
     */
    public List getResults(Query q) {
        return (List) results.get(q);
    }

    /** 
     * Returns query results accumulated during query execution with this
     * object as an operation observer. 
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
     * Stores all objects in <code>dataRows</code> in an internal
     * result list. 
     */
    public void nextDataRows(Query query, List dataRows) {

        super.nextDataRows(query, dataRows);
        if (dataRows != null) {
            results.put(query, dataRows);
        }

        selectCount++;
    }

    /** 
      * Returns results for a given query object as DataObjects. <code>rootQuery</code> argument
      * is assumed to be the root query, and the rest are either independent queries or queries
      * prefetching relationships for the root query. 
      * 
      * <p>If no results are found, an empty immutable list is returned. Most common case for this
      * is when a delegate has blocked the query from execution.
      * </p>
      * 
      * <p>Side effect of this method call is that all data rows currently stored in this
      * SelectObserver are loaded as objects to a given DataContext (thus resolving
      * prefetched to-one relationships). Any to-many relationships for the root query
      * are resolved as well.</p>
      * 
      * @since 1.1
      */
    public List getResultsAsObjects(DataContext dataContext, Query rootQuery) {
        List dataRows = getResults(rootQuery);

        if (dataRows == null) {
            return Collections.EMPTY_LIST;
        }

        ObjEntity entity = dataContext.getEntityResolver().lookupObjEntity(rootQuery);
        boolean refresh =
            (rootQuery instanceof GenericSelectQuery)
                ? ((GenericSelectQuery) rootQuery).isRefreshingObjects()
                : true;
        List objects = dataContext.objectsFromDataRows(entity, dataRows, refresh);

        // handle prefetches for this query results
        Iterator queries = results.keySet().iterator();
        while (queries.hasNext()) {
            Query nextQuery = (Query) queries.next();

            if (rootQuery == nextQuery) {
                continue;
            }

            List nextDataRows = getResults(nextQuery);
            if (nextDataRows == null) {
                throw new CayenneRuntimeException(
                    "Can't find results for query: " + nextQuery);
            }

            ObjEntity nextEntity =
                dataContext.getEntityResolver().lookupObjEntity(nextQuery);

            // TODO: how should we handle refreshing of prefetched objects???
            // should we propagate the setting from parent query?
            List nextObjects =
                dataContext.objectsFromDataRows(nextEntity, nextDataRows, refresh);

            // now deal with to-many prefetching
            if (!(nextQuery instanceof PrefetchSelectQuery)) {
                continue;
            }

            PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) nextQuery;
            if (prefetchQuery.getRootQuery() != rootQuery) {
                continue;
            }

            ObjRelationship relationship =
                prefetchQuery.getSingleStepToManyRelationship();

            if (relationship == null) {
                continue;
            }

            mergePrefetchResultsRelationships(objects, relationship, nextObjects);
        }

        return objects;
    }

    /** 
     * Overrides super implementation to rethrow an exception immediately. 
     */
    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    /** 
     * Overrides superclass implementation to rethrow an exception
     * immediately. 
     */
    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(ex));
    }

    /**
     * Takes a list of "root" (or "source") objects,
     * a list of destination objects, and the relationship which relates them
     * (from root to destination).  It then merges the destination objects
     * into the toMany relationships of the relevant root objects, thus clearing
     * the toMany fault.  This method is typically only used internally by Cayenne
     * and is not intended for client use.
     * @param rootObjects
     * @param theRelationship
     * @param destinationObjects
     */
    static void mergePrefetchResultsRelationships(
        List rootObjects,
        ObjRelationship relationship,
        List destinationObjects) {

        if (rootObjects.size() == 0) {
            // nothing to do
            return;
        }

        Class sourceObjectClass = ((DataObject) rootObjects.get(0)).getClass();
        ObjRelationship reverseRelationship = relationship.getReverseRelationship();
        
        // Might be used later on... obtain and cast only once
        DbRelationship dbRelationship =
            (DbRelationship) relationship.getDbRelationships().get(0);

        Factory listFactory = new Factory() {
            public Object create() {
                return new ArrayList();
            }
        };

        Map toManyLists = MapUtils.lazyMap(new HashMap(), listFactory);

        Iterator destIterator = destinationObjects.iterator();
        while (destIterator.hasNext()) {
            DataObject thisDestinationObject = (DataObject) destIterator.next();
            DataObject sourceObject = null;
            if (reverseRelationship != null) {
                sourceObject =
                    (DataObject) thisDestinationObject.readNestedProperty(
                        reverseRelationship.getName());
            }
            else {
                // Reverse relationship doesn't exist... match objects manually
                DataContext context = thisDestinationObject.getDataContext();
                Map sourcePk =
                    dbRelationship.srcPkSnapshotWithTargetSnapshot(
                        context.getObjectStore().getSnapshot(
                            thisDestinationObject.getObjectId(),
                            context));
                sourceObject =
                    context.registeredObject(new ObjectId(sourceObjectClass, sourcePk));
            }

            if (sourceObject != null) {
                List relatedObjects = (List) toManyLists.get(sourceObject);
                relatedObjects.add(thisDestinationObject);
            }
        }

        // destinationObjects has now been partitioned into a list per
        // source object... Iterate over the source objects and fix up 
        // the relationship on each.
        Iterator rootIterator = rootObjects.iterator();
        while (rootIterator.hasNext()) {
            DataObject thisRoot = (DataObject) rootIterator.next();
            ToManyList toManyList =
                (ToManyList) thisRoot.readNestedProperty(relationship.getName());

            toManyList.setObjectList((List) toManyLists.get(thisRoot));
        }
    }
}