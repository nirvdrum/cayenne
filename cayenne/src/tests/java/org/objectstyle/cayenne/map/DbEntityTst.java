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

import java.util.List;

import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class DbEntityTst extends CayenneTestCase {
    protected DbEntity ent;

    public DbEntityTst(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        ent = new DbEntity();
    }

    public void testConstructor1() throws Exception {
    	ent = new DbEntity();
    	assertNull(ent.getName());
    }
    
    public void testConstructor2() throws Exception {
    	ent = new DbEntity("abc");
    	assertEquals("abc", ent.getName());
    }
    
    public void testCatalog() throws Exception {
        String tstName = "tst_name";
        ent.setCatalog(tstName);
        assertEquals(tstName, ent.getCatalog());
    }

    public void testSchema() throws Exception {
        String tstName = "tst_name";
        ent.setSchema(tstName);
        assertEquals(tstName, ent.getSchema());
    }

    public void testFullyQualifiedName() throws Exception {
        String tstName = "tst_name";
        String schemaName = "tst_schema_name";
        ent.setName(tstName);

        assertEquals(tstName, ent.getName());
        assertEquals(tstName, ent.getFullyQualifiedName());

        ent.setSchema(schemaName);

        assertEquals(tstName, ent.getName());
        assertEquals(schemaName + "." + tstName, ent.getFullyQualifiedName());
    }

    public void testGetPrimaryKey() throws Exception {
        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        DbAttribute a2 = new DbAttribute();
        a2.setName("a2");
        a2.setPrimaryKey(true);
        ent.addAttribute(a2);

        List pk = ent.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertSame(a2, pk.get(0));
    }

    public void testRemovAttribute() throws Exception {
        DataMap map = new DataMap("map");
        ent.setName("ent");
        map.addDbEntity(ent);
       
        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        DbEntity otherEntity = new DbEntity("22ent1");
        assertNotNull(otherEntity.getName());
        map.addDbEntity(otherEntity);
        DbAttribute a11 = new DbAttribute();
        a11.setName("a11");
        a11.setPrimaryKey(false);
        otherEntity.addAttribute(a11);

        DbRelationship rel = new DbRelationship("relfrom");
        ent.addRelationship(rel);
        rel.setTargetEntity(otherEntity);
        rel.addJoin(new DbAttributePair(a1, a11));

        DbRelationship rel1 = new DbRelationship("relto");
        otherEntity.addRelationship(rel1);
        rel1.setTargetEntity(ent);
        rel1.addJoin(new DbAttributePair(a11, a1));

        // check that the test case is working
        assertSame(a1, ent.getAttribute(a1.getName()));
        assertSame(rel, ent.getRelationship(rel.getName()));

        // test removal
        ent.removeAttribute(a1.getName());

        assertNull(ent.getAttribute(a1.getName()));
        assertEquals(0, rel1.getJoins().size());
        assertEquals(0, rel.getJoins().size());
    }
}
