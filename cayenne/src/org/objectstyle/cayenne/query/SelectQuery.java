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
import java.util.logging.Logger;

import org.objectstyle.cayenne.exp.Expression;

public class SelectQuery extends QualifiedQuery {
	static Logger logObj = Logger.getLogger(SelectQuery.class.getName());

	protected ArrayList resultsDbAttributes = new ArrayList();
	protected ArrayList orderings = new ArrayList();
	protected ArrayList prefetches = new ArrayList();
	protected boolean distinct;
	protected boolean fetchingDataRows;

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
	public List getResultsDbAttributes() {
		return resultsDbAttributes;
	}

	/**
	 * Adds a path to the DbAttribute that should be included 
	 * in the results of this query. Valid paths would look like
	 * <code>ARTIST_NAME</code>, <code>PAINTING_ARRAY.PAINTING_ID</code>,
	 * etc.
	 */
	public void addResultDbAttribute(String attributePath) {
		resultsDbAttributes.add(attributePath);
	}
	
	/**
	 * Returns <code>true</code> if there is no custom query 
	 * attributes specified, and the query results
	 * will contain only the root entity attributes.
	 */
	public boolean isUsingRootEntityAttributes() {
		return resultsDbAttributes.size() == 0;
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

	/**
	 * Returns <code>true</code> if this query 
	 * should produce a list of data rows as opposed
	 * to DataObjects, <code>false</code> for DataObjects. 
	 * This is a hint to QueryEngine executing this query.
	 */
	public boolean isFetchingDataRows() {
		return fetchingDataRows;
	}

	/**	
	 * Sets query result type. If <code>flag</code> parameter is
	 * <code>true</code>, then results will be in the form of data rows.
	 */
	public void setFetchingDataRows(boolean flag) {
		this.fetchingDataRows = flag;
	}
}
