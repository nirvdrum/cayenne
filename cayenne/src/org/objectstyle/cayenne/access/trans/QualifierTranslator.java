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
package org.objectstyle.cayenne.access.trans;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.QualifiedQuery;
import org.objectstyle.cayenne.query.Query;

/** Translates query qualifier to SQL. Used as a helper
 *  class by query translators. */
public class QualifierTranslator
    extends QueryAssemblerHelper
    implements TraversalHandler {
    static Logger logObj = Logger.getLogger(QualifierTranslator.class.getName());

    private ExpressionTraversal treeWalker = new ExpressionTraversal();

    public QualifierTranslator() {
        this(null);
    }

    public QualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
        treeWalker.setHandler(this);
    }

    /** 
     * Translates query qualifier to SQL WHERE clause. 
     * Qualifier is obtained from <code>queryAssembler</code> object. 
     */
    public void performTranslation() {
    	buffer = null;
    	
        Query q = queryAssembler.getQuery();
        Expression rootNode = ((QualifiedQuery) q).getQualifier();
        if (rootNode == null) {
            return;
        }

        // build SQL where clause string based on expression
        // (using '?' for object values)
        buffer = new StringBuffer();
        treeWalker.traverseExpression(rootNode);
    }

    /** Opportunity to insert an operation */
    public void finishedChild(
        Expression node,
        int childIndex,
        boolean hasMoreChildren) {
        if (!hasMoreChildren)
            return;

        switch (node.getType()) {
            case Expression.AND :
                buffer.append(" AND ");
                break;
            case Expression.OR :
                buffer.append(" OR ");
                break;
            case Expression.EQUAL_TO :
                buffer.append(" = ");
                break;
            case Expression.NOT_EQUAL_TO :
                buffer.append(" <> ");
                break;
            case Expression.LESS_THAN :
                buffer.append(" < ");
                break;
            case Expression.GREATER_THAN :
                buffer.append(" > ");
                break;
            case Expression.LESS_THAN_EQUAL_TO :
                buffer.append(" <= ");
                break;
            case Expression.GREATER_THAN_EQUAL_TO :
                buffer.append(" >= ");
                break;
            case Expression.IN :
                buffer.append(" IN ");
                break;
            case Expression.LIKE :
                buffer.append(" LIKE ");
                break;
            case Expression.LIKE_IGNORE_CASE :
                buffer.append(") LIKE UPPER(");
                break;
            case Expression.ADD :
                buffer.append(" + ");
                break;
            case Expression.SUBTRACT :
                buffer.append(" - ");
                break;
            case Expression.MULTIPLY :
                buffer.append(" * ");
                break;
            case Expression.DIVIDE :
                buffer.append(" / ");
                break;
            case Expression.BETWEEN :
                if (childIndex == 0)
                    buffer.append(" BETWEEN ");
                else if (childIndex == 1)
                    buffer.append(" AND ");
                break;
        }
    }

    /** Opportunity to open a bracket */
    public void startUnaryNode(Expression node, Expression parentNode) {
        if (parenthesisNeeded(node, parentNode))
            buffer.append('(');

        if (node.getType() == Expression.NEGATIVE)
            buffer.append('-');
        // ignore POSITIVE - it is a NOOP
        // else if(node.getType() == Expression.POSITIVE)
        //     buffer.append('+');
        else if (node.getType() == Expression.NOT)
            buffer.append("NOT ");
        else if (node.getType() == Expression.EXISTS)
            buffer.append("EXISTS ");
        else if (node.getType() == Expression.ALL)
            buffer.append("ALL ");
        else if (node.getType() == Expression.SOME)
            buffer.append("SOME ");
        else if (node.getType() == Expression.ANY)
            buffer.append("ANY ");
    }

    /** Opportunity to open a bracket */
    public void startBinaryNode(Expression node, Expression parentNode) {
        if (parenthesisNeeded(node, parentNode)) {
            buffer.append('(');
        }
        if (node.getType() == Expression.LIKE_IGNORE_CASE) {
            buffer.append("UPPER(");
        }
    }

    /** Opportunity to open a bracket */
    public void startTernaryNode(Expression node, Expression parentNode) {
        if (parenthesisNeeded(node, parentNode)) {
            buffer.append('(');
        }
    }

    /** Opportunity to close a bracket */
    public void endUnaryNode(Expression node, Expression parentNode) {
        if (parenthesisNeeded(node, parentNode)) {
            buffer.append(')');
        }
    }

    /** Opportunity to close a bracket */
    public void endBinaryNode(Expression node, Expression parentNode) {
        if (parenthesisNeeded(node, parentNode)) {
            buffer.append(')');
        }
        if (node.getType() == Expression.LIKE_IGNORE_CASE) {
            buffer.append(')');
        }
    }

    /** Opportunity to close a bracket */
    public void endTernaryNode(Expression node, Expression parentNode) {
        if (parenthesisNeeded(node, parentNode)) {
            buffer.append(')');
        }
    }

    public void objectNode(Object leaf, Expression parentNode) {
        if (parentNode.getType() == Expression.RAW_SQL) {
            appendRawSql(leaf);
        }
        else if (parentNode.getType() == Expression.OBJ_PATH) {
            appendObjPath(buffer, parentNode);
        }
        else if (parentNode.getType() == Expression.DB_NAME) {
            appendDbPath(buffer, parentNode);
        }
        else if (parentNode.getType() == Expression.LIST) {
            appendList(parentNode, paramsDbType(parentNode));
        }
        else {
            appendLiteral(buffer, leaf, paramsDbType(parentNode));
        }
    }

    private boolean parenthesisNeeded(Expression node, Expression parentNode) {
        if (parentNode == null) {
            return false;
        }

        // only unary expressions can go w/o parenthesis
        if (node.getOperandCount() > 1) {
            return true;
        }

        if (node.getType() == Expression.OBJ_PATH) {
            return false;
        }

        if (node.getType() == Expression.DB_NAME) {
            return false;
        }

        return true;
    }

    private void appendRawSql(Object sql) {
        if (sql != null) {
            buffer.append(sql);
        }
    }

    private final void appendList(Expression listExpr, DbAttribute paramDesc) {
        List list = (List) listExpr.getOperand(0);

        Iterator it = list.iterator();
        // process first element outside the loop
        // (unroll loop to avoid condition checking
        if (it.hasNext())
            appendLiteral(buffer, it.next(), paramDesc);
        else
            return;

        while (it.hasNext()) {
            buffer.append(", ");
            appendLiteral(buffer, it.next(), paramDesc);
        }
    }
}