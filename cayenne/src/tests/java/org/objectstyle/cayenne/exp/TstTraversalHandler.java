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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/** Class that collects statistics of expression traversal. It is both traversal 
 *  engine and traversal handler. */
public class TstTraversalHandler
    extends ExpressionTraversal
    implements TraversalHandler {
    protected List treeFlatView = new ArrayList();
    protected int children;
    protected int unaryNodes;
    protected int binaryNodes;
    protected int ternaryNodes;
    protected int listNodes;
    protected int listNodesStarted;
    protected int unaryNodesStarted;
    protected int binaryNodesStarted;
    protected int ternaryNodesStarted;
    protected int leafs;

    /**
     * Performs independent traversal of two expressions,
     * comparing the results. If expressions structure is different,
     * throws an exception.
     */
    public static void compareExps(Expression exp1, Expression exp2) {
        TstTraversalHandler handler1 = new TstTraversalHandler();
        handler1.traverseExpression(exp1);

        TstTraversalHandler handler2 = new TstTraversalHandler();
        handler2.traverseExpression(exp2);

        Assert.assertEquals(handler1.unaryNodes, handler2.unaryNodes);
        Assert.assertEquals(handler1.binaryNodes, handler2.binaryNodes);
        Assert.assertEquals(handler1.ternaryNodes, handler2.ternaryNodes);
        Assert.assertEquals(handler1.listNodes, handler2.listNodes);
    }

    public TstTraversalHandler() {
        setHandler(this);
    }

    public void assertConsistency() throws Exception {
        Assert.assertEquals(unaryNodesStarted, unaryNodes);
        Assert.assertEquals(binaryNodesStarted, binaryNodes);
        Assert.assertEquals(ternaryNodesStarted, ternaryNodes);
        Assert.assertEquals(listNodesStarted, listNodes);
    }

    public List getTreeFlatView() {
        return treeFlatView;
    }

    public void traverseExpression(Expression exp) {
        reset();
        super.traverseExpression(exp);
    }

    public void reset() {
        children = 0;
        unaryNodes = 0;
        binaryNodes = 0;
        ternaryNodes = 0;
        unaryNodesStarted = 0;
        binaryNodesStarted = 0;
        ternaryNodesStarted = 0;
        listNodes = 0;
        listNodesStarted = 0;
        leafs = 0;
    }

    public int getNodeCount() {
        return unaryNodes + binaryNodes + ternaryNodes + listNodes;
    }

    public int getChildren() {
        return children;
    }

    public int getUnaryNodes() {
        return unaryNodes;
    }

    public int getListNodes() {
        return listNodes;
    }

    public int getListNodesStarted() {
        return listNodesStarted;
    }

    public int getBinaryNodes() {
        return binaryNodes;
    }

    public int getTernaryNodes() {
        return ternaryNodes;
    }

    public int getUnaryNodesStarted() {
        return unaryNodesStarted;
    }

    public int getBinaryNodesStarted() {
        return binaryNodesStarted;
    }

    public int getTernaryNodesStarted() {
        return ternaryNodesStarted;
    }

    public int getLeafs() {
        return leafs;
    }

    public void finishedChild(
        Expression node,
        int childIndex,
        boolean hasMoreChildren) {
        children++;
    }

    public void startUnaryNode(Expression node, Expression parentNode) {
        treeFlatView.add(node);
        unaryNodesStarted++;
    }

    public void startBinaryNode(Expression node, Expression parentNode) {
        treeFlatView.add(node);
        binaryNodesStarted++;
    }

    public void startTernaryNode(Expression node, Expression parentNode) {
        treeFlatView.add(node);
        ternaryNodesStarted++;
    }

    public void endUnaryNode(Expression node, Expression parentNode) {
        unaryNodes++;
    }

    public void endBinaryNode(Expression node, Expression parentNode) {
        binaryNodes++;
    }

    public void endTernaryNode(Expression node, Expression parentNode) {
        ternaryNodes++;
    }

    public void objectNode(Object leaf, Expression parentNode) {
        treeFlatView.add(leaf);
        leafs++;
    }
    /**
     * @see org.objectstyle.cayenne.exp.TraversalHandler#endListNode(org.objectstyle.cayenne.exp.Expression, org.objectstyle.cayenne.exp.Expression)
     */
    public void endListNode(Expression node, Expression parentNode) {
        listNodes++;
    }

    /**
     * @see org.objectstyle.cayenne.exp.TraversalHandler#startListNode(org.objectstyle.cayenne.exp.Expression, org.objectstyle.cayenne.exp.Expression)
     */
    public void startListNode(Expression node, Expression parentNode) {
        treeFlatView.add(node);
        listNodesStarted++;
    }
}
