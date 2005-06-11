package org.objectstyle.cayenne.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceContext;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.DataContext;
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

    PersistenceContext parent;

    ObjectDataContext(PersistenceContext parent) {
        this.parent = parent;
    }

    public void commitChanges() throws CayenneRuntimeException {

        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "ObjectContext has no parent PersistenceContext.");
        }

        getParentContext().commitChangesInContext(this);
    }

    public PersistenceContext getParentContext() {
        return parent;
    }

    public void deleteObject(Persistent object) {

    }

    public Persistent newObject(Class persistentClass) {
        return null;
    }

    public void objectWillRead(Persistent object, String property) {

    }

    public void objectWillWrite(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

    }

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

    public void commitChangesInContext(ObjectContext context) {
        // TODO: implement me
        throw new CayenneRuntimeException("Nested contexts are not supported yet");
    }

    public List performQueryInContext(ObjectContext context, GenericSelectQuery query) {
        // TODO: implement me
        throw new CayenneRuntimeException("Nested contexts are not supported yet");
    }

    public List performQueryInContext(
            ObjectContext context,
            String queryName,
            Map parameters,
            boolean refresh) {
        // TODO: implement me
        throw new CayenneRuntimeException("Nested contexts are not supported yet");
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public int[] performNonSelectingQuery(Query query) {
        return getParentContext().performNonSelectingQuery(query);
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return getParentContext().performNonSelectingQuery(queryName, parameters);
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public List performQuery(GenericSelectQuery query) {
        return getParentContext().performQueryInContext(this, query);
    }

    /**
     * Overrides super implementation to use parent PersistenceContext for query
     * execution.
     */
    public List performQuery(String queryName, Map parameters, boolean refresh) {
        return getParentContext().performQueryInContext(this,
                queryName,
                parameters,
                refresh);
    }
}
