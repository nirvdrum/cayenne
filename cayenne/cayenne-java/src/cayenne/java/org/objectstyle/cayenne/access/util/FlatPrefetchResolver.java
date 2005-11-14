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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectFactory;

/**
 * Encapsulates resolving of a cartesian product result set. Uses FlatPrefetchTreeNode
 * tree to obtain each entity metadata and store intermediary results.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
final class FlatPrefetchResolver {

    ObjectFactory factory;

    /**
     * Create and configure an operation.
     */
    FlatPrefetchResolver(ObjectFactory factory) {
        this.factory = factory;
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

        List results = new ArrayList(capacity);

        // run
        int len = flatRows.size();
        for (int i = 0; i < len; i++) {

            DataRow flatRow = (DataRow) flatRows.get(i);

            // pass each row through the tree

            DataObject object = resolveRow(flatRow, rootNode, null);

            // null will be returned for duplicates...
            if (object != null) {
                results.add(object);
            }
        }

        // assemble fetched objects tree
        rootNode.connectToParents();

        return results;
    }

    /**
     * Recursively resolves a flat row by processing it for each tree node, going
     * depth-first. Returns a root object of the hierarchy, or null if this object is a
     * duplicate of a previously resolved one.
     */
    private DataObject resolveRow(
            DataRow flatRow,
            FlatPrefetchTreeNode node,
            DataObject parentObject) {

        boolean existing = true;
        DataObject object = null;

        // resolve for a given node...
        if (!node.isPhantom()) {
            // find existing object, if found skip further processing
            Map id = node.idFromFlatRow(flatRow);
            object = node.getResolved(id);

            if (object == null) {
                existing = false;
                object = objectFromFlatRow(flatRow, node);
                node.putResolved(id, object);
            }

            // categorization by parent needed even if an object is already there
            // (many-to-many case)
            node.linkToParent(object, parentObject);
        }

        // recursively resolve for children
        Collection children = node.getChildren();
        if (children != null) {

            DataObject parentOfChildren = (!node.isPhantom()) ? object : null;

            Iterator it = children.iterator();
            while (it.hasNext()) {
                FlatPrefetchTreeNode child = (FlatPrefetchTreeNode) it.next();
                resolveRow(flatRow, child, parentOfChildren);
            }
        }

        // return root object if it was newly resolved
        return (existing) ? null : object;
    }

    private DataObject objectFromFlatRow(DataRow flatRow, FlatPrefetchTreeNode node) {
        // TODO: this should be optimized - DataContext.objectsFromDataRows does
        // some batching that we should do once instead of N * M times (e.g.
        // synchronization blocks, etc.)
        DataRow row = node.rowFromFlatRow(flatRow);
        List objects = factory.objectsFromDataRows(node.getEntity(), Collections
                .singletonList(row));
        return (DataObject) objects.get(0);
    }
}