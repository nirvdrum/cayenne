package org.objectstyle.cayenne.wocompat;
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

import java.io.PrintWriter;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class EOModelProcessorTst extends CayenneTestCase {
    private static final PrintWriter out = new NullPrintWriter();

    protected EOModelProcessor processor;

    public EOModelProcessorTst(String name) {
        super(name);
    }

    public void setUp() throws java.lang.Exception {
        processor = new EOModelProcessor();
    }

    public void testLoadModel() throws Exception {
        DataMap map = processor.loadEOModel("test-resources/art.eomodeld");
        assertLoaded(map);
    }

    protected void assertLoaded(DataMap map) throws Exception {
        assertNotNull(map);
        assertEquals("art", map.getName());

        // check obj entities
        ObjEntity artistE = map.getObjEntity("Artist");
        assertNotNull(artistE);
        assertEquals("Artist", artistE.getName());

        // check Db entities
        DbEntity artistDE = map.getDbEntity("ARTIST");
        DbEntity artistDE1 = artistE.getDbEntity();
        assertSame(artistDE, artistDE1);

        // check attributes
        ObjAttribute a1 = (ObjAttribute)artistE.getAttribute("artistName");
        assertNotNull(a1);
        
        DbAttribute da1 =  a1.getDbAttribute();
        assertNotNull(da1);
        assertSame(da1, artistDE.getAttribute("ARTIST_NAME"));
        
        
        // check ObjRelationships
        ObjRelationship rel =
            (ObjRelationship) artistE.getRelationship("artistExhibitArray");
        assertNotNull(rel);
        assertEquals(1, rel.getDbRelationshipList().size());
        
        // check DbRelationships
        DbRelationship drel =
            (DbRelationship) artistDE.getRelationship("artistExhibitArray");
        assertNotNull(drel);
        assertSame(drel, rel.getDbRelationshipList().get(0));

        // flattened relationships
        ObjRelationship frel =
            (ObjRelationship) artistE.getRelationship("exhibitArray");
        assertNotNull(frel);
        assertEquals(2, frel.getDbRelationshipList().size());
        
        
        // storing data map may uncover some inconsistencies
        MapLoader loader = new MapLoader();
        loader.storeDataMap(out, map);
    }

    static class NullPrintWriter extends PrintWriter {
        public NullPrintWriter() {
            super(System.out);
        }

        public void close() {}
        public void flush() {}

        public void write(char[] arg0, int arg1, int arg2) {}
        public void write(char[] arg0) {}
        public void write(int arg0) {}
        public void write(String arg0, int arg1, int arg2) {}
        public void write(String arg0) {}
    }
}