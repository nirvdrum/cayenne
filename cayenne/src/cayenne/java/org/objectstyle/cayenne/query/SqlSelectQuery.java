/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Allows to send "raw" SQL select statements to the database 
 * using Cayenne connection layer. Its intention is to allow 
 * programmers to access database-specific features not covered 
 * by Cayenne. Queries created using SqlSelectQuery are very likely 
 * not portable accross database engines. 
 */
public class SqlSelectQuery extends AbstractQuery implements GenericSelectQuery {
    protected String sqlString;
    protected DbAttribute[] resultDescriptors;
    protected ObjAttribute[] objDescriptors;
    protected int fetchLimit;
    protected int pageSize;

    /** Creates empty SqlSelectQuery. */
    public SqlSelectQuery() {}

    private void init(Object root, String sqlString) {
    	setRoot(root);
    	setSqlString(sqlString);
    }
    
    /**
     * Creates a SqlSelectQuery with no initial sqlString, for the specifed ObjEntity
     * @param root the ObjEntity to use as root
    */
    public SqlSelectQuery(ObjEntity root) {
    	this(root, null);
    }
    
     /**
     * Creates a SqlSelectQuery using the given ObjEntity as a root, with the given sql string 
     * @param root the ObjEntity to use as root
     * @param sqlString the sql to execute
     */
   public SqlSelectQuery(ObjEntity root, String sqlString) {
		init(root, sqlString);
    }
    
    /**
     * Creates a SqlSelectQuery with no initial sqlString, for the specifed DbEntity
     * @param root the DbEntity to use as root
    */
    public SqlSelectQuery(DbEntity root) {
    	this(root, null);
    }
    
     /**
     * Creates a SqlSelectQuery using the given DbEntity as a root, with the given sql string 
     * @param root the DbEntity to use as root
     * @param sqlString the sql to execute
     */
   public SqlSelectQuery(DbEntity root, String sqlString) {
		init(root, sqlString);
    }
    
     /**
     * Creates a SqlSelectQuery with null qualifier, for the entity which uses the given class
     * @param root the Class of objects this SqlSelectQuery is for.
     */
   public SqlSelectQuery(Class rootClass) {
    	this(rootClass, null);
    }
    
	/**
	 * Creates a SqlSelectQuery for the entity which uses the given class,  with the given qualifier
	 * @param root the Class of objects this SqlSelectQuery is for.
     * @param sqlString the sql to execute
     */
   public SqlSelectQuery(Class rootClass, String sqlString) {
    	init(rootClass, sqlString);
    }
    /** Creates SqlSelectQuery with <code>objEntityName</code> parameter. */
    public SqlSelectQuery(String objEntityName) {
        this(objEntityName, null);
    }

    /** Creates SqlSelectQuery with <code>objEntityName</code> and <code>qualifier</code> parameters. */
    public SqlSelectQuery(String objEntityName, String sqlString) {
        init(objEntityName, sqlString);
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

    /**
     * Returns the fetchLimit.
     * @return int
     */
    public int getFetchLimit() {
        return fetchLimit;
    }

    /**
     * Sets the fetchLimit.
     * @param fetchLimit The fetchLimit to set
     */
    public void setFetchLimit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
    }

    /** Always returns <code>true</code>. */
    public boolean isFetchingDataRows() {
        return true;
    }

    /**
     * Returns the resultDescriptors.
     * @return DbAttribute[]
     */
    public DbAttribute[] getResultDescriptors() {
        return resultDescriptors;
    }

    /**
     * Sets the resultDescriptors.
     * @param resultDescriptors The resultDescriptors to set
     */
    public void setResultDescriptors(DbAttribute[] resultDescriptors) {
        this.resultDescriptors = resultDescriptors;
    }

    /**
     * Returns the objDescriptors.
     * @return ObjAttribute[]
     */
    public ObjAttribute[] getObjDescriptors() {
        return objDescriptors;
    }

    /**
     * Sets the objDescriptors.
     * @param objDescriptors The objDescriptors to set
     */
    public void setObjDescriptors(ObjAttribute[] objDescriptors) {
        this.objDescriptors = objDescriptors;
    }

    /**
     * @see org.objectstyle.cayenne.query.GenericSelectQuery#getPageSize()
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Sets  <code>pageSize</code> property.
     * 
     * @param pageSize The pageSize to set
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
