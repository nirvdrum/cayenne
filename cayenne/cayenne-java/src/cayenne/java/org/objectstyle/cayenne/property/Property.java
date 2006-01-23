package org.objectstyle.cayenne.property;

/**
 * Defines bean property API used by Cayenne to access object data, do faulting and graph
 * maintenance tasks.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface Property extends PropertyAccessor {

    /**
     * Performs any needed processing right before object property is accessed for read or
     * write.
     */
    void prepareForAccess(Object object) throws PropertyAccessException;

    /**
     * Copies a property value from one object to another.
     */
    void copyValue(Object from, Object to) throws PropertyAccessException;
}
