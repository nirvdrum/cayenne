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
package org.objectstyle.cayenne;

import java.util.*;

import org.apache.log4j.*;
import org.objectstyle.art.*;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.query.*;

public class CDOMany2OneTst extends CayenneDOTestBase {
    static Logger logObj = Logger.getLogger(CDOMany2OneTst.class.getName());

    public CDOMany2OneTst(String name) {
        super(name);
    }

   public void testReadRO1() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        a1.addToPaintingArray(p1);
        ctxt.commitChanges();

        // do select
        Expression e =
            ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "toArtist", a1);
        SelectQuery q = new SelectQuery("ROPainting", e);

        // *** TESTING THIS *** 
        List paints = ctxt.performQuery(q);
        assertEquals(1, paints.size());
        
        Painting rop1 = (Painting)paints.get(0);
        assertSame(a1, rop1.getToArtist());
    }
    
    public void testReadRO2() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        a1.addToPaintingArray(p1);
        ctxt.commitChanges();

        resetContext();
        
        // do select
        Expression e =
            ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "toArtist", a1);
        SelectQuery q = new SelectQuery("ROPainting", e);

        // *** TESTING THIS *** 
        List paints = ctxt.performQuery(q);
        assertEquals(1, paints.size());
        
        Painting rop1 = (Painting)paints.get(0);
        assertNotNull(rop1.getToArtist());
        
        // trigger fetch
        rop1.getToArtist().getArtistName();
        assertEquals(PersistenceState.COMMITTED, rop1.getToArtist().getPersistenceState());
    }
    
    
    public void testSelectViaRelationship() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        a1.addToPaintingArray(p1);
        ctxt.commitChanges();

        // do select
        Expression e =
            ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "toArtist", a1);
        SelectQuery q = new SelectQuery("Painting", e);

        // *** TESTING THIS *** 
        List paints = ctxt.performQuery(q);
        assertEquals(1, paints.size());
        assertSame(p1, paints.get(0));
    }
    
    public void testSelectViaMultiRelationship() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        Painting p2 = newPainting();
        Gallery g1 = newGallery();
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);
        p1.setToGallery(g1);
        p2.setToGallery(g1);
        ctxt.commitChanges();

        // do select
        Expression e =
            ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "paintingArray.toGallery", g1);
        SelectQuery q = new SelectQuery("Artist", e);

        // *** TESTING THIS *** 
        List artists = ctxt.performQuery(q);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }
    

    public void testNewAdd() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // *** TESTING THIS *** 
        p1.setToArtist(a1);

        // test before save
        assertSame(a1, p1.getToArtist());
        assertEquals(1, a1.getPaintingArray().size());
        assertSame(p1, a1.getPaintingArray().get(0));

        // do save
        ctxt.commitChanges();
        resetContext();

        // test database data
        Painting p2 = fetchPainting();
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(artistName, a2.getArtistName());
    }

    public void testRemove() throws Exception {
        Painting p1 = newPainting();
        Gallery g1 = newGallery();
        p1.setToGallery(g1);

        // do save
        ctxt.commitChanges();
        resetContext();

        // test database data
        Painting p2 = fetchPainting();
        Gallery g2 = p2.getToGallery();

        // *** TESTING THIS *** 
        p2.setToGallery(null);

        // test before save
        assertEquals(0, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        resetContext();

        Painting p3 = fetchPainting();
        assertNull(p3.getToGallery());
    }

    public void testReplace() throws Exception {
        String altGalleryName = "alt gallery";

        Painting p1 = newPainting();
        Gallery g1 = newGallery();
        g1.setGalleryName(altGalleryName);

        p1.setToGallery(g1);

        // do save
        ctxt.commitChanges();
        resetContext();

        // test database data
        Painting p2 = fetchPainting();
        Gallery g21 = p2.getToGallery();
        assertNotNull(g21);
        assertEquals(altGalleryName, g21.getGalleryName());
        assertEquals(1, g21.getPaintingArray().size());
        assertSame(p2, g21.getPaintingArray().get(0));

        Gallery g22 = newGallery();

        // *** TESTING THIS *** 
        p2.setToGallery(g22);

        // test before save
        assertEquals(0, g21.getPaintingArray().size());
        assertEquals(1, g22.getPaintingArray().size());
        assertSame(p2, g22.getPaintingArray().get(0));

        // do save II
        ctxt.commitChanges();
        resetContext();

        Painting p3 = fetchPainting();
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals(galleryName, g3.getGalleryName());
        assertEquals(1, g3.getPaintingArray().size());
        assertSame(p3, g3.getPaintingArray().get(0));
    }

    public void testSavedAdd() throws Exception {
        Painting p1 = newPainting();
		assertEquals(p1.getObjectId(), ctxt.registeredObject(p1.getObjectId()).getObjectId());
		assertTrue(ctxt.hasChanges());

        // do save
        ctxt.commitChanges();
        resetContext();

        // test database data
        Painting p2 = fetchPainting();
        assertNull(p2.getToGallery());

        Gallery g2 = newGallery();

        // *** TESTING THIS *** 
        p2.setToGallery(g2);

        // test before save
        assertEquals(1, g2.getPaintingArray().size());
        assertSame(p2, g2.getPaintingArray().get(0));

        // do save II
        ctxt.commitChanges();
        resetContext();

        Painting p3 = fetchPainting();
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals(galleryName, g3.getGalleryName());
        assertEquals(1, g3.getPaintingArray().size());
        assertSame(p3, g3.getPaintingArray().get(0));
    }
}