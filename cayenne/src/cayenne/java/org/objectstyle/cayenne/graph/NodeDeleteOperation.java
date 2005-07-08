package org.objectstyle.cayenne.graph;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class NodeDeleteOperation extends NodeDiff {

    NodeDeleteOperation(Object nodeId) {
        super(nodeId);
    }

    public void apply(GraphChangeTracker tracker) {
        tracker.nodeDeleted(nodeId);
    }

    public void undo(GraphChangeTracker tracker) {
        tracker.nodeCreated(nodeId);
    }
}