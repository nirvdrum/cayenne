package org.objectstyle.cayenne.modeler.pref;

import java.io.File;

import org.objectstyle.cayenne.modeler.pref.auto._FSPath;
import org.objectstyle.cayenne.pref.Domain;

public class FSPath extends _FSPath {

    public static final String LAST_DIR_PREF_KEY = "lastDir";

    public static FSPath getPreference(Domain domain) {
        return (FSPath) domain.getPreferenceDetail(
                FSPath.LAST_DIR_PREF_KEY,
                FSPath.class,
                true);
    }

    public void setPath(File file) {
        if (file.isFile()) {
            setPath(file.getParentFile().getAbsolutePath());
        }
        else {
            setPath(file.getAbsolutePath());
        }
    }

    public File getExistingDirectory(boolean create) {
        if (getPath() == null) {
            return null;
        }

        File path = new File(getPath());
        if (path.isDirectory()) {
            return path;
        }

        if (path.isFile()) {
            return path.getParentFile();
        }

        if (create) {
            path.mkdirs();
            return path;
        }

        return null;
    }
}

