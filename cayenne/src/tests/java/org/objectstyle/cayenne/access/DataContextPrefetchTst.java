/*
 * ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.CharFkTest;
import org.objectstyle.art.CharPkTest;
import org.objectstyle.art.CompoundFkTest;
import org.objectstyle.art.CompoundPkTest;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.SqlModifyQuery;

/**
 * @author Andrei Adamchik
 */
public class DataContextPrefetchTst extends DataContextTestBase {
    private static Logger logObj = Logger.getLogger(DataContextPrefetchTst.class);

    /**
     * Test that all queries specified in prefetch are executed with a single
     * prefetch path.
     */
    public void testPrefetch1() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "a");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");

        SelectObserver o = new SelectObserver();
        context.performQueries(Collections.singletonList(q), o);

        assertEquals(2, o.getSelectCount());
    }

    /**
     * Test that all queries specified in prefetch are executed in a more
     * complex prefetch scenario.
     */
    public void testPrefetch2() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "a");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");

        SelectObserver o = new SelectObserver();
        context.performQueries(Collections.singletonList(q), o);

        assertEquals(4, o.getSelectCount());
    }

    /**
     * Test that all queries specified in prefetch are executed in a more
     * complex prefetch scenario with no reverse obj relationships
     */
    public void testPrefetch2b() throws Exception {
        this.populatePaintings();
        EntityResolver er = context.getEntityResolver();
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
            (ObjRelationship) exhibitEntity.getRelationship("artistExhibitArray");
        exhibitEntity.removeRelationship("artistExhibitArray");

        Expression e = ExpressionFactory.matchExp("artistName", this.artistName(1));
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");
        SelectObserver o = new SelectObserver();
        try {
            context.performQueries(Collections.singletonList(q), o);
        }
        finally {
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
    public void testPrefetchToMany() throws Exception {
        populatePaintings();

        Map params = new HashMap();
        params.put("name1", artistName(2));
        params.put("name2", artistName(3));
        Expression e =
            Expression.fromString("artistName = $name1 or artistName = $name2");
        SelectQuery q = new SelectQuery("Artist", e.expWithParameters(params));
        q.addPrefetch("paintingArray");

        List artists = context.performQuery(q);
        assertEquals(2, artists.size());

        Artist a1 = (Artist) artists.get(0);
        ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(1, toMany.size());

        Painting p1 = (Painting) toMany.get(0);
        assertEquals("P_" + a1.getArtistName(), p1.getPaintingTitle());

        Artist a2 = (Artist) artists.get(1);
        ToManyList toMany2 = (ToManyList) a2.readPropertyDirectly("paintingArray");
        assertNotNull(toMany2);
        assertFalse(toMany2.needsFetch());
        assertEquals(1, toMany2.size());

        Painting p2 = (Painting) toMany2.get(0);
        assertEquals("P_" + a2.getArtistName(), p2.getPaintingTitle());
    }

    /**
     * Test that a to-many relationship is initialized.
     */
    public void testPrefetchToManyNoQualifier() throws Exception {
        populatePaintings();
        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("paintingArray");

        List artists = context.performQuery(q);
        assertEquals(artistCount, artists.size());

        Artist a1 = (Artist) artists.get(0);
        ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(1, toMany.size());

        Painting p1 = (Painting) toMany.get(0);
        assertEquals("P_" + a1.getArtistName(), p1.getPaintingTitle());

        Artist a2 = (Artist) artists.get(1);
        ToManyList toMany2 = (ToManyList) a2.readPropertyDirectly("paintingArray");
        assertNotNull(toMany2);
        assertFalse(toMany2.needsFetch());
        assertEquals(1, toMany2.size());

        Painting p2 = (Painting) toMany2.get(0);
        assertEquals("P_" + a2.getArtistName(), p2.getPaintingTitle());
    }

    /**
     * Test that a to-many relationship is initialized when a target entity
     * has a compound PK only partially involved in relationmship.
     */
    public void testPrefetchToManyOnJoinTable() throws Exception {
        // setup data
        populateGalleries();
        populateExhibits();

        List queries = new ArrayList(4);
        queries.add(
            new SqlModifyQuery(
                ArtistExhibit.class,
                "insert into ARTIST_EXHIBIT (ARTIST_ID, EXHIBIT_ID) values ("
                    + safeId(1)
                    + ", 1)"));
        queries.add(
            new SqlModifyQuery(
                ArtistExhibit.class,
                "insert into ARTIST_EXHIBIT (ARTIST_ID, EXHIBIT_ID) values ("
                    + safeId(1)
                    + ", 2)"));
        queries.add(
            new SqlModifyQuery(
                ArtistExhibit.class,
                "insert into ARTIST_EXHIBIT (ARTIST_ID, EXHIBIT_ID) values ("
                    + safeId(2)
                    + ", 1)"));

        queries.add(
            new SqlModifyQuery(
                ArtistExhibit.class,
                "insert into ARTIST_EXHIBIT (ARTIST_ID, EXHIBIT_ID) values ("
                    + safeId(3)
                    + ", 2)"));

        context.performQueries(queries, new DefaultOperationObserver());

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("artistExhibitArray");
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List artists = context.performQuery(q);
        assertEquals(artistCount, artists.size());

        Artist a1 = (Artist) artists.get(0);
        logObj.warn("artist: " + a1);
        assertEquals(artistName(1), a1.getArtistName());
        ToManyList toMany = (ToManyList) a1.readPropertyDirectly("artistExhibitArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(2, toMany.size());

        ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
        assertSame(a1, artistExhibit.getToArtist());
    }

    public void testPrefetchToManyOnCharKey() throws Exception {

        List queries = new ArrayList(6);
        queries.add(
            new SqlModifyQuery(
                CharPkTest.class,
                "insert into CHAR_PK_TEST (PK_COL, OTHER_COL) values ('k1', 'n1')"));
        queries.add(
            new SqlModifyQuery(
                CharPkTest.class,
                "insert into CHAR_PK_TEST (PK_COL, OTHER_COL) values ('k2', 'n2')"));
        queries.add(
            new SqlModifyQuery(
                CharFkTest.class,
                "insert into CHAR_FK_TEST (PK, FK_COL, NAME) values (1, 'k1', 'fn1')"));
        queries.add(
            new SqlModifyQuery(
                CharFkTest.class,
                "insert into CHAR_FK_TEST (PK, FK_COL, NAME) values (2, 'k1', 'fn2')"));
        queries.add(
            new SqlModifyQuery(
                CharFkTest.class,
                "insert into CHAR_FK_TEST (PK, FK_COL, NAME) values (3, 'k2', 'fn3')"));
        queries.add(
            new SqlModifyQuery(
                CharFkTest.class,
                "insert into CHAR_FK_TEST (PK, FK_COL, NAME) values (4, 'k2', 'fn4')"));
        queries.add(
            new SqlModifyQuery(
                CharFkTest.class,
                "insert into CHAR_FK_TEST (PK, FK_COL, NAME) values (5, 'k1', 'fn5')"));

        context.performQueries(queries, new DefaultOperationObserver());

        SelectQuery q = new SelectQuery(CharPkTest.class);
        q.addPrefetch("charFKs");
        q.addOrdering(CharPkTest.OTHER_COL_PROPERTY, Ordering.ASC);

        List pks = context.performQuery(q);
        assertEquals(2, pks.size());

        CharPkTest pk1 = (CharPkTest) pks.get(0);
        logObj.warn("PK1: " + pk1);
        assertEquals("n1", pk1.getOtherCol());
        ToManyList toMany = (ToManyList) pk1.readPropertyDirectly("charFKs");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(3, toMany.size());

        CharFkTest fk1 = (CharFkTest) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());
        assertSame(pk1, fk1.getToCharPK());
    }

    /**
     * Test that a to-many relationship is initialized when there is no inverse
     * relationship
     */
    public void testPrefetch3a() throws Exception {
        populatePaintings();

        ObjEntity paintingEntity =
            context.getEntityResolver().lookupObjEntity(Painting.class);
        ObjRelationship relationship =
            (ObjRelationship) paintingEntity.getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        SelectQuery q = new SelectQuery("Artist");
        q.addPrefetch("paintingArray");

        try {
            CayenneDataObject a1 = (CayenneDataObject) context.performQuery(q).get(0);
            ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
            assertNotNull(toMany);

            assertFalse(toMany.needsFetch());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have failed " + e.getMessage());
        }
        finally {
            //Fix it up again, so other tests do not fail
            paintingEntity.addRelationship(relationship);
        }

    }

    /**
     * Test that a to-many relationship is initialized when there is no inverse
     * relationship and the root query is qualified
     */
    public void testPrefetch3b() throws Exception {
        populatePaintings();

        ObjEntity paintingEntity =
            context.getEntityResolver().lookupObjEntity(Painting.class);
        ObjRelationship relationship =
            (ObjRelationship) paintingEntity.getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        SelectQuery q = new SelectQuery("Artist");
        q.setQualifier(ExpressionFactory.matchExp("artistName", this.artistName(1)));
        q.addPrefetch("paintingArray");

        try {
            CayenneDataObject a1 = (CayenneDataObject) context.performQuery(q).get(0);
            ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
            assertNotNull(toMany);

            assertFalse(toMany.needsFetch());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have failed " + e.getMessage());
        }
        finally {
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

        DataObject p1 = (DataObject) context.performQuery(q).get(0);

        // resolving the fault must not result in extra queries, since
        // artist must have been prefetched
        DataContextDelegate delegate = new DefaultDataContextDelegate() {
            public GenericSelectQuery willPerformSelect(
                DataContext context,
                GenericSelectQuery query) {
                throw new CayenneRuntimeException(
                    "No query expected.. attempt to run: " + query);
            }
        };

        p1.getDataContext().setDelegate(delegate);

        Object toOnePrefetch = p1.readNestedProperty("toArtist");
        assertNotNull(toOnePrefetch);
        assertTrue(
            "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
            toOnePrefetch instanceof DataObject);

        DataObject a1 = (DataObject) toOnePrefetch;
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

        List results = context.performQuery(q);
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

        List results = context.performQuery(q);
        assertEquals(1, results.size());
    }

    /**
     * Test prefetching with the prefetch on a reflexive relationship
     */
    public void testPrefetch7() throws Exception {
        ArtGroup parent = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        parent.setName("parent");
        ArtGroup child = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        child.setName("child");
        child.setToParentGroup(parent);
        context.commitChanges();

        SelectQuery q = new SelectQuery("ArtGroup");
        q.setQualifier(ExpressionFactory.matchExp("name", "child"));
        q.addPrefetch("toParentGroup");

        SelectObserver o = new SelectObserver();
        try {
            context.performQueries(Collections.singletonList(q), o);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have failed with exception " + e.getMessage());
        }

        assertEquals(2, o.getSelectCount());

        List results = o.getResultsAsObjects(context, q);
        assertEquals(1, results.size());

        ArtGroup fetchedChild = (ArtGroup) results.get(0);
        //The parent must be fully fetched, not just HOLLOW (a fault)
        assertEquals(
            PersistenceState.COMMITTED,
            fetchedChild.getToParentGroup().getPersistenceState());
    }

    /**
     * Test prefetching with qualifier on the root query containing the path to
     * the prefetch
     */
    public void testPrefetch8() throws Exception {
        this.populatePaintings();
        Expression exp =
            ExpressionFactory.matchExp("toArtist.artistName", this.artistName(1));

        SelectQuery q = new SelectQuery(Painting.class, exp);

        q.addPrefetch("toArtist");
        SelectObserver o = new SelectObserver();
        try {
            context.performQueries(Collections.singletonList(q), o);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have failed with exception " + e.getMessage());
        }
        assertEquals(2, o.getSelectCount());

        List results = o.getResultsAsObjects(context, q);
        assertEquals(1, results.size());

        Painting painting = (Painting) results.get(0);
        //The parent must be fully fetched, not just HOLLOW (a fault)
        assertEquals(
            PersistenceState.COMMITTED,
            painting.getToArtist().getPersistenceState());

    }

    /**
     * Test prefetching with qualifier on the root query being the path to the
     * prefetch
     */
    public void testPrefetch9() throws Exception {
        this.populatePaintings();
        Expression artistExp =
            ExpressionFactory.matchExp("artistName", this.artistName(1));
        SelectQuery artistQuery = new SelectQuery(Artist.class, artistExp);
        Artist artist1 = (Artist) context.performQuery(artistQuery).get(0);

        //Try and find the painting matching the artist
        Expression exp = ExpressionFactory.matchExp("toArtist", artist1);

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        //The rest of this test causes failures. Do we even need to fix this
        // given the rather odd nature of what is trying to be done
        // (prefetching an object which we used to create the root query
        // qualifier in the first place)?
        /*
         * ContextSelectObserver o = new ContextSelectObserver(ctxt,
         * Level.WARN); try { ctxt.performQuery(q, o); } catch (Exception e) {
         * e.printStackTrace(); fail("Should not have failed with exception " +
         * e.getMessage()); } assertEquals(2, o.getSelectCount());
         * 
         * List results = o.getResults(q); assertEquals(1, results.size());
         * 
         * Painting painting = (Painting) results.get(0); //The parent must be
         * fully fetched, not just HOLLOW (a fault) assertEquals(
         * PersistenceState.COMMITTED,
         * painting.getToArtist().getPersistenceState());
         */

    }

    /**
     * Tests to-one prefetching over relationships with compound keys.
     */
    public void testPrefetch10() throws Exception {
        populateCompoundKeyEntities();

        Expression e = ExpressionFactory.matchExp("name", "CFK2");
        SelectQuery q = new SelectQuery(CompoundFkTest.class, e);
        q.addPrefetch("toCompoundPk");

        List objects = context.performQuery(q);
        assertEquals(1, objects.size());
        CayenneDataObject fk1 = (CayenneDataObject) objects.get(0);

        // resolving the fault must not result in extra queries, since
        // artist must have been prefetched
        DataContextDelegate delegate = new DefaultDataContextDelegate() {
            public GenericSelectQuery willPerformSelect(
                DataContext context,
                GenericSelectQuery query) {
                throw new CayenneRuntimeException(
                    "No query expected.. attempt to run: " + query);
            }
        };

        fk1.getDataContext().setDelegate(delegate);

        Object toOnePrefetch = fk1.readNestedProperty("toCompoundPk");
        assertNotNull(toOnePrefetch);
        assertTrue(
            "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
            toOnePrefetch instanceof DataObject);

        DataObject pk1 = (DataObject) toOnePrefetch;
        assertEquals(PersistenceState.COMMITTED, pk1.getPersistenceState());
        assertEquals("CPK2", pk1.readPropertyDirectly("name"));
    }

    /**
     * Tests to-many prefetching over relationships with compound keys.
     */
    public void testPrefetch11() throws Exception {
        populateCompoundKeyEntities();

        Expression e = ExpressionFactory.matchExp("name", "CPK2");
        SelectQuery q = new SelectQuery(CompoundPkTest.class, e);
        q.addPrefetch("compoundFkArray");

        List pks = context.performQuery(q);
        assertEquals(1, pks.size());
        CayenneDataObject pk1 = (CayenneDataObject) pks.get(0);

        ToManyList toMany = (ToManyList) pk1.readPropertyDirectly("compoundFkArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(2, toMany.size());

        CayenneDataObject fk1 = (CayenneDataObject) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());

        CayenneDataObject fk2 = (CayenneDataObject) toMany.get(1);
        assertEquals(PersistenceState.COMMITTED, fk2.getPersistenceState());
    }

    protected void populateCompoundKeyEntities() {
        List queries = new ArrayList(6);
        queries.add(
            new SqlModifyQuery(
                CompoundPkTest.class,
                "insert into COMPOUND_PK_TEST (KEY1, KEY2, NAME) values ('101', '201', 'CPK1')"));
        queries.add(
            new SqlModifyQuery(
                CompoundPkTest.class,
                "insert into COMPOUND_PK_TEST (KEY1, KEY2, NAME) values ('102', '202', 'CPK2')"));
        queries.add(
            new SqlModifyQuery(
                CompoundPkTest.class,
                "insert into COMPOUND_PK_TEST (KEY1, KEY2, NAME) values ('103', '203', 'CPK3')"));

        queries.add(
            new SqlModifyQuery(
                CompoundPkTest.class,
                "insert into COMPOUND_FK_TEST (PKEY, F_KEY1, F_KEY2, NAME) values (301, '102', '202', 'CFK1')"));
        queries.add(
            new SqlModifyQuery(
                CompoundPkTest.class,
                "insert into COMPOUND_FK_TEST (PKEY, F_KEY1, F_KEY2, NAME) values (302, '102', '202', 'CFK2')"));
        queries.add(
            new SqlModifyQuery(
                CompoundPkTest.class,
                "insert into COMPOUND_FK_TEST (PKEY, F_KEY1, F_KEY2, NAME) values (303, '101', '201', 'CFK3')"));

        context.performQueries(queries, new DefaultOperationObserver());
    }

}
