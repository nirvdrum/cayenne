/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.project.ProjectTraversal;
import org.objectstyle.cayenne.project.ProjectTraversalHandler;

/**
 * ProjectTreeModel is a model of Cayenne project tree.
 * 
 * @author Andrei Adamchik
 */
public class ProjectTreeModel extends DefaultTreeModel {
    private static Logger logObj = Logger.getLogger(ProjectTreeModel.class);

    /**
     * Creates a tree of Swing TreeNodes wrapping Cayenne project.
     * Returns the root node of the tree.
     */
    public static DefaultMutableTreeNode wrapProject(Project project) {
        return wrapProjectNode(project);
    }

    /**
     * Creates a tree of Swing TreeNodes wrapping Cayenne project object.
     * Returns the root node of the tree.
     */
    public static DefaultMutableTreeNode wrapProjectNode(Object node) {
        TraversalHelper helper = new TraversalHelper();
        new ProjectTraversal(helper).traverse(node);
        return helper.getStartNode();
    }

    /**
       * Creates a tree of Swing TreeNodes wrapping Cayenne project object.
       * Returns the root node of the tree.
       */
    public static DefaultMutableTreeNode wrapProjectNode(
        Object node,
        DefaultMutableTreeNode parentPath) {

        TraversalHelper helper = new TraversalHelper();

        // build a project path from tree node
        ProjectPath path = new ProjectPath();
        if (parentPath != null) {
            path = helper.registerNodes(parentPath.getPath());
        }

        new ProjectTraversal(helper).traverse(node, path);
        return helper.getStartNode();
    }

    /**
     * Constructor for ProjectTreeModel.
     * @param root
     */
    public ProjectTreeModel(Project project) {
        super(wrapProject(project));
    }

    /**
     * Fixes ordering of nodes.
     */
    public void reorder() {
        CopyTraversalHelper helper = new CopyTraversalHelper();
        new ProjectTraversal(helper).traverse(
            getRootNode().getUserObject(),
            new ProjectPath());
        setRoot(helper.getStartNode());
    }

    /**
     * Returns root node cast into DefaultMutableTreeNode.
     */
    public DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) super.getRoot();
    }

    /** 
     * Wraps an object into a tree node and inserts it as one 
     * of the children of supplied node.
     */
    public DefaultMutableTreeNode insertObject(
        Object obj,
        DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode node = wrapProjectNode(obj, parent);
        super.insertNodeInto(node, parent, parent.getChildCount());
        return node;
    }

    public DefaultMutableTreeNode getNodeForObjectPath(Object[] path) {
        if (path == null || path.length == 0) {
            return null;
        }

        DefaultMutableTreeNode currentNode = getRootNode();

        // adjust for root node being in the path
        int start = 0;
        if (currentNode.getUserObject() == path[0]) {
            start = 1;
        }

        for (int i = start; i < path.length; i++) {
            DefaultMutableTreeNode foundNode = null;
            Enumeration children = currentNode.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) children.nextElement();
                if (child.getUserObject() == path[i]) {
                    foundNode = child;
                    break;
                }
            }

            if (foundNode == null) {
                return null;
            }
            else {
                currentNode = foundNode;
            }
        }

        return currentNode;
    }

    static class TraversalHelper implements ProjectTraversalHandler {
        protected DefaultMutableTreeNode startNode;
        protected Map nodesMap;

        public TraversalHelper() {
            this.nodesMap = new HashMap();
        }

        public DefaultMutableTreeNode getStartNode() {
            return startNode;
        }

        /**
         * Creates a starting point for tree traversal.
         */
        public ProjectPath registerNodes(TreeNode[] nodes) {
            ProjectPath path = new ProjectPath();

            for (int i = 0; i < nodes.length; i++) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) nodes[i];

                path = path.appendToPath(treeNode.getUserObject());

                // register node with helper
                registerNode(treeNode);
            }

            return path;
        }

        public void registerNode(DefaultMutableTreeNode node) {
            nodesMap.put(node.getUserObject(), node);
        }

        public void projectNode(ProjectPath nodePath) {
            if (ProjectTreeModel.logObj.isDebugEnabled()) {
                logObj.debug("Read node: " + nodePath);
            }

            Object parent = nodePath.getObjectParent();
            Object nodeObj = nodePath.getObject();
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeObj);

            if (startNode == null) {
                startNode = node;
            }
            else {
                DefaultMutableTreeNode nodeParent =
                    (DefaultMutableTreeNode) nodesMap.get(parent);
                nodeParent.add(node);
            }

            registerNode(node);
        }

        public boolean shouldReadChildren(Object node, ProjectPath parentPath) {
            // do not read deatils of linked maps
            if ((node instanceof DataMap)
                && parentPath != null
                && (parentPath.getObject() instanceof DataNode)) {
                return false;
            }

            return (node instanceof Project)
                || (node instanceof DataDomain)
                || (node instanceof DataMap)
                || (node instanceof DataNode);
        }
    }

    /**
     * Traversal hanlder that rebuilds the tree from another tree.
     * Used to reorder tree nodes.
     */
    class CopyTraversalHelper extends TraversalHelper {

        public void projectNode(ProjectPath nodePath) {
            DefaultMutableTreeNode node;

            if (startNode == null) {
                startNode = new DefaultMutableTreeNode(nodePath.getObject());
                node = startNode;
            }
            else {
                DefaultMutableTreeNode original =
                    ProjectTreeModel.this.getNodeForObjectPath(nodePath.getPath());
                DefaultMutableTreeNode nodeParent =
                    (DefaultMutableTreeNode) nodesMap.get(nodePath.getObjectParent());
                node = new DefaultMutableTreeNode(original.getUserObject());
                nodeParent.add(node);
            }

            registerNode(node);
        }
    }
}
