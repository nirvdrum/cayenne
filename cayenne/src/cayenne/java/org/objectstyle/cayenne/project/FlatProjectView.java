/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.project;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;

/**
 * FlatProjectView converts a project tree into a list of nodes,
 * thus flattening the tree. Normally used as a singleton.
 * 
 * @author Andrei Adamchik
 */
public class FlatProjectView {

    protected static FlatProjectView instance = new FlatProjectView();

    /** 
     * Returns a FlatProjectView singleton.
     */
    public static FlatProjectView getInstance() {
        return instance;
    }

    /**
     * Expands path array, appending a treeNode at the end.
     */
    public static Object[] buildPath(Object treeNode, Object[] parentTreeNodePath) {
        if (parentTreeNodePath == null || parentTreeNodePath.length == 0) {
            return new Object[] { treeNode };
        }

        Object[] newPath = new Object[parentTreeNodePath.length + 1];
        System.arraycopy(parentTreeNodePath, 0, newPath, 0, parentTreeNodePath.length);
        newPath[parentTreeNodePath.length] = treeNode;
        return newPath;
    }

    /**
     * Returns an object corresponding to the node represented
     * by the path. This is the last object in the path.
     */
    public static Object objectFromPath(Object[] treeNodePath) {
        if (treeNodePath == null) {
            throw new NullPointerException("Null path to validated object.");
        }

        if (treeNodePath.length == 0) {
            throw new ProjectException("Validation path is empty.");
        }

        // return last object
        return treeNodePath[treeNodePath.length - 1];
    }

    /**
     * Returns an object corresponding to the parent node 
     * of the node represented by the path. This is the object 
     * next to last object in the path.
     */
    public static Object objectParentFromPath(Object[] treeNodePath) {
        if (treeNodePath == null) {
            throw new NullPointerException("Null path to validated object.");
        }

        if (treeNodePath.length == 0) {
            throw new ProjectException("Validation path is empty.");
        }

        // return next to last object
        return (treeNodePath.length > 1) ? treeNodePath[treeNodePath.length - 2] : null;
    }


    /**
     * Returns flat project tree view.
     */
   /* public List flattenProject(Project project) {
        ArrayList nodes = new ArrayList();
        TraversalHelper helper = new TraversalHelper(nodes);
        return nodes;
    }
    */

    /**
     * Helper class that serves as project traversal helper.
     */
    class TraversalHelper implements ProjectTraversalHandler {
        protected List nodes;

        public TraversalHelper(List nodes) {
            this.nodes = nodes;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectTraversalHandler#projectNode(Object, Object)
         */
        public void projectNode(Object node, Object parent) {
            nodes.add(node);
        }

        /**
         * Always returns true for max depth traversal.
         */
        public boolean shouldReadChildren(Object parent) {
            return true;
        }
    }
}
