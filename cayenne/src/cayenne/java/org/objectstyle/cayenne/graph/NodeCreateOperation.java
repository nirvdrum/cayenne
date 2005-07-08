package org.objectstyle.cayenne.graph;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class NodeCreateOperation extends NodeDiff {

    NodeCreateOperation(Object nodeId) {
        super(nodeId);
    }

    public void apply(GraphChangeTracker tracker) {
        tracker.nodeCreated(nodeId);
    }

    public void undo(GraphChangeTracker tracker) {
        tracker.nodeDeleted(nodeId);
    }
}
