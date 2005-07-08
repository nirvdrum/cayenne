package org.objectstyle.cayenne.graph;

import java.io.Serializable;

/**
 * Represents a change in an object graph. This can be a simple change (like a node
 * property update) or a composite change that consists of a number of smaller changes.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface GraphDiff extends Serializable {

    /**
     * Calls appropriate methods on the tracker to "replay" this change.
     */
    void apply(GraphChangeTracker tracker);

    /**
     * Calls appropriate methods on the tracker to revert this change.
     */
    void undo(GraphChangeTracker tracker);
}
