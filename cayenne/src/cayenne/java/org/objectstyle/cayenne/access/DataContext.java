/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
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
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.access.util.PrefetchHelper;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.access.util.RelationshipDataSource;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.access.util.DataRowUtils;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * User-level Cayenne access class. Provides isolated object view of
 * the datasource to the application code. Normal use pattern is to
 * create one DataContext per session (whatever session may mean in
 * a given application).
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public class DataContext implements QueryEngine, Serializable {
    private static Logger logObj = Logger.getLogger(DataContext.class);

    // noop delegate 
    private static final DataContextDelegate defaultDelegate =
        new DataContextDelegate() {

        public GenericSelectQuery willPerformSelect(
            DataContext context,
            GenericSelectQuery query) {
            return query;
        }

        public void snapshotChangedInDataRowStore(
            DataObject object,
            DataRow snapshotInStore) {
            // noop
        }
    };

    // DataContext events
    public static final EventSubject WILL_COMMIT =
        EventSubject.getSubject(DataContext.class, "DataContextWillCommit");
    public static final EventSubject DID_COMMIT =
        EventSubject.getSubject(DataContext.class, "DataContextDidCommit");
    public static final EventSubject DID_ROLLBACK =
        EventSubject.getSubject(DataContext.class, "DataContextDidRollback");

    // event posting default for new DataContexts
    private static boolean transactionEventsEnabledDefault;

    // enable/disable event handling for individual instances
    private boolean transactionEventsEnabled;

    // Set of DataContextDelegates to be notified.
    private DataContextDelegate delegate;

    private List flattenedInserts = new ArrayList();
    private List flattenedDeletes = new ArrayList();

    protected ObjectStore objectStore;

    protected transient QueryEngine parent;

    /**
     * Stores the name of parent DataDomain. Used to defer initialization 
     * of the parent QueryEngine after deserialization. This helps
     * avoid an issue with certain servlet engines (e.g. Tomcat) where
     * HttpSessions with DataContext's are deserialized at startup
     * before Cayenne stack is fully initialized.
     */
    protected transient String lazyInitParentDomainName;

    protected transient ToManyListDataSource relationshipDataSource;

    /**
     * Convenience method to create a new instance of
     * DataContext based on default domain. If more
     * than one domain exists, createDataContext(String)
     * must be used.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
      * A factory method of DataObjects. Uses Configuration ClassLoader to
      * instantiate a new instance of DataObject of a given class.
      */
    private static final DataObject newDataObject(String className) throws Exception {
        return (DataObject) Configuration
            .getResourceLoader()
            .loadClass(className)
            .newInstance();
    }

    /**
     * Convenience method to create a new instance of
     * DataContext based on a named domain.
     * If there is no domain matching the name,
     * an exception is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    public DataContext() {
        this(null);
    }

    /**
     * Creates new DataContext and initializes it
     * with the parent QueryEngine. Normally parent is an
     * instance of DataDomain. DataContext will use parent
     * to execute database queries, updates, and access
     * DataMap objects.
     */
    public DataContext(QueryEngine parent) {
        setParent(parent);

        DataRowStore snapshotCache = null;
        if (parent != null) {
            snapshotCache = ((DataDomain) parent).getSnapshotCache();
        }

        this.objectStore = new ObjectStore(snapshotCache);
        this.relationshipDataSource = new RelationshipDataSource(this);
        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
    }

    /**
     * Initializes parent if deserialization left it uninitialized.
     */
    private final void awakeFromDeserialization() {
        if (parent == null && lazyInitParentDomainName != null) {

            this.parent =
                Configuration.getSharedConfiguration().getDomain(
                    lazyInitParentDomainName);

            if (parent instanceof DataDomain) {
                this.objectStore.setDataRowCache(
                    ((DataDomain) parent).getSnapshotCache());
            }
        }
    }

    /** Returns parent QueryEngine object. */
    public QueryEngine getParent() {
        awakeFromDeserialization();
        return parent;
    }

    /**
     * Sets parent QueryEngine of this DataContext.
     */
    public void setParent(QueryEngine parent) {
        this.parent = parent;
    }

    /**
     * Sets a DataContextDelegate for this context.
     */
    public void setDelegate(DataContextDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a delegate currently associated with this DataContext.
     */
    public DataContextDelegate getDelegate() {
        return delegate;
    }

    /**
     * Returns delegate instance if it is initialized, or a shared
     * noop implementation if not. 
     * 
     * @since 1.1
     */
    DataContextDelegate nonNullDelegate() {
        return (delegate != null) ? delegate : DataContext.defaultDelegate;
    }

    /**
     * @deprecated Since 1.1 all SnapshotManager methods are static
     */
    public SnapshotManager getSnapshotManager() {
        return new SnapshotManager(relationshipDataSource);
    }

    public ToManyListDataSource getRelationshipDataSource() {
        return this.relationshipDataSource;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        awakeFromDeserialization();
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified,
     * deleted or new objects registered with this DataContext,
     * <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        // TODO: the logic checking flattened relationships should 
        // most likely be pushed to the ObjectStore

        return !this.getFlattenedInserts().isEmpty()
            || !this.getFlattenedDeletes().isEmpty()
            || getObjectStore().hasChanges();
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.NEW
     */
    public Collection newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.DELETED
     */
    public Collection deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.MODIFIED
     */
    public Collection modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns an object for a given ObjectId.
     * If object is not registered with this context,
     * a "hollow" object fault is created, registered, 
     * and returned to the caller.
     */
    public DataObject registeredObject(ObjectId oid) {
        // must synchronize on ObjectStore since we must read and write atomically
        synchronized (getObjectStore()) {
            DataObject obj = objectStore.getObject(oid);
            if (obj == null) {
                try {
                    obj = DataContext.newDataObject(oid.getObjClass().getName());
                }
                catch (Exception ex) {
                    String entity =
                        (oid != null)
                            ? getEntityResolver()
                                .lookupObjEntity(oid.getObjClass())
                                .getName()
                            : null;
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

        Map attrMap = entity.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute objAttr = (ObjAttribute) attrMap.get(attrName);
            //processing compound attributes correctly
            snapshot.put(
                objAttr.getDbAttributePath(),
                anObject.readPropertyDirectly(attrName));
        }

        Map relMap = entity.getRelationshipMap();
        Iterator itr = relMap.keySet().iterator();
        while (itr.hasNext()) {
            String relName = (String) itr.next();
            ObjRelationship rel = (ObjRelationship) relMap.get(relName);

            // to-many will be handled on the other side
            if (rel.isToMany()) {
                continue;
            }

            if (rel.isToDependentEntity()) {
                continue;
            }

            DataObject target = (DataObject) anObject.readPropertyDirectly(relName);
            if (target == null) {
                continue;
            }

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
            // put only thise that do not exist in the map
            Iterator itm = thisIdParts.keySet().iterator();
            while (itm.hasNext()) {
                Object nextKey = itm.next();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, thisIdParts.get(nextKey));
                }
            }
        }
        return snapshot;
    }

    /** 
     * Takes a snapshot of current object state. 
     * 
     * @deprecated Since 1.1 use "currentSnapshot"
     */
    public Map takeObjectSnapshot(DataObject anObject) {
        return currentSnapshot(anObject);
    }

    /**
     * Converts a list of data rows to a list of DataObjects. 
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(ObjEntity entity, List dataRows, boolean refresh) {

        if (dataRows == null && dataRows.size() == 0) {
            return new ArrayList(1);
        }

        // do a sanity check on ObjEntity... if it's DbEntity has no PK defined,
        // we can't build a valid ObjectId
        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException(
                "ObjEntity '" + entity.getName() + "' has no DbEntity.");
        }

        if (dbEntity.getPrimaryKey().size() == 0) {
            throw new CayenneRuntimeException(
                "Can't create ObjectId for '"
                    + entity.getName()
                    + "'. Reason: DbEntity '"
                    + dbEntity.getName()
                    + "' has no Primary Key defined.");
        }

        List results = new ArrayList(dataRows.size());
        Iterator it = dataRows.iterator();
        while (it.hasNext()) {
            DataRow dataRow = (DataRow) it.next();
            ObjectId anId = dataRow.createObjectId(entity);

            // this will create a HOLLOW object if it is not registered yet
            DataObject object = registeredObject(anId);

            // deal with object state
            if (refresh) {
                // make all COMMITTED objects HOLLOW
                if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                    // TODO: temporary hack - should do lazy conversion - make an object HOLLOW
                    // and resolve on first read... unfortunately lots of other things break...

                    DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                    // object.setPersistenceState(PersistenceState.HOLLOW);
                }
                // merge all MODIFIED objects immediately 
                else if (object.getPersistenceState() == PersistenceState.MODIFIED) {
                    DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                }
                // TODO: temporary hack - should do lazy conversion - keep an object HOLLOW
                // and resolve on first read...unfortunately lots of other things break...
                else if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                    DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                }
            }
            // TODO: temporary hack - this else clause must go... unfortunately lots of other things break
            // at the moment...
            else {
                if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                    DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                }
            }

            object.setSnapshotVersion(dataRow.getVersion());
            object.fetchFinished();
            results.add(object);
        }

        // now deal with snapshots 
        getObjectStore().snapshotsUpdatedForObjects(results, dataRows, refresh);

        return results;
    }

    /**
     * Creates and returns a DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     *
     * <p>Internally this method calls {@link 
     * #objectsFromDataRows(org.objectststyle.cayenne.map.ObjEntity,java.util.List,boolean)
     * objectsFromDataRows(ObjEntity, List, boolean)}
     * with <code>false</code> "refersh" parameter.</p>
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(Class objectClass, List dataRows, boolean refresh) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);
        return objectsFromDataRows(entity, dataRows, false);
    }

    /**
     * A convenience shortcut to {@link 
     * #objectsFromDataRows(Class,java.util.List,boolean)
     * objectsFromDataRows(Class, List, boolean)}, that allows to easily create an object
     * from a map of values.</p>
     */
    public DataObject objectFromDataRow(
        Class objectClass,
        DataRow dataRow,
        boolean refresh) {
        List list =
            objectsFromDataRows(objectClass, Collections.singletonList(dataRow), refresh);
        return (DataObject) list.get(0);
    }

    /**
      * @deprecated Since 1.1 use {@link 
      * #objectFromDataRow(Class,org.objectstyle.cayenne.access.DataRow,boolean)
      * objectFromDataRow(Class, DataRow, boolean)}.
      */
    public DataObject objectFromDataRow(String entityName, Map dataRow) {
        // backwards compatibility... wrap this in a DataRow
        if (!(dataRow instanceof DataRow)) {
            dataRow = new DataRow(dataRow);
        }

        ObjEntity ent = this.getEntityResolver().lookupObjEntity(entityName);
        List list = objectsFromDataRows(ent, Collections.singletonList(dataRow), false);
        return (DataObject) list.get(0);
    }

    /**
      * @deprecated Since 1.1 use {@link 
      * #objectFromDataRow(Class,org.objectstyle.cayenne.access.DataRow,boolean)
      * objectFromDataRow(Class, DataRow, boolean)}.
      */
    public DataObject objectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {

        // backwards compatibility... wrap this in a DataRow
        if (!(dataRow instanceof DataRow)) {
            dataRow = new DataRow(dataRow);
        }

        List list =
            objectsFromDataRows(objEntity, Collections.singletonList(dataRow), refresh);
        return (DataObject) list.get(0);
    }

    /**
     * Creates and returns a read-only DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     * 
     * @deprecated Since 1.1 This method is not used in Cayenne anymore. Use
     * #objectsFromDataRows(org.objectststyle.cayenne.map.ObjEntity,java.util.List,boolean)
     * objectsFromDataRows(ObjEntity, List, boolean)} instead.
     */
    protected DataObject readOnlyObjectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {

        return this.objectFromDataRow(objEntity, dataRow, refresh);
    }

    /**
     * Instantiates new object and registers it with itself. Object class
     * is determined from ObjEntity. Object class must have a default constructor.
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objEntityName);

        if (entity == null) {
            throw new IllegalArgumentException("Invalid entity name: " + objEntityName);
        }

        String objClassName = entity.getClassName();
        DataObject dobj = null;
        try {
            dobj = DataContext.newDataObject(objClassName);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObject(dobj, objEntityName);
        return dobj;
    }

    /** Registers a new object (that is not yet persistent) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     * @param objEntityName a name of the ObjEntity in the map used to get
     *  persistence information for this object.
     */
    public void registerNewObject(DataObject dataObject, String objEntityName) {
        ObjEntity objEntity = getEntityResolver().lookupObjEntity(objEntityName);
        registerNewObjectWithEntity(dataObject, objEntity);
    }

    /** Registers a new object (that is not yet persistent) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     */
    public void registerNewObject(DataObject dataObject) {
        if (dataObject == null) {
            throw new NullPointerException("Can't register null object.");
        }

        ObjEntity objEntity = getEntityResolver().lookupObjEntity(dataObject);

        // sanity check 
        if (objEntity == null) {
            throw new CayenneRuntimeException(
                "Can't find ObjEntity for DataObject class: "
                    + dataObject.getClass().getName());
        }

        registerNewObjectWithEntity(dataObject, objEntity);
    }

    private void registerNewObjectWithEntity(
        DataObject dataObject,
        ObjEntity objEntity) {
        TempObjectId tempId = new TempObjectId(dataObject.getClass());
        dataObject.setObjectId(tempId);

        // initialize to-many relationships with empty lists
        Iterator it = objEntity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.isToMany()) {
                ToManyList relList =
                    new ToManyList(
                        relationshipDataSource,
                        dataObject.getObjectId(),
                        rel.getName());
                dataObject.writePropertyDirectly(rel.getName(), relList);
            }
        }

        getObjectStore().addObject(dataObject);
        dataObject.setDataContext(this);
        dataObject.setPersistenceState(PersistenceState.NEW);
    }

    /**
     * @deprecated Since 1.1, use 
     * {@link #unregisterObjects(java.util.Collection) unregisterObjects(Collections.singletonList(dataObject))}
     * to invalidate a single object.
     */
    public void unregisterObject(DataObject dataObject) {
        unregisterObjects(Collections.singletonList(dataObject));
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext 
     * and the underlying ObjectStore. This operation also unsets 
     * DataContext and ObjectId for each object and changes its state 
     * to TRANSIENT.
     */
    public void unregisterObjects(Collection dataObjects) {
        getObjectStore().objectsUnregistered(dataObjects);
    }

    /**
     * @deprecated Since 1.1, use 
     * {@link #invalidateObjects(java.util.Collection) invalidateObjects(Collections.singletonList(dataObject))}
     * to invalidate a single object.
     */
    public void invalidateObject(DataObject dataObject) {
        invalidateObjects(Collections.singletonList(dataObject));
    }

    /**
      * "Invalidates" a Collection of DataObject. This operation would remove 
      * each object's snapshot from cache and change object's state to HOLLOW.
      * On the next access to this object, it will be refeched.
      */
    public void invalidateObjects(Collection dataObjects) {
        getObjectStore().objectsInvalidated(dataObjects);
    }

    /**
     * Notifies data context that a registered object need to be deleted on
     * next commit.
     *
     * @param anObject data object that we want to delete.
     */

    public void deleteObject(DataObject anObject) {
        if (anObject.getPersistenceState() == PersistenceState.DELETED) {
            //Drop out... we might be about to get into a horrible
            // recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return;
        }

        //Save the current state in case of a deny, in which case it should be reset.
        //We cannot delay setting it to deleted, as Cascade deletes might cause
        // recursion, and the "deleted" state is the best way we have of noticing that and bailing out (see above)
        int oldState = anObject.getPersistenceState();

        //TODO - figure out what to do when an object is still in
        //PersistenceState.NEW (unregister maybe?)
        anObject.setPersistenceState(PersistenceState.DELETED);

        //Do the right thing with all the relationships of the deleted object
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(anObject);
        Iterator relationshipIterator = entity.getRelationships().iterator();
        while (relationshipIterator.hasNext()) {
            ObjRelationship thisRelationship =
                (ObjRelationship) relationshipIterator.next();
            String thisRelationshipName = thisRelationship.getName();

            List relatedObjects;
            if (thisRelationship.isToMany()) {
                //Get an independent copy of the list so that
                // deleting objects doesn't result in concurrent modification
                // exceptions
                relatedObjects =
                    new ArrayList(
                        (List) anObject.readPropertyDirectly(thisRelationship.getName()));
            }
            else {
                //thisRelationship is toOne... make a list of one object
                relatedObjects = new ArrayList(1);
                DataObject relatedObject =
                    (DataObject) anObject.readPropertyDirectly(thisRelationshipName);
                if (relatedObject != null) {
                    relatedObjects.add(relatedObject);
                }
            }

            switch (thisRelationship.getDeleteRule()) {
                case DeleteRule.NULLIFY :
                    ObjRelationship inverseRelationship =
                        thisRelationship.getReverseRelationship();
                    if (null == inverseRelationship) {
                        continue;
                        //with next relationship... nothing we can do here
                    }
                    String inverseRelationshipName = inverseRelationship.getName();

                    if (inverseRelationship.isToMany()) {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.removeToManyTarget(
                                inverseRelationshipName,
                                anObject,
                                true);
                        }
                    }
                    else {
                        //Inverse is to-one - find all related objects and
                        // nullify the reverse relationship
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.setToOneTarget(
                                inverseRelationshipName,
                                null,
                                true);
                        }
                    }
                    break;
                case DeleteRule.CASCADE :
                    //Delete all related objects
                    Iterator iterator = relatedObjects.iterator();
                    while (iterator.hasNext()) {
                        DataObject relatedObject = (DataObject) iterator.next();
                        this.deleteObject(relatedObject);
                    }
                    break;
                case DeleteRule.DENY :
                    int relatedObjectsCount = relatedObjects.size();
                    if (relatedObjectsCount != 0) {
                        //Clean up - we shouldn't be deleting this object
                        anObject.setPersistenceState(oldState);
                        throw new DeleteDenyException(
                            "Cannot delete a "
                                + getEntityResolver().lookupObjEntity(anObject).getName()
                                + " because it has "
                                + relatedObjectsCount
                                + " object"
                                + (relatedObjectsCount > 1 ? "s" : "")
                                + "in it's "
                                + thisRelationshipName
                                + " relationship"
                                + " and this relationship has DENY "
                                + "as it's delete rule");
                    }
                    break;
                case DeleteRule.NO_ACTION :
                    // no action it is...
                    break;
                default :
                    //Clean up - we shouldn't be deleting this object
                    anObject.setPersistenceState(oldState);
                    throw new CayenneRuntimeException(
                        "Unknown type of delete rule "
                            + thisRelationship.getDeleteRule());
            }
        }
    }

    /**
     * Refetches object data for ObjectId. This method is used
     * internally by Cayenne to resolve objects in state
     * <code>PersistenceState.HOLLOW</code>. It can also be used
     * to refresh certain objects.
     *
     * @throws CayenneRuntimeException if object id doesn't match
     * any records, or if there is more than one object is fetched.
     */
    public DataObject refetchObject(ObjectId oid) {

        synchronized (getObjectStore()) {
            DataObject object = objectStore.getObject(oid);

            // clean up any cached data for this object
            if (object != null) {
                this.invalidateObject(object);
            }
        }

        SelectQuery sel = QueryUtils.selectObjectForId(oid);
        List results = this.performQuery(sel);

        if (results.size() != 1) {
            String msg =
                (results.size() == 0)
                    ? "No matching objects found for ObjectId " + oid
                    : "More than 1 object found for ObjectId "
                        + oid
                        + ". Fetch matched "
                        + results.size()
                        + " objects.";

            throw new CayenneRuntimeException(msg);
        }

        return (DataObject) results.get(0);
    }

    /**
     * Rollsback any changes that have occurred to objects
     * registered with this data context.
     */
    public void rollbackChanges() {
        synchronized (getObjectStore()) {
            List objectsToUnregister = new ArrayList();
            Iterator it = objectStore.getObjectIterator();

            // collect candidates
            while (it.hasNext()) {
                DataObject thisObject = (DataObject) it.next();
                int objectState = thisObject.getPersistenceState();
                switch (objectState) {
                    case PersistenceState.NEW :
                        //We cannot unregister at this stage, because that would modify the map upon which
                        // the iterator returned by objectStore.getObjectIterator() is based.  It is done outside the iterator loop
                        objectsToUnregister.add(thisObject);
                        break;
                    case PersistenceState.DELETED :
                        //Do the same as for modified... deleted is only a persistence state, so
                        // rolling the object back will set the state to committed
                    case PersistenceState.MODIFIED :
                        // this will clean any modifications and defer refresh from snapshot
                        // till the next object accessor is called
                        thisObject.setPersistenceState(PersistenceState.HOLLOW);
                        break;
                    default :
                        //Transient, committed and hollow need no handling
                        break;
                }
            }

            // unregister candidates
            unregisterObjects(objectsToUnregister);

            // finally clear flattened inserts & deletes
            this.clearFlattenedUpdateQueries();
        }

    }

    /**
     * Synchronizes object graph with the database. Executes needed
     * insert, update and delete queries (generated internally).
     */
    public void commitChanges() throws CayenneRuntimeException {
        commitChanges(null);
    }

    /**
     * Synchronizes object graph with the database. Executes needed
     * insert, update and delete queries (generated internally).
     *
     * @param logLevel if logLevel is higher or equals to the level
     * set for QueryLogger, statements execution will be logged.
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {

        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        // prevent multiple commits occuring simulteneously 
        synchronized (getObjectStore()) {
            // is there anything to do?
            if (this.hasChanges() == false) {
                return;
            }

            ContextCommit worker = new ContextCommit(this);

            try {
                worker.commit(logLevel);
                this.clearFlattenedUpdateQueries();
            }
            catch (CayenneException ex) {
                throw new CayenneRuntimeException(ex);
            }
        }
    }

    /**
     * Performs a single database select query.
     *
     * @return A list of DataObjects or a list of data rows
     * depending on the value returned by <code>query.isFetchingDataRows()</code>.
     */
    public List performQuery(GenericSelectQuery query) {

        // check if result pagination is requested
        // let a list handle fetch in this case
        if (query.getPageSize() > 0) {
            return new IncrementalFaultList(this, query);
        }

        SelectObserver observer = new SelectObserver(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);

        return (query.isFetchingDataRows())
            ? observer.getResults(query)
            : observer.getResultsAsObjects(this, query);
    }

    /**
     * Performs a single database select query returning result as a ResultIterator.
     * Returned ResultIterator will provide access to "data rows"
     * - maps with database data that can be used to create DataObjects.
     */
    public ResultIterator performIteratedQuery(GenericSelectQuery query)
        throws CayenneException {

        IteratedSelectObserver observer = new IteratedSelectObserver();
        observer.setLoggingLevel(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
    }

    /** Delegates node lookup to parent QueryEngine. */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().dataNodeForObjEntity(objEntity);
    }

    /**
     * Delegates queries execution to parent QueryEngine. If there are select
     * queries that require prefetching relationships, will create additional
     * queries to perform necessary prefetching.
     */
    public void performQueries(List queries, OperationObserver resultConsumer) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        DataContextDelegate localDelegate = nonNullDelegate();
        List finalQueries = new ArrayList(queries.size());
        boolean hasPrefetches = false;

        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Object query = it.next();

            if (query instanceof GenericSelectQuery) {
                GenericSelectQuery genericSelect = (GenericSelectQuery) query;

                // filter via a delegate
                GenericSelectQuery filteredSelect =
                    localDelegate.willPerformSelect(this, genericSelect);

                // suppressed by the delegate
                if (filteredSelect != null) {
                    finalQueries.add(filteredSelect);

                    // check if prefetching is required
                    if (!hasPrefetches && (filteredSelect instanceof SelectQuery)) {
                        hasPrefetches =
                            !((SelectQuery) filteredSelect).getPrefetches().isEmpty();
                    }
                }
            }
            else {
                finalQueries.add(query);
            }
        }

        if (!resultConsumer.isIteratedResult() && hasPrefetches) {
            // do a second pass to add prefetches (prefetches must go after all main queries)
            it = queries.iterator();
            while (it.hasNext()) {
                SelectQuery select = (SelectQuery) it.next();
                Collection prefetchRels = select.getPrefetches();
                if (prefetchRels.size() > 0) {
                    Iterator prIt = prefetchRels.iterator();
                    while (prIt.hasNext()) {
                        PrefetchSelectQuery prefetchQuery =
                            QueryUtils.selectPrefetchPath(
                                this,
                                select,
                                (String) prIt.next());

                        // filter via a delegate
                        GenericSelectQuery filteredPrefetch =
                            localDelegate.willPerformSelect(this, prefetchQuery);

                        // if not suppressed by delegate
                        if (filteredPrefetch != null) {
                            finalQueries.add(filteredPrefetch);
                        }
                    }
                }
            }
        }

        this.getParent().performQueries(finalQueries, resultConsumer);
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships
     * for a list of DataObjects in the most optimized way (preferrably in
     * a single query per relationship).
     *
     * <p><i>WARNING: Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed eventually.</i></p>
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
                throw new CayenneRuntimeException(
                    "Only one-step relationships are "
                        + "supported at the moment, this will be fixed soon. "
                        + "Unsupported path : "
                        + prefetchKey);
            }

            ObjRelationship relationship =
                (ObjRelationship) entity.getRelationship(prefetchKey);
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
     * Delegates query execution to parent QueryEngine.
     *  
     * @deprecated Since 1.1 use performQueries(List, OperationObserver).
     * This method is redundant and doesn't add value.
     */
    public void performQuery(Query query, OperationObserver operationObserver) {
        this.performQueries(Collections.singletonList(query), operationObserver);
    }

    /**
     *  Populates the <code>map</code> with ObjectId values from master objects
     *  related to this object.
     */
    private void appendPkFromMasterRelationships(Map map, DataObject dataObject) {
        ObjEntity objEntity = this.getEntityResolver().lookupObjEntity(dataObject);
        DbEntity dbEntity = objEntity.getDbEntity();

        Iterator it = dbEntity.getRelationshipMap().values().iterator();
        while (it.hasNext()) {
            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK()) {
                continue;
            }

            ObjRelationship rel = objEntity.getRelationshipForDbRelationship(dbRel);
            if (rel == null) {
                continue;
            }

            DataObject targetDo =
                (DataObject) dataObject.readPropertyDirectly(rel.getName());
            if (targetDo == null) {
                // this is bad, since we will not be able to obtain PK in any other way
                // throw an exception
                throw new CayenneRuntimeException("Null master object, can't create primary key.");
            }

            Map idMap = targetDo.getObjectId().getIdSnapshot();
            if (idMap == null) {
                // this is bad, since we will not be able to obtain PK in any other way
                // provide a detailed error message
                StringBuffer msg =
                    new StringBuffer("Can't create primary key, master object has no PK snapshot.");
                msg
                    .append("\nrelationship name: ")
                    .append(dbRel.getName())
                    .append(", src object: ")
                    .append(dataObject.getObjectId().getObjClass().getName())
                    .append(", target obj: ")
                    .append(targetDo.getObjectId().getObjClass().getName());
                throw new CayenneRuntimeException(msg.toString());
            }

            map.putAll(dbRel.srcFkSnapshotWithTargetSnapshot(idMap));
        }
    }

    /** Creates permanent ObjectId for <code>anObject</code>.
     *  Object must already have a temporary ObjectId.
     *
     *  <p>This method is called when we are about to save a new object to
     *  the database. Primary key columns are populated assigning values
     *  in the following sequence:
     *  <ul>
     *     <li>Object attribute values are used.</li>
     *     <li>Values from ObjectId's propagated from master relationshop
     *     are used. <i>If master object does not have a permanent id
     *     created yet, an exception is thrown.</i></li>
     *     <li>Values generated from the database provided by DbAdapter.
     *     <i>Autogeneration only works for a single column. If more than
     *     one column requires an autogenerated primary key, an exception is
     *     thrown</i></li>
     *   </ul>
     *
     *   @return Newly created ObjectId.
     */
    public ObjectId createPermId(DataObject anObject) throws CayenneRuntimeException {
        ObjectId id = anObject.getObjectId();
        if (!(id instanceof TempObjectId)) {
            return id;
            //If the id is not a temp, then it must be permanent.  Return it and do nothing else
        }

        if (id.getReplacementId() != null) {
            return id.getReplacementId();
        }

        ObjEntity objEntity = this.getEntityResolver().lookupObjEntity(id.getObjClass());
        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode aNode = this.dataNodeForObjEntity(objEntity);

        Map idMap = new HashMap();
        // first get values delivered via relationships
        appendPkFromMasterRelationships(idMap, anObject);

        boolean autoPkDone = false;
        Iterator it = dbEntity.getPrimaryKey().iterator();
        while (it.hasNext()) {
            DbAttribute attr = (DbAttribute) it.next();

            // see if it is there already
            if (idMap.get(attr.getName()) != null) {
                continue;
            }

            // try object value as PK
            ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(attr);
            if (objAttr != null) {
                idMap.put(
                    attr.getName(),
                    anObject.readPropertyDirectly(objAttr.getName()));
                continue;
            }

            // run PK autogeneration
            if (autoPkDone) {
                throw new CayenneRuntimeException("Primary Key autogeneration only works for a single attribute.");
            }

            try {
                PkGenerator gen = aNode.getAdapter().getPkGenerator();
                Object pk = gen.generatePkForDbEntity(aNode, objEntity.getDbEntity());
                autoPkDone = true;
                idMap.put(attr.getName(), pk);
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK", ex);
            }
        }

        ObjectId permId = new ObjectId(anObject.getClass(), idMap);

        // note that object registration did not change (new id is not attached to context, only to temp. oid)
        id.setReplacementId(permId);
        return permId;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it.  Then when deserialization happens, we can get back the DataDomain by name,
        // from the shared configuration (which will either load it if need be, or return
        // an existing one.
        out.defaultWriteObject();
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
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        Object value = in.readObject();
        if (value instanceof QueryEngine) {
            //Must be a real QueryEngine object - use it
            this.parent = (QueryEngine) value;
        }
        else if (value instanceof String) {
            //Must be the name of a DataDomain - use it
            this.lazyInitParentDomainName = (String) value;
        }
        else {
            throw new CayenneRuntimeException(
                "Parent attribute of DataContext was neither a QueryEngine nor "
                    + "the name of a valid DataDomain:"
                    + value);
        }

        // initialized new relationship datasource
        this.relationshipDataSource = new RelationshipDataSource(this);

        // CayenneDataObjects have a transient datacontext
        // because at deserialize time the datacontext may need to be different
        // than the one at serialize time (for programmer defined reasons).
        // So, when a dataobject is resurrected because it's datacontext was
        // serialized, it will then set the objects datacontext to the correctone
        // If deser'd "otherwise", it will not have a datacontext (good)

        synchronized (getObjectStore()) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject object = (DataObject) it.next();
                object.setDataContext(this);
            }
        }
    }

    public EntityResolver getEntityResolver() {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().getEntityResolver();
    }

    public void registerFlattenedRelationshipInsert(
        DataObject source,
        ObjRelationship relationship,
        DataObject destination) {
        if (logObj.isDebugEnabled()) {
            logObj.debug(
                "registerFlattenedRelationshipInsert for source of class "
                    + source.getClass().getName()
                    + ", rel="
                    + relationship.getName()
                    + ", destination class="
                    + destination.getClass().getName());
        }
        //Register this combination (so we can remove it later if an insert occurs before commit)
        FlattenedRelationshipInfo info =
            new FlattenedRelationshipInfo(source, destination, relationship);

        if (flattenedDeletes.contains(info)) {
            //If this combination has already been deleted, simply undelete it.
            logObj.debug(
                "This combination already deleted.. undeleting to simulate the insert");
            flattenedDeletes.remove(info);
        }
        else if (!flattenedInserts.contains(info)) {
            logObj.debug("This combination is not currently inserted... ok");
            flattenedInserts.add(info);
        }
    }

    public void registerFlattenedRelationshipDelete(
        DataObject source,
        ObjRelationship relationship,
        DataObject destination) {
        if (logObj.isDebugEnabled()) {
            logObj.debug(
                "registerFlattenedRelationshipDelete for source of class "
                    + source.getClass().getName()
                    + ", rel="
                    + relationship.getName()
                    + ", destination class="
                    + destination.getClass().getName());
        }
        //Register this combination (so we can remove it later if an insert occurs before commit)
        FlattenedRelationshipInfo info =
            new FlattenedRelationshipInfo(source, destination, relationship);

        if (flattenedInserts.contains(info)) {
            //If this combination has already been inserted, simply uninsert it.
            logObj.debug(
                "This combination already inserted..uninserting to simulate the delete");
            flattenedInserts.remove(info);
        }
        else if (!flattenedDeletes.contains(info)) { //Do not delete it twice
            logObj.debug(
                "This combination is not currently deleted... registering for deletes");
            flattenedDeletes.add(info);
        }
    }

    /**
     * Should be called once the queries returned by getFlattenedUpdateQueries
     * have been succesfully executed or reverted and are no longer needed.
     */
    protected void clearFlattenedUpdateQueries() {
        this.flattenedDeletes = new ArrayList();
        this.flattenedInserts = new ArrayList();
    }

    /**
     * Sets default for posting transaction events by new DataContexts.
     */
    public static void setTransactionEventsEnabledDefault(boolean flag) {
        transactionEventsEnabledDefault = flag;
    }

    /**
     * Enable/disable posting of transaction events by this DataContext.
     */
    public void setTransactionEventsEnabled(boolean flag) {
        this.transactionEventsEnabled = flag;
    }

    public boolean isTransactionEventsEnabled() {
        return this.transactionEventsEnabled;
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

    List getFlattenedInserts() {
        return flattenedInserts;
    }

    List getFlattenedDeletes() {
        return flattenedDeletes;
    }

    //Stores the information about a flattened relationship between two objects in a
    // canonical form, such that equals returns true if both objects refer to the same
    // pair of DataObjects connected by the same relationship (regardless of the
    // direction of the relationship used to construct this info object)
    static final class FlattenedRelationshipInfo extends Object {
        private DataObject source;
        private DataObject destination;
        private ObjRelationship baseRelationship;
        private String canonicalRelationshipName;

        public FlattenedRelationshipInfo(
            DataObject aSource,
            DataObject aDestination,
            ObjRelationship relationship) {
            super();
            this.source = aSource;
            this.destination = aDestination;
            this.baseRelationship = relationship;

            //Calculate canonical relationship name
            String relName1 = relationship.getName();
            ObjRelationship reverseRel = relationship.getReverseRelationship();
            if (reverseRel != null) {
                String relName2 = reverseRel.getName();
                //Find the lexically lesser name and use it first, then use the second.
                //If equal (the same name), it doesn't matter which order.. be arbitrary
                if (relName1.compareTo(relName2) <= 0) {
                    this.canonicalRelationshipName = relName1 + "." + relName2;
                }
                else {
                    this.canonicalRelationshipName = relName2 + "." + relName1;
                }
            }
            else {
                this.canonicalRelationshipName = relName1;
            }
        }

        /**
         * Does not care about the order of source/destination, only that the
         * pair and the canonical relationship name match
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof FlattenedRelationshipInfo)) {
                return false;
            }
            if (this == obj) {
                return true;
            }

            FlattenedRelationshipInfo otherObj = (FlattenedRelationshipInfo) obj;

            if (!this
                .canonicalRelationshipName
                .equals(otherObj.canonicalRelationshipName)) {
                return false;
            }
            //Check that either direct mapping matches (src=>src, dest=>dest), or that
            // cross mapping matches (src=>dest, dest=>src).
            if (((this.source.equals(otherObj.source))
                && (this.destination.equals(otherObj.destination)))
                || ((this.source.equals(otherObj.destination))
                    && (this.destination.equals(otherObj.source)))) {
                return true;
            }
            return false;
        }

        /**
         * Because equals effectively ignores the order of dataObject1/2,
         * summing the hashcodes is sufficient to fulfill the equals/hashcode
         * contract
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return source.hashCode()
                + destination.hashCode()
                + canonicalRelationshipName.hashCode();
        }
        /**
         * Returns the baseRelationship.
         * @return ObjRelationship
         */
        public ObjRelationship getBaseRelationship() {
            return baseRelationship;
        }

        /**
         * Returns the destination.
         * @return DataObject
         */
        public DataObject getDestination() {
            return destination;
        }

        /**
         * Returns the source.
         * @return DataObject
         */
        public DataObject getSource() {
            return source;
        }

    }

}
