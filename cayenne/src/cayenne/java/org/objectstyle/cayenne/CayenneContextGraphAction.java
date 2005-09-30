package org.objectstyle.cayenne;

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
class CayenneContextGraphAction {

    CayenneContext context;
    ThreadLocal arcChangeInProcess;

    CayenneContextGraphAction(CayenneContext context) {
        this.context = context;
        this.arcChangeInProcess = new ThreadLocal();
    }

    boolean isArchChangeInProcess() {
        return arcChangeInProcess.get() != null;
    }

    void setArcChangeInProcess(boolean flag) {
        arcChangeInProcess.set(flag ? Boolean.TRUE : null);
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

            try {
                handleArcPropertyChange(
                        object,
                        (ArcProperty) property,
                        oldValue,
                        newValue);
            }
            finally {
                setArcChangeInProcess(false);
            }
        }
        // simple property
        else {
            handleSimplePropertyChange(object, propertyName, oldValue, newValue);
        }
    }

    void handleArcPropertyChange(
            Persistent object,
            ArcProperty property,
            Object oldValue,
            Object newValue) {

        boolean arcChangeInProcess = isArchChangeInProcess();

        // prevent reverse actions down the stack
        setArcChangeInProcess(true);

        if (oldValue instanceof Persistent) {
            context.getGraphManager().arcDeleted(
                    object.getGlobalID(),
                    ((Persistent) oldValue).getGlobalID(),
                    property.getPropertyName());

            if (!arcChangeInProcess) {
                unsetReverse(property, object, (Persistent) oldValue);
            }

            markAsDirty(object);
        }

        if (newValue instanceof Persistent) {
            context.getGraphManager().arcCreated(
                    object.getGlobalID(),
                    ((Persistent) newValue).getGlobalID(),
                    property.getPropertyName());

            if (!arcChangeInProcess) {
                setReverse(property, object, (Persistent) newValue);
            }
            
            markAsDirty(object);
        }
    }

    void handleSimplePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {
        context.getGraphManager().nodePropertyChanged(
                object.getGlobalID(),
                propertyName,
                oldValue,
                newValue);
        markAsDirty(object);
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
