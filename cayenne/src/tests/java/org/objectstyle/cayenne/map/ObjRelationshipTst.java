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
package org.objectstyle.cayenne.map;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.unittest.CayenneTestCase;


public class ObjRelationshipTst extends CayenneTestCase {
    protected ObjRelationship rel;
    
    public ObjRelationshipTst(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        rel = new ObjRelationship();
    }
    
    public void testTargetEntity() throws Exception {
    	rel.setTargetEntityName("targ");
    	assertNull(rel.getTargetEntity());
    	
    	ObjEntity src = new ObjEntity("src");
    	src.setClassName("src");
    	src.addRelationship(rel);
    	assertNull(rel.getTargetEntity());
    	
    	DataMap map = new DataMap();
    	map.addObjEntity(src);
    	assertNull(rel.getTargetEntity());
    	
    	ObjEntity targ = new ObjEntity("targ");
    	targ.setClassName("targ");
    	map.addObjEntity(targ);
    	rel.setTargetEntity(targ);
    	assertSame(targ, rel.getTargetEntity());
    }
    
    public void testGetReverseRel1() throws Exception {
        DataDomain dom = getSharedDomain();
        ObjEntity artistEnt = dom.getEntityResolver().lookupObjEntity("Artist");
        ObjEntity paintingEnt = dom.getEntityResolver().lookupObjEntity("Painting");
        
        // start with "to many"
        ObjRelationship r1 = (ObjRelationship)artistEnt.getRelationship("paintingArray");
        ObjRelationship r2 = r1.getReverseRelationship();
        
        assertNotNull(r2);
        assertSame(paintingEnt.getRelationship("toArtist"), r2);
    }
    
    public void testGetReverseRel2() throws Exception {
        DataDomain dom = getSharedDomain();
        ObjEntity artistEnt = dom.getEntityResolver().lookupObjEntity("Artist");
        ObjEntity paintingEnt = dom.getEntityResolver().lookupObjEntity("Painting");
        
        // start with "to one"
        ObjRelationship r1 = (ObjRelationship)paintingEnt.getRelationship("toArtist");
        ObjRelationship r2 = r1.getReverseRelationship();
        
        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }
    
    
    public void testDbRelationship() throws Exception {
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();

        rel.addDbRelationship(r1);
        rel.addDbRelationship(r2);
        assertEquals(2, rel.getDbRelationshipList().size());
        assertEquals(r1, rel.getDbRelationshipList().get(0));
        assertEquals(r2, rel.getDbRelationshipList().get(1));
        
        rel.removeDbRelationship(r1);
        assertEquals(1, rel.getDbRelationshipList().size());
        assertEquals(r2, rel.getDbRelationshipList().get(0));
    }
}
