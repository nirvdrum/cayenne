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

import org.objectstyle.art.oneway.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.query.*;
import org.objectstyle.cayenne.unittest.*;

/**
 * @author Andrei Adamchik
 */
public class OneWayManyToOneTst extends OneWayMappingTestCase {
    protected DataContext ctxt;

    /**
     * Constructor for OneWayOneToManyTst.
     * @param name
     */
    public OneWayManyToOneTst(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        CayenneTestDatabaseSetup setup = getDatabaseSetup();
        setup.cleanTableData();
        DataDomain dom = getDomain();
        setup.createPkSupportForMapEntities(dom.getDataNodes()[0]);

        ctxt = getDomain().createDataContext();
    }

    public void testSavedAdd() throws Exception {
        // prepare and save a gallery
        Gallery g1 = newGallery("g1");
        ctxt.commitChanges();

        Painting p2 = newPainting();

        // *** TESTING THIS *** 
        p2.setToGallery(g1);

        // test before save
        assertSame(g1, p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        ctxt = getDomain().createDataContext();

        Painting p3 = fetchPainting();
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals("g1", g3.getGalleryName());
    }

    public void testSavedReplace() throws Exception {
        // prepare and save a gallery
        Gallery g11 = newGallery("g1");
        Gallery g12 = newGallery("g1");
        ctxt.commitChanges();

        Painting p1 = newPainting();
        p1.setToGallery(g11);

        // test before save
        assertSame(g11, p1.getToGallery());
        ctxt.commitChanges();

        p1.setToGallery(g12);
        ctxt.commitChanges();

        ctxt = getDomain().createDataContext();

        Painting p2 = fetchPainting();
        Gallery g21 = p2.getToGallery();
        assertNotNull(g21);
        assertEquals(g12.getGalleryName(), g21.getGalleryName());
    }

    protected Painting newPainting() {
        Painting p1 = (Painting) ctxt.createAndRegisterNewObject("Painting");
        p1.setPaintingTitle(CayenneDOTestBase.paintingName);
        return p1;
    }

    protected Gallery newGallery(String name) {
        Gallery g1 = (Gallery) ctxt.createAndRegisterNewObject("Gallery");
        g1.setGalleryName(name);
        return g1;
    }

    protected Painting fetchPainting() {
        SelectQuery q =
            new SelectQuery(
                "Painting",
                ExpressionFactory.binaryPathExp(
                    Expression.EQUAL_TO,
                    "paintingTitle",
                    CayenneDOTestBase.paintingName));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (Painting) pts.get(0) : null;
    }
}
