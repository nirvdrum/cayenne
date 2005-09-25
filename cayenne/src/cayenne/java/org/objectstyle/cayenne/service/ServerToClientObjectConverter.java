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
import org.objectstyle.cayenne.distribution.GlobalID;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.Property;

class ServerToClientObjectConverter {

    Map clientObjectsByOID;
    List converted;
    EntityResolver resolver;
    ClientEntityResolver clientResolver;

    ServerToClientObjectConverter(List serverObjects, EntityResolver resolver,
            Collection prefetchPaths) {

        this.clientObjectsByOID = new HashMap();
        this.converted = new ArrayList(serverObjects.size());
        this.resolver = resolver;
        this.clientResolver = resolver.getClientEntityResolver();

        if (!serverObjects.isEmpty()) {

            // note that 'someServerEntity' is an entity located in some unpredictable
            // place of object inheritance hierarchy, so it is simply used to resolve
            // prefetches. IT CAN NOT BE USED TO OBTAIN CLASS DESCRIPTORS - THIS HAS TO BE
            // DONE INDIVIDUALLY FOR EACH OBJECT.
            ObjEntity someServerEntity = resolver.lookupObjEntity(serverObjects
                    .get(0)
                    .getClass());
            if (someServerEntity == null) {
                String className = (someServerEntity != null) ? someServerEntity
                        .getName() : "<null>";
                throw new CayenneRuntimeException("Can't find entity for server class: "
                        + className);
            }

            // create traversal map using the client entity
            new ObjectTraversalMap(clientResolver.entityForName(someServerEntity
                    .getName()), prefetchPaths).traverse(serverObjects, this);
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

            GlobalID id = resolver.convertToGlobalID(object.getObjectId());

            // DO NOT USE NODE'S ENTITY TO LOOKUP DESCRIPTOR, as inheritance may be
            // involved and different objects may be of different class.

            // TODO: Andrus, 09/24/2005: maybe analyze inheritance tree upfront and build
            // a smaller lookup map ... and combine with "convertToGlobalID"; can save a
            // few CPU cycles on big lists

            ClassDescriptor descriptor = clientResolver
                    .entityForName(id.getEntityName())
                    .getClassDescriptor();
            clientObject = (Persistent) descriptor.createObject();
            clientObject.setGlobalID(id);

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
