package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * Encapsulates a collection of other queries.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class QueryChain implements Query {

    protected String name;
    protected Level loggingLevel;
    protected Collection chain;

    /**
     * Creates an empty QueryChain.
     */
    public QueryChain() {
    }

    /**
     * Creates a new QueryChain with a collection of Queries.
     */
    public QueryChain(Collection queries) {
        if (queries != null && !queries.isEmpty()) {
            this.chain = new ArrayList(queries);
        }
    }

    /**
     * Adds a query to the chain.
     */
    public void addQuery(Query query) {
        if (chain == null) {
            chain = new ArrayList();
        }

        chain.add(query);
    }

    /**
     * Removes a query from the chain, returning true if the query was indeed present in
     * the chain and was removed.
     */
    public boolean removeQuery(Query query) {
        return (chain != null) ? chain.remove(query) : false;
    }

    public boolean isEmpty() {
        return chain == null || chain.isEmpty();
    }

    /**
     * Delegates routing to each individual query in the chain. If there is no queries,
     * this method does nothing.
     */
    public void routeQuery(QueryRouter router, EntityResolver resolver) {
        if (chain != null && !chain.isEmpty()) {
            Iterator it = chain.iterator();
            while (it.hasNext()) {
                Query q = (Query) it.next();
                q.routeQuery(router, resolver);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Level getLoggingLevel() {
        return loggingLevel;
    }

    /**
     * Logging level of the QueryChanin can be set, but will be ignored. Keeping for
     * compatibility with Query interface.
     */
    public void setLoggingLevel(Level loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    /**
     * Throws an exception - chain has no root of its own and each query in a chain is
     * routed individually.
     */
    public Object getRoot() {
        throw new CayenneRuntimeException("Chain doesn't support its own root.");
    }

    /**
     * Throws an exception - chain has no root of its own and each query in a chain is
     * routed individually.
     */
    public void setRoot(Object root) {
        throw new CayenneRuntimeException(
                "Chain doesn't support its own root. An attempt to set it to " + root);
    }

    /**
     * Throws an exception as execution should've been delegated to the queries contained
     * in the chain.
     */
    public SQLAction toSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException("Chain doesn't support its own execution "
                + "and should've been split into separate queries during routing phase.");
    }
}
