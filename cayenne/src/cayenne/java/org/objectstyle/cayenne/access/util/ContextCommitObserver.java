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
package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.event.DataContextTransactionEventListener;
import org.objectstyle.cayenne.access.event.DataObjectTransactionEventListener;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.query.Query;

/**
 * ContextCommitObserver is used as an observer for DataContext 
 * commit operations.
 * 
 * @author Andrei Adamchik
 */
public class ContextCommitObserver
    extends DefaultOperationObserver
    implements DataContextTransactionEventListener {

    protected List updObjects;
    protected List delObjects;
    protected List insObjects;
    protected List objectsToNotify;

    protected DataContext context;

    public ContextCommitObserver(
        Level logLevel,
        DataContext context,
        List insObjects,
        List updObjects,
        List delObjects) {
        super.setLoggingLevel(logLevel);
        this.context = context;
        this.insObjects = insObjects;
        this.updObjects = updObjects;
        this.delObjects = delObjects;
        this.objectsToNotify = new ArrayList();

        // Build a list of objects that need to be notified about posted
        // DataContext events. When notifying about a successful completion
        // of a transaction we cannot build this list anymore, since all
        // the work will be done by then.
        Iterator collIter =
            (Arrays.asList(new List[] { delObjects, updObjects, insObjects }))
                .iterator();
        while (collIter.hasNext()) {
            Iterator objIter = ((Collection) collIter.next()).iterator();
            while (objIter.hasNext()) {
                Object element = objIter.next();
                if (element instanceof DataObjectTransactionEventListener) {
                    this.objectsToNotify.add(element);
                }
            }
        }
    }

    public boolean useAutoCommit() {
        return false;
    }

    /** Update the state of all objects we were synchronizing
     *  in this transaction.
     */
    public void transactionCommitted() {
        super.transactionCommitted();

        Iterator insIt = insObjects.iterator();
        ObjectStore objectStore = context.getObjectStore();

        synchronized (objectStore) {
            while (insIt.hasNext()) {

                // replace temp id's w/perm.
                DataObject nextObject = (DataObject) insIt.next();
                TempObjectId tempId = (TempObjectId) nextObject.getObjectId();
                ObjectId permId = tempId.getPermId();

                objectStore.changeObjectKey(tempId, permId);
                nextObject.setObjectId(permId);
                Map snapshot = context.takeObjectSnapshot(nextObject);
                objectStore.addSnapshot(permId, snapshot);

                nextObject.setPersistenceState(PersistenceState.COMMITTED);
            }
        }

        Iterator delIt = delObjects.iterator();
        while (delIt.hasNext()) {
            DataObject nextObject = (DataObject) delIt.next();
            ObjectId anId = nextObject.getObjectId();

            objectStore.removeObject(anId);
            nextObject.setPersistenceState(PersistenceState.TRANSIENT);
            nextObject.setDataContext(null);
        }

        Iterator updIt = updObjects.iterator();
        while (updIt.hasNext()) {
            DataObject nextObject = (DataObject) updIt.next();
            // refresh this object's snapshot, check if id data has changed
            Map snapshot = context.takeObjectSnapshot(nextObject);

            objectStore.addSnapshot(nextObject.getObjectId(), snapshot);
            nextObject.setPersistenceState(PersistenceState.COMMITTED);
        }
    }

    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException("Raising from query exception.", ex);
    }

    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException(
            "Raising from underlyingQueryEngine exception.",
            ex);
    }

    public void registerForDataContextEvents() {
        try {
            EventManager mgr = EventManager.getDefaultManager();
            mgr.addListener(
                this,
                "dataContextWillCommit",
                DataContextEvent.class,
                DataContext.WILL_COMMIT,
                this.context);
            mgr.addListener(
                this,
                "dataContextDidCommit",
                DataContextEvent.class,
                DataContext.DID_COMMIT,
                this.context);
            mgr.addListener(
                this,
                "dataContextDidRollback",
                DataContextEvent.class,
                DataContext.DID_ROLLBACK,
                this.context);
        } catch (NoSuchMethodException nsm) {
            // this really should not happen since we implement all required methods
            throw new CayenneRuntimeException(nsm);
        }
    }

    public void unregisterFromDataContextEvents() {
        EventManager mgr = EventManager.getDefaultManager();
        mgr.removeListener(this, DataContext.WILL_COMMIT);
        mgr.removeListener(this, DataContext.DID_COMMIT);
        mgr.removeListener(this, DataContext.DID_ROLLBACK);
    }

    public void dataContextWillCommit(DataContextEvent event) {
        Iterator iter = objectsToNotify.iterator();
        while (iter.hasNext()) {
            ((DataObjectTransactionEventListener) iter.next()).willCommit(event);
        }
    }

    public void dataContextDidCommit(DataContextEvent event) {
        Iterator iter = objectsToNotify.iterator();
        while (iter.hasNext()) {
            ((DataObjectTransactionEventListener) iter.next()).didCommit(event);
        }
    }

    public void dataContextDidRollback(DataContextEvent event) {
        // do nothing for now
    }
}
