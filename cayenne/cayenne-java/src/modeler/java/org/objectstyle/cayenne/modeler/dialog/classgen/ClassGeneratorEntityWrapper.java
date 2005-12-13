package org.objectstyle.cayenne.modeler.dialog.classgen;

import org.objectstyle.cayenne.map.ObjEntity;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorEntityWrapper {

    protected ObjEntity entity;
    protected boolean selected;
    protected String validationMessage;

    public ClassGeneratorEntityWrapper(ObjEntity entity, boolean selected) {
        this(entity, selected, null);
    }

    public ClassGeneratorEntityWrapper(ObjEntity entity, boolean selected,
            String validationMessage) {
        this.entity = entity;
        this.selected = selected;
        this.validationMessage = validationMessage;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setEntity(ObjEntity entity) {
        this.entity = entity;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEnabled() {
        return validationMessage == null;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
}
