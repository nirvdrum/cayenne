package org.objectstyle.cayenne.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages changes of an object graph. Supports recording history of the changes made to
 * objects and also merging external changes to the graph.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class GraphStateManager implements GraphMap {

    protected Map nodes;
    protected ChangeRecorder changeRecorder;
    protected GraphChangeTracker changeMerger;

    public GraphStateManager() {
        this.nodes = new HashMap();
    }

    public synchronized Object getNode(Object nodeId) {
        return nodes.get(nodeId);
    }

    public synchronized void registerNode(Object nodeId, Object nodeObject) {
        nodes.put(nodeId, nodeObject);
    }

    public synchronized Object unregisterNode(Object nodeId) {
        return nodes.remove(nodeId);
    }

    /**
     * Merges external changes to this graph.
     */
    public synchronized void mergeChange(GraphDiff change) {
        if (changeMerger == null) {
            return;
        }

        // temporarily stop recording...
        if (changeRecorder != null) {
            changeRecorder.setRecording(false);
        }

        try {
            change.apply(changeMerger);
        }
        finally {
            if (changeRecorder != null) {
                changeRecorder.setRecording(true);
            }
        }
    }

    /**
     * Returns ChangeRecorder that is used to record changes originating in the managed
     * graph. Such change recorder should be already hooked up somehow to receive update
     * notifications from the graph. This is usually done via injection.
     */
    public ChangeRecorder getChangeRecorder() {
        return changeRecorder;
    }

    public void setChangeRecorder(ChangeRecorder changeRecorder) {
        this.changeRecorder = changeRecorder;

        if (changeRecorder != null) {
            changeRecorder.setRecording(true);
        }
    }

    /**
     * Returns a GraphChangeTracker that is used to process external graph changes.
     */
    public GraphChangeTracker getChangeMerger() {
        return changeMerger;
    }

    /**
     * Sets a GraphChangeTracker that will be used to process external graph changes. The
     * nature of the change merger depends on what kind of objects the underlying graph
     * consists of.
     */
    public void setChangeMerger(GraphChangeTracker changeMerger) {
        this.changeMerger = changeMerger;
    }
}
