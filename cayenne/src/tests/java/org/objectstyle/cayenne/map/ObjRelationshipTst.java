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

import org.objectstyle.art.Artist;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.unittest.CayenneTestCase;


public class ObjRelationshipTst extends CayenneTestCase {
    protected ObjRelationship rel;
    protected DbEntity artistDBEntity = getDomain().getEntityResolver().lookupDbEntity(Artist.class);
	//There may not be an ObjEntity for Artist_exhibit... jump straight to the dbentity
    protected DbEntity artistExhibitDBEntity = getDomain().getMapForDbEntity("ARTIST_EXHIBIT").getDbEntity("ARTIST_EXHIBIT");
	protected DbEntity exhibitDBEntity = getDomain().getEntityResolver().lookupDbEntity(Exhibit.class);
	protected DbEntity paintingDbEntity = getDomain().getEntityResolver().lookupDbEntity(Painting.class);
	protected DbEntity galleryDBEntity = getDomain().getEntityResolver().lookupDbEntity(Gallery.class);
   
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
        DataDomain dom = getDomain();
        ObjEntity artistObjEnt = dom.getEntityResolver().lookupObjEntity("Artist");
        ObjEntity paintingObjEnt = dom.getEntityResolver().lookupObjEntity("Painting");
        
        // start with "to many"
        ObjRelationship r1 = (ObjRelationship)artistObjEnt.getRelationship("paintingArray");
        ObjRelationship r2 = r1.getReverseRelationship();
        
        assertNotNull(r2);
        assertSame(paintingObjEnt.getRelationship("toArtist"), r2);
    }
    
    public void testGetReverseRel2() throws Exception {
        DataDomain dom = getDomain();
        ObjEntity artistEnt = dom.getEntityResolver().lookupObjEntity("Artist");
        ObjEntity paintingEnt = dom.getEntityResolver().lookupObjEntity("Painting");
        
        // start with "to one"
        ObjRelationship r1 = (ObjRelationship)paintingEnt.getRelationship("toArtist");
        ObjRelationship r2 = r1.getReverseRelationship();
        
        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }
    
    public void testSingleDbRelationship() {
    	DbRelationship r1 = new DbRelationship();
    	rel.addDbRelationship(r1);
        assertEquals(1, rel.getDbRelationshipList().size());
        assertEquals(r1, rel.getDbRelationshipList().get(0));
        assertTrue(!rel.isFlattened());
        assertTrue(!rel.isReadOnly());

        rel.removeDbRelationship(r1);
        assertEquals(0, rel.getDbRelationshipList().size());
     }
    
    public void testFlattenedRelationship() throws Exception {
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();
		
		r1.setSourceEntity(artistDBEntity);
		r1.setTargetEntity(artistExhibitDBEntity);
		r1.setToMany(true);
		
		r2.setSourceEntity(artistExhibitDBEntity);
		r2.setTargetEntity(exhibitDBEntity);
		r2.setToMany(false);
		
        rel.addDbRelationship(r1);
        rel.addDbRelationship(r2);
        assertEquals(2, rel.getDbRelationshipList().size());
        assertEquals(r1, rel.getDbRelationshipList().get(0));
        assertEquals(r2, rel.getDbRelationshipList().get(1));
        
        assertTrue(rel.isFlattened());
        assertTrue(!rel.isReadOnly());
        
        rel.removeDbRelationship(r1);
        assertEquals(1, rel.getDbRelationshipList().size());
        assertEquals(r2, rel.getDbRelationshipList().get(0));
        assertTrue(!rel.isFlattened());
        assertTrue(!rel.isReadOnly());
    }
    
    public void testReadOnlyMoreThan3DbRelsRelationship() {
    	//Readonly is a flattened relationship that isn't over a single many->many link table
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();
        DbRelationship r3 = new DbRelationship();

 		r1.setSourceEntity(artistDBEntity);
		r1.setTargetEntity(artistExhibitDBEntity);
		r1.setToMany(true);
		r2.setSourceEntity(artistExhibitDBEntity);
		r2.setTargetEntity(exhibitDBEntity);
		r2.setToMany(false);
		r3.setSourceEntity(exhibitDBEntity);
		r3.setTargetEntity(galleryDBEntity);
		r3.setToMany(false);
		
        rel.addDbRelationship(r1);
        rel.addDbRelationship(r2);
        rel.addDbRelationship(r3);
        
 		assertTrue(rel.isFlattened());
        assertTrue(rel.isReadOnly());
  	
    }
    
    //Test for a read-only flattened relationship that is readonly because it's dbrel sequence is "incorrect" (or rather, unsupported)
    public void testIncorrectSequenceReadOnlyRelationship() {
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();

 		r1.setSourceEntity(artistDBEntity);
    	r1.setTargetEntity(paintingDbEntity);
    	r1.setToMany(true);
    	r2.setSourceEntity(paintingDbEntity);
    	r2.setTargetEntity(galleryDBEntity);
    	r2.setToMany(false);
    	
		rel.addDbRelationship(r1);
		rel.addDbRelationship(r2);
		
		assertTrue(rel.isFlattened());
		assertTrue(rel.isReadOnly());
    }
    
    //Test a relationship loaded from the test datamap that we know should be flattened
    public void testKnownFlattenedRelationship() {
        ObjEntity artistEnt = getDomain().getEntityResolver().lookupObjEntity("Artist");
     	ObjRelationship theRel=(ObjRelationship)artistEnt.getRelationship("groupArray");
     	assertNotNull(theRel);
     	assertTrue(theRel.isFlattened());
     	assertTrue(!theRel.isReadOnly());
    }
}
