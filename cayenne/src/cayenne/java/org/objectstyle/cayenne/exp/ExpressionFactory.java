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

package org.objectstyle.cayenne.exp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/** 
 * Helper class to build expressions. 
 * 
 * @author Andrei Adamchik
 */
public class ExpressionFactory {
    private static Logger logObj = Logger.getLogger(ExpressionFactory.class);

    private static Class[] typeLookup;

    static {
        // make sure all types are small integers, then we can use
        // them as indexes in lookup array
        int[] allTypes =
            new int[] {
                Expression.AND,
                Expression.OR,
                Expression.NOT,
                Expression.EQUAL_TO,
                Expression.NOT_EQUAL_TO,
                Expression.LESS_THAN,
                Expression.GREATER_THAN,
                Expression.LESS_THAN_EQUAL_TO,
                Expression.GREATER_THAN_EQUAL_TO,
                Expression.BETWEEN,
                Expression.IN,
                Expression.LIKE,
                Expression.LIKE_IGNORE_CASE,
                Expression.EXISTS,
                Expression.ADD,
                Expression.SUBTRACT,
                Expression.MULTIPLY,
                Expression.DIVIDE,
                Expression.NEGATIVE,
                Expression.POSITIVE,
                Expression.ALL,
                Expression.SOME,
                Expression.ANY,
                Expression.RAW_SQL,
                Expression.OBJ_PATH,
                Expression.DB_PATH,
                Expression.LIST,
                Expression.SUBQUERY,
                Expression.COUNT,
                Expression.SUM,
                Expression.AVG,
                Expression.MIN,
                Expression.MAX };

        int max = 0;
        int min = 0;
        int allLen = allTypes.length;
        for (int i = 0; i < allLen; i++) {
            if (allTypes[i] > max)
                max = allTypes[i];
            else if (allTypes[i] < min)
                min = allTypes[i];
        }

        // sanity check....
        if (max > 500)
            throw new RuntimeException("Types values are too big: " + max);
        if (min < 0)
            throw new RuntimeException("Types values are too small: " + min);

        // now we know that if types are used as indexes, 
        // they will fit in array "max + 1" long (though gaps are possible)

        typeLookup = new Class[max + 1];

        // list types
        typeLookup[Expression.AND] = ListExpression.class;
        typeLookup[Expression.OR] = ListExpression.class;

        // ternary types
        typeLookup[Expression.BETWEEN] = TernaryExpression.class;

        // binary types
        typeLookup[Expression.EQUAL_TO] = BinaryExpression.class;
        typeLookup[Expression.NOT_EQUAL_TO] = BinaryExpression.class;
        typeLookup[Expression.LESS_THAN] = BinaryExpression.class;
        typeLookup[Expression.GREATER_THAN] = BinaryExpression.class;
        typeLookup[Expression.LESS_THAN_EQUAL_TO] = BinaryExpression.class;
        typeLookup[Expression.GREATER_THAN_EQUAL_TO] = BinaryExpression.class;
        typeLookup[Expression.IN] = BinaryExpression.class;
        typeLookup[Expression.LIKE] = BinaryExpression.class;
        typeLookup[Expression.LIKE_IGNORE_CASE] = BinaryExpression.class;
        typeLookup[Expression.ADD] = BinaryExpression.class;
        typeLookup[Expression.SUBTRACT] = BinaryExpression.class;
        typeLookup[Expression.MULTIPLY] = BinaryExpression.class;
        typeLookup[Expression.DIVIDE] = BinaryExpression.class;

        // unary types
        typeLookup[Expression.EXISTS] = UnaryExpression.class;
        typeLookup[Expression.NOT] = UnaryExpression.class;
        typeLookup[Expression.NEGATIVE] = UnaryExpression.class;
        typeLookup[Expression.POSITIVE] = UnaryExpression.class;
        typeLookup[Expression.ALL] = UnaryExpression.class;
        typeLookup[Expression.SOME] = UnaryExpression.class;
        typeLookup[Expression.ANY] = UnaryExpression.class;
        typeLookup[Expression.RAW_SQL] = UnaryExpression.class;
        typeLookup[Expression.OBJ_PATH] = UnaryExpression.class;
        typeLookup[Expression.DB_PATH] = UnaryExpression.class;
        typeLookup[Expression.LIST] = UnaryExpression.class;
        typeLookup[Expression.SUBQUERY] = UnaryExpression.class;
        typeLookup[Expression.SUM] = UnaryExpression.class;
        typeLookup[Expression.AVG] = UnaryExpression.class;
        typeLookup[Expression.COUNT] = UnaryExpression.class;
        typeLookup[Expression.MIN] = UnaryExpression.class;
        typeLookup[Expression.MAX] = UnaryExpression.class;
    }

    /** 
     * Creates a new expression for the type requested. 
     * If type is unknown, ExpressionException is thrown. 
     */
    public static Expression expressionOfType(int type) {
        if (type < 0 || type >= typeLookup.length) {
            throw new ExpressionException("Bad expression type: " + type);
        }

        if (typeLookup[type] == null) {
            throw new ExpressionException("Bad expression type: " + type);
        }

        if (BinaryExpression.class == typeLookup[type]) {
            return new BinaryExpression(type);
        }

        if (UnaryExpression.class == typeLookup[type]) {
            return new UnaryExpression(type);
        }

        if (TernaryExpression.class == typeLookup[type]) {
            return new TernaryExpression(type);
        }

        if (ListExpression.class == typeLookup[type]) {
            return new ListExpression(type);
        }

        throw new ExpressionException("Bad expression type: " + type);
    }

    /**
     * Applies a few default rules for adding operands to
     * expressions. In particular wraps all lists into LIST
     * expressions. Applied only in path expressions.
     */
    protected static Object wrapPathOperand(Object op) {
        if ((op instanceof List) || (op instanceof Object[])) {
            return unaryExp(Expression.LIST, op);
        } else {
            return op;
        }
    }

    /** Creates a unary expression. Unary expression is an expression with only single operand.
     * <code>type</code> must be a valid type defined in Expression interface. It must also
     * resolve to a unary expression, or an ExpressionException will be thrown. 
     *
     * <p>An example of valid unary expression is "negative" (Expression.NEGATIVE). 
     * Important Cayenne unary expression type is Expression.OBJ_PATH. It specifies 
     * an property or an object related to an object (or an attribute or an entity related to
     * an entity) using DataMap terms. 
     */
    public static Expression unaryExp(int type, Object operand) {
        Expression exp = expressionOfType(type);
        if (!(exp instanceof UnaryExpression))
            throw new ExpressionException("Bad unary expression type: " + type);

        exp.setOperand(0, operand);
        return exp;
    }

    public static Expression listExp(int type, List operands) {
        Expression exp = expressionOfType(type);
        if (!(exp instanceof ListExpression)) {
            throw new ExpressionException("Bad list expression type: " + type);
        }

        ((ListExpression) exp).appendOperands(operands);
        return exp;
    }

    /** 
     * Creates a binary expression. Binary expression is an expression 
     * with two operands. <code>type</code> must be a valid type defined 
     * in Expression interface. It must also resolve to a binary expression, 
     * or an ExpressionException will be thrown. 
     */
    public static Expression binaryExp(
        int type,
        Object leftOperand,
        Object rightOperand) {
        Expression exp = expressionOfType(type);
        if (!(exp instanceof BinaryExpression)) {
            // if this is AND/OR expression, provide a temporary workaround
            if (type == Expression.AND || type == Expression.OR) {
                logObj.warn(
                    "Use of AND/OR Expressions as 'binary' is deprecated, "
                        + "use ListExpression instead.");

                List list = new ArrayList();
                list.add(leftOperand);
                list.add(rightOperand);
                return listExp(type, list);
            }

            throw new ExpressionException(
                "Bad binary expression type: " + type);
        }

        exp.setOperand(0, leftOperand);
        exp.setOperand(1, rightOperand);
        return exp;
    }

    /** 
     * Creates a binary expression with left operand being Expression.OBJ_PATH. 
     * This is a more useful method to build binary expressions then 
     * generic {@link #binaryExp(int, Object, Object) binaryExp}.
     * It is intended for cases when one of the operands is OBJ_PATH. 
     * This method wraps <code>pathSpec</code> string representing the path in a 
     * {@link org.objectstyle.cayenne.exp.Expression#OBJ_PATH OBJ_PATH} expression 
     * and uses it as a left operand of binary expression.
     *
     * @see #binaryExp(int, Object, Object)
     */
    public static Expression binaryPathExp(
        int type,
        String pathSpec,
        Object value) {
        return binaryExp(
            type,
            unaryExp(Expression.OBJ_PATH, pathSpec),
            wrapPathOperand(value));
    }

    /** 
     * Creates a binary expression with left operand being 
     * Expression.DB_PATH. This is a useful method to build 
     * binary expressions that match DbAttributes not included 
     * in the entities (for instance primary keys).
     *
     * @see #binaryExp(int, Object, Object)
     */
    public static Expression binaryDbPathExp(
        int type,
        String pathSpec,
        Object value) {
        return binaryExp(
            type,
            unaryExp(Expression.DB_PATH, pathSpec),
            wrapPathOperand(value));
    }

    /** 
     * Creates a ternary expression. Ternary expression is an expression with three operands.
     * <code>type</code> must be a valid type defined in Expression interface. It must also
     * resolve to a ternary expression, or an ExpressionException will be thrown.
     *
     * <p>Example of ternary expression type is Expression.BETWEEN.</p>
     */
    public static Expression ternaryExp(
        int type,
        Object firstOperand,
        Object secondOperand,
        Object thirdOperand) {
        Expression exp = expressionOfType(type);
        if (!(exp instanceof TernaryExpression))
            throw new ExpressionException(
                "Bad ternary expression type: " + type);

        exp.setOperand(0, firstOperand);
        exp.setOperand(1, secondOperand);
        exp.setOperand(2, thirdOperand);
        return exp;
    }

    /** 
     * Creates an expression that matches any of the key-values pairs in <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a DB_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. OR is used to join pair binary expressions.
     */
    public static Expression matchAnyDbExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            pairs.add(binaryDbPathExp(pairType, key, value));
        }

        return joinExp(Expression.OR, pairs);
    }

    /** 
     * Creates an expression that matches all key-values pairs in <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a DB_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. AND is used to join pair binary expressions.
     */
    public static Expression matchAllDbExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            pairs.add(binaryDbPathExp(pairType, key, value));
        }

        return joinExp(Expression.AND, pairs);
    }

    /** 
     * Creates an expression that matches any of the key-values pairs in the <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a OBJ_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. OR is used to join pair binary expressions.
     */
    public static Expression matchAnyExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            pairs.add(binaryPathExp(pairType, key, value));
        }

        return joinExp(Expression.OR, pairs);
    }

    /** 
     * Creates an expression that matches all key-values pairs in <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a OBJ_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. AND is used to join pair binary expressions.
     */
    public static Expression matchAllExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            pairs.add(binaryPathExp(pairType, key, value));
        }

        return joinExp(Expression.AND, pairs);
    }

    /**
     * An shortcut for <code>binaryDbNameExp(Expression.EQUAL_TO, pathSpec, value)</code>.
     */
    public static Expression matchDbExp(String pathSpec, Object value) {
        return binaryDbPathExp(Expression.EQUAL_TO, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.EQUAL_TO,
     * pathSpec, value)
     * </code>.
     */
    public static Expression matchExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.EQUAL_TO, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.NOT_EQUAL_TO,
     * pathSpec, value)
     * </code>.
     */
    public static Expression noMatchExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.NOT_EQUAL_TO, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.LESS_THAN,
     * pathSpec, value)
     * </code>.
     */
    public static Expression lessExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.LESS_THAN, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.LESS_THAN_EQUAL_TO,
     * pathSpec, value)
     * </code>.
     */
    public static Expression lessOrEqualExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.LESS_THAN_EQUAL_TO, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.GREATER_THAN,
     * pathSpec, value)
     * </code>.
     */
    public static Expression greaterExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.GREATER_THAN, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.GREATER_THAN_EQUAL_TO,
     * pathSpec, value)
     * </code>.
     */
    public static Expression greaterOrEqualExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.GREATER_THAN_EQUAL_TO, pathSpec, value);
    }

    /**
     * A convenience shortcut for building IN expression.
     */
    public static Expression inExp(String pathSpec, Object[] values) {
        return binaryPathExp(Expression.IN, pathSpec, wrapPathOperand(values));
    }

    /**
     * A convenience shortcut for building IN expression.
     */
    public static Expression inExp(String pathSpec, List values) {
        return binaryPathExp(Expression.IN, pathSpec, wrapPathOperand(values));
    }

    /**
     * A convenience shortcut for building BETWEEN expressions.
     */
    public static Expression betweenExp(
        String pathSpec,
        Object value1,
        Object value2) {
        Expression path = unaryExp(Expression.OBJ_PATH, pathSpec);
        return ternaryExp(Expression.BETWEEN, path, value1, value2);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.LIKE, pathSpec,
     * value)</code>.
     */
    public static Expression likeExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.LIKE, pathSpec, value);
    }

    /**
     * A convenience shortcut for <code>binaryPathExp(Expression.
     * LIKE_IGNORE_CASE, pathSpec, value)</code>.
     */
    public static Expression likeIgnoreCaseExp(String pathSpec, Object value) {
        return binaryPathExp(Expression.LIKE_IGNORE_CASE, pathSpec, value);
    }
    

    /** 
     * Joins all <code>expressions</code> in a single expression. 
     * <code>type</code> is used as an expression type for expressions joining 
     * each one of the items on the list. <code>type</code> must be binary 
     * expression type.
     * 
     * <p>For example, if type is Expression.AND, resulting expression would match 
     * all expressions in the list. If type is Expression.OR, resulting expression 
     * would match any of the expressions. </p>
     */
    public static Expression joinExp(int type, List expressions) {
        int len = expressions.size();
        if (len == 0)
            return null;

        Expression currentExp = (Expression) expressions.get(0);
        if (len == 1) {
            return currentExp;
        }

        ListExpression exp = new ListExpression(type);
        exp.appendOperands(expressions);
        return exp;
    }
}