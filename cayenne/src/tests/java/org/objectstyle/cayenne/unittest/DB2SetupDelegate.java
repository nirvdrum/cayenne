package org.objectstyle.cayenne.unittest;

import org.objectstyle.cayenne.dba.DbAdapter;

/**
 * @author Andrei Adamchik
 */
public class DB2SetupDelegate extends DatabaseSetupDelegate {

    public DB2SetupDelegate(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsBinaryPK() {
        return false;
    }
    
    public boolean supportsLobs() {
        return true;
    }
}
