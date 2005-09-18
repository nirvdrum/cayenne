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
package org.objectstyle.cayenne.query;

import java.util.Collections;

import junit.framework.TestCase;

import org.objectstyle.cayenne.distribution.HessianConnector;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.parser.ASTObjPath;
import org.objectstyle.cayenne.map.EntityResolver;

public class SelectTst extends TestCase {

    public void testConstructorClassExpression() {
        Select select = new Select(Object.class, new ASTObjPath("x"));
        assertNull(select.getEntityName());
        assertEquals("java.lang.Object", select.getObjectClass());
        assertEquals("x", select.getQualifier());

        try {
            new Select((Class) null, new ASTObjPath("x"));
            fail("must throw on null class");
        }
        catch (IllegalArgumentException e) {

        }

        // must not throw on null expression
        new Select(Object.class, (Expression) null);
    }

    public void testConstructorClassString() {
        Select select = new Select(Object.class, "x");
        assertNull(select.getEntityName());
        assertEquals("java.lang.Object", select.getObjectClass());
        assertEquals("x", select.getQualifier());

        try {
            new Select((Class) null, "x");
            fail("must throw on null class");
        }
        catch (IllegalArgumentException e) {

        }

        // must not throw on null expression
        new Select(Object.class, (String) null);
    }

    public void testConstructorStringExpression() {
        Select select = new Select("e", new ASTObjPath("x"));
        assertNull(select.getObjectClass());
        assertEquals("e", select.getEntityName());
        assertEquals("x", select.getQualifier());

        try {
            new Select((String) null, new ASTObjPath("x"));
            fail("must throw on null entity name");
        }
        catch (IllegalArgumentException e) {

        }

        // must not throw on null expression
        new Select("e", (Expression) null);
    }

    public void testConstructorStringString() {
        Select select = new Select("e", "x");
        assertNull(select.getObjectClass());
        assertEquals("e", select.getEntityName());
        assertEquals("x", select.getQualifier());

        try {
            new Select((String) null, "x");
            fail("must throw on null entity name");
        }
        catch (IllegalArgumentException e) {

        }

        // must not throw on null expression
        new Select("e", (String) null);
    }

    public void testSetParametersMap() {
        Select select = new Select("e", "x");
        assertTrue(select.getParameters().isEmpty());

        select.setParameters(Collections.singletonMap("a", "b"));
        assertEquals(1, select.getParameters().size());
        assertEquals("b", select.getParameters().get("a"));
    }

    public void testSetParametersArrayArray() {
        Select select = new Select("e", "x");
        assertTrue(select.getParameters().isEmpty());

        select.setParameters(new String[] {
            "a"
        }, new Object[] {
            "b"
        });
        assertEquals(1, select.getParameters().size());
        assertEquals("b", select.getParameters().get("a"));
    }

    public void testSetParametersOverride() {
        Select select = new Select("e", "x");
        assertTrue(select.getParameters().isEmpty());

        select.setParameters(Collections.singletonMap("a", "b"));
        assertEquals(1, select.getParameters().size());
        assertEquals("b", select.getParameters().get("a"));

        select.setParameters(Collections.singletonMap("c", "d"));
        assertEquals(1, select.getParameters().size());
        assertEquals("d", select.getParameters().get("c"));
    }

    public void testBuildReplacementQuery() {
        Ordering o = new Ordering("a", true);

        Select select = new Select("e", "x");
        select.addOrdering(o);
        select.addPrefetch("abc");

        Query query = select.buildReplacementQuery(new EntityResolver());
        assertTrue(query instanceof SelectQuery);
        SelectQuery executableSelect = (SelectQuery) query;
        assertEquals("e", executableSelect.getRoot());
        assertTrue(executableSelect.getOrderings().contains(o));
        assertTrue(executableSelect.getPrefetches().contains("abc"));
    }

    public void testSerializableWithHessian() throws Exception {

        Select select = new Select("e", "x");
        select.addOrdering(new Ordering("a", true));
        select.addPrefetch("abc");

        Select clone = (Select) HessianConnector.cloneViaHessianSerialization(select);
        assertEquals("e", clone.getEntityName());
        assertEquals(1, clone.getOrderings().size());
        assertEquals("a", ((Ordering) clone.getOrderings().get(0)).getSortSpecString());
        assertTrue(clone.getPrefetches().contains("abc"));
    }
}
