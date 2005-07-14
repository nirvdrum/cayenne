package org.objectstyle.cayenne.service;

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.graph.GraphChangeHandler;

/**
 * A GraphChangeHandler that propagates object graph changes to an underlying
 * ObjectContext. Assumes that node ids are Cayenne ObjectIds.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectDataContextChangeMerger implements GraphChangeHandler {

    ObjectDataContext context;

    ObjectDataContextChangeMerger(ObjectDataContext context) {
        this.context = context;
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        throw new CayenneRuntimeException("Unimplemented");
    }

    public void nodeCreated(Object nodeId) {
        ObjectId id = toObjectId(nodeId);
        context.createAndRegisterNewObject(id);
    }

    public void nodeDeleted(Object nodeId) {
        Persistent object = findObject(nodeId);
        context.deleteObject(object);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        Persistent object = findObject(nodeId);
        try {
            PropertyUtils.setSimpleProperty(object, property, newValue);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error setting property: " + property, e);
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        throw new CayenneRuntimeException(
                "TODO: implement relationship change updates...");
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        throw new CayenneRuntimeException(
                "TODO: implement relationship change updates...");
    }

    Persistent findObject(Object nodeId) {
        ObjectId id = toObjectId(nodeId);
        return context.getObjectStore().getObject(id);
    }

    ObjectId toObjectId(Object nodeId) {
        if (nodeId instanceof ObjectId) {
            return (ObjectId) nodeId;
        }
        else if (nodeId == null) {
            throw new NullPointerException("Null ObjectId");
        }
        else {
            throw new CayenneRuntimeException("Node id is expected to be ObjectId, got: "
                    + nodeId);
        }
    }
}
