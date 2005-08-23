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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.access.util.PrefetchHelper;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.ParameterizedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Class that provides applications with access to Cayenne persistence features. In most
 * cases this is the only access class directly used in the application.
 * <p>
 * Most common DataContext use pattern is to create one DataContext per session. "Session"
 * may be a an HttpSesession in a web application, or any other similar concept in a
 * multiuser application.
 * </p>
 * <p>
 * DataObjects are registered with DataContext either implicitly when they are fetched via
 * a query, or read via a relationship from another object, or explicitly via calling
 * {@link #createAndRegisterNewObject(Class)}during new DataObject creation. DataContext
 * tracks changes made to its DataObjects in memory, and flushes them to the database when
 * {@link #commitChanges()}is called. Until DataContext is committed, changes made to its
 * objects are not visible in other DataContexts.
 * </p>
 * <p>
 * Each DataObject can belong only to a single DataContext. To create a replica of an
 * object from a different DataContext in a local context, use
 * {@link #localObjects(java.util.List)}method.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class DataContext implements QueryEngine, Serializable {

    // noop delegate
    private static final DataContextDelegate defaultDelegate = new DataContextDelegate() {

        public GenericSelectQuery willPerformSelect(
                DataContext context,
                GenericSelectQuery query) {
            return query;
        }

        public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
            return true;
        }

        public boolean shouldProcessDelete(DataObject object) {
            return true;
        }

        public void finishedMergeChanges(DataObject object) {

        }

        public void finishedProcessDelete(DataObject object) {

        }
    };

    // DataContext events
    public static final EventSubject WILL_COMMIT = EventSubject.getSubject(
            DataContext.class,
            "DataContextWillCommit");
    public static final EventSubject DID_COMMIT = EventSubject.getSubject(
            DataContext.class,
            "DataContextDidCommit");
    public static final EventSubject DID_ROLLBACK = EventSubject.getSubject(
            DataContext.class,
            "DataContextDidRollback");
    

    
    /**
     * A holder of a DataContext bound to the current thread.
     * 
     * @since 1.1
     */
    protected static final ThreadLocal threadDataContext = new ThreadLocal();

    // event posting default for new DataContexts
    private static boolean transactionEventsEnabledDefault;

    // enable/disable event handling for individual instances
    private boolean transactionEventsEnabled;

    // Set of DataContextDelegates to be notified.
    private DataContextDelegate delegate;

    protected boolean usingSharedSnaphsotCache;
    protected boolean validatingObjectsOnCommit;
    protected ObjectStore objectStore;

    protected transient QueryEngine parent;
    
    /**
     * Stores user defined properties associated with this DataContext.
     * 
     * @since 1.2
     */
    protected Map userProperties;

    /**
     * Stores the name of parent DataDomain. Used to defer initialization of the parent
     * QueryEngine after deserialization. This helps avoid an issue with certain servlet
     * engines (e.g. Tomcat) where HttpSessions with DataContext's are deserialized at
     * startup before Cayenne stack is fully initialized.
     */
    protected transient String lazyInitParentDomainName;

    /**
     * A factory method of DataObjects. Uses Configuration ClassLoader to instantiate a
     * new instance of DataObject of a given class.
     */
    private static final DataObject newDataObject(String className) throws Exception {
        return (DataObject) Configuration
                .getResourceLoader()
                .loadClass(className)
                .newInstance();
    }
    
    /**
     * Returns the DataContext bound to the current thread.
     * 
     * @since 1.1
     * @return the DataContext associated with caller thread.
     * @throws IllegalStateException if there is no DataContext bound to the current
     *             thread.
     * @see org.objectstyle.cayenne.conf.WebApplicationContextProvider
     */
    public static DataContext getThreadDataContext() throws IllegalStateException {
        DataContext dc = (DataContext) threadDataContext.get();
        if (dc == null) {
            throw new IllegalStateException("Current thread has no bound DataContext.");
        }

        return dc;
    }
    
    /**
     * Binds a DataContext to the current thread. DataContext can later be retrieved by 
     * users in the same thread by calling {@link DataContext#getThreadDataContext}. Using
     * null parameter will unbind currently bound DataContext. 
     * 
     * @since 1.1
     */
    public static void bindThreadDataContext(DataContext context) {
        threadDataContext.set(context);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String)} must be used instead. ObjectStore associated
     * with created DataContext will have a cache stack configured using parent domain settings.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String, boolean)} must be used instead.
     * ObjectStore associated with newly created DataContext will have a cache 
     * stack configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(boolean useSharedCache) {
        return Configuration.getSharedConfiguration().getDomain().createDataContext(
                useSharedCache);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext using named
     * domain as its parent. If there is no domain matching the name argument, an
     * exception is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    /**
     * Creates and returns new DataContext that will use a named DataDomain as its parent.
     * ObjectStore associated with newly created DataContext will have a cache 
     * stack configured according to the specified policy, overriding a parent domain 
     * setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(String domainName, boolean useSharedCache) {

        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext(useSharedCache);
    }

    /**
     * Default constructor that creates a DataContext that has no association with a
     * DataDomain.
     */
    public DataContext() {
        this(null, null);
    }

    /**
     * Creates a DataContext with parent QueryEngine and a DataRowStore that should be
     * used by the ObjectStore.
     * 
     * @since 1.1
     * @param parent parent QueryEngine used to communicate with the data source.
     * @param objectStore ObjectStore used by DataContext.
     */
    public DataContext(QueryEngine parent, ObjectStore objectStore) {
        setParent(parent);

        this.objectStore = objectStore;
        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
        this.usingSharedSnaphsotCache = getParentDataDomain() != null
                && objectStore.getDataRowCache() == getParentDataDomain()
                        .getSharedSnapshotCache();
    }

    /**
     * Initializes parent if deserialization left it uninitialized.
     */
    private final void awakeFromDeserialization() {
        if (parent == null && lazyInitParentDomainName != null) {

            DataDomain domain = Configuration.getSharedConfiguration().getDomain(
                    lazyInitParentDomainName);

            this.parent = domain;

            if (isUsingSharedSnapshotCache() && domain != null) {
                this.objectStore.setDataRowCache(domain.getSharedSnapshotCache());
            }
        }
    }
    
    /**
     * Returns a map of user-defined properties associated with this DataContext.
     * 
     * @since 1.2
     */
    protected Map getUserProperties() {
        // do lazy init..
        // as not all users will take advantage of properties, creating the
        // map on demand to keep DataContext lean...
        if (userProperties == null) {
            userProperties = new HashMap();
        }

        return userProperties;
    }
    
    /**
     * Returns a user-defined property previously set via 'setUserProperty'. Note that it
     * is a caller responsibility to synchronize access to properties.
     * 
     * @since 1.2
     */
    public Object getUserProperty(String key) {
        return getUserProperties().get(key);
    }

    /**
     * Sets a user-defined property. Note that it is a caller responsibility to
     * synchronize access to properties.
     * 
     * @since 1.2
     */
    public void setUserProperty(String key, Object value) {
        getUserProperties().put(key, value);
    }
    

    /**
     * Returns parent QueryEngine object. In most cases returned object is an instance of
     * DataDomain.
     */
    public QueryEngine getParent() {
        awakeFromDeserialization();
        return parent;
    }

    /**
     * <i>Note: currently nested DataContexts are not supported, so this method simply
     * calls "getParent()". Using this method is preferrable to calling "getParent()"
     * directly and casting it to DataDomain, since it more likely to be compatible with
     * the future releases of Cayenne. </i>
     * 
     * @return DataDomain that is a direct or indirect parent of this DataContext.
     * @since 1.1
     */
    public DataDomain getParentDataDomain() {
        return (DataDomain) getParent();
    }

    /**
     * Sets direct parent of this DataContext.
     */
    public void setParent(QueryEngine parent) {
        this.parent = parent;
    }

    /**
     * Sets a DataContextDelegate for this context. Delegate is notified of certain events
     * in the DataContext lifecycle and can customize DataContext behavior.
     * 
     * @since 1.1
     */
    public void setDelegate(DataContextDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a delegate currently associated with this DataContext.
     * 
     * @since 1.1
     */
    public DataContextDelegate getDelegate() {
        return delegate;
    }

    /**
     * @return a delegate instance if it is initialized, or a shared noop implementation
     *         the context has no delegate. Useful to prevent extra null checks and
     *         conditional logic in the code.
     * @since 1.1
     */
    DataContextDelegate nonNullDelegate() {
        return (delegate != null) ? delegate : DataContext.defaultDelegate;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        awakeFromDeserialization();
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this DataContext, <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.NEW
     */
    public Collection newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.DELETED
     */
    public Collection deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.MODIFIED
     */
    public Collection modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns an object for a given ObjectId. If object is not registered with this
     * context, a "hollow" object fault is created, registered, and returned to the
     * caller.
     */
    public DataObject registeredObject(ObjectId oid) {
        // must synchronize on ObjectStore since we must read and write atomically
        synchronized (getObjectStore()) {
            DataObject obj = objectStore.getObject(oid);
            if (obj == null) {
                try {
                    // TODO: shouldn't we replace this with oid.getObjectClass().newInstance()
                    obj = DataContext.newDataObject(oid.getObjectClass().getName());
                }
                catch (Exception ex) {
                    String entity = (oid != null) ? getEntityResolver().lookupObjEntity(
                            oid.getObjectClass()).getName() : null;
                    throw new CayenneRuntimeException(
                            "Error creating object for entity '" + entity + "'.",
                            ex);
                }

                obj.setObjectId(oid);
                obj.setPersistenceState(PersistenceState.HOLLOW);
                obj.setDataContext(this);
                objectStore.addObject(obj);
            }
            return obj;
        }
    }

    /**
     * Creates or gets from cache a DataRow reflecting current object state.
     * 
     * @since 1.1
     */
    public DataRow currentSnapshot(DataObject anObject) {
        ObjEntity entity = getEntityResolver().lookupObjEntity(anObject);

        // for a HOLLOW object return snapshot from cache
        if (anObject.getPersistenceState() == PersistenceState.HOLLOW
                && anObject.getDataContext() != null) {

            ObjectId id = anObject.getObjectId();
            return getObjectStore().getSnapshot(id, this);
        }

        DataRow snapshot = new DataRow(10);

        Iterator attributes = entity.getAttributeMap().entrySet().iterator();
        while (attributes.hasNext()) {
            Map.Entry entry = (Map.Entry) attributes.next();
            String attrName = (String) entry.getKey();
            ObjAttribute objAttr = (ObjAttribute) entry.getValue();

            // processing compound attributes correctly
            snapshot.put(objAttr.getDbAttributePath(), anObject
                    .readPropertyDirectly(attrName));
        }

        Iterator relationships = entity.getRelationshipMap().entrySet().iterator();
        while (relationships.hasNext()) {
            Map.Entry entry = (Map.Entry) relationships.next();
            ObjRelationship rel = (ObjRelationship) entry.getValue();

            // if target doesn't propagates its key value, skip it
            if (rel.isSourceIndependentFromTargetChange()) {
                continue;
            }

            Object targetObject = anObject.readPropertyDirectly(rel.getName());
            if (targetObject == null) {
                continue;
            }

            // if target is Fault, get id attributes from stored snapshot
            // to avoid unneeded fault triggering
            if (targetObject instanceof Fault) {
                DataRow storedSnapshot = getObjectStore().getSnapshot(
                        anObject.getObjectId(),
                        this);
                if (storedSnapshot == null) {
                    throw new CayenneRuntimeException(
                            "No matching objects found for ObjectId "
                                    + anObject.getObjectId()
                                    + ". Object may have been deleted externally.");
                }

                DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
                Iterator joins = dbRel.getJoins().iterator();
                while (joins.hasNext()) {
                    DbJoin join = (DbJoin) joins.next();
                    String key = join.getSourceName();
                    snapshot.put(key, storedSnapshot.get(key));
                }

                continue;
            }

            // target is resolved and we have an FK->PK to it, 
            // so extract it from target...
            DataObject target = (DataObject) targetObject;
            Map idParts = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects
            if (idParts.isEmpty()) {
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            snapshot.putAll(fk);
        }

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map thisIdParts = anObject.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {
            
            // put only those that do not exist in the map
            Iterator idIterator = thisIdParts.entrySet().iterator();
            while (idIterator.hasNext()) {
                Map.Entry entry = (Map.Entry) idIterator.next();
                Object nextKey = entry.getKey();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, entry.getValue());
                }
            }
        }
        
        return snapshot;
    }

    /**
     * Creates a list of DataObjects local to this DataContext from a list of DataObjects
     * coming from a different DataContext. Note that all objects in the source list must
     * be either in COMMITTED or in HOLLOW state.
     * 
     * @since 1.0.3
     */
    public List localObjects(List objects) {
        List localObjects = new ArrayList(objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // sanity check
            if (object.getPersistenceState() != PersistenceState.COMMITTED
                    && object.getPersistenceState() != PersistenceState.HOLLOW) {
                throw new CayenneRuntimeException(
                        "Only COMMITTED and HOLLOW objects can be transferred between contexts. "
                                + "Invalid object state '"
                                + PersistenceState.persistenceStateName(object
                                        .getPersistenceState())
                                + "', ObjectId: "
                                + object.getObjectId());
            }

            DataObject localObject = (object.getDataContext() != this)
                    ? registeredObject(object.getObjectId())
                    : object;
            localObjects.add(localObject);
        }

        return localObjects;
    }

    /**
     * Converts a list of data rows to a list of DataObjects.
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(
            ObjEntity entity,
            List dataRows,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {
        
        return new DataContextObjectFactory(this, refresh, resolveInheritanceHierarchy)
                .objectsFromDataRows(entity, dataRows);
    }

    /**
     * Converts a list of DataRows to a List of DataObject registered with this
     * DataContext. Internally calls
     * {@link #objectsFromDataRows(ObjEntity,List,boolean,boolean)}.
     * 
     * @since 1.1
     * @see DataRow
     * @see DataObject
     */
    public List objectsFromDataRows(
            Class objectClass,
            List dataRows,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);
        return objectsFromDataRows(entity, dataRows, refresh, resolveInheritanceHierarchy);
    }

    /**
     * Creates a DataObject from DataRow. This is a convenience shortcut to
     * {@link #objectsFromDataRows(Class,java.util.List,boolean,boolean)}.
     * 
     * @see DataRow
     * @see DataObject
     */
    public DataObject objectFromDataRow(
            Class objectClass,
            DataRow dataRow,
            boolean refresh) {
        List list = objectsFromDataRows(
                objectClass,
                Collections.singletonList(dataRow),
                refresh,
                true);
        return (DataObject) list.get(0);
    }


    /**
     * Instantiates new object and registers it with itself. Object class is determined
     * from ObjEntity. Object class must have a default constructor.
     * <p>
     * <i>Note: preferred way to create new objects is via
     * {@link #createAndRegisterNewObject(Class)}method. It works exactly the same way,
     * but makes the application type-safe. </i>
     * </p>
     * 
     * @see #createAndRegisterNewObject(Class)
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        ObjEntity entity = this.getEntityResolver().getObjEntity(objEntityName);

        if (entity == null) {
            throw new IllegalArgumentException("Invalid entity name: " + objEntityName);
        }

        String objClassName = entity.getClassName();
        DataObject dataObject = null;
        try {
            dataObject = DataContext.newDataObject(objClassName);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObjectWithEntity(dataObject, entity);
        return dataObject;
    }

    /**
     * Instantiates new object and registers it with itself. Object class must have a
     * default constructor.
     * 
     * @since 1.1
     */
    public DataObject createAndRegisterNewObject(Class objectClass) {
        if (objectClass == null) {
            throw new NullPointerException("DataObject class can't be null.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(objectClass);
        if (entity == null) {
            throw new IllegalArgumentException("Class is not mapped with Cayenne: "
                    + objectClass.getName());
        }

        DataObject dataObject = null;
        try {
            dataObject = (DataObject) objectClass.newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObjectWithEntity(dataObject, entity);
        return dataObject;
    }


    /**
     * Registers a new object (that is not yet persistent) with itself.
     * 
     * @param dataObject new object that we want to make persistent.
     */
    public void registerNewObject(DataObject dataObject) {
        if (dataObject == null) {
            throw new NullPointerException("Can't register null object.");
        }

        // sanity check - maybe already registered
        if (dataObject.getObjectId() != null) {
            if (dataObject.getDataContext() == this) {
                // already registered, just ignore
                return;
            }
            else if (dataObject.getDataContext() != null) {
                throw new IllegalStateException(
                        "DataObject is already registered with another DataContext. Try using 'localObjects()' instead.");
            }
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(dataObject);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Can't find ObjEntity for DataObject class: "
                            + dataObject.getClass().getName()
                            + ", class is likely not mapped.");
        }

        registerNewObjectWithEntity(dataObject, entity);
    }

    private void registerNewObjectWithEntity(DataObject dataObject, ObjEntity objEntity) {
        // method is private ... assuming all sanity checks on the DataObject have been
        // performed by the caller depending on the invocation context

        if (dataObject.getObjectId() == null) {
            dataObject.setObjectId(new TempObjectId(dataObject.getClass()));
        }

        // initialize to-many relationships with a fault
        Iterator it = objEntity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.isToMany()) {
                dataObject.writePropertyDirectly(rel.getName(), Fault.getToManyFault());
            }
        }

        getObjectStore().addObject(dataObject);
        dataObject.setDataContext(this);
        dataObject.setPersistenceState(PersistenceState.NEW);
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext and the underlying
     * ObjectStore. This operation also unsets DataContext and ObjectId for each object
     * and changes its state to TRANSIENT.
     */
    public void unregisterObjects(Collection dataObjects) {
        getObjectStore().objectsUnregistered(dataObjects);
    }

    /**
     * "Invalidates" a Collection of DataObject. This operation would remove each object's
     * snapshot from cache and change object's state to HOLLOW. On the next access to this
     * object, it will be refetched.
     */
    public void invalidateObjects(Collection dataObjects) {
        getObjectStore().objectsInvalidated(dataObjects);
    }

    /**
     * Schedules all objects in the collection for deletion on the next commit of this
     * DataContext. Object's persistence state is changed to PersistenceState.DELETED;
     * objects related to this object are processed according to delete rules, i.e.
     * relationships can be unset ("nullify" rule), deletion operation is cascaded
     * (cascade rule).
     * <p>
     * <i>"Nullify" delete rule side effect: </i> passing a collection representing
     * to-many relationship with nullify delete rule may result in objects being removed
     * from collection.
     * </p>
     * 
     * @since 1.2
     */
    public void deleteObjects(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }
        
        // clone object list... this maybe a relationship collection with nullify delete
        // rule, so modifying 
        Iterator it = new ArrayList(objects).iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            deleteObject(object);
        }
    }
    
    /**
     * Schedules an object for deletion on the next commit of this DataContext. Object's
     * persistence state is changed to PersistenceState.DELETED; objects related to this object 
     * are processed according to delete rules, i.e. relationships can be unset ("nullify" rule), 
     * deletion operation is cascaded (cascade rule).
     * 
     * @param object data object that we want to delete.
     * @throws DeleteDenyException if a DENY delete rule is applicable for object deletion.
     * @throws NullPointerException if object is null.
     */
    public void deleteObject(DataObject object) throws DeleteDenyException {
        new DataContextDeleteAction(this).performDelete(object);
    }

    /**
     * Refetches object data for ObjectId. This method is used internally by Cayenne to
     * resolve objects in state <code>PersistenceState.HOLLOW</code>. It can also be
     * used to refresh certain objects.
     * 
     * @throws CayenneRuntimeException if object id doesn't match any records, or if there
     *             is more than one object is fetched.
     */
    public DataObject refetchObject(ObjectId oid) {

        if (oid == null) {
            throw new NullPointerException("Null ObjectId");
        }

        if (oid.isTemporary()) {
            throw new CayenneRuntimeException("Can't refetch ObjectId "
                    + oid
                    + ", as it is a temporary id.");
        }

        synchronized (getObjectStore()) {
            DataObject object = objectStore.getObject(oid);

            // clean up any cached data for this object
            if (object != null) {
                this.invalidateObjects(Collections.singleton(object));
            }
        }

        SelectQuery sel = QueryUtils.selectObjectForId(oid);
        List results = this.performQuery(sel);

        if (results.size() != 1) {
            String msg = (results.size() == 0)
                    ? "Refetch failure: no matching objects found for ObjectId " + oid
                    : "Refetch failure: more than 1 object found for ObjectId "
                            + oid
                            + ". Fetch matched "
                            + results.size()
                            + " objects.";

            throw new CayenneRuntimeException(msg);
        }

        return (DataObject) results.get(0);
    }

    /**
     * Returns a DataNode that should hanlde queries for all DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().lookupDataNode(dataMap);
    }

    /**
     * Reverts any changes that have occurred to objects registered with DataContext.
     */
    public void rollbackChanges() {
        getObjectStore().objectsRolledBack();
    }

    /**
     * Synchronizes object graph with the database. Executes needed insert, update and
     * delete queries (generated internally).
     */
    public void commitChanges() throws CayenneRuntimeException {
        commitChanges(null);
    }

    /**
     * Synchronizes object graph with the database. Executes needed insert, update and
     * delete queries (generated internally).
     * 
     * @param logLevel if logLevel is higher or equals to the level set for QueryLogger,
     *            statements execution will be logged.
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {

        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        // prevent multiple commits occuring simulteneously
        synchronized (getObjectStore()) {
            // is there anything to do?
            if (!this.hasChanges()) {
                return;
            }

            if (isValidatingObjectsOnCommit()) {
                getObjectStore().validateUncommittedObjects();
            }

            DataContextCommitAction worker = new DataContextCommitAction(this);

            try {
                worker.commit(logLevel);
            }
            catch (CayenneException ex) {
                Throwable unwound = Util.unwindException(ex);

                if (unwound instanceof CayenneRuntimeException) {
                    throw (CayenneRuntimeException) unwound;
                }
                else {
                    throw new CayenneRuntimeException("Commit Exception", unwound);
                }
            }
        }
    }

    /**
     * Performs a single database query that does not select rows. Returns an array of
     * update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(Query query) {
        QueryResult result = new QueryResult();
        performQueries(Collections.singletonList(query), result);
        List updateCounts = result.getUpdates(query);

        if (updateCounts == null || updateCounts.isEmpty()) {
            return new int[0];
        }

        int len = updateCounts.size();
        int[] counts = new int[len];

        for (int i = 0; i < len; i++) {
            counts[i] = ((Number) updateCounts.get(i)).intValue();
        }

        return counts;
    }

    /**
     * Performs a named mapped query that does not select rows. Returns an array of update
     * counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName) {
        return performNonSelectingQuery(queryName, Collections.EMPTY_MAP);
    }

    /**
     * Performs a named mapped non-selecting query using a map of parameters. Returns an
     * array of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        // find query...
        Query query = getEntityResolver().getQuery(queryName);
        if (query == null) {
            throw new CayenneRuntimeException("There is no saved query for name '"
                    + queryName
                    + "'.");
        }

        if (parameters != null
                && !parameters.isEmpty()
                && query instanceof ParameterizedQuery) {
            query = ((ParameterizedQuery) query).createQuery(parameters);
        }

        return performNonSelectingQuery(query);
    }

    /**
     * Performs a single database select query returning result as a ResultIterator.
     * Returned ResultIterator will provide access to DataRows.
     */
    public ResultIterator performIteratedQuery(GenericSelectQuery query)
            throws CayenneException {

        IteratedSelectObserver observer = new IteratedSelectObserver();
        observer.setLoggingLevel(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
    }

    /**
     * Delegates queries execution to parent QueryEngine. If there are select queries that
     * require prefetching relationships, will create additional queries to perform
     * necessary prefetching.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        // note - use external transaction for iterated queries;
        // other types of transactions won't be safe in this case
        Transaction transaction = (observer.isIteratedResult())
                ? Transaction.externalTransaction(getParentDataDomain()
                        .getTransactionDelegate())
                : getParentDataDomain().createTransaction();

        transaction.performQueries(this, queries, observer);
    }

    /**
     * Delegates queries execution to parent QueryEngine.
     * 
     * @since 1.1
     */
    public void performQueries(
            Collection queries,
            OperationObserver resultConsumer,
            Transaction transaction) {

        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        DataContextDelegate localDelegate = nonNullDelegate();
        List finalQueries = new ArrayList(queries.size());

        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Object query = it.next();

            if (query instanceof GenericSelectQuery) {
                GenericSelectQuery genericSelect = (GenericSelectQuery) query;

                // filter via a delegate
                GenericSelectQuery filteredSelect = localDelegate.willPerformSelect(
                        this,
                        genericSelect);

                // suppressed by the delegate
                if (filteredSelect != null) {
                    finalQueries.add(filteredSelect);
                }
            }
            else {
                finalQueries.add(query);
            }
        }

        this.getParent().performQueries(finalQueries, resultConsumer, transaction);
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships for a list
     * of DataObjects in the most optimized way (preferrably in a single query per
     * relationship).
     * <p>
     * <i>WARNING: Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed eventually. </i>
     * </p>
     */
    public void prefetchRelationships(SelectQuery query, List objects) {
        Collection prefetches = query.getPrefetches();

        if (objects == null || objects.size() == 0 || prefetches.size() == 0) {
            return;
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(query);
        Iterator prefetchesIt = prefetches.iterator();
        while (prefetchesIt.hasNext()) {
            String prefetchKey = (String) prefetchesIt.next();
            if (prefetchKey.indexOf(Entity.PATH_SEPARATOR) >= 0) {
                throw new CayenneRuntimeException("Only one-step relationships are "
                        + "supported at the moment, this will be fixed soon. "
                        + "Unsupported path : "
                        + prefetchKey);
            }

            ObjRelationship relationship = (ObjRelationship) entity
                    .getRelationship(prefetchKey);
            if (relationship == null) {
                throw new CayenneRuntimeException("Invalid relationship: " + prefetchKey);
            }

            if (relationship.isToMany()) {
                throw new CayenneRuntimeException(
                        "Only to-one relationships are supported at the moment. "
                                + "Can't prefetch to-many: "
                                + prefetchKey);
            }

            PrefetchHelper.resolveToOneRelations(this, objects, prefetchKey);
        }

    }

    /**
     * Performs a single selecting query. If if query is a SelectQuery that require
     * prefetching relationships, will create additional queries to perform necessary
     * prefetching. Various query setting control the behavior of this method and the
     * results returned:
     * <ul>
     * <li>Query caching policy defines whether the results are retrieved from cache or
     * fetched from the database. Note that queries that use caching must have a name that
     * is used as a caching key.</li>
     * <li>Query refreshing policy controls whether to refresh existing data objects and
     * ignore any cached values.</li>
     * <li>Query data rows policy defines whether the result should be returned as
     * DataObjects or DataRows.</li>
     * </ul>
     * 
     * @return A list of DataObjects or a DataRows, depending on the value returned by
     *         {@link GenericSelectQuery#isFetchingDataRows()}.
     */
    public List performQuery(GenericSelectQuery query) {
        return new DataContextSelectAction(this).performQuery(query);
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param refresh A flag that determines whether refresh is required in case a query
     *            uses caching.
     * @since 1.1
     */
    public List performQuery(String queryName, boolean refresh) {
        return performQuery(queryName, Collections.EMPTY_MAP, refresh);
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param parameters A map of parameters to use with stored query.
     * @param refresh A flag that determines whether refresh is required in case a query
     *            uses caching.
     * @since 1.1
     */
    public List performQuery(String queryName, Map parameters, boolean refresh) {
        // find query...
        Query query = getEntityResolver().getQuery(queryName);
        if (query == null) {
            throw new CayenneRuntimeException("There is no saved query for name '"
                    + queryName
                    + "'.");
        }

        // for SelectQuery we must always run parameter substitution as the query
        // in question might have unbound values in the qualifier... that's a bit
        // inefficient... any better ideas to determine whether we can skip parameter
        // processing?

        // another side effect from NOT substituting parameters is that caching key of the
        // final query will be that of the original query... thus parameters vs. no
        // paramete will result in inconsistent caching behavior.

        if (query instanceof SelectQuery) {
            SelectQuery select = (SelectQuery) query;
            if (select.getQualifier() != null) {
                query = select.createQuery(parameters != null
                        ? parameters
                        : Collections.EMPTY_MAP);
            }
        }
        else if (parameters != null
                && !parameters.isEmpty()
                && query instanceof ParameterizedQuery) {
            query = ((ParameterizedQuery) query).createQuery(parameters);
        }

        if (!(query instanceof GenericSelectQuery)) {
            throw new CayenneRuntimeException("Query for name '"
                    + queryName
                    + "' is not a GenericSelectQuery: "
                    + query);
        }

        return new DataContextSelectAction(this).performQuery((GenericSelectQuery) query, query
                .getName(), refresh);
    }



    // serialization support
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it. Then when deserialization happens, we can get back the DataDomain
        // by name,
        // from the shared configuration (which will either load it if need be, or return
        // an existing one.

        if (this.parent == null && this.lazyInitParentDomainName != null) {
            out.writeObject(lazyInitParentDomainName);
        }
        else if (this.parent instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.parent;
            out.writeObject(domain.getName());
        }
        else {
            // Hope that whatever this.parent is, that it is Serializable
            out.writeObject(this.parent);
        }

        // Serialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            out.writeObject(objectStore.getDataRowCache());
        }
    }

    //serialization support
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        // 1. read non-transient properties
        in.defaultReadObject();

        // 2. read parent or its name
        Object value = in.readObject();
        if (value instanceof QueryEngine) {
            // A real QueryEngine object - use it
            this.parent = (QueryEngine) value;
        }
        else if (value instanceof String) {
            // The name of a DataDomain - use it
            this.lazyInitParentDomainName = (String) value;
        }
        else {
            throw new CayenneRuntimeException(
                    "Parent attribute of DataContext was neither a QueryEngine nor "
                            + "the name of a valid DataDomain:"
                            + value);
        }

        // 3. Deserialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            DataRowStore cache = (DataRowStore) in.readObject();
            objectStore.setDataRowCache(cache);
        }

        // CayenneDataObjects have a transient datacontext
        // because at deserialize time the datacontext may need to be different
        // than the one at serialize time (for programmer defined reasons).
        // So, when a dataobject is resurrected because it's datacontext was
        // serialized, it will then set the objects datacontext to the correctone
        // If deserialized "otherwise", it will not have a datacontext (good)

        synchronized (getObjectStore()) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject object = (DataObject) it.next();
                object.setDataContext(this);
            }
        }
    }

    /**
     * Returns EntityResolver object used to resolve and route queries.
     */
    public EntityResolver getEntityResolver() {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().getEntityResolver();
    }

    /**
     * Sets default for posting transaction events by new DataContexts.
     */
    public static void setTransactionEventsEnabledDefault(boolean flag) {
        transactionEventsEnabledDefault = flag;
    }

    /**
     * Enables or disables posting of transaction events by this DataContext.
     */
    public void setTransactionEventsEnabled(boolean flag) {
        this.transactionEventsEnabled = flag;
    }

    public boolean isTransactionEventsEnabled() {
        return this.transactionEventsEnabled;
    }

    /**
     * Returns <code>true</code> if the ObjectStore uses shared cache of a parent
     * DataDomain.
     * 
     * @since 1.1
     */
    public boolean isUsingSharedSnapshotCache() {
        return usingSharedSnaphsotCache;
    }

    /**
     * Returns whether this DataContext performs object validation before commit is
     * executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this DataContext should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    public Collection getDataMaps() {
        return (parent != null) ? parent.getDataMaps() : Collections.EMPTY_LIST;
    }

    void fireWillCommit() {
        // post event: WILL_COMMIT
        if (this.transactionEventsEnabled) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.WILL_COMMIT);
        }
    }

    void fireTransactionRolledback() {
        // post event: DID_ROLLBACK
        if ((this.transactionEventsEnabled)) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.DID_ROLLBACK);
        }
    }

    void fireTransactionCommitted() {
        // post event: DID_COMMIT
        if ((this.transactionEventsEnabled)) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.DID_COMMIT);
        }
    }
}