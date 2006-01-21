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
import java.util.Date;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.query.SingleObjectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Tests objects registration in DataContext, transferring objects between contexts and
 * such.
 * 
 * @author Andrus Adamchik
 */
public class DataContextObjectTrackingTst extends CayenneTestCase {

    public void testUnregisterObject() {

        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getDataContext());
        assertSame(obj, context.getObjectStore().getObject(oid));

        context.unregisterObjects(Collections.singletonList(obj));

        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());
        assertNull(obj.getDataContext());
        assertNull(obj.getObjectId());
        assertNull(context.getObjectStore().getObject(oid));
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
    }

    public void testInvalidateObject() {
        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getDataContext());
        assertSame(obj, context.getObjectStore().getObject(oid));

        context.invalidateObjects(Collections.singletonList(obj));

        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertSame(context, obj.getDataContext());
        assertSame(oid, obj.getObjectId());
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
        assertNotNull(context.getObjectStore().getObject(oid));
    }

    public void testLocalObjectsPeerContext() throws Exception {
        deleteTestData();
        createTestData("testArtists");

        // must create both contexts before running the queries, as each call to
        // 'createDataContext' clears the cache.
        DataContext context = createDataContext();
        DataContext peerContext = createDataContext();

        DataObject _new = context.createAndRegisterNewObject(Artist.class);

        DataObject hollow = context.registeredObject(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33001));
        DataObject committed = DataObjectUtils.objectForQuery(
                context,
                new SingleObjectQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33002)));

        int modifiedId = 33003;
        Artist modified = (Artist) DataObjectUtils.objectForQuery(
                context,
                new SingleObjectQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        modified.setArtistName("MODDED");
        DataObject deleted = DataObjectUtils.objectForQuery(
                context,
                new SingleObjectQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33004)));
        context.deleteObject(deleted);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        // now check how objects in different state behave
        try {
            peerContext.localObjects(Collections.singletonList(_new));
            fail("A presence of new object should have triggered an exception");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }

        blockQueries();

        try {

            List hollows = peerContext.localObjects(Collections.singletonList(hollow));
            assertEquals(1, hollows.size());
            DataObject hollowPeer = (DataObject) hollows.get(0);
            assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
            assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
            assertSame(peerContext, hollowPeer.getDataContext());
            assertSame(context, hollow.getDataContext());

            List commits = peerContext.localObjects(Collections.singletonList(committed));
            assertEquals(1, commits.size());
            DataObject committedPeer = (DataObject) commits.get(0);
            assertEquals(PersistenceState.HOLLOW, committedPeer.getPersistenceState());
            assertEquals(committed.getObjectId(), committedPeer.getObjectId());
            assertSame(peerContext, committedPeer.getDataContext());
            assertSame(context, committed.getDataContext());

            List mods = peerContext.localObjects(Collections.singletonList(modified));
            assertEquals(1, mods.size());
            DataObject modifiedPeer = (DataObject) mods.get(0);
            assertEquals(PersistenceState.HOLLOW, modifiedPeer.getPersistenceState());
            assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
            assertSame(peerContext, modifiedPeer.getDataContext());
            assertSame(context, modified.getDataContext());

            List deletes = peerContext.localObjects(Collections.singletonList(deleted));
            assertEquals(1, deletes.size());
            DataObject deletedPeer = (DataObject) deletes.get(0);
            assertEquals(PersistenceState.HOLLOW, deletedPeer.getPersistenceState());
            assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
            assertSame(peerContext, deletedPeer.getDataContext());
            assertSame(context, deleted.getDataContext());
        }
        finally {
            unblockQueries();
        }
    }

    public void testLocalObjectsPeerContextNoOverride() throws Exception {
        deleteTestData();
        createTestData("testArtists");

        // must create both contexts before running the queries, as each call to
        // 'createDataContext' clears the cache.
        DataContext context = createDataContext();
        DataContext peerContext = createDataContext();

        int modifiedId = 33003;
        Artist modified = (Artist) DataObjectUtils.objectForQuery(
                context,
                new SingleObjectQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        Artist peerModified = (Artist) DataObjectUtils.objectForQuery(
                peerContext,
                new SingleObjectQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));

        modified.setArtistName("M1");
        peerModified.setArtistName("M2");

        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, peerModified.getPersistenceState());

        blockQueries();

        try {

            List mods = peerContext.localObjects(Collections.singletonList(modified));
            assertEquals(1, mods.size());
            DataObject peerModified2 = (DataObject) mods.get(0);
            assertSame(peerModified, peerModified2);
            assertEquals(PersistenceState.MODIFIED, peerModified2.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

}
