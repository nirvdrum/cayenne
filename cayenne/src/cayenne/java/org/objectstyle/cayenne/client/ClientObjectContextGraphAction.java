package org.objectstyle.cayenne.client;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.Property;

/**
 * An action object that processes graph change calls from Persistent object. It handles
 * GraphManager notifications and bi-directional graph consistency.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ClientObjectContextGraphAction {

    ClientObjectContext context;

    ClientObjectContextGraphAction(ClientObjectContext context) {
        this.context = context;
    }

    /**
     * Handles property change in a Peristent object.
     */
    void handlePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {

        // translate ObjectContext generic property change callback to GraphManager terms
        // (simple properties vs. relationships)

        ClassDescriptor descriptor = context.getClassDescriptor(object);
        Property property = descriptor.getProperty(propertyName);

        // relationship property
        if (property instanceof ArcProperty) {
            if (oldValue instanceof Persistent) {
                context.getGraphManager().arcDeleted(
                        object.getGlobalID(),
                        ((Persistent) oldValue).getGlobalID(),
                        propertyName);

                unsetReverse((ArcProperty) property, object, (Persistent) oldValue);
                markAsDirty(object);
            }

            if (newValue instanceof Persistent) {
                context.getGraphManager().arcCreated(
                        object.getGlobalID(),
                        ((Persistent) newValue).getGlobalID(),
                        propertyName);

                setReverse((ArcProperty) property, object, (Persistent) newValue);
                markAsDirty(object);
            }
        }
        // simple property
        else {
            context.getGraphManager().nodePropertyChanged(
                    object.getGlobalID(),
                    propertyName,
                    oldValue,
                    newValue);
            markAsDirty(object);
        }
    }

    /**
     * Changes object state to MODIFIED if needed.
     */
    private void markAsDirty(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
        }
    }

    private void setReverse(
            ArcProperty property,
            Persistent sourceObject,
            Persistent targetObject) {

        String reverseName = property.getReversePropertyName();
        if (reverseName != null) {
            ClassDescriptor reverseDescriptor = context.getClassDescriptor(targetObject);
            reverseDescriptor.getProperty(reverseName).writeValue(
                    targetObject,
                    null,
                    sourceObject);

            context.getGraphManager().arcCreated(
                    targetObject.getGlobalID(),
                    sourceObject.getGlobalID(),
                    reverseName);

            markAsDirty(targetObject);
        }
    }

    private void unsetReverse(
            ArcProperty property,
            Persistent sourceObject,
            Persistent targetObject) {

        String reverseName = property.getReversePropertyName();
        if (reverseName != null) {
            ClassDescriptor reverseDescriptor = context.getClassDescriptor(targetObject);
            reverseDescriptor.getProperty(reverseName).writeValue(
                    targetObject,
                    sourceObject,
                    null);

            context.getGraphManager().arcDeleted(
                    targetObject.getGlobalID(),
                    sourceObject.getGlobalID(),
                    reverseName);

            markAsDirty(targetObject);
        }
    }
}
