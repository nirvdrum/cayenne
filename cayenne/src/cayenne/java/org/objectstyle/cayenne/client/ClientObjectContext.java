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

import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.distribution.CayenneConnector;
import org.objectstyle.cayenne.distribution.ChainedCommand;
import org.objectstyle.cayenne.distribution.ClientCommand;
import org.objectstyle.cayenne.distribution.CommitCommand;
import org.objectstyle.cayenne.distribution.NamedQueryCommand;
import org.objectstyle.cayenne.distribution.SyncCommand;
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

    public ClientObjectContext(CayenneConnector connector) {
        this.connector = connector;
        this.objectStore = new ClientObjectStore();
    }

    public void commitChanges() {

        if (objectStore.hasChanges()) {

            ClientCommand[] commands = new ClientCommand[2];
            commands[0] = new SyncCommand(objectStore.getDirtyObjects());
            commands[1] = new CommitCommand();

            ClientCommand chain = new ChainedCommand(commands);
            Collection objectIds = (Collection) connector.sendCommand(chain);

            objectStore.objectsCommitted(objectIds);
        }
    }

    public void commitChangesInContext(ObjectContext context) {
        throw new CayenneClientException("Child ObjectContexts are not supported (yet).");
    }

    public List performQuery(String queryName, Map parameters, boolean refresh) {
        ClientCommand command = new NamedQueryCommand(
                queryName,
                parameters,
                true,
                refresh);
        return (List) connector.sendCommand(command);
    }

    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        ClientCommand command = new NamedQueryCommand(queryName, parameters, false, false);
        return (int[]) connector.sendCommand(command);
    }

    public int[] performNonSelectingQuery(Query query) {
        return null;
    }

    public List performQuery(GenericSelectQuery query) {
        return null;
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