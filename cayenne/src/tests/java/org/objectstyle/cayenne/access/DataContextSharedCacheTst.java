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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.unittest.MultiContextTestCase;

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
        Artist artist3 = (Artist)artists.get(0);
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
}
