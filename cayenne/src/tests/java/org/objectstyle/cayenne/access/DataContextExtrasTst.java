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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.query.SqlSelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/** 
 * "Lightweight" test cases for DataContext. These
 * tests do not require any additional database setup.
 * 
 * @author Andrei Adamchik
 */
public class DataContextExtrasTst extends CayenneTestCase {

    protected DataContext ctxt;

    protected void setUp() throws Exception {
        ctxt = createDataContext();
    }
    
    public void testTransactionEventsEnabled() {
        ctxt.setTransactionEventsEnabled(false);
        assertFalse(ctxt.isTransactionEventsEnabled());
        ctxt.setTransactionEventsEnabled(true);
        assertTrue(ctxt.isTransactionEventsEnabled());
    }

    public void testHasChangesNew() throws Exception {
        assertTrue("No changes expected in context", !ctxt.hasChanges());
        ctxt.createAndRegisterNewObject("Artist");
        assertTrue(
            "Object added to context, expected to report changes",
            ctxt.hasChanges());
    }

    public void testCreateAndRegisterNewObject() throws Exception {
        Artist a1 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        assertTrue(ctxt.getObjectStore().getObjects().contains(a1));
        assertTrue(ctxt.newObjects().contains(a1));
    }

    public void testIdObjectFromDataRow() throws Exception {
        Map row = new HashMap();
        row.put("ARTIST_ID", new Integer(1));
        DataObject obj = ctxt.objectFromDataRow("Artist", row);
        assertNotNull(obj);

        // TODO
        assertTrue(ctxt.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertNull(obj.getCommittedSnapshot());
    }

    public void testPartialObjectFromDataRow() throws Exception {
        Map row = new HashMap();
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        DataObject obj = ctxt.objectFromDataRow("Artist", row);
        assertNotNull(obj);

        // TODO
        assertTrue(ctxt.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertNull(obj.getCommittedSnapshot());
    }

    public void testFullObjectFromDataRow() throws Exception {
        Map row = new HashMap();
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = ctxt.objectFromDataRow("Artist", row);

        assertTrue(ctxt.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertNotNull(ctxt.getObjectStore().getSnapshot(obj.getObjectId()));
    }

    public void testCommitChangesError() throws Exception {
        JdbcPkGenerator gen = (JdbcPkGenerator) getNode().getAdapter().getPkGenerator();
        int cache = gen.getPkCacheSize();

        // make sure we insert enough objects to exhaust the cache
        for (int i = 0; i < cache + 2; i++) {
            Artist o1 = new Artist();
            o1.setArtistName("a" + i);
            ctxt.registerNewObject(o1);
        }

        // this should cause PK generation exception in commit later
        DataMap map = (DataMap) getNode().getDataMaps().iterator().next();

        gen.dropAutoPk(getNode(), new ArrayList(map.getDbEntities()));

        // disable logging for thrown exceptions
        Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);
        try {
            ctxt.commitChanges(Level.DEBUG);
            fail("Exception expected but not thrown due to missing PK generation routine.");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
        }
        finally {
            observerLogger.setLevel(oldLevel);
            gen.createAutoPk(getNode(), new ArrayList(map.getDbEntities()));
        }
    }

    /** 
     * Testing behavior of Cayenne when a database exception
     * is thrown in SELECT query.
     */
    public void testSelectException() throws Exception {
        SqlSelectQuery q =
            new SqlSelectQuery("Artist", "SELECT * FROM NON_EXISTENT_TABLE");

        // disable logging for thrown exceptions
        Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);
        try {
            ctxt.performQueries(Collections.singletonList(q), new SelectObserver());
            fail("Query was invalid and was supposed to fail.");
        }
        catch (RuntimeException ex) {
            // exception expected
        }
        finally {
            observerLogger.setLevel(oldLevel);
        }
    }

    public void testEntityResolver() {
        EntityResolver er = ctxt.getEntityResolver();
        assertNotNull(er);
    }
}