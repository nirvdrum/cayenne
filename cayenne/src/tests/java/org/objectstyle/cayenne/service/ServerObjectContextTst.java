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

import junit.framework.TestCase;

import org.objectstyle.cayenne.MockPersistenceContext;
import org.objectstyle.cayenne.PersistenceContext;
import org.objectstyle.cayenne.access.MockDataRowStore;
import org.objectstyle.cayenne.distribution.NamedQueryMessage;
import org.objectstyle.cayenne.distribution.QueryMessage;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.MockQuery;

/**
 * @author Andrus Adamchik
 */
public class ServerObjectContextTst extends TestCase {

    public void testParentContext() {
        PersistenceContext parent = new MockPersistenceContext();
        ServerObjectContext context = new ServerObjectContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());
        assertSame(parent, context.getParentContext());
    }

    //    public void testOnCommit() {
    //        MockPersistenceContext parent = new MockPersistenceContext();
    //        ServerObjectContext context = new ServerObjectContext(
    //                parent,
    //                new EntityResolver(),
    //                new MockDataRowStore());
    //
    //        context.onCommit(new CommitMessage());
    //
    //        // no changes in context, so no commit should be executed
    //        assertFalse(parent.isCommitChangesInContext());
    //
    //        // introduce changes
    //        fail("TODO: implement CommitMessage to be able to carry client changes");
    //    }

    public void testOnNonSelectingQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ServerObjectContext context = new ServerObjectContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        context.onQuery(new QueryMessage(new MockQuery(), false));
        assertTrue(parent.isPerformQuery());
    }

    //    public void testOnSelectingQuery() {
    //        MockPersistenceContext parent = new MockPersistenceContext();
    //        ServerObjectContext context = new ServerObjectContext(
    //                parent,
    //                new EntityResolver(),
    //                new MockDataRowStore());
    //
    //        context.onQuery(new QueryMessage(new MockGenericSelectQuery(), true));
    //        assertTrue(parent.isPerformQuery());
    //    }

    public void testOnNonSelectingNamedQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ServerObjectContext context = new ServerObjectContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        context.onNamedQuery(new NamedQueryMessage(
                "test",
                Collections.EMPTY_MAP,
                false,
                false));
        assertTrue(parent.isPerformQuery());
    }

    //    public void testOnNamedSelectingQuery() {
    //        MockPersistenceContext parent = new MockPersistenceContext();
    //        ServerObjectContext context = new ServerObjectContext(
    //                parent,
    //                new EntityResolver(),
    //                new MockDataRowStore());
    //
    //        context.onNamedQuery(new NamedQueryMessage(
    //                "test",
    //                Collections.EMPTY_MAP,
    //                true,
    //                false));
    //        assertTrue(parent.isPerformQuery());
    //    }
}
