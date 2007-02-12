/* Generated By:JJTree: Do not edit this line. SimpleNode.java */

/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp.parser;

import java.io.PrintWriter;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.util.Util;

/**
 * Superclass of AST* expressions that implements Node interface defined by JavaCC
 * framework.
 * <p>
 * Some parts of the parser are based on OGNL parser, copyright (c) 2002, Drew Davidson
 * and Luke Blanshard.
 * </p>
 * 
 * @since 1.1
 */
public abstract class SimpleNode extends Expression implements Node {

    protected Node parent;
    protected Node[] children;
    protected int id;

    /**
     * Utility method that encodes an object that is not an expression Node to String.
     */
    protected static void encodeScalarAsString(PrintWriter pw, Object scalar) {
        boolean quote = scalar instanceof String;

        if (quote) {
            pw.print('\"');
        }

        // encode only ObjectId for Persistent, ensure that the order of keys is
        // predictable....
        if (scalar instanceof Persistent) {
            ObjectId id = ((Persistent) scalar).getObjectId();
            if(id != null) {
                scalar = id;
            }
        }

        encodeAsEscapedString(pw, String.valueOf(scalar));
        if (quote) {
            pw.print('\"');
        }
    }

    /**
     * Utility method that prints a string to the provided PrintWriter, escaping special
     * characters.
     */
    protected static void encodeAsEscapedString(PrintWriter pw, String source) {
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);

            switch (c) {
                case '\n':
                    pw.print("\\n");
                    continue;
                case '\r':
                    pw.print("\\r");
                    continue;
                case '\t':
                    pw.print("\\t");
                    continue;
                case '\b':
                    pw.print("\\b");
                    continue;
                case '\f':
                    pw.print("\\f");
                    continue;
                case '\\':
                    pw.print("\\\\");
                    continue;
                case '\'':
                    pw.print("\\'");
                    continue;
                case '\"':
                    pw.print("\\\"");
                    continue;
                default:
                    pw.print(c);
            }
        }
    }

    protected SimpleNode(int i) {
        id = i;
    }

    protected abstract String getExpressionOperator(int index);

    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return true;
    }

    /**
     * Implemented for backwards compatibility with exp package.
     */
    public String expName() {
        return ExpressionParserTreeConstants.jjtNodeName[id];
    }

    /**
     * Flattens the tree under this node by eliminating any children that are of the same
     * class as this node and copying their children to this node.
     */
    protected void flattenTree() {
        boolean shouldFlatten = false;
        int newSize = 0;

        for (int i = 0; i < children.length; i++) {
            if (children[i].getClass() == getClass()) {
                shouldFlatten = true;
                newSize += children[i].jjtGetNumChildren();
            }
            else {
                newSize++;
            }
        }

        if (shouldFlatten) {
            Node[] newChildren = new Node[newSize];
            int j = 0;

            for (int i = 0; i < children.length; ++i) {
                Node c = children[i];
                if (c.getClass() == getClass()) {
                    for (int k = 0; k < c.jjtGetNumChildren(); ++k)
                        newChildren[j++] = c.jjtGetChild(k);
                }
                else {
                    newChildren[j++] = c;
                }
            }

            if (j != newSize) {
                throw new ExpressionException("Assertion error: " + j + " != " + newSize);
            }

            this.children = newChildren;
        }
    }

    public void encodeAsString(PrintWriter pw) {
        if (parent != null) {
            pw.print("(");
        }

        if ((children != null) && (children.length > 0)) {
            for (int i = 0; i < children.length; ++i) {
                if (i > 0) {
                    pw.print(' ');
                    pw.print(getExpressionOperator(i));
                    pw.print(' ');
                }

                ((SimpleNode) children[i]).encodeAsString(pw);
            }
        }

        if (parent != null) {
            pw.print(')');
        }
    }

    public Object getOperand(int index) {
        Node child = jjtGetChild(index);

        // unwrap ASTScalar nodes - this is likely a temporary thing to keep it compatible
        // with QualifierTranslator. In the future we might want to keep scalar nodes
        // for the purpose of expression evaluation.
        return unwrapChild(child);
    }

    protected Node wrapChild(Object child) {
        return (child instanceof Node || child == null) ? (Node) child : new ASTScalar(
                child);
    }

    protected Object unwrapChild(Node child) {
        return (child instanceof ASTScalar) ? ((ASTScalar) child).getValue() : child;
    }

    public int getOperandCount() {
        return jjtGetNumChildren();
    }

    public void setOperand(int index, Object value) {
        Node node = (value == null || value instanceof Node)
                ? (Node) value
                : new ASTScalar(value);
        jjtAddChild(node, index);

        // set the parent, as jjtAddChild doesn't do it...
        if (node != null) {
            node.jjtSetParent(this);
        }
    }

    public void jjtOpen() {

    }

    public void jjtClose() {

    }

    public void jjtSetParent(Node n) {
        parent = n;
    }

    public Node jjtGetParent() {
        return parent;
    }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new Node[i + 1];
        }
        else if (i >= children.length) {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public final int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    /**
     * Evaluates itself with object, pushing result on the stack.
     */
    protected abstract Object evaluateNode(Object o) throws Exception;

    protected Object evaluateChild(int index, Object o) throws Exception {
        return ((SimpleNode) jjtGetChild(index)).evaluate(o);
    }

    public Expression notExp() {
        return new ASTNot(this);
    }

    public Object evaluate(Object o) {
        // wrap in try/catch to provide unified exception processing
        try {
            return evaluateNode(o);
        }
        catch (Throwable th) {
            String string = this.toString();
            throw new ExpressionException(
                    "Error evaluating expression '" + string + "'",
                    string,
                    Util.unwindException(th));
        }
    }
}