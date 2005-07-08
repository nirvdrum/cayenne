package org.objectstyle.cayenne.graph;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class NodePropertyChangeOperation extends NodeDiff {

    String property;
    Object oldValue;
    Object newValue;

    NodePropertyChangeOperation(Object nodeId, String property, Object oldValue,
            Object newValue) {

        super(nodeId);
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public void apply(GraphChangeTracker tracker) {
        tracker.nodePropertyChanged(nodeId, property, oldValue, newValue);
    }

    public void undo(GraphChangeTracker tracker) {
        tracker.nodePropertyChanged(nodeId, property, newValue, oldValue);
    }
}
