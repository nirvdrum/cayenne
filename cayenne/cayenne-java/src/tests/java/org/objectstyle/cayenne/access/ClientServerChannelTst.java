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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.MockQueryResponse;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.MockGraphDiff;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.opp.BootstrapMessage;
import org.objectstyle.cayenne.opp.GenericQueryMessage;
import org.objectstyle.cayenne.opp.MockOPPChannel;
import org.objectstyle.cayenne.opp.SelectMessage;
import org.objectstyle.cayenne.opp.SyncMessage;
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

        final boolean[] commitDone = new boolean[1];
        MockOPPChannel parent = new MockOPPChannel(getDomain().getEntityResolver()) {

            public GraphDiff onSync(SyncMessage message) {
                commitDone[0] = true;

                // replace temp ids to satisfy ObjectStore
                Iterator it = message.getSource().newObjects().iterator();
                int i = 0;
                while (it.hasNext()) {
                    DataObject o = (DataObject) it.next();
                    o.getObjectId().getReplacementIdMap().put("x", "y" + i++);
                }

                return super.onSync(message);
            }
        };
        ObjectDataContext context = new ObjectDataContext(parent, new MockDataRowStore());

        ClientServerChannel channel = new ClientServerChannel(context, false);
        channel.onSync(new SyncMessage(
                context,
                SyncMessage.COMMIT_TYPE,
                new MockGraphDiff()));

        // no changes in context, so no commit should be executed
        assertFalse(commitDone[0]);

        // introduce changes
        channel.onSync(new SyncMessage(
                context,
                SyncMessage.COMMIT_TYPE,
                new NodeCreateOperation(new ObjectId("MtTable1"))));
        assertTrue(commitDone[0]);
    }

    public void testOnSelectQueryGlobalIDInjection() {

        ObjectId oid = new ObjectId("MtTable1", "TABLE1_ID", new Integer(1));
        QueryResponse response = new MockQueryResponse(oid.getIdSnapshot());

        final boolean[] selectDone = new boolean[1];
        MockOPPChannel parent = new MockOPPChannel(
                getDomain().getEntityResolver(),
                response) {

            // note that select query calls "onGenericQuery" on the channel
            public QueryResponse onGenericQuery(GenericQueryMessage message) {
                selectDone[0] = true;
                return super.onGenericQuery(message);
            }
        };

        ObjectDataContext context = new ObjectDataContext(parent, new MockDataRowStore());

        SelectMessage message = new SelectMessage(new MockGenericSelectQuery("MtTable1"));
        List results = new ClientServerChannel(context, false).onSelectQuery(message);
        assertTrue(selectDone[0]);

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(result instanceof ClientMtTable1);
        ClientMtTable1 clientObject = (ClientMtTable1) result;
        assertNotNull(clientObject.getObjectId());

        assertEquals(oid, clientObject.getObjectId());
    }

    public void testOnSelectQueryValuePropagation() {

        ObjectId oid = new ObjectId(
                "MtTable3",
                MtTable3.TABLE3_ID_PK_COLUMN,
                new Integer(1));
        Map row = new HashMap(oid.getIdSnapshot());

        row.put("BINARY_COLUMN", new byte[] {
                1, 2, 3
        });

        row.put("CHAR_COLUMN", "abc");
        row.put("INT_COLUMN", new Integer(4));

        MockOPPChannel parent = new MockOPPChannel(
                getDomain().getEntityResolver(),
                new MockQueryResponse(row));

        ObjectDataContext context = new ObjectDataContext(parent, new MockDataRowStore());

        SelectMessage message = new SelectMessage(new MockGenericSelectQuery("MtTable3"));
        List results = new ClientServerChannel(context, false).onSelectQuery(message);

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue("Result is of wrong type: " + result, result instanceof ClientMtTable3);
        ClientMtTable3 clientObject = (ClientMtTable3) result;

        assertEquals(row.get("CHAR_COLUMN"), clientObject.getCharColumn());
        assertEquals(row.get("INT_COLUMN"), clientObject.getIntColumn());
        assertTrue(new EqualsBuilder().append(
                clientObject.getBinaryColumn(),
                row.get("BINARY_COLUMN")).isEquals());
    }

    public void testOnSelectQueryValuePropagationInheritance() {

        ObjectId oid = new ObjectId(
                "MtTable1Subclass",
                MtTable1Subclass.TABLE1_ID_PK_COLUMN,
                new Integer(1));

        Map row = new HashMap(oid.getIdSnapshot());

        // note that "sub1" is used in subclass qualifier
        row.put("GLOBAL_ATTRIBUTE1", "sub1");
        row.put("SERVER_ATTRIBUTE1", "xyz");

        MockOPPChannel parent = new MockOPPChannel(
                getDomain().getEntityResolver(),
                new MockQueryResponse(row));

        ObjectDataContext context = new ObjectDataContext(parent, new MockDataRowStore());

        MockGenericSelectQuery query = new MockGenericSelectQuery(MtTable1.class);
        query.setResolvingInherited(true);
        SelectMessage message = new SelectMessage(query);
        List results = new ClientServerChannel(context, false).onSelectQuery(message);

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(
                "Result is of wrong type: " + result,
                result instanceof ClientMtTable1Subclass);
        ClientMtTable1Subclass clientObject = (ClientMtTable1Subclass) result;

        assertEquals(
                "Invalid object: " + clientObject,
                row.get("GLOBAL_ATTRIBUTE1"),
                clientObject.getGlobalAttribute1());
    }

    public void testOnGenericQuery() {

        final boolean[] genericDone = new boolean[1];
        MockOPPChannel parent = new MockOPPChannel(new EntityResolver()) {

            public QueryResponse onGenericQuery(GenericQueryMessage message) {
                genericDone[0] = true;
                return super.onGenericQuery(message);
            }
        };
        ObjectDataContext context = new ObjectDataContext(parent, new MockDataRowStore());

        GenericQueryMessage message = new GenericQueryMessage(new MockQuery());
        new ClientServerChannel(context, false).onGenericQuery(message);
        assertTrue(genericDone[0]);
    }
}
