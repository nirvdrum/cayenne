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

import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.PaintingInfo;

public class CDOOneDep2OneTst extends CayenneDOTestBase {
    static Logger logObj = Logger.getLogger(CDOOneDep2OneTst.class.getName());

    public CDOOneDep2OneTst(String name) {
        super(name);
    }

    public void testNewAdd1() throws Exception {
        Artist a1 = newArtist();
        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);

        // *** TESTING THIS *** 
        pi1.setPainting(p1);

        // test before save
        assertSame(pi1, p1.getToPaintingInfo());
        assertSame(p1, pi1.getPainting());

        // do save 
        ctxt.commitChanges();
        resetContext();

        // test database data
        PaintingInfo pi2 = fetchPaintingInfo();
        Painting p2 = pi2.getPainting();
        assertNotNull(p2);
        assertEquals(paintingName, p2.getPaintingTitle());
    }

    /** Tests how primary key is propagated from one new object to another. */
    public void testNewAdd2() throws Exception {
        Artist a1 = this.newArtist();
        Gallery g1 = this.newGallery();
        Exhibit e1 = this.newExhibit(g1);

        ArtistExhibit ae1 = this.newArtistExhibit();
        ae1.setToArtist(a1);
        ae1.setToExhibit(e1);

        // do save 

        // *** TESTING THIS *** 
        ctxt.commitChanges();
    }

    public void testReplace() throws Exception {
        String altPaintingName = "alt painting";

        Artist a1 = newArtist();
		assertEquals(a1.getObjectId(), ctxt.registeredObject(a1.getObjectId()).getObjectId());

        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();
        p1.setPaintingTitle(altPaintingName);

        pi1.setPainting(p1);

        assertTrue(ctxt.hasChanges());

        // do save
        ctxt.commitChanges();
        resetContext();

        // test database data
        PaintingInfo pi2 = fetchPaintingInfo();
        Painting p21 = pi2.getPainting();
        assertNotNull(p21);
        assertEquals(altPaintingName, p21.getPaintingTitle());
        assertSame(pi2, p21.getToPaintingInfo());

        Painting p22 = newPainting();

        // *** TESTING THIS *** 
        pi2.setPainting(p22);

        // test before save
        assertNull(p21.getToPaintingInfo());
        assertSame(pi2, p22.getToPaintingInfo());
        assertEquals(PersistenceState.MODIFIED, pi2.getPersistenceState());

        // do save II
        ctxt.commitChanges();
        ObjectId pi2oid = pi2.getObjectId();
        resetContext();

        PaintingInfo pi3 = fetchPaintingInfo();
        Painting p3 = pi3.getPainting();
        assertNotNull(p3);
        assertEquals(paintingName, p3.getPaintingTitle());
        assertSame(pi3, p3.getToPaintingInfo());

        // test that object id was updated.
        assertEquals(pi2oid, pi3.getObjectId());

    }
}