package org.objectstyle.cayenne.wocompat;

import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * An EOObjAttribute is a mapping descriptor of a Java class property with added
 * fields for WebOBjects EOModel.
 *
 * @author Dario Bagatto
 *
 */
public class EOObjAttribute extends ObjAttribute {

    // flag whether this attribute is read only.
    protected boolean readOnly;


    public EOObjAttribute() {
        super();
    }


    public EOObjAttribute(String name) {
        super(name);
    }


    public EOObjAttribute(String name, String type, ObjEntity entity) {
        super(name, type, entity);
    }


    /**
     * Sets the read only state of this attribute.
     * @param readOnly
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Get the read only state of this attribute
     * @return read only state of this attribute
     */
    public boolean getReadOnly() {
        return readOnly;
    }

}
