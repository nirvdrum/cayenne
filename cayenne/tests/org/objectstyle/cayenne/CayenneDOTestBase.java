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

import java.util.List;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.objectstyle.TestMain;
import org.objectstyle.art.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class CayenneDOTestBase extends CayenneTestCase {    
    static final String artistName = "artist with one painting";
    static final String galleryName = "my gallery";
    static final String textReview = "this painting sucks...";
    static final String paintingName = "painting about nothing";    
    static final byte[] paintingImage = new byte[] {2, 3, 4, 5};
    
    protected DataContext ctxt;
    
    public CayenneDOTestBase(String name) {
        super(name);
    }
    
    public void setUp() throws java.lang.Exception {
        TestMain.getSharedDatabaseSetup().cleanTableData();        
        DataDomain dom = getSharedDomain();
        Level oldLevel = QueryLogger.getLogLevel();
        QueryLogger.setLogLevel(Level.SEVERE);
        dom.getDataNodes()[0].createPkSupportForMapEntities();
        QueryLogger.setLogLevel(oldLevel);
        resetContext();
    }
    
    protected void resetContext() {
        ctxt = getSharedDomain().createDataContext();
    }
    
    protected Exhibit newExhibit(Gallery gallery) {
        Exhibit e1 = (Exhibit)ctxt.createAndRegisterNewObject("Exhibit");
        e1.setOpeningDate(new java.util.Date());
        e1.setClosingDate(new java.util.Date());
        e1.setToGallery(gallery);
        return e1;
    }
    
    protected ArtistExhibit newAritistExhibit() {
        return (ArtistExhibit)ctxt.createAndRegisterNewObject("ArtistExhibit");
    }
    
    protected Gallery newGallery() {
        Gallery g1 = (Gallery)ctxt.createAndRegisterNewObject("Gallery");
        g1.setGalleryName(galleryName);
        return g1;
    }
    
    protected Artist newArtist() {
        Artist a1 = (Artist)ctxt.createAndRegisterNewObject("Artist");
        a1.setArtistName(artistName);
        return a1;
    }
    
    protected Painting newPainting() {
        Painting p1 = (Painting)ctxt.createAndRegisterNewObject("Painting");
        p1.setPaintingTitle(paintingName);
        return p1;
    }
    
    protected PaintingInfo newPaintingInfo() {
        PaintingInfo p1 = (PaintingInfo)ctxt.createAndRegisterNewObject("PaintingInfo");
        p1.setTextReview(textReview);
        p1.setImageBlob(paintingImage);
        return p1;
    }
    
    protected Gallery fetchGallery() {
        SelectQuery q = new SelectQuery(
        "Gallery", 
        ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "galleryName", galleryName)
        );
        List gls = ctxt.performQuery(q);
        return (gls.size() > 0) ? (Gallery)gls.get(0) : null;
    }
    
    protected Artist fetchArtist() {
        SelectQuery q = new SelectQuery(
        "Artist", 
        ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "artistName", artistName)
        );
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist)ats.get(0) : null;
    }
    
    protected Painting fetchPainting() {
        SelectQuery q = new SelectQuery(
        "Painting", 
        ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "paintingTitle", paintingName)
        );
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (Painting)pts.get(0) : null;
    }
    
    protected PaintingInfo fetchPaintingInfo() {
        // we are using "LIKE" comparison, since Sybase does not allow
        // "=" comparisons on "text" columns
        SelectQuery q = new SelectQuery(
        "PaintingInfo", 
        ExpressionFactory.binaryPathExp(Expression.LIKE, "textReview", textReview)
        );
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (PaintingInfo)pts.get(0) : null;
    }
}

