package org.objectstyle.cayenne.unittest;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;

/**
 * @author Andrei Adamchik
 */
public class DB2SetupDelegate extends DatabaseSetupDelegate {
    private static Logger logObj = Logger.getLogger(DB2SetupDelegate.class);

    public DB2SetupDelegate(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsBinaryPK() {
        return false;
    }

    public boolean supportsLobs() {
        return true;
    }

    public boolean supportsStoredProcedures() {
        return false;
    }

    public void createdTables(Connection con, DataMap map) throws Exception {
        executeDDL(con, super.ddlFile("db2", "create-update-sp.sql"));
        executeDDL(con, super.ddlFile("db2", "create-out-sp.sql"));
        executeDDL(con, super.ddlFile("db2", "create-select-sp.sql"));
    }

    public void willDropTables(Connection con, DataMap map) throws Exception {
        // still have to figure out how to safely drop procedures

        try {
            executeDDL(con, super.ddlFile("db2", "drop-select-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }

        try {
            executeDDL(con, super.ddlFile("db2", "drop-update-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }

        try {
            executeDDL(con, super.ddlFile("db2", "drop-out-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }
    }
}
