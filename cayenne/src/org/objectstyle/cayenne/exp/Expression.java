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

/** Defines basic API of a generic data expression. */
public abstract class Expression {
    
    /** Corresponds to SQL "A AND B" expression. */
    public static final int AND = 0;
    
    /** Corresponds to SQL "A OR B" expression. */
    public static final int OR = 1;
    
    /** Corresponds to SQL "NOT A" expression. */
    public static final int NOT = 2;
    
    /** Corresponds to SQL "A = B" expression. */
    public static final int EQUAL_TO = 3;
    
    /** Corresponds to SQL 'not equals' expression. 
     *  Will be translated to an expression similar to <code>A != B</code> (or <code>A <> B</code>). */
    public static final int NOT_EQUAL_TO = 4;
    public static final int LESS_THAN = 5;
    public static final int GREATER_THAN = 6;
    public static final int LESS_THAN_EQUAL_TO = 7;
    public static final int GREATER_THAN_EQUAL_TO = 8;
    public static final int BETWEEN = 9;
    public static final int IN = 10;
    public static final int LIKE = 11;
    public static final int LIKE_IGNORE_CASE = 12;
    public static final int EXISTS = 15;
    public static final int ADD = 16;
    public static final int SUBTRACT = 17;
    public static final int MULTIPLY = 18;
    public static final int DIVIDE = 19;
    public static final int NEGATIVE = 20;
    public static final int POSITIVE = 21;
    public static final int ALL = 22;
    public static final int SOME = 23;
    public static final int ANY = 24;
    
    /** Expression interpreted as raw SQL. 
    * No translations will be done for this kind of expressions. */ 
    public static final int RAW_SQL = 25;
    
    
    /** Expression describes a path relative to an ObjEntity.
    * OBJ_PATH expression is resolved relative to some root ObjEntity. Path expression components
    * are separated by "." (dot). Path can point to either one of these:
    * <ul>
    * <li> <i>An attribute of root ObjEntity.</i>
    * For entity Gallery OBJ_PATH expression "galleryName" will point to ObjAttribute "galleryName" 
    * <li> <i>Another ObjEntity related to root ObjEntity via a chain of relationships.</i>
    * For entity Gallery OBJ_PATH expression "paintingArray.toArtist" will point to ObjEntity "Artist" 
    * <li><i>ObjAttribute of another ObjEntity related to root ObjEntity via a chain of relationships.</i>
    * For entity Gallery OBJ_PATH expression "paintingArray.toArtist.artistName" will point to ObjAttribute "artistName" 
    * </ul>
    */ 
    public static final int OBJ_PATH = 26;
    
    
    /** 
     * Describes a table column name.
     * DB_NAME expression is resolved relative to a root 
     * DbEntity. 
     */ 
    public static final int DB_NAME = 27;
    
    
    /** Interpreted as a comma-separated list of literals. */ 
    public static final int LIST = 28;
    /** Interpreted as a subquery within a parent query. */ 
    public static final int SUBQUERY = 29;
    /** Interpreted as an aggregate count function. */ 
    public static final int COUNT = 30;
    /** Interpreted as an aggregate avg function. */ 
    public static final int AVG = 31;
    /** Interpreted as an aggregate sum function. */ 
    public static final int SUM = 32;
    /** Interpreted as an aggregate max function. */ 
    public static final int MAX = 33;
    /** Interpreted as an aggregate min function. */ 
    public static final int MIN = 34;
    
    protected int type;
    
    /** 
     * Returns a type of expression. Most common types are defined 
     * as public static fields of this interface.
     */
	public int getType() {
        return type;
    }

	public void setType(int type) {
        this.type = type;
    }

    /** 
     * Creates a new expression that joins this object
     * with another expression, using specified join type.
     * It is very useful for incrementally building chained expressions,
     * like long AND or OR statements. 
     */
    public Expression joinExp(int type, Expression exp) {
         Expression join = ExpressionFactory.expressionOfType(type);
         join.setOperand(0, this);
         join.setOperand(1, exp);
         return join;
    }
    
    /**
     * A shortcut for <code>joinExp(Expression.AND, exp)</code>.
     */
    public Expression andExp(Expression exp) {
    	return joinExp(Expression.AND, exp);
    }
    
    /**
     * A shortcut for <code>joinExp(Expression.OR, exp)</code>.
     */
    public Expression orExp(Expression exp) {
    	return joinExp(Expression.OR, exp);
    }
    
    
    /** 
     * Returns a count of operands of this expression. In real life there are
     * unary (count == 1), binary (count == 2) and ternary (count == 3) 
     * expressions.
     */
    public abstract int getOperandCount();
    
    /** 
     * Returns a value of operand at <code>index</code>. 
     * Operand indexing starts at 0. 
     */
    public abstract Object getOperand(int index);
    
    
    /** 
     * Sets a value of operand at <code>index</code>. 
     * Operand indexing starts at 0.
     */
    public abstract void setOperand(int index, Object value);
}
