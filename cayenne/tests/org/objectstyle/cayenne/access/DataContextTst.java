package org.objectstyle.cayenne.access;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.objectstyle.TestMain;
import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.TestOperationObserver;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class DataContextTst extends TestCase {
    static Logger logObj = Logger.getLogger(DataContextTst.class.getName());

    private static final int _artistCount = 25;
    private static final int _galleryCount = 10;

    protected DataContext ctxt;
    protected TestOperationObserver opObserver;

    public DataContextTst(String name) {
        super(name);
    }

    protected void setUp() throws java.lang.Exception {
        super.setUp();
        TestMain.getSharedDatabaseSetup().cleanTableData();
        populateTables();

        DataDomain dom = TestMain.getSharedDomain();
        dom.getDataNodes()[0].createPkSupportForMapEntities();

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
        ctxt.mergeObjectWithSnapshot(a1, s2);

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
        assertNotNull(ctxt.lookupEntity("Artist"));
        assertNull(ctxt.lookupEntity("NonExistent"));
    }

   public void testPrefetch1() throws java.lang.Exception {
        SelectQuery q = new SelectQuery("Artist");
        q.addPrefetch("paintingArray");
        
        SelectOperationObserver o = new SelectOperationObserver();
        o.
        ctxt.performQuery(q, o);
        assertEquals(2, o.getSelectCount());
    }
    
    
    public void testPerformQueries() throws Exception {
        SelectQuery q1 = new SelectQuery();
        q1.setObjEntityName("Artist");
        SelectQuery q2 = new SelectQuery();
        q2.setObjEntityName("Gallery");

        ArrayList qs = new ArrayList();
        qs.add(q1);
        qs.add(q2);
        ctxt.performQueries(qs, opObserver);

        // check query results
        ArrayList o1 = opObserver.objectsForQuery(q1);
        assertNotNull(o1);
        assertEquals(_artistCount, o1.size());

        ArrayList o2 = opObserver.objectsForQuery(q2);
        assertNotNull(o2);
        assertEquals(_galleryCount, o2.size());
    }

    public void testPerformSelectQuery() throws Exception {
        SelectQuery query = new SelectQuery();
        query.setObjEntityName("Artist");

        // check query results
        List objects = ctxt.performQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testPerformQuery() throws Exception {
        SelectQuery query = new SelectQuery();
        query.setObjEntityName("Artist");

        ctxt.performQuery(query, opObserver);

        // check query results
        ArrayList objects = opObserver.objectsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    private Artist fetchArtist(String name) {
        SelectQuery q =
            new SelectQuery(
                "Artist",
                ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", name));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

    protected void populateTables() throws java.lang.Exception {
        String insertArtist =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";

        Connection conn = TestMain.getSharedConnection();

        try {
            conn.setAutoCommit(false);
            
            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            for (int i = 1; i <= _artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "artist" + i);
                stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();

            String insertGal =
                "INSERT INTO GALLERY (GALLERY_ID, GALLERY_NAME) VALUES (?,?)";
            stmt = conn.prepareStatement(insertGal);

            for (int i = 1; i <= _galleryCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "gallery" + i);
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        }
        finally {
            conn.close();
        }
    }
}