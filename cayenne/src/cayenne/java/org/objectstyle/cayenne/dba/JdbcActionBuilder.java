package org.objectstyle.cayenne.dba;

import org.objectstyle.cayenne.access.jdbc.BatchAction;
import org.objectstyle.cayenne.access.jdbc.ProcedureAction;
import org.objectstyle.cayenne.access.jdbc.SQLTemplateAction;
import org.objectstyle.cayenne.access.jdbc.SQLTemplateSelectAction;
import org.objectstyle.cayenne.access.jdbc.SelectAction;
import org.objectstyle.cayenne.access.jdbc.UpdateAction;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SQLActionVisitor;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * Implements a default mechanism for executing queries. Individual processing methods
 * instantiate appropriate SQLActions and delegate the actual executing to them.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
public class JdbcActionBuilder implements SQLActionVisitor {

    protected DbAdapter adapter;
    protected EntityResolver entityResolver;

    public JdbcActionBuilder(DbAdapter adapter, EntityResolver resolver) {
        this.adapter = adapter;
        this.entityResolver = resolver;
    }

    public SQLAction makeBatchUpdate(BatchQuery query) {
        // check run strategy...

        // optimistic locking is not supported in batches due to JDBC driver limitations
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        boolean runningAsBatch = !useOptimisticLock && adapter.supportsBatchUpdates();
        BatchAction action = new BatchAction(query, adapter, entityResolver);
        action.setBatch(runningAsBatch);
        return action;
    }

    public SQLAction makeProcedure(ProcedureQuery query) {
        return new ProcedureAction(query, adapter, entityResolver);
    }

    public SQLAction makeSelect(GenericSelectQuery query) {
        return new SelectAction(query, adapter, entityResolver);
    }

    public SQLAction makeSQL(SQLTemplate query) {
        return (query.isSelecting())
                ? new SQLTemplateSelectAction(query, adapter)
                : new SQLTemplateAction(query, adapter);
    }

    public SQLAction makeUpdate(Query query) {
        return new UpdateAction(query, adapter, entityResolver);
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }
}
