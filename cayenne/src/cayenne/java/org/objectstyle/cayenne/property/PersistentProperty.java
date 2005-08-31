package org.objectstyle.cayenne.property;

import java.io.Serializable;

/**
 * Defines bean property API used by Cayenne to access object data, do faulting and graph
 * maintenance tasks.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface PersistentProperty extends Serializable {

    /**
     * Returns property name.
     */
    String getPropertyName();

    /**
     * Returns property type.
     */
    Class getPropertyType();

    /**
     * Performs any needed processing right before a property will be read.
     */
    void willRead(Object object) throws PropertyAccessException;

    /**
     * Performs any needed processing right before a property will be written.
     */
    void willWrite(Object object, Object newValue) throws PropertyAccessException;

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
     * Copies a property from one object to another.
     */
    void copy(Object from, Object to) throws PropertyAccessException;
}
