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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.MockGraphDiff;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.opp.BootstrapMessage;
import org.objectstyle.cayenne.opp.GenericQueryMessage;
import org.objectstyle.cayenne.opp.SelectMessage;
import org.objectstyle.cayenne.opp.SyncMessage;
import org.objectstyle.cayenne.opp.UpdateMessage;
import org.objectstyle.cayenne.query.MockGenericSelectQuery;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1Subclass;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable3;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable1Subclass;
import org.objectstyle.cayenne.testdo.mt.MtTable3;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

/**
 * @author Andrus Adamchik
 */
public class ClientServerChannelTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testOnBootstrap() throws Exception {
        EntityResolver resolver = new ClientServerChannel(getDomain())
                .onBootstrap(new BootstrapMessage());
        assertNotNull(resolver);
        assertNotNull(resolver.lookupObjEntity(ClientMtTable1.class));
    }

    public void testOnCommit() {
        MockPersistenceContext parent = new MockPersistenceContext() {

            public void commitChangesInContext(
                    ObjectContext context,
                    GraphChangeHandler callback) {
                super.commitChangesInContext(context, callback);

                // replace temp ids to satisfy ObjectStore
                Iterator it = context.newObjects().iterator();
                int i = 0;
                while (it.hasNext()) {
                    DataObject o = (DataObject) it.next();
                    o.getObjectId().getReplacementIdMap().put("x", "y" + i++);
                }
            }
        };
        ObjectDataContext context = new ObjectDataContext(parent, getDomain()
                .getEntityResolver(), new MockDataRowStore());

        ClientServerChannel channel = new ClientServerChannel(context, false);
        channel.onSync(new SyncMessage(
                context,
                SyncMessage.COMMIT_TYPE,
                new MockGraphDiff()));

        // no changes in context, so no commit should be executed
        assertFalse(parent.isCommitChangesInContext());

        parent.reset();

        // introduce changes
        channel.onSync(new SyncMessage(
                context,
                SyncMessage.COMMIT_TYPE,
                new NodeCreateOperation(new ObjectId("MtTable1"))));
        assertTrue(parent.isCommitChangesInContext());
    }

    public void testOnUpdateQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        UpdateMessage message = new UpdateMessage(new MockQuery());
        new ClientServerChannel(context, false).onUpdateQuery(message);
        assertTrue(parent.isPerformQuery());
    }

    public void testOnSelectQueryGlobalIDInjection() {

        ObjEntity entity = getDomain()
                .getEntityResolver()
                .lookupObjEntity(MtTable1.class);
        ObjectId oid = new ObjectId("MtTable1", "key", new Integer(1));
        MtTable1 serverObject = new MtTable1();
        serverObject.setObjectId(oid);
        serverObject.setObjEntity(entity);

        MockPersistenceContext parent = new MockPersistenceContext(getDomain()
                .getEntityResolver(), Collections.singletonList(serverObject));

        ObjectDataContext context = new ObjectDataContext(parent, getDomain()
                .getEntityResolver(), new MockDataRowStore());

        SelectMessage message = new SelectMessage(new MockGenericSelectQuery(true));
        List results = new ClientServerChannel(context, false).onSelectQuery(message);
        assertTrue(parent.isPerformQuery());

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(result instanceof ClientMtTable1);
        ClientMtTable1 clientObject = (ClientMtTable1) result;
        assertNotNull(clientObject.getObjectId());

        assertEquals(oid, clientObject.getObjectId());
    }

    public void testOnSelectQueryValuePropagation() {

        ObjEntity entity = getDomain()
                .getEntityResolver()
                .lookupObjEntity(MtTable3.class);

        MtTable3 serverObject = new MtTable3();
        serverObject.setObjectId(new ObjectId("MtTable3", "key", new Integer(1)));
        serverObject.setObjEntity(entity);

        serverObject.setBinaryColumn(new byte[] {
                1, 2, 3
        });
        serverObject.setCharColumn("abc");
        serverObject.setIntColumn(new Integer(4));

        MockPersistenceContext parent = new MockPersistenceContext(getDomain()
                .getEntityResolver(), Collections.singletonList(serverObject));

        ObjectDataContext context = new ObjectDataContext(parent, getDomain()
                .getEntityResolver(), new MockDataRowStore());

        SelectMessage message = new SelectMessage(new MockGenericSelectQuery(true));
        List results = new ClientServerChannel(context, false).onSelectQuery(message);

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue("Result is of wrong type: " + result, result instanceof ClientMtTable3);
        ClientMtTable3 clientObject = (ClientMtTable3) result;

        assertEquals(serverObject.getCharColumn(), clientObject.getCharColumn());
        assertEquals(serverObject.getIntColumn(), clientObject.getIntColumn());
        assertTrue(new EqualsBuilder().append(
                clientObject.getBinaryColumn(),
                serverObject.getBinaryColumn()).isEquals());
    }

    public void testOnSelectQueryValuePropagationInheritance() {

        final ObjEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                MtTable1Subclass.class);

        MtTable1Subclass serverObject = new MtTable1Subclass();
        serverObject.setObjectId(new ObjectId("MtTable1Subclass", "key", new Integer(1)));
        serverObject.setObjEntity(entity);

        serverObject.setGlobalAttribute1("abc");

        MockPersistenceContext parent = new MockPersistenceContext(getDomain()
                .getEntityResolver(), Collections.singletonList(serverObject));

        ObjectDataContext context = new ObjectDataContext(parent, getDomain()
                .getEntityResolver(), new MockDataRowStore());

        SelectMessage message = new SelectMessage(new MockGenericSelectQuery(true));
        List results = new ClientServerChannel(context, false).onSelectQuery(message);

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(
                "Result is of wrong type: " + result,
                result instanceof ClientMtTable1Subclass);
        ClientMtTable1Subclass clientObject = (ClientMtTable1Subclass) result;

        assertEquals(serverObject.getGlobalAttribute1(), clientObject
                .getGlobalAttribute1());
    }

    public void testOnGenericQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        GenericQueryMessage message = new GenericQueryMessage(new MockQuery());
        new ClientServerChannel(context, false).onGenericQuery(message);
        assertTrue(parent.isPerformQuery());
    }
}
