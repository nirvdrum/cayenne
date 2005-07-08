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
public class IntrospectionChangeMerger implements GraphChangeTracker {

    protected GraphMap graphMap;

    public IntrospectionChangeMerger(GraphMap graphMap) {
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
