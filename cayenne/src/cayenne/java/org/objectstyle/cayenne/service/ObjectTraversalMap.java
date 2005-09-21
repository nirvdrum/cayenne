package org.objectstyle.cayenne.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

public class ObjectTraversalMap {

    Map children;
    ObjEntity entity;
    ObjRelationship incoming;

    ObjectTraversalMap(ObjEntity rootEntity, Collection pathSpecs) {

        this.entity = rootEntity;

        Iterator it = pathSpecs.iterator();
        while (it.hasNext()) {
            addChildren((String) it.next());
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
    ObjectTraversalMap addChildren(String path) {
        Iterator it = entity.resolvePathComponents(path);

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
