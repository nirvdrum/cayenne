/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Fabricio Voznika
 */
public class ValidationResultTst extends TestCase {

    private ValidationResult res;

    private Object obj1;
    private Object obj2;
    private Object obj3;
    private Object obj4;
    private Object obj5;
    private Object obj6;

    protected void setUp() throws Exception {
        super.setUp();
        obj1 = new Object();
        obj2 = new Object();
        obj3 = new Object();
        obj4 = new Object();
        obj5 = new Object();
        obj6 = new Object();
    }

    public void addFailures() {
        res = new ValidationResult();

        res.addFailure(obj1, "obj1 1", "mes obj1 1");
        res.addFailure(obj1, "obj1 2", "mes obj1 2");
        res.addFailure(obj1, "obj1 3", "mes obj1 3");

        res.addFailure(obj2, "obj2 1", "mes obj2 1");
        res.addFailure(obj2, "obj2 2", "mes obj2 2a");
        res.addFailure(obj2, "obj2 2", "mes obj2 2b");

        res.addFailure(obj3, "obj3 1", "mes obj3 1");
        res.addFailure(obj3, null, "obj3 null");

        res.addFailure(obj4, "obj4 1", "mes obj4 1");
        res.addFailure(obj4, null, "mes obj4 nulla");
        res.addFailure(obj4, null, "mes obj4 nullb");

        res.addFailure(obj5, null, "mes obj5 null");

        res.addFailure(null, null, "null");
    }

    public void testHasFailure() {
        this.addFailures();

        assertTrue(res.hasFailures());

        assertTrue(res.hasFailures(obj1));
        assertTrue(res.hasFailure(obj1, "obj1 1"));
        assertTrue(res.hasFailure(obj1, "obj1 2"));
        assertTrue(res.hasFailure(obj1, "obj1 3"));
        assertFalse(res.hasFailure(obj1, "Foobar"));
        assertFalse(res.hasFailure(obj1, null));

        assertTrue(res.hasFailures(obj2));
        assertTrue(res.hasFailure(obj2, "obj2 1"));
        assertTrue(res.hasFailure(obj2, "obj2 2"));
        assertFalse(res.hasFailure(obj2, "Foobar"));

        assertTrue(res.hasFailure(obj3, "obj3 1"));
        assertTrue(res.hasFailure(obj3, null));
        assertFalse(res.hasFailure(obj3, "Foobar"));

        assertTrue(res.hasFailure(obj4, "obj4 1"));
        assertTrue(res.hasFailure(obj4, null));
        assertFalse(res.hasFailure(obj4, "Foobar"));

        assertTrue(res.hasFailure(obj5, null));
        assertFalse(res.hasFailure(obj5, "Foobar"));

        assertFalse(res.hasFailure(obj6, null));
        assertFalse(res.hasFailure(obj6, "Foobar"));
    }

    public void testGetFailure() {
        this.addFailures();

        String[] values =
            new String[] {
                "mes obj1 1",
                "mes obj1 2",
                "mes obj1 3",
                "mes obj2 1",
                "mes obj2 2a",
                "mes obj2 2b",
                "mes obj3 1",
                "obj3 null",
                "mes obj4 1",
                "mes obj4 nulla",
                "mes obj4 nullb",
                "mes obj5 null",
                "null" };
        this.privateTestGetFailure(values, res.getFailures(), true);

        values = new String[] { "mes obj1 1", "mes obj1 2", "mes obj1 3" };
        this.privateTestGetFailure(values, res.getFailures(obj1), true);

        values = new String[] { "mes obj1 1" };
        this.privateTestGetFailure(values, res.getFailures(obj1, "obj1 1"), false);

        values = new String[] { "mes obj1 2" };
        this.privateTestGetFailure(values, res.getFailures(obj1, "obj1 2"), false);

        values = new String[] {
        };
        this.privateTestGetFailure(values, res.getFailures(obj1, "foobar"), false);

        values = new String[] { "mes obj2 2a", "mes obj2 2b" };
        this.privateTestGetFailure(values, res.getFailures(obj2, "obj2 2"), false);

        values = new String[] { "obj3 null" };
        this.privateTestGetFailure(values, res.getFailures(obj3, null), false);

        values = new String[] { "mes obj4 nulla", "mes obj4 nullb" };
        this.privateTestGetFailure(values, res.getFailures(obj4, null), false);

        values = new String[] { "null" };
        this.privateTestGetFailure(values, res.getFailures(null, null), false);
        this.privateTestGetFailure(values, res.getFailures(null), false);

        values = new String[] {
        };
        this.privateTestGetFailure(values, res.getFailures(obj6, null), false);
        this.privateTestGetFailure(values, res.getFailures(obj6, "foobar"), false);
    }

    private void privateTestGetFailure(String[] values, List fails, boolean sort) {
        assertEquals(values.length, fails.size());

        if (sort) {
            Arrays.sort(values);
            Collections.sort(fails, new Comparator() {
                public int compare(Object o1, Object o2) {
                    ValidationFailure f1 = (ValidationFailure) o1;
                    ValidationFailure f2 = (ValidationFailure) o2;
                    return f1.getDescription().compareTo(f2.getDescription());
                }
            });
        }

        for (int i = 0; i < values.length; i++) {
            ValidationFailure failure = (ValidationFailure) fails.get(i);
            assertEquals(values[i], failure.getDescription());
        }
    }

    public void testEmpty() {
        res = new ValidationResult();
        assertFalse(res.hasFailures());

        assertFalse(res.hasFailures(obj1));
        assertFalse(res.hasFailures(null));

        assertFalse(res.hasFailure(obj1, "property"));
        assertFalse(res.hasFailure(obj1, null));
        assertFalse(res.hasFailure(null, null));
    }

    public void testAsserts() {
        this.addFailures();

        try {
            res.addFailure(null);
            fail();
        }
        catch (IllegalArgumentException e) {
        }
        try {
            res.addFailure(null, "property", "message");
            fail();
        }
        catch (IllegalArgumentException e) {
        }
        try {
            res.hasFailure(null, "property");
            fail();
        }
        catch (IllegalArgumentException e) {
        }
    }

}
