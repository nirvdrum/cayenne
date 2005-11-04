package org.objectstyle.cayenne.property;

import java.io.Serializable;

/**
 * Defines bean property API used by Cayenne to access object data, do faulting and graph
 * maintenance tasks.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface Property extends Serializable {

    /**
     * Returns property name.
     */
    String getPropertyName();

    /**
     * Returns property declared Java class.
     */
    Class getPropertyType();

    /**
     * Performs any needed processing right before object property is accessed for read or
     * write.
     */
    void prepareForAccess(Object object) throws PropertyAccessException;

    /**
     * Returns a property value of an object.
     */
    Object readValue(Object object) throws PropertyAccessException;

    /**
     * Sets a property value of an object. Old value of the property is specified as a
     * hint.
     */
    void writeValue(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException;

    /**
     * Copies a property value from one object to another.
     */
    void copyValue(Object from, Object to) throws PropertyAccessException;
}
