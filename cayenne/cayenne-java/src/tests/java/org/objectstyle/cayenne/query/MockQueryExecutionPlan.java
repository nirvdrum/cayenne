package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.map.EntityResolver;

public class MockQueryExecutionPlan implements QueryExecutionPlan {

    protected boolean selecting;

    protected boolean resolveCalled;

    public MockQueryExecutionPlan(boolean selecting) {
        this.selecting = selecting;
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return null;
    }

    public Query resolve(EntityResolver resolver) {
        this.resolveCalled = true;
        return (selecting) ? new MockGenericSelectQuery() : new MockQuery();
    }

    public void route(QueryRouter router, EntityResolver resolver) {
    }

    public boolean isResolveCalled() {
        return resolveCalled;
    }

    public boolean isSelecting() {
        return selecting;
    }
}
