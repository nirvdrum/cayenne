package org.objectstyle.cayenne.map;

import java.util.Collection;

/**
 * Defines API for sorting of Cayenne entities based on their mutual dependencies.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public interface EntitySorter
    extends org.objectstyle.cayenne.access.util.DependencySorter {

    /**
     * Initializes a list of DataMaps used by the sorter.
     */
    public void setDataMaps(Collection dataMaps);
}
