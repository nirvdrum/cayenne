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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;

/** 
 * Metadata for the navigational association between the data objects. For
 * example, if class "Employee" you may need to get to the department  entity by
 * calling "employee.getDepartment()". In this case you navigate  from data
 * object class Employee to Department. In this case Employee is  source and
 * Department is target. The navigation from Department to the  list of
 * employees would be expressed by another instance of  ObjRelationship.
 * ObjRelationship class also stores the navigation information in terms  of the
 * database entity relationships.  The ObjRelationship objects are stored in the
 * ObjEntities. 
 */
public class ObjRelationship extends Relationship {

	// What to do with any inverse relationship when the source object
	// is deleted
	private int deleteRule = DeleteRule.NO_ACTION;

	// Not flattened initially;
	// will be set when dbRels are added that make it flattened
	private boolean isFlattened = false;

	// Initially all relationships are read/write;
	// a flattened relationship may be readonly (in certain circumstances),
	// will be set in that case
	private boolean isReadOnly = false;

	private List dbRelationships = new ArrayList();
	private List dbRelationshipsRef = Collections.unmodifiableList(dbRelationships);

	public ObjRelationship() {
	}

	public ObjRelationship(String name) {
		super(name);
	}

	public ObjRelationship(
		ObjEntity source,
		ObjEntity target,
		boolean toMany) {
		setSourceEntity(source);
		setTargetEntity(target);
		setToMany(toMany);
		if (toMany)
			setName(target.getName() + "Array");
		else
			setName("to" + target.getName());
	}

	public Entity getTargetEntity() {
		if (getTargetEntityName() == null) {
			return null;
		}

		Entity src = getSourceEntity();
		if (src == null) {
			return null;
		}

		DataMap map = src.getDataMap();
		if (map == null) {
			return null;
		}

		return map.getObjEntity(getTargetEntityName(), true);
	}

	/**
	 * Returns true if underlying DbRelationships point to dependent entity.
	 */
	public boolean isToDependentEntity() {
		return ((DbRelationship)dbRelationships.get(0)).isToDependentPK();
	}

	/**
	 * Returns ObjRelationship that is the opposite of this ObjRelationship.
	 * returns null if no such relationship exists.
	 */
	public ObjRelationship getReverseRelationship() {
		// reverse the list
		List reversed = new ArrayList();
		Iterator rit = this.getDbRelationships().iterator();
		while (rit.hasNext()) {
			DbRelationship rel = (DbRelationship)rit.next();
			DbRelationship reverse = rel.getReverseRelationship();
			if (reverse == null)
				return null;

			reversed.add(0, reverse);
		}

		Entity target = this.getTargetEntity();
		Entity src = this.getSourceEntity();

		Iterator it = target.getRelationships().iterator();
		while (it.hasNext()) {
			ObjRelationship rel = (ObjRelationship)it.next();
			if (rel.getTargetEntity() != src)
				continue;

			List otherRels = rel.getDbRelationships();
			if (reversed.size() != otherRels.size())
				continue;

			int len = reversed.size();
			boolean relsMatch = true;
			for (int i = 0; i < len; i++) {
				if (otherRels.get(i) != reversed.get(i)) {
					relsMatch = false;
					break;
				}
			}

			if (relsMatch)
				return rel;
		}

		return null;
	}

	/**
	 * Returns a list of underlying DbRelationships.
	 * @deprecated Since 1.0 Beta1; use #getDbRelationships() instead.
	 */
	public List getDbRelationshipList() {
		return this.getDbRelationships();
	}

	/**
	 * Returns a list of underlying DbRelationships.
	 */
	public List getDbRelationships() {
		return dbRelationshipsRef;
	}

	/** Appends a DbRelationship to the existing list of DbRelationships.*/
	public void addDbRelationship(DbRelationship dbRel) {
		//Adding a second is creating a flattened relationship.
		//Ensure that the new relationship properly continues on the flattened path
		int numDbRelationships = dbRelationships.size(); 
		if (numDbRelationships > 0) {
			DbRelationship lastRel =
				(DbRelationship)dbRelationships.get(numDbRelationships - 1);
			if (!lastRel
				.getTargetEntityName()
				.equals(dbRel.getSourceEntity().getName())) {
				throw new CayenneRuntimeException(
					"Error adding db relationship "
						+ dbRel
						+ " to ObjRelationship "
						+ this
						+ " because the source of the newly added relationship is not the target of the previous relationship in the chain");
			}
			isFlattened = true;
			//Now there will be more than one dbRel - this is a flattened relationship
		}
		dbRelationships.add(dbRel);
		this.isReadOnly = this.newReadOnlyValue();
	}

	/** Removes a relationship <code>dbRel</code> from the list of relationships. */
	public void removeDbRelationship(DbRelationship dbRel) {
		dbRelationships.remove(dbRel);
		//If we removed all but one dbRel, then it's no longer flattened
		if (dbRelationships.size() <= 1) {
			isFlattened = false;
		}
		this.isReadOnly = this.newReadOnlyValue();
	}

	public void clearDbRelationships() {
		dbRelationships.clear();
	}

	// calculates a new readonly value after having added/removed dbRelationships
	private boolean newReadOnlyValue() {
		//Quickly filter the single dbrel case
		if (dbRelationships.size() < 2) {
			return false;
		}

		//Also quickly filter any really complex db rel cases
		if (dbRelationships.size() > 2) {
			return true;
		}

		//Now check for a toMany -> toOne series (to return false)
		DbRelationship firstRel = (DbRelationship) dbRelationships.get(0);
		DbRelationship secondRel = (DbRelationship) dbRelationships.get(1);

		//First toOne or second toMany means read only
		if (!firstRel.isToMany() || secondRel.isToMany()) {
			return true;
		}

		//Relationship type is in order, now we only have to check the intermediate table
		DataMap map = firstRel.getTargetEntity().getDataMap();
		if (map == null) {
			throw new CayenneRuntimeException(
				this.getClass().getName()
					+ " could not obtain a DataMap for the destination of "
					+ firstRel.getName());
		}

		DbEntity intermediateEntity = map.getDbEntity(firstRel.getTargetEntityName(), true);
		List pkAttribs = intermediateEntity.getPrimaryKey();
		Iterator allAttribs = intermediateEntity.getAttributes().iterator();
		while (allAttribs.hasNext()) {
			if (!pkAttribs.contains(allAttribs.next())) {
				return true;
				//one of the attributes of intermediate entity is not in the pk.  Must be readonly
			}
		}

		return false;
	}

	/**
	 * Returns true if the relationship is a "flattened" relationship.
	 * This means that the ObjRelationship represents a series of DbRelationships (a relationship path)
	 * transparently.  All flattened relationships are "readable", but only those formed across a many-many link table
	 * (with no custom attributes other than foreign keys) can be automatically written.  isReadOnly handles that 
	 * @see #isReadOnly
	 * @return flag indicating if the relationship is flattened or not
	 */
	public boolean isFlattened() {
		return isFlattened;
	}

	/**
	 * Returns true if the relationship is flattened, but is not of the single case that can have automatic write support
	 * Otherwise, it returns false.
	 * @return flag indicating if the relationship is read only or not
	 */
	public boolean isReadOnly() {
		return isReadOnly;
	}

	/**
	 * Returns the deleteRule.
	 * The delete rule is a constant from the DeleteRule class, and specifies
	 * what should happen to the destination object when the source object is 
	 * deleted.
	 * @return int a constant from DeleteRule
	 * @see #setDeleteRule
	 */
	public int getDeleteRule() {
		return deleteRule;
	}

	/**
	 * Sets the deleteRule.
	 * @param deleteRule The deleteRule to set
	 * @see #getDeleteRule
	 * @throws IllegalArgumentException if the value is not a known value.
	 */
	public void setDeleteRule(int value) {
		if ((value != DeleteRule.CASCADE)
			&& (value != DeleteRule.DENY)
			&& (value != DeleteRule.NULLIFY) 
			&& (value != DeleteRule.NO_ACTION)) {

			throw new IllegalArgumentException(
				"Delete rule value "
					+ value
					+ " is not a constant from the DeleteRule class");
		}
		this.deleteRule = value;
	}

}
