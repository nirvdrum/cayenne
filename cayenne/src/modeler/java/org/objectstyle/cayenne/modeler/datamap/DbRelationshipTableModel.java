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
package org.objectstyle.cayenne.modeler.datamap;

import java.util.ArrayList;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.MapUtil;

/** 
 * Table model for DbRelationship table.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbRelationshipTableModel extends CayenneTableModel {
	// Columns
	static final int NAME = 0;
	static final int TARGET = 1;
	static final int TO_DEPENDENT_KEY = 2;
	static final int CARDINALITY = 3;

	protected DbEntity entity;

	public DbRelationshipTableModel(
		DbEntity entity,
		EventController mediator,
		Object eventSource) {

		super(mediator, eventSource, new ArrayList(entity.getRelationships()));
		this.entity = entity;
	}

	/**
	 * Returns DbRelationship class.
	 */
	public Class getElementsClass() {
		return DbRelationship.class;
	}

	public int getColumnCount() {
		return 4;
	}

	public String getColumnName(int col) {
		switch (col) {
			case NAME :
				return "Name";
			case TARGET :
				return "Target";
			case TO_DEPENDENT_KEY :
				return "To Dep PK";
			case CARDINALITY :
				return "To Many";
			default :
				return null;
		}
	}

	public Class getColumnClass(int col) {
		switch (col) {
			case TO_DEPENDENT_KEY :
			case CARDINALITY :
				return Boolean.class;
			default :
				return String.class;
		}
	}

	public DbRelationship getRelationship(int row) {
		return (row >= 0 && row < objectList.size())
			? (DbRelationship) objectList.get(row)
			: null;
	}

	public Object getValueAt(int row, int col) {
		DbRelationship rel = getRelationship(row);
		if (rel == null) {
			return null;
		}

		switch (col) {
			case NAME :
				return rel.getName();
			case TARGET :
				DbEntity target = (DbEntity) rel.getTargetEntity();
				return (target != null) ? target.getName() : null;
			case TO_DEPENDENT_KEY :
				return new Boolean(rel.isToDependentPK());
			case CARDINALITY :
				return new Boolean(rel.isToMany());
			default :
				return null;
		}
	}

	public void setUpdatedValueAt(Object aValue, int row, int column) {

		DbRelationship rel = getRelationship(row);
		// If name column
		if (column == NAME) {
			String text = (String) aValue;
			String old_name = rel.getName();
			MapUtil.setRelationshipName(entity, rel, text);
			RelationshipEvent e =
				new RelationshipEvent(eventSource, rel, entity, old_name);
			mediator.fireDbRelationshipEvent(e);
			fireTableCellUpdated(row, column);
		}
		// If target column
		else if (column == TARGET) {
			if (null == aValue)
				return;
			String target_name = aValue.toString();
			if (target_name == null)
				target_name = "";
			target_name = target_name.trim();
			// Set new target, if applicable
			DbEntity target = null;
			if ("".equals(target_name))
				target = null;
			else
				target = mediator.getCurrentDataMap().getDbEntity(target_name, true);
			rel.setTargetEntity(target);
			RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
			mediator.fireDbRelationshipEvent(e);
		} else if (column == TO_DEPENDENT_KEY) {
			Boolean temp = (Boolean) aValue;
			rel.setToDependentPK(temp.booleanValue());
			RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
			mediator.fireDbRelationshipEvent(e);
		} else if (column == CARDINALITY) {
			Boolean temp = (Boolean) aValue;
			rel.setToMany(temp.booleanValue());
			RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
			mediator.fireDbRelationshipEvent(e);
		}
		fireTableRowsUpdated(row, row);
	}

	/** Relationship just needs to be removed from the model. 
	 *  It is already removed from the DataMap. */
	void removeRelationship(Relationship rel) {
		objectList.remove(rel);
		fireTableDataChanged();
	}

	public boolean isCellEditable(int row, int col) {
		DbRelationship rel = getRelationship(row);
		if (rel == null) {
			return false;
		} else if (col == TO_DEPENDENT_KEY) {
			return MapUtil.isValidForDepPk(rel);
		}
		return true;
	}
}
