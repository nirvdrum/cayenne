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

import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.dba.postgres.PostgresAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class DbLoaderTst extends CayenneTestCase {
    protected DbLoader loader;

    public void setUp() throws Exception {
        loader = new DbLoader(getConnection(), getNode().getAdapter(), null);
    }

    /** 
     * DataMap loading is in one big test method, since breaking it in
     * individual tests would require multiple reads of metatdata 
     * which is extremely slow on some RDBMS (Sybase). 
     * 
     * <p>TODO: need to break to a bunch of private methods that test individual
     * aspects.</p>
     */
    public void testLoad() throws Exception {
        try {
            boolean supportsFK = getNode().getAdapter().supportsFkConstraints();

            boolean supportsLobs =
                super.getDatabaseSetupDelegate().supportsLobs();

            DataMap map = new DataMap();

            // *** TESTING THIS ***
            loader.loadDbEntities(
                map,
                loader.getTables(null, null, "%", new String[] { "TABLE" }));

            assertDbEntities(map);

            if (supportsLobs) {
                assertLobDbEntities(map);
            }

            if (supportsFK) {
                // *** TESTING THIS ***
                loader.loadDbRelationships(map);
                Collection rels = getDbEntity(map, "ARTIST").getRelationships();
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

            if (supportsLobs) {
                assertLobObjEntities(map);
            }

            if (supportsFK) {
                Collection rels = ae.getRelationships();
                assertNotNull(rels);
                assertTrue(rels.size() > 0);
            }

            // now when the map is loaded, test 
            // various things

            // selectively check how different types were processed
            checkTypes(map);
        } finally {
            loader.getCon().close();
        }
    }

    private void assertDbEntities(DataMap map) {
        DbEntity dae = getDbEntity(map, "ARTIST");
        assertNotNull(dae);
        assertEquals("ARTIST", dae.getName().toUpperCase());

        DbAttribute a = getDbAttribute(dae, "ARTIST_ID");
        assertNotNull(a);
        assertTrue(a.isPrimaryKey());
    }

    private void assertLobDbEntities(DataMap map) {
        DbEntity blobEnt = getDbEntity(map, "BLOB_TEST");
        assertNotNull(blobEnt);

        DbAttribute blobAttr = getDbAttribute(blobEnt, "BLOB_COL");
        assertNotNull(blobAttr);
        assertTrue(
            msgForTypeMismatch(Types.BLOB, blobAttr),
            Types.BLOB == blobAttr.getType()
                || Types.LONGVARBINARY == blobAttr.getType());

        DbEntity clobEnt = getDbEntity(map, "CLOB_TEST");
        assertNotNull(clobEnt);

        DbAttribute clobAttr = getDbAttribute(clobEnt, "CLOB_COL");
        assertNotNull(clobAttr);
        assertTrue(
            msgForTypeMismatch(Types.CLOB, clobAttr),
            Types.CLOB == clobAttr.getType()
                || Types.LONGVARCHAR == clobAttr.getType());
    }

    private void assertLobObjEntities(DataMap map) {
        ObjEntity blobEnt = map.getObjEntity("BlobTest");
        assertNotNull(blobEnt);

        // BLOBs should be mapped as byte[]
        ObjAttribute blobAttr = (ObjAttribute) blobEnt.getAttribute("blobCol");
        assertNotNull("BlobTest.blobCol failed to load", blobAttr);
        assertEquals("byte[]", blobAttr.getType());

        ObjEntity clobEnt = map.getObjEntity("ClobTest");
        assertNotNull(clobEnt);

        // CLOBs should be mapped as Strings by default
        ObjAttribute clobAttr = (ObjAttribute) clobEnt.getAttribute("clobCol");
        assertNotNull(clobAttr);
        assertEquals(String.class.getName(), clobAttr.getType());
    }

    private DbEntity getDbEntity(DataMap map, String name) {
        DbEntity de = map.getDbEntity(name);

        // sometimes table names get converted to lowercase
        if (de == null) {
            de = map.getDbEntity(name.toLowerCase());
        }

        return de;
    }

    private DbAttribute getDbAttribute(DbEntity ent, String name) {
        DbAttribute da = (DbAttribute) ent.getAttribute(name);

        // sometimes table names get converted to lowercase
        if (da == null) {
            da = (DbAttribute) ent.getAttribute(name.toLowerCase());
        }

        return da;
    }

    private DataMap originalMap() {
        return (DataMap)getNode().getDataMaps().iterator().next();
    }

    /** Selectively check how different types were processed. */
    public void checkTypes(DataMap map) {
        DbEntity dbe = getDbEntity(map, "PAINTING");
        DbEntity floatTest = getDbEntity(map, "FLOAT_TEST");

        DbAttribute integerAttr = getDbAttribute(dbe, "PAINTING_ID");
        DbAttribute decimalAttr = getDbAttribute(dbe, "ESTIMATED_PRICE");
        DbAttribute varcharAttr = getDbAttribute(dbe, "PAINTING_TITLE");
        DbAttribute floatAttr = getDbAttribute(floatTest, "FLOAT_COL");

        // check decimal
        // postgresql does not have a decimal type, instead columns that
        // are declared as DECIMAL will be converted to NUMERIC instead
        // which will be read as Types.NUMERIC when reengineering the
        // database. 
        DbAdapter adapter = this.getNode().getAdapter();
        if (adapter instanceof PostgresAdapter) {
            assertEquals(
                msgForTypeMismatch(Types.NUMERIC, decimalAttr),
                Types.NUMERIC,
                decimalAttr.getType());
        } else {
            assertEquals(
                msgForTypeMismatch(Types.DECIMAL, decimalAttr),
                Types.DECIMAL,
                decimalAttr.getType());

            assertEquals(2, decimalAttr.getPrecision());
        }

        // check varchar
        assertEquals(
            msgForTypeMismatch(Types.VARCHAR, varcharAttr),
            Types.VARCHAR,
            varcharAttr.getType());
        assertEquals(255, varcharAttr.getMaxLength());

        // check integer
        assertEquals(
            msgForTypeMismatch(Types.INTEGER, integerAttr),
            Types.INTEGER,
            integerAttr.getType());

        // check float
        assertTrue(
            msgForTypeMismatch(Types.FLOAT, floatAttr),
            Types.FLOAT == floatAttr.getType()
                || Types.DOUBLE == floatAttr.getType());
    }

    public void checkAllDBEntities(DataMap map) {
        Iterator entIt = originalMap().getDbEntities().iterator();
        while (entIt.hasNext()) {
            DbEntity origEnt = (DbEntity) entIt.next();
            DbEntity newEnt = map.getDbEntity(origEnt.getName());

            Iterator it = origEnt.getAttributes().iterator();
            while (it.hasNext()) {
                DbAttribute origAttr = (DbAttribute) it.next();
                DbAttribute newAttr =
                    (DbAttribute) newEnt.getAttribute(origAttr.getName());
                assertNotNull(
                    "No matching DbAttribute for '" + origAttr.getName(),
                    newAttr);
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

    private String msgForTypeMismatch(
        DbAttribute origAttr,
        DbAttribute newAttr) {
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