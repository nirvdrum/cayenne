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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * A tree structure that allows to resolve prefetch query dependencies and build an object
 * tree out of multiple query results.
 * 
 * @since 1.2 moved from the parent class to a standalone class.
 * @author Andrei Adamchik
 */
class PrefetchResolver {

    private static final Logger logObj = Logger.getLogger(PrefetchResolver.class);

    List dataRows;
    ObjEntity entity;
    ObjRelationship incoming;
    Map children;

    ObjRelationship getIncoming() {
        return incoming;
    }

    void setIncoming(ObjRelationship incoming) {
        this.incoming = incoming;
    }

    /**
     * Initializes a prefetch tree for the map of query results.
     */
    void buildTree(ObjEntity entity, Query rootQuery, Map resultsByQuery) {
        this.entity = entity;

        // add children
        Iterator it = resultsByQuery.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            Query query = (Query) entry.getKey();
            List dataRows = (List) entry.getValue();

            if (dataRows == null) {
                logObj.warn("Can't find prefetch results for query: " + query);
                continue;

                // ignore null result (this shouldn't happen), however do not ignore
                // empty result, since it should be used to
                // update the source objects...
            }

            if (rootQuery == query) {
                this.dataRows = dataRows;
                continue;
            }

            // add prefetch queries to the tree
            if (query instanceof PrefetchSelectQuery) {
                PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) query;

                if (prefetchQuery.getParentQuery() == rootQuery) {
                    addChildWithPath(prefetchQuery.getPrefetchPath(), dataRows);
                }
            }
        }
    }

    /**
     * Adds a (possibly indirect) child to this node.
     */
    PrefetchResolver addChildWithPath(String prefetchPath, List dataRows) {
        Iterator it = entity.resolvePathComponents(prefetchPath);

        if (!it.hasNext()) {
            return null;
        }

        PrefetchResolver lastChild = this;

        while (it.hasNext()) {
            ObjRelationship r = (ObjRelationship) it.next();
            lastChild = lastChild.addChild(r);
        }

        lastChild.dataRows = dataRows;
        return lastChild;
    }

    /**
     * Adds a direct child to this node.
     */
    PrefetchResolver addChild(ObjRelationship outgoing) {
        PrefetchResolver child = null;

        if (children == null) {
            children = new LinkedMap();
        }
        else {
            child = (PrefetchResolver) children.get(outgoing.getName());
        }

        if (child == null) {
            child = new PrefetchResolver();
            child.setIncoming(outgoing);
            children.put(outgoing.getName(), child);
        }

        return child;
    }

    /**
     * Recursively resolves a hierarchy of prefetched data rows.
     */
    List resolveObjectTree(
            DataContext dataContext,
            boolean refresh,
            boolean resolveHierarchy) {

        // resolve the tree recursively...
        List objects = resolveObjectTree(dataContext,
                refresh,
                resolveHierarchy,
                null,
                false);
        return (objects != null) ? objects : new ArrayList(1);
    }

    /**
     * Recursively resolves a hierarchy of prefetched data rows starting from a child tree
     * node. Allows to skip the resolution of this node and only do children. This is
     * useful in chaining various prefetch resolving strategies.
     */
    List resolveObjectTree(
            DataContext dataContext,
            boolean refresh,
            boolean resolveHierarchy,
            List parentObjects,
            boolean skipSelf) {

        List objects = null;

        // skip most operations on a "phantom" node that had no prefetch query
        if (!skipSelf && dataRows != null) {
            ObjEntity entity = (incoming != null) ? (ObjEntity) incoming
                    .getTargetEntity() : this.entity;

            // resolve objects;
            objects = dataContext.objectsFromDataRows(entity,
                    dataRows,
                    refresh,
                    resolveHierarchy);

            // connect to parent - to-many only, as to-one are connected by Cayenne
            // automatically.
            if (incoming != null && incoming.isToMany()) {

                Map partitioned = partitionBySource(objects);

                // depending on whether parent is a "phantom" node,
                // use different strategy

                if (parentObjects != null && parentObjects.size() > 0) {
                    connectToNodeParents(parentObjects, partitioned);
                }
                else {
                    connectToFaultedParents(partitioned);
                }
            }
        }

        //  resolve children
        if (children != null) {
            Iterator it = children.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                PrefetchResolver node = (PrefetchResolver) entry.getValue();
                node.resolveObjectTree(dataContext,
                        refresh,
                        resolveHierarchy,
                        objects,
                        false);
            }
        }

        return objects;
    }

    void connectToNodeParents(List parentObjects, Map partitioned) {

        // destinationObjects has now been partitioned into a list per
        // source object... Now init their "toMany"

        Iterator it = parentObjects.iterator();
        while (it.hasNext()) {
            DataObject root = (DataObject) it.next();
            List related = (List) partitioned.get(root);

            if (related == null) {
                related = new ArrayList(1);
            }

            ToManyList toManyList = (ToManyList) root.readProperty(incoming.getName());
            toManyList.setObjectList(related);
        }
    }

    void connectToFaultedParents(Map partitioned) {
        Iterator it = partitioned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            DataObject root = (DataObject) entry.getKey();
            List related = (List) entry.getValue();

            ToManyList toManyList = (ToManyList) root.readProperty(incoming.getName());

            // TODO: if a list is modified, should we
            // merge to-many instead of simply overwriting it?
            toManyList.setObjectList(related);
        }
    }

    /**
     * Organizes this node objects in a map keyed by the source related object for the
     * node "incoming" relationship.
     */
    Map partitionBySource(List objects) {
        Class sourceObjectClass = ((ObjEntity) incoming.getSourceEntity())
                .getJavaClass(Configuration.getResourceLoader());
        ObjRelationship reverseRelationship = incoming.getReverseRelationship();

        // Might be used later on... obtain and cast only once
        DbRelationship dbRelationship = (DbRelationship) incoming
                .getDbRelationships()
                .get(0);

        Factory listFactory = new Factory() {

            public Object create() {
                return new ArrayList();
            }
        };

        Map toManyLists = MapUtils.lazyMap(new HashMap(), listFactory);
        Iterator destIterator = objects.iterator();
        while (destIterator.hasNext()) {
            DataObject destinationObject = (DataObject) destIterator.next();
            DataObject sourceObject = null;
            if (reverseRelationship != null) {
                sourceObject = (DataObject) destinationObject
                        .readProperty(reverseRelationship.getName());
            }
            else {
                // Reverse relationship doesn't exist... match objects manually
                DataContext context = destinationObject.getDataContext();
                ObjectStore objectStore = context.getObjectStore();

                Map sourcePk = dbRelationship.srcPkSnapshotWithTargetSnapshot(objectStore
                        .getSnapshot(destinationObject.getObjectId(), context));

                // if object does not exist yet, don't create it
                // the reason for its absense is likely due to the absent intermediate
                // prefetch
                sourceObject = objectStore.getObject(new ObjectId(
                        sourceObjectClass,
                        sourcePk));
            }

            // don't attach to hollow objects
            if (sourceObject != null
                    && sourceObject.getPersistenceState() != PersistenceState.HOLLOW) {
                List relatedObjects = (List) toManyLists.get(sourceObject);
                relatedObjects.add(destinationObject);
            }
        }

        return toManyLists;
    }
}