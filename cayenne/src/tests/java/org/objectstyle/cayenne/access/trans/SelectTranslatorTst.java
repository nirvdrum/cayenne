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
package org.objectstyle.cayenne.access.trans;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

public class SelectTranslatorTst extends CayenneTestCase {
    static Logger logObj = Logger.getLogger(SelectTranslatorTst.class.getName());

    protected SelectQuery q;
    protected DbEntity artistEnt;

    public SelectTranslatorTst(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        q = new SelectQuery();
        artistEnt = getSharedDomain().lookupEntity("Artist").getDbEntity();
    }

    private SelectTranslator buildTranslator(Connection con) throws Exception {
        SelectTranslator transl =
            (SelectTranslator) getSharedNode().getAdapter().getQueryTranslator(q);
        transl.setEngine(getSharedNode());
        transl.setCon(con);

        return transl;
    }

    /**
     * Tests query creation with qualifier and ordering.
     */
    public void testCreateSqlString1() throws Exception {
        Connection con = getSharedConnection();

        try {
            // query with qualifier and ordering
            q.setObjEntityName("Artist");
            q.setQualifier(
                ExpressionFactory.binaryExp(Expression.LIKE, "artistName", "a%"));
            q.addOrdering("dateOfBirth", Ordering.ASC);

            String generatedSql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));
            assertTrue(generatedSql.indexOf(" FROM ") > 0);
            assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));
            assertTrue(
                generatedSql.indexOf(" ORDER BY ") > generatedSql.indexOf(" WHERE "));
        } finally {
            con.close();
        }
    }

    /**
     * Tests query creation with "distinct" specified.
     */
    public void testCreateSqlString2() throws java.lang.Exception {
        Connection con = getSharedConnection();
        try {
            // query with "distinct" set
            q.setObjEntityName("Artist");
            q.setDistinct(true);

            String generatedSql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT DISTINCT"));
        } finally {
            con.close();
        }
    }

    /**
     * Tests query creation with relationship from derived entity.
     */
    public void testCreateSqlString3() throws Exception {
        ObjectId id = new ObjectId("Artist", "ARTIST_ID", 35);
        Artist a1 = (Artist) createDataContext().registeredObject(id);
        Connection con = getSharedConnection();

        try {
            // query with qualifier and ordering
            q.setObjEntityName("ArtistAssets");
            q.setQualifier(ExpressionFactory.matchExp("toArtist", a1));

            String sql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(sql);
            assertTrue(sql.startsWith("SELECT "));
            assertTrue(sql.indexOf(" FROM ") > 0);

            // no WHERE clause
            assertTrue(sql.indexOf(" WHERE ") < 0);

            assertTrue(sql.indexOf(" GROUP BY ") > 0);
            assertTrue(sql.indexOf("ARTIST_ID =") > 0);
            assertTrue(sql.indexOf("ARTIST_ID =") > sql.indexOf(" GROUP BY "));
        } finally {
            con.close();
        }
    }

    /**
     * Tests query creation with relationship from derived entity.
     */
    public void testCreateSqlString4() throws Exception {
        Connection con = getSharedConnection();

        try {
            // query with qualifier and ordering
            q.setObjEntityName("ArtistAssets");
            q.setParentObjEntityName("Painting");
            q.setParentQualifier(
                ExpressionFactory.matchExp("toArtist.artistName", "abc"));
            q.setQualifier(
                ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(3)));

            String sql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(sql);
            assertTrue(sql.startsWith("SELECT "));
            assertTrue(sql.indexOf(" FROM ") > 0);

            // no WHERE clause
            assertTrue("WHERE clause is expected: " + sql, sql.indexOf(" WHERE ") > 0);

            assertTrue(
                "GROUP BY clause is expected:" + sql,
                sql.indexOf(" GROUP BY ") > 0);
            assertTrue("HAVING clause is expected", sql.indexOf(" HAVING ") > 0);
            assertTrue(sql.indexOf("ARTIST_ID =") > 0);
            assertTrue(
                "Relationship join must be in WHERE: " + sql,
                sql.indexOf("ARTIST_ID =") > sql.indexOf(" WHERE "));
            assertTrue(
                "Relationship join must be in WHERE: " + sql,
                sql.indexOf("ARTIST_ID =") < sql.indexOf(" GROUP BY "));
            assertTrue(
                "Qualifier for related entity must be in WHERE: " + sql,
                sql.indexOf("ARTIST_NAME") > sql.indexOf(" WHERE "));
            assertTrue(
                "Qualifier for related entity must be in WHERE: " + sql,
                sql.indexOf("ARTIST_NAME") < sql.indexOf(" GROUP BY "));
        } finally {
            con.close();
        }
    }

    /**
     * Test aliases when the same table used in more then 1 relationship.
     * Check translation of relationship path "ArtistExhibit.toArtist.artistName"
     * and "ArtistExhibit.toExhibit.toGallery.paintingArray.toArtist.artistName".
     */
    public void testCreateSqlString5() throws Exception {
        Connection con = getSharedConnection();

        try {
            // query with qualifier and ordering
            q.setObjEntityName("ArtistExhibit");
            q.setQualifier(
                ExpressionFactory.binaryPathExp(
                    Expression.LIKE,
                    "toArtist.artistName",
                    "a%"));
            q.andQualifier(
                ExpressionFactory.binaryPathExp(
                    Expression.LIKE,
                    "toExhibit.toGallery.paintingArray.toArtist.artistName",
                    "a%"));

            SelectTranslator transl = buildTranslator(con);
            String generatedSql = transl.createSqlString();
            // logObj.warn("Query: " + generatedSql);

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));
            assertTrue(generatedSql.indexOf(" FROM ") > 0);
            assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));

            // check that there are 2 distinct aliases for the ARTIST table
            int ind1 = generatedSql.indexOf("ARTIST t", generatedSql.indexOf(" FROM "));
            assertTrue(ind1 > 0);

            int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
            assertTrue(ind2 > 0);

            assertTrue(
                generatedSql.charAt(ind1 + "ARTIST t".length())
                    != generatedSql.charAt(ind2 + "ARTIST t".length()));

        } finally {
            con.close();
        }
    }

    /**
     * Test aliases when the same table used in more then 1 relationship.
     * Check translation of relationship path "ArtistExhibit.toArtist.artistName"
     * and "ArtistExhibit.toArtist.paintingArray.paintingTitle".
     */
    public void testCreateSqlString6() throws Exception {
        Connection con = getSharedConnection();

        try {
            // query with qualifier and ordering
            q.setObjEntityName("ArtistExhibit");
            q.setQualifier(
                ExpressionFactory.binaryPathExp(
                    Expression.LIKE,
                    "toArtist.artistName",
                    "a%"));
            q.andQualifier(
                ExpressionFactory.binaryPathExp(
                    Expression.LIKE,
                    "toArtist.paintingArray.paintingTitle",
                    "p%"));

            SelectTranslator transl = buildTranslator(con);
            String generatedSql = transl.createSqlString();
            // logObj.warn("Query: " + generatedSql);

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));
            assertTrue(generatedSql.indexOf(" FROM ") > 0);
            assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));

            // check that there is only one distinct alias for the ARTIST table
            int ind1 = generatedSql.indexOf("ARTIST t", generatedSql.indexOf(" FROM "));
            assertTrue(ind1 > 0);

            int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
            assertTrue(ind2 < 0);
        } finally {
            con.close();
        }
    }


    public void testBuildColumnList1() throws Exception {
        Connection con = getSharedConnection();

        try {
            // configure query with entity that maps one-to-one to DbEntity
            q.setObjEntityName("Artist");
            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumnList();
            List dbAttrs = artistEnt.getAttributeList();

            assertEquals(dbAttrs.size(), columns.size());
            Iterator it = dbAttrs.iterator();
            while (it.hasNext()) {
                assertTrue(columns.contains(it.next()));
            }

        } finally {
            con.close();
        }
    }

    public void testBuildColumnList2() throws Exception {
        Connection con = getSharedConnection();

        try {
            // configure query with custom attributes
            q.setObjEntityName("Artist");
            q.addCustDbAttribute("ARTIST_ID");

            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumnList();
            Object[] dbAttrs = new Object[] { artistEnt.getAttribute("ARTIST_ID")};

            assertEquals(dbAttrs.length, columns.size());
            for (int i = 0; i < dbAttrs.length; i++) {
                assertTrue(columns.contains(dbAttrs[i]));
            }

        } finally {
            con.close();
        }
    }

    public void testBuildColumnList3() throws Exception {
        Connection con = getSharedConnection();

        try {
            // configure query with entity that maps to a subset of DbEntity
            q.setObjEntityName("SubPainting");
            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumnList();

            ObjEntity subPainting = getSharedDomain().lookupEntity("SubPainting");

            // assert that the number of attributes in the query is right
            // 1 (obj attr) + 1 (pk) = 2 
            assertEquals(2, columns.size());

        } finally {
            con.close();
        }
    }

    public void testBuildColumnList4() throws Exception {
        Connection con = getSharedConnection();

        try {
            // configure query with derived entity that maps to a subset of DbEntity
            q.setObjEntityName("ArtistPaintingCounts");
            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumnList();

            ObjEntity countsEnt = getSharedDomain().lookupEntity("ArtistPaintingCounts");

            // assert that the number of attributes in the query is right
            // 1 (obj attr) + 1 (pk) = 2 
            assertEquals(2, columns.size());

        } finally {
            con.close();
        }
    }
}