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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.access.DataContext;

/**
 * An object that encapsulates resolving of a cartesian product result set. Works together
 * with FlatPrefetchResolver tree that contains descriptors of the entities involved, and
 * resolving inntermediate results.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
final class FlatPrefetchResolver {

    FlatPrefetchTreeNode rootNode;
    List results;
    DataContext dataContext;
    boolean refresh;
    boolean resolveHierarchy;

    private DataRow currentRow;

    /**
     * Create and configure an operation.
     */
    FlatPrefetchResolver(DataContext dataContext, boolean refresh,
            boolean resolveHierarchy) {

        this.dataContext = dataContext;
        this.refresh = refresh;
        this.resolveHierarchy = resolveHierarchy;
    }

    /**
     * Main method that runs resloving of the flattened result set.
     */
    List resolveObjectTree(FlatPrefetchTreeNode rootNode, List flatRows) {
        // prepare...

        // list may shrink as a result of duplicates in flattened rows.. so don't
        // allocate too much space
        int capacity = flatRows.size();
        if (capacity > 100) {
            capacity = capacity / 2;
        }

        this.results = new ArrayList(capacity);
        this.currentRow = null;
        this.rootNode = rootNode;

        // run
        int len = flatRows.size();
        for (int i = 0; i < len; i++) {

            this.currentRow = (DataRow) flatRows.get(i);

            // pass each row through the tree, allowing nodes to extract their part and
            // establish connections ... that's sort of like an informal visitor
            // pattern...

            traverseTree(rootNode, null);
        }

        return results;
    }

    private void traverseTree(FlatPrefetchTreeNode node, DataObject parentObject) {

        // existing check maybe disabled for efficiency for the root objects that are
        // checked elsewhere..
        DataObject object = null;

        if (node.isPhantom()) {
            object = resolveRowForNode(node, parentObject);
        }

        // process children
        Collection children = node.getChildren();
        if (children != null) {

            // if this node is phantom use parentObject passed from the parent node
            DataObject parentOfChildren = (node.isPhantom()) ? parentObject : object;

            Iterator it = children.iterator();
            while (it.hasNext()) {
                FlatPrefetchTreeNode child = (FlatPrefetchTreeNode) it.next();
                traverseTree(child, parentOfChildren);
            }
        }
    }

    private DataObject resolveRowForNode(FlatPrefetchTreeNode node, DataObject parentObject) {
        // find existing object
        Map id = node.idFromFlatRow(currentRow);
        DataObject object = node.getFetchedObject(id);

        if (object != null) {
            return object;
        }

        // create using operation parameters ..
        // this should probably be optimized - DataContext.objectsFromDataRows does
        // some batching that we can do here (e.g. synchronization blocks, etc.)
        DataRow row = node.rowFromFlatRow(currentRow);
        List objects = dataContext.objectsFromDataRows(node.getEntity(), Collections
                .singletonList(row), refresh, resolveHierarchy);
        object = (DataObject) objects.get(0);

        // populate node only if this is a new object...
        node.putFetchedObject(id, object, parentObject);

        // register in results if root
        if (node == rootNode) {
            results.add(object);
        }

        return object;
    }
}