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
package org.objectstyle.cayenne.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectTraversalMap {

    private Map children;
    private ObjEntity entity;
    ObjRelationship incoming;

    ObjectTraversalMap(ObjEntity rootEntity, PrefetchTreeNode prefetchTreeNode) {

        this.entity = rootEntity;

        if (prefetchTreeNode != null) {
            Iterator it = prefetchTreeNode.nonPhantomNodes().iterator();
            while (it.hasNext()) {
                addChildren((PrefetchTreeNode) it.next());
            }
        }
    }

    private ObjectTraversalMap() {

    }

    void traverse(Collection objects, ServerToClientObjectConverter transformer) {
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            traverse(object, null, transformer);
        }
    }

    private void traverse(
            DataObject object,
            DataObject parent,
            ServerToClientObjectConverter transformer) {

        transformer.processNode(object, parent, this);

        if (children != null) {
            Iterator it = children.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object child = object.readProperty((String) entry.getKey());
                ObjectTraversalMap childMap = (ObjectTraversalMap) entry.getValue();

                if (child instanceof DataObject) {
                    childMap.traverse((DataObject) child, object, transformer);
                }
                else if (child instanceof Collection) {
                    Iterator childIt = ((Collection) child).iterator();
                    while (childIt.hasNext()) {
                        DataObject toManyChild = (DataObject) childIt.next();
                        childMap.traverse(toManyChild, object, transformer);
                    }
                }
            }
        }
    }

    /**
     * Creates and child node and all nodes in between, as specified by the path.
     */
    ObjectTraversalMap addChildren(PrefetchTreeNode path) {
        Iterator it = entity.resolvePathComponents(path.getPath());

        if (!it.hasNext()) {
            return null;
        }

        ObjectTraversalMap lastChild = this;

        while (it.hasNext()) {
            ObjRelationship r = (ObjRelationship) it.next();
            lastChild = lastChild.addChild(r);
        }

        return lastChild;
    }

    /**
     * Adds a direct child for a given outgoing relationship if such child dow not yet
     * exist. Returns new or existing child node for path,
     */
    ObjectTraversalMap addChild(ObjRelationship outgoing) {
        ObjectTraversalMap child = null;

        if (children == null) {
            children = new LinkedMap();
        }
        else {
            child = (ObjectTraversalMap) children.get(outgoing.getName());
        }

        if (child == null) {
            child = new ObjectTraversalMap();
            child.incoming = outgoing;
            child.entity = (ObjEntity) outgoing.getTargetEntity();
            children.put(outgoing.getName(), child);
        }

        return child;
    }
}
