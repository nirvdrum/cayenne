package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;

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

/** Defines ordering policy. Queries can have multiple Ordering's. */
public class Ordering {
	/** Symbolic representation of ascending ordering criterion. */
	public static final boolean ASC = true;

	/** Symbolic representation of descending ordering criterion. */
	public static final boolean DESC = false;

	protected Expression sortSpec;
	protected boolean ascending;

	public Ordering() {
	}

	public Ordering(String sortPathSpec, boolean ascending) {
		setSortSpec(sortPathSpec);
		this.ascending = ascending;
	}

	/** 
	 * Returns sortPathSpec OBJ_PATH specification used in ordering.
	 * 
	 * @deprecated Since ordering now supports expression types other than OBJ_PATH,
	 * this method is deprected. Use <code>getSortSpec().getOperand(0)</code> instead.
     */
	public String getSortPathSpec() {
		return (String)getSortSpec().getOperand(0);
	}

	/** 
	 * Sets path of the sort specification. 
	 * 
	 * @deprecated Since ordering now supports expression types other than OBJ_PATH,
	 * this method is deprected. Use <code>setSortSpec()</code> instead.
	 */
	public void setSortPathSpec(String sortPathSpec) {
		setSortSpec(sortPathSpec);
	}

	/** 
	 * Sets sortSpec to be OBJ_PATH expression. 
	 * with path specified as <code>sortPathSpec</code>
	 * parameter.
	 */
	public void setSortSpec(String sortPathSpec) {
		this.sortSpec =
			ExpressionFactory.unaryExp(Expression.OBJ_PATH, sortPathSpec);
	}

	/** Returns true if sorting is done in ascending order. */
	public boolean isAscending() {
		return ascending;
	}

	/** Sets <code>ascending</code> property of this Ordering. */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * Returns the sortSpec.
	 * @return Expression
	 */
	public Expression getSortSpec() {
		return sortSpec;
	}

	/**
	 * Sets the sortSpec.
	 * @param sortSpec The sortSpec to set
	 */
	public void setSortSpec(Expression sortSpec) {
		this.sortSpec = sortSpec;
	}
}
