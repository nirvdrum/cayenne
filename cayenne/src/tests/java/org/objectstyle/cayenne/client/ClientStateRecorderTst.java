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

import org.objectstyle.cayenne.MockPersistentObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.graph.GraphMap;
import org.objectstyle.cayenne.graph.MockGraphMap;

/**
 * @author Andrus Adamchik
 */
public class ClientStateRecorderTst extends TestCase {

    public void testDirtyNodesInState() {

        GraphMap map = new MockGraphMap();
        ClientStateRecorder recorder = new ClientStateRecorder();

        // check for null collections
        assertNotNull(recorder.dirtyNodes(map, PersistenceState.MODIFIED));
        assertNotNull(recorder.dirtyNodes(map, PersistenceState.COMMITTED));
        assertNotNull(recorder.dirtyNodes(map, PersistenceState.DELETED));
        assertNotNull(recorder.dirtyNodes(map, PersistenceState.NEW));
        assertNotNull(recorder.dirtyNodes(map, PersistenceState.TRANSIENT));
        assertNotNull(recorder.dirtyNodes(map, PersistenceState.HOLLOW));

        assertTrue(recorder.dirtyNodes(map, PersistenceState.MODIFIED).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.COMMITTED).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.DELETED).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.NEW).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.TRANSIENT).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.HOLLOW).isEmpty());

        MockPersistentObject modified = new MockPersistentObject();
        modified.setObjectId(new ObjectId(Object.class, "key", "value1"));
        modified.setPersistenceState(PersistenceState.MODIFIED);
        map.registerNode(modified.getObjectId(), modified);
        recorder.nodePropertyChanged(modified.getObjectId(), "a", "b", "c");

        assertTrue(recorder.dirtyNodes(map, PersistenceState.MODIFIED).contains(modified));
        assertTrue(recorder.dirtyNodes(map, PersistenceState.COMMITTED).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.DELETED).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.NEW).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.TRANSIENT).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.HOLLOW).isEmpty());

        MockPersistentObject deleted = new MockPersistentObject();
        deleted.setObjectId(new ObjectId(Object.class, "key", "value2"));
        deleted.setPersistenceState(PersistenceState.DELETED);
        map.registerNode(deleted.getObjectId(), deleted);
        recorder.nodeDeleted(deleted.getObjectId());

        assertTrue(recorder.dirtyNodes(map, PersistenceState.MODIFIED).contains(modified));
        assertTrue(recorder.dirtyNodes(map, PersistenceState.COMMITTED).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.DELETED).contains(deleted));
        assertTrue(recorder.dirtyNodes(map, PersistenceState.NEW).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.TRANSIENT).isEmpty());
        assertTrue(recorder.dirtyNodes(map, PersistenceState.HOLLOW).isEmpty());
    }

    public void testDirtyNodes() {
        GraphMap map = new MockGraphMap();
        ClientStateRecorder recorder = new ClientStateRecorder();

        assertNotNull(recorder.dirtyNodes(map));
        assertTrue(recorder.dirtyNodes(map).isEmpty());

        // introduce a fake dirty object
        MockPersistentObject object = new MockPersistentObject();
        object.setObjectId(new ObjectId(Object.class, "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        map.registerNode(object.getObjectId(), object);
        recorder.nodePropertyChanged(object.getObjectId(), "a", "b", "c");

        assertTrue(recorder.dirtyNodes(map).contains(object));

        // must go away on clear...
        recorder.clear();
        assertNotNull(recorder.dirtyNodes(map));
        assertTrue(recorder.dirtyNodes(map).isEmpty());
    }

    public void testHasChanges() {

        ClientStateRecorder recorder = new ClientStateRecorder();
        assertFalse(recorder.hasChanges());

        // introduce a fake dirty object
        MockPersistentObject object = new MockPersistentObject();
        object.setObjectId(new ObjectId(Object.class, "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        recorder.nodePropertyChanged(object.getObjectId(), "xyz", "a", "b");

        assertTrue(recorder.hasChanges());

        // must go away on clear...
        recorder.clear();
        assertFalse(recorder.hasChanges());
    }

}
