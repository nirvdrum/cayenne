package org.objectstyle.cayenne.graph;

class NodeIdChangeOperation extends NodeDiff {

    Object newNodeId;

    public NodeIdChangeOperation(Object nodeId, Object newNodeId) {
        super(nodeId);

        this.newNodeId = newNodeId;
    }

    public void apply(GraphChangeTracker tracker) {
    }

    public void undo(GraphChangeTracker tracker) {
    }
}
