package org.objectstyle.cayenne.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An object that stores graph changes as "operations" that can be replayed later.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ChangeRecorder implements GraphChangeTracker, GraphDiff {

    protected boolean recording;
    protected List diffs;

    public ChangeRecorder() {
        this.diffs = new ArrayList();
    }

    public void clear() {
        this.diffs = new ArrayList();
    }

    public boolean isEmpty() {
        return diffs.isEmpty();
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public void apply(GraphChangeTracker tracker) {
        // implements a naive linear commit - simply replay stored operations
        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            GraphDiff change = (GraphDiff) it.next();
            change.apply(tracker);
        }
    }

    public void undo(GraphChangeTracker tracker) {

        // implements a naive linear commit - simply replay stored operations
        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            GraphDiff change = (GraphDiff) it.next();
            change.undo(tracker);
        }
    }

    public void nodeCreated(Object nodeId) {
        if (recording) {
            synchronized (diffs) {
                diffs.add(new NodeCreateOperation(nodeId));
            }
        }
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        if (recording) {
            synchronized (diffs) {
                diffs.add(new NodeIdChangeOperation(nodeId, newId));
            }
        }
    }

    public void nodeDeleted(Object nodeId) {
        if (recording) {
            synchronized (diffs) {
                diffs.add(new NodeDeleteOperation(nodeId));
            }
        }
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        if (recording) {
            synchronized (diffs) {
                diffs.add(new NodePropertyChangeOperation(
                        nodeId,
                        property,
                        oldValue,
                        newValue));
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        if (recording) {
            synchronized (diffs) {
                diffs.add(new ArcCreateOperation(nodeId, targetNodeId, arcId));
            }
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        if (recording) {
            synchronized (diffs) {
                diffs.add(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
            }
        }
    }
}
