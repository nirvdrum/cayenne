/* ====================================================================
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
package org.objectstyle.cayenne.exp.parser;

import java.math.BigDecimal;
import java.util.Iterator;

import org.objectstyle.art.Artist;
import org.objectstyle.art.NonPersistentBean;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ExpressionEvaluateInMemoryTst extends CayenneTestCase {

    public void testEvaluateOBJ_PATH_DataObject() throws Exception {
        ASTObjPath node = new ASTObjPath("artistName");

        Artist a1 = new Artist();
        a1.setArtistName("abc");
        assertEquals("abc", node.evaluate(a1));

        Artist a2 = new Artist();
        a2.setArtistName("123");
        assertEquals("123", node.evaluate(a2));
    }

    public void testEvaluateOBJ_PATH_JavaBean() throws Exception {
        ASTObjPath node = new ASTObjPath("property2");

        NonPersistentBean b1 = new NonPersistentBean();
        b1.setProperty2(1);
        assertEquals(new Integer(1), node.evaluate(b1));

        NonPersistentBean b2 = new NonPersistentBean();
        b2.setProperty2(-3);
        assertEquals(new Integer(-3), node.evaluate(b2));
    }

    public void testEvaluateOBJ_PATH_ObjEntity() throws Exception {
        ASTObjPath node = new ASTObjPath("paintingArray.paintingTitle");

        ObjEntity ae = getDomain().getEntityResolver().lookupObjEntity(Artist.class);

        Object target = node.evaluate(ae);
        assertTrue(target instanceof Iterator);

        Iterator it = (Iterator) target;
        assertTrue(it.next() instanceof ObjRelationship);
        assertTrue(it.next() instanceof ObjAttribute);
        assertFalse(it.hasNext());
    }

    public void testEvaluateDB_PATH_DbEntity() throws Exception {
        Expression e = Expression.fromString("db:paintingArray.PAINTING_TITLE");

        ObjEntity ae = getDomain().getEntityResolver().lookupObjEntity(Artist.class);
        DbEntity ade = ae.getDbEntity();

        Object objTarget = e.evaluate(ae);
        assertTrue(objTarget instanceof Iterator);

        Iterator it = (Iterator) objTarget;
        assertTrue(it.next() instanceof DbRelationship);
        assertTrue(it.next() instanceof DbAttribute);
        assertFalse(it.hasNext());

        Object dbTarget = e.evaluate(ade);
        assertTrue(dbTarget instanceof Iterator);

        it = (Iterator) dbTarget;
        assertTrue(it.next() instanceof DbRelationship);
        assertTrue(it.next() instanceof DbAttribute);
        assertFalse(it.hasNext());
    }

    public void testEvaluateEQUAL_TO() throws Exception {
        Expression equalTo = new ASTEqual(new ASTObjPath("artistName"), "abc");
        Expression notEqualTo = new ASTNotEqual(new ASTObjPath("artistName"), "abc");

        Artist match = new Artist();
        match.setArtistName("abc");
        assertTrue(equalTo.evaluateBoolean(match));
        assertFalse(notEqualTo.evaluateBoolean(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse("Failed: " + equalTo, equalTo.evaluateBoolean(noMatch));
        assertTrue("Failed: " + notEqualTo, notEqualTo.evaluateBoolean(noMatch));
    }

    public void testEvaluateEQUAL_TODataObject() throws Exception {
        DataContext context = createDataContext();
        Artist a1 = (Artist) context.createAndRegisterNewObject("Artist");
        Artist a2 = (Artist) context.createAndRegisterNewObject("Artist");
        Painting p1 = (Painting) context.createAndRegisterNewObject("Painting");
        Painting p2 = (Painting) context.createAndRegisterNewObject("Painting");
        Painting p3 = (Painting) context.createAndRegisterNewObject("Painting");

        p1.setToArtist(a1);
        p2.setToArtist(a2);

        Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1);

        assertTrue(e.evaluateBoolean(p1));
        assertFalse(e.evaluateBoolean(p2));
        assertFalse(e.evaluateBoolean(p3));
    }

    public void testEvaluateAND() throws Exception {
        Expression e1 = new ASTEqual(new ASTObjPath("artistName"), "abc");
        Expression e2 = new ASTEqual(new ASTObjPath("artistName"), "abc");

        ASTAnd e = new ASTAnd(new Object[] { e1, e2 });

        Artist match = new Artist();
        match.setArtistName("abc");
        assertTrue(e.evaluateBoolean(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse(e.evaluateBoolean(noMatch));
    }

    public void testEvaluateOR() throws Exception {
        Expression e1 = new ASTEqual(new ASTObjPath("artistName"), "abc");
        Expression e2 = new ASTEqual(new ASTObjPath("artistName"), "xyz");

        ASTOr e = new ASTOr(new Object[] { e1, e2 });

        Artist match1 = new Artist();
        match1.setArtistName("abc");
        assertTrue("Failed: " + e, e.evaluateBoolean(match1));

        Artist match2 = new Artist();
        match2.setArtistName("xyz");
        assertTrue("Failed: " + e, e.evaluateBoolean(match2));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse("Failed: " + e, e.evaluateBoolean(noMatch));
    }

    public void testEvaluateNOT() throws Exception {
        ASTNot e = new ASTNot(new ASTEqual(new ASTObjPath("artistName"), "abc"));

        Artist noMatch = new Artist();
        noMatch.setArtistName("abc");
        assertFalse(e.evaluateBoolean(noMatch));

        Artist match = new Artist();
        match.setArtistName("123");
        assertTrue("Failed: " + e, e.evaluateBoolean(match));
    }

    public void testEvaluateLESS_THAN() throws Exception {
        Expression e =
            new ASTLess(new ASTObjPath("estimatedPrice"), new BigDecimal(10000));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(10001));
        assertFalse("Failed: " + e, e.evaluateBoolean(noMatch));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(10000));
        assertFalse("Failed: " + e, e.evaluateBoolean(noMatch1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(9999));
        assertTrue("Failed: " + e, e.evaluateBoolean(match));
    }

    public void testEvaluateLESS_THAN_EQUAL_TO() throws Exception {
        Expression e =
            new ASTLessOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(10001));
        assertFalse(e.evaluateBoolean(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(10000));
        assertTrue(e.evaluateBoolean(match1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(9999));
        assertTrue("Failed: " + e, e.evaluateBoolean(match));
    }

    public void testEvaluateGREATER_THAN() throws Exception {
        Expression e =
            new ASTGreater(new ASTObjPath("estimatedPrice"), new BigDecimal(10000));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(9999));
        assertFalse(e.evaluateBoolean(noMatch));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(10000));
        assertFalse(e.evaluateBoolean(noMatch1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(10001));
        assertTrue("Failed: " + e, e.evaluateBoolean(match));
    }

    public void testEvaluateGREATER_THAN_EQUAL_TO() throws Exception {
        Expression e =
            new ASTGreaterOrEqual(
                new ASTObjPath("estimatedPrice"),
                new BigDecimal(10000));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(9999));
        assertFalse(e.evaluateBoolean(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(10000));
        assertTrue(e.evaluateBoolean(match1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(10001));
        assertTrue("Failed: " + e, e.evaluateBoolean(match));
    }

    public void testEvaluateBETWEEN() throws Exception {
        // evaluate both BETWEEN and NOT_BETWEEN
        Expression between =
            new ASTBetween(
                new ASTObjPath("estimatedPrice"),
                new BigDecimal(10),
                new BigDecimal(20));
        Expression notBetween =
            new ASTNotBetween(
                new ASTObjPath("estimatedPrice"),
                new BigDecimal(10),
                new BigDecimal(20));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(21));
        assertFalse(between.evaluateBoolean(noMatch));
        assertTrue(notBetween.evaluateBoolean(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(20));
        assertTrue(between.evaluateBoolean(match1));
        assertFalse(notBetween.evaluateBoolean(match1));

        Painting match2 = new Painting();
        match2.setEstimatedPrice(new BigDecimal(10));
        assertTrue("Failed: " + between, between.evaluateBoolean(match2));
        assertFalse("Failed: " + notBetween, notBetween.evaluateBoolean(match2));

        Painting match3 = new Painting();
        match3.setEstimatedPrice(new BigDecimal(11));
        assertTrue("Failed: " + between, between.evaluateBoolean(match3));
        assertFalse("Failed: " + notBetween, notBetween.evaluateBoolean(match3));
    }

    public void testEvaluateIN() throws Exception {
        Expression in =
            new ASTIn(
                new ASTObjPath("estimatedPrice"),
                new ASTList(new Object[] { new BigDecimal(10), new BigDecimal(20)}));

        Expression notIn =
            new ASTNotIn(
                new ASTObjPath("estimatedPrice"),
                new ASTList(new Object[] { new BigDecimal(10), new BigDecimal(20)}));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(21));
        assertFalse(in.evaluateBoolean(noMatch1));
        assertTrue(notIn.evaluateBoolean(noMatch1));

        Painting noMatch2 = new Painting();
        noMatch2.setEstimatedPrice(new BigDecimal(11));
        assertFalse("Failed: " + in, in.evaluateBoolean(noMatch2));
        assertTrue("Failed: " + notIn, notIn.evaluateBoolean(noMatch2));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(20));
        assertTrue(in.evaluateBoolean(match1));
        assertFalse(notIn.evaluateBoolean(match1));

        Painting match2 = new Painting();
        match2.setEstimatedPrice(new BigDecimal(10));
        assertTrue("Failed: " + in, in.evaluateBoolean(match2));
        assertFalse("Failed: " + notIn, notIn.evaluateBoolean(match2));
    }

    public void testEvaluateLIKE1() throws Exception {
        Expression like = new ASTLike(new ASTObjPath("artistName"), "abc%d");
        Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc%d");

        Artist noMatch = new Artist();
        noMatch.setArtistName("dabc");
        assertFalse(like.evaluateBoolean(noMatch));
        assertTrue(notLike.evaluateBoolean(noMatch));

        Artist match1 = new Artist();
        match1.setArtistName("abc123d");
        assertTrue("Failed: " + like, like.evaluateBoolean(match1));
        assertFalse("Failed: " + notLike, notLike.evaluateBoolean(match1));

        Artist match2 = new Artist();
        match2.setArtistName("abcd");
        assertTrue("Failed: " + like, like.evaluateBoolean(match2));
        assertFalse("Failed: " + notLike, notLike.evaluateBoolean(match2));
    }

    public void testEvaluateLIKE2() throws Exception {
        Expression like = new ASTLike(new ASTObjPath("artistName"), "abc?d");
        Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc?d");

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("dabc");
        assertFalse(like.evaluateBoolean(noMatch1));
        assertTrue(notLike.evaluateBoolean(noMatch1));

        Artist noMatch2 = new Artist();
        noMatch2.setArtistName("abc123d");
        assertFalse("Failed: " + like, like.evaluateBoolean(noMatch2));
        assertTrue("Failed: " + notLike, notLike.evaluateBoolean(noMatch2));

        Artist match = new Artist();
        match.setArtistName("abcXd");
        assertTrue("Failed: " + like, like.evaluateBoolean(match));
        assertFalse("Failed: " + notLike, notLike.evaluateBoolean(match));
    }

    public void testEvaluateLIKE3() throws Exception {
        // test special chars
        Expression like = new ASTLike(new ASTObjPath("artistName"), "/./");

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("/a/");
        assertFalse(like.evaluateBoolean(noMatch1));

        Artist match = new Artist();
        match.setArtistName("/./");
        assertTrue("Failed: " + like, like.evaluateBoolean(match));
    }

    public void testEvaluateLIKE_IGNORE_CASE() throws Exception {
        Expression like = new ASTLikeIgnoreCase(new ASTObjPath("artistName"), "aBcD");
        Expression notLike =
            new ASTNotLikeIgnoreCase(new ASTObjPath("artistName"), "aBcD");

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("dabc");
        assertFalse(like.evaluateBoolean(noMatch1));
        assertTrue(notLike.evaluateBoolean(noMatch1));

        Artist match1 = new Artist();
        match1.setArtistName("abcd");
        assertTrue("Failed: " + like, like.evaluateBoolean(match1));
        assertFalse("Failed: " + notLike, notLike.evaluateBoolean(match1));

        Artist match2 = new Artist();
        match2.setArtistName("ABcD");
        assertTrue("Failed: " + like, like.evaluateBoolean(match2));
        assertFalse("Failed: " + notLike, notLike.evaluateBoolean(match2));
    }
}
