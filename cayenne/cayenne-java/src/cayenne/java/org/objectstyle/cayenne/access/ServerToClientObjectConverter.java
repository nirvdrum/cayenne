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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class ServerToClientObjectConverter {

    Map clientObjectsByOID;
    List converted;
    EntityResolver clientResolver;

    ServerToClientObjectConverter(List serverObjects, EntityResolver clientResolver,
            PrefetchTreeNode prefetchTree) {

        this.clientObjectsByOID = new HashMap();
        this.converted = new ArrayList(serverObjects.size());
        this.clientResolver = clientResolver;

        if (!serverObjects.isEmpty()) {

            // note that 'someEntityName' is an entity located in some unpredictable
            // place of object inheritance hierarchy, so it is simply used to resolve
            // prefetches. IT CAN NOT BE USED TO OBTAIN CLASS DESCRIPTORS - THIS HAS TO BE
            // DONE INDIVIDUALLY FOR EACH OBJECT.
            String someEntityName = ((Persistent) serverObjects.get(0))
                    .getObjectId()
                    .getEntityName();

            // create traversal map using the client entity
            new ObjectTraversalMap(
                    clientResolver.lookupObjEntity(someEntityName),
                    prefetchTree).traverse(serverObjects, this);
        }
    }

    List getConverted() {
        return converted;
    }

    void processNode(DataObject object, DataObject parent, ObjectTraversalMap node) {

        // check if client object is already resolved to ensure uniquing
        Persistent clientObject = (Persistent) clientObjectsByOID.get(object
                .getObjectId());

        if (clientObject == null) {

            ObjectId id = object.getObjectId();

            // DO NOT USE NODE'S ENTITY TO LOOKUP DESCRIPTOR, as inheritance may be
            // involved and different objects may be of different class.

            // TODO: Andrus, 09/24/2005: maybe analyze inheritance tree upfront and build
            // a smaller lookup map ... can save a
            // few CPU cycles on big lists

            ObjEntity entity = clientResolver.lookupObjEntity(id.getEntityName());
            if (entity == null) {
                throw new CayenneRuntimeException("No client entity mapped for name: "
                        + id.getEntityName());
            }

            ClassDescriptor descriptor = entity.getClassDescriptor();
            clientObject = (Persistent) descriptor.createObject();
            clientObject.setObjectId(id);

            // copy attributes properties
            Iterator it = descriptor.getPropertyNames().iterator();
            while (it.hasNext()) {
                Property property = descriptor.getProperty((String) it.next());
                if (!(property instanceof ArcProperty)) {
                    property.writeValue(clientObject, null, object.readProperty(property
                            .getPropertyName()));
                }
            }
            clientObjectsByOID.put(object.getObjectId(), clientObject);
        }

        // ... connect to parent ...
        // if parent is null, this is one of the converted objects...
        if (parent == null) {
            converted.add(clientObject);
        }
        else {
            Persistent clientParentObject = (Persistent) clientObjectsByOID.get(parent
                    .getObjectId());

            // sanity check
            if (clientParentObject == null) {
                throw new CayenneRuntimeException(
                        "Can't find parent object. Server pier id: "
                                + parent.getObjectId());
            }

            ObjEntity parentEntity = (ObjEntity) node.incoming.getSourceEntity();
            ClassDescriptor parentDescriptor = parentEntity.getClassDescriptor();

            Property arcProperty = parentDescriptor.getProperty(node.incoming.getName());
            arcProperty.writeValue(clientParentObject, null, clientObject);

            // don't write reverse property ... in case it is a list, we don't want it
            // resolved ... let it stay a fault.

            // TODO (Andrus 09/19/2005): at least hook up to-one reverse relationships...
            // This would do no harm at all and can potentailly save more than a few trips
            // to the server.
        }
    }
}
