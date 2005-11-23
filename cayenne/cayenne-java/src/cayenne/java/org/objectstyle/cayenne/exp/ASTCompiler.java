/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

import java.util.HashMap;
import java.util.Map;

/**
 * A compiler for Cayenne Expressions.
 * 
 * @since 1.0.6
 * @author Andrei Adamchik
 */
class ASTCompiler {

    /**
     * Produces a chain of ASTNodes, returning the starting node that can be used
     * to evaluate expressions.
     */
    static ASTNode compile(Expression expression) throws ExpressionException {
        ExpressionParser handler = new ExpressionParser();
        expression.traverse(handler);
        return handler.finishParsing(expression);
    }

    static final class ExpressionParser extends TraversalHelper {
        // TODO - for big expressions we may remove cached AST from the map
        // once a node and all its children are procesed, to keep the map as small 
        // as possible during compilation... Though most data expressions are rather small,
        // and have from a few to a few dozen nodes...

        ASTNode currentNode;
        ASTNode startNode;
        Map astMap = new HashMap();

        ASTNode finishParsing(Expression expression) {
            // must add the top node to the tree. since "finishedChild"
            // is never invoked for the top node as child. 
            processNode((ASTNode) astMap.get(expression));

            return startNode;
        }

        public void startNode(Expression node, Expression parentNode) {
            // create an ASTNode and store it so that children could link 
            // to it.
            ASTNode parentAST = (ASTNode) astMap.get(parentNode);
            astMap.put(node, ASTNode.buildExpressionNode(node, parentAST));
        }

        public void finishedChild(
            Expression node,
            int childIndex,
            boolean hasMoreChildren) {

            // skip children of precompiled nodes, such as
            // OBJ_PATH, LIST, and varieties of LIKE
            int type = node.getType();
            if (type == Expression.OBJ_PATH
                || type == Expression.LIST
                || (childIndex == 1
                    && (type == Expression.LIKE
                        || type == Expression.LIKE_IGNORE_CASE
                        || type == Expression.NOT_LIKE
                        || type == Expression.NOT_LIKE_IGNORE_CASE))) {
                return;
            }

            // add expression to the chain
            Object child = node.getOperand(childIndex);

            // unless child is not an expression, it must be cached already
            ASTNode newAST;
            if (child instanceof Expression) {
                newAST = (ASTNode) astMap.get(child);
            }
            else {
                ASTNode parentAST = (ASTNode) astMap.get(node);
                newAST = ASTNode.buildObjectNode(child, parentAST);
            }

            processNode(newAST);
        }

        void processNode(ASTNode newAST) {
            if (startNode == null) {
                startNode = newAST;
                currentNode = newAST;
            }
            else {
                currentNode.setNextNode(newAST);
                currentNode = newAST;
            }
        }
    }
}