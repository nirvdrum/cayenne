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

package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.exp.ExpressionParameter;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class SelectQueryBasicsTst extends CayenneTestCase {

    public void testAddJointPrefetch() {
        SelectQuery q = new SelectQuery();

        String path = "a.b.c";
        q.addJointPrefetch(path);
        assertEquals(1, q.getJointPrefetches().size());
        assertSame(path, q.getJointPrefetches().iterator().next());
    }

    public void testAddJointPrefetches() {
        SelectQuery q = new SelectQuery();

        Collection prefetches = Arrays.asList(new Object[] {
                "abc", "123", "rrr"
        });

        q.addJointPrefetches(prefetches);
        assertEquals(prefetches.size(), q.getJointPrefetches().size());
        assertEquals(new HashSet(prefetches), q.getJointPrefetches());
    }

    public void testClearJointPrefetches() {
        SelectQuery q = new SelectQuery();

        q.addJointPrefetch("abc");
        q.addJointPrefetch("xyz");
        assertFalse(q.getJointPrefetches().isEmpty());

        q.clearJointPrefetches();
        assertTrue(q.getJointPrefetches().isEmpty());
    }

    public void testAddPrefetch() {
        SelectQuery q = new SelectQuery();

        String path = "a.b.c";
        q.addPrefetch(path);
        assertEquals(1, q.getPrefetches().size());
        assertSame(path, q.getPrefetches().iterator().next());
    }

    public void testAddPrefetches() {
        SelectQuery q = new SelectQuery();

        Collection prefetches = Arrays.asList(new Object[] {
                "abc", "123", "rrr"
        });

        q.addPrefetches(prefetches);
        assertEquals(prefetches.size(), q.getPrefetches().size());
        assertEquals(new HashSet(prefetches), q.getPrefetches());
    }

    public void testClearPrefetches() {
        SelectQuery q = new SelectQuery();

        q.addPrefetch("abc");
        q.addPrefetch("xyz");
        assertFalse(q.getPrefetches().isEmpty());

        q.clearPrefetches();
        assertTrue(q.getPrefetches().isEmpty());
    }

    public void testPageSize() throws Exception {
        SelectQuery q = new SelectQuery();
        q.setPageSize(10);
        assertEquals(10, q.getPageSize());
    }

    public void testAddOrdering1() throws Exception {
        SelectQuery q = new SelectQuery();

        Ordering ord = new Ordering();
        q.addOrdering(ord);
        assertEquals(1, q.getOrderings().size());
        assertSame(ord, q.getOrderings().get(0));
    }

    public void testAddOrdering2() throws Exception {
        SelectQuery q = new SelectQuery();

        String path = "a.b.c";
        q.addOrdering(path, Ordering.DESC);
        assertEquals(1, q.getOrderings().size());

        Ordering ord = (Ordering) q.getOrderings().get(0);
        assertEquals(path, ord.getSortSpec().getOperand(0));
        assertEquals(Ordering.DESC, ord.isAscending());
    }

    public void testDistinct() throws Exception {
        SelectQuery q = new SelectQuery();

        assertFalse(q.isDistinct());
        q.setDistinct(true);
        assertTrue(q.isDistinct());
    }

    public void testFetchingDataRowsSpecial() {
        SelectQuery q = new SelectQuery();

        assertFalse(q.isFetchingDataRows());
        q.addCustomDbAttribute("ARTIST_ID");
        assertTrue(q.isFetchingDataRows());

        // this shouldn't have any effect, since custom attributes are fetched
        q.setFetchingDataRows(false);
        assertTrue(q.isFetchingDataRows());
    }

    public void testQueryAttributes() throws Exception {
        SelectQuery q = new SelectQuery();

        assertEquals(0, q.getCustomDbAttributes().size());

        q.addCustomDbAttribute("ARTIST_ID");
        assertEquals(1, q.getCustomDbAttributes().size());
        assertEquals("ARTIST_ID", q.getCustomDbAttributes().get(0));
    }

    public void testUsingRootEntityAttributes() throws Exception {
        SelectQuery q = new SelectQuery();

        assertFalse(q.isFetchingCustomAttributes());

        q.addCustomDbAttribute("ARTIST_ID");
        assertTrue(q.isFetchingCustomAttributes());
    }

    public void testSetParentQualifier() throws Exception {
        SelectQuery q = new SelectQuery();
        assertNull(q.getParentQualifier());

        Expression qual = ExpressionFactory.expressionOfType(Expression.AND);
        q.setParentQualifier(qual);
        assertNotNull(q.getParentQualifier());
        assertSame(qual, q.getParentQualifier());
    }

    public void testAndParentQualifier() throws Exception {
        SelectQuery q = new SelectQuery();
        assertNull(q.getParentQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        q.andParentQualifier(e1);
        assertSame(e1, q.getParentQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        q.andParentQualifier(e2);
        assertEquals(Expression.AND, q.getParentQualifier().getType());
    }

    public void testOrParentQualifier() throws Exception {
        SelectQuery q = new SelectQuery();
        assertNull(q.getParentQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        q.orParentQualifier(e1);
        assertSame(e1, q.getParentQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        q.orParentQualifier(e2);
        assertEquals(Expression.OR, q.getParentQualifier().getType());
    }

    public void testParentObjEntityName() throws Exception {
        SelectQuery q = new SelectQuery();
        assertNull(q.getParentObjEntityName());

        q.setParentObjEntityName("SomeEntity");
        assertSame("SomeEntity", q.getParentObjEntityName());
    }

    public void testQueryWithParams1() throws Exception {
        SelectQuery q = new SelectQuery();
        q.setRoot(Artist.class);
        q.setDistinct(true);

        SelectQuery q1 = q.queryWithParameters(new HashMap(), true);
        assertSame(q.getRoot(), q1.getRoot());
        assertEquals(q.isDistinct(), q1.isDistinct());
        assertNull(q1.getQualifier());
    }

    public void testQueryWithParams2() throws Exception {
        SelectQuery q = new SelectQuery();
        q.setRoot(Artist.class);

        List list = new ArrayList();
        list.add(ExpressionFactory.matchExp("k1", new ExpressionParameter("test1")));
        list.add(ExpressionFactory.matchExp("k2", new ExpressionParameter("test2")));
        list.add(ExpressionFactory.matchExp("k3", new ExpressionParameter("test3")));
        list.add(ExpressionFactory.matchExp("k4", new ExpressionParameter("test4")));
        q.setQualifier(ExpressionFactory.joinExp(Expression.OR, list));

        Map params = new HashMap();
        params.put("test2", "abc");
        params.put("test3", "xyz");
        SelectQuery q1 = q.queryWithParameters(params, true);
        assertSame(q.getRoot(), q1.getRoot());
        assertNotNull(q1.getQualifier());
        assertTrue(q1.getQualifier() != q.getQualifier());
    }

    /**
     * "queryWithParameters" must not copy the name as name is used as the cache key and
     * new query shouldn't run against the same cache.
     */
    public void testQueryWithParamsSkipName() {
        SelectQuery q = new SelectQuery();
        q.setRoot(Artist.class);
        q.setDistinct(true);
        q.setName("name");

        SelectQuery q1 = q.queryWithParameters(Collections.EMPTY_MAP);
        assertEquals("name", q.getName());
        assertNull(q1.getName());
    }

    /**
     * Tests that all queries specified in prefetch are executed in a more complex
     * prefetch scenario.
     */
    public void testRouteWithPrefetches() {
        EntityResolver resolver = getDomain().getEntityResolver();
        MockQueryRouter router = new MockQueryRouter();

        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                "a"));

        q.route(router, resolver);
        assertEquals(1, router.getQueryCount());

        q.addPrefetch("paintingArray");
        router.reset();
        q.route(router, resolver);
        assertEquals(2, router.getQueryCount());

        q.addPrefetch("paintingArray.toGallery");
        router.reset();
        q.route(router, resolver);
        assertEquals(3, router.getQueryCount());

        q.addPrefetch("artistExhibitArray.toExhibit");
        router.reset();
        q.route(router, resolver);
        assertEquals(4, router.getQueryCount());

        q.removePrefetch("paintingArray");
        router.reset();
        q.route(router, resolver);
        assertEquals(3, router.getQueryCount());
    }

    /**
     * Tests that all queries specified in prefetch are executed in a more complex
     * prefetch scenario with no reverse obj relationships.
     */
    public void testRouteQueryWithPrefetchesNoReverse() {

        EntityResolver resolver = getDomain().getEntityResolver();
        ObjEntity paintingEntity = resolver.lookupObjEntity(Painting.class);
        ObjEntity galleryEntity = resolver.lookupObjEntity(Gallery.class);
        ObjEntity artistExhibitEntity = resolver.lookupObjEntity(ArtistExhibit.class);
        ObjEntity exhibitEntity = resolver.lookupObjEntity(Exhibit.class);
        ObjRelationship paintingToArtistRel = (ObjRelationship) paintingEntity
                .getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        ObjRelationship galleryToPaintingRel = (ObjRelationship) galleryEntity
                .getRelationship("paintingArray");
        galleryEntity.removeRelationship("paintingArray");

        ObjRelationship artistExhibitToArtistRel = (ObjRelationship) artistExhibitEntity
                .getRelationship("toArtist");
        artistExhibitEntity.removeRelationship("toArtist");

        ObjRelationship exhibitToArtistExhibitRel = (ObjRelationship) exhibitEntity
                .getRelationship("artistExhibitArray");
        exhibitEntity.removeRelationship("artistExhibitArray");

        Expression e = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");

        try {
            MockQueryRouter router = new MockQueryRouter();
            q.route(router, resolver);
            assertEquals(4, router.getQueryCount());
        }
        finally {
            paintingEntity.addRelationship(paintingToArtistRel);
            galleryEntity.addRelationship(galleryToPaintingRel);
            artistExhibitEntity.addRelationship(artistExhibitToArtistRel);
            exhibitEntity.addRelationship(exhibitToArtistExhibitRel);
        }
    }

    /**
     * Test prefetching with qualifier on the root query being the path to the prefetch.
     */
    public void testRouteQueryWithPrefetchesPrefetchExpressionPath() {

        // find the painting not matching the artist (this is the case where such prefetch
        // at least makes sense)
        Expression exp = ExpressionFactory.noMatchExp("toArtist", new Object());

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        // test how prefetches are resolved in this case - this was a stumbling block for
        // a while
        EntityResolver resolver = getDomain().getEntityResolver();
        MockQueryRouter router = new MockQueryRouter();
        q.route(router, resolver);
        assertEquals(2, router.getQueryCount());
    }
}