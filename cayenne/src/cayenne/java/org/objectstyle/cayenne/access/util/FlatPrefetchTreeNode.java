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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * Converts DataRows representing a cartesian product of one or more tables into a tree of
 * objects. Row keys in input DataRows must be DB path expressions rooted in the main
 * entity. Instances are not reentrant or reusable as they store intermediate processing
 * state.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
class FlatPrefetchTreeNode {

    FlatPrefetchTreeNode parent;
    ObjEntity entity;
    ObjRelationship incoming;
    Collection children;
    boolean phantom;

    SourceTargetTuple[] rowMapping;
    int rowCapacity;
    int[] idIndices;

    Map partitionedByParent;
    Map fetchedObjects;

    FlatPrefetchTreeNode() {
        Factory listFactory = new Factory() {

            public Object create() {
                return new ArrayList();
            }
        };

        this.partitionedByParent = MapUtils.lazyMap(new HashMap(), listFactory);
        this.fetchedObjects = new HashMap();
    }

    ObjEntity getEntity() {
        return entity;
    }

    void setEntity(ObjEntity entity) {
        this.entity = entity;
    }

    ObjRelationship getIncoming() {
        return incoming;
    }

    void setIncoming(ObjRelationship incoming) {
        this.incoming = incoming;
    }

    FlatPrefetchTreeNode getParent() {
        return parent;
    }

    void setParent(FlatPrefetchTreeNode parent) {
        this.parent = parent;
    }

    Collection getChildren() {
        return children;
    }

    /**
     * Returns whether this tree node is "phantom", i.e. query result is not expected to
     * provide data for it and it simply sits between two other nodes.
     */
    boolean isPhantom() {
        return phantom;
    }

    void setPhantom(boolean phantom) {
        this.phantom = phantom;
    }

    /**
     * Builds a prefetch tree for a cartesian product of joined DataRows from multiple
     * entities.
     */
    void buildTree(ObjEntity entity, Collection jointPrefetchKeys) {

        this.phantom = false;
        this.entity = entity;

        // assemble tree...
        Iterator i1 = jointPrefetchKeys.iterator();
        while (i1.hasNext()) {
            String prefetchPath = (String) i1.next();
            addChildWithPath(entity, prefetchPath);
        }

        // create descriptors of non-phantom nodes
        Closure c = new Closure() {

            public void execute(Object input) {
                FlatPrefetchTreeNode resolver = (FlatPrefetchTreeNode) input;
                if (!resolver.isPhantom()) {
                    resolver.buildRowMapping();
                }
            }
        };
        executeDepthFirst(c);
    }

    DataObject getFetchedObject(Map id) {
        return (DataObject) fetchedObjects.get(id);
    }

    void putFetchedObject(Map id, DataObject object, DataObject parent) {
        fetchedObjects.put(id, object);

        List peers = (List) partitionedByParent.get(parent);
        peers.add(object);
    }

    /**
     * Returns an ObjectId map from the flat row.
     */
    Map idFromFlatRow(DataRow flatRow) {

        // TODO: should we also check for nulls in ID (and skip such rows) - this will
        // likely be an indicator of an outer join ... and considering SQLTemplate,
        // this is reasonable to expect...

        Map id = new TreeMap();
        for (int i = 0; i < idIndices.length; i++) {
            Object value = flatRow.get(rowMapping[idIndices[i]].source);
            id.put(rowMapping[idIndices[i]].target, value);
        }

        return id;
    }

    /**
     * Returns a DataRow from the flat row.
     */
    DataRow rowFromFlatRow(DataRow flatRow) {
        DataRow row = new DataRow(rowCapacity);

        // extract subset of flat row columns, recasting to the target keys
        for (int i = 0; i < rowMapping.length; i++) {
            row.put(rowMapping[i].target, flatRow.get(rowMapping[i].source));
        }

        return row;
    }

    /**
     * Configures row metadat for this node entity.
     */
    private void buildRowMapping() {
        // TODO me.
    }

    /**
     * Returns a collection of all non-phantom nodes in the tree. Traversal starts from
     * this node, proceeding to its children in a depth-first manner.
     */
    private void executeDepthFirst(Closure closure) {

        closure.execute(this);

        if (children != null) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                FlatPrefetchTreeNode child = (FlatPrefetchTreeNode) it.next();
                child.executeDepthFirst(closure);
            }
        }
    }

    /**
     * Processes path, linking all intermediate children to each other.
     */
    private FlatPrefetchTreeNode addChildWithPath(
            ObjEntity rootEntity,
            String prefetchPath) {
        Iterator it = rootEntity.resolvePathComponents(prefetchPath);

        if (!it.hasNext()) {
            return null;
        }

        FlatPrefetchTreeNode lastChild = this;

        while (it.hasNext()) {
            ObjRelationship r = (ObjRelationship) it.next();
            lastChild = lastChild.addChild(r);
        }

        // mark last node as non-phantom
        lastChild.setPhantom(false);
        return lastChild;
    }

    /**
     * Adds a direct child to this node.
     */
    private FlatPrefetchTreeNode addChild(ObjRelationship outgoing) {
        FlatPrefetchTreeNode child = null;

        if (children == null) {
            children = new ArrayList();
        }
        else {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                FlatPrefetchTreeNode next = (FlatPrefetchTreeNode) it.next();
                if (next.getIncoming() == outgoing) {
                    child = next;
                    break;
                }
            }
        }

        if (child == null) {
            child = new FlatPrefetchTreeNode();
            child.setPhantom(true);
            child.setIncoming(outgoing);
            child.setEntity((ObjEntity) outgoing.getTargetEntity());
            child.setParent(this);
            children.add(child);
        }

        return child;
    }

    final class SourceTargetTuple {

        String source;
        String target;
    }
}