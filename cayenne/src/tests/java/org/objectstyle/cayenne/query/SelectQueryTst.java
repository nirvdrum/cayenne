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
package org.objectstyle.cayenne.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;

public class SelectQueryTst extends SelectQueryBase {
    private static final int _artistCount = 20;

    public SelectQueryTst(String name) {
        super(name);
    }

    public void testFetchLimit() throws java.lang.Exception {
        query.setRoot(Artist.class);
        query.setFetchLimit(7);
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(7, objects.size());
    }

    public void testSelectAllObjectsRootEntityName()
        throws java.lang.Exception {
        query.setRoot(Artist.class);
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectAllObjectsRootClass() throws java.lang.Exception {
        query.setRoot(Artist.class);
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectAllObjectsRootObjEntity()
        throws java.lang.Exception {
        //Crude technique to obtain the Artist ObjEntity, but it works
        query.setRoot(
            this.getDomain().getEntityResolver().lookupObjEntity(Artist.class));
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectLike() throws Exception {
        query.setRoot(Artist.class);
        Expression qual =
            ExpressionFactory.binaryPathExp(
                Expression.LIKE,
                "artistName",
                "artist11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(1, objects.size());
    }

    /** Test how "like ignore case" works when using uppercase parameter. */
    public void testSelectLikeIgnoreCaseObjects1() throws Exception {
        query.setRoot(Artist.class);
        Expression qual =
            ExpressionFactory.binaryPathExp(
                Expression.LIKE_IGNORE_CASE,
                "artistName",
                "ARTIST%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    /** Test how "like ignore case" works when using lowercase parameter. */
    public void testSelectLikeIgnoreCaseObjects2() throws Exception {
        query.setRoot(Artist.class);
        Expression qual =
            ExpressionFactory.binaryPathExp(
                Expression.LIKE_IGNORE_CASE,
                "artistName",
                "artist%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectCustAttributes() throws java.lang.Exception {
        query.setRoot(Artist.class);
        query.addCustDbAttribute("ARTIST_NAME");

        List results = getDomain().createDataContext().performQuery(query);

        // check query results
        assertEquals(_artistCount, results.size());

        Map row = (Map) results.get(0);
        assertNotNull(row.get("ARTIST_NAME"));
        assertEquals(1, row.size());
    }

    protected void populateTables() throws java.lang.Exception {
        String insertArtist =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";
        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            for (int i = 1; i <= _artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "artist" + i);
                stmt.setDate(
                    3,
                    new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        } finally {
            conn.close();
        }
    }
}