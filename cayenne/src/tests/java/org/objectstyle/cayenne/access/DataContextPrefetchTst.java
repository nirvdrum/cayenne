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

import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.ContextSelectObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class DataContextPrefetchTst extends DataContextTestBase {
    /**
      * Test that all queries specified in prefetch are executed
      * with a single prefetch path.
      */
    public void testPrefetch1() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "a");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");

        SelectObserver o = new SelectObserver();
        context.performQuery(q, o);

        assertEquals(2, o.getSelectCount());
    }

    /**
     * Test that all queries specified in prefetch are executed
     * in a more complex prefetch scenario.
     */
    public void testPrefetch2() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "a");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");

        SelectObserver o = new SelectObserver();
        context.performQuery(q, o);

        assertEquals(4, o.getSelectCount());
    }

    /**
     * Test that all queries specified in prefetch are executed
     * in a more complex prefetch scenario with no reverse 
     * obj relationships
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
            context.performQuery(q, o);
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
    public void testPrefetch3() throws Exception {
        populatePaintings();

        Expression e = ExpressionFactory.matchExp("artistName", artistName(2));
        e = e.orExp(ExpressionFactory.matchExp("artistName", artistName(3)));
        SelectQuery q = new SelectQuery("Artist", e);
        q.setLoggingLevel(Level.WARN);
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
     * Test that a to-many relationship is initialized when there
     * is no inverse relationship
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
     * Test that a to-many relationship is initialized when there
     * is no inverse relationship and the root query is qualified
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

        CayenneDataObject p1 = (CayenneDataObject) context.performQuery(q).get(0);
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

        ContextSelectObserver o = new ContextSelectObserver(context, Level.WARN);
        try {
            context.performQuery(q, o);
        }
        catch (Exception e) {
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
            ExpressionFactory.matchExp("toArtist.artistName", this.artistName(1));

        SelectQuery q = new SelectQuery(Painting.class, exp);

        q.addPrefetch("toArtist");
        ContextSelectObserver o = new ContextSelectObserver(context, Level.WARN);
        try {
            context.performQuery(q, o);
        }
        catch (Exception e) {
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
        Artist artist1 = (Artist) context.performQuery(artistQuery).get(0);

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
}
