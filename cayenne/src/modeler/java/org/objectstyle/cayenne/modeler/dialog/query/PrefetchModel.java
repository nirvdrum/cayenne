package org.objectstyle.cayenne.modeler.dialog.query;

import java.util.Iterator;

import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Relationship;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * A Scope active model for a query prefetch.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class PrefetchModel extends BasicModel {

    public static final Selector PATH_SELECTOR = Selector.fromString("path");
    public static final Selector TO_MANY_SELECTOR = Selector.fromString("toMany");

    protected String path;
    protected boolean toMany;

    public PrefetchModel(Entity entity, String path) {
        Iterator components = entity.resolvePathComponents(path);
        Object last = null;

        while (components.hasNext()) {
            last = components.next();
        }

        if (!(last instanceof Relationship)) {
            throw new IllegalArgumentException(
                "Invalid relationship path, must end with relationship...: " + path);
        }

        this.path = path;
        this.toMany = ((Relationship) last).isToMany();
    }

    public String getPath() {
        return path;
    }

    public boolean isToMany() {
        return toMany;
    }
}
