/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.access;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.SqlModifyQuery;
import org.objectstyle.cayenne.unittest.MultiContextTestCase;
import org.objectstyle.cayenne.unittest.ThreadedTestHelper;

/**
 * Test suite for testing behavior of multiple DataContexts that share the same 
 * underlying DataDomain.
 * 
 * @author Andrei Adamchik
 */
public class DataContextSharedCacheTst extends MultiContextTestCase {
    private static Logger logObj = Logger.getLogger(DataContextSharedCacheTst.class);

    protected Artist artist;

    protected void setUp() throws Exception {
        super.setUp();

        DataContext context = createDataContextWithSharedCache();

        // prepare a single artist record
        artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("version1");
        artist.setDateOfBirth(new Date());
        context.commitChanges();
    }

    /**
      * Test case to prove that refreshing snapshots as a result of the database fetch
      * will be propagated accross DataContexts. 
      * 
      * @throws Exception
      */
    public void testSnapshotChangePropagationOnSelect() throws Exception {
        String originalName = artist.getArtistName();
        final String newName = "version2";

        DataContext context = artist.getDataContext();

        // create alternative context making sure that no cache is flushed
        DataContext altContext = context.getParentDataDomain().createDataContext(true);

        // update artist using raw SQL
        SqlModifyQuery update =
            new SqlModifyQuery(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = '"
                    + newName
                    + "' WHERE ARTIST_NAME = '"
                    + originalName
                    + "'");
        context.performQueries(
            Collections.singletonList(update),
            new DefaultOperationObserver());

        // fetch updated artist into the new context, and see if the original one gets updated
        Expression qual = ExpressionFactory.matchExp("artistName", newName);
        List artists = altContext.performQuery(new SelectQuery(Artist.class, qual));
        assertEquals(1, artists.size());
        Artist altArtist = (Artist) artists.get(0);

        // check underlying cache
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));

        // check both artists
        assertEquals(newName, altArtist.getArtistName());

        ThreadedTestHelper helper = new ThreadedTestHelper() {
            protected void assertResult() throws Exception {
                assertEquals(
                    "Peer object state wasn't refreshed on fetch",
                    newName,
                    artist.getArtistName());
            }
        };
        helper.assertWithTimeout(3000);
    }

    /**
     * Test case to prove that changes made to an object in one ObjectStore
     * and committed to the database will be reflected in the peer ObjectStore
     * using the same DataRowCache.
     * 
     * @throws Exception
     */
    public void testSnapshotChangePropagation() throws Exception {
        String originalName = artist.getArtistName();
        String newName = "version2";

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        // make sure we have a fully resolved copy of an artist object 
        // in the second context
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(originalName, altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update Artist
        artist.setArtistName(newName);

        // no changes propagated till commit...
        assertEquals(originalName, altArtist.getArtistName());
        context.commitChanges();

        // check underlying cache
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId());
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));

        // check peer artist
        assertEquals(newName, altArtist.getArtistName());
    }

    /**
     * Test case to prove that changes made to an object in one ObjectStore
     * and committed to the database will be correctly merged in the peer ObjectStore
     * using the same DataRowCache. E.g. modified objects will be merged so that no
     * new changes are lost.
     * 
     * @throws Exception
     */
    public void testSnapshotChangePropagationToModifiedObjects() throws Exception {
        String originalName = artist.getArtistName();
        Date originalDate = artist.getDateOfBirth();
        String newName = "version2";
        Date newDate = new Date(originalDate.getTime() - 10000);
        String newAltName = "version3";

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        // make sure we have a fully resolved copy of an artist object 
        // in the second context
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(originalName, altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update Artist peers independently
        artist.setArtistName(newName);
        artist.setDateOfBirth(newDate);
        altArtist.setArtistName(newAltName);

        context.commitChanges();

        // check underlying cache
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId());
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
        assertEquals(newDate, freshSnapshot.get("DATE_OF_BIRTH"));

        // check peer artist
        assertEquals(newAltName, altArtist.getArtistName());
        assertEquals(newDate, altArtist.getDateOfBirth());
        assertEquals(PersistenceState.MODIFIED, altArtist.getPersistenceState());
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore
     * and committed to the database will be reflected in the peer ObjectStore
     * using the same DataRowCache. By default COMMITTED objects will be changed 
     * to TRANSIENT.
     * 
     * @throws Exception
     */
    public void testSnapshotDeletePropagationToCommitted() throws Exception {

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        // make sure we have a fully resolved copy of an artist object 
        // in the second context
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update Artist
        context.deleteObject(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId()));

        // check peer artist
        assertEquals(PersistenceState.TRANSIENT, altArtist.getPersistenceState());
        assertNull(altArtist.getDataContext());
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore
     * and committed to the database will be reflected in the peer ObjectStore
     * using the same DataRowCache. By default HOLLOW objects will be changed 
     * to TRANSIENT.
     * 
     * @throws Exception
     */
    public void testSnapshotDeletePropagationToHollow() throws Exception {

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        // make sure we have a fully resolved copy of an artist object 
        // in the second context
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(PersistenceState.HOLLOW, altArtist.getPersistenceState());

        // Update Artist
        context.deleteObject(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId()));

        // check peer artist
        assertEquals(PersistenceState.TRANSIENT, altArtist.getPersistenceState());
        assertNull(altArtist.getDataContext());
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore
     * and committed to the database will be reflected in the peer ObjectStore
     * using the same DataRowCache. By default MODIFIED objects will be changed 
     * to NEW.
     * 
     * @throws Exception
     */
    public void testSnapshotDeletePropagationToModified() throws Exception {

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        // make sure we have a fully resolved copy of an artist object 
        // in the second context
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);

        // modify peer
        altArtist.setArtistName("version2");
        assertEquals(PersistenceState.MODIFIED, altArtist.getPersistenceState());

        // Update Artist
        context.deleteObject(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId()));

        // check peer artist
        assertEquals(PersistenceState.NEW, altArtist.getPersistenceState());

        // check if now we can save this object again, and with the original ObjectId
        ObjectId id = altArtist.getObjectId();
        assertNotNull(id);
        assertNotNull(id.getValueForAttribute(Artist.ARTIST_ID_PK_COLUMN));
        assertFalse(id.isTemporary());

        altContext.commitChanges();

        // create independent context and fetch artist in it
        DataContext context3 = getDomain().createDataContext(false);
        List artists = context3.performQuery(QueryUtils.selectObjectForId(id));
        assertEquals(1, artists.size());
        Artist artist3 = (Artist) artists.get(0);
        assertEquals(id, artist3.getObjectId());
    }

    /**
     * Test case to prove that deleting an object in one ObjectStore
     * and committed to the database will be reflected in the peer ObjectStore
     * using the same DataRowCache. By default DELETED objects will be changed 
     * to TRANSIENT.
     * 
     * @throws Exception
     */
    public void testSnapshotDeletePropagationToDeleted() throws Exception {

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        // make sure we have a fully resolved copy of an artist object 
        // in the second context
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);

        // delete peer
        altContext.deleteObject(altArtist);

        // Update Artist
        context.deleteObject(artist);
        context.commitChanges();

        // check underlying cache
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId()));

        // check peer artist
        assertEquals(PersistenceState.TRANSIENT, altArtist.getPersistenceState());
        assertNull(altArtist.getDataContext());
        assertFalse(altContext.hasChanges());
    }

    /**
     * Checks that cache is not refreshed when a query "refreshingObjects"
     * property is set to false.
     * 
     * @throws Exception
     */
    public void testCacheNonRefreshingOnSelect() throws Exception {
        String originalName = artist.getArtistName();
        final String newName = "version2";

        DataContext context = artist.getDataContext();

        DataRow oldSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotNull(oldSnapshot);
        assertEquals(originalName, oldSnapshot.get("ARTIST_NAME"));

        // update artist using raw SQL
        SqlModifyQuery update =
            new SqlModifyQuery(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = '"
                    + newName
                    + "' WHERE ARTIST_NAME = '"
                    + originalName
                    + "'");
        context.performQueries(
            Collections.singletonList(update),
            new DefaultOperationObserver());

        // fetch updated artist without refreshing
        Expression qual = ExpressionFactory.matchExp("artistName", newName);
        SelectQuery query = new SelectQuery(Artist.class, qual);
        query.setRefreshingObjects(false);
        List artists = context.performQuery(query);
        assertEquals(1, artists.size());
        artist = (Artist) artists.get(0);

        // check underlying cache
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertSame(oldSnapshot, freshSnapshot);

        // check an artist
        assertEquals(originalName, artist.getArtistName());
    }

    /**
     * Checks that cache is refreshed when a query "refreshingObjects"
     * property is set to true.
     * 
     * @throws Exception
     */
    public void testCacheRefreshingOnSelect() throws Exception {
        String originalName = artist.getArtistName();
        final String newName = "version2";

        DataContext context = artist.getDataContext();

        DataRow oldSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotNull(oldSnapshot);
        assertEquals(originalName, oldSnapshot.get("ARTIST_NAME"));

        // update artist using raw SQL
        SqlModifyQuery update =
            new SqlModifyQuery(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = '"
                    + newName
                    + "' WHERE ARTIST_NAME = '"
                    + originalName
                    + "'");
        context.performQueries(
            Collections.singletonList(update),
            new DefaultOperationObserver());

        // fetch updated artist without refreshing
        Expression qual = ExpressionFactory.matchExp("artistName", newName);
        SelectQuery query = new SelectQuery(Artist.class, qual);
        query.setRefreshingObjects(true);
        List artists = context.performQuery(query);
        assertEquals(1, artists.size());
        artist = (Artist) artists.get(0);

        // check underlying cache
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotSame(oldSnapshot, freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));

        // check an artist
        assertEquals(newName, artist.getArtistName());
    }

    public void testSnapshotEvictedForHollow() throws Exception {
        String originalName = artist.getArtistName();
        DataContext context = artist.getDataContext();

        context.invalidateObjects(Collections.singletonList(artist));
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId()));

        // resolve object
        assertEquals(originalName, artist.getArtistName());
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(originalName, freshSnapshot.get("ARTIST_NAME"));
    }

    public void testSnapshotEvictedForCommitted() throws Exception {
        String newName = "version2";
        DataContext context = artist.getDataContext();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        context.getObjectStore().getDataRowCache().forgetSnapshot(artist.getObjectId());
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId()));

        // modify object and try to save
        artist.setArtistName(newName);
        context.commitChanges();

        assertEquals(newName, artist.getArtistName());
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
    }

    public void testSnapshotEvictedForModified() throws Exception {
        String newName = "version2";
        DataContext context = artist.getDataContext();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        // modify object PRIOR to killing the snapshot 
        artist.setArtistName(newName);

        context.getObjectStore().getDataRowCache().forgetSnapshot(artist.getObjectId());
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId()));

        context.commitChanges();

        assertEquals(newName, artist.getArtistName());
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
    }

    public void testSnapshotEvictedAndChangedForModified() throws Exception {
        String originalName = artist.getArtistName();
        String newName = "version2";
        String backendName = "version3";
        DataContext context = artist.getDataContext();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        // modify object PRIOR to killing the snapshot 
        artist.setArtistName(newName);

        context.getObjectStore().getDataRowCache().forgetSnapshot(artist.getObjectId());
        assertNull(
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId()));

        // now replace the row in the database
        SqlModifyQuery update =
            new SqlModifyQuery(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = '"
                    + backendName
                    + "' WHERE ARTIST_NAME = '"
                    + originalName
                    + "'");
        context.performQueries(
            Collections.singletonList(update),
            new DefaultOperationObserver());

        context.commitChanges();

        assertEquals(newName, artist.getArtistName());
        DataRow freshSnapshot =
            context.getObjectStore().getDataRowCache().getCachedSnapshot(
                artist.getObjectId());
        assertNotNull(freshSnapshot);
        assertEquals(newName, freshSnapshot.get("ARTIST_NAME"));
    }

    public void testSnapshotEvictedForDeleted() throws Exception {
        // remember ObjectId
        ObjectId id = artist.getObjectId();

        DataContext context = artist.getDataContext();

        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        // delete object PRIOR to killing the snapshot 
        context.deleteObject(artist);

        context.getObjectStore().getDataRowCache().forgetSnapshot(id);
        assertNull(context.getObjectStore().getDataRowCache().getCachedSnapshot(id));

        context.commitChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        assertNull(context.getObjectStore().getDataRowCache().getCachedSnapshot(id));
    }
}
