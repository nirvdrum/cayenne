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

import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class FlatProjectViewTst extends CayenneTestCase {

    /**
     * Constructor for FlatProjectView.
     * @param name
     */
    public FlatProjectViewTst(String name) {
        super(name);
    }

    public void testObjectFromPath1() throws Exception {
        Object[] path = new Object[] { new Object(), new Object()};
        assertSame(path[1], FlatProjectView.objectFromPath(path));
    }

    public void testObjectFromPath2() throws Exception {
        Object[] path = new Object[] { new Object()};
        assertSame(path[0], FlatProjectView.objectFromPath(path));
    }

    public void testObjectFromPath3() throws Exception {
        Object[] path = new Object[] {};

        try {
            FlatProjectView.objectFromPath(path);
            fail("Must throw exception on empty list");
        } catch (ProjectException ex) {}
    }

    public void testBuildPath1() throws Exception {
        Object obj1 = new Object();
        Object[] path = FlatProjectView.buildPath(obj1, null);
        assertNotNull(path);
        assertEquals(1, path.length);
        assertSame(obj1, path[0]);
    }

    public void testBuildPath2() throws Exception {
        Object obj1 = new Object();
        Object[] tstPath = new Object[] { new Object(), new Object()};

        Object[] path = FlatProjectView.buildPath(obj1, tstPath);
        assertNotNull(path);
        assertEquals(3, path.length);
        assertSame(obj1, path[2]);
        assertSame(tstPath[1], path[1]);
        assertSame(tstPath[0], path[0]);
    }

    public void testObjectParentFromPath1() throws Exception {
        Object[] path = new Object[] { new Object(), new Object()};
        assertSame(path[0], FlatProjectView.objectParentFromPath(path));
    }

    public void testObjectParentFromPath2() throws Exception {
        Object[] path = new Object[] { new Object()};
        assertNull(FlatProjectView.objectParentFromPath(path));
    }
}
