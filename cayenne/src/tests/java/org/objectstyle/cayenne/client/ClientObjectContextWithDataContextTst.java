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

import java.util.Arrays;
import java.util.List;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.PersistentObjectHolder;
import org.objectstyle.cayenne.PersistentObjectList;
import org.objectstyle.cayenne.distribution.GlobalID;
import org.objectstyle.cayenne.distribution.MockOPPChannel;
import org.objectstyle.cayenne.query.QueryExecutionPlan;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class ClientObjectContextWithDataContextTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testBeforePropertyReadShouldInflateHollow() {

        GlobalID gid = new GlobalID("MtTable1", "a", "b");
        final ClientMtTable1 inflated = new ClientMtTable1();
        inflated.setPersistenceState(PersistenceState.COMMITTED);
        inflated.setGlobalID(gid);
        inflated.setGlobalAttribute1("abc");

        MockOPPChannel channel = new MockOPPChannel(Arrays.asList(new Object[] {
            inflated
        }));

        // check that a HOLLOW object is infalted on "beforePropertyRead"
        ClientMtTable1 hollow = new ClientMtTable1();
        hollow.setPersistenceState(PersistenceState.HOLLOW);
        hollow.setGlobalID(gid);

        final boolean[] selectExecuted = new boolean[1];
        ClientObjectContext context = new ClientObjectContext(channel) {

            public List performSelectQuery(QueryExecutionPlan query) {
                selectExecuted[0] = true;
                return super.performSelectQuery(query);
            }
        };

        context.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());

        context.graphManager.registerNode(hollow.getGlobalID(), hollow);

        // testing this...
        context.prepareForAccess(hollow, ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY);
        assertTrue(selectExecuted[0]);
        assertEquals(inflated.getGlobalAttribute1Direct(), hollow
                .getGlobalAttribute1Direct());
        assertEquals(PersistenceState.COMMITTED, hollow.getPersistenceState());
    }

    public void testNewObjectShouldInflateHolders() {

        ClientObjectContext context = new ClientObjectContext(new MockOPPChannel());
        context.setEntityResolver(getDomain()
                .getEntityResolver()
                .getClientEntityResolver());

        // test that holders are present and that they are resolved... (new object has no
        // relationships by definition, so no need to keep holders as faults).

        // to one
        ClientMtTable2 o1 = (ClientMtTable2) context.newObject(ClientMtTable2.class);
        assertNotNull(o1.getTable1Direct());
        assertFalse(((PersistentObjectHolder) o1.getTable1Direct()).isFault());

        // to many
        ClientMtTable1 o2 = (ClientMtTable1) context.newObject(ClientMtTable1.class);
        assertNotNull(o2.getTable2ArrayDirect());
        assertFalse(((PersistentObjectList) o2.getTable2ArrayDirect()).isFault());
    }
}
