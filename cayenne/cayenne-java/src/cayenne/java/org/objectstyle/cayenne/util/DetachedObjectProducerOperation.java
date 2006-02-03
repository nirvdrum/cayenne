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
package org.objectstyle.cayenne.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ObjectGraphVisitor;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * An operation that produces a detached object tree with subgraph delineated per optional
 * PrefetchTreeNode.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class DetachedObjectProducerOperation implements ObjectGraphVisitor {

    protected Map mergeMap;
    protected Map children;

    public DetachedObjectProducerOperation(PrefetchTreeNode tree) {
        this(tree, new HashMap());
    }

    protected DetachedObjectProducerOperation(PrefetchTreeNode tree, Map sharedMergeMap) {
        this.mergeMap = sharedMergeMap;

        // traverse the tree to build child operations
        if (tree != null && tree.hasChildren()) {
            children = new HashMap();
            Iterator it = tree.getChildren().iterator();
            while (it.hasNext()) {
                PrefetchTreeNode child = (PrefetchTreeNode) it.next();
                children.put(child.getName(), new DetachedObjectProducerOperation(child));
            }
        }
    }

    public boolean visitSimpleProperty(Property property) {
        return true;
    }

    public boolean visitToOneArcProperty(ArcProperty property, Object targetValue) {
        return children != null && children.containsKey(property.getPropertyName());
    }

    public boolean visitToManyArcProperty(ArcProperty property, Object targetValue) {
        return visitToOneArcProperty(property, targetValue);
    }

    public ObjectGraphVisitor getChildVisitor(ArcProperty property) {
        if (children == null) {
            throw new CayenneRuntimeException("Visiting arc property '"
                    + property.getPropertyName()
                    + "' is not allowed.");
        }

        ObjectGraphVisitor child = (ObjectGraphVisitor) children.get(property
                .getPropertyName());
        if (child == null) {
            throw new CayenneRuntimeException("Visiting arc property '"
                    + property.getPropertyName()
                    + "' is not allowed.");
        }

        return child;
    }

    public ObjectContext getContext() {
        return null;
    }

    public Object getVisitedObject(Object id) {
        return mergeMap.get(id);
    }

    public void objectVisited(Object id, Object object) {
        mergeMap.put(id, object);
    }
}
