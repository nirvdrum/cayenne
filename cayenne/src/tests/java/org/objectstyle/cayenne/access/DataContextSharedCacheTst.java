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
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.unittest.MultiContextTestCase;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DataContextSharedCacheTst extends MultiContextTestCase {
    private static Logger logObj = Logger.getLogger(DataContextSharedCacheTst.class);

    protected Artist artist;

    protected void setUp() throws Exception {
        super.setUp();

        // prepare a single artist record
        artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("version1");
        context.commitChanges();
    }

    public void testUpdatePropagationViaEvents() throws Exception {
        // turn on the events
        getDomain().getSnapshotCache().setNotifyingObjectStores(true);

        try {
            // prepare a second context
			logObj.warn("initial domain BEFORE: " + Configuration.getSharedConfiguration().getDomain("domain"));
			logObj.warn("expected domain BEFORE: " + getDomain());
            DataContext altContext = mirrorDataContext(context);
            Artist altArtist =
                (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
            assertNotNull(altArtist);

		
            logObj.warn("initial domain: " + context.getParent());
            logObj.warn("Cloned domain: " + altContext.getParent());
			logObj.warn("expected domain: " + getDomain());
            assertFalse(altArtist == artist);
            assertEquals(artist.getArtistName(), altArtist.getArtistName());

            // test update propagation
            artist.setArtistName("version2");
            context.commitChanges();

            assertEquals(artist.getArtistName(), altArtist.getArtistName());
        }
        finally {
            // reset shared settings to default
            getDomain().getSnapshotCache().setNotifyingObjectStores(false);
        }
    }

    public void testUpdatePropagationViaExplicitSync() throws Exception {

        // prepare a second context
        DataContext altContext = mirrorDataContext(context);
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);

        assertEquals(artist.getArtistName(), altArtist.getArtistName());

        // test update propagation
        artist.setArtistName("version2");
        context.commitChanges();

        // before sync
        assertFalse(
            Util.nullSafeEquals(artist.getArtistName(), altArtist.getArtistName()));

        altContext.getObjectStore().synchronizeWithCache();

        // after sync
        // TODO: uncomment this test when the feature is implemented
        // assertEquals(artist.getArtistName(), altArtist.getArtistName());
    }

    public void testCommitUpdateWithExternallyUpdatedSnapshot1() throws Exception {
        // prepare a second context
        DataContext altContext = mirrorDataContext(context);
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());

        // update
        artist.setArtistName("version2");
        context.commitChanges();

        // test behavior on commit when snapshot has changed underneath
        // (case when the same property has changed twice);
        altArtist.setArtistName("version3");
        altContext.commitChanges();
        Map snapshot =
            altContext.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId());
        assertEquals("version3", snapshot.get("ARTIST_NAME"));
    }

    public void testCommitUpdateWithExternallyUpdatedSnapshot2() throws Exception {
        // prepare a second context
        DataContext altContext = mirrorDataContext(context);
        Artist altArtist =
            (Artist) altContext.getObjectStore().getObject(artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());

        // update
        Date dob = new Date();
        artist.setDateOfBirth(dob);
        context.commitChanges();

        // test behavior on commit when snapshot has changed underneath
        // (case when a different property has changed);
        altArtist.setArtistName("version3");
        altContext.commitChanges();
        Map snapshot =
            altContext.getObjectStore().getDataRowCache().getCachedSnapshot(
                altArtist.getObjectId());
        assertEquals("version3", snapshot.get("ARTIST_NAME"));

        // TODO: uncomment this once snapshot timestamping is implemented
        // assertEquals(dob, snapshot.get("DATE_OF_BIRTH"));
    }
}
