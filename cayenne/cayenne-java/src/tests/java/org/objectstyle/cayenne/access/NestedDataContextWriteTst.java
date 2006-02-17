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
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class NestedDataContextWriteTst extends CayenneTestCase {

    public void testFlushChanges() throws Exception {
        deleteTestData();
        createTestData("testFlushChanges");

        DataContext context = createDataContext();
        DataContext childContext = context.createChildDataContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, true);
        List objects = childContext.performQuery(query);

        assertEquals(4, objects.size());

        Artist childNew = (Artist) childContext.newObject(Artist.class);
        childNew.setArtistName("NNN");

        Artist childModified = (Artist) objects.get(0);
        childModified.setArtistName("MMM");

        Artist childCommitted = (Artist) objects.get(1);

        Artist childHollow = (Artist) objects.get(3);
        childContext.invalidateObjects(Collections.singleton(childHollow));

        blockQueries();

        try {
            childContext.flushChanges();

            // * all modified child objects must be in committed state now
            // * all modifications should be propagated to the parent
            // * no actual commit should occur.

            assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
            assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

            Artist parentNew = (Artist) context.getObjectStore().getObject(
                    childNew.getObjectId());
            Artist parentModified = (Artist) context.getObjectStore().getObject(
                    childModified.getObjectId());
            Artist parentCommitted = (Artist) context.getObjectStore().getObject(
                    childCommitted.getObjectId());
            Artist parentHollow = (Artist) context.getObjectStore().getObject(
                    childHollow.getObjectId());

            assertNotNull(parentNew);
            assertEquals(PersistenceState.NEW, parentNew.getPersistenceState());
            assertEquals("NNN", parentNew.getArtistName());

            assertNotNull(parentModified);
            assertEquals(PersistenceState.MODIFIED, parentModified.getPersistenceState());
            assertEquals("MMM", parentModified.getArtistName());

            assertNotNull(parentCommitted);
            assertEquals(PersistenceState.COMMITTED, parentCommitted
                    .getPersistenceState());

            assertNotNull(parentHollow);
            // TODO: we can assert that when we figure out how nested "invalidate" should
            // work
            // assertEquals(PersistenceState.HOLLOW, parentHollow.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    public void testFlushChangesDeleted() throws Exception {
        deleteTestData();
        createTestData("testFlushChanges");

        DataContext context = createDataContext();
        DataContext childContext = context.createChildDataContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, true);
        List objects = childContext.performQuery(query);

        assertEquals(4, objects.size());

        // delete AND modify
        Artist childDeleted = (Artist) objects.get(2);
        childContext.deleteObject(childDeleted);
        childDeleted.setArtistName("DDD");

        // don't block queries - on delete Cayenne may need to resolve delete rules via
        // fetch
        childContext.flushChanges();

        // * all modified child objects must be in committed state now
        // * all modifications should be propagated to the parent
        // * no actual commit should occur.

        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());

        Artist parentDeleted = (Artist) context.getObjectStore().getObject(
                childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());
        assertEquals("DDD", parentDeleted.getArtistName());
    }
}
