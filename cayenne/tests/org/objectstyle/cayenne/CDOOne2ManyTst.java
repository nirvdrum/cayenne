package org.objectstyle.cayenne;
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

import java.util.logging.Logger;

import org.objectstyle.art.*;

public class CDOOne2ManyTst extends CayenneDOTestBase {
    static Logger logObj = Logger.getLogger(CDOOne2ManyTst.class.getName());
    
    public CDOOne2ManyTst(String name) {
        super(name);
    }
    
    public void testNewAdd() throws Exception { 
        Artist a1 = newArtist();        
        Painting p1 = newPainting();
        
        // *** TESTING THIS *** 
        a1.addToPaintingArray(p1);
        
        // test before save
        assertSame(p1, a1.getPaintingArray().get(0));
        assertSame(a1, p1.getToArtist());
        
        // do save
        ctxt.commitChanges();
        resetContext();
	
        // test database data
        Artist a2 = fetchArtist();
        assertEquals(1, a2.getPaintingArray().size());
        assertEquals(paintingName, ((Painting)a2.getPaintingArray().get(0)).getPaintingTitle());
    }
    
    public void testNewAddMultiples() throws Exception { 
        Artist a1 = newArtist();        
        Painting p11 = newPainting();
        Painting p12 = newPainting();

        // *** TESTING THIS *** 
        a1.addToPaintingArray(p11);
        a1.addToPaintingArray(p12);
        
        // test before save
        assertEquals(2, a1.getPaintingArray().size());
        assertSame(a1, p11.getToArtist());
        assertSame(a1, p12.getToArtist());
        
        // do save
        ctxt.commitChanges();
        resetContext();
	
        // test database data
        Artist a2 = fetchArtist();
        assertEquals(2, a2.getPaintingArray().size());
    }
    
    public void testRemove1() throws Exception {        
        Painting p1 = newPainting();
        Gallery g1 = newGallery();        
        g1.addToPaintingArray(p1);

        // do save
        ctxt.commitChanges();
        resetContext();
        
        // test database data
        Gallery g2 = fetchGallery();
        Painting p2 = (Painting)g2.getPaintingArray().get(0);
 
        // *** TESTING THIS *** 
        g2.removeFromPaintingArray(p2);

        // test before save
        assertEquals(0, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        resetContext();

        Painting p3 = fetchPainting();
        assertNull(p3.getToGallery());
        
        Gallery g3 = fetchGallery();
        assertEquals(0, g3.getPaintingArray().size());
    }
    
    
    public void testRemove2() throws Exception {        
        Gallery g1 = newGallery();        
        g1.addToPaintingArray(newPainting());
        g1.addToPaintingArray(newPainting());

        // do save
        ctxt.commitChanges();
        resetContext();
        
        // test database data
        Gallery g2 = fetchGallery();
        assertEquals(2, g2.getPaintingArray().size());
        Painting p2 = (Painting)g2.getPaintingArray().get(0);
        
        // *** TESTING THIS *** 
        g2.removeFromPaintingArray(p2);

        // test before save
        assertEquals(1, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        resetContext();
        
        Gallery g3 = fetchGallery();
        assertEquals(1, g3.getPaintingArray().size());
    }
    
    public void testPropagatePK() throws Exception {
        // setup data
        Gallery g1 = newGallery();
        Exhibit e1 = newExhibit(g1);
        Artist a1 = newArtist();
        ctxt.commitChanges();
        
        // *** TESTING THIS ***
        ArtistExhibit ae1 = (ArtistExhibit)ctxt.createAndRegisterNewObject("ArtistExhibit");
        e1.addToArtistExhibitArray(ae1);
        a1.addToArtistExhibitArray(ae1);
        
        // check before save
        assertSame(e1, ae1.getToExhibit());
        assertSame(a1, ae1.getToArtist());
        
        // save
        // test "assertion" is that commit succeeds (PK of ae1 was set properly)
        ctxt.commitChanges();        
    }
}

