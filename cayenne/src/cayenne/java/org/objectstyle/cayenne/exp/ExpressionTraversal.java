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

/** 
 * Implements a depth-first algorithm for Expression tree traversal. 
 * Delegates any actions that need to be performed at certain points
 * during traversal to a TraversalHandler instance. 
 * 
 * @author Andrei Adamchik
 */
public class ExpressionTraversal {
    private TraversalHandler handler;

    public ExpressionTraversal() {

    }

    /**
     * @since 1.0.6
     */
    public ExpressionTraversal(TraversalHandler handler) {
        setHandler(handler);
    }

    /** Sets traversal handler. The whole expression traversal process
      * is done for the benefit of handler, since ExpressionTraversal
      * object itself does not use Expression information, it just parses it. */
    public void setHandler(TraversalHandler handler) {
        this.handler = handler;
    }

    /** 
     * Returns TraversalHandler used to process expressions being traversed. 
     */
    public TraversalHandler getHandler() {
        return handler;
    }

    /** 
     * Walks the expression tree, depth-first. When passing through 
     * points of interest, invokes callback methods on TraversalHandler. 
     */
    public void traverseExpression(Expression expression) {
        if(handler == null) {
            throw new NullPointerException("Null handler.");
        }
        
        traverseExpression(expression, null);
    }

    protected void traverseExpression(Object expObj, Expression parentExp) {
        // see if "expObj" is a leaf node
        if (!(expObj instanceof Expression)) {
            handler.objectNode(expObj, parentExp);
            return;
        }

        Expression exp = (Expression) expObj;
        int count = exp.getOperandCount();

        // announce start node
        if (exp instanceof ListExpression) {
            handler.startListNode(exp, parentExp);
        }
        else {
            switch (count) {
                case 2 :
                    handler.startBinaryNode(exp, parentExp);
                    break;
                case 1 :
                    handler.startUnaryNode(exp, parentExp);
                    break;
                case 3 :
                    handler.startTernaryNode(exp, parentExp);
                    break;
            }
        }

        // traverse each child
        int count_1 = count - 1;
        for (int i = 0; i <= count_1; i++) {
            traverseExpression(exp.getOperand(i), exp);

            // announce finished child
            handler.finishedChild(exp, i, i < count_1);
        }

        // announce the end of traversal
        if (exp instanceof ListExpression) {
            handler.endListNode(exp, parentExp);
        }
        else {
            switch (count) {
                case 2 :
                    handler.endBinaryNode(exp, parentExp);
                    break;
                case 1 :
                    handler.endUnaryNode(exp, parentExp);
                    break;
                case 3 :
                    handler.endTernaryNode(exp, parentExp);
                    break;
            }
        }
    }
}
