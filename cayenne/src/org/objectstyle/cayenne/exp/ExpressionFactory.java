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

import java.util.*;

/** 
 * Helper class to build expressions. 
 * 
 * @author Andrei Adamchik
 */
public class ExpressionFactory {    
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
                Expression.DB_NAME,
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

        // ternary types
        typeLookup[Expression.BETWEEN] = TernaryExpression.class;

        // binary types
        typeLookup[Expression.AND] = BinaryExpression.class;
        typeLookup[Expression.OR] = BinaryExpression.class;
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
        typeLookup[Expression.DB_NAME] = UnaryExpression.class;
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
        if (type < 0 || type >= typeLookup.length)
            throw new ExpressionException("Bad expression type: " + type);

        if (typeLookup[type] == null)
            throw new ExpressionException("Bad expression type: " + type);

        if (BinaryExpression.class == typeLookup[type])
            return new BinaryExpression(type);

        if (UnaryExpression.class == typeLookup[type])
            return new UnaryExpression(type);

        if (TernaryExpression.class == typeLookup[type])
            return new TernaryExpression(type);

        throw new ExpressionException("Bad expression type: " + type);
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
        if (!(exp instanceof BinaryExpression))
            throw new ExpressionException("Bad binary expression type: " + type);

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
        return binaryExp(type, unaryExp(Expression.OBJ_PATH, pathSpec), value);
    }
    
    /** 
     * Creates a binary expression with left operand being Expression.DB_NAME. 
     * This is a useful method to build binary expressions that match DbAttributes
     * not included in the entities (for instance primary keys).
     *
     * @see #binaryExp(int, Object, Object)
     */
    public static Expression binaryDbNameExp(
        int type,
        String pathSpec,
        Object value) {
        return binaryExp(type, unaryExp(Expression.DB_NAME, pathSpec), value);
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
            throw new ExpressionException("Bad ternary expression type: " + type);

        exp.setOperand(0, firstOperand);
        exp.setOperand(1, secondOperand);
        exp.setOperand(2, thirdOperand);
        return exp;
    }

    /** Creates an expression that matches all key-values pairs in <code>map</code>.
      * 
      * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
      * Key is considered to be a OBJ_PATH expression. Therefore all keys must be java.lang.String
      * objects, or ClassCastException is thrown. AND is used to join pair binary expressions.
      */
    public static Expression matchAllExp(Map map, int pairType) {
        ArrayList pairs = new ArrayList();

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            pairs.add(binaryPathExp(pairType, key, value));
        }

        return joinExp(Expression.AND, pairs);
    }

    /** Joins all <code>expressions</code> in a single expression. 
     * <code>type</code> is used as an expression type for expressions joining each one of 
     * the items on the list. <code>type</code> must be binary expression type 
     *  For example, if type is Expression.AND, resulting expression would match all expressions
     *  in the list. If type is Expression.OR, resulting expression would match any of the expressions. */
    public static Expression joinExp(int type, List expressions) {
        int len = expressions.size();
        if (len == 0)
            return null;

        Expression currentExp = (Expression) expressions.get(0);
        if (len == 1)
            return currentExp;

        for (int i = 1; i < len; i++) {
            Expression exp = expressionOfType(type);
            exp.setOperand(0, currentExp);
            // cast to expression, so that invalid entries will not go through
            exp.setOperand(1, (Expression) expressions.get(i));
            currentExp = exp;
        }

        return currentExp;
    }   
}