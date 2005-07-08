package org.objectstyle.cayenne.graph;

import java.io.Serializable;

/**
 * Defines callback API that can be used by object graph nodes to notify of their state
 * changes. Graph nodes can be any objects as long as each node supports a notion of a
 * unique id within the graph and each directional arc has a unique identifier within its
 * source node.
 * <p>
 * GraphStateTracker can be either "injected" directly into the graph nodes or wrapped in
 * some event notification mechanism to collect graph changes.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface GraphChangeTracker extends Serializable {
    
    void nodeIdChanged(Object nodeId, Object newId);

    /**
     * Notifies implementing object that a new node was created in the graph.
     */
    void nodeCreated(Object nodeId);

    /**
     * Notifies implementing object that a node was removed from the graph.
     */
    void nodeDeleted(Object nodeId);

    /**
     * Notifies implementing object that a node's property was modified.
     */
    void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue);

    /**
     * Notifies implementing object that a new arc was created between two nodes.
     */
    void arcCreated(Object nodeId, Object targetNodeId, Object arcId);

    /**
     * Notifies implementing object that an arc between two nodes was deleted.
     */
    void arcDeleted(Object nodeId, Object targetNodeId, Object arcId);
}