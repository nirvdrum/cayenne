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
package org.objectstyle.cayenne.map;

import java.util.*;

import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * DbEntity subclass that is based on another DbEntity
 * and allows to define complex database expressions 
 * like GROUP BY and aggregate functions.
 * 
 * @author Andrei Adamchik
 */
public class DerivedDbEntity extends DbEntity {
	protected DbEntity parentEntity;
	protected ArrayList groupByAttributes = new ArrayList();

	/**
	 * Constructor for DerivedDbEntity.
	 */
	public DerivedDbEntity() {
		super();
	}

	/**
	 * Constructor for DerivedDbEntity.
	 * @param name
	 */
	public DerivedDbEntity(String name) {
		super(name);
	}

	/**
	 * Constructor for DerivedDbEntity. Creates
	 * a derived entity with the attribute set of a parent entity.
	 */
	public DerivedDbEntity(String name, DbEntity parentEntity) {
		super(name);

		this.setParentEntity(parentEntity);
		this.resetToParentView();
	}

	/**
	 * Removes all attributes and relationships, 
	 * and replaces them with the data of 
	 * the parent entity.
	 */
	public void resetToParentView() {
		clearAttributes();
		clearRelationships();

		Iterator it = getParentEntity().getAttributeList().iterator();
		while (it.hasNext()) {
			DbAttribute at = (DbAttribute) it.next();
			addAttribute(new DbAttribute(at));
		}
	}

	/**
	 * Returns the parentEntity.
	 * 
	 * @return DbEntity
	 */
	public DbEntity getParentEntity() {
		return parentEntity;
	}

	/**
	 * Sets the parentEntity.
	 * 
	 * @param parentEntity The parentEntity to set
	 */
	public void setParentEntity(DbEntity parentEntity) {
		this.parentEntity = parentEntity;
	}

	/** 
	 * Returns attributes used in GROUP BY as an unmodifiable list.
	 */
	public List getGroupByAttributes() {
		return Collections.unmodifiableList(groupByAttributes);
	}

	/** Adds an attribute to the GROUP BY clause. */
	public void addGroupByAttribute(DbAttribute dbAttr) {
		groupByAttributes.add(dbAttr);
	}

	/** 
	 * Removes an  attribute from the list of attributes used 
	 * in GROUP BY clause. 
	 */
	public void removeGroupByAttribute(String attrName) {
		groupByAttributes.remove(getAttribute(attrName));
	}

	public void clearGroupByAttributes() {
		groupByAttributes.clear();
	}

	/**
	 * @see org.objectstyle.cayenne.map.DbEntity#getFullyQualifiedName()
	 */
	public String getFullyQualifiedName() {
		return (getParentEntity() != null)
			? getParentEntity().getFullyQualifiedName()
			: null;
	}

	/** 
	 * Returns schema of the parent entity.
	 */
	public String getSchema() {
		return (getParentEntity() != null)
			? getParentEntity().getSchema()
			: null;
	}

	/** Throws exception. */
	public void setSchema(String schema) {
		throw new CayenneRuntimeException("Can't change schema of a derived entity.");
	}

	/** 
	 * Returns catalog of the parent entity.
	 */
	public String getCatalog() {
		return (getParentEntity() != null)
			? getParentEntity().getCatalog()
			: null;
	}

	/** Throws exception. */
	public void setCatalog(String catalog) {
		throw new CayenneRuntimeException("Can't change catalogue of a derived entity.");
	}

	/**
	 * @see org.objectstyle.cayenne.map.Entity#clearAttributes()
	 */
	public void clearAttributes() {
		super.clearAttributes();
		clearGroupByAttributes();
	}

	/**
	 * @see org.objectstyle.cayenne.map.Entity#removeAttribute(String)
	 */
	public void removeAttribute(String attrName) {
		super.removeAttribute(attrName);
		removeGroupByAttribute(attrName);
	}
}
