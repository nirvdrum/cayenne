/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
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

    protected DataContext context;

    protected void setUp() throws Exception {
        context = createDataContext();
    }
    
    public void testTransactionEventsEnabled() {
        context.setTransactionEventsEnabled(false);
        assertFalse(context.isTransactionEventsEnabled());
        context.setTransactionEventsEnabled(true);
        assertTrue(context.isTransactionEventsEnabled());
    }

    public void testHasChangesNew() throws Exception {
        assertTrue("No changes expected in context", !context.hasChanges());
        context.createAndRegisterNewObject("Artist");
        assertTrue(
            "Object added to context, expected to report changes",
            context.hasChanges());
    }

    public void testCreateAndRegisterNewObject() throws Exception {
        Artist a1 = (Artist) context.createAndRegisterNewObject("Artist");
        assertTrue(context.getObjectStore().getObjects().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    public void testIdObjectFromDataRow() throws Exception {
		DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(100000));
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        assertNotNull(obj);
        assertTrue(context.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        
        assertNotNull(context.getObjectStore().getSnapshot(obj.getObjectId(), context));
    }

    public void testPartialObjectFromDataRow() throws Exception {
		DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(100001));
        row.put("ARTIST_NAME", "ArtistXYZ");
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        assertNotNull(obj);
        assertTrue(context.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
		assertNotNull(context.getObjectStore().getSnapshot(obj.getObjectId(), context));
    }

    public void testFullObjectFromDataRow() throws Exception {
		DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(123456));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
		Artist obj = (Artist)context.objectFromDataRow(Artist.class, row, false);

        assertTrue(context.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertNotNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
        assertEquals("ArtistXYZ", obj.getArtistName());
    }

    public void testCommitChangesError() throws Exception {
        // can't run this test due to the nature of some adapters
        if(!getDatabaseSetupDelegate().supportsDroppingPK()) {
            return;
        }
        
        JdbcPkGenerator gen = (JdbcPkGenerator) getNode().getAdapter().getPkGenerator();
        int cache = gen.getPkCacheSize();

        // make sure we insert enough objects to exhaust the cache
        for (int i = 0; i < cache + 2; i++) {
            Artist o1 = new Artist();
            o1.setArtistName("a" + i);
            context.registerNewObject(o1);
        }

        // this should cause PK generation exception in commit later
        DataMap map = (DataMap) getNode().getDataMaps().iterator().next();

        gen.dropAutoPk(getNode(), new ArrayList(map.getDbEntities()));

        // disable logging for thrown exceptions
        Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);
        try {
            context.commitChanges(Level.DEBUG);
            fail("Exception expected but not thrown due to missing PK generation routine.");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
        }
        finally {
            observerLogger.setLevel(oldLevel);
            getDatabaseSetup().createPkSupportForMapEntities(getNode());
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
            context.performQueries(Collections.singletonList(q), new SelectObserver());
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
        EntityResolver er = context.getEntityResolver();
        assertNotNull(er);
    }
}