package org.objectstyle.cayenne.query;
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

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.map.ObjAttribute;


/** Allows to send "raw" SQL select statements to the database using 
 * Cayenne connection layer. It is intended to allow programmers to access
 * database-specific features not covered by Cayenne. Queries created
 * using SqlSelectQuery are very likely not portable accross database engines. */
public class SqlSelectQuery extends Query {
    protected String sqlString;
    protected ObjAttribute[] resultDesc;
        
    /** Creates empty SqlSelectQuery. */
    public SqlSelectQuery() {}
    
    
    /** Creates SqlSelectQuery with <code>objEntityName</code> parameter. */
    public SqlSelectQuery(String objEntityName) {
        setObjEntityName(objEntityName);
    }
    
    /** Creates SqlSelectQuery with <code>objEntityName</code> and <code>qualifier</code> parameters. */
    public SqlSelectQuery(String objEntityName, String sqlString) {
        setObjEntityName(objEntityName);
        setSqlString(sqlString);
    }
    
    public int getQueryType() {
        return SELECT_QUERY;
    }
    
    public void setSqlString(String sqlString) {
        this.sqlString = sqlString;
    }
    
    public String getSqlString() {
        return sqlString;
    } 
    
    /** Sets resultDesc property. This allows to control
      * Java types and key names in the query result. */
    public void setResultDesc(ObjAttribute[] resultDesc) {
        this.resultDesc = resultDesc;
    }
    
    public ObjAttribute[] getResultDesc() {
        return resultDesc;
    }
}
