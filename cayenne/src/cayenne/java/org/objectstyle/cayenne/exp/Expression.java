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

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.exp.parser.ExpressionParser;
import org.objectstyle.cayenne.exp.parser.ParseException;
import org.objectstyle.cayenne.util.ColnversionUtil;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLSerializable;

/** 
 * Superclass of Cayenne expressions that defines basic
 * API for expressions use.
 */
public abstract class Expression implements Serializable, XMLSerializable {
    private static Logger logObj = Logger.getLogger(Expression.class);

    public static final int AND = 0;
    public static final int OR = 1;
    public static final int NOT = 2;
    public static final int EQUAL_TO = 3;
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

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int ALL = 22;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int SOME = 23;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int ANY = 24;

    /** 
     * Expression interpreted as raw SQL. 
     * No translations will be done for this kind of expressions. 
     */
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

    /** 
     * Interpreted as a comma-separated list of literals. 
     */
    public static final int LIST = 28;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int SUBQUERY = 29;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int COUNT = 30;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int AVG = 31;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int SUM = 32;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int MAX = 33;

    /**
     * <i><b>Warning:</b> currently not supported in Cayenne.</i>
     */
    public static final int MIN = 34;

    public static final int NOT_BETWEEN = 35;
    public static final int NOT_IN = 36;
    public static final int NOT_LIKE = 37;
    public static final int NOT_LIKE_IGNORE_CASE = 38;

    protected int type;

    /**
     * Parses string, converting it to Expression. If string does
     * not represent a semantically correct expression, an ExpressionException
     * is thrown.
     * 
     * @since 1.1
     */
    public static Expression fromString(String expressionString) {
        if (expressionString == null) {
            throw new NullPointerException("Null expression string.");
        }

        Reader reader = new StringReader(expressionString);
        try {
            return new ExpressionParser(reader).expression();
        }
        catch (ParseException ex) {
            throw new ExpressionException(ex.getMessage(), ex);
        }
    }

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

    public boolean equals(Object object) {
        if (!(object instanceof Expression)) {
            return false;
        }

        Expression e = (Expression) object;

        if (e.getType() != getType() || e.getOperandCount() != getOperandCount()) {
            return false;
        }

        // compare operands
        int len = e.getOperandCount();
        for (int i = 0; i < len; i++) {
            if (!Util.nullSafeEquals(e.getOperand(i), getOperand(i))) {
                return false;
            }
        }

        return true;
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
        this.traverse(builder);
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
     * @deprecated Since 1.1 use {@link #evaluate(Object)} or {@link #evaluateBoolean(Object)}.
     */
    public boolean eval(Object o) {
        return evaluateBoolean(o);
    }

    /**
     * Calculates expression value with object as a context for 
     * path expressions.
     * 
     * @since 1.1
     */
    public Object evaluate(Object o) {
        return ASTCompiler.compile(this).evaluateASTChain(o);
    }

    /**
     * Calculates expression boolean value with object as a context for 
     * path expressions.
     * 
     * @since 1.1
     */
    public boolean evaluateBoolean(Object o) {
        return ColnversionUtil.toBoolean(evaluate(o));
    }

    /**
     * Returns a list of objects that match the expression.
     */
    public List filterObjects(List objects) {
        if (objects == null || objects.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        int size = objects.size();
        LinkedList filtered = new LinkedList();

        for (int i = 0; i < size; i++) {
            Object o = objects.get(i);
            if (evaluateBoolean(o)) {
                filtered.addLast(o);
            }
        }

        return filtered;
    }

    /**
     * Clones this expression.
     * 
     * @since 1.1
     */
    public Expression deepCopy() {
        return transform(null);
    }

    /**
     * Creates a transformed copy of this expression applying 
     * transformation provided by Transformer to all its nodes.
     * Null transformer will result in an identical deep copy of
     * this expression.
     *
     * @since 1.1
     */
    public Expression transform(Transformer transformer) {
        ExpressionDeepCopy transformEngine = new ExpressionDeepCopy(transformer);
        traverse(transformEngine);

        return transformEngine.getTransformed();
    }

    /**
     * Creates a copy of this expression node, without copying children.
     * 
     * @since 1.1
     */
    public abstract Expression shallowCopy();

    /**
     * Traverses itself and child expressions, notifying visitor via callback
     * methods as it goes. This is an Expression-specific implementation of
     * the "Visitor" design pattern.
     * 
     * @since 1.1
     */
    public void traverse(TraversalHandler visitor) {
        if (visitor == null) {
            throw new NullPointerException("Null Visitor.");
        }

        traverse(this, null, visitor);
    }

    /**
     * Traverses itself and child expressions, notifying visitor via callback
     * methods as it goes.
     * 
     * @since 1.1
     */
    protected void traverse(
        Object expressionObj,
        Expression parentExp,
        TraversalHandler visitor) {

        // see if "expObj" is a leaf node
        if (!(expressionObj instanceof Expression)) {
            visitor.objectNode(expressionObj, parentExp);
            return;
        }

        Expression exp = (Expression) expressionObj;
        int count = exp.getOperandCount();

        // announce start node
        if (exp instanceof ListExpression) {
            visitor.startListNode(exp, parentExp);
        }
        else {
            switch (count) {
                case 2 :
                    visitor.startBinaryNode(exp, parentExp);
                    break;
                case 1 :
                    visitor.startUnaryNode(exp, parentExp);
                    break;
                case 3 :
                    visitor.startTernaryNode(exp, parentExp);
                    break;
            }
        }

        // traverse each child
        int count_1 = count - 1;
        for (int i = 0; i <= count_1; i++) {
            traverse(exp.getOperand(i), exp, visitor);

            // announce finished child
            visitor.finishedChild(exp, i, i < count_1);
        }

        // announce the end of traversal
        if (exp instanceof ListExpression) {
            visitor.endListNode(exp, parentExp);
        }
        else {
            switch (count) {
                case 2 :
                    visitor.endBinaryNode(exp, parentExp);
                    break;
                case 1 :
                    visitor.endUnaryNode(exp, parentExp);
                    break;
                case 3 :
                    visitor.endTernaryNode(exp, parentExp);
                    break;
            }
        }
    }

    /**
     * Encodes itself, wrapping the string into XML CDATA section.
     * @since 1.1
     */
    public void encodeAsXML(PrintWriter pw, String linePadding) {
        pw.print(linePadding);
        pw.print("<![CDATA[");
        encodeAsString(pw);
        pw.print("]]>");
    }

    /**
     * Stores a String representation of Expression using a provided
     * PrintWriter.
     * 
     * @since 1.1
     */
    public abstract void encodeAsString(PrintWriter pw);

    /**
     * Convenience method to log nested expressions. Used mainly for debugging.
     * Called from "toString".
     * 
     * @deprecated Since 1.1 <code>encode</code> is used to recursively
     * print expressions.
     */
    protected void toStringBuffer(StringBuffer buf) {
        for (int i = 0; i < getOperandCount(); i++) {
            if (i > 0 || getOperandCount() == 1) {
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
        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        encodeAsString(pw);
        pw.close();
        buffer.flush();
        return buffer.toString();
    }

    // ====================================================
    // Deep copy traversal handler
    // ====================================================
    final class ExpressionDeepCopy implements TraversalHandler {
        Transformer transformer;
        LinkedList stack;

        ExpressionDeepCopy(Transformer transformer) {
            this.transformer = transformer;
            this.stack = new LinkedList();
        }

        public Expression getTransformed() {
            return (Expression) stack.getLast();
        }

        public void objectNode(Object leaf, Expression parentNode) {
            stack.addLast(leaf);
        }

        public void finishedChild(
            Expression node,
            int childIndex,
            boolean hasMoreChildren) {

            Object childCopy = stack.removeLast();

            // there is always be a parent clone on the stack, 
            // so the line below is safe

            Expression parentCopy = (Expression) stack.getLast();
            parentCopy.setOperand(childIndex, childCopy);
        }

        void startNode(Expression node, Expression parentNode) {
            stack.addLast(node.shallowCopy());
        }

        void endNode() {
            // now that the clone's children are fully assembled,
            // apply trasformer... First pick and see if transformer
            // changes an objects, and if so, re-insert it to the 
            // top of the stack

            if (transformer != null) {
                Object object = stack.getLast();
                Object transformed = transformer.transform(object);

                if (object != transformed) {
                    stack.removeLast();
                    stack.addLast(transformed);
                }
            }
        }

        public void startUnaryNode(Expression node, Expression parentNode) {
            startNode(node, parentNode);
        }

        public void startBinaryNode(Expression node, Expression parentNode) {
            startNode(node, parentNode);
        }

        public void startTernaryNode(Expression node, Expression parentNode) {
            startNode(node, parentNode);
        }

        public void startListNode(Expression node, Expression parentNode) {
            startNode(node, parentNode);
        }

        public void endUnaryNode(Expression node, Expression parentNode) {
            endNode();
        }

        public void endBinaryNode(Expression node, Expression parentNode) {
            endNode();
        }

        public void endTernaryNode(Expression node, Expression parentNode) {
            endNode();
        }

        public void endListNode(Expression node, Expression parentNode) {
            endNode();
        }
    }

    // ====================================================
    // Helper class to process parameterized expressions.
    // ====================================================
    final class ParametrizedExpressionBuilder extends TraversalHelper {
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
