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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.art.ROArtist;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.TestOperationObserver;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;

/**
 * @author Andrei Adamchik
 */
public class DataContextTestBase extends CayenneTestCase {

    public static final int artistCount = 25;
    public static final int galleryCount = 10;

    protected DataContext context;
    protected TestOperationObserver opObserver;

    protected void setUp() throws java.lang.Exception {
        super.setUp();

        CayenneTestDatabaseSetup setup = getDatabaseSetup();
        setup.cleanTableData();
        populateTables();

        DataDomain dom = getDomain();
        setup.createPkSupportForMapEntities(
            (DataNode) dom.getDataNodes().iterator().next());

        context = createDataContext();
        opObserver = new TestOperationObserver();
    }

    // TODO: deprecate me
    public String artistName(int ind) {
        return artistName(ind, false);
    }

    public String artistName(int ind, boolean padToConstWidth) {
        String prefix = (padToConstWidth && ind < 10) ? "artist0" : "artist";
        return prefix + ind;
    }

    public String galleryName(int ind, boolean padToConstWidth) {
        String prefix = (padToConstWidth && ind < 10) ? "gallery0" : "gallery";
        return prefix + ind;
    }

    protected Painting fetchPainting(String name, boolean prefetchArtist) {
        SelectQuery select =
            new SelectQuery(
                Painting.class,
                ExpressionFactory.matchExp("paintingTitle", name));
        if (prefetchArtist) {
            select.addPrefetch("toArtist");
        }

        List ats = context.performQuery(select);
        return (ats.size() > 0) ? (Painting) ats.get(0) : null;
    }

    protected Artist fetchArtist(String name, boolean prefetchPaintings) {
        SelectQuery q =
            new SelectQuery(Artist.class, ExpressionFactory.matchExp("artistName", name));
        if (prefetchPaintings) {
            q.addPrefetch("paintingArray");
        }
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

    protected ROArtist fetchROArtist(String name) {
        SelectQuery q =
            new SelectQuery(
                ROArtist.class,
                ExpressionFactory.matchExp("artistName", name));
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (ROArtist) ats.get(0) : null;
    }
    
    protected int safeId(int i) {
        // something in the range that we are unlikely to hit
        return 33000 + i;
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
                stmt.setInt(1, safeId(i));
                stmt.setString(2, "P_" + artistName(i));
                stmt.setInt(3, safeId(i));
                stmt.setBigDecimal(4, new BigDecimal(i * 1000));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        } finally {
            conn.close();
        }
    }

    // TODO: deprecate me
    public void populateTables() throws Exception {
        populateTables(false);
    }
    
    public void populateTables(boolean padToConstWidth) throws Exception {
        String insertArtist =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";

        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            for (int i = 1; i <= artistCount; i++) {
                // create ID's somewhere outside the range that we can reach
                stmt.setInt(1, safeId(i));
                stmt.setString(2, artistName(i, padToConstWidth));
                stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();

            String insertGal =
                "INSERT INTO GALLERY (GALLERY_ID, GALLERY_NAME) VALUES (?,?)";
            stmt = conn.prepareStatement(insertGal);

            for (int i = 1; i <= galleryCount; i++) {
                // create ID's somewhere outside the range that we can reach
                stmt.setInt(1, safeId(i));
                stmt.setString(2, galleryName(i, padToConstWidth));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        } finally {
            conn.close();
        }
    }

    /**
     * Helper method to update a single column in a database row.
     */
    protected void updateRow(ObjectId id, String dbAttribute, Object newValue) {

        UpdateQuery updateQuery = new UpdateQuery();
        updateQuery.setRoot(id.getObjClass());
        updateQuery.addUpdAttribute(dbAttribute, newValue);

        // set qualifier
        updateQuery.setQualifier(
            ExpressionFactory.matchAllDbExp(id.getIdSnapshot(), Expression.EQUAL_TO));

        getNode().performQueries(
            Collections.singletonList(updateQuery),
            new DefaultOperationObserver());
    }

    protected void deleteRow(ObjectId id) {
        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setRoot(id.getObjClass());
        deleteQuery.setQualifier(
            ExpressionFactory.matchAllDbExp(id.getIdSnapshot(), Expression.EQUAL_TO));
        getNode().performQueries(
            Collections.singletonList(deleteQuery),
            new DefaultOperationObserver());
    }

    /**
     * Helper method that takes one of the artists from the standard
     * dataset (always the same one) and creates a new painting for this artist,
     * committing it to the database. Both Painting and Artist will be cached in current
     * DataContext.
     */
    protected Painting insertPaintingInContext(String paintingName) {
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle(paintingName);
        painting.setToArtist(fetchArtist("artist2", false));

        context.commitChanges();

        return painting;
    }

    protected void insertPaintingBypassingContext(
        String paintingName,
        String artistName) {

        Artist artist = fetchArtist(artistName, false);

        Map snapshot = new HashMap();
        snapshot.put("ARTIST_ID", artist.getObjectId().getValueForAttribute("ARTIST_ID"));
        snapshot.put("PAINTING_TITLE", paintingName);

        ObjectId oid = new ObjectId(Painting.class, "PAINTING_ID", 10);
        InsertQuery ins = new InsertQuery();
        ins.setRoot(Painting.class);
        ins.setObjectSnapshot(snapshot);
        ins.setObjectId(oid);

        getNode().performQueries(
            Collections.singletonList(ins),
            new DefaultOperationObserver());
    }
}
