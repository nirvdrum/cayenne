/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistAssets;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TestOperationObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;

public class DataContextTst extends CayenneTestCase {
	static Logger logObj = Logger.getLogger(DataContextTst.class.getName());

	public static final int artistCount = 25;
	public static final int galleryCount = 10;

	protected DataContext ctxt;
	protected TestOperationObserver opObserver;

	public DataContextTst(String name) {
		super(name);
	}

	protected void setUp() throws java.lang.Exception {
		super.setUp();

        CayenneTestDatabaseSetup setup = getDatabaseSetup();
        setup.cleanTableData();        
		populateTables();

		DataDomain dom = getDomain();
		setup.createPkSupportForMapEntities(dom.getDataNodes()[0]);

		ctxt = dom.createDataContext();
		opObserver = new TestOperationObserver();
	}

	public void testCreatePermId1() throws Exception {
		Artist artist = new Artist();
		ctxt.registerNewObject(artist, "Artist");
		ObjectId id = ctxt.createPermId(artist);
		assertNotNull(id);
	}

	public void testMerge() throws Exception {
		String n1 = "changed";
		String n2 = "changed again";

		Artist a1 = fetchArtist("artist1");
		a1.setArtistName(n1);

		Map s2 = new HashMap();
		s2.put("ARTIST_NAME", n2);
		s2.put("DATE_OF_BIRTH", new java.util.Date());
		ObjEntity e = ctxt.getEntityResolver().lookupObjEntity("Artist");
		ctxt.getSnapshotManager().mergeObjectWithSnapshot(e, a1, s2);

		// name was modified, so it should not change during merge
		assertEquals(n1, a1.getArtistName());

		// date of birth came from database, it should be updated during merge
		assertEquals(s2.get("DATE_OF_BIRTH"), a1.getDateOfBirth());
	}

	public void testTakeObjectsSnapshot1() throws Exception {
		Artist artist = fetchArtist("artist1");
		Map snapshot = ctxt.takeObjectSnapshot(artist);
		assertEquals(artist.getArtistName(), snapshot.get("ARTIST_NAME"));
		assertEquals(artist.getDateOfBirth(), snapshot.get("DATE_OF_BIRTH"));
	}

	public void testTakeObjectsSnapshot2() throws Exception {
		// test  null values
		Artist artist = fetchArtist("artist1");
		artist.setArtistName(null);
		artist.setDateOfBirth(null);

		Map snapshot = ctxt.takeObjectSnapshot(artist);
		assertTrue(snapshot.containsKey("ARTIST_NAME"));
		assertNull(snapshot.get("ARTIST_NAME"));

		assertTrue(snapshot.containsKey("DATE_OF_BIRTH"));
		assertNull(snapshot.get("DATE_OF_BIRTH"));
	}

	public void testTakeObjectsSnapshot3() throws Exception {
		// test FK relationship snapshotting
		Artist a1 = fetchArtist("artist1");

		Painting p1 = new Painting();
		ctxt.registerNewObject(p1, "Painting");
		p1.setToArtist(a1);

		Map s1 = ctxt.takeObjectSnapshot(p1);
		Map idMap = a1.getObjectId().getIdSnapshot();
		assertEquals(idMap.get("ARTIST_ID"), s1.get("ARTIST_ID"));
	}

	public void testLookupEntity() throws Exception {
		assertNotNull(ctxt.getEntityResolver().lookupObjEntity("Artist"));
		assertNull(ctxt.getEntityResolver().lookupObjEntity("NonExistent"));
	}

	/** 
	 * Tests how CHAR field is handled during fetch.
	 * Some databases (Oracle...) would pad a CHAR column
	 * with extra spaces, returned to the client. Cayenne
	 * should trim it.
	 */
	public void testCharFetch() throws Exception {
		SelectQuery q = new SelectQuery("Artist");
		List artists = ctxt.performQuery(q);
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
		List artists = ctxt.performQuery(q);
		assertEquals(1, artists.size());
	}

	/** 
	 * Test that all queries specified in prefetch are executed
	 * with a single prefetch path. 
	 */
	public void testPrefetch1() throws Exception {
		Expression e =
			ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", "a");
		SelectQuery q = new SelectQuery("Artist", e);
		q.addPrefetch("paintingArray");

		SelectObserver o = new SelectObserver();
		ctxt.performQuery(q, o);

		assertEquals(2, o.getSelectCount());
	}

	/** 
	 * Test that all queries specified in prefetch are executed
	 * in a more complex prefetch scenario. 
	 */
	public void testPrefetch2() throws Exception {
		Expression e =
			ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", "a");
		SelectQuery q = new SelectQuery("Artist", e);
		q.addPrefetch("paintingArray");
		q.addPrefetch("paintingArray.toGallery");
		q.addPrefetch("artistExhibitArray.toExhibit");

		SelectObserver o = new SelectObserver();
		ctxt.performQuery(q, o);

		assertEquals(4, o.getSelectCount());
	}

	/** 
	 * Test that a to-many relationship is initialized.
	 */
	public void testPrefetch3() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Artist");
		q.addPrefetch("paintingArray");

		CayenneDataObject a1 = (CayenneDataObject) ctxt.performQuery(q).get(0);
		ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
		// assertTrue(!toMany.needsFetch());
	}

	/** 
	 * Test that a to-one relationship is initialized.
	 */
	public void testPrefetch4() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Painting");
		q.addPrefetch("toArtist");

		CayenneDataObject p1 = (CayenneDataObject) ctxt.performQuery(q).get(0);
		CayenneDataObject a1 = (CayenneDataObject) p1.readPropertyDirectly("toArtist");

		assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
	}

	/** 
	 * Test prefetching with queries using DB_PATH.
	 */
	public void testPrefetch5() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Painting");
		q.andQualifier(
			ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", artistName(2)));
		q.addPrefetch("toArtist");
		// q.setLoggingLevel(Level.INFO);

		List results = ctxt.performQuery(q);
		assertEquals(1, results.size());
	}

	/** 
	 * Test prefetching with queries using OBJ_PATH.
	 */
	public void testPrefetch6() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Painting");
		q.andQualifier(ExpressionFactory.matchExp("toArtist.artistName", artistName(2)));
		q.addPrefetch("toArtist");
		// q.setLoggingLevel(Level.INFO);

		List results = ctxt.performQuery(q);
		assertEquals(1, results.size());
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
		List results = ctxt.performQuery(q);

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
		List results = ctxt.performQuery(q);

		assertEquals(2, results.size());
	}

	/** 
	 * Test fetching a derived entity.
	 */
	public void testDerivedEntityFetch1() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("ArtistAssets");
		q.setQualifier(
			ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(1000)));
		q.setLoggingLevel(Level.INFO);

		ArtistAssets a1 = (ArtistAssets) ctxt.performQuery(q).get(0);
		assertEquals(1, a1.getPaintingsCount().intValue());
	}

	/** 
	 * Test fetching a derived entity with complex qualifier including relationships.
	 */
	public void testDerivedEntityFetch2() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("ArtistAssets");
		q.setParentObjEntityName("Painting");
		q.andQualifier(
			ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(1000)));
		q.andParentQualifier(
			ExpressionFactory.matchExp("toArtist.artistName", artistName(1)));
		q.setLoggingLevel(Level.INFO);

		ArtistAssets a1 = (ArtistAssets) ctxt.performQuery(q).get(0);
		assertEquals(1, a1.getPaintingsCount().intValue());
	}


	public void testPerformQueries() throws Exception {
		SelectQuery q1 = new SelectQuery();
		q1.setRoot(Artist.class);
		SelectQuery q2 = new SelectQuery();
		q2.setRoot(Gallery.class);

		ArrayList qs = new ArrayList();
		qs.add(q1);
		qs.add(q2);
		ctxt.performQueries(qs, opObserver);

		// check query results
		ArrayList o1 = opObserver.objectsForQuery(q1);
		assertNotNull(o1);
		assertEquals(artistCount, o1.size());

		ArrayList o2 = opObserver.objectsForQuery(q2);
		assertNotNull(o2);
		assertEquals(galleryCount, o2.size());
	}

	public void testPerformSelectQuery() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		List objects = ctxt.performQuery(query);

		assertNotNull(objects);
		assertEquals(artistCount, objects.size());
		assertTrue(
			"Artist expected, got " + objects.get(0).getClass(),
			objects.get(0) instanceof Artist);
	}

	public void testPerformQuery() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		ctxt.performQuery(query, opObserver);
		ArrayList objects = opObserver.objectsForQuery(query);

		assertNotNull(objects);
		assertEquals(artistCount, objects.size());
	}
	
	public void testPerformPagedQuery() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		query.setPageSize(5);
		List objects = ctxt.performQuery(query);
		assertNotNull(objects);
		assertTrue(objects instanceof IncrementalFaultList);
		
		assertTrue(((IncrementalFaultList)objects).elements.get(0) instanceof Artist);
		assertTrue(((IncrementalFaultList)objects).elements.get(7) instanceof Map);
	}

	public void testPerformDataRowQuery() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		query.setFetchingDataRows(true);
		List objects = ctxt.performQuery(query);

		assertNotNull(objects);
		assertEquals(artistCount, objects.size());
		assertTrue(
			"Map expected, got " + objects.get(0).getClass(),
			objects.get(0) instanceof Map);
	}

	public void testCommitChangesRO1() throws Exception {
		Artist a1 = (Artist) ctxt.createAndRegisterNewObject("ROArtist");
		a1.setArtistName("abc");

		try {
			ctxt.commitChanges();
			fail("Inserting a 'read-only' object must fail.");
		} catch (Exception ex) {
			// exception is expected, 
			// must blow on saving new "read-only" object.
		}
	}

	public void testCommitChangesRO2() throws Exception {
		Artist a1 = fetchROArtist("artist1");
		a1.setArtistName("abc");

		try {
			ctxt.commitChanges();
			fail("Updating a 'read-only' object must fail.");
		} catch (Exception ex) {
			// exception is expected, 
			// must blow on saving new "read-only" object.
		}
	}

	public void testCommitChangesRO3() throws Exception {
		Artist a1 = fetchROArtist("artist1");
		ctxt.deleteObject(a1);

		try {
			ctxt.commitChanges();
			fail("Deleting a 'read-only' object must fail.");
		} catch (Exception ex) {
			// exception is expected, 
			// must blow on saving new "read-only" object.
		}
	}

	private Artist fetchArtist(String name) {
		SelectQuery q =
			new SelectQuery(
				"Artist",
				ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", name));
		List ats = ctxt.performQuery(q);
		return (ats.size() > 0) ? (Artist) ats.get(0) : null;
	}

	private Artist fetchROArtist(String name) {
		SelectQuery q =
			new SelectQuery(
				"ROArtist",
				ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", name));
		List ats = ctxt.performQuery(q);
		return (ats.size() > 0) ? (Artist) ats.get(0) : null;
	}

	public String artistName(int ind) {
		return "artist" + ind;
	}

	public void populateTables() throws Exception {
		String insertArtist =
			"INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";

		Connection conn = getConnection();

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(insertArtist);
			long dateBase = System.currentTimeMillis();

			for (int i = 1; i <= artistCount; i++) {
				stmt.setInt(1, i);
				stmt.setString(2, artistName(i));
				stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
				stmt.executeUpdate();
			}

			stmt.close();
			conn.commit();

			String insertGal =
				"INSERT INTO GALLERY (GALLERY_ID, GALLERY_NAME) VALUES (?,?)";
			stmt = conn.prepareStatement(insertGal);

			for (int i = 1; i <= galleryCount; i++) {
				stmt.setInt(1, i);
				stmt.setString(2, "gallery" + i);
				stmt.executeUpdate();
			}

			stmt.close();
			conn.commit();
		} finally {
			conn.close();
		}
	}

	/** Give each artist a single painting. */
	public void populatePaintings() throws Exception {
		String insertPaint =
			"INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) VALUES (?, ?, ?, ?)";

		Connection conn = getConnection();

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(insertPaint);

			for (int i = 1; i <= artistCount; i++) {
				stmt.setInt(1, i);
				stmt.setString(2, "P_" + artistName(i));
				stmt.setInt(3, i);
				stmt.setBigDecimal(4, new BigDecimal(i * 1000));
				stmt.executeUpdate();
			}

			stmt.close();
			conn.commit();
		} finally {
			conn.close();
		}
	}

	public void testPerformIteratedQuery1() throws Exception {
		SelectQuery q1 = new SelectQuery("Artist");
		ResultIterator it = ctxt.performIteratedQuery(q1);

		try {
			int count = 0;
			while (it.hasNextRow()) {
				it.nextDataRow();
				count++;
			}

			assertEquals(DataContextTst.artistCount, count);
		} finally {
			it.close();
		}
	}

	public void testPerformIteratedQuery2() throws Exception {
		populatePaintings();

		SelectQuery q1 = new SelectQuery("Artist");
		ResultIterator it = ctxt.performIteratedQuery(q1);

		// just for this test increase pool size
		changeMaxConnections(1);

		try {
			while (it.hasNextRow()) {
				Map row = it.nextDataRow();

				// try instantiating an object and fetching its relationships
				Artist obj = (Artist) ctxt.objectFromDataRow("Artist", row);
				List paintings = obj.getPaintingArray();
				assertNotNull(paintings);
				assertEquals(1, paintings.size());
			}
		} finally {
			// change allowed connections back
			changeMaxConnections(-1);

			it.close();
		}
	}

	public void changeMaxConnections(int delta) {
		DataNode node = ((DataDomain) ctxt.getParent()).getDataNodes()[0];
		PoolManager manager = (PoolManager) node.getDataSource();
		manager.setMaxConnections(manager.getMaxConnections() + delta);
	}

}