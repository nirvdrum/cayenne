package org.objectstyle.cayenne.modeler.model;

import org.scopemvc.core.Selector;

/**
 * @author Andrei Adamchik
 */
public class RemoteNotificationsConfigModel {

    public static final Object[] NOTIFICATION_TYPES =
        new Object[] { "JavaGroups Multicast (Default)", "JMS Transport", "Custom Transport" };

    public static final Selector NOTIFICATION_TYPES_SELECTOR =
        Selector.fromString("notificationTypes");

    public static final Selector SELECTED_TYPE_SELECTOR =
        Selector.fromString("selectedType");

    protected String selectedType;

    public RemoteNotificationsConfigModel() {
        super();
    }

    public String getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(String selectedType) {
        this.selectedType = selectedType;
    }

    public Object[] getNotificationTypes() {
        return NOTIFICATION_TYPES;
    }

}
