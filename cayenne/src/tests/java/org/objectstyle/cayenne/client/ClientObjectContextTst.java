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

import junit.framework.TestCase;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.distribution.ClientMessage;
import org.objectstyle.cayenne.distribution.CommitMessage;
import org.objectstyle.cayenne.distribution.MockCayenneConnector;

/**
 * @author Andrus Adamchik
 */
public class ClientObjectContextTst extends TestCase {

    public void testConnector() {
        MockCayenneConnector connector = new MockCayenneConnector();
        ClientObjectContext context = new ClientObjectContext(connector);

        assertSame(connector, context.getConnector());

        // should connect lazily
        assertFalse(connector.isConnected());
    }

    public void testCommitChangesUnchanged() {

        MockCayenneConnector connector = new MockCayenneConnector();
        ClientObjectContext context = new ClientObjectContext(connector);

        // no context changes so no connector access is expected
        context.commitChanges();
        assertTrue(connector.getCommands().isEmpty());
    }

    public void testCommitChangesCommandExecuted() {

        MockCayenneConnector connector = new MockCayenneConnector(new ObjectId[0]);
        ClientObjectContext context = new ClientObjectContext(connector);

        // test that a command is being sent via connector on commit...

        MockClientDataObject object = new MockClientDataObject();
        object.setObjectId(new ObjectId(Object.class, "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        context.objectStore.trackObject(object);

        context.commitChanges();
        assertEquals(1, connector.getCommands().size());

        // expect a sync/commit chain
        ClientMessage mainMessage = (ClientMessage) connector
                .getCommands()
                .iterator()
                .next();
        assertTrue(mainMessage instanceof CommitMessage);

    }

    public void testCommitChangesNew() {

        // test that ids that are passed back are actually propagated to the right
        // objects...
        MockCayenneConnector connector = new MockCayenneConnector() {

            public Object sendMessage(ClientMessage message)
                    throws CayenneClientException {
                CommitMessage commit = (CommitMessage) message;

                // assume a single NEW object...
                Persistent object = (Persistent) commit
                        .getContext()
                        .newObjects()
                        .iterator()
                        .next();

                // fake creating a replacement ID on the server... return back the
                // original id with attached replacement values
                object.getObjectId().getReplacementIdMap().put("key", "generated");
                return new ObjectId[] {
                    object.getObjectId()
                };
            }
        };

        ClientObjectContext context = new ClientObjectContext(connector);

        // check that a generted object ID is assigned back to the object...
        Persistent object = context.newObject(MockClientDataObject.class);
        context.commitChanges();

        assertFalse(object.getObjectId().isTemporary());
        assertEquals(new ObjectId(MockClientDataObject.class, "key", "generated"), object
                .getObjectId());
    }

    public void testNewObject() {
        MockCayenneConnector connector = new MockCayenneConnector();
        ClientObjectContext context = new ClientObjectContext(connector);

        // an invalid class should blow
        try {
            context.newObject(Object.class);
            fail("ClientObjectContext created an object that is not persistent.");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }

        // now try a good one... note that unlike 1.1 server side cayenne there is no
        // entity checking performed; DataMap is not needed at this step
        Persistent object = context.newObject(MockClientDataObject.class);
        assertNotNull(object);
        assertTrue(object instanceof MockClientDataObject);
        assertEquals(PersistenceState.NEW, object.getPersistenceState());
        assertTrue(context.objectStore
                .objectsInState(PersistenceState.NEW)
                .contains(object));
        assertNotNull(object.getObjectId());
        assertTrue(object.getObjectId().isTemporary());
    }

    public void testDeleteObject() {
        MockCayenneConnector connector = new MockCayenneConnector();
        ClientObjectContext context = new ClientObjectContext(connector);

        // TRANSIENT ... should quietly ignore it
        Persistent transientObject = new MockClientDataObject();
        context.deleteObject(transientObject);
        assertEquals(PersistenceState.TRANSIENT, transientObject.getPersistenceState());

        // NEW ... should make it TRANSIENT
        // create via context to make sure that object store would register it
        Persistent newObject = context.newObject(MockClientDataObject.class);
        context.deleteObject(newObject);
        assertEquals(PersistenceState.TRANSIENT, newObject.getPersistenceState());
        assertFalse(context.objectStore.dirtyObjects.containsValue(newObject));

        // COMMITTED
        Persistent committed = new MockClientDataObject();
        committed.setPersistenceState(PersistenceState.COMMITTED);
        committed.setObjectId(new ObjectId(MockClientDataObject.class, "key", "value1"));
        context.deleteObject(committed);
        assertEquals(PersistenceState.DELETED, committed.getPersistenceState());

        // MODIFIED
        Persistent modified = new MockClientDataObject();
        modified.setPersistenceState(PersistenceState.MODIFIED);
        modified.setObjectId(new ObjectId(MockClientDataObject.class, "key", "value2"));
        context.deleteObject(modified);
        assertEquals(PersistenceState.DELETED, modified.getPersistenceState());

        // DELETED
        Persistent deleted = new MockClientDataObject();
        deleted.setPersistenceState(PersistenceState.DELETED);
        deleted.setObjectId(new ObjectId(MockClientDataObject.class, "key", "value3"));
        context.deleteObject(deleted);
        assertEquals(PersistenceState.DELETED, committed.getPersistenceState());
    }
}
