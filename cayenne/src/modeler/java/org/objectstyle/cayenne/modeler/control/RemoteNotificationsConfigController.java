package org.objectstyle.cayenne.modeler.control;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.model.RemoteNotificationsConfigModel;
import org.objectstyle.cayenne.modeler.view.RemoteNotificationsConfigDialog;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @author Andrei Adamchik
 */
public class RemoteNotificationsConfigController extends BasicController {
    private static Logger logObj = Logger.getLogger(RemoteNotificationsConfigController.class);
    
    public static final String SAVE_CONFIG_CONTROL =
        "cayenne.modeler.remoteNotificationsConfig.save.button";
    public static final String CANCEL_CONFIG_CONTROL =
        "cayenne.modeler.remoteNotificationsConfig.cancel.button";

    public RemoteNotificationsConfigController() {
        super();
    }
    

    public void startup() {
        setModel(new RemoteNotificationsConfigModel());
        setView(new RemoteNotificationsConfigDialog());
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONFIG_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONFIG_CONTROL)) {
            commitChanges();
        }
    }
    
    protected void commitChanges() {
        shutdown();
    }
}
