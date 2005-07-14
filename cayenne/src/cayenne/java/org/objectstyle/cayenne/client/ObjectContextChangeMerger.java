package org.objectstyle.cayenne.client;

import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.graph.BeanChangeMerger;
import org.objectstyle.cayenne.graph.GraphMap;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectContextChangeMerger extends BeanChangeMerger {

    ObjectContext context;

    ObjectContextChangeMerger(ObjectContext context, GraphMap graphMap) {
        super(graphMap);

        this.context = context;
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        Object node = graphMap.unregisterNode(nodeId);

        if (node != null) {
            graphMap.registerNode(newId, node);

            if (node instanceof Persistent) {
                ((Persistent) node).setOid(newId);
            }
        }
    }
}
