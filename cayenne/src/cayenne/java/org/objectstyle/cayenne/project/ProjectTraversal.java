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

import java.util.Collections;
import java.util.Iterator;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Relationship;

/**
 * ProjectTraversal allows to traverse Cayenne project tree in a
 * "depth-first" order starting from an arbitrary level to its children. 
 * 
 * <p><i>Current implementation is not very efficient
 * and would actually first read the whole tree, before returning 
 * the first element from the iterator.</i></p>
 * 
 * @author Andrei Adamchik
 */
public class ProjectTraversal {

    protected ProjectTraversalHandler handler;

    public ProjectTraversal(ProjectTraversalHandler handler) {
        this.handler = handler;
    }

    /**
     * Performs traversal starting from the root node. Root node can be
     * of any type supported in Cayenne projects (Configuration, DataMap, DataNode, etc...)
     */
    public void traverse(Object rootNode) {
        this.traverse(rootNode, new ProjectPath());
    }

    public void traverse(Object rootNode, ProjectPath path) {
        if (rootNode instanceof Project) {
            this.traverseProject((Project)rootNode, path);
        } else if (rootNode instanceof DataDomain) {
            this.traverseDomains(Collections.singletonList(rootNode).iterator(), path);
        } else if (rootNode instanceof DataMap) {
            this.traverseMaps(Collections.singletonList(rootNode).iterator(), path);
        } else if (rootNode instanceof Entity) {
            this.traverseEntities(Collections.singletonList(rootNode).iterator(), path);
        } else if (rootNode instanceof Attribute) {
            this.traverseAttributes(Collections.singletonList(rootNode).iterator(), path);
        } else if (rootNode instanceof Relationship) {
            this.traverseRelationships(Collections.singletonList(rootNode).iterator(), path);
        } else if (rootNode instanceof DataNode) {
            this.traverseNodes(Collections.singletonList(rootNode).iterator(), path);
        } else {
            String nodeClass = (rootNode != null)
            					? rootNode.getClass().getName()
            					: "(null)";
            throw new IllegalArgumentException(
                "Unsupported root node: " + nodeClass);
        }
    }

    /** 
     * Performs traversal starting from the Project and down to its children.
     */
    public void traverseProject(Project project, ProjectPath path) {
        ProjectPath projectPath = path.appendToPath(project);
        handler.projectNode(projectPath);

        if (handler.shouldReadChildren(project, path)) {
            Iterator it = project.getChildren().iterator();
            while (it.hasNext()) {
                this.traverse(it.next(), projectPath);
            }
        }
    }

    /**
      * Performs traversal starting from a list of domains.
      */
    public void traverseDomains(Iterator domains, ProjectPath path) {
        while (domains.hasNext()) {
            DataDomain domain = (DataDomain)domains.next();
            ProjectPath domainPath = path.appendToPath(domain);
            handler.projectNode(domainPath);

            if (handler.shouldReadChildren(domain, path)) {
                this.traverseMaps(domain.getDataMaps().iterator(), domainPath);
                this.traverseNodes(domain.getDataNodes().iterator(), domainPath);
            }
        }
    }

    public void traverseNodes(Iterator nodes, ProjectPath path) {
        while (nodes.hasNext()) {
            DataNode node = (DataNode)nodes.next();
            ProjectPath nodePath = path.appendToPath(node);
            handler.projectNode(nodePath);

            if (handler.shouldReadChildren(node, path)) {
                this.traverseMaps(node.getDataMaps().iterator(), nodePath);
            }
        }
    }

    public void traverseMaps(Iterator maps, ProjectPath path) {
        while (maps.hasNext()) {
            DataMap map = (DataMap)maps.next();
            ProjectPath mapPath = path.appendToPath(map);
            handler.projectNode(mapPath);

            if (handler.shouldReadChildren(map, path)) {
                this.traverseEntities(map.getObjEntitiesAsList().iterator(), mapPath);
                this.traverseEntities(map.getDbEntitiesAsList().iterator(), mapPath);
            }
        }
    }

    public void traverseEntities(Iterator entities, ProjectPath path) {
        while (entities.hasNext()) {
            Entity ent = (Entity)entities.next();
            ProjectPath entPath = path.appendToPath(ent);
            handler.projectNode(entPath);

            if (handler.shouldReadChildren(ent, path)) {
                this.traverseAttributes(ent.getAttributes().iterator(), entPath);
                this.traverseRelationships(ent.getRelationships().iterator(), entPath);
            }
        }
    }

    public void traverseAttributes(Iterator attributes, ProjectPath path) {
        while (attributes.hasNext()) {
            handler.projectNode(path.appendToPath(attributes.next()));
        }
    }

    public void traverseRelationships(Iterator relationships, ProjectPath path) {
        while (relationships.hasNext()) {
            handler.projectNode(path.appendToPath(relationships.next()));
        }
    }
}
