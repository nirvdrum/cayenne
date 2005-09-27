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
package org.objectstyle.cayenne;

import junit.framework.TestCase;

import org.objectstyle.cayenne.opp.HessianConnector;
import org.objectstyle.cayenne.util.Util;

public class GlobalIDTst extends TestCase {

    public void testConstructor() {
        GlobalID temp1 = new GlobalID("e");
        assertEquals("e", temp1.getEntityName());
        assertTrue(temp1.isTemporary());
        assertNotNull(temp1.getKey());

        byte[] key = new byte[] {
                1, 2, 3
        };
        GlobalID temp2 = new GlobalID("e1", key);
        assertEquals("e1", temp2.getEntityName());
        assertTrue(temp2.isTemporary());
        assertSame(key, temp2.getKey());
    }

    public void testSerializabilityTemp() throws Exception {
        GlobalID temp1 = new GlobalID("e");
        GlobalID temp2 = (GlobalID) Util.cloneViaSerialization(temp1);
        
        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    public void testSerializabilityPerm() throws Exception {
        GlobalID perm1 = new GlobalID("e", "a", "b");
        GlobalID perm2 = (GlobalID) Util.cloneViaSerialization(perm1);
       
        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    public void testHessianSerializabilityTemp() throws Exception {
        GlobalID temp1 = new GlobalID("e");
        GlobalID temp2 = (GlobalID) HessianConnector.cloneViaHessianSerialization(temp1);
        
        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    public void testHessianSerializabilityPerm() throws Exception {
        GlobalID perm1 = new GlobalID("e", "a", "b");
        GlobalID perm2 = (GlobalID) HessianConnector.cloneViaHessianSerialization(perm1);
        
        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }
}
