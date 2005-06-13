package org.objectstyle.cayenne.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceContext;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataRowStore;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.Transaction;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * A temporary subclass of DataContext that implements ObjectContext interface. Used to
 * test ObjectContext support without disturbing current DataContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: merge into DataContext
class ObjectDataContext extends DataContext implements ObjectContext {

    PersistenceContext parentContext;
    EntityResolver entityResolver;

    /**
     * Initializes ObjectDataContext obtaining settings from parent DataDomain.
     */
    ObjectDataContext(DataDomain parentDomain) {
        this.parentContext = parentDomain;
        super.parent = parentDomain;
        this.entityResolver = parentDomain.getEntityResolver();

        DataRowStore cache = parentDomain.isSharedCacheEnabled() ? parentDomain
                .getSharedSnapshotCache() : new DataRowStore(
                parentDomain.getName(),
                parentDomain.getProperties());

        super.objectStore = new ObjectStore(cache);
    }

    ObjectDataContext(PersistenceContext parentContext, EntityResolver entityResolver,
            DataRowStore cache) {
        this.parentContext = parentContext;
        this.entityResolver = entityResolver;

        // if parent is a DataDomain, init it as "old" parent for backwards
        // compatibility...
        if (parentContext instanceof DataDomain) {
            super.parent = (DataDomain) parentContext;
        }

        super.objectStore = new ObjectStore(cache);
    }

    public void commitChanges() throws CayenneRuntimeException {

        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "ObjectContext has no parent PersistenceContext.");
        }

        synchronized (getObjectStore()) {
            if (!hasChanges()) {
                return;
            }

            if (isValidatingObjectsOnCommit()) {
                getObjectStore().validateUncommittedObjects();
            }

            getParentContext().commitChangesInContext(this);
        }
    }

    public PersistenceContext getParentContext() {
        return parentContext;
    }

    public void deleteObject(Persistent object) {

        // TODO: only supports DataObject subclasses
        if (object != null && !(object instanceof DataObject)) {
            throw new IllegalArgumentException(
                    this
                            + ": this implementation of ObjectContext only supports full DataObjects. Object "
                            + object
                            + " is not supported.");
        }

        super.deleteObject((DataObject) object);
    }

    /**
     * Creates and registers new persistent object.
     */
    public Persistent newObject(Class persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Null 'persistentClass'");
        }

        // TODO: only supports DataObject subclasses
        if (!DataObject.class.isAssignableFrom(persistentClass)) {
            throw new IllegalArgumentException(
                    this
                            + ": this implementation of ObjectContext only supports full DataObjects. Class "
                            + persistentClass
                            + " is invalid.");
        }

        return super.createAndRegisterNewObject(persistentClass);
    }

    /**
     * Returns a collection of all uncommitted registered objects.
     */
    public Collection uncommittedObjects() {
        // TODO: whenever this code is merged to DataContext, this method should be moved
        // to ObjectStore.

        int len = getObjectStore().registeredObjectsCount();
        if (len == 0) {
            return Collections.EMPTY_LIST;
        }

        // guess target collection size
        Collection objects = new ArrayList(len > 100 ? len / 2 : len);

        Iterator it = getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            int state = object.getPersistenceState();
            if (state == PersistenceState.MODIFIED
                    || state == PersistenceState.MODIFIED
                    || state == PersistenceState.MODIFIED) {

                objects.add(object);
            }
        }

        return objects;
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public int[] performNonSelectingQuery(Query query) {
        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent PersistenceContext is not set.");
        }

        return new PersistenceContextQueryAction(getParentContext())
                .performNonSelectingQuery(query);
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return performNonSelectingQuery(new NamedQueryProxy(queryName, parameters));
    }

    /**
     * Overrides super implementation to channel query execution using new algorithm.
     */
    public List performQuery(GenericSelectQuery query) {
        // channel through our own implementation... TODO: this method should be deprected
        // in super at some point...
        return performQuery((Query) query);
    }

    public List performQuery(Query query) {
        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent PersistenceContext is not set.");
        }

        return new PersistenceContextSelectAction(getParentContext()).performQuery(this,
                query,
                false);
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public List performQuery(String queryName, Map parameters, boolean refresh) {
        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent PersistenceContext is not set.");
        }

        return new PersistenceContextSelectAction(getParentContext()).performQuery(this,
                new NamedQueryProxy(queryName, parameters),
                refresh);
    }

    /**
     * Overrides DataContext implementation to return EntityResolver stored in ivar.
     */
    public EntityResolver getEntityResolver() {
        // TODO: ready to be moved to DataContext
        return entityResolver;
    }

    /**
     * Delegates execution to parent.
     */
    public void performQuery(Query query, OperationObserver resultConsumer) {

        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run queries - parent PersistenceContext is not set.");
        }

        // TODO: this method doesn't check the DataContextDelegate ... do we need
        // a new delegate interface for ObjectContext?

        this.getParentContext().performQuery(query, resultConsumer);
    }

    public void performQuery(
            Query query,
            OperationObserver resultConsumer,
            Transaction transaction) {

        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run queries - parent PersistenceContext is not set.");
        }

        // TODO: this method doesn't check the DataContextDelegate ... do we need
        // a new delegate interface for ObjectContext?

        this.getParentContext().performQuery(query, resultConsumer, transaction);
    }

    // *** Unfinished stuff

    public void commitChangesInContext(ObjectContext context) {
        // TODO: implement me
        throw new CayenneRuntimeException("Nested contexts are not supported yet");
    }

    public void objectWillRead(Persistent object, String property) {
        // TODO: implement me
        throw new CayenneRuntimeException(
                "Persistent interface methods are not yet handled.");
    }

    public void objectWillWrite(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {
        // TODO: implement me
        throw new CayenneRuntimeException(
                "Persistent interface methods are not yet handled.");
    }
}
