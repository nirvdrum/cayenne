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
package org.objectstyle.cayenne.graph;

import java.util.Collection;

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * GraphChangeTracker that applies received changes to the objects in the graph, treating
 * nodes and arcs as properties and using introspection.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class BeanChangeMerger implements GraphChangeHandler {

    protected GraphMap graphMap;

    public BeanChangeMerger(GraphMap graphMap) {
        this.graphMap = graphMap;
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        Object node = graphMap.unregisterNode(nodeId);

        if (node != null) {
            graphMap.registerNode(newId, node);
        }
    }

    public void nodeCreated(Object nodeId) {
        // noop

        // TODO: in an interactive application we may actually need to instantiate this
        // node and display it... Or maybe just have a delegate that decides whether it
        // cares about the node (e.g. if a node is outside the viewport we don't have
        // to show it)
    }

    public void nodeDeleted(Object nodeId) {
        graphMap.unregisterNode(nodeId);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        Object node = graphMap.getNode(nodeId);
        if (node != null) {
            try {
                PropertyUtils.setSimpleProperty(node, property, newValue);
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error setting property " + property, e);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        Object node = graphMap.getNode(nodeId);
        if (node != null) {

            Object nodeTarget = graphMap.getNode(targetNodeId);

            // treat arc id as a relationship name, and see if the property is a
            // collection ... if it is add to collection, otherwise set directly
            try {

                Object property = PropertyUtils.getSimpleProperty(node, arcId.toString());
                if (property instanceof Collection) {

                    Collection collection = (Collection) property;
                    if (nodeTarget != null && !collection.contains(nodeTarget)) {
                        collection.add(nodeTarget);
                    }
                }
                else {
                    PropertyUtils.setSimpleProperty(node, arcId.toString(), nodeTarget);
                }
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error creating arc " + arcId, e);
            }
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        Object node = graphMap.getNode(nodeId);
        if (node != null) {

            Object nodeTarget = graphMap.getNode(targetNodeId);

            // treat arc id as a relationship name, and see if the property is a
            // collection ... if it is remove from collection, otherwise set to null
            try {

                Object property = PropertyUtils.getSimpleProperty(node, arcId.toString());
                if (property instanceof Collection) {

                    Collection collection = (Collection) property;
                    if (nodeTarget != null) {
                        collection.remove(nodeTarget);
                    }
                }
                else {
                    PropertyUtils.setSimpleProperty(node, arcId.toString(), null);
                }
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error removing arc " + arcId, e);
            }
        }
    }
}