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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistAssets;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.MeaningfulPKTest1;
import org.objectstyle.art.Painting;
import org.objectstyle.art.ROArtist;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TestOperationObserver;
import org.objectstyle.cayenne.access.util.ContextSelectObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;

public class DataContextTst extends CayenneTestCase {

	public static final int artistCount = 25;
	public static final int galleryCount = 10;

	protected DataContext ctxt;
	protected TestOperationObserver opObserver;

	protected void setUp() throws java.lang.Exception {
		super.setUp();

		CayenneTestDatabaseSetup setup = getDatabaseSetup();
		setup.cleanTableData();
		populateTables();

		DataDomain dom = getDomain();
		setup.createPkSupportForMapEntities(
			(DataNode) dom.getDataNodes().iterator().next());

		ctxt = dom.createDataContext();
		opObserver = new TestOperationObserver();
	}

    public void testInsertWithMeaningfulPK() throws Exception {
    	MeaningfulPKTest1 obj = (MeaningfulPKTest1)ctxt.createAndRegisterNewObject("MeaningfulPKTest1");
    	obj.setArtistId(new Integer(1000));
    	obj.setArtistName("aaa-aaa");
    	obj.setDateOfBirth(new java.util.Date());
    	ctxt.commitChanges();
    }
    
	public void testCreatePermId1() throws Exception {
		Artist artist = new Artist();
		ctxt.registerNewObject(artist);
		ObjectId id = ctxt.createPermId(artist);
		assertNotNull(id);
	}

	public void testCreatePermId2() throws Exception {
		Artist artist = new Artist();
		ctxt.registerNewObject(artist, "Artist");
		ObjectId id1 = ctxt.createPermId(artist);
		ObjectId id2 = ctxt.createPermId(artist);
		//Must not fail on second call

		assertNotNull(id1);
		assertNotNull(id2);
		assertEquals(id1, id2); //Must be the same,
	}

	public void testMerge() throws Exception {
		String n1 = "changed";
		String n2 = "changed again";

		Artist a1 = fetchArtist("artist1");
		a1.setArtistName(n1);

		Map s2 = new HashMap();
		s2.put("ARTIST_NAME", n2);
		s2.put("DATE_OF_BIRTH", new java.util.Date());
		ObjEntity e = ctxt.getEntityResolver().lookupObjEntity(a1);
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
		ctxt.registerNewObject(p1);
		p1.setToArtist(a1);

		Map s1 = ctxt.takeObjectSnapshot(p1);
		Map idMap = a1.getObjectId().getIdSnapshot();
		assertEquals(idMap.get("ARTIST_ID"), s1.get("ARTIST_ID"));
	}

	public void testLookupEntity() throws Exception {
		assertNotNull(ctxt.getEntityResolver().lookupObjEntity(Artist.class));
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
			ExpressionFactory.binaryPathExp(
				Expression.EQUAL_TO,
				"artistName",
				"artist1");
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
			ExpressionFactory.binaryPathExp(
				Expression.EQUAL_TO,
				"artistName",
				"a");
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
			ExpressionFactory.binaryPathExp(
				Expression.EQUAL_TO,
				"artistName",
				"a");
		SelectQuery q = new SelectQuery("Artist", e);
		q.addPrefetch("paintingArray");
		q.addPrefetch("paintingArray.toGallery");
		q.addPrefetch("artistExhibitArray.toExhibit");

		SelectObserver o = new SelectObserver();
		ctxt.performQuery(q, o);

		assertEquals(4, o.getSelectCount());
	}

	/**
	 * Test that all queries specified in prefetch are executed
	 * in a more complex prefetch scenario with no reverse 
	 * obj relationships
	 */
	public void testPrefetch2b() throws Exception {
		this.populatePaintings();
		EntityResolver er = ctxt.getEntityResolver();
		ObjEntity paintingEntity = er.lookupObjEntity(Painting.class);
		ObjEntity galleryEntity = er.lookupObjEntity(Gallery.class);
		ObjEntity artistExhibitEntity = er.lookupObjEntity(ArtistExhibit.class);
		ObjEntity exhibitEntity = er.lookupObjEntity(Exhibit.class);
		ObjRelationship paintingToArtistRel =
			(ObjRelationship) paintingEntity.getRelationship("toArtist");
		paintingEntity.removeRelationship("toArtist");

		ObjRelationship galleryToPaintingRel =
			(ObjRelationship) galleryEntity.getRelationship("paintingArray");
		galleryEntity.removeRelationship("paintingArray");

		ObjRelationship artistExhibitToArtistRel =
			(ObjRelationship) artistExhibitEntity.getRelationship("toArtist");
		artistExhibitEntity.removeRelationship("toArtist");

		ObjRelationship exhibitToArtistExhibitRel =
			(ObjRelationship) exhibitEntity.getRelationship(
				"artistExhibitArray");
		exhibitEntity.removeRelationship("artistExhibitArray");

		Expression e =
			ExpressionFactory.binaryPathExp(
				Expression.EQUAL_TO,
				"artistName",
				this.artistName(1));
		SelectQuery q = new SelectQuery("Artist", e);
		q.addPrefetch("paintingArray");
		q.addPrefetch("paintingArray.toGallery");
		q.addPrefetch("artistExhibitArray.toExhibit");
		SelectObserver o = new SelectObserver();
		try {
			ctxt.performQuery(q, o);
		} finally {
			paintingEntity.addRelationship(paintingToArtistRel);
			galleryEntity.addRelationship(galleryToPaintingRel);
			artistExhibitEntity.addRelationship(artistExhibitToArtistRel);
			exhibitEntity.addRelationship(exhibitToArtistExhibitRel);
		}

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
		ToManyList toMany =
			(ToManyList) a1.readPropertyDirectly("paintingArray");
		assertNotNull(toMany);

		assertFalse(toMany.needsFetch());
	}

	/**
	 * Test that a to-many relationship is initialized when there
	 * is no inverse relationship
	 */
	public void testPrefetch3a() throws Exception {
		populatePaintings();

		ObjEntity paintingEntity =
			ctxt.getEntityResolver().lookupObjEntity(Painting.class);
		ObjRelationship relationship =
			(ObjRelationship) paintingEntity.getRelationship("toArtist");
		paintingEntity.removeRelationship("toArtist");

		SelectQuery q = new SelectQuery("Artist");
		q.addPrefetch("paintingArray");

		try {
			CayenneDataObject a1 =
				(CayenneDataObject) ctxt.performQuery(q).get(0);
			ToManyList toMany =
				(ToManyList) a1.readPropertyDirectly("paintingArray");
			assertNotNull(toMany);

			assertFalse(toMany.needsFetch());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should not have failed " + e.getMessage());
		} finally {
			//Fix it up again, so other tests do not fail
			paintingEntity.addRelationship(relationship);
		}

	}

	/**
	 * Test that a to-many relationship is initialized when there
	 * is no inverse relationship and the root query is qualified
	 */
	public void testPrefetch3b() throws Exception {
		populatePaintings();

		ObjEntity paintingEntity =
			ctxt.getEntityResolver().lookupObjEntity(Painting.class);
		ObjRelationship relationship =
			(ObjRelationship) paintingEntity.getRelationship("toArtist");
		paintingEntity.removeRelationship("toArtist");

		SelectQuery q = new SelectQuery("Artist");
		q.setQualifier(
			ExpressionFactory.binaryPathExp(
				Expression.EQUAL_TO,
				"artistName",
				this.artistName(1)));
		q.addPrefetch("paintingArray");

		try {
			CayenneDataObject a1 =
				(CayenneDataObject) ctxt.performQuery(q).get(0);
			ToManyList toMany =
				(ToManyList) a1.readPropertyDirectly("paintingArray");
			assertNotNull(toMany);

			assertFalse(toMany.needsFetch());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should not have failed " + e.getMessage());
		} finally {
			//Fix it up again, so other tests do not fail
			paintingEntity.addRelationship(relationship);
		}

	}

	/**
	 * Test that a to-one relationship is initialized.
	 */
	public void testPrefetch4() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Painting");
		q.addPrefetch("toArtist");

		CayenneDataObject p1 = (CayenneDataObject) ctxt.performQuery(q).get(0);
		CayenneDataObject a1 =
			(CayenneDataObject) p1.readPropertyDirectly("toArtist");

		assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
	}

	/**
	 * Test prefetching with queries using DB_PATH.
	 */
	public void testPrefetch5() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Painting");
		q.andQualifier(
			ExpressionFactory.matchDbExp(
				"toArtist.ARTIST_NAME",
				artistName(2)));
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
		q.andQualifier(
			ExpressionFactory.matchExp("toArtist.artistName", artistName(2)));
		q.addPrefetch("toArtist");
		// q.setLoggingLevel(Level.INFO);

		List results = ctxt.performQuery(q);
		assertEquals(1, results.size());
	}

	/**
	 * Test prefetching with the prefetch on a reflexive relationship
	 */
	public void testPrefetch7() throws Exception {
		ArtGroup parent =
			(ArtGroup) ctxt.createAndRegisterNewObject("ArtGroup");
		parent.setName("parent");
		ArtGroup child = (ArtGroup) ctxt.createAndRegisterNewObject("ArtGroup");
		child.setName("child");
		child.setToParentGroup(parent);
		ctxt.commitChanges();

		SelectQuery q = new SelectQuery("ArtGroup");
		q.setQualifier(ExpressionFactory.matchExp("name", "child"));
		q.addPrefetch("toParentGroup");

		ContextSelectObserver o = new ContextSelectObserver(ctxt, Level.WARN);
		try {
			ctxt.performQuery(q, o);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should not have failed with exception " + e.getMessage());
		}

		assertEquals(2, o.getSelectCount());

		List results = o.getResults(q);
		assertEquals(1, results.size());

		ArtGroup fetchedChild = (ArtGroup) results.get(0);
		//The parent must be fully fetched, not just HOLLOW (a fault)
		assertEquals(
			PersistenceState.COMMITTED,
			fetchedChild.getToParentGroup().getPersistenceState());
	}

	/**
	 * Test prefetching with qualifier on the root query 
	 * containing the path to the prefetch
	 */
	public void testPrefetch8() throws Exception {
		this.populatePaintings();
		Expression exp =
			ExpressionFactory.matchExp(
				"toArtist.artistName",
				this.artistName(1));

		SelectQuery q = new SelectQuery(Painting.class, exp);

		q.addPrefetch("toArtist");
		ContextSelectObserver o = new ContextSelectObserver(ctxt, Level.WARN);
		try {
			ctxt.performQuery(q, o);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should not have failed with exception " + e.getMessage());
		}
		assertEquals(2, o.getSelectCount());

		List results = o.getResults(q);
		assertEquals(1, results.size());

		Painting painting = (Painting) results.get(0);
		//The parent must be fully fetched, not just HOLLOW (a fault)
		assertEquals(
			PersistenceState.COMMITTED,
			painting.getToArtist().getPersistenceState());

	}

	/**
	 * Test prefetching with qualifier on the root query 
	 * being the path to the prefetch
	 */
	public void testPrefetch9() throws Exception {
		this.populatePaintings();
		Expression artistExp =
			ExpressionFactory.matchExp("artistName", this.artistName(1));
		SelectQuery artistQuery = new SelectQuery(Artist.class, artistExp);
		Artist artist1=(Artist)ctxt.performQuery(artistQuery).get(0);
		
		//Try and find the painting matching the artist
		Expression exp = ExpressionFactory.matchExp("toArtist", artist1);

		SelectQuery q = new SelectQuery(Painting.class, exp);
		q.addPrefetch("toArtist");
		
		//The rest of this test causes failures.  Do we even need to fix this
		// given the rather odd nature of what is trying to be done 
		// (prefetching an object which we used to create the root query
		// qualifier in the first place)?
		/*
		ContextSelectObserver o = new ContextSelectObserver(ctxt, Level.WARN);
		try {
			ctxt.performQuery(q, o);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should not have failed with exception " + e.getMessage());
		}
		assertEquals(2, o.getSelectCount());

		List results = o.getResults(q);
		assertEquals(1, results.size());

		Painting painting = (Painting) results.get(0);
		//The parent must be fully fetched, not just HOLLOW (a fault)
		assertEquals(
			PersistenceState.COMMITTED,
			painting.getToArtist().getPersistenceState());
		*/

	}
	/**
	 * Test fetching query with multiple relationship
	 * paths between the same 2 entities used in qualifier.
	 */
	public void testMultiObjRelFetch() throws Exception {
		populatePaintings();

		SelectQuery q = new SelectQuery("Painting");
		q.andQualifier(
			ExpressionFactory.matchExp("toArtist.artistName", artistName(2)));
		q.orQualifier(
			ExpressionFactory.matchExp("toArtist.artistName", artistName(4)));
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
			ExpressionFactory.matchDbExp(
				"toArtist.ARTIST_NAME",
				artistName(2)));
		q.orQualifier(
			ExpressionFactory.matchDbExp(
				"toArtist.ARTIST_NAME",
				artistName(4)));
		List results = ctxt.performQuery(q);

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

		ArtistAssets a1 = (ArtistAssets) ctxt.performQuery(q).get(0);
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

		ArtistAssets a1 = (ArtistAssets) ctxt.performQuery(q).get(0);
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
		ctxt.performQueries(qs, opObserver);

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
		List objects = ctxt.performQuery(query);

		assertNotNull(objects);
		assertEquals(artistCount, objects.size());

		Artist a1 = (Artist) objects.get(0);
		assertEquals(java.util.Date.class, a1.getDateOfBirth().getClass());
	}

	public void testPerformSelectQuery1() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		List objects = ctxt.performQuery(query);

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
		List objects = ctxt.performQuery(query);

		assertNotNull(objects);
		assertEquals(3, objects.size());
		assertTrue(
			"Artist expected, got " + objects.get(0).getClass(),
			objects.get(0) instanceof Artist);
	}

	public void testPerformQuery() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		ctxt.performQuery(query, opObserver);
		List objects = opObserver.objectsForQuery(query);

		assertNotNull(objects);
		assertEquals(artistCount, objects.size());
	}

	public void testPerformPagedQuery() throws Exception {
		SelectQuery query = new SelectQuery("Artist");
		query.setPageSize(5);
		List objects = ctxt.performQuery(query);
		assertNotNull(objects);
		assertTrue(objects instanceof IncrementalFaultList);

		assertTrue(
			((IncrementalFaultList) objects).elements.get(0) instanceof Artist);
		assertTrue(
			((IncrementalFaultList) objects).elements.get(7) instanceof Map);
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
		ROArtist a1 = (ROArtist) ctxt.createAndRegisterNewObject("ROArtist");
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
		ROArtist a1 = fetchROArtist("artist1");
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
		ROArtist a1 = fetchROArtist("artist1");
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
				ExpressionFactory.matchExp("artistName", name));
		List ats = ctxt.performQuery(q);
		return (ats.size() > 0) ? (Artist) ats.get(0) : null;
	}

	private ROArtist fetchROArtist(String name) {
		SelectQuery q =
			new SelectQuery(
				"ROArtist",
				ExpressionFactory.matchExp("artistName", name));
		List ats = ctxt.performQuery(q);
		return (ats.size() > 0) ? (ROArtist) ats.get(0) : null;
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
				stmt.setDate(
					3,
					new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
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
		DataNode node = (DataNode)((DataDomain)ctxt.getParent()).getDataNodes().iterator().next();
		PoolManager manager = (PoolManager)node.getDataSource();
		manager.setMaxConnections(manager.getMaxConnections() + delta);
	}

	public void testRollbackNewObject() {
		String artistName = "revertTestArtist";
		Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName(artistName);

		ctxt.rollbackChanges();

		assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
		ctxt.commitChanges();
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
		Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName(artistName);

		Painting painting =
			(Painting) ctxt.createAndRegisterNewObject("Painting");
		painting.setPaintingTitle(paintingTitle);
		painting.setToArtist(artist);

		try {
			ctxt.rollbackChanges();
		} catch (Exception e) {
			e.printStackTrace();
			fail(
				"rollbackChanges should not have caused the exception "
					+ e.getMessage());
		}

		assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
		ctxt.commitChanges();
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
		Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName(artistName);
		ctxt.commitChanges();
		//Save... cayenne doesn't yet handle deleting objects that are uncommitted
		ctxt.deleteObject(artist);
		ctxt.rollbackChanges();

		//Now check everything is as it should be
		assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());

		ctxt.commitChanges();
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
		Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName(artistName);
		ctxt.commitChanges();

		artist.setArtistName("a new value");

		ctxt.rollbackChanges();

		//Make sure the inmemory changes have been rolled back
		assertEquals(artistName, artist.getArtistName());

		//Commit what's in memory...
		ctxt.commitChanges();

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
		Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName(artistName);
		Painting painting =
			(Painting) ctxt.createAndRegisterNewObject("Painting");
		painting.setPaintingTitle(paintingTitle);
		painting.setToArtist(artist);
		ctxt.commitChanges();

		painting.setToArtist(null);
		ctxt.rollbackChanges();

		assertEquals(artist, painting.getToArtist());

		//Check that the reverse relationship was handled
		assertEquals(1, artist.getPaintingArray().size());
		ctxt.commitChanges();

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
		String artistName="ArtistName";
		Artist artist=(Artist)ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName(artistName);
		ctxt.commitChanges();
		
		artist.setArtistName(artistName); //Set again to *exactly* the same value
		assertFalse(ctxt.hasChanges());
	}
	
	/**
	 * Tests that hasChanges performs correctly when an object is "modified" 
	 * and the property is simply set to the same value (an unreal modification) 
	 */
	public void testHasChangesRealModify() {
		Artist artist=(Artist)ctxt.createAndRegisterNewObject("Artist");
		artist.setArtistName("ArtistName");
		ctxt.commitChanges();
		
		artist.setArtistName("Something different"); 
		assertTrue(ctxt.hasChanges());
	}

}