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

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;

/**
 * Test suite covering possible scenarios of refreshing updated 
 * objects. This includes refreshing relationships and attributes
 * changed outside of Cayenne with and witout prefetching.
 * 
 * @author Andrei Adamchik
 */
public class DataContextRefreshingTst extends DataContextTestBase {

    public void testRefetchRootWithUpdatedAttributes() throws Exception {
        String nameBefore = "artist2";
        String nameAfter = "not an artist";

        Artist artist = fetchArtist(nameBefore, false);
        assertNotNull(artist);
        assertEquals(nameBefore, artist.getArtistName());

        // update via DataNode directly
        updateRow(artist.getObjectId(), "ARTIST_NAME", nameAfter);

        // fetch into the same context
        artist = fetchArtist(nameBefore, false);
        assertNull(artist);

        artist = fetchArtist(nameAfter, false);
        assertNotNull(artist);
        assertEquals(nameAfter, artist.getArtistName());
    }

    public void testRefetchRootWithNullifiedToOne() throws Exception {
        Painting painting = insertPaintingInContext("p");
        assertNotNull(painting.getToArtist());

        // update via DataNode directly
        updateRow(painting.getObjectId(), "ARTIST_ID", null);

        // select without prefetch
        painting = fetchPainting(painting.getPaintingTitle(), false);
        assertNotNull(painting);
        assertNull(painting.getToArtist());
    }

    public void testRefetchRootWithChangedToOneTarget() throws Exception {
        Painting painting = insertPaintingInContext("p");
        Artist artistBefore = painting.getToArtist();
        assertNotNull(artistBefore);

        Artist artistAfter = fetchArtist("artist3", false);
        assertNotNull(artistAfter);
        assertNotSame(artistBefore, artistAfter);

        // update via DataNode directly
        updateRow(
            painting.getObjectId(),
            "ARTIST_ID",
            artistAfter.getObjectId().getValueForAttribute("ARTIST_ID"));

        // select without prefetch
        painting = fetchPainting(painting.getPaintingTitle(), false);
        assertNotNull(painting);
        assertSame(artistAfter, painting.getToArtist());
    }

    public void testRefetchRootWithNullToOneTargetChangedToNotNull() throws Exception {
        Painting painting = insertPaintingInContext("p");
        painting.setToArtist(null);
        context.commitChanges();

        assertNull(painting.getToArtist());

        Artist artistAfter = fetchArtist("artist3", false);
        assertNotNull(artistAfter);

        // update via DataNode directly
        updateRow(
            painting.getObjectId(),
            "ARTIST_ID",
            artistAfter.getObjectId().getValueForAttribute("ARTIST_ID"));

        // select without prefetch
        painting = fetchPainting(painting.getPaintingTitle(), false);
        assertNotNull(painting);
        assertSame(artistAfter, painting.getToArtist());
    }

    public void testRefetchRootWithDeletedToMany() throws Exception {
        Painting painting = insertPaintingInContext("p");
        Artist artist = painting.getToArtist();
        assertEquals(artist.getPaintingArray().size(), 1);

        deleteRow(painting.getObjectId());

        // select without prefetch
        artist = fetchArtist(artist.getArtistName(), false);
        assertEquals(artist.getPaintingArray().size(), 1);

        // select using relationship prefetching
        artist = fetchArtist(artist.getArtistName(), true);
        assertEquals(artist.getPaintingArray().size(), 0);
    }

    public void testRefetchRootWithAddedToMany() throws Exception {
        Artist artist = fetchArtist("artist2", false);
        assertEquals(artist.getPaintingArray().size(), 0);

        insertPaintingBypassingContext("p", artist.getArtistName());

        // select without prefetch
        artist = fetchArtist(artist.getArtistName(), false);
        assertEquals(artist.getPaintingArray().size(), 0);

        // select using relationship prefetching
        artist = fetchArtist(artist.getArtistName(), true);
        assertEquals(artist.getPaintingArray().size(), 1);
    }

    public void testInvalidateRootWithUpdatedAttributes() throws Exception {
        String nameBefore = "artist2";
        String nameAfter = "not an artist";

        Artist artist = fetchArtist(nameBefore, false);
        assertNotNull(artist);
        assertEquals(nameBefore, artist.getArtistName());

        // update via DataNode directly
        updateRow(artist.getObjectId(), "ARTIST_NAME", nameAfter);

        context.invalidateObject(artist);
        assertEquals(nameAfter, artist.getArtistName());
    }

    public void testInvalidateRootWithNullifiedToOne() throws Exception {
        Painting painting = insertPaintingInContext("p");
        assertNotNull(painting.getToArtist());

        // update via DataNode directly
        updateRow(painting.getObjectId(), "ARTIST_ID", null);

        context.invalidateObject(painting);
        assertNull(painting.getToArtist());
    }

    public void testInvalidateRootWithChangedToOneTarget() throws Exception {
        Painting painting = insertPaintingInContext("p");
        Artist artistBefore = painting.getToArtist();
        assertNotNull(artistBefore);

        Artist artistAfter = fetchArtist("artist3", false);
        assertNotNull(artistAfter);
        assertNotSame(artistBefore, artistAfter);

        // update via DataNode directly
        updateRow(
            painting.getObjectId(),
            "ARTIST_ID",
            artistAfter.getObjectId().getValueForAttribute("ARTIST_ID"));

        context.invalidateObject(painting);
        assertSame(artistAfter, painting.getToArtist());
    }

    public void testInvalidateRootWithNullToOneTargetChangedToNotNull()
        throws Exception {
        Painting painting = insertPaintingInContext("p");
        painting.setToArtist(null);
        context.commitChanges();

        assertNull(painting.getToArtist());

        Artist artistAfter = fetchArtist("artist3", false);
        assertNotNull(artistAfter);

        // update via DataNode directly
        updateRow(
            painting.getObjectId(),
            "ARTIST_ID",
            artistAfter.getObjectId().getValueForAttribute("ARTIST_ID"));

        context.invalidateObject(painting);
        assertSame(artistAfter, painting.getToArtist());
    }

    public void testInvalidateRootWithDeletedToMany() throws Exception {
        Painting painting = insertPaintingInContext("p");
        Artist artist = painting.getToArtist();
        assertEquals(artist.getPaintingArray().size(), 1);

        deleteRow(painting.getObjectId());

        context.invalidateObject(artist);
        assertEquals(artist.getPaintingArray().size(), 0);
    }

    public void testInvaliateRootWithAddedToMany() throws Exception {
        Artist artist = fetchArtist("artist2", false);
        assertEquals(artist.getPaintingArray().size(), 0);

        insertPaintingBypassingContext("p", artist.getArtistName());
		assertEquals(artist.getPaintingArray().size(), 0);
        context.invalidateObject(artist);
        assertEquals(artist.getPaintingArray().size(), 1);
    }

    public void testRefetchRootWithAddedToManyViaRefetchObject() throws Exception {
        Artist artist = fetchArtist("artist2", false);
        assertEquals(artist.getPaintingArray().size(), 0);

        insertPaintingBypassingContext("p", artist.getArtistName());

		assertEquals(artist.getPaintingArray().size(), 0);
		artist = (Artist)context.refetchObject(artist.getObjectId());
        assertEquals(artist.getPaintingArray().size(), 1);
    }
}
