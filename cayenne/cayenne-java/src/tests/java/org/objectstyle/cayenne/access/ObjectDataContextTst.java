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

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.MockDataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.ObjectDataContext;
import org.objectstyle.cayenne.access.PersistenceContext;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.MockEntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.MockGenericSelectQuery;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.query.MockQueryExecutionPlan;

/**
 * @author Andrus Adamchik
 */
public class ObjectDataContextTst extends TestCase {

    public void testParentContext() {
        PersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());
        assertSame(parent, context.getParentContext());
    }

    public void testHasChanges() {
        // mocking 1.1 Cayenne stack is painful...
        MockDataRowStore cache = new MockDataRowStore();
        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(parent, new MockEntityResolver(
                new ObjEntity("test")), cache);

        assertFalse(context.hasChanges());

        ObjectId oid = new ObjectId(Object.class, "key", "value");
        DataObject object = new MockDataObject(context, oid, PersistenceState.MODIFIED);
        context.getObjectStore().addObject(object);
        cache.putSnapshot(oid, Collections.singletonMap("p1", "v1"));

        assertTrue(context.hasChanges());
    }

    public void testCommitChanges() {

        // mocking 1.1 Cayenne stack is painful...
        MockDataRowStore cache = new MockDataRowStore();
        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(parent, new MockEntityResolver(
                new ObjEntity("test")), cache);

        context.commitChanges();

        // no changes in context, so no commit should be executed
        assertFalse(parent.isCommitChangesInContext());

        // introduce changes
        ObjectId oid = new ObjectId(Object.class, "key", "value");
        DataObject object = new MockDataObject(context, oid, PersistenceState.MODIFIED);
        context.getObjectStore().addObject(object);
        cache.putSnapshot(oid, Collections.singletonMap("p1", "v1"));

        assertTrue(context.hasChanges());
        context.commitChanges();
        assertTrue(parent.isCommitChangesInContext());
        assertFalse(context.hasChanges());
    }

    public void testPerformNonSelectingQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        context.performNonSelectingQuery(new MockQuery());
        assertTrue(parent.isPerformQuery());
    }

    public void testPerformQuery() {
        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        // perform both generic select and a "plan" query to test both legacy and new API
        context.performQuery(new MockGenericSelectQuery(true));
        assertTrue(parent.isPerformQuery());

        context.performSelectQuery(new MockQueryExecutionPlan(true));
        assertTrue(parent.isPerformQuery());
    }

    public void testUncommittedObjects() {

        MockPersistenceContext parent = new MockPersistenceContext();
        ObjectDataContext context = new ObjectDataContext(
                parent,
                new EntityResolver(),
                new MockDataRowStore());

        DataObject newObject = new MockDataObject(context, new ObjectId(
                MockDataObject.class), PersistenceState.NEW);
        context.getObjectStore().addObject(newObject);
        Collection uncommitted1 = context.uncommittedObjects();
        assertNotNull(uncommitted1);
        assertEquals(1, uncommitted1.size());
        assertTrue(uncommitted1.contains(newObject));

        DataObject modifiedObject = new MockDataObject(context, new ObjectId(
                MockDataObject.class), PersistenceState.MODIFIED);
        context.getObjectStore().addObject(modifiedObject);
        Collection uncommitted2 = context.uncommittedObjects();
        assertNotNull(uncommitted2);
        assertEquals(2, uncommitted2.size());
        assertTrue(uncommitted2.contains(newObject));
        assertTrue(uncommitted2.contains(modifiedObject));

        DataObject deletedObject = new MockDataObject(context, new ObjectId(
                MockDataObject.class), PersistenceState.DELETED);
        context.getObjectStore().addObject(deletedObject);
        Collection uncommitted3 = context.uncommittedObjects();
        assertNotNull(uncommitted3);
        assertEquals(3, uncommitted3.size());
        assertTrue(uncommitted3.contains(newObject));
        assertTrue(uncommitted3.contains(modifiedObject));
        assertTrue(uncommitted3.contains(deletedObject));

        DataObject committedObject = new MockDataObject(context, new ObjectId(
                MockDataObject.class), PersistenceState.COMMITTED);
        context.getObjectStore().addObject(committedObject);
        Collection uncommitted4 = context.uncommittedObjects();
        assertNotNull(uncommitted4);
        assertEquals(3, uncommitted4.size());
        assertTrue(uncommitted4.contains(newObject));
        assertTrue(uncommitted4.contains(modifiedObject));
        assertTrue(uncommitted4.contains(deletedObject));
    }
}
