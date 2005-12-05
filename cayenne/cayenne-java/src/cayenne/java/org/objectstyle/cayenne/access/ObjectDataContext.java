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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryChain;
import org.objectstyle.cayenne.query.QueryExecutionPlan;

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
                parentDomain.getProperties(),
                parentDomain.getEventManager());

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

    // ==== START: DataContext compatibility code... need to merge to DataContext
    // --------------------------------------------------------------------------

    public EntityResolver getEntityResolver() {
        // TODO: ready to be moved to DataContext
        return entityResolver;
    }

    public int[] performNonSelectingQuery(Query query) {
        // channel to the right implementation
        return performUpdateQuery((QueryExecutionPlan) query);
    }

    public List performQuery(GenericSelectQuery query) {
        // channel through a new implementation...
        return performSelectQuery((QueryExecutionPlan) query);
    }

    /**
     * @deprecated since 1.2 as QueryChains are now possible.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        QueryChain query = new QueryChain(queries);
        getParentContext().performQuery(query.resolve(getEntityResolver()), observer);
    }

    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return performUpdateQuery(new NamedQuery(queryName, parameters));
    }

    public int[] performNonSelectingQuery(String queryName) {
        return performUpdateQuery(new NamedQuery(queryName));
    }

    public List performQuery(String queryName, boolean refresh) {
        // TODO: refresh is not handled...
        return performSelectQuery(new NamedQuery(queryName));
    }

    // ==== END: DataContext compatibility code... need to merge to DataContext
    // --------------------------------------------------------------------------

    DataObject createAndRegisterNewObject(ObjectId id) {
        if (id.getEntityName() == null) {
            throw new NullPointerException("Null entity name in id " + id);
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(id.getEntityName());
        if (entity == null) {
            throw new IllegalArgumentException("Entity not mapped with Cayenne: " + id);
        }

        DataObject dataObject = null;
        try {
            dataObject = (DataObject) entity.getJavaClass().newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        dataObject.setObjectId(id);
        registerNewObject(dataObject);
        return dataObject;
    }

    public void commitChanges() throws CayenneRuntimeException {
        doCommitChanges();
    }

    GraphDiff doCommitChanges() {
        return new ObjectDataContextCommitAction().commit(this);
    }

    public void flushChanges() {
        // noop ... for now...
    }

    public void revertChanges() {
        rollbackChanges();
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
                    || state == PersistenceState.NEW
                    || state == PersistenceState.DELETED) {

                objects.add(object);
            }
        }

        return objects;
    }

    public QueryResponse performGenericQuery(QueryExecutionPlan query) {
        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent PersistenceContext is not set.");
        }

        return new PersistenceContextQueryAction(getEntityResolver()).performMixed(
                getParentContext(),
                query);
    }

    public int[] performUpdateQuery(QueryExecutionPlan query) {
        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent PersistenceContext is not set.");
        }

        return new PersistenceContextQueryAction(getEntityResolver())
                .performNonSelectingQuery(getParentContext(), query);
    }

    public List performSelectQuery(QueryExecutionPlan query) {
        if (this.getParentContext() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent PersistenceContext is not set.");
        }

        return new ObjectDataContextSelectAction(this).performQuery(query);
    }

    // *** Unfinished stuff
    // --------------------------------------------------------------------------

    public OPPChannel getChannel() {
        // TODO: DataDomain must implement OPPChannel instead of PersistentContext
        throw new CayenneRuntimeException("'getChannel' is not implemented yet");
    }

    public GraphManager getGraphManager() {
        // TODO: ObjectStore must implement GraphManager
        throw new CayenneRuntimeException("'getGraphManager' is not implemented yet");
    }

    public void prepareForAccess(Persistent object, String property) {
        // TODO: implement me
        throw new CayenneRuntimeException("'prepareForAccess' is not implemented yet.");
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {
        // TODO: implement me
        throw new CayenneRuntimeException(
                "Persistent interface methods are not yet handled.");
    }
}
