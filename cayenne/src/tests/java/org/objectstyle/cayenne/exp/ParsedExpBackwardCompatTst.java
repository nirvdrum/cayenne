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
package org.objectstyle.cayenne.exp;

import java.util.Arrays;
import java.util.List;

import org.objectstyle.cayenne.unittest.CayenneSimpleTestCase;

/**
 * Tests compatibility of the new expressions package with the old one.
 * 
 * @author Andrei Adamchik
 */
public class ParsedExpBackwardCompatTst extends CayenneSimpleTestCase {

    public void testOr() throws Exception {
        Expression compareTo = simpleExp().orExp(simpleExp());
        Expression parsed = Expression.fromString("a = 'b' or a = 'b'");
        assertEquals(compareTo, parsed);

        compareTo = compareTo.orExp(simpleExp());
        parsed = Expression.fromString("a = 'b' or a = 'b' or a = 'b'");
        assertEquals(compareTo, parsed);
    }

    public void testAnd() throws Exception {
        Expression compareTo = simpleExp().andExp(simpleExp());
        Expression parsed = Expression.fromString("a = 'b' and a = 'b'");
        assertEquals(compareTo, parsed);

        compareTo = compareTo.andExp(simpleExp());
        parsed = Expression.fromString("a = 'b' and a = 'b' and a = 'b'");
        assertEquals(compareTo, parsed);
    }

    public void testNot() throws Exception {
        Expression compareTo = simpleExp().notExp();
        Expression parsed1 = Expression.fromString("not a = 'b'");
        Expression parsed2 = Expression.fromString("! a = 'b'");
        assertEquals(compareTo, parsed1);
        assertEquals(compareTo, parsed2);
    }

    public void testEqual() throws Exception {
        Expression compareTo = simpleExp(Expression.EQUAL_TO, new Integer("3"));
        Expression parsed1 = Expression.fromString("a = 3");
        Expression parsed2 = Expression.fromString("a == 3");
        assertEquals(compareTo, parsed1);
        assertEquals(compareTo, parsed2);
    }

    public void testNotEqual() throws Exception {
        Expression compareTo = simpleExp(Expression.NOT_EQUAL_TO, new Integer("3"));
        Expression parsed1 = Expression.fromString("a != 3");
        Expression parsed2 = Expression.fromString("a <> 3");
        assertEquals(compareTo, parsed1);
        assertEquals(compareTo, parsed2);
    }

    public void testLessThan() throws Exception {
        Expression compareTo = simpleExp(Expression.LESS_THAN, new Integer("3"));
        Expression parsed1 = Expression.fromString("a < 3");
        assertEquals(compareTo, parsed1);
    }

    public void testLessThanEqualTo() throws Exception {
        Expression compareTo = simpleExp(Expression.LESS_THAN_EQUAL_TO, new Integer("3"));
        Expression parsed1 = Expression.fromString("a <= 3");
        assertEquals(compareTo, parsed1);
    }

    public void testGreaterThan() throws Exception {
        Expression compareTo = simpleExp(Expression.GREATER_THAN, new Integer("3"));
        Expression parsed1 = Expression.fromString("a > 3");
        assertEquals(compareTo, parsed1);
    }

    public void testGreaterThanEqualTo() throws Exception {
        Expression compareTo =
            simpleExp(Expression.GREATER_THAN_EQUAL_TO, new Integer("3"));
        Expression parsed1 = Expression.fromString("a >= 3");
        assertEquals(compareTo, parsed1);
    }

    public void testLike() throws Exception {
        Expression compareTo = simpleExp(Expression.LIKE, "a%b");
        Expression parsed1 = Expression.fromString("a like 'a%b'");
        assertEquals(compareTo, parsed1);
    }

    public void testLikeIgnoreCase() throws Exception {
        Expression compareTo = simpleExp(Expression.LIKE_IGNORE_CASE, "a%b");
        Expression parsed1 = Expression.fromString("a likeIgnoreCase 'a%b'");
        assertEquals(compareTo, parsed1);
    }

    public void testNotLike() throws Exception {
        Expression compareTo = simpleExp(Expression.NOT_LIKE, "a%b");
        Expression parsed1 = Expression.fromString("a not like 'a%b'");
        assertEquals(compareTo, parsed1);
    }

    public void testNotLikeIgnoreCase() throws Exception {
        Expression compareTo = simpleExp(Expression.NOT_LIKE_IGNORE_CASE, "a%b");
        Expression parsed1 = Expression.fromString("a not likeIgnoreCase 'a%b'");
        assertEquals(compareTo, parsed1);
    }

    public void testIn() throws Exception {
        List list =
            Arrays.asList(new Object[] { new Integer(5), new Integer(2), new Integer(6)});
        Expression compareTo = simpleExp(Expression.IN, list);
        Expression parsed1 = Expression.fromString("a in (5, 2, 6)");
        assertEquals(compareTo, parsed1);
    }

    public void testNotIn() throws Exception {
        List list =
            Arrays.asList(new Object[] { new Integer(5), new Integer(2), new Integer(6)});
        Expression compareTo = simpleExp(Expression.NOT_IN, list);
        Expression parsed1 = Expression.fromString("a not in (5, 2, 6)");
        assertEquals(compareTo, parsed1);
    }

    public void testBetween() throws Exception {
        Expression compareTo =
            simpleExp(Expression.BETWEEN, new Integer(2), new Integer(6));
        Expression parsed1 = Expression.fromString("a between 2 and 6");
        assertEquals(compareTo, parsed1);
    }

    public void testNotBetween() throws Exception {
        Expression compareTo =
            simpleExp(Expression.NOT_BETWEEN, new Integer(2), new Integer(6));
        Expression parsed1 = Expression.fromString("a not between 2 and 6");
        assertEquals(compareTo, parsed1);
    }

    public void testParameter() throws Exception {
        Expression compareTo =
            simpleExp(Expression.EQUAL_TO, new ExpressionParameter("param1"));
        Expression parsed1 = Expression.fromString("a =$param1");
        assertEquals(compareTo, parsed1);
    }

    public void testDbExpression() throws Exception {
        Expression compareTo = simpleDbExp(Expression.EQUAL_TO, new Integer("5"));
        Expression parsed1 = Expression.fromString("db:a = 5");
        assertEquals(compareTo, parsed1);
    }

    public void testFloatExpression() throws Exception {
        Expression compareTo = simpleExp(Expression.EQUAL_TO, new Double(3.33));
        Expression parsed1 = Expression.fromString("a = 3.33");
        assertEquals(compareTo, parsed1);
    }

    public void testNullExpression() throws Exception {
        Expression compareTo = simpleExp(Expression.EQUAL_TO, null);
        Expression parsed1 = Expression.fromString("a = null");
        Expression parsed2 = Expression.fromString("a = NULL");
        assertEquals(compareTo, parsed1);
        assertEquals(compareTo, parsed2);
    }

    /**
     * Creates a test fixture out of old expression objects, 
     * bypassing ExpressionFactory, since ExpressionFactory is being
     * switched to the new expressions format.
     */
    private Expression simpleExp() {
        return simpleExp(Expression.EQUAL_TO);
    }

    /**
     * Creates a test fixture out of old expression objects, 
     * bypassing ExpressionFactory, since ExpressionFactory is being
     * switched to the new expressions format.
     */
    private Expression simpleExp(int type) {
        return simpleExp(type, "b");
    }

    /**
     * Creates a test fixture out of old expression objects, 
     * bypassing ExpressionFactory, since ExpressionFactory is being
     * switched to the new expressions format.
     */
    private Expression simpleExp(int type, Object value) {
        UnaryExpression path = new UnaryExpression(Expression.OBJ_PATH);
        path.setOperand(0, "a");

        BinaryExpression exp = new BinaryExpression(type);
        exp.setOperand(0, path);
        exp.setOperand(1, ExpressionFactory.wrapPathOperand(value));
        return exp;
    }

    /**
     * Creates a test fixture out of old expression objects, 
     * bypassing ExpressionFactory, since ExpressionFactory is being
     * switched to the new expressions format.
     */
    private Expression simpleExp(int type, Object value1, Object value2) {
        UnaryExpression path = new UnaryExpression(Expression.OBJ_PATH);
        path.setOperand(0, "a");

        TernaryExpression exp = new TernaryExpression(type);
        exp.setOperand(0, path);
        exp.setOperand(1, value1);
        exp.setOperand(2, value2);
        return exp;
    }

    /**
     * Creates a test fixture out of old expression objects, 
     * bypassing ExpressionFactory, since ExpressionFactory is being
     * switched to the new expressions format.
     */
    private Expression simpleDbExp(int type, Object value) {
        UnaryExpression path = new UnaryExpression(Expression.DB_PATH);
        path.setOperand(0, "a");

        BinaryExpression exp = new BinaryExpression(type);
        exp.setOperand(0, path);
        exp.setOperand(1, ExpressionFactory.wrapPathOperand(value));
        return exp;
    }
}
