/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.access;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * DataDomain unit tests.
 * 
 * @author Andrei Adamchik 
 */
public class DataDomainTst extends CayenneTestCase {

    public void testName() throws Exception {
        DataDomain domain = new DataDomain("some name");
        assertEquals("some name", domain.getName());
        domain.setName("tst_name");
        assertEquals("tst_name", domain.getName());
    }

    public void testNodes() throws java.lang.Exception {
        DataDomain domain = new DataDomain("dom1");
        assertEquals(0, domain.getDataNodes().size());
        domain.addNode(new DataNode("1"));
        assertEquals(1, domain.getDataNodes().size());
        domain.addNode(new DataNode("2"));
        assertEquals(2, domain.getDataNodes().size());
    }

    public void testNodeMaps() throws java.lang.Exception {
        DataDomain domain = new DataDomain("dom1");
        assertNull(domain.getMap("map"));

        DataNode node = new DataNode("1");
        node.addDataMap(new DataMap("map"));

        domain.addNode(node);
        assertNotNull(domain.getMap("map"));
    }

    public void testMaps() throws java.lang.Exception {
        DataDomain d1 = new DataDomain("dom1");

        DataMap m1 = new DataMap("m1");
        d1.addMap(m1);
        assertSame(m1, d1.getMap(m1.getName()));

        d1.removeMap(m1.getName());
        assertNull(d1.getMap(m1.getName()));
    }

    public void testDataNodeForObjEntity() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        assertNull(domain.getMap("map"));

        DataMap map = new DataMap("map");
        ObjEntity oe = new ObjEntity("a");
        map.addObjEntity(oe);
        DataNode node = new DataNode("1");
        node.addDataMap(map);

        domain.addNode(node);
        assertNotNull(domain.dataNodeForObjEntity(oe));
        assertSame(node, domain.dataNodeForObjEntity(oe));
    }

    public void testDataNodeForObjEntityName() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        assertNull(domain.getMap("map"));

        DataMap map = new DataMap("map");
        ObjEntity oe = new ObjEntity("a");
        map.addObjEntity(oe);
        DataNode node = new DataNode("1");
        node.addDataMap(map);

        domain.addNode(node);
        assertNotNull(domain.dataNodeForObjEntityName(oe.getName()));
        assertSame(node, domain.dataNodeForObjEntityName(oe.getName()));
    }

    public void testReindexNodes() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        DataMap map = new DataMap("map");
        DataNode node = new DataNode("1");
        node.addDataMap(map);
        domain.addNode(node);

        // add new entity dynamically
        ObjEntity oe1 = new ObjEntity("e1");
        map.addObjEntity(oe1);
        assertNull(domain.nodesByEntityName.get("e1"));

        // testing this
        domain.reindexNodes();

        assertSame(node, domain.nodesByEntityName.get("e1"));
    }

    public void testEntityResolver() {
        assertNotNull(getDomain().getEntityResolver());

        DataDomain domain = new DataDomain("dom1");
        assertNotNull(domain.getEntityResolver());
    }

    public void testCreateDataContextWithSharedCache() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.SHARED_CACHE_ENABLED_PROPERTY, Boolean.TRUE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isSharedCacheEnabled());

        DataContext c1 = domain.createDataContext();
        assertSame(
            c1.getObjectStore().getDataRowCache(),
            domain.getSharedSnapshotCache());

        DataContext c2 = domain.createDataContext(true);
        assertSame(
            c2.getObjectStore().getDataRowCache(),
            domain.getSharedSnapshotCache());

        DataContext c3 = domain.createDataContext(false);
        assertNotSame(
            c3.getObjectStore().getDataRowCache(),
            domain.getSharedSnapshotCache());
    }

    public void testCreateDataContextWithLocalCache() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.SHARED_CACHE_ENABLED_PROPERTY, Boolean.FALSE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isSharedCacheEnabled());

        DataContext c2 = domain.createDataContext(true);
        assertSame(
            c2.getObjectStore().getDataRowCache(),
            domain.getSharedSnapshotCache());

        DataContext c3 = domain.createDataContext(false);
        assertNotSame(
            c3.getObjectStore().getDataRowCache(),
            domain.getSharedSnapshotCache());

        DataContext c1 = domain.createDataContext();
        assertNotSame(
            c1.getObjectStore().getDataRowCache(),
            domain.getSharedSnapshotCache());

        DataContext c4 = domain.createDataContext();
        assertNotSame(
            c4.getObjectStore().getDataRowCache(),
            c1.getObjectStore().getDataRowCache());
    }

    public void testCreatedataContextValidation() throws Exception {
        Map properties = new HashMap();
        properties.put(
            DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
            Boolean.TRUE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isValidatingObjectsOnCommit());

        DataContext c1 = domain.createDataContext(true);
        assertTrue(c1.isValidatingObjectsOnCommit());
    }

    public void testCreatedataContextNoValidation() throws Exception {
        Map properties = new HashMap();
        properties.put(
            DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
            Boolean.FALSE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isValidatingObjectsOnCommit());

        DataContext c1 = domain.createDataContext(true);
        assertFalse(c1.isValidatingObjectsOnCommit());
    }
}
