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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A DerivedDbAttribute is a DbAttribute that resolves to an SQL expression based on 
 * a set of other attributes. DerivedDbAttribute's allow to build
 * expressions like "<code>count(id)</code>", "<code>sum(price)</code>", etc.
 * 
 * <p>
 * Internally DerivedDbAttribute is defined as a specification string and a set of
 * substitution DbAttribute parameters. Specification string is an SQL expression that contains
 * placeholders (<code>%@</code>) for attribute parameters, for example:
 * </p>
 * 
 * <p><code>sum(%@) + sum(%@)</code></p>
 * 
 * 
 * @author Andrei Adamchik
 */
public class DerivedDbAttribute extends DbAttribute {
	public static final String ATTRIBUTE_TOKEN = "%@";

	protected String expressionSpec;
	protected List params = new ArrayList();
	protected boolean groupBy;

	/**
	 * Constructor for DerivedDbAttribute.
	 */
	public DerivedDbAttribute() {
		super();
	}
	
	/**
	 * Constructor for DerivedDbAttribute.
	 */
	public DerivedDbAttribute(String name) {
		super(name);
	}
	
	/**
	 * Constructor for DerivedDbAttribute.
	 * 
	 */
	public DerivedDbAttribute(String name, int type, DbEntity entity, String spec) {
		super(name, type, entity);
		setExpressionSpec(spec);
	}
	

	/**
	 * Creates and initializes a derived attribute with 
	 * an attribute of a parent entity.
	 */
	public DerivedDbAttribute(DbEntity entity, DbAttribute parentProto) {
		setName(parentProto.getName());
		setType(parentProto.getType());
		setMandatory(parentProto.isMandatory());
		setMaxLength(parentProto.getMaxLength());
		setPrecision(parentProto.getPrecision());
		setPrimaryKey(parentProto.isPrimaryKey());

		setExpressionSpec(ATTRIBUTE_TOKEN);
		addParam(parentProto);
		setEntity(entity);
	}

	public String getAliasedName(String alias) {
		if (expressionSpec == null) {
			return super.getAliasedName(alias);
		}

		int len = params.size();
		StringBuffer buf = new StringBuffer();
		int ind = 0;
		for (int i = 0; i < len; i++) {
			// no bound checking
			// expression is assumed to be valid
			int match = expressionSpec.indexOf(ATTRIBUTE_TOKEN, ind);
			DbAttribute at = (DbAttribute) params.get(i);
			if (match > i) {
				buf.append(expressionSpec.substring(ind, match));
			}
			buf.append(at.getAliasedName(alias));
			ind = match + 2;
		}

		if (ind < expressionSpec.length()) {
			buf.append(expressionSpec.substring(ind));
		}

		return buf.toString();
	}

	/**
	 * Returns true if this attribute is used in GROUP BY
	 * clause of the parent entity.
	 */
	public boolean isGroupBy() {
		return groupBy;
	}

	public void setGroupBy(boolean flag) {
		groupBy = flag;
	}

	/**
	 * Returns the params.
	 * @return List
	 */
	public List getParams() {
		return Collections.unmodifiableList(params);
	}

	/**
	 * Returns the expressionSpec.
	 * @return String
	 */
	public String getExpressionSpec() {
		return expressionSpec;
	}

	/**
	 * Sets the params.
	 * @param params The expParams to set
	 */
	public void addParam(DbAttribute param) {
		params.add(param);
	}

	public void removeParam(DbAttribute param) {
		params.remove(param);
	}

	public void clearParams() {
		params.clear();
	}

	/**
	 * Sets the expressionSpec.
	 * @param expressionSpec The expressionSpec to set
	 */
	public void setExpressionSpec(String expressionSpec) {
		this.expressionSpec = expressionSpec;
	}
}
