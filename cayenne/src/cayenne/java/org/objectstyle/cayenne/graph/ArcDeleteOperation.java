package org.objectstyle.cayenne.graph;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class ArcDeleteOperation extends NodeDiff {

    Object targetNodeId;
    Object arcId;

    public ArcDeleteOperation(Object nodeId, Object targetNodeId, Object arcId) {
        super(nodeId);
        this.targetNodeId = targetNodeId;
        this.arcId = arcId;
    }

    public void apply(GraphChangeTracker tracker) {
        tracker.arcDeleted(nodeId, targetNodeId, arcId);
    }

    public void undo(GraphChangeTracker tracker) {
        tracker.arcCreated(nodeId, targetNodeId, arcId);
    }
}