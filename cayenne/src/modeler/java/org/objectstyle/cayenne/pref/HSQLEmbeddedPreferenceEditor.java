package org.objectstyle.cayenne.pref;

import java.io.File;

/**
 * @author Andrei Adamchik
 */
public class HSQLEmbeddedPreferenceEditor extends CayennePreferenceEditor {

    protected Delegate delegate;

    public HSQLEmbeddedPreferenceEditor(HSQLEmbeddedPreferenceService service) {
        super(service);
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    protected void restart() {
        try {
            service.stopService();
            checkForLocks();
            service.startService();
        }
        finally {
            restartRequired = false;
        }
    }

    protected HSQLEmbeddedPreferenceService getHSQLService() {
        return (HSQLEmbeddedPreferenceService) getService();
    }

    protected boolean checkForLocks() {
        if (delegate != null) {
            HSQLEmbeddedPreferenceService service = getHSQLService();
            if (service.isSecondaryDB()) {
                File lock = service.getMasterLock();
                if (lock.isFile()) {
                    return delegate.deleteMasterLock(lock);
                }
            }
        }

        return true;
    }

    // delegate interface allowing UI to interfere with the editor tasks
    public static interface Delegate {

        boolean deleteMasterLock(File lock);
    }
}