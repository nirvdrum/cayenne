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
import java.util.List;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
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
        if (rootNode instanceof Configuration) {
            traverseConfig((Configuration) rootNode, null);
        } else if (rootNode instanceof DataDomain) {
            traverseDomains(Collections.singletonList(rootNode), null);
        } else if (rootNode instanceof DataMap) {
            traverseMaps(Collections.singletonList(rootNode), null);
        } else if (rootNode instanceof Entity) {
            traverseEntities(Collections.singletonList(rootNode), null);
        } else if (rootNode instanceof Attribute) {
            traverseAttributes(Collections.singletonList(rootNode), null);
        } else if (rootNode instanceof Relationship) {
            traverseRelationships(Collections.singletonList(rootNode), null);
        } else if (rootNode instanceof DataNode) {
            traverseNodes(Collections.singletonList(rootNode), null);
        } else {
            String nodeClass =
                (rootNode != null) ? rootNode.getClass().getName() : "(null)";
            throw new IllegalArgumentException("Unsupported root node: " + nodeClass);
        }
    }

    /**
     * Performs traversal starting from Configuration node.
     */
    public void traverseConfig(Configuration config, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        ProjectPath configPath = path.appendToPath(config);
        handler.projectNode(configPath);

        if (handler.shouldReadChildren(config, path)) {
            traverseDomains(config.getDomainList(), configPath);
        }
    }

    /**
      * Performs traversal starting from a list of domains.
      */
    public void traverseDomains(List domains, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        Iterator it = domains.iterator();
        while (it.hasNext()) {
            DataDomain domain = (DataDomain) it.next();
            ProjectPath domainPath = path.appendToPath(domain);
            handler.projectNode(domainPath);

            if (handler.shouldReadChildren(domain, path)) {
                traverseNodes(domain.getDataNodeList(), domainPath);
                traverseMaps(domain.getMapList(), domainPath);
            }
        }
    }

    public void traverseNodes(List nodes, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        Iterator it = nodes.iterator();
        while (it.hasNext()) {
            handler.projectNode(path.appendToPath(it.next()));
        }
    }

    public void traverseMaps(List maps, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        Iterator it = maps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            ProjectPath mapPath = path.appendToPath(map);
            handler.projectNode(mapPath);

            if (handler.shouldReadChildren(map, path)) {
                traverseEntities(map.getObjEntitiesAsList(), mapPath);
                traverseEntities(map.getDbEntitiesAsList(), mapPath);
            }
        }
    }

    public void traverseEntities(List entities, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            Entity ent = (Entity) it.next();
            ProjectPath entPath = path.appendToPath(ent);
            handler.projectNode(entPath);

            if (handler.shouldReadChildren(ent, path)) {
                traverseAttributes(ent.getAttributeList(), entPath);
                traverseRelationships(ent.getRelationshipList(), entPath);
            }
        }
    }

    public void traverseAttributes(List attributes, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            handler.projectNode(path.appendToPath(it.next()));
        }
    }

    public void traverseRelationships(List relationships, ProjectPath path) {
        if (path == null) {
            path = new ProjectPath();
        }

        Iterator it = relationships.iterator();
        while (it.hasNext()) {
            handler.projectNode(path.appendToPath(it.next()));
        }
    }
}
