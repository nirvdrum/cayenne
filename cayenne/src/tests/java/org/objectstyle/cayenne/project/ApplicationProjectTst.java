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
package org.objectstyle.cayenne.project;

import java.io.File;
import java.util.List;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ApplicationProjectTst extends CayenneTestCase {
    protected ApplicationProject p;
    protected File f;

    protected void setUp() throws Exception {
        super.setUp();
        f = new File("cayenne.xml");
        p = new ApplicationProject(f);
    }

    public void testProjectFileForObject() throws Exception {
    	DataNode node = new DataNode("dn");
    	DataDomain dm = new DataDomain();
    	dm.addNode(node);
    	p.getConfig().addDomain(dm);
    	
        ProjectFile pf = p.projectFileForObject(node);
        assertNull(pf);

        node.setDataSourceFactory(DriverDataSourceFactory.class.getName());
        ProjectFile pf1 = p.projectFileForObject(node);
        assertTrue(pf1 instanceof DataNodeFile);
        assertSame(node, pf1.getObject());
    }


    public void testConfig() throws Exception {
        assertNotNull(p.getConfig());
    }

    public void testConstructor() throws Exception {
        assertEquals(f.getCanonicalFile(), p.getMainFile());
        assertTrue(p.projectFileForObject(p) instanceof ApplicationProjectFile);

        assertNotNull(p.getChildren());
        assertEquals(0, p.getChildren().size());
    }

    public void testBuildFileList() throws Exception {
        // build a test project tree
        DataDomain d1 = new DataDomain("d1");
        DataMap m1 = new DataMap("m1");
        DataNode n1 = new DataNode("n1");
        n1.setDataSourceFactory(DriverDataSourceFactory.class.getName());

        d1.addMap(m1);
        d1.addNode(n1);

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        n1.addDataMap(m1);

        // initialize project 
        p.getConfig().addDomain(d1);

        // make assertions
        List files = p.buildFileList();

        assertNotNull(files);

        // list must have 3 files total
        assertEquals("Unexpected number of files: " + files, 3, files.size());
    }
}
