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
package org.objectstyle.cayenne.service;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.MockDataRowStore;
import org.objectstyle.cayenne.access.MockPersistenceContext;
import org.objectstyle.cayenne.access.PersistenceContext;
import org.objectstyle.cayenne.distribution.CommitMessage;
import org.objectstyle.cayenne.distribution.GenericQueryMessage;
import org.objectstyle.cayenne.distribution.GlobalID;
import org.objectstyle.cayenne.distribution.SelectMessage;
import org.objectstyle.cayenne.distribution.UpdateMessage;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.MockGraphDiff;
import org.objectstyle.cayenne.graph.OperationRecorder;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.MockGenericSelectQuery;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

/**
 * @author Andrus Adamchik
 */
public class ServerObjectContextTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testEntityResolver() {
        EntityResolver resolver = new EntityResolver();
        ServerObjectContext context = new ServerObjectContext(
                new MockPersistenceContext(),
                resolver,
                new MockDataRowStore());
        assertSame(resolver, context.getEntityResolver());
    }

    public void testParentContext() {
        PersistenceContext parent = new MockPersistenceContext();
        ServerObjectContext context = new ServerObjectContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());
        assertSame(parent, context.getParentContext());
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
        ServerObjectContext context = new ServerObjectContext(parent, getDomain()
                .getEntityResolver(), new MockDataRowStore());

        context.onCommit(new CommitMessage(new MockGraphDiff()));

        // no changes in context, so no commit should be executed
        assertFalse(parent.isCommitChangesInContext());

        parent.reset();

        // introduce changes
        OperationRecorder recorder = new OperationRecorder();
        recorder.nodeCreated(new GlobalID("MtTable1"));

        context.onCommit(new CommitMessage(recorder.getDiffs()));
        assertTrue(parent.isCommitChangesInContext());
    }

    public void testOnUpdateQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ServerObjectContext context = new ServerObjectContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        UpdateMessage message = new UpdateMessage(new MockQuery());
        context.onUpdateQuery(message);
        assertTrue(parent.isPerformQuery());
    }

    public void testOnSelectQuery() {

        final ObjEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                MtTable1.class);
        final ObjectId oid = new ObjectId(MtTable1.class, "key", 1);
        MtTable1 serverObject = new MtTable1() {

            public ObjEntity getObjEntity() {
                return entity;
            }

            public ObjectId getObjectId() {
                return oid;
            }
        };

        MockPersistenceContext parent = new MockPersistenceContext(getDomain()
                .getEntityResolver(), Collections.singletonList(serverObject));

        ServerObjectContext context = new ServerObjectContext(parent, getDomain()
                .getEntityResolver(), new MockDataRowStore());

        SelectMessage message = new SelectMessage(new MockGenericSelectQuery(true));
        List results = context.onSelectQuery(message);
        assertTrue(parent.isPerformQuery());

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(result instanceof ClientMtTable1);
        ClientMtTable1 clientObject = (ClientMtTable1) result;
        assertNotNull(clientObject.getGlobalID());

        GlobalID refId = getDomain().getEntityResolver().convertToGlobalID(
                new ObjectId(MtTable1.class, "key", 1));
        assertEquals(refId, clientObject.getGlobalID());
    }

    public void testOnGenericQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ServerObjectContext context = new ServerObjectContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        GenericQueryMessage message = new GenericQueryMessage(new MockQuery());
        context.onGenericQuery(message);
        assertTrue(parent.isPerformQuery());
    }
}
