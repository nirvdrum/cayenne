package org.objectstyle.cayenne.graph;

/**
 * A map of graph nodes by their ids.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface GraphMap {

    Object getNode(Object nodeId);

    void registerNode(Object nodeId, Object nodeObject);

    Object unregisterNode(Object nodeId);
}
