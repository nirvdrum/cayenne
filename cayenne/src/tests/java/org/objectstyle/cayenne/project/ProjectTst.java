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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.project.validator.Validator;

/**
 * @author Andrei Adamchik
 */
public class ProjectTst extends CayenneTestCase {
	static Logger logObj = Logger.getLogger(ProjectTst.class);
	
    protected Project p;
    protected File f;

    /**
     * Constructor for ProjectTst.
     * @param arg0
     */
    public ProjectTst(String arg0) {
        super(arg0);
    }

    /**
      * @see junit.framework.TestCase#setUp()
      */
    protected void setUp() throws Exception {
        super.setUp();
        f = new File("xyz");
        p = new TstProject(f);
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
    	
    	// logObj.warn("Files: " + files);
    	
    	assertNotNull(files);
    	
    	// list must have 3 files total
    	assertEquals(3, files.size());    	
    }

    public void testConstructor() throws Exception {
        assertEquals(f.getCanonicalFile(), p.getMainProjectFile());
    }

    public void testValidator() throws Exception {
        Validator v1 = p.getValidator();
        assertSame(p, v1.getProject());

        Validator v2 = p.getValidator();
        assertSame(p, v2.getProject());

        assertTrue(v1 != v2);
    }

    public void testProcessSave() throws Exception {
        ArrayList list = new ArrayList();
        SaveEmulator file = new SaveEmulator(false);
        list.add(file);
        list.add(file);

        p.processSave(list);
        assertEquals(2, file.saveTempCount);
        assertEquals(0, file.commitCount);
        assertEquals(0, file.undoCount);
    }

    public void testProcessSaveFail() throws Exception {
        ArrayList list = new ArrayList();
        SaveEmulator file = new SaveEmulator(true);
        list.add(file);

        try {
            p.processSave(list);
            fail("Save must have failed.");
        } catch (ProjectException ex) {
            // exception expected
            assertEquals(1, file.saveTempCount);
            assertEquals(0, file.commitCount);
            assertEquals(1, file.undoCount);
        }
    }


    class SaveEmulator extends ProjectFile {
        protected int commitCount;
        protected int undoCount;
        protected int deleteCount;
        protected int saveTempCount;
        protected boolean shouldFail;

        public SaveEmulator(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#canHandle(Object)
         */
        public boolean canHandle(Object obj) {
            return false;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#createProjectFile(Object)
         */
        public ProjectFile createProjectFile(Project project, Object obj) {
            return null;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getObject()
         */
        public Object getObject() {
            return null;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getObjectName()
         */
        public String getObjectName() {
            return null;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveToFile(File)
         */
        public void save(PrintWriter out) throws Exception {
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveCommit()
         */
        public File saveCommit() {
            commitCount++;
            return new File("abc");
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveDelete()
         */
        public boolean saveDelete() {
            deleteCount++;
            return !shouldFail;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveTemp()
         */
        public void saveTemp() throws Exception {
            saveTempCount++;
            
            if(shouldFail) {
            	throw new Exception("You forced me to fail...");
            }
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveUndo()
         */
        public void saveUndo() {
            undoCount++;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getFileName()
         */
        public String getLocation() {
            return null;
        }


        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getOldFileName()
         */
        public String getOldLocation() {
            return null;
        }


        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#resolveFile()
         */
        public File resolveFile() {
            return new File("abc");
        }


        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#resolveOldFile()
         */
        public File resolveOldFile() {
            return new File("xyz");
        }


    }
}
