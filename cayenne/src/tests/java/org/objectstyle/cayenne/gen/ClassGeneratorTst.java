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
package org.objectstyle.cayenne.gen;

import java.io.StringWriter;

import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class ClassGeneratorTst extends CayenneTestCase {

	private static final String SUPER_CLASS_PACKAGE="org.objectstyle.art";
	private static final String SUPER_CLASS_NAME="ArtDataObject";
	private static final String FQ_SUPER_CLASS_NAME=SUPER_CLASS_PACKAGE+"."+SUPER_CLASS_NAME;
	
    protected ClassGenerator cgen;
    protected ObjEntity testEntity;

    public void setUp() throws Exception {
        cgen = new ClassGenerator(MapClassGenerator.SUPERCLASS_TEMPLATE);
        testEntity = getDomain().getEntityResolver().lookupObjEntity("Painting");

        // add attribute with reserved keyword name
        ObjAttribute reservedAttribute = new ObjAttribute("abstract");
        reservedAttribute.setType("boolean");
		testEntity.addAttribute(reservedAttribute);
    }

	public void tearDown() {
		// remove attribute to restore entity
		testEntity.removeAttribute("abstract");
	}

    /** All tests are done in one method to avoid Velocity template parsing
      * every time a new generator instance is made for each test. */
    public void testAll() throws Exception {
        // test 1
        doClassName();

        // test 2
        doPackageName();

        // test 3
        doSuperPrefix();

		//test 4
		doSuperClassName();
		
        // final template test
        StringWriter out = new StringWriter();
        cgen.generateClass(out, testEntity);
        out.flush();
        out.close();

        String classCode = out.toString();

        assertNotNull(classCode);
        assertTrue(classCode.length() > 0);
        //Must contain the clause "extends <classname>", where classname is either fully qualified or not 
		//If the class name is not fully qualified, then both the package and the class name must appear, the package as
		// either a package statement or an import.
        int indexOfFQSuperClass=classCode.indexOf(FQ_SUPER_CLASS_NAME);
        if(indexOfFQSuperClass==-1) {
 			int indexOfSuperClassName=classCode.indexOf(SUPER_CLASS_NAME);
 			int indexOfSuperPackageName=classCode.indexOf(SUPER_CLASS_PACKAGE);
 			//Both must be found
       		assertTrue((indexOfSuperClassName!=-1) && (indexOfSuperPackageName!=-1));
       		//Should probably also check for the extends clause, but there can be arbitrary whitespace between extends and
       		// the class name - would need regex to do that easily and correctly... a task for another time.
        }

		// basic test that our reserved attribute has been generated correctly
        assertTrue(classCode.indexOf("_abstract") != -1);
    }

    private void doClassName() throws Exception {
        String className = "aaa";
        cgen.setClassName(className);
        assertEquals(className, cgen.getClassName());
    }

    private void doSuperPrefix() throws Exception {
        String prefix = "pr_";
        cgen.setSuperPrefix(prefix);
        assertEquals(prefix, cgen.getSuperPrefix());
    }

    public void doPackageName() throws Exception {
        assertFalse(cgen.isUsingPackage());
        String pkgName = "aaa.org";
        cgen.setPackageName(pkgName);
        assertEquals(pkgName, cgen.getPackageName());
        assertTrue(cgen.isUsingPackage());
    }
    
    private void doSuperClassName() throws Exception {
    	cgen.setSuperClassName(FQ_SUPER_CLASS_NAME);
    	assertEquals(FQ_SUPER_CLASS_NAME, cgen.getSuperClassName());
    }
}
