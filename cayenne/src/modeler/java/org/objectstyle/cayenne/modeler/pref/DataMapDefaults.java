package org.objectstyle.cayenne.modeler.pref;

import org.objectstyle.cayenne.PersistenceState;

public class DataMapDefaults extends _DataMapDefaults {

    public void setPersistenceState(int persistenceState) {

        // init defaults on insert...
        if (this.persistenceState == PersistenceState.TRANSIENT
                && persistenceState == PersistenceState.NEW) {
            setGeneratePairs(Boolean.TRUE);
        }
        super.setPersistenceState(persistenceState);
    }
}

