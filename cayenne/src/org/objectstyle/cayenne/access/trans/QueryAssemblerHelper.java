package org.objectstyle.cayenne.access.trans;
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

import java.util.Iterator;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.*;

/** Translates parts of the query to SQL.
 * Always works in the context of parent Translator. */
public abstract class QueryAssemblerHelper {

    protected QueryAssembler queryAssembler;


    /** Creates QueryAssemblerHelper using queryAssembler as a context
     * future translation runs. */
    public QueryAssemblerHelper(QueryAssembler queryAssembler) {
        this.queryAssembler = queryAssembler;
    }


    public QueryAssembler getQueryAssembler() {
        return queryAssembler;
    }


    /** Will translate the part of parent translator's query that is supported by this PartTranslator.
     * For example QualifierTranslator will process qualifier expression. In the process of translation
     * parent translator is notified of any join tables added (so that it can update its "FROM" clause. 
     * Also parent translator is consulted about table aliases to use when translating columns. */
    public abstract String doTranslation();


    protected void appendObjPath(StringBuffer buf, Expression pathExp) {
        Iterator it = getQueryAssembler().getRootEntity().resolvePathComponents(pathExp);
        while(it.hasNext()) {
            Object nextComp = it.next();
            if(nextComp instanceof ObjRelationship)
                // find and add joins ....
                processRelParts((ObjRelationship)nextComp);
            else {
                ObjAttribute objAttr = (ObjAttribute)nextComp;
                processColumn(buf, objAttr.getDbAttribute());
            }
        }
    }

    protected void appendDbPath(StringBuffer buf, Expression pathExp) {
        String attrName = (String)pathExp.getOperand(0);
        DbAttribute attr = (DbAttribute)getQueryAssembler().getRootEntity().getDbEntity().getAttribute(attrName);
        processColumn(buf, attr);
    }


    /** Appends column name of a column in a root entity. */
    protected void processColumn(StringBuffer buf, Expression nameExp) {
        if(queryAssembler.supportsTableAlases()) {
            String alias = queryAssembler.aliasForTable(getQueryAssembler().getRootEntity().getDbEntity());
            buf.append(alias).append('.');
        }

        buf.append(nameExp.getOperand(0));
    }


    protected void appendLiteral(StringBuffer buf, Object val) {
        if(val == null)
            buf.append("NULL");
        else {
            buf.append('?');

            // we are hoping that when processing parameter list, the correct type will be
            // guessed without looking at DbAttribute...
            queryAssembler.addToParamList(null, val);
        }
    }


    protected void processColumn(StringBuffer buf, DbAttribute dbAttr) {
        if(queryAssembler.supportsTableAlases()) {
            String alias = queryAssembler.aliasForTable((DbEntity)dbAttr.getEntity());
            buf.append(alias).append('.');
        }

        buf.append(dbAttr.getName());
    }


    protected void processRelParts(ObjRelationship rel) {
        Iterator it = rel.getDbRelationshipList().iterator();
        while(it.hasNext()) {
            queryAssembler.dbRelationshipAdded((DbRelationship)it.next());
        }
    }
}
