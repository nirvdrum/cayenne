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

import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;

/**
 * @author Andrei Adamchik
 */
public class DataNodeFileTst extends CayenneTestCase {
    protected DataNodeFile dnf;
    protected DataNode node;
    protected Project pr;

    /**
     * Constructor for DataNodeFileTst.
     * @param arg0
     */
    public DataNodeFileTst(String arg0) {
        super(arg0);
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        pr = new TstProject(new File("xyz"));
        node = new DataNode("n1");
        dnf = new DataNodeFile(pr, node);
    }

    public void testProjectFileForObject() throws Exception {
        ProjectFile pf = ProjectFile.projectFileForObject(pr, node);
        assertNull(pf);

        node.setDataSourceFactory(DriverDataSourceFactory.class.getName());
        ProjectFile pf1 = ProjectFile.projectFileForObject(pr, node);
        assertTrue(pf1 instanceof DataNodeFile);
        assertSame(node, pf1.getObject());
    }

    public void testGetObject() throws Exception {
        assertSame(node, dnf.getObject());
    }

    public void testGetObjectName() throws Exception {
        assertEquals(node.getName(), dnf.getObjectName());
    }

    public void testGetFileName() throws Exception {
        assertEquals(node.getName() + ".driver.xml", dnf.getLocation());
    }
}
