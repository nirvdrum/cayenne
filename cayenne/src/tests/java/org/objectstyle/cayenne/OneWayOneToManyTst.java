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

import java.util.Iterator;
import java.util.List;

import org.objectstyle.art.oneway.Artist;
import org.objectstyle.art.oneway.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;
import org.objectstyle.cayenne.unittest.OneWayMappingTestCase;

/**
 * @author Andrei Adamchik
 */
public class OneWayOneToManyTst extends OneWayMappingTestCase {
    protected DataContext ctxt;

    protected void setUp() throws Exception {
        CayenneTestDatabaseSetup setup = getDatabaseSetup();
        setup.cleanTableData();
        DataDomain dom = getDomain();
        setup.createPkSupportForMapEntities((DataNode)dom.getDataNodes().iterator().next());

        ctxt = getDomain().createDataContext();
    }

    public void testReadList() throws Exception {
        // prepare and save a gallery
        Painting p11 = newPainting("g1");
        Painting p12 = newPainting("g1");
        ctxt.commitChanges();

        Artist a1 = newArtist();
        a1.addToPaintingArray(p11);
        a1.addToPaintingArray(p12);

        // test before save
        assertEquals(2, a1.getPaintingArray().size());
        ctxt.commitChanges();

        ctxt = getDomain().createDataContext();

        Artist a2 = fetchArtist();
        assertNotNull(a2);

        Iterator it = a2.getPaintingArray().iterator();
        while (it.hasNext()) {
            it.next();
        }
    }

   public void testRevertModification() throws Exception {
        // prepare and save a gallery
        Painting p11 = newPainting("p11");
        Painting p12 = newPainting("p12");
        ctxt.commitChanges();

        Artist a1 = newArtist();
        a1.addToPaintingArray(p11);
 
        // test before save
        assertEquals(1, a1.getPaintingArray().size());
        ctxt.commitChanges();
		
       	a1.addToPaintingArray(p12);
		assertEquals(2, a1.getPaintingArray().size());
      	ctxt.rollbackChanges();
 
		/* TODO - these all fail until the one-way relationship code works correctly
        assertEquals(1, a1.getPaintingArray().size()); //Should only be one..
        assertEquals(p11, a1.getPaintingArray().get(0)); //..and it should be the original one
    	
    	ctxt.commitChanges(); //Save so we can be sure the rollback really worked
    	
        ctxt = getDomain().createDataContext();

        Artist a2 = fetchArtist();
        assertNotNull(a2);
        assertEquals(1, a2.getPaintingArray().size()); //Should only be one..
        Painting p21 = (Painting)a1.getPaintingArray().get(0);
        assertEquals(p11.getPaintingTitle(), p21.getPaintingTitle()); //..and it should be the same as the original one
        */
     }

    protected Painting newPainting(String name) {
        Painting p1 = (Painting) ctxt.createAndRegisterNewObject("Painting");
        p1.setPaintingTitle(name);
        return p1;
    }

    protected Artist newArtist() {
        Artist a1 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a1.setArtistName(CayenneDOTestBase.artistName);
        return a1;
    }

    protected Artist fetchArtist() {
        SelectQuery q =
            new SelectQuery(
                "Artist",
                ExpressionFactory.binaryPathExp(
                    Expression.EQUAL_TO,
                    "artistName",
                    CayenneDOTestBase.artistName));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }
}
