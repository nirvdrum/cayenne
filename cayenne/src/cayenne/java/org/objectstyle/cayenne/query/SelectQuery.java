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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.exp.Expression;

/**
 * Describes a database SELECT statement in object terms.
 * In other words, <code>SelectQuery</code> is a specification
 * that tells Cayenne how to generate SQL that will be eventually
 * sent to the database.
 * 
 * <p>SelectQuery defines ObjEntity that should be fetched and a set of conditions. 
 * It allows lots of fine tuning of the fetch process.</p>
 * 
 * @author Andrei Adamchik
 */
public class SelectQuery extends QualifiedQuery implements GenericSelectQuery {
	static Logger logObj = Logger.getLogger(SelectQuery.class.getName());

	protected ArrayList custDbAttributes = new ArrayList();
	protected ArrayList orderings = new ArrayList();
	protected ArrayList prefetches = new ArrayList();
	protected boolean distinct;
	protected boolean fetchingDataRows;
	protected int fetchLimit;
	protected Expression parentQualifier;
	protected String parentObjEntityName;
	protected int pageSize;

	/** Creates empty SelectQuery. */
	public SelectQuery() {
	}

	/** Creates SelectQuery with <code>objEntityName</code> parameter. */
	public SelectQuery(String objEntityName) {
		setObjEntityName(objEntityName);
	}

	/** Creates SelectQuery with <code>objEntityName</code> and <code>qualifier</code> parameters. */
	public SelectQuery(String objEntityName, Expression qualifier) {
		setObjEntityName(objEntityName);
		setQualifier(qualifier);
	}

	/** 
	 * Returns <code>Query.SELECT_QUERY</code> type.
	 */
	public int getQueryType() {
		return SELECT_QUERY;
	}

	/** Adds ordering specification to this query orderings. */
	public void addOrdering(Ordering ordering) {
		orderings.add(ordering);
	}

	/** Adds ordering specification to this query orderings. */
	public void addOrdering(String sortPathSpec, boolean isAscending) {
		addOrdering(new Ordering(sortPathSpec, isAscending));
	}

	/** Returns a list of orderings used by this query. */
	public List getOrderingList() {
		return orderings;
	}

	/** Returns true if this query returns distinct rows. */
	public boolean isDistinct() {
		return distinct;
	}

	/** Sets <code>distinct</code> property. */
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	/**
	 * Returns a list of attributes that will be included
	 * in the results of this query.
	 */
	public List getCustDbAttributes() {
		return custDbAttributes;
	}

	/**
	 * Adds a path to the DbAttribute that should be included 
	 * in the results of this query. Valid paths would look like
	 * <code>ARTIST_NAME</code>, <code>PAINTING_ARRAY.PAINTING_ID</code>,
	 * etc.
	 */
	public void addCustDbAttribute(String attributePath) {
		custDbAttributes.add(attributePath);
	}

	/**
	 * Returns <code>true</code> if there is at least one custom query 
	 * attribute specified, otherwise returns <code>false</code>
	 * for the case when the query results will contain only the 
	 * root entity attributes.
	 * 
	 * <p>Note that queries that are fetching custom attributes
	 * always return data rows instead of DataObjects.
	 * </p>
	 */
	public boolean isFetchingCustAttributes() {
		return custDbAttributes.size() > 0;
	}

	/**
	 * Returns a list of relationships that must be prefetched 
	 * as a part of this query.
	 */
	public List getPrefetchList() {
		return prefetches;
	}

	/** 
	 * Adds a relationship path. ObjRelationship names are separated by ".".
	 * to the list of relationship paths that should be prefetched when the
	 * query is executed.
	 */
	public void addPrefetch(String relPath) {
		prefetches.add(relPath);
	}
	
	public void addPrefetches(List relPaths) {
		prefetches.addAll(relPaths);
	}

	/**
	 * Returns <code>true</code> if this query 
	 * should produce a list of data rows as opposed
	 * to DataObjects, <code>false</code> for DataObjects. 
	 * This is a hint to QueryEngine executing this query.
	 */
	public boolean isFetchingDataRows() {
		return isFetchingCustAttributes() || fetchingDataRows;
	}

	/**	
	 * Sets query result type. If <code>flag</code> parameter is
	 * <code>true</code>, then results will be in the form of data rows.
	 * 
	 * <p><i>Note that if <code>isFetchingCustAttributes()</code>
	 * returns <code>true</code>, this setting has no effect, and data 
	 * rows are always fetched.</i></p>
	 */
	public void setFetchingDataRows(boolean flag) {
		this.fetchingDataRows = flag;
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
	 * 
	 * @param fetchLimit The fetchLimit to set
	 */
	public void setFetchLimit(int fetchLimit) {
		this.fetchLimit = fetchLimit;
	}

	/** Setter for query's parent entity qualifier. */
	public void setParentQualifier(Expression parentQualifier) {
		this.parentQualifier = parentQualifier;
	}

	/** Getter for query parent entity qualifier. */
	public Expression getParentQualifier() {
		return parentQualifier;
	}

	/**
	 * Adds specified parent entity qualifier to the 
	 * existing parent entity qualifier joining it using "AND".
	 */
	public void andParentQualifier(Expression e) {
		parentQualifier =
			(parentQualifier != null) ? parentQualifier.andExp(e) : e;
	}

	/**
	* Adds specified parent entity qualifier to the existing 
	* qualifier joining it using "OR".
	*/
	public void orParentQualifier(Expression e) {
		parentQualifier =
			(parentQualifier != null) ? parentQualifier.orExp(e) : e;
	}

	/**
	 * Returns the name of parent ObjEntity.
	 * 
	 * @return String
	 */
	public String getParentObjEntityName() {
		return parentObjEntityName;
	}

	/**
	 * Sets the name of parent ObjEntity. If query's root 
	 * ObjEntity maps to a derived entity in the DataMap,
	 * this query qualifier will resolve to a HAVING clause
	 * of an SQL statement. To allow fine tuning the query 
	 * before applying GROUP BY and HAVING, callers can setup
	 * the name of parent ObjEntity and parent qualifier that
	 * will be used to create WHERE clause preceeding GROUP BY.
	 * 
	 * <p>For instance this is helpful to qualify the fetch 
	 * on a related entity attributes, since HAVING does not 
	 * allow joins.</p>
	 * 
	 * @param parentObjEntityName The parentObjEntityName to set
	 */
	public void setParentObjEntityName(String parentObjEntityName) {
		this.parentObjEntityName = parentObjEntityName;
	}
	
	
	/**
	 * Returns <code>true</code> if this query has an extra
	 * qualifier that uses a parent entity of the query
	 * root entity for additional result filtering.
	 */
	public boolean isQualifiedOnParent() {
		return getParentObjEntityName() != null && parentQualifier != null;
	}
	
    /**
     * Returns <code>pageSize</code> property.
     * Page size is a hint telling Cayenne QueryEngine 
     * that query result should use paging instead of
     * reading the whole result in the memory.
     * 
     * @return int
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
