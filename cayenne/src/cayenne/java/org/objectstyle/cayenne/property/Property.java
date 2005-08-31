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
     * Returns true if a property is implemented with a level of indirection, e.g. via a
     * ValueHolder or Collection.
     */
    boolean isIndirect();

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
     * Reads and returns a property value of an object, skipping any special
     * preprocessing, such as resolving faults, etc.
     */
    Object directRead(Object object) throws PropertyAccessException;

    /**
     * Sets a property value of an object skipping any special preprocessing, such as
     * resolving faults, etc.
     */
    void directWrite(Object object, Object value) throws PropertyAccessException;

    /**
     * Copies a property value from one object to another.
     */
    void copyProperty(Object from, Object to) throws PropertyAccessException;
}
