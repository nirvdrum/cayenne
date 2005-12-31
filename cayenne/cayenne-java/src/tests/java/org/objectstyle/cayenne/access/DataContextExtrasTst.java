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

package org.objectstyle.cayenne.access;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * "Lightweight" test cases for DataContext. These tests do not require any additional
 * database setup.
 * 
 * @author Andrei Adamchik
 */
public class DataContextExtrasTst extends CayenneTestCase {

    public void testUserPropertiesLazyInit() {
        DataContext context = createDataContext();
        assertNull(context.userProperties);

        Map properties = context.getUserProperties();
        assertNotNull(properties);
        assertSame(properties, context.getUserProperties());
    }

    public void testUserProperties() {
        DataContext context = createDataContext();

        assertNull(context.getUserProperty("ABC"));
        Object object = new Object();

        context.setUserProperty("ABC", object);
        assertSame(object, context.getUserProperty("ABC"));
    }

    public void testTransactionEventsEnabled() {
        DataContext context = createDataContext();
        context.setTransactionEventsEnabled(false);
        assertFalse(context.isTransactionEventsEnabled());
        context.setTransactionEventsEnabled(true);
        assertTrue(context.isTransactionEventsEnabled());
    }

    public void testHasChangesNew() {
        DataContext context = createDataContext();
        assertTrue("No changes expected in context", !context.hasChanges());
        context.createAndRegisterNewObject("Artist");
        assertTrue("Object added to context, expected to report changes", context
                .hasChanges());
    }

    public void testCreateAndRegisterNewObject() {
        DataContext context = createDataContext();
        Artist a1 = (Artist) context.createAndRegisterNewObject("Artist");
        assertTrue(context.getObjectStore().getObjects().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    public void testCreateAndRegisterNewObjectWithClass() {
        DataContext context = createDataContext();
        Artist a1 = (Artist) context.createAndRegisterNewObject(Artist.class);
        assertTrue(context.getObjectStore().getObjects().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    public void testIdObjectFromDataRow() {
        DataContext context = createDataContext();
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(100000));
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        assertNotNull(obj);
        assertTrue(context.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());

        assertNotNull(context.getObjectStore().getSnapshot(
                obj.getObjectId(),
                context.getChannel()));
    }

    public void testPartialObjectFromDataRow() {
        DataContext context = createDataContext();
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(100001));
        row.put("ARTIST_NAME", "ArtistXYZ");
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        assertNotNull(obj);
        assertTrue(context.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertNotNull(context.getObjectStore().getSnapshot(
                obj.getObjectId(),
                context.getChannel()));
    }

    public void testFullObjectFromDataRow() {
        DataContext context = createDataContext();
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(123456));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        Artist obj = (Artist) context.objectFromDataRow(Artist.class, row, false);

        assertTrue(context.getObjectStore().getObjects().contains(obj));
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertNotNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
        assertEquals("ArtistXYZ", obj.getArtistName());
    }

    public void testCommitChangesError() {
        DataContext context = createDataContext();

        // setup mockup PK generator that will blow on PK request
        // to emulate an exception
        PkGenerator newGenerator = new JdbcPkGenerator() {

            public Object generatePkForDbEntity(DataNode node, DbEntity ent)
                    throws Exception {
                throw new CayenneRuntimeException("Synthetic error....");
            }
        };

        PkGenerator oldGenerator = getNode().getAdapter().getPkGenerator();
        JdbcAdapter adapter = (JdbcAdapter) getNode().getAdapter();

        adapter.setPkGenerator(newGenerator);
        try {
            Artist newArtist = (Artist) context.createAndRegisterNewObject(Artist.class);
            newArtist.setArtistName("aaa");
            context.commitChanges();
            fail("Exception expected but not thrown due to missing PK generation routine.");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
        }
        finally {
            adapter.setPkGenerator(oldGenerator);
        }
    }

    /**
     * Testing behavior of Cayenne when a database exception is thrown in SELECT query.
     */
    public void testSelectException() {
        DataContext context = createDataContext();

        SQLTemplate q = new SQLTemplate(Artist.class, "SELECT * FROM NON_EXISTENT_TABLE");

        // disable logging for thrown exceptions
        Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);
        try {
            context.performQueries(Collections.singletonList(q), new QueryResult());
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
        DataContext context = createDataContext();
        assertNotNull(context.getEntityResolver());
    }

    public void testValidatePhantomModifications() throws Exception {
        deleteTestData();
        createTestData("testValidatePhantomModifications");
        DataContext context = createDataContext();

        List objects = context.performQuery(new SelectQuery(Artist.class));
        Artist a1 = (Artist) objects.get(0);
        Artist a2 = (Artist) objects.get(1);

        a1.setArtistName(a1.getArtistName());
        a1.resetValidationFlags();
        a2.resetValidationFlags();
        context.commitChanges();

        assertFalse(a1.isValidateForSaveCalled());
        assertFalse(a2.isValidateForSaveCalled());

        // "phantom" modification - the property is really unchanged
        a1.setArtistName(a1.getArtistName());

        // some other unrelated object modification caused phantom modification to be
        // committed as well...
        // (see CAY-355)
        a2.setArtistName(a2.getArtistName() + "_x");

        a1.resetValidationFlags();
        a2.resetValidationFlags();
        context.commitChanges();

        assertTrue(a2.isValidateForSaveCalled());
        assertFalse(a1.isValidateForSaveCalled());
    }
}