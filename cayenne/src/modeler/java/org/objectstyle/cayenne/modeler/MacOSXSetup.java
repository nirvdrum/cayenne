package org.objectstyle.cayenne.modeler;

import org.objectstyle.cayenne.modeler.action.AboutAction;
import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.action.ExitAction;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * Configures Modeler to better integrate into Mac OS X environment. Has no effect
 * on other platforms.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
class MacOSXSetup {

    static void configureMacOSX() {
        // Application.getApplication().setEnabledPreferencesMenu(true);
        Application.getApplication().addApplicationListener(new MacEventsAdapter());
    }

    static class MacEventsAdapter extends ApplicationAdapter {

        public void handleAbout(ApplicationEvent e) {
            if (!e.isHandled()) {
                ((AboutAction) getAction(AboutAction.getActionName())).showAboutDialog();
                e.setHandled(true);
            }
        }

        public void handleQuit(ApplicationEvent e) {
            if (!e.isHandled()) {
                ((ExitAction) getAction(ExitAction.getActionName())).exit();
                e.setHandled(true);
            }
        }

        CayenneAction getAction(String name) {
            return CayenneModelerFrame.getFrame().getAction(name);
        }
    }
}
