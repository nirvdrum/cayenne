/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.graph;

import java.sql.Date;

import junit.framework.TestCase;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.unit.util.TestBean;

public class BeanMergeHandlerTst extends TestCase {

    public void testConstructor() {
        GraphMap map = new MockGraphMap();
        BeanMergeHandler handler = new BeanMergeHandler(map);
        assertSame(map, handler.getGraphMap());
    }

    public void testNodeIdChanged() {
        GraphMap map = new MockGraphMap();
        Object id1 = new Object();
        Object id2 = new Object();
        Object node = new Object();
        map.registerNode(id1, node);

        BeanMergeHandler handler = new BeanMergeHandler(map);

        handler.nodeIdChanged(id1, id2);

        assertNull(map.getNode(id1));
        assertSame(node, map.getNode(id2));
    }

    public void testPropertyChanged() {
        GraphMap map = new MockGraphMap();
        BeanMergeHandler handler = new BeanMergeHandler(map);

        Object id = new Object();
        TestBean node = new TestBean();
        map.registerNode(id, node);

        // Integer property
        assertNull(node.getInteger());

        handler.nodePropertyChanged(id, "integer", node.getInteger(), new Integer(55));
        assertEquals(new Integer(55), node.getInteger());

        handler.nodePropertyChanged(id, "integer", node.getInteger(), new Integer(56));
        assertEquals(new Integer(56), node.getInteger());

        handler.nodePropertyChanged(id, "integer", node.getInteger(), null);
        assertNull(node.getInteger());

        // Date property
        assertNull(node.getDateProperty());

        Date date = new Date(System.currentTimeMillis());
        handler.nodePropertyChanged(id, "dateProperty", node.getDateProperty(), date);
        assertSame(date, node.getDateProperty());

        handler.nodePropertyChanged(id, "dateProperty", node.getDateProperty(), null);
        assertNull(node.getDateProperty());
    }

    public void testInvalidPropertyChanged() {
        GraphMap map = new MockGraphMap();
        BeanMergeHandler handler = new BeanMergeHandler(map);

        Object id = new Object();
        TestBean node = new TestBean();
        map.registerNode(id, node);

        // property class mismatch
        node.setInteger(new Integer(55));
        try {
            handler.nodePropertyChanged(id, "integer", node.getInteger(), new Object());
            fail("Didn't throw an exception when invalid property was set");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
            assertEquals(new Integer(55), node.getInteger());
        }
    }

    public void testArcCreatedRemoved() {
        GraphMap map = new MockGraphMap();
        BeanMergeHandler handler = new BeanMergeHandler(map);

        Object id1 = new Object();
        Object id2 = new Object();
        Object id3 = new Object();
        TestBean node1 = new TestBean();
        TestBean node2 = new TestBean();
        TestBean node3 = new TestBean();
        map.registerNode(id1, node1);
        map.registerNode(id2, node2);
        map.registerNode(id3, node3);

        // Integer property
        assertNull(node1.getRelatedBean());

        handler.arcCreated(id1, id2, "relatedBean");
        assertEquals(node2, node1.getRelatedBean());

        handler.arcCreated(id1, id3, "relatedBean");
        assertEquals(node3, node1.getRelatedBean());

        handler.arcDeleted(id1, id2, "relatedBean");
        assertNull(node1.getRelatedBean());
    }

}
