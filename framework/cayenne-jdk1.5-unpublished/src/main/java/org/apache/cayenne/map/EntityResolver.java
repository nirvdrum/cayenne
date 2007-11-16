/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.reflect.SingletonFaultFactory;
import org.apache.cayenne.reflect.generic.DataObjectDescriptorFactory;
import org.apache.cayenne.reflect.pojo.EnhancedPojoDescriptorFactory;
import org.apache.cayenne.reflect.valueholder.ValueHolderDescriptorFactory;
import org.apache.cayenne.util.Util;
import org.apache.commons.collections.collection.CompositeCollection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a virtual shared namespace for zero or more DataMaps. Unlike DataMap,
 * EntityResolver is intended to work as a runtime container of mapping. DataMaps can be
 * added or removed dynamically at runtime.
 * <p>
 * EntityResolver is thread-safe.
 * </p>
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class EntityResolver implements MappingNamespace, Serializable {

    static final Object DUPLICATE_MARKER = new Object();

    protected static final Log logger = LogFactory.getLog(EntityResolver.class);

    protected boolean indexedByClass;

    protected transient Map queryCache;
    protected transient Map embeddableCache;
    protected transient Map dbEntityCache;
    protected transient Map objEntityCache;
    protected transient Map procedureCache;
    protected List maps;
    protected transient Map entityInheritanceCache;
    protected EntityResolver clientEntityResolver;

    // must be transient, as resolver may get deserialized in another VM, and descriptor
    // recompilation will be desired.
    protected transient ClassDescriptorMap classDescriptorMap;

    // callbacks are not serializable
    protected transient LifecycleCallbackRegistry callbackRegistry;

    /**
     * Creates new EntityResolver.
     */
    public EntityResolver() {
        this.indexedByClass = true;
        this.maps = new ArrayList();
        this.embeddableCache = new HashMap();
        this.queryCache = new HashMap();
        this.dbEntityCache = new HashMap();
        this.objEntityCache = new HashMap();
        this.procedureCache = new HashMap();
        this.entityInheritanceCache = new HashMap();
    }

    /**
     * Creates new EntityResolver that indexes a collection of DataMaps.
     */
    public EntityResolver(Collection dataMaps) {
        this();
        this.maps.addAll(dataMaps); // Take a copy
        this.constructCache();
    }

    /**
     * Compiles internal callback registry.
     */
    synchronized void initCallbacks() {
        if (callbackRegistry == null) {
            LifecycleCallbackRegistry callbackRegistry = new LifecycleCallbackRegistry(
                    this);

            // load default callbacks
            Iterator maps = this.maps.iterator();
            while (maps.hasNext()) {
                DataMap map = (DataMap) maps.next();
                Iterator listeners = map.getDefaultEntityListeners().iterator();
                while (listeners.hasNext()) {
                    EntityListener listener = (EntityListener) listeners.next();
                    Object listenerInstance = createListener(listener);

                    CallbackDescriptor[] callbacks = listener
                            .getCallbackMap()
                            .getCallbacks();
                    for (int i = 0; i < callbacks.length; i++) {

                        Iterator callbackMethods = callbacks[i]
                                .getCallbackMethods()
                                .iterator();
                        while (callbackMethods.hasNext()) {
                            String method = (String) callbackMethods.next();

                            // note that callbacks[i].getCallbackType() == i
                            callbackRegistry.addDefaultListener(
                                    i,
                                    listenerInstance,
                                    method);
                        }
                    }
                }
            }

            // load entity callbacks
            Iterator entities = getObjEntities().iterator();
            while (entities.hasNext()) {
                ObjEntity entity = (ObjEntity) entities.next();
                Class entityClass = entity.getJavaClass();

                // external listeners go first, entity's own callbacks go next
                Iterator entityListeners = entity.getEntityListeners().iterator();
                while (entityListeners.hasNext()) {
                    EntityListener listener = (EntityListener) entityListeners.next();
                    Object listenerInstance = createListener(listener);

                    CallbackDescriptor[] callbacks = listener
                            .getCallbackMap()
                            .getCallbacks();
                    for (int i = 0; i < callbacks.length; i++) {

                        Iterator callbackMethods = callbacks[i]
                                .getCallbackMethods()
                                .iterator();
                        while (callbackMethods.hasNext()) {
                            String method = (String) callbackMethods.next();

                            // note that callbacks[i].getCallbackType() == i
                            callbackRegistry.addListener(
                                    i,
                                    entityClass,
                                    listenerInstance,
                                    method);
                        }
                    }
                }

                CallbackDescriptor[] callbacks = entity.getCallbackMap().getCallbacks();
                for (int i = 0; i < callbacks.length; i++) {
                    Iterator callbackMethods = callbacks[i]
                            .getCallbackMethods()
                            .iterator();
                    while (callbackMethods.hasNext()) {
                        String method = (String) callbackMethods.next();

                        // note that callbacks[i].getCallbackType() == i
                        callbackRegistry.addListener(i, entityClass, method);
                    }
                }
            }

            this.callbackRegistry = callbackRegistry;
        }
    }

    /**
     * Creates a listener instance.
     */
    private Object createListener(EntityListener listener) {
        Class listenerClass;

        try {
            listenerClass = Util.getJavaClass(listener.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Invalid listener class: "
                    + listener.getClassName(), e);
        }

        try {
            return listenerClass.newInstance();
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Listener class "
                    + listener.getClassName()
                    + " default constructor call failed", e);
        }
    }

    /**
     * Returns a {@link LifecycleCallbackRegistry} for handling callbacks. Registry is
     * lazily initialized on first call.
     * 
     * @since 3.0
     */
    public LifecycleCallbackRegistry getCallbackRegistry() {
        if (callbackRegistry == null) {
            initCallbacks();
        }

        return callbackRegistry;
    }

    /**
     * Sets a lifecycle callbacks registry of the EntityResolver. Users rarely if ever
     * need to call this method as Cayenne would instantiate a registry itself as needed
     * based on mapped configuration.
     * 
     * @since 3.0
     */
    public void setCallbackRegistry(LifecycleCallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    /**
     * Returns ClientEntityResolver with mapping information that only includes entities
     * available on CWS Client Tier.
     * 
     * @since 1.2
     */
    public EntityResolver getClientEntityResolver() {

        if (clientEntityResolver == null) {

            synchronized (this) {

                if (clientEntityResolver == null) {

                    EntityResolver resolver = new ClientEntityResolver();

                    // translate to client DataMaps
                    Iterator it = getDataMaps().iterator();
                    while (it.hasNext()) {
                        DataMap map = (DataMap) it.next();
                        DataMap clientMap = map.getClientDataMap(this);

                        if (clientMap != null) {
                            resolver.addDataMap(clientMap);
                        }
                    }

                    clientEntityResolver = resolver;
                }
            }
        }

        return clientEntityResolver;
    }

    /**
     * Returns all DbEntities.
     */
    public Collection getDbEntities() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getDbEntities());
        }

        return c;
    }

    public Collection getObjEntities() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getObjEntities());
        }

        return c;
    }

    public Collection getProcedures() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getProcedures());
        }

        return c;
    }

    public Collection getQueries() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getQueries());
        }

        return c;
    }

    public DbEntity getDbEntity(String name) {
        return _lookupDbEntity(name);
    }

    public ObjEntity getObjEntity(String name) {
        return _lookupObjEntity(name);
    }

    public Procedure getProcedure(String name) {
        return lookupProcedure(name);
    }

    public Query getQuery(String name) {
        return lookupQuery(name);
    }

    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
        Embeddable result = (Embeddable) embeddableCache.get(className);

        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (Embeddable) embeddableCache.get(className);
        }

        return result;
    }

    /**
     * Returns ClassDescriptor for the ObjEntity matching the name. Returns null if no
     * matching entity exists.
     * 
     * @since 1.2
     */
    public synchronized ClassDescriptor getClassDescriptor(String entityName) {
        if (entityName == null) {
            throw new IllegalArgumentException("Null entityName");
        }

        return getClassDescriptorMap().getDescriptor(entityName);
    }

    public synchronized void addDataMap(DataMap map) {
        if (!maps.contains(map)) {
            maps.add(map);
            map.setNamespace(this);
            clearCache();
        }
    }

    /**
     * Removes all entity mappings from the cache. Cache can be rebuilt either explicitly
     * by calling <code>constructCache</code>, or on demand by calling any of the
     * <code>lookup...</code> methods.
     */
    public synchronized void clearCache() {
        queryCache.clear();
        dbEntityCache.clear();
        objEntityCache.clear();
        procedureCache.clear();
        entityInheritanceCache.clear();
        clientEntityResolver = null;
    }

    /**
     * Creates caches of DbEntities by ObjEntity, DataObject class, and ObjEntity name
     * using internal list of maps.
     */
    protected synchronized void constructCache() {
        clearCache();

        // rebuild index

        // index DbEntities separatly and before ObjEntities to avoid infinite loops when
        // looking up DbEntities during ObjEntity index op
        Iterator mapIterator0 = maps.iterator();
        while (mapIterator0.hasNext()) {
            DataMap map = (DataMap) mapIterator0.next();

            Iterator dbEntities = map.getDbEntities().iterator();
            while (dbEntities.hasNext()) {
                DbEntity de = (DbEntity) dbEntities.next();
                dbEntityCache.put(de.getName(), de);
            }
        }

        Iterator mapIterator1 = maps.iterator();
        while (mapIterator1.hasNext()) {
            DataMap map = (DataMap) mapIterator1.next();

            // index ObjEntities
            Iterator objEntities = map.getObjEntities().iterator();
            while (objEntities.hasNext()) {
                ObjEntity oe = (ObjEntity) objEntities.next();

                // index by name
                objEntityCache.put(oe.getName(), oe);

                // index by class.. use class name as a key to avoid class loading here...
                if (indexedByClass) {
                    String className = oe.getJavaClassName();
                    if (className == null) {
                        continue;
                    }

                    String classKey = classKey(className);

                    // allow duplicates, but put a special marker indicating that this
                    // entity can't be looked up by class
                    Object existing = objEntityCache.get(classKey);
                    if (existing != null) {

                        if (existing != DUPLICATE_MARKER) {
                            objEntityCache.put(classKey, DUPLICATE_MARKER);
                        }
                    }
                    else {
                        objEntityCache.put(classKey, oe);
                    }
                }
            }

            // index stored procedures
            Iterator procedures = map.getProcedures().iterator();
            while (procedures.hasNext()) {
                Procedure proc = (Procedure) procedures.next();
                procedureCache.put(proc.getName(), proc);
            }

            // index queries
            Iterator queries = map.getQueries().iterator();
            while (queries.hasNext()) {
                Query query = (Query) queries.next();
                String name = query.getName();
                Object existingQuery = queryCache.put(name, query);

                if (existingQuery != null && query != existingQuery) {
                    throw new CayenneRuntimeException("More than one Query for name"
                            + name);
                }
            }
        }

        // restart the map iterator to index inheritance
        Iterator mapIterator2 = maps.iterator();
        while (mapIterator2.hasNext()) {
            DataMap map = (DataMap) mapIterator2.next();

            // index ObjEntity inheritance
            Iterator objEntities = map.getObjEntities().iterator();
            while (objEntities.hasNext()) {
                ObjEntity oe = (ObjEntity) objEntities.next();

                // build inheritance tree... include nodes that
                // have no children to avoid uneeded cache rebuilding on lookup...
                EntityInheritanceTree node = (EntityInheritanceTree) entityInheritanceCache
                        .get(oe.getName());
                if (node == null) {
                    node = new EntityInheritanceTree(oe);
                    entityInheritanceCache.put(oe.getName(), node);
                }

                String superOEName = oe.getSuperEntityName();
                if (superOEName != null) {
                    EntityInheritanceTree superNode = (EntityInheritanceTree) entityInheritanceCache
                            .get(superOEName);

                    if (superNode == null) {
                        // do direct entity lookup to avoid recursive cache rebuild
                        ObjEntity superOE = (ObjEntity) objEntityCache.get(superOEName);
                        if (superOE != null) {
                            superNode = new EntityInheritanceTree(superOE);
                            entityInheritanceCache.put(superOEName, superNode);
                        }
                        else {
                            // bad mapping? Or most likely some classloader issue
                            logger.warn("No super entity mapping for '"
                                    + superOEName
                                    + "'");
                            continue;
                        }
                    }

                    superNode.addChildNode(node);
                }
            }
        }
    }

    /**
     * Returns a DataMap matching the name.
     */
    public synchronized DataMap getDataMap(String mapName) {
        if (mapName == null) {
            return null;
        }

        Iterator it = maps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            if (mapName.equals(map.getName())) {
                return map;
            }
        }

        return null;
    }

    public synchronized void setDataMaps(Collection maps) {
        this.maps.clear();
        this.maps.addAll(maps);
        clearCache();
    }

    /**
     * Returns an unmodifiable collection of DataMaps.
     */
    public Collection getDataMaps() {
        return Collections.unmodifiableList(maps);
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity that
     * services the specified class
     * 
     * @return the required DbEntity, or null if none matches the specifier
     * @deprecated since 3.0 - lookup DbEntity via ObjEntity instead.
     */
    public synchronized DbEntity lookupDbEntity(Class aClass) {
        ObjEntity oe = lookupObjEntity(aClass);
        return oe != null ? oe.getDbEntity() : null;
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity that
     * services the specified data Object
     * 
     * @return the required DbEntity, or null if none matches the specifier
     * @deprecated since 3.0 - lookup DbEntity via ObjEntity instead.
     */
    public synchronized DbEntity lookupDbEntity(Persistent dataObject) {
        return lookupDbEntity(dataObject.getClass());
    }

    /**
     * Returns EntityInheritanceTree representing inheritance hierarchy that starts with a
     * given ObjEntity as root, and includes all its subentities. If ObjEntity has no
     * known subentities, null is returned.
     */
    public EntityInheritanceTree lookupInheritanceTree(ObjEntity entity) {

        EntityInheritanceTree tree = (EntityInheritanceTree) entityInheritanceCache
                .get(entity.getName());

        if (tree == null) {
            // since we keep inheritance trees for all entities, null means
            // unknown entity...

            // rebuild cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            tree = (EntityInheritanceTree) entityInheritanceCache.get(entity.getName());
        }

        // don't return "trivial" trees
        return (tree == null || tree.getChildrenCount() == 0) ? null : tree;
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that
     * maps to the services the specified class
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(Class aClass) {
        if (!indexedByClass) {
            throw new CayenneRuntimeException("Class index is disabled.");
        }

        return _lookupObjEntity(classKey(aClass.getName()));
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that
     * services the specified data Object
     * 
     * @return the required ObjEntity, or null if none matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(Object object) {
        if (object instanceof ObjEntity) {
            return (ObjEntity) object;
        }

        if (object instanceof Persistent) {
            ObjectId id = ((Persistent) object).getObjectId();
            if (id != null) {
                return _lookupObjEntity(id.getEntityName());
            }
        }
        else if (object instanceof Class) {
            return lookupObjEntity((Class) object);
        }

        return lookupObjEntity(object.getClass());
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that
     * maps to the services the class with the given name
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     * @deprecated since 3.0 - use getObjEntity() instead.
     */
    public synchronized ObjEntity lookupObjEntity(String entityName) {
        return _lookupObjEntity(entityName);
    }

    public Procedure lookupProcedure(Query q) {
        return q.getMetaData(this).getProcedure();
    }

    public Procedure lookupProcedure(String procedureName) {

        Procedure result = (Procedure) procedureCache.get(procedureName);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (Procedure) procedureCache.get(procedureName);
        }

        return result;
    }

    /**
     * Returns a named query or null if no query exists for a given name.
     */
    public synchronized Query lookupQuery(String name) {
        Query result = (Query) queryCache.get(name);

        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (Query) queryCache.get(name);
        }
        return result;
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            clearCache();
        }
    }

    public boolean isIndexedByClass() {
        return indexedByClass;
    }

    public void setIndexedByClass(boolean b) {
        indexedByClass = b;
    }

    /**
     * Generates a map key for the object class.
     * 
     * @since 3.0
     */
    protected String classKey(String className) {
        // need to ensure that there is no conflict with entity names... I guess such
        // prefix is enough to guarantee that:
        return "^cl^" + className;
    }

    /**
     * Internal usage only - provides the type-unsafe implementation which services the
     * four typesafe public lookupDbEntity methods Looks in the DataMap's that this object
     * was created with for the ObjEntity that maps to the specified object. Object may be
     * a Entity name, ObjEntity, DataObject class (Class object for a class which
     * implements the DataObject interface), or a DataObject instance itself
     * 
     * @return the required DbEntity, or null if none matches the specifier
     */
    protected DbEntity _lookupDbEntity(Object object) {
        if (object instanceof DbEntity) {
            return (DbEntity) object;
        }

        Object result = dbEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = dbEntityCache.get(object);
        }

        if (result == DUPLICATE_MARKER) {
            throw new CayenneRuntimeException(
                    "Can't perform lookup. There is more than one DbEntity mapped to "
                            + object);
        }

        return (DbEntity) result;
    }

    /**
     * Internal usage only - provides the type-unsafe implementation which services the
     * three typesafe public lookupObjEntity methods Looks in the DataMap's that this
     * object was created with for the ObjEntity that maps to the specified object. Object
     * may be a Entity name, DataObject instance or DataObject class (Class object for a
     * class which implements the DataObject interface)
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    protected ObjEntity _lookupObjEntity(String key) {

        Object result = objEntityCache.get(key);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = objEntityCache.get(key);
        }

        if (result == DUPLICATE_MARKER) {
            throw new CayenneRuntimeException(
                    "Can't perform lookup. There is more than one ObjEntity mapped to "
                            + key);
        }

        return (ObjEntity) result;
    }

    /**
     * Returns an object that compiles and stores {@link ClassDescriptor} instances for
     * all entities.
     * 
     * @since 3.0
     */
    public ClassDescriptorMap getClassDescriptorMap() {
        if (classDescriptorMap == null) {
            ClassDescriptorMap classDescriptorMap = new ClassDescriptorMap(this);
            FaultFactory faultFactory = new SingletonFaultFactory();

            // add factories in reverse of the desired chain order
            classDescriptorMap.addFactory(new ValueHolderDescriptorFactory(
                    classDescriptorMap));
            classDescriptorMap.addFactory(new EnhancedPojoDescriptorFactory(
                    classDescriptorMap,
                    faultFactory));
            classDescriptorMap.addFactory(new DataObjectDescriptorFactory(
                    classDescriptorMap,
                    faultFactory));

            // since ClassDescriptorMap is not synchronized, we need to prefill it with
            // entity proxies here.
            Iterator maps = this.maps.iterator();
            while (maps.hasNext()) {
                DataMap map = (DataMap) maps.next();
                Iterator entities = map.getObjEntityMap().keySet().iterator();
                while (entities.hasNext()) {
                    classDescriptorMap.getDescriptor((String) entities.next());
                }
            }

            this.classDescriptorMap = classDescriptorMap;
        }

        return classDescriptorMap;
    }
}