package org.objectstyle.cayenne.modeler.model;

import org.objectstyle.cayenne.map.ObjEntity;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorEntityWrapper {
    protected ObjEntity entity;
    protected boolean selected;

    public ClassGeneratorEntityWrapper(ObjEntity entity, boolean selected) {
        this.entity = entity;
        this.selected = selected;
    }
    
    /**
     * Returns the entity.
     * @return ObjEntity
     */
    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Returns the selected.
     * @return boolean
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the entity.
     * @param entity The entity to set
     */
    public void setEntity(ObjEntity entity) {
        this.entity = entity;
    }

    /**
     * Sets the selected.
     * @param selected The selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
