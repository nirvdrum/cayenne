package org.objectstyle.cayenne.dba.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;

/**
 * SQLServer-specific DataNode implementation that handles certian issues like identity
 * columns, etc.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
public class SQLServerDataNode extends DataNode {

    public SQLServerDataNode() {
        super();
    }

    public SQLServerDataNode(String name) {
        super(name);
    }

    /**
     * Implements handling of identity PK columns.
     */
    protected void runBatchUpdate(
            Connection connection,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        if (query instanceof InsertBatchQuery
                && isIdentityInsert()
                && query.getDbEntity() != null) {

            String configSQL = "SET IDENTITY_INSERT "
                    + query.getDbEntity().getFullyQualifiedName()
                    + " ON";

            QueryLogger.logQuery(
                    query.getLoggingLevel(),
                    configSQL,
                    Collections.EMPTY_LIST);

            Statement statement = connection.createStatement();
            try {
                statement.execute(configSQL);
            }
            finally {
                try {
                    statement.close();
                }
                catch (Exception e) {
                }
            }
        }

        super.runBatchUpdate(connection, query, delegate);
    }

    /**
     * Returns the value of "identityInsert" property of the underlying adapter.
     */
    protected boolean isIdentityInsert() {
        if (adapter instanceof SQLServerAdapter) {
            return ((SQLServerAdapter) adapter).isIdentityInsert();
        }

        return false;
    }
}