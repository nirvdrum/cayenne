/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/** Defines basic API of a generic data expression. */
public abstract class Expression implements Serializable {
    private static Logger logObj = Logger.getLogger(Expression.class);

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

    /** 
     * Expression describes a path relative to an ObjEntity.
     * OBJ_PATH expression is resolved relative to some root ObjEntity. Path expression components
     * are separated by "." (dot). Path can point to either one of these:
     * <ul>
     *    <li><i>An attribute of root ObjEntity.</i>
     *    For entity Gallery OBJ_PATH expression "galleryName" will point to ObjAttribute "galleryName" 
     *    <li><i>Another ObjEntity related to root ObjEntity via a chain of relationships.</i>
     *    For entity Gallery OBJ_PATH expression "paintingArray.toArtist" will point to ObjEntity "Artist" 
     *    <li><i>ObjAttribute of another ObjEntity related to root ObjEntity via a chain of relationships.</i>
     *    For entity Gallery OBJ_PATH expression "paintingArray.toArtist.artistName" will point to ObjAttribute "artistName" 
     * </ul>
     */
    public static final int OBJ_PATH = 26;

    /** 
     * Expression describes a path relative to a DbEntity.
     * DB_PATH expression is resolved relative to some root DbEntity. 
     * Path expression components are separated by "." (dot). Path can 
     * point to either one of these:
     * <ul>
     *    <li><i>An attribute of root DbEntity.</i>
     *    For entity GALLERY, DB_PATH expression "GALLERY_NAME" will point 
     *    to a DbAttribute "GALLERY_NAME".
     * 	  </li>
     * 
     *    <li><i>Another DbEntity related to root DbEntity via a chain of relationships.</i>
     *    For entity GALLERY DB_PATH expression "paintingArray.toArtist" will point to 
     *    DbEntity "ARTIST".
     *    </li>
     * 
     *    <li><i>DbAttribute of another ObjEntity related to root DbEntity via a chain 
     *    of relationships.</i>
     *    For entity GALLERY DB_PATH expression "paintingArray.toArtist.ARTIST_NAME" will point 
     *    to DbAttribute "ARTIST_NAME".
     *    </li>
     * </ul>
     */
    public static final int DB_PATH = 27;

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

    public static final int NOT_BETWEEN = 35;
    public static final int NOT_IN = 36;
    public static final int NOT_LIKE = 37;
    public static final int NOT_LIKE_IGNORE_CASE = 38;

    protected int type;

    /**
     * Returns String label for this expression. Used for debugging.
     */
    public String expName() {
        switch (type) {
            case AND :
                return "AND";
            case OR :
                return "OR";
            case NOT :
                return "NOT";
            case EQUAL_TO :
                return "=";
            case NOT_EQUAL_TO :
                return "<>";
            case LESS_THAN :
                return "<";
            case LESS_THAN_EQUAL_TO :
                return "<=";
            case GREATER_THAN :
                return ">";
            case GREATER_THAN_EQUAL_TO :
                return ">=";
            case BETWEEN :
                return "BETWEEN";
            case IN :
                return "IN";
            case LIKE :
                return "LIKE";
            case LIKE_IGNORE_CASE :
                return "LIKE_IGNORE_CASE";
            case EXISTS :
                return "EXISTS";
            case OBJ_PATH :
                return "OBJ_PATH";
            case DB_PATH :
                return "DB_PATH";
            case LIST :
                return "LIST";
            case NOT_BETWEEN :
                return "NOT BETWEEN";
            case NOT_IN :
                return "NOT IN";
            case NOT_LIKE :
                return "NOT LIKE";
            case NOT_LIKE_IGNORE_CASE :
                return "NOT LIKE IGNORE CASE";
            default :
                return "other";
        }
    }

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
     * A shortcut for <code>expWithParams(params, true)</code>.
     */
    public Expression expWithParameters(Map parameters) {
        return expWithParameters(parameters, true);
    }

    /**
     * Creates and returns a new Expression instance using this expression as a
     * prototype. All ExpressionParam operands are substituted with the values
     * in the <code>params</code> map. 
     * 
     * <p><i>Null values in the <code>params</code> map should be
     * explicitly created in the map for the corresponding key.
     * </i></p>
     * 
     * @param parameters a map of parameters, with each key being a string name of
     * an expression parameter, and value being the value that should be used in
     * the final expression.
     * @param pruneMissing If <code>true</code>, subexpressions that rely
     * on missing parameters will be pruned from the resulting tree. If
     * <code>false</code>, any missing values will generate an exception.
     * 
     * @return Expression resulting from the substitution of parameters with
     * real values, or null if the whole expression was pruned, due to the
     * missing parameters.
     */
    public Expression expWithParameters(Map parameters, boolean pruneMissing) {
        ParametrizedExpressionBuilder builder =
            new ParametrizedExpressionBuilder(this, parameters, pruneMissing);
        ExpressionTraversal traversal = new ExpressionTraversal();
        traversal.setHandler(builder);
        traversal.traverseExpression(this);
        Expression newExp = builder.getExpression();

        if (logObj.isDebugEnabled()) {
            logObj.debug("Created expression: " + newExp);
            logObj.debug("  Parameters: " + parameters);
        }

        return newExp;
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
     * Returns a logical NOT of current expression.
     * 
     * @since 1.0.6
     */
    public Expression notExp() {
        return ExpressionFactory.unaryExp(Expression.NOT, this);
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

    /** 
     * Method for in-memory evaluation of expressions. 
     * 
     * @return <code>true</code> if object matches the expression,
     * <code>false</code> otherwise.
     */
    public boolean eval(Object o) {
        return ASTCompiler.compile(this).evaluateBooleanASTChain(o);
    }

    /**
     * Returns a list of objects that match the expression.
     */
    public List filterObjects(List objects) {
        if (objects == null || objects.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        int size = objects.size();
        List filtered = new ArrayList(size);

        // compile expression
        ASTNode compiled = ASTCompiler.compile(this);

        for (int i = 0; i < size; i++) {
            Object o = objects.get(i);
            if (compiled.evaluateBooleanASTChain(o)) {
                filtered.add(o);
            }
        }

        return filtered;
    }

    /**
     * Convenience method to log nested expressions. Used mainly for debugging.
     * Called from "toString".
     * 
     * @param buf
     */
    protected void toStringBuffer(StringBuffer buf) {
        for (int i = 0; i < getOperandCount(); i++) {
            if (i > 0) {
                buf.append(" ").append(expName()).append(" ");
            }

            Object op = getOperand(i);
            if (op == null) {
                buf.append("<null>");
            }
            else if (op instanceof String) {
                buf.append("'").append(op).append("'");
            }
            else if (op instanceof Expression) {
                buf.append('(');
                ((Expression) op).toStringBuffer(buf);
                buf.append(')');
            }
            else {
                buf.append(String.valueOf(op));
            }
        }

    }

    public String toString() {
        StringBuffer buf = new StringBuffer("[");
        toStringBuffer(buf);
        buf.append("]");
        return buf.toString();
    }

    /**
     * Helper class to process parameterized expressions.
     */
    class ParametrizedExpressionBuilder extends TraversalHelper {
        protected final Expression fakeTopLevelParent = new UnaryExpression();
        protected Expression proto;

        /**
         * Stores pruned children of an expression node. Key is the node itself,
         * value is a list of pruned children.
         */
        protected Map pruned = new HashMap();

        /**
         * Stores a mapping of the nodes of newly created expression back to
         * the nodes of the prototype expression.
         */
        protected Map nodeMap = new HashMap();

        protected Map params;
        protected boolean pruneMissing;

        /**
         * Constructor ParametrizedExpressionBuilder.
         * @param expression
         */
        public ParametrizedExpressionBuilder(
            Expression proto,
            Map params,
            boolean pruneMissing) {
            this.proto = proto;
            this.params = params;
            this.pruneMissing = pruneMissing;
        }

        /**
         * Creates a new expression node for the prototype expression node.
         * Stores mapping from the original expression node to the new one.
         */
        protected Expression makeExp(Expression proto) {
            Expression e = ExpressionFactory.expressionOfType(proto.getType());
            nodeMap.put(proto, e);
            return e;
        }

        /**
         * Finds an expression node corresponding to the prototype node. Throws
         * IllegalStateException if no such mapping exists.
         */
        protected Expression findExp(Expression proto) {
            Expression e = (Expression) nodeMap.get(proto);
            if (e == null) {
                throw new IllegalStateException(
                    "Can't find expression for prototype: " + proto);
            }
            return e;
        }

        protected void pruneChild(Object child, Expression parent) {
            List prunedChildren = prunedChildren(parent);
            if (prunedChildren == null) {
                prunedChildren = new ArrayList();
                pruned.put(parent, prunedChildren);
            }
            prunedChildren.add(child);
        }

        protected List prunedChildren(Expression parent) {
            return (List) pruned.get(parent);
        }

        protected void endNonListNode(Expression node, Expression parentNode) {
            // we need to prune the expression if it has pruned children
            Expression exp = findExp(node);
            if (prunedChildren(exp) != null) {
                if (logObj.isDebugEnabled()) {
                    logObj.debug("---- Prune node, since there are pruned children ----");
                    logObj.debug("  exp: " + exp);
                    logObj.debug("  children: " + prunedChildren(exp));
                }

                Expression parent =
                    (parentNode != null) ? findExp(parentNode) : fakeTopLevelParent;

                pruneChild(exp, parent);
            }
        }

        /**
         * Method getExpression.
         * @return Expression
         */
        public Expression getExpression() {
            return (prunedChildren(fakeTopLevelParent) == null) ? findExp(proto) : null;
        }

        public void startBinaryNode(Expression node, Expression parentNode) {
            makeExp(node);
        }

        public void startListNode(Expression node, Expression parentNode) {
            makeExp(node);
        }

        public void startTernaryNode(Expression node, Expression parentNode) {
            makeExp(node);
        }

        public void startUnaryNode(Expression node, Expression parentNode) {
            makeExp(node);
        }

        public void endBinaryNode(Expression node, Expression parentNode) {
            endNonListNode(node, parentNode);
        }

        public void endListNode(Expression node, Expression parentNode) {
            // see if we need to prune the expression
            ListExpression exp = (ListExpression) findExp(node);

            // prune empty list
            if (exp.getOperandCount() == 0) {
                logObj.debug("---- Prune empty list. ----");
                pruneChild(exp, findExp(parentNode));
                return;
            }

            List prunedChildren = prunedChildren(exp);

            // no children pruned
            if (prunedChildren == null || prunedChildren.size() == 0) {
                return;
            }

            // all children pruned, prune self
            if (prunedChildren.size() == exp.getOperandCount()) {
                if (logObj.isDebugEnabled()) {
                    logObj.debug(
                        "List node got all "
                            + prunedChildren.size()
                            + " children pruned.");
                }
                pruneChild(exp, findExp(parentNode));
            }

            // remove pruned children, see what remained
            Iterator it = prunedChildren.iterator();
            while (it.hasNext()) {
                exp.removeOperand(it.next());
            }

            // if only one child remained, unwrap and replace self with it
            if (exp.getOperandCount() == 1) {
                logObj.debug("List node has only one remaining child.");
                Object operand = exp.getOperand(0);
                nodeMap.put(node, operand);
            }
        }

        public void endTernaryNode(Expression node, Expression parentNode) {
            endNonListNode(node, parentNode);
        }

        public void endUnaryNode(Expression node, Expression parentNode) {
            endNonListNode(node, parentNode);
        }

        public void finishedChild(
            Expression node,
            int childIndex,
            boolean hasMoreChildren) {

            Expression parent = findExp(node);
            Object child = node.getOperand(childIndex);

            // link child to parent in the expression being built
            if (child instanceof Expression) {
                parent.setOperand(childIndex, findExp((Expression) child));
            }
            else {
                // check for parameter substitution
                if (child instanceof ExpressionParameter) {
                    ExpressionParameter param = (ExpressionParameter) child;

                    // explicitly check if key exists, since null value
                    // may simply indicate NULL
                    if (params.containsKey(param.getName())) {
                        child = params.get(param.getName());
                    }
                    else {
                        if (pruneMissing) {
                            if (logObj.isDebugEnabled()) {
                                logObj.debug("---- Prune parameter: " + param);
                            }
                            pruneChild(param, parent);
                        }
                        else {
                            throw new ExpressionException(
                                "Missing required parameter for key: " + param.getName());
                        }
                    }
                }

                parent.setOperand(childIndex, child);
            }
        }
    }
}
