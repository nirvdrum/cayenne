package org.objectstyle.cayenne.graph;

/**
 * An abstract superclass of operations on individual nodes and arcs in a digraph.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
abstract class NodeDiff implements GraphDiff {

    Object nodeId;

    public NodeDiff(Object nodeId) {
        this.nodeId = nodeId;
    }

    public abstract void apply(GraphChangeTracker tracker);

    public abstract void undo(GraphChangeTracker tracker);

    Object getNodeId() {
        return nodeId;
    }
}
