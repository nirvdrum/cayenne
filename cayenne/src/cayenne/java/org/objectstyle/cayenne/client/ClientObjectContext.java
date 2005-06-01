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
package org.objectstyle.cayenne.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.distribution.CayenneConnector;
import org.objectstyle.cayenne.distribution.ChainedMessage;
import org.objectstyle.cayenne.distribution.ClientMessage;
import org.objectstyle.cayenne.distribution.CommitMessage;
import org.objectstyle.cayenne.distribution.NamedQueryMessage;
import org.objectstyle.cayenne.distribution.QueryMessage;
import org.objectstyle.cayenne.distribution.SyncMessage;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * Client-side ObjectContext implementation.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientObjectContext implements ObjectContext {

    protected CayenneConnector connector;
    protected ClientObjectStore objectStore;

    /**
     * Creates a new ClientObjectContext, initializaing it with a connector instance that
     * should be used to connect to a remote Cayenne service.
     */
    public ClientObjectContext(CayenneConnector connector) {
        this.connector = connector;
        this.objectStore = new ClientObjectStore();
    }

    /**
     * Returns connector used to access remote Cayenne service.
     */
    public CayenneConnector getConnector() {
        return connector;
    }

    /**
     * Sends commit and sync commands to remote Cayenne service via an internal instance
     * of CayenneConnector.
     */
    public void commitChanges() {

        if (objectStore.hasChanges()) {

            ClientMessage[] commands = new ClientMessage[2];
            commands[0] = new SyncMessage(objectStore.getDirtyObjects());
            commands[1] = new CommitMessage();

            ClientMessage chain = new ChainedMessage(commands);
            Collection objectIds = (Collection) connector.sendMessage(chain);

            objectStore.objectsCommitted(objectIds);
        }
    }

    public void commitChangesInContext(ObjectContext context) {
        // TODO: implement
        throw new CayenneRuntimeException(
                "ObjectContext hierarchy is not supported (yet).");
    }

    /**
     * Deletes an object locally, scheduling it for future deletion from the external data
     * store.
     */
    public void deleteObject(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.TRANSIENT) {
            return;
        }

        if (object.getPersistenceState() == PersistenceState.DELETED) {
            return;
        }

        if (object.getPersistenceState() == PersistenceState.NEW) {
            // kick it out of context
            objectStore.forgetObject(object);
            object.setPersistenceState(PersistenceState.TRANSIENT);
            return;
        }

        // TODO: no delete rules (yet)

        object.setPersistenceState(PersistenceState.DELETED);
        objectStore.trackObject(object);
    }

    /**
     * Creates and registers a new Persistent object instance.
     */
    public Persistent newObject(Class persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Persistent class can't be null.");
        }

        Persistent object = null;
        try {
            object = (Persistent) persistentClass.newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException(
                    "Error instantiating persistent object of class " + persistentClass,
                    ex);
        }

        // make object "cayenne-persistent"
        object.setObjectId(new TempObjectId(persistentClass));
        object.setPersistenceState(PersistenceState.NEW);
        objectStore.trackObject(object);

        return object;
    }

    public List performQuery(String queryName, Map parameters, boolean refresh) {
        return new NamedQueryMessage(queryName, parameters, true, refresh)
                .sendPerformQuery(connector);
    }

    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return new NamedQueryMessage(queryName, parameters, false, false)
                .sendPerformNonSelectingQuery(connector);
    }

    public int[] performNonSelectingQuery(Query query) {
        return new QueryMessage(query, false).sendPerformNonSelectingQuery(connector);
    }

    public List performQuery(GenericSelectQuery query) {
        return new QueryMessage(query, true).sendPerformQuery(connector);
    }

    public void objectWillRead(Persistent dataObject, String property) {
        if (dataObject.getPersistenceState() == PersistenceState.HOLLOW) {
            // must resolve...
            throw new CayenneClientException("Resolving an object is Unimplemented");
        }
    }

    public void objectWillWrite(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

        // change state...
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);

            objectStore.trackObject(object);
        }

        // TODO: take a better advantage of the property change info, e.g build a
        // diff for synchronization instead of sending full objects...
    }

    public Collection deletedObjects() {
        return objectStore.objectsInState(PersistenceState.DELETED);
    }

    public Collection modifiedObjects() {
        return objectStore.objectsInState(PersistenceState.MODIFIED);
    }

    public Collection newObjects() {
        return objectStore.objectsInState(PersistenceState.NEW);
    }
}