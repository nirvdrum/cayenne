package org.objectstyle.cayenne.modeler.pref;

import org.objectstyle.cayenne.PersistenceState;

public class DataMapDefaults extends _DataMapDefaults {

    public void setPersistenceState(int persistenceState) {

        // init defaults on insert...
        if (this.persistenceState == PersistenceState.TRANSIENT
                && persistenceState == PersistenceState.NEW) {
            setGeneratePairs(Boolean.TRUE);
            setSuperclassPackageSuffix("auto");
        }
        super.setPersistenceState(persistenceState);
    }

    public String getSuperclassPackage(String classPackage) {
        String suffix = getSuperclassPackageSuffix();

        String dot = (classPackage != null && suffix != null) ? "." : "";

        if (classPackage == null) {
            classPackage = "";
        }

        if (suffix == null) {
            suffix = "";
        }

        String packageName = classPackage + dot + suffix;
        return (packageName.length() > 0) ? packageName : null;
    }

    public void setSuperclassPackage(String superclassPackage, String classPackage) {
        if (superclassPackage == null) {
            setSuperclassPackageSuffix(null);
        }
        else if (classPackage == null) {
            setSuperclassPackageSuffix(superclassPackage);
        }
        else if (superclassPackage.length() > classPackage.length() + 2
                && superclassPackage.startsWith(classPackage + ".")) {
            setSuperclassPackageSuffix(superclassPackage
                    .substring(classPackage.length() + 2));
        }
        else {
            setSuperclassPackageSuffix(null);
        }
    }
}

