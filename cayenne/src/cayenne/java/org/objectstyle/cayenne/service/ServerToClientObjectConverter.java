package org.objectstyle.cayenne.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.client.ClientEntityResolver;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.Property;

class ServerToClientObjectConverter {

    Map clientObjectsByOID;
    List converted;
    EntityResolver resolver;

    ServerToClientObjectConverter(Collection serverObjects, EntityResolver resolver,
            Class serverObjectClass, Collection prefetchPaths) {

        this.clientObjectsByOID = new HashMap();
        this.converted = new ArrayList(serverObjects.size());
        this.resolver = resolver;

        if (!serverObjects.isEmpty()) {
            ObjEntity serverEntity = resolver.lookupObjEntity(serverObjectClass);
            if (serverEntity == null) {
                String className = (serverObjectClass != null) ? serverObjectClass
                        .getName() : "<null>";
                throw new CayenneRuntimeException("Can't find entity for server class: "
                        + className);
            }

            ClientEntityResolver clientResolver = resolver.getClientEntityResolver();

            // create traversal map using the client entity
            new ObjectTraversalMap(
                    clientResolver.entityForName(serverEntity.getName()),
                    prefetchPaths).traverse(serverObjects, this);
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
            ClassDescriptor descriptor = node.entity.getClassDescriptor();
            clientObject = (Persistent) descriptor.createObject();
            clientObject.setGlobalID(resolver.convertToGlobalID(object.getObjectId()));

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
