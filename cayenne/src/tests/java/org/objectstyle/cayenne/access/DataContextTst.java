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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistAssets;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.ROArtist;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class DataContextTst extends DataContextTestBase {
    

    public void testCreatePermId1() throws Exception {
        Artist artist = new Artist();
        context.registerNewObject(artist);
        ObjectId id = context.createPermId(artist);
        assertNotNull(id);
    }

    public void testCreatePermId2() throws Exception {
        Artist artist = new Artist();
        context.registerNewObject(artist, "Artist");
        ObjectId id1 = context.createPermId(artist);
        ObjectId id2 = context.createPermId(artist);
        //Must not fail on second call

        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(id1, id2); //Must be the same,
    }

    public void testTakeObjectsSnapshot1() throws Exception {
        Artist artist = fetchArtist("artist1", false);
        Map snapshot = context.takeObjectSnapshot(artist);
        assertEquals(artist.getArtistName(), snapshot.get("ARTIST_NAME"));
        assertEquals(artist.getDateOfBirth(), snapshot.get("DATE_OF_BIRTH"));
    }

    public void testTakeObjectsSnapshot2() throws Exception {
        // test  null values
        Artist artist = fetchArtist("artist1", false);
        artist.setArtistName(null);
        artist.setDateOfBirth(null);

        Map snapshot = context.takeObjectSnapshot(artist);
        assertTrue(snapshot.containsKey("ARTIST_NAME"));
        assertNull(snapshot.get("ARTIST_NAME"));

        assertTrue(snapshot.containsKey("DATE_OF_BIRTH"));
        assertNull(snapshot.get("DATE_OF_BIRTH"));
    }

    public void testTakeObjectsSnapshot3() throws Exception {
        // test FK relationship snapshotting
        Artist a1 = fetchArtist("artist1", false);

        Painting p1 = new Painting();
        context.registerNewObject(p1);
        p1.setToArtist(a1);

        Map s1 = context.takeObjectSnapshot(p1);
        Map idMap = a1.getObjectId().getIdSnapshot();
        assertEquals(idMap.get("ARTIST_ID"), s1.get("ARTIST_ID"));
    }

    public void testLookupEntity() throws Exception {
        assertNotNull(context.getEntityResolver().lookupObjEntity(Artist.class));
        assertNull(context.getEntityResolver().lookupObjEntity("NonExistent"));
    }

    /**
     * Tests how CHAR field is handled during fetch.
     * Some databases (Oracle...) would pad a CHAR column
     * with extra spaces, returned to the client. Cayenne
     * should trim it.
     */
    public void testCharFetch() throws Exception {
        SelectQuery q = new SelectQuery("Artist");
        List artists = context.performQuery(q);
        Artist a = (Artist) artists.get(0);
        assertEquals(a.getArtistName().trim(), a.getArtistName());
    }

    /**
     * Tests how CHAR field is handled during fetch in the WHERE clause.
     * Some databases (Oracle...) would pad a CHAR column
     * with extra spaces, returned to the client. Cayenne
     * should trim it.
     */
    public void testCharInQualifier() throws Exception {
        Expression e =
            ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", "artist1");
        SelectQuery q = new SelectQuery("Artist", e);
        List artists = context.performQuery(q);
        assertEquals(1, artists.size());
    }

 
    /**
     * Test fetching query with multiple relationship
     * paths between the same 2 entities used in qualifier.
     */
    public void testMultiObjRelFetch() throws Exception {
        populatePaintings();

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(ExpressionFactory.matchExp("toArtist.artistName", artistName(2)));
        q.orQualifier(ExpressionFactory.matchExp("toArtist.artistName", artistName(4)));
        List results = context.performQuery(q);

        assertEquals(2, results.size());
    }

    /**
     * Test fetching query with multiple relationship
     * paths between the same 2 entities used in qualifier.
     */
    public void testMultiDbRelFetch() throws Exception {
        populatePaintings();

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(
            ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", artistName(2)));
        q.orQualifier(
            ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", artistName(4)));
        List results = context.performQuery(q);

        assertEquals(2, results.size());
    }

    /**
     * Test fetching a derived entity.
     */
    public void testDerivedEntityFetch1() throws Exception {
        // Skip HSQLDB, since it currently does not support HAVING;
        // this is supposed to appear in the next release.
        if (((DataNode) getDomain().getDataNodes().iterator().next())
            .getAdapter()
            .getClass()
            == HSQLDBAdapter.class) {
            return;
        }

        populatePaintings();

        SelectQuery q = new SelectQuery("ArtistAssets");
        q.setQualifier(
            ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(1000)));
        q.setLoggingLevel(Level.INFO);

        ArtistAssets a1 = (ArtistAssets) context.performQuery(q).get(0);
        assertEquals(1, a1.getPaintingsCount().intValue());
    }

    /**
     * Test fetching a derived entity with complex qualifier including relationships.
     */
    public void testDerivedEntityFetch2() throws Exception {
        // Skip HSQLDB, since it currently does not support HAVING;
        // this is supposed to appear in the next release.
        if (((DataNode) getDomain().getDataNodes().iterator().next())
            .getAdapter()
            .getClass()
            == HSQLDBAdapter.class) {
            return;
        }

        populatePaintings();

        SelectQuery q = new SelectQuery("ArtistAssets");
        q.setParentObjEntityName("Painting");
        q.andQualifier(
            ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(1000)));
        q.andParentQualifier(
            ExpressionFactory.matchExp("toArtist.artistName", artistName(1)));
        q.setLoggingLevel(Level.INFO);

        ArtistAssets a1 = (ArtistAssets) context.performQuery(q).get(0);
        assertEquals(1, a1.getPaintingsCount().intValue());
    }

    public void testPerformQueries() throws Exception {
        SelectQuery q1 = new SelectQuery();
        q1.setRoot(Artist.class);
        SelectQuery q2 = new SelectQuery();
        q2.setRoot(Gallery.class);

        List qs = new ArrayList();
        qs.add(q1);
        qs.add(q2);
        context.performQueries(qs, opObserver);

        // check query results
        List o1 = opObserver.objectsForQuery(q1);
        assertNotNull(o1);
        assertEquals(artistCount, o1.size());

        List o2 = opObserver.objectsForQuery(q2);
        assertNotNull(o2);
        assertEquals(galleryCount, o2.size());
    }

    public void testSelectDate() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(artistCount, objects.size());

        Artist a1 = (Artist) objects.get(0);
        assertEquals(java.util.Date.class, a1.getDateOfBirth().getClass());
    }

    public void testPerformSelectQuery1() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(artistCount, objects.size());
        assertTrue(
            "Artist expected, got " + objects.get(0).getClass(),
            objects.get(0) instanceof Artist);
    }

    public void testPerformSelectQuery2() throws Exception {
        // do a query with complex qualifier
        List expressions = new ArrayList();
        expressions.add(ExpressionFactory.matchExp("artistName", "artist3"));
        expressions.add(ExpressionFactory.matchExp("artistName", "artist5"));
        expressions.add(ExpressionFactory.matchExp("artistName", "artist15"));

        SelectQuery query =
            new SelectQuery(
                "Artist",
                ExpressionFactory.joinExp(Expression.OR, expressions));
        query.setLoggingLevel(Level.ERROR);
        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(
            "Artist expected, got " + objects.get(0).getClass(),
            objects.get(0) instanceof Artist);
    }

    public void testPerformQuery() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        context.performQuery(query, opObserver);
        List objects = opObserver.objectsForQuery(query);

        assertNotNull(objects);
        assertEquals(artistCount, objects.size());
    }

    public void testPerformPagedQuery() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        query.setPageSize(5);
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList);

        assertTrue(((IncrementalFaultList) objects).elements.get(0) instanceof Artist);
        assertTrue(((IncrementalFaultList) objects).elements.get(7) instanceof Map);
    }

    public void testPerformDataRowQuery() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        query.setFetchingDataRows(true);
        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(artistCount, objects.size());
        assertTrue(
            "Map expected, got " + objects.get(0).getClass(),
            objects.get(0) instanceof Map);
    }

    public void testCommitChangesRO1() throws Exception {
        ROArtist a1 = (ROArtist) context.createAndRegisterNewObject("ROArtist");
        a1.setArtistName("abc");

        try {
            context.commitChanges();
            fail("Inserting a 'read-only' object must fail.");
        }
        catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO2() throws Exception {
        ROArtist a1 = fetchROArtist("artist1");
        a1.setArtistName("abc");

        try {
            context.commitChanges();
            fail("Updating a 'read-only' object must fail.");
        }
        catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO3() throws Exception {
        ROArtist a1 = fetchROArtist("artist1");
        context.deleteObject(a1);

        try {
            context.commitChanges();
            fail("Deleting a 'read-only' object must fail.");
        }
        catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }


    public void testPerformIteratedQuery1() throws Exception {
        SelectQuery q1 = new SelectQuery("Artist");
        ResultIterator it = context.performIteratedQuery(q1);

        try {
            int count = 0;
            while (it.hasNextRow()) {
                it.nextDataRow();
                count++;
            }

            assertEquals(DataContextTst.artistCount, count);
        }
        finally {
            it.close();
        }
    }

    public void testPerformIteratedQuery2() throws Exception {
        populatePaintings();

        SelectQuery q1 = new SelectQuery("Artist");
        ResultIterator it = context.performIteratedQuery(q1);

        // just for this test increase pool size
        changeMaxConnections(1);

        try {
            while (it.hasNextRow()) {
                Map row = it.nextDataRow();

                // try instantiating an object and fetching its relationships
                Artist obj = (Artist) context.objectFromDataRow("Artist", row);
                List paintings = obj.getPaintingArray();
                assertNotNull(paintings);
                assertEquals(1, paintings.size());
            }
        }
        finally {
            // change allowed connections back
            changeMaxConnections(-1);

            it.close();
        }
    }

    public void changeMaxConnections(int delta) {
        DataNode node =
            (DataNode) ((DataDomain) context.getParent()).getDataNodes().iterator().next();
        PoolManager manager = (PoolManager) node.getDataSource();
        manager.setMaxConnections(manager.getMaxConnections() + delta);
    }

    public void testRollbackNewObject() {
        String artistName = "revertTestArtist";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);

        context.rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();
        //The commit should have made no changes, so
        //perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = getDomain().createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "artistName",
                artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(0, queryResults.size());
    }

    //Catches a bug where new objects were unregistered within an object iterator, thus modifying the
    // collection the iterator was iterating over (ConcurrentModificationException)
    public void testRollbackWithMultipleNewObjects() {
        String artistName = "rollbackTestArtist";
        String paintingTitle = "rollbackTestPainting";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);

        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);

        try {
            context.rollbackChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(
                "rollbackChanges should not have caused the exception " + e.getMessage());
        }

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();
        //The commit should have made no changes, so
        //perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = getDomain().createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "artistName",
                artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(0, queryResults.size());
    }

    public void testRollbackDeletedObject() {
        String artistName = "deleteTestArtist";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();
        //Save... cayenne doesn't yet handle deleting objects that are uncommitted
        context.deleteObject(artist);
        context.rollbackChanges();

        //Now check everything is as it should be
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

        context.commitChanges();
        //The commit should have made no changes, so
        //perform a fetch to ensure that this artist hasn't been deleted from the db

        DataContext freshContext = getDomain().createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "artistName",
                artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
    }

    public void testRollbackModifiedObject() {
        String artistName = "initialTestArtist";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();

        artist.setArtistName("a new value");

        context.rollbackChanges();

        //Make sure the inmemory changes have been rolled back
        assertEquals(artistName, artist.getArtistName());

        //Commit what's in memory...
        context.commitChanges();

        //.. and ensure that the correct data is in the db
        DataContext freshContext = getDomain().createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "artistName",
                artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());

    }

    public void testRollbackRelationshipModification() {
        String artistName = "relationshipModArtist";
        String paintingTitle = "relationshipTestPainting";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);
        context.commitChanges();

        painting.setToArtist(null);
        context.rollbackChanges();

        assertEquals(artist, painting.getToArtist());

        //Check that the reverse relationship was handled
        assertEquals(1, artist.getPaintingArray().size());
        context.commitChanges();

        DataContext freshContext = getDomain().createDataContext();
        SelectQuery query = new SelectQuery(Painting.class);
        query.setQualifier(
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "paintingTitle",
                paintingTitle));
        List queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
        Painting queriedPainting = (Painting) queryResults.get(0);

        //NB:  This is an easier comparison than manually fetching artist
        assertEquals(artistName, queriedPainting.getToArtist().getArtistName());
    }

    /**
     * Tests that hasChanges performs correctly when an object is "modified" 
     * and the property is simply set to the same value (an unreal modification) 
     */
    public void testHasChangesUnrealModify() {
        String artistName = "ArtistName";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();

        artist.setArtistName(artistName); //Set again to *exactly* the same value
        assertFalse(context.hasChanges());
    }

    /**
     * Tests that hasChanges performs correctly when an object is "modified" 
     * and the property is simply set to the same value (an unreal modification) 
     */
    public void testHasChangesRealModify() {
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("ArtistName");
        context.commitChanges();

        artist.setArtistName("Something different");
        assertTrue(context.hasChanges());
    }

}