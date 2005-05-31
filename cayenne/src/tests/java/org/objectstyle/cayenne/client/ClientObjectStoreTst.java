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

import java.util.Collections;

import junit.framework.TestCase;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;

/**
 * @author Andrus Adamchik
 */
public class ClientObjectStoreTst extends TestCase {

    public void testObjectsInState() {
        ClientObjectStore store = new ClientObjectStore();

        // check for null collections
        assertNotNull(store.objectsInState(PersistenceState.MODIFIED));
        assertNotNull(store.objectsInState(PersistenceState.COMMITTED));
        assertNotNull(store.objectsInState(PersistenceState.DELETED));
        assertNotNull(store.objectsInState(PersistenceState.NEW));
        assertNotNull(store.objectsInState(PersistenceState.TRANSIENT));
        assertNotNull(store.objectsInState(PersistenceState.HOLLOW));

        assertTrue(store.objectsInState(PersistenceState.MODIFIED).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.COMMITTED).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.DELETED).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.NEW).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.TRANSIENT).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.HOLLOW).isEmpty());

        MockClientDataObject modified = new MockClientDataObject();
        modified.setObjectId(new ObjectId(Object.class, "key", "value1"));
        modified.setPersistenceState(PersistenceState.MODIFIED);
        store.trackObject(modified);

        assertTrue(store.objectsInState(PersistenceState.MODIFIED).contains(modified));
        assertTrue(store.objectsInState(PersistenceState.COMMITTED).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.DELETED).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.NEW).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.TRANSIENT).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.HOLLOW).isEmpty());

        MockClientDataObject deleted = new MockClientDataObject();
        deleted.setObjectId(new ObjectId(Object.class, "key", "value2"));
        deleted.setPersistenceState(PersistenceState.DELETED);
        store.trackObject(deleted);

        assertTrue(store.objectsInState(PersistenceState.MODIFIED).contains(modified));
        assertTrue(store.objectsInState(PersistenceState.COMMITTED).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.DELETED).contains(deleted));
        assertTrue(store.objectsInState(PersistenceState.NEW).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.TRANSIENT).isEmpty());
        assertTrue(store.objectsInState(PersistenceState.HOLLOW).isEmpty());
    }

    public void testDirtyObjects() {
        ClientObjectStore store = new ClientObjectStore();
        assertNotNull(store.getDirtyObjects());
        assertTrue(store.getDirtyObjects().isEmpty());

        // introduce a fake dirty object
        MockClientDataObject object = new MockClientDataObject();
        object.setObjectId(new ObjectId(Object.class, "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        store.trackObject(object);

        assertTrue(store.getDirtyObjects().contains(object));

        // must go away on commit...
        store.objectsCommitted(Collections.EMPTY_SET);
        assertNotNull(store.getDirtyObjects());
        assertTrue(store.getDirtyObjects().isEmpty());

    }

    public void testHasChanges() {
        ClientObjectStore store = new ClientObjectStore();
        assertFalse(store.hasChanges());

        // introduce a fake dirty object
        MockClientDataObject object = new MockClientDataObject();
        object.setObjectId(new ObjectId(Object.class, "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        store.trackObject(object);

        assertTrue(store.hasChanges());

        // must go away on commit...
        store.objectsCommitted(Collections.EMPTY_SET);
        assertFalse(store.hasChanges());
    }

    public void testObjectLifecycle() {
        ClientObjectStore store = new ClientObjectStore();

        // MODIFIED -> COMMITTED
        MockClientDataObject modified = new MockClientDataObject();
        modified.setObjectId(new ObjectId(Object.class, "key", "value1"));
        modified.setPersistenceState(PersistenceState.MODIFIED);

        store.trackObject(modified);
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        store.objectsCommitted(Collections.EMPTY_SET);
        assertEquals(PersistenceState.COMMITTED, modified.getPersistenceState());

        // DELETED -> TRANSIENT
        MockClientDataObject deleted = new MockClientDataObject();
        deleted.setObjectId(new ObjectId(Object.class, "key", "value2"));
        deleted.setPersistenceState(PersistenceState.DELETED);

        store.trackObject(deleted);
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        store.objectsCommitted(Collections.EMPTY_SET);
        assertEquals(PersistenceState.TRANSIENT, deleted.getPersistenceState());

        // NEW -> COMMITTED
        MockClientDataObject newObject = new MockClientDataObject();
        newObject.setObjectId(new ObjectId(Object.class, "key", "value3"));
        newObject.setPersistenceState(PersistenceState.NEW);

        store.trackObject(newObject);
        assertEquals(PersistenceState.NEW, newObject.getPersistenceState());
        store.objectsCommitted(Collections.EMPTY_SET);
        assertEquals(PersistenceState.COMMITTED, newObject.getPersistenceState());
    }
}
