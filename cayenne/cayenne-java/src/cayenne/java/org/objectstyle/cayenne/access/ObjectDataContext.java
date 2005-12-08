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

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.opp.GenericQueryMessage;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.SyncMessage;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryChain;

/**
 * A temporary subclass of DataContext that implements ObjectContext interface. Used to
 * test ObjectContext support without disturbing current DataContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: merge into DataContext
class ObjectDataContext extends DataContext implements ObjectContext {

    ObjectDataContext(DataDomain dataDomain) {
        // call a setter, as it properly initializes EntityResolver...
        setChannel(dataDomain);

        DataRowStore cache = dataDomain.isSharedCacheEnabled() ? dataDomain
                .getSharedSnapshotCache() : new DataRowStore(
                dataDomain.getName(),
                dataDomain.getProperties(),
                dataDomain.getEventManager());

        this.usingSharedSnaphsotCache = dataDomain.isSharedCacheEnabled();
        this.objectStore = new ObjectStore(cache);
    }

    ObjectDataContext(OPPChannel channel, DataRowStore cache) {
        super(channel, new ObjectStore(cache));
    }

    // ==== START: DataContext compatibility code... need to merge to DataContext
    // --------------------------------------------------------------------------

    /**
     * @deprecated since 1.2 as QueryChains are now possible.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        QueryChain query = new QueryChain(queries);
        getParentDataDomain().performQuery(query.resolve(getEntityResolver()), observer);
    }

    public List performQuery(String queryName, boolean refresh) {
        // TODO: refresh is not handled...
        return performQuery(new NamedQuery(queryName));
    }

    // ==== END: DataContext compatibility code... need to merge to DataContext
    // --------------------------------------------------------------------------

    public void commitChanges() throws CayenneRuntimeException {
        doCommitChanges();
    }

    GraphDiff doCommitChanges() {
        if (getChannel() == null) {
            throw new CayenneRuntimeException(
                    "DataContext is not attached to an OPPChannel");
        }

        synchronized (getObjectStore()) {

            if (!hasChanges()) {
                return new CompoundDiff();
            }

            if (isValidatingObjectsOnCommit()) {
                getObjectStore().validateUncommittedObjects();
            }

            // TODO: Andrus, 12/06/2005 - this is a violation of OPP rules, as we do not
            // pass changes down the stack. Instead this code assumes that a channel will
            // get them directly from the context.
            GraphDiff resultDiff = getChannel().onSync(
                    new SyncMessage(this, SyncMessage.COMMIT_TYPE, null));

            getObjectStore().objectsCommitted();

            // do not ever return null to caller....
            return resultDiff != null ? resultDiff : new CompoundDiff();
        }
    }

    public void flushChanges() {
        // noop ... for now...
    }

    public void revertChanges() {
        rollbackChanges();
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

    public QueryResponse performGenericQuery(Query query) {
        if (this.getChannel() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent OPPChannel is not set.");
        }

        return getChannel().onGenericQuery(new GenericQueryMessage(query));
    }

    public List performQuery(Query query) {
        return new ObjectDataContextSelectAction(this).performQuery(query);
    }

    // *** Unfinished stuff
    // --------------------------------------------------------------------------

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
