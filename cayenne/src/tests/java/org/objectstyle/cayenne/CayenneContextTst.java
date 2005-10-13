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
package org.objectstyle.cayenne;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.MockGraphDiff;
import org.objectstyle.cayenne.graph.NodeIdChangeOperation;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.opp.CommitMessage;
import org.objectstyle.cayenne.opp.MockOPPChannel;
import org.objectstyle.cayenne.opp.OPPMessage;
import org.objectstyle.cayenne.query.NamedQuery;

/**
 * @author Andrus Adamchik
 */
public class CayenneContextTst extends TestCase {

    public void testConstructor() {

        CayenneContext context = new CayenneContext();

        // test default property parameters
        assertNotNull(context.getGraphManager());
        assertNull(context.getChannel());

        MockOPPChannel channel = new MockOPPChannel();
        context.setChannel(channel);
        assertSame(channel, context.getChannel());
    }

    public void testChannel() {
        MockOPPChannel channel = new MockOPPChannel();
        CayenneContext context = new CayenneContext(channel);

        assertSame(channel, context.getChannel());
    }

    public void testCommitUnchanged() {

        MockOPPChannel channel = new MockOPPChannel();
        CayenneContext context = new CayenneContext(channel);

        // no context changes so no connector access is expected
        context.commitChanges();
        assertTrue(channel.getMessages().isEmpty());
    }

    public void testCommitCommandExecuted() {

        MockOPPChannel channel = new MockOPPChannel(new MockGraphDiff());
        CayenneContext context = new CayenneContext(channel);

        // test that a command is being sent via connector on commit...

        context.internalGraphManager().nodePropertyChanged(
                new Object(),
                "x",
                "y",
                "z");

        context.commitChanges();
        assertEquals(1, channel.getMessages().size());

        // expect a sync/commit chain
        OPPMessage mainMessage = (OPPMessage) channel.getMessages().iterator().next();
        assertTrue(mainMessage instanceof CommitMessage);
    }

    public void testCommitChangesNew() {
        final CompoundDiff diff = new CompoundDiff();
        final Object newObjectId = new GlobalID("test", "key", "generated");
        final EventManager eventManager = new EventManager(0);

        // test that ids that are passed back are actually propagated to the right
        // objects...

        MockOPPChannel channel = new MockOPPChannel() {

            public GraphDiff onCommit(CommitMessage message) {
                return diff;
            }

            // must provide a channel with working event manager
            public EventManager getEventManager() {
                return eventManager;
            }
        };

        CayenneContext context = new CayenneContext(channel);
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));
        Persistent object = context.newObject(MockPersistentObject.class);

        // record change here to make it available to the anonymous connector method..
        diff.add(new NodeIdChangeOperation(object.getGlobalID(), newObjectId));

        // check that a generated object ID is assigned back to the object...
        assertNotSame(newObjectId, object.getGlobalID());
        context.commitChanges();
        assertSame(newObjectId, object.getGlobalID());
        assertSame(object, context.graphManager.getNode(newObjectId));
    }

    public void testPerformSelectQuery() {
        final MockPersistentObject o1 = new MockPersistentObject();
        GlobalID oid1 = new GlobalID("test_entity");
        o1.setGlobalID(oid1);

        MockOPPChannel channel = new MockOPPChannel(Arrays.asList(new Object[] {
            o1
        }));

        CayenneContext context = new CayenneContext(channel);
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

        List list = context.performSelectQuery(new NamedQuery("dummy"));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(o1));

        // ObjectContext must be injected
        assertEquals(context, o1.getObjectContext());
        assertSame(o1, context.graphManager.getNode(oid1));
    }

    public void testPerformSelectQueryOverrideCached() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(entities);

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        GlobalID oid = new GlobalID("test_entity", "x", "y");

        MockPersistentObject o1 = new MockPersistentObject(oid);
        context.graphManager.registerNode(oid, o1);
        assertSame(o1, context.getGraphManager().getNode(oid));

        // another object with the same GID ... we must merge it with cached and return
        // cached object instead of the one fetched
        MockPersistentObject o2 = new MockPersistentObject(oid);
        MockOPPChannel channel = new MockOPPChannel(Arrays.asList(new Object[] {
            o2
        }));

        context.setChannel(channel);
        List list = context.performSelectQuery(new NamedQuery("dummy"));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue("Expected cached object, got: " + list, list.contains(o1));
        assertSame(o1, context.graphManager.getNode(oid));
    }

    public void testPerformSelectQueryOverrideModifiedCached() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());
        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(entities);
        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        GlobalID oid = new GlobalID("test_entity", "x", "y");

        MockPersistentObject o1 = new MockPersistentObject(oid);
        o1.setPersistenceState(PersistenceState.MODIFIED);
        context.graphManager.registerNode(oid, o1);
        assertSame(o1, context.getGraphManager().getNode(oid));

        // another object with the same GID ... we must merge it with cached and return
        // cached object instead of the one fetched
        MockPersistentObject o2 = new MockPersistentObject(oid);
        MockOPPChannel channel = new MockOPPChannel(Arrays.asList(new Object[] {
            o2
        }));

        context.setChannel(channel);
        List list = context.performSelectQuery(new NamedQuery("dummy"));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue("Expected cached object, got: " + list, list.contains(o1));
        assertSame(o1, context.graphManager.getNode(oid));
    }

    public void testNewObject() {

        CayenneContext context = new CayenneContext(new MockOPPChannel());

        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

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
        Persistent object = context.newObject(MockPersistentObject.class);
        assertNotNull(object);
        assertTrue(object instanceof MockPersistentObject);
        assertEquals(PersistenceState.NEW, object.getPersistenceState());
        assertSame(context, object.getObjectContext());
        assertTrue(context
                .internalGraphManager()
                .dirtyNodes(PersistenceState.NEW)
                .contains(object));
        assertNotNull(object.getGlobalID());
        assertTrue(object.getGlobalID() instanceof GlobalID);
        assertTrue(((GlobalID) object.getGlobalID()).isTemporary());
    }

    public void testDeleteObject() {

        CayenneContext context = new CayenneContext(new MockOPPChannel());
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

        // TRANSIENT ... should quietly ignore it
        Persistent transientObject = new MockPersistentObject();
        context.deleteObject(transientObject);
        assertEquals(PersistenceState.TRANSIENT, transientObject.getPersistenceState());

        // NEW ... should make it TRANSIENT
        // create via context to make sure that object store would register it
        Persistent newObject = context.newObject(MockPersistentObject.class);
        context.deleteObject(newObject);
        assertEquals(PersistenceState.TRANSIENT, newObject.getPersistenceState());
        assertFalse(context.internalGraphManager().dirtyNodes().contains(
                newObject));

        // COMMITTED
        Persistent committed = new MockPersistentObject();
        committed.setPersistenceState(PersistenceState.COMMITTED);
        committed.setGlobalID(new GlobalID("MockPersistentObject", "key", "value1"));
        context.deleteObject(committed);
        assertEquals(PersistenceState.DELETED, committed.getPersistenceState());

        // MODIFIED
        Persistent modified = new MockPersistentObject();
        modified.setPersistenceState(PersistenceState.MODIFIED);
        modified.setGlobalID(new GlobalID("MockPersistentObject", "key", "value2"));
        context.deleteObject(modified);
        assertEquals(PersistenceState.DELETED, modified.getPersistenceState());

        // DELETED
        Persistent deleted = new MockPersistentObject();
        deleted.setPersistenceState(PersistenceState.DELETED);
        deleted.setGlobalID(new GlobalID("MockPersistentObject", "key", "value3"));
        context.deleteObject(deleted);
        assertEquals(PersistenceState.DELETED, committed.getPersistenceState());
    }
}