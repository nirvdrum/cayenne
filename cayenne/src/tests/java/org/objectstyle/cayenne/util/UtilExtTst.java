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
package org.objectstyle.cayenne.util;

import java.io.File;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.CayenneTestCase;

public class UtilExtTst extends CayenneTestCase {
    static Logger logObj = Logger.getLogger(UtilTst.class.getName());

    public UtilExtTst(String name) {
        super(name);
    }

    public void testPackagePath1() throws java.lang.Exception {
        String expectedPath = "org/objectstyle/cayenne/util";
        assertEquals(expectedPath, Util.getPackagePath(UtilExtTst.class.getName()));
    }

    public void testPackagePath2() throws java.lang.Exception {
        // inner class
        class Tst extends Object {};
        String expectedPath = "org/objectstyle/cayenne/util";
        assertEquals(expectedPath, Util.getPackagePath(Tst.class.getName()));
    }

    public void testPackagePath3() throws java.lang.Exception {
        assertEquals("", Util.getPackagePath("ClassWithNoPackage"));
    }

    public void testIsEmptyString1() throws Exception {
        assertTrue(Util.isEmptyString(""));
    }

    public void testIsEmptyString2() throws Exception {
        assertTrue(!Util.isEmptyString("  "));
    }

    public void testIsEmptyString3() throws Exception {
        assertTrue(Util.isEmptyString(null));
    }

    public void testBackslashFix() throws java.lang.Exception {
        String strBefore = "abcd\\12345\\";
        String strAfter = "abcd/12345/";
        assertEquals(strAfter, Util.substBackslashes(strBefore));
    }

    public void testNullSafeEquals() throws java.lang.Exception {
        // need a special subclass of Object to make "clone" method public
        class CloneableObject implements Cloneable {
            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            public boolean equals(Object obj) {
                if (obj == null)
                    return false;

                // for the purpose of this test
                // all objects of this class considered equal
                // (since they carry no state)
                return obj.getClass() == this.getClass();
            }
        }

        CloneableObject o1 = new CloneableObject();
        Object o2 = new Object();
        Object o3 = o1.clone();

        assertTrue(o3.equals(o1));
        assertTrue(Util.nullSafeEquals(o1, o1));
        assertTrue(!Util.nullSafeEquals(o1, o2));
        assertTrue(Util.nullSafeEquals(o1, o3));
        assertTrue(!Util.nullSafeEquals(o1, null));
        assertTrue(!Util.nullSafeEquals(null, o1));
        assertTrue(Util.nullSafeEquals(null, null));
    }

    public void testExtractFileExtension1() throws Exception {
        String fullName = "n.ext";
        assertEquals("ext", Util.extractFileExtension(fullName));
    }

    public void testExtractFileExtension2() throws Exception {
        String fullName = "n";
        assertNull(Util.extractFileExtension(fullName));
    }

    public void testExtractFileExtension3() throws Exception {
        String fullName = ".ext";
        assertNull(Util.extractFileExtension(fullName));
    }

    public void testStripFileExtension1() throws Exception {
        String fullName = "n.ext";
        assertEquals("n", Util.stripFileExtension(fullName));
    }

    public void testStripFileExtension2() throws Exception {
        String fullName = "n";
        assertEquals("n", Util.stripFileExtension(fullName));
    }

    public void testStripFileExtension3() throws Exception {
        String fullName = ".ext";
        assertEquals(".ext", Util.stripFileExtension(fullName));
    }

    public void testEncodeXmlAttribute1() throws Exception {
        String unencoded = "normalstring";
        assertEquals(unencoded, Util.encodeXmlAttribute(unencoded));
    }

    public void testEncodeXmlAttribute2() throws Exception {
        String unencoded = "<a>";
        assertEquals("&lt;a&gt;", Util.encodeXmlAttribute(unencoded));
    }

    public void testEncodeXmlAttribute3() throws Exception {
        String unencoded = "a&b";
        assertEquals("a&amp;b", Util.encodeXmlAttribute(unencoded));
    }
}
