package org.objectstyle.cayenne.exp;
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

import org.objectstyle.cayenne.unittest.*;


public class ExpressionFactoryTst extends CayenneTestCase {
    // non-existent type
    private static final int badType = -50;
    
    
    public ExpressionFactoryTst(String name) {
        super(name);
    }
    
    public void testUnaryExp() throws Exception {
        Object o1 = new Object();
        Expression e1 = ExpressionFactory.unaryExp(Expression.NOT, o1);
        assertTrue(e1 instanceof UnaryExpression);
        assertSame(o1, e1.getOperand(0));
        assertEquals(Expression.NOT, e1.getType());
    }
    
    
    public void testBinaryExp() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();
        Expression e1 = ExpressionFactory.binaryExp(Expression.AND, o1, o2);
        assertTrue(e1 instanceof BinaryExpression);
        assertSame(o1, e1.getOperand(0));
        assertSame(o2, e1.getOperand(1));
        assertEquals(Expression.AND, e1.getType());
    }
    
    public void testTernaryExp() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        Expression e1 = ExpressionFactory.ternaryExp(Expression.BETWEEN, o1, o2, o3);
        assertTrue(e1 instanceof TernaryExpression);
        assertSame(o1, e1.getOperand(0));
        assertSame(o2, e1.getOperand(1));
        assertSame(o3, e1.getOperand(2));
        assertEquals(Expression.BETWEEN, e1.getType());
    }
    
    
    public void testExpressionOfType() throws java.lang.Exception {
        assertTrue(ExpressionFactory.expressionOfType(Expression.AND) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.OR) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.NOT) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.EQUAL_TO) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.LESS_THAN) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.GREATER_THAN) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.LESS_THAN_EQUAL_TO) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.GREATER_THAN_EQUAL_TO) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.BETWEEN) instanceof TernaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.IN) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.LIKE) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.LIKE_IGNORE_CASE) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.EXISTS) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.ADD) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.SUBTRACT) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.MULTIPLY) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.DIVIDE) instanceof BinaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.NEGATIVE) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.POSITIVE) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.ALL) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.SOME) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.ANY) instanceof UnaryExpression);
        
        assertTrue(ExpressionFactory.expressionOfType(Expression.RAW_SQL) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.OBJ_PATH) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.DB_PATH) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.LIST) instanceof UnaryExpression);
        
        assertTrue(ExpressionFactory.expressionOfType(Expression.SUBQUERY) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.COUNT) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.AVG) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.SUM) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.MAX) instanceof UnaryExpression);
        assertTrue(ExpressionFactory.expressionOfType(Expression.MIN) instanceof UnaryExpression);

    }
    
    
    public void testExpressionOfBadType() throws java.lang.Exception {
        try {
            ExpressionFactory.expressionOfType(badType);
            fail();
        }
        catch(ExpressionException ex) {
            // exception expected   
        }
    }
}
