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
package org.objectstyle.cayenne.exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ParametrizedExpressionTst extends CayenneTestCase {

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     * 
     * @throws Exception
     */
    public void testCopy1() throws Exception {
        Expression e1 = ExpressionFactory.matchExp("k1", "v1");
        e1 = e1.orExp(ExpressionFactory.matchExp("k2", "v2"));
        e1 = e1.orExp(ExpressionFactory.matchExp("k3", "v3"));

        Expression e2 = e1.expWithParameters(new HashMap(), true);

        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     *
     * @throws Exception
     */
    public void testCopy2() throws Exception {
        Expression andExp = ExpressionFactory.matchExp("k1", "v1");
        andExp = andExp.andExp(ExpressionFactory.matchExp("k2", "v2"));
        andExp = andExp.andExp(ExpressionFactory.matchExp("k3", "v3"));

        List exprs = new ArrayList();
        exprs.add(andExp);
        exprs.add(ExpressionFactory.matchExp("k1", "v1"));

        Expression e1 = ExpressionFactory.joinExp(Expression.OR, exprs);
        Expression e2 = e1.expWithParameters(new HashMap(), true);

        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     *
     * @throws Exception
     */
    public void testFailOnMissingParams() throws Exception {
        Expression e1 =
            ExpressionFactory.matchExp("k1", new ExpressionParameter("test"));
        e1 = e1.orExp(ExpressionFactory.matchExp("k2", "v2"));
        e1 = e1.orExp(ExpressionFactory.matchExp("k3", "v3"));

        try {
            e1.expWithParameters(new HashMap(), false);
            fail("Parameter was missing, but no exception was thrown.");
        } catch (ExpressionException ex) {
            // exception expected
        }
    }

    public void testParams1() throws Exception {
        Expression e1 =
            ExpressionFactory.matchExp("k1", new ExpressionParameter("test"));

        Map map = new HashMap();
        map.put("test", "xyz");
        Expression e2 = e1.expWithParameters(map, false);
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals(Expression.EQUAL_TO, e2.getType());
        assertEquals("xyz", e2.getOperand(1));
    }

    public void testNoParams1() throws Exception {
        Expression e1 =
            ExpressionFactory.matchExp("k1", new ExpressionParameter("test"));

        Expression e2 = e1.expWithParameters(new HashMap(), true);

        // all expression nodes must be pruned
        assertNull(e2);
    }

    public void testNoParams2() throws Exception {
        List list = new ArrayList();
        list.add(
            ExpressionFactory.matchExp("k1", new ExpressionParameter("test1")));
        list.add(
            ExpressionFactory.matchExp("k2", new ExpressionParameter("test2")));
        list.add(
            ExpressionFactory.matchExp("k3", new ExpressionParameter("test3")));
        list.add(
            ExpressionFactory.matchExp("k4", new ExpressionParameter("test4")));
        Expression e1 = ExpressionFactory.joinExp(Expression.OR, list);

        Map params = new HashMap();
        params.put("test2", "abc");
        params.put("test3", "xyz");
        Expression e2 = e1.expWithParameters(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertTrue(
            "Not a list expression: " + e2,
            e2 instanceof ListExpression);

        ListExpression le = (ListExpression) e2;
        assertEquals(2, le.getOperandCount());

        Expression k2 = (Expression) le.getOperand(0);
        assertEquals("abc", k2.getOperand(1));

        Expression k3 = (Expression) le.getOperand(1);
        assertEquals("xyz", k3.getOperand(1));
    }

    public void testNoParams3() throws Exception {
        List list = new ArrayList();
        list.add(
            ExpressionFactory.matchExp("k1", new ExpressionParameter("test1")));
        list.add(
            ExpressionFactory.matchExp("k2", new ExpressionParameter("test2")));
        list.add(
            ExpressionFactory.matchExp("k3", new ExpressionParameter("test3")));
        list.add(
            ExpressionFactory.matchExp("k4", new ExpressionParameter("test4")));
        Expression e1 = ExpressionFactory.joinExp(Expression.OR, list);

        Map params = new HashMap();
        params.put("test4", "123");
        Expression e2 = e1.expWithParameters(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertTrue("List expression: " + e2, !(e2 instanceof ListExpression));

        assertEquals(2, e2.getOperandCount());
        assertEquals("123", e2.getOperand(1));
        assertEquals("k4", ((Expression) e2.getOperand(0)).getOperand(0));
    }
}
