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

import org.objectstyle.cayenne.util.Util;

import junit.framework.TestCase;

public class TempObjectIdTst extends TestCase {
    
    public void testEqualsDeserialized() throws Exception {
        TempObjectId oid1 = new TempObjectId(Object.class);
        Object oid2 = Util.cloneViaSerialization(oid1);
        assertNotSame(oid1, oid2);
        assertEquals(oid1, oid2);
    }

    public void testEqualsSame() {
        Class class1 = Number.class;
        TempObjectId oid1 = new TempObjectId(class1);
        assertEquals(oid1, oid1);
        assertEquals(oid1.hashCode(), oid1.hashCode());
    }

    public void testEquals() {
        byte[] b1 = new byte[] {
            1
        };
        byte[] b2 = new byte[] {
            1
        };

        TempObjectId oid1 = new TempObjectId(Object.class, b1);
        TempObjectId oid2 = new TempObjectId(Object.class, b2);
        assertEquals(oid1, oid1);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    public void testNotEquals1() {
        Class class1 = Number.class;
        TempObjectId oid1 = new TempObjectId(class1);
        TempObjectId oid2 = new TempObjectId(class1);
        assertFalse(oid1.equals(oid2));

        // don't make any assertions about hashCode .. it may be the same
    }

    public void testNotEquals2() {
        byte[] b1 = new byte[] {
            1
        };
        byte[] b2 = new byte[] {
                1, 2
        };

        TempObjectId oid1 = new TempObjectId(Object.class, b1);
        TempObjectId oid2 = new TempObjectId(Object.class, b2);
        assertFalse(oid1.equals(oid2));
    }

    public void testNotEquals3() {
        byte[] b1 = new byte[] {
            1
        };

        TempObjectId oid1 = new TempObjectId(Object.class, b1);
        TempObjectId oid2 = new TempObjectId(Object.class);
        assertFalse(oid1.equals(oid2));
        assertFalse(oid2.equals(oid1));
    }

    public void testNotEquals4() {
        byte[] b1 = new byte[] {
            1
        };
        byte[] b2 = new byte[] {
            3
        };

        TempObjectId oid1 = new TempObjectId(Object.class, b1);
        TempObjectId oid2 = new TempObjectId(Object.class, b2);
        assertFalse(oid1.equals(oid2));
    }

    public void testNotEqualsNull() {
        TempObjectId o = new TempObjectId(Object.class);
        assertFalse(o.equals(null));
    }
}
