package org.objectstyle.cayenne.wocompat;

import org.objectstyle.cayenne.map.ObjEntity;

/**
 * An EOObjEntity is a mapping descriptor of a Java class property with added
 * fields for WebOBjects EOModel.
 * It contains the information about the Java class itself, as well
 * as its mapping to the DbEntity layer.
 *
 * @author Dario Bagatto
 *
 */
public class EOObjEntity extends ObjEntity {

    // flag that indicates whether this Entity represents a client java class
    protected boolean isClientEntity;
    // flag that indicates whether this Entity has a superclass set in the eonmodel
    protected boolean hasSuperClass;
    // flag that indicates whether this Entity is set as abstract in the eomodel
    protected boolean isAbstractEntity;


    public EOObjEntity() {
        super();
    }

    public EOObjEntity(String s) {
        super(s);
    }

    /**
     * Sets the the superclass state.
     * @param value
     */
    public void setHasSuperClass(boolean value) {
        hasSuperClass = value;
    }

    /**
     * Returns the superclass state.
     * @return true when there is a superclass defined in the eomodel.
     */
    public boolean getHasSuperClass() {
        return hasSuperClass;
    }

    /**
     * Sets the client entity state.
     * @param value
     */
    public void setIsClientEntity(boolean value) {
        isClientEntity = value;
    }

    /**
     * Returns the client entity flag
     * @return true when this entity object represents a client java class.
     */
    public boolean getIsClientEntity() {
        return isClientEntity;
    }

    /**
     * Sets the abstract entity flag.
     * @param value
     */
    public void setIsAbstractEntity(boolean value) {
        isAbstractEntity = value;
    }

    /**
     * Returns the abstract Entity state
     * @return true if this entity is set as abstract int the eomodel.
     */
    public boolean getIsAbstractEntity() {
        return isAbstractEntity;
    }

}
