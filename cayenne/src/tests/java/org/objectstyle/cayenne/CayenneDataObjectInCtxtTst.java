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
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;

public class CayenneDataObjectInCtxtTst extends CayenneTestCase {
    protected DataContext ctxt;

    public void setUp() throws java.lang.Exception {
        CayenneTestDatabaseSetup setup = getDatabaseSetup();
        setup.cleanTableData();

        DataDomain dom = getDomain();
        setup.createPkSupportForMapEntities(
            (DataNode) dom.getDataNodes().iterator().next());

        ctxt = dom.createDataContext();
    }

    public void testCommitChangesInBatch() throws Exception {
        Artist a1 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a1.setArtistName("abc1");

        Artist a2 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a2.setArtistName("abc2");

        Artist a3 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a3.setArtistName("abc3");

        ctxt.commitChanges();

        List artists = ctxt.performQuery(new SelectQuery(Artist.class));
        assertEquals(3, artists.size());
    }

    public void testSetObjectId() throws Exception {
        CayenneDataObject o1 = new CayenneDataObject();
        assertNull(o1.getObjectId());

        ctxt.registerNewObject(o1, "Artist");
        assertNotNull(o1.getObjectId());
    }

    public void testStateTransToNew() throws Exception {
        Artist o1 = new Artist();
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());

        ctxt.registerNewObject(o1);
        assertEquals(PersistenceState.NEW, o1.getPersistenceState());
    }

    public void testStateNewToCommitted() throws Exception {
        Artist o1 = new Artist();
        o1.setArtistName("a");

        ctxt.registerNewObject(o1);
        assertEquals(PersistenceState.NEW, o1.getPersistenceState());

        ctxt.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    public void testStateCommittedToModified() throws Exception {
        Artist o1 = new Artist();
        o1.setArtistName("a");
        ctxt.registerNewObject(o1);
        ctxt.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());

        o1.setArtistName(o1.getArtistName() + "_1");
        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());
    }

    public void testStateModifiedToCommitted() throws Exception {
        Artist o1 = newSavedArtist();
        o1.setArtistName(o1.getArtistName() + "_1");
        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        ctxt.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    public void testStateCommittedToDeleted() throws Exception {
        Artist o1 = new Artist();
        o1.setArtistName("a");
        ctxt.registerNewObject(o1);
        ctxt.commitChanges();
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());

        ctxt.deleteObject(o1);
        assertEquals(PersistenceState.DELETED, o1.getPersistenceState());
    }

    public void testStateDeletedToTransient() throws Exception {
        Artist o1 = newSavedArtist();
        ctxt.deleteObject(o1);
        assertEquals(PersistenceState.DELETED, o1.getPersistenceState());

        ctxt.commitChanges();
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());
        assertFalse(ctxt.getObjectStore().getObjects().contains(o1));
        assertNull(o1.getDataContext());
    }

    public void testSetDataContext() throws Exception {
        CayenneDataObject o1 = new CayenneDataObject();
        assertNull(o1.getDataContext());

        ctxt.registerNewObject(o1, "Artist");
        assertSame(ctxt, o1.getDataContext());
    }

    public void testFetchByAttr() throws Exception {
        String artistName = "artist with one painting";
        TestCaseDataFactory.createArtistWithPainting(artistName, new String[] {
        }, false);

        SelectQuery q =
            new SelectQuery(
                "Artist",
                ExpressionFactory.binaryPathExp(
                    Expression.EQUAL_TO,
                    "artistName",
                    artistName));

        List artists = ctxt.performQuery(q);
        assertEquals(1, artists.size());
        Artist o1 = (Artist) artists.get(0);
        assertNotNull(o1);
        assertEquals(artistName, o1.getArtistName());
    }

    public void testUniquing() throws Exception {
        String artistName = "unique artist with no paintings";
        TestCaseDataFactory.createArtistWithPainting(artistName, new String[] {
        }, false);

        Artist a1 = fetchArtist(artistName);
        Artist a2 = fetchArtist(artistName);

        assertNotNull(a1);
        assertNotNull(a2);
        assertEquals(1, ctxt.getObjectStore().getObjects().size());
        assertSame(a1, a2);
    }

    private Artist newSavedArtist() {
        Artist o1 = new Artist();
        o1.setArtistName("a");
        o1.setDateOfBirth(new java.sql.Date(System.currentTimeMillis()));
        ctxt.registerNewObject(o1);
        ctxt.commitChanges();
        return o1;
    }

    private Artist fetchArtist(String name) {
        SelectQuery q =
            new SelectQuery(
                "Artist",
                ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", name));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

}