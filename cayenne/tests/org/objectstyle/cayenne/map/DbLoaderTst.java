package org.objectstyle.cayenne.map;
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

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.objectstyle.TestMain;
import org.objectstyle.cayenne.dba.TypesMapping;

public class DbLoaderTst extends TestCase {
    static Logger logObj = Logger.getLogger(DbLoaderTst.class.getName());

    protected DbLoader loader;

    public DbLoaderTst(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        loader = new DbLoader(TestMain.getSharedConnection());
    }

    /** 
     * DataMap loading is in one big test method, since breaking it in
     * individual tests would require multiple reads of metatdata 
     * which is extremely slow on some RDBMS (Sybase).
     * TODO: need to break to a bunch of private methods that test individual aspects. 
     */
    public void testLoad() throws Exception {
        boolean supportsFK =
            TestMain.getSharedNode().getAdapter().supportsFkConstraints();

        DataMap map = new DataMap();

        // *** TESTING THIS ***
        loader.loadDbEntities(
            map,
            loader.getTables(null, null, "%", new String[] { "TABLE" }));
        DbEntity dae = map.getDbEntity("ARTIST");

        // sometimes table names get converted to lowercase
        if (dae == null) {
            dae = map.getDbEntity("artist");
        }

        assertNotNull(dae);
        assertEquals("ARTIST", dae.getName().toUpperCase());
        assertTrue(((DbAttribute) dae.getAttribute("ARTIST_ID")).isPrimaryKey());

        if (supportsFK) {
            // *** TESTING THIS ***
            loader.loadDbRelationships(map);
            List rels = dae.getRelationshipList();
            assertNotNull(rels);
            assertTrue(rels.size() > 0);
        }

        // *** TESTING THIS ***
        loader.loadObjEntities(map);

        ObjEntity ae = map.getObjEntity("Artist");
        assertNotNull(ae);
        assertEquals("Artist", ae.getName());

        // assert primary key is not an attribute
        assertNull(ae.getAttribute("artistId"));

        if (supportsFK) {
            // *** TESTING THIS ***
            loader.loadObjRelationships(map);
            List rels = ae.getRelationshipList();
            assertNotNull(rels);
            assertTrue(rels.size() > 0);
        }

        // now when the map is loaded, test 
        // various things

        // selectively check how different types were processed
        checkTypes(map);
    }

    private DataMap originalMap() {
        return TestMain.getSharedNode().getDataMaps()[0];
    }

    /** Selectively check how different types were processed. */
    public void checkTypes(DataMap map) {
        DbEntity dbe = map.getDbEntity("PAINTING");
        
        // take into account a possibility of a lowercase names
        if(dbe == null) {
            dbe = map.getDbEntity("painting");
        }
        
        DbAttribute integerAttr = (DbAttribute) dbe.getAttribute("PAINTING_ID");
        DbAttribute decimalAttr = (DbAttribute) dbe.getAttribute("ESTIMATED_PRICE");
        DbAttribute varcharAttr = (DbAttribute) dbe.getAttribute("PAINTING_TITLE");

        // check decimal
        assertEquals(
            msgForTypeMismatch(Types.DECIMAL, decimalAttr),
            Types.DECIMAL,
            decimalAttr.getType());

        // check varchar
        assertEquals(
            msgForTypeMismatch(Types.VARCHAR, varcharAttr),
            Types.VARCHAR,
            varcharAttr.getType());
        
        // check integer
        assertEquals(
            msgForTypeMismatch(Types.INTEGER, integerAttr),
            Types.INTEGER,
            integerAttr.getType());
    }

    public void checkAllDBEntities(DataMap map) {
        Iterator entIt = originalMap().getDbEntitiesAsList().iterator();
        while (entIt.hasNext()) {
            DbEntity origEnt = (DbEntity) entIt.next();
            DbEntity newEnt = map.getDbEntity(origEnt.getName());

            Iterator it = origEnt.getAttributeList().iterator();
            while (it.hasNext()) {
                DbAttribute origAttr = (DbAttribute) it.next();
                DbAttribute newAttr = (DbAttribute) newEnt.getAttribute(origAttr.getName());
                assertNotNull("No matching DbAttribute for '" + origAttr.getName(), newAttr);
                assertEquals(
                    msgForTypeMismatch(origAttr, newAttr),
                    origAttr.getType(),
                    newAttr.getType());
                // length and precision doesn't have to be the same
                // it must be greater or equal
                assertTrue(origAttr.getMaxLength() <= newAttr.getMaxLength());
                assertTrue(origAttr.getPrecision() <= newAttr.getPrecision());
            }
        }
    }

    private String msgForTypeMismatch(DbAttribute origAttr, DbAttribute newAttr) {
        return msgForTypeMismatch(origAttr.getType(), newAttr);
    }

    private String msgForTypeMismatch(int origType, DbAttribute newAttr) {
        String nt = TypesMapping.getSqlNameByType(newAttr.getType());
        String ot = TypesMapping.getSqlNameByType(origType);
        return attrMismatch(
            newAttr.getName(),
            "expected type: <" + ot + ">, but was <" + nt + ">");
    }

    private String attrMismatch(String attrName, String msg) {
        StringBuffer buf = new StringBuffer();
        buf
            .append("[Error loading attribute '")
            .append(attrName)
            .append("': ")
            .append(msg)
            .append("]");
        return buf.toString();
    }
}