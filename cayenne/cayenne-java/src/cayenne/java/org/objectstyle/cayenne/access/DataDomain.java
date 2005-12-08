/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.access.util.PrimaryKeyHelper;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.opp.BootstrapMessage;
import org.objectstyle.cayenne.opp.GenericQueryMessage;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.SelectMessage;
import org.objectstyle.cayenne.opp.SyncMessage;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryChain;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.util.Util;

/**
 * DataDomain performs query routing functions in Cayenne. DataDomain creates single data
 * source abstraction hiding multiple physical data sources from the user. When a child
 * DataContext sends a query to the DataDomain, it is transparently routed to an
 * appropriate DataNode.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class DataDomain implements QueryEngine, OPPChannel {

    private static final Logger logObj = Logger.getLogger(DataDomain.class);

    public static final String SHARED_CACHE_ENABLED_PROPERTY = "cayenne.DataDomain.sharedCache";
    public static final boolean SHARED_CACHE_ENABLED_DEFAULT = true;

    public static final String VALIDATING_OBJECTS_ON_COMMIT_PROPERTY = "cayenne.DataDomain.validatingObjectsOnCommit";
    public static final boolean VALIDATING_OBJECTS_ON_COMMIT_DEFAULT = true;

    public static final String USING_EXTERNAL_TRANSACTIONS_PROPERTY = "cayenne.DataDomain.usingExternalTransactions";
    public static final boolean USING_EXTERNAL_TRANSACTIONS_DEFAULT = false;

    /**
     * Defines a property name for storing an optional DataContextFactory.
     * 
     * @since 1.2
     */
    public static final String DATA_CONTEXT_FACTORY_PROPERTY = "cayenne.DataDomain.dataContextFactory";

    /** Stores mapping of data nodes to DataNode name keys. */
    protected Map nodes = Collections.synchronizedMap(new TreeMap());
    protected Map nodesByDataMapName = Collections.synchronizedMap(new HashMap());
    protected Collection nodesRef = Collections.unmodifiableCollection(nodes.values());

    /**
     * Properties configured for DataDomain. These include properties of the DataRowStore
     * and remote notifications.
     */
    protected Map properties = Collections.synchronizedMap(new TreeMap());

    protected EntityResolver entityResolver;
    protected PrimaryKeyHelper primaryKeyHelper;
    protected DataRowStore sharedSnapshotCache;
    protected TransactionDelegate transactionDelegate;
    protected DataContextFactory dataContextFactory;
    protected String name;

    // these are initializable from properties...
    protected boolean sharedCacheEnabled;
    protected boolean validatingObjectsOnCommit;
    protected boolean usingExternalTransactions;

    /**
     * @since 1.2
     */
    protected EventManager eventManager;

    /**
     * Creates a DataDomain and assigns it a name.
     */
    public DataDomain(String name) {
        setName(name);
        resetProperties();
    }

    /**
     * Creates new DataDomain.
     * 
     * @param name DataDomain name. Domain can be located using its name in the
     *            Configuration object.
     * @param properties A Map containing domain configuration properties.
     */
    public DataDomain(String name, Map properties) {
        setName(name);
        initWithProperties(properties);
    }

    /**
     * @since 1.1
     */
    protected void resetProperties() {
        if (properties != null) {
            properties.clear();
        }

        sharedCacheEnabled = SHARED_CACHE_ENABLED_DEFAULT;
        validatingObjectsOnCommit = VALIDATING_OBJECTS_ON_COMMIT_DEFAULT;
        usingExternalTransactions = USING_EXTERNAL_TRANSACTIONS_DEFAULT;
        dataContextFactory = null;
    }

    /**
     * Reinitializes domain state with a new set of properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map properties) {
        // create map with predictable modification and synchronization behavior
        Map localMap = new HashMap();
        if (properties != null) {
            localMap.putAll(properties);
        }

        this.properties = localMap;

        Object sharedCacheEnabled = localMap.get(SHARED_CACHE_ENABLED_PROPERTY);
        Object validatingObjectsOnCommit = localMap
                .get(VALIDATING_OBJECTS_ON_COMMIT_PROPERTY);
        Object usingExternalTransactions = localMap
                .get(USING_EXTERNAL_TRANSACTIONS_PROPERTY);

        Object dataContextFactory = localMap.get(DATA_CONTEXT_FACTORY_PROPERTY);

        if (logObj.isDebugEnabled()) {
            logObj.debug("DataDomain property "
                    + SHARED_CACHE_ENABLED_PROPERTY
                    + " = "
                    + sharedCacheEnabled);
            logObj.debug("DataDomain property "
                    + VALIDATING_OBJECTS_ON_COMMIT_PROPERTY
                    + " = "
                    + validatingObjectsOnCommit);
            logObj.debug("DataDomain property "
                    + USING_EXTERNAL_TRANSACTIONS_PROPERTY
                    + " = "
                    + usingExternalTransactions);
            logObj.debug("DataDomain property "
                    + DATA_CONTEXT_FACTORY_PROPERTY
                    + " = "
                    + dataContextFactory);
        }

        // init ivars from properties
        this.sharedCacheEnabled = (sharedCacheEnabled != null)
                ? "true".equalsIgnoreCase(sharedCacheEnabled.toString())
                : SHARED_CACHE_ENABLED_DEFAULT;
        this.validatingObjectsOnCommit = (validatingObjectsOnCommit != null)
                ? "true".equalsIgnoreCase(validatingObjectsOnCommit.toString())
                : VALIDATING_OBJECTS_ON_COMMIT_DEFAULT;
        this.usingExternalTransactions = (usingExternalTransactions != null)
                ? "true".equalsIgnoreCase(usingExternalTransactions.toString())
                : USING_EXTERNAL_TRANSACTIONS_DEFAULT;

        if (dataContextFactory != null
                && !Util.isEmptyString(dataContextFactory.toString())) {
            try {
                Class factoryClass = Class.forName(
                        dataContextFactory.toString(),
                        true,
                        Thread.currentThread().getContextClassLoader());

                if (!DataContextFactory.class.isAssignableFrom(factoryClass)) {
                    throw new CayenneRuntimeException(
                            "Error loading DataContextFactory - factory class does not implement 'DataContextFactory': "
                                    + factoryClass.getName());
                }
                this.dataContextFactory = (DataContextFactory) factoryClass.newInstance();
            }
            catch (CayenneRuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error loading DataContextFactory: "
                        + dataContextFactory, e);
            }
        }
        else {
            this.dataContextFactory = null;
        }
    }

    /**
     * Returns EventManager used by this DataDomain.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Sets EventManager used by this DataDomain.
     * 
     * @since 1.2
     */
    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;

        if (sharedSnapshotCache != null) {
            sharedSnapshotCache.setEventManager(eventManager);
        }
    }

    /**
     * Returns "name" property value.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets "name" property to a new value.
     */
    public synchronized void setName(String name) {
        this.name = name;
        if (sharedSnapshotCache != null) {
            this.sharedSnapshotCache.setName(name);
        }
    }

    /**
     * Returns <code>true</code> if DataContexts produced by this DataDomain are using
     * shared DataRowStore. Returns <code>false</code> if each DataContext would work
     * with its own DataRowStore.
     */
    public boolean isSharedCacheEnabled() {
        return sharedCacheEnabled;
    }

    public void setSharedCacheEnabled(boolean sharedCacheEnabled) {
        this.sharedCacheEnabled = sharedCacheEnabled;
    }

    /**
     * Returns whether child DataContexts default behavior is to perform object validation
     * before commit is executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether child DataContexts should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    /**
     * Returns whether this DataDomain should internally commit all transactions, or let
     * container do that.
     * 
     * @since 1.1
     */
    public boolean isUsingExternalTransactions() {
        return usingExternalTransactions;
    }

    /**
     * Sets a property defining whether this DataDomain should internally commit all
     * transactions, or let container do that.
     * 
     * @since 1.1
     */
    public void setUsingExternalTransactions(boolean flag) {
        this.usingExternalTransactions = flag;
    }

    /**
     * @since 1.1
     * @return a Map of properties for this DataDomain. There is no guarantees of specific
     *         synchronization behavior of this map.
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * @since 1.1
     * @return TransactionDelegate associated with this DataDomain, or null if no delegate
     *         exist.
     */
    public TransactionDelegate getTransactionDelegate() {
        return transactionDelegate;
    }

    /**
     * Initializes TransactionDelegate used by all DataContexts associated with this
     * DataDomain.
     * 
     * @since 1.1
     */
    public void setTransactionDelegate(TransactionDelegate transactionDelegate) {
        this.transactionDelegate = transactionDelegate;
    }

    /**
     * Returns snapshots cache for this DataDomain, lazily initializing it on the first
     * call.
     */
    public synchronized DataRowStore getSharedSnapshotCache() {
        if (sharedSnapshotCache == null) {
            this.sharedSnapshotCache = new DataRowStore(name, properties, eventManager);
        }

        return sharedSnapshotCache;
    }

    public synchronized void setSharedSnapshotCache(DataRowStore snapshotCache) {
        if (this.sharedSnapshotCache != snapshotCache) {
            if (this.sharedSnapshotCache != null) {
                this.sharedSnapshotCache.shutdown();
            }
            this.sharedSnapshotCache = snapshotCache;
        }
    }

    public DataContextFactory getDataContextFactory() {
        return dataContextFactory;
    }

    public void setDataContextFactory(DataContextFactory dataContextFactory) {
        this.dataContextFactory = dataContextFactory;
    }

    /** Registers new DataMap with this domain. */
    public void addMap(DataMap map) {
        getEntityResolver().addDataMap(map);
    }

    /** Returns DataMap matching <code>name</code> parameter. */
    public DataMap getMap(String mapName) {
        return getEntityResolver().getDataMap(mapName);
    }

    /**
     * Removes named DataMap from this DataDomain and any underlying DataNodes that
     * include it.
     */
    public synchronized void removeMap(String mapName) {
        DataMap map = getMap(mapName);
        if (map == null) {
            return;
        }

        // remove from data nodes
        Iterator it = nodes.values().iterator();
        while (it.hasNext()) {
            DataNode node = (DataNode) it.next();
            node.removeDataMap(mapName);
        }

        // remove from EntityResolver
        getEntityResolver().removeDataMap(map);

        // reindex nodes to remove references on removed map entities
        reindexNodes();
    }

    /**
     * Removes a DataNode from DataDomain. Any maps previously associated with this node
     * within domain will still be kept around, however they wan't be mapped to any node.
     */
    public synchronized void removeDataNode(String nodeName) {
        DataNode removed = (DataNode) nodes.remove(nodeName);
        if (removed != null) {

            removed.setEntityResolver(null);

            Iterator it = nodesByDataMapName.values().iterator();
            while (it.hasNext()) {
                if (it.next() == removed) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Returns a collection of registered DataMaps.
     */
    public Collection getDataMaps() {
        return getEntityResolver().getDataMaps();
    }

    /**
     * Returns an unmodifiable collection of DataNodes associated with this domain.
     */
    public Collection getDataNodes() {
        return nodesRef;
    }

    /**
     * Closes all data nodes, removes them from the list of available nodes.
     */
    public void reset() {
        synchronized (nodes) {
            nodes.clear();
            nodesByDataMapName.clear();

            if (entityResolver != null) {
                entityResolver.clearCache();
                entityResolver = null;
            }
        }
    }

    /**
     * Clears the list of internal DataMaps. In most cases it is wise to call "reset"
     * before doing that.
     */
    public void clearDataMaps() {
        getEntityResolver().setDataMaps(Collections.EMPTY_LIST);
    }

    /**
     * Adds new DataNode.
     */
    public synchronized void addNode(DataNode node) {

        // add node to name->node map
        nodes.put(node.getName(), node);
        node.setEntityResolver(this.getEntityResolver());

        // add node to "ent name->node" map
        Iterator nodeMaps = node.getDataMaps().iterator();
        while (nodeMaps.hasNext()) {
            DataMap map = (DataMap) nodeMaps.next();
            this.addMap(map);
            this.nodesByDataMapName.put(map.getName(), node);
        }
    }

    /**
     * Creates and returns a new DataContext. If this DataDomain is configured to use
     * shared cache, returned DataContext will use shared cache as well. Otherwise a new
     * instance of DataRowStore will be used as its local cache.
     */
    public DataContext createDataContext() {
        return createDataContext(isSharedCacheEnabled());
    }

    /**
     * Creates a new DataContext.
     * 
     * @param useSharedCache determines whether resulting DataContext should use shared
     *            vs. local cache. This setting overrides default behavior configured for
     *            this DataDomain via {@link #SHARED_CACHE_ENABLED_PROPERTY}.
     * @since 1.1
     */
    public DataContext createDataContext(boolean useSharedCache) {
        // for new dataRowStores use the same name for all stores
        // it makes it easier to track the event subject
        DataRowStore snapshotCache = (useSharedCache)
                ? getSharedSnapshotCache()
                : new DataRowStore(name, properties, eventManager);

        DataContext context;
        if (null == dataContextFactory) {
            context = new DataContext((OPPChannel) this, new ObjectStore(snapshotCache));
        }
        else {
            context = dataContextFactory.createDataContext(this, new ObjectStore(
                    snapshotCache));
        }
        context.setValidatingObjectsOnCommit(isValidatingObjectsOnCommit());
        return context;
    }

    /**
     * Creates and returns a new inactive transaction. Returned transaction is bound to
     * the current execution thread.
     * <p>
     * If there is a TransactionDelegate, adds the delegate to the newly created
     * Transaction. Behavior of the returned Transaction depends on
     * "usingInternalTransactions" property setting.
     * </p>
     * 
     * @since 1.1
     */
    public Transaction createTransaction() {
        return (isUsingExternalTransactions()) ? Transaction
                .externalTransaction(getTransactionDelegate()) : Transaction
                .internalTransaction(getTransactionDelegate());
    }

    /**
     * Returns registered DataNode whose name matches <code>name</code> parameter.
     */
    public DataNode getNode(String nodeName) {
        return (DataNode) nodes.get(nodeName);
    }

    /**
     * Updates internal index of DataNodes stored by the entity name.
     */
    public synchronized void reindexNodes() {
        nodesByDataMapName.clear();

        Iterator nodes = this.getDataNodes().iterator();
        while (nodes.hasNext()) {
            DataNode node = (DataNode) nodes.next();
            Iterator nodeMaps = node.getDataMaps().iterator();
            while (nodeMaps.hasNext()) {
                DataMap map = (DataMap) nodeMaps.next();
                addMap(map);
                nodesByDataMapName.put(map.getName(), node);
            }
        }
    }

    /**
     * Returns a DataNode that should handle queries for all entities in a DataMap.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap map) {
        synchronized (nodesByDataMapName) {
            DataNode node = (DataNode) nodesByDataMapName.get(map.getName());
            if (node == null) {
                reindexNodes();
                return (DataNode) nodesByDataMapName.get(map.getName());
            }
            else {
                return node;
            }
        }
    }

    /**
     * @since 1.1
     * @deprecated since 1.2, as the corresponding interface method is deprecated.
     */
    public void performQueries(
            Collection queries,
            OperationObserver resultConsumer,
            Transaction transaction) {

        if (queries.isEmpty()) {
            return;
        }

        Transaction old = Transaction.getThreadTransaction();
        Transaction.bindThreadTransaction(transaction);

        try {
            performQuery(new QueryChain(queries), resultConsumer);
        }
        finally {
            Transaction.bindThreadTransaction(old);
        }
    }

    /**
     * Routes queries to appropriate DataNodes for execution.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        if (queries.isEmpty()) {
            return;
        }

        performQuery(new QueryChain(queries), observer);
    }

    public EntityResolver getEntityResolver() {
        if (entityResolver == null) {
            createEntityResolver();
        }

        return entityResolver;
    }

    /**
     * Sets EntityResolver. If not set explicitly, DataDomain creates a default
     * EntityResolver internally on demand.
     * 
     * @since 1.1
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    // creates default entity resolver if there is none set yet
    private synchronized void createEntityResolver() {
        if (entityResolver == null) {
            // entity resolver will be self-indexing as we add all our maps
            // to it as they are added to the DataDomain
            entityResolver = new org.objectstyle.cayenne.map.EntityResolver();
        }
    }

    // creates default PrimaryKeyHelper
    private void createKeyGenerator() {
        primaryKeyHelper = new PrimaryKeyHelper(this);
    }

    /**
     * @return PrimaryKeyHelper
     */
    public synchronized PrimaryKeyHelper getPrimaryKeyHelper() {
        // TODO instead of on the spot generation, we can
        // use lazy initialization features similar to DefaultSorter
        if (primaryKeyHelper == null) {
            createKeyGenerator();
        }

        return primaryKeyHelper;
    }

    /**
     * Shutdowns all owned data nodes. Invokes DataNode.shutdown().
     */
    public void shutdown() {
        this.sharedSnapshotCache.shutdown();

        Collection dataNodes = getDataNodes();
        for (Iterator i = dataNodes.iterator(); i.hasNext();) {
            DataNode node = (DataNode) i.next();
            try {
                node.shutdown();
            }
            catch (Exception ex) {
            }
        }
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", name).toString();
    }

    // **** new 1.2 PersistenceContext methods:
    // =======================================

    /**
     * Routes and executes a given query, wrapping it in a transaction. Note that query
     * resolution phase is not done at this level and is a responsibility of the caller.
     * 
     * @since 1.2
     */
    public void performQuery(Query query, OperationObserver resultConsumer) {

        final Map queryMap = new HashMap();

        // TODO: optimize for single engine - the most common case...

        // QueryRouter to organize queries by engine
        QueryRouter router = new QueryRouter() {

            public QueryEngine engineForDataMap(DataMap map) {
                if (map == null) {
                    throw new NullPointerException(
                            "Null DataMap, can't determine DataNode.");
                }

                QueryEngine node = lookupDataNode(map);

                if (node == null) {
                    throw new CayenneRuntimeException("No DataNode exists for DataMap "
                            + map);
                }

                return node;
            }

            public void useEngineForQuery(QueryEngine engine, Query query) {

                List queriesByEngine = (List) queryMap.get(engine);
                if (queriesByEngine == null) {
                    queriesByEngine = new ArrayList();
                    queryMap.put(engine, queriesByEngine);
                }

                queriesByEngine.add(query);
            }
        };

        query.route(router, getEntityResolver());

        // perform queries on each node
        Iterator nodeIt = queryMap.entrySet().iterator();
        while (nodeIt.hasNext()) {
            Map.Entry entry = (Map.Entry) nodeIt.next();
            QueryEngine nextNode = (QueryEngine) entry.getKey();
            Collection nodeQueries = (Collection) entry.getValue();
            nextNode.performQueries(nodeQueries, resultConsumer);
        }
    }

    // ****** OPPChannel methods:

    /**
     * @since 1.2
     */
    public EntityResolver onBootstrap(BootstrapMessage message) {
        return entityResolver;
    }

    /**
     * @since 1.2
     */
    public QueryResponse onGenericQuery(GenericQueryMessage message) {
        QueryResult result = new QueryResult();
        performQuery(message.getQuery().resolve(getEntityResolver()), result);
        return result;
    }

    /**
     * @since 1.2
     */
    public List onSelectQuery(SelectMessage message) {
        Query query = message.getQuery().resolve(getEntityResolver());
        QueryResult result = new QueryResult();
        performQuery(query, result);
        return result.getFirstRows(query);
    }

    /**
     * @since 1.2
     */
    public GraphDiff onSync(SyncMessage message) {

        // ignore all but commit messages
        if (message.getType() == SyncMessage.COMMIT_TYPE) {
            return new DataDomainCommitAction(this).commit(message);
        }

        return null;
    }
}