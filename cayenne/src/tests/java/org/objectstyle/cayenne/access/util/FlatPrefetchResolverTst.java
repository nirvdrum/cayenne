/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class FlatPrefetchResolverTst extends CayenneTestCase {

    public void testResolveObjectTree() {

        Collection prefetches = Arrays.asList(new Object[] {
            "toArtist"
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);
        FlatPrefetchTreeNode tree = new FlatPrefetchTreeNode(paint, prefetches);
        FlatPrefetchResolver resolver = new FlatPrefetchResolver(context, false, false);

        List objects = resolver.resolveObjectTree(tree, buildRows());
        assertEquals(2, objects.size());

        // object store must have only 3 objects
        Iterator objectsIt = context.getObjectStore().getObjectIterator();
        int i = 0;
        while (objectsIt.hasNext()) {
            DataObject o = (DataObject) objectsIt.next();
            assertEquals("Unresolved object: " + o, PersistenceState.COMMITTED, o
                    .getPersistenceState());
            i++;
        }
        assertEquals(3, i);

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Painting p = (Painting) it.next();
            assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
            Artist target = p.getToArtist();
            assertNotNull(target);
            assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
        }
    }

    List buildRows() {
        List rows = new ArrayList();

        Date d1 = new Date();

        rows.add(buildRow1(d1));
        rows.add(buildRow2(d1));

        return rows;
    }

    DataRow buildRow1(Date d) {
        DataRow r1 = new DataRow(10);
        r1.put("PAINTING_ID", new Integer(1));
        r1.put("ARTIST_ID", new Integer(1));
        r1.put("GALLERY_ID", new Integer(1));
        r1.put("PAINTING_TITLE", "P1");
        r1.put("ESTIMATED_PRICE", new BigDecimal("22.01"));
        r1.put("toArtist.ARTIST_NAME", "Artist1");
        r1.put("toArtist.DATE_OF_BIRTH", d);
        return r1;
    }

    public DataRow buildRow2(Date d) {
        DataRow r2 = new DataRow(10);
        r2.put("PAINTING_ID", new Integer(2));
        r2.put("ARTIST_ID", new Integer(1));
        r2.put("GALLERY_ID", new Integer(1));
        r2.put("PAINTING_TITLE", "P2");
        r2.put("ESTIMATED_PRICE", new BigDecimal("25.01"));
        r2.put("toArtist.ARTIST_NAME", "Artist1");
        r2.put("toArtist.DATE_OF_BIRTH", d);
        return r2;
    }

}