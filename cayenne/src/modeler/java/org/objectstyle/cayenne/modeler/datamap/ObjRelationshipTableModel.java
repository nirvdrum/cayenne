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
package org.objectstyle.cayenne.modeler.datamap;

import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import javax.swing.*;
import javax.swing.table.*;

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.modeler.event.*;
import org.objectstyle.cayenne.modeler.util.*;

/** 
 * Table model to display ObjRelationships. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjRelationshipTableModel extends CayenneTableModel {
	static Logger logObj =
		Logger.getLogger(ObjRelationshipTableModel.class.getName());

	// Columns
	static final int REL_NAME = 0;
	static final int REL_TARGET = 1;
	static final int REL_CARDINALITY = 2;

	protected ObjEntity entity;

	public ObjRelationshipTableModel(
		ObjEntity entity,
		Mediator mediator,
		Object eventSource) {
			
		super(mediator, eventSource, entity.getRelationshipList());
		this.entity = entity;
	}
	
	/**
	 * Returns ObjRelationship class.
	 */
	public Class getElementsClass() {
		return ObjRelationship.class;
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int column) {
		if (column == REL_NAME)
			return "Name";
		else if (column == REL_TARGET)
			return "Target";
		else if (column == REL_CARDINALITY)
			return "To many";
		else
			return "";
	}

	public Class getColumnClass(int col) {
		switch (col) {
			case REL_CARDINALITY :
				return Boolean.class;
			default :
				return String.class;
		}
	}

	public ObjRelationship getRelationship(int row) {
		return (row >= 0 && row < objectList.size())
			? (ObjRelationship) objectList.get(row)
			: null;
	}

	public Object getValueAt(int row, int column) {
		ObjRelationship rel = getRelationship(row);
		// If name column
		if (column == REL_NAME)
			return rel.getName();
		// If target column
		else if (column == REL_TARGET) {
			if (null == rel.getTargetEntity())
				return null;
			return rel.getTargetEntity().getName();
		} else if (column == REL_CARDINALITY)
			return new Boolean(rel.isToMany());
		else
			return "";
	}

	public void setUpdatedValueAt(Object aValue, int row, int column) {
		ObjRelationship rel = getRelationship(row);
		
		// If name column
		if (column == REL_NAME) {
			String text = (String) aValue;
			String old_name = rel.getName();
			MapUtil.setRelationshipName(entity, rel, text);
			RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity, old_name);
			mediator.fireObjRelationshipEvent(e);
			fireTableCellUpdated(row, column);
		}
		// If target column
		else if (column == REL_TARGET) {
			if (null == aValue)
				return;
			String target_name = aValue.toString();
			if (target_name == null)
				target_name = "";
			target_name = target_name.trim();
			// If data hasn't changed, do nothing
			if (rel.getTargetEntity() != null
				&& target_name.equals(rel.getTargetEntity().getName())) {
				return;
			}
			// Remove db relationship mappings.
			rel.clearDbRelationships();
			
			// Set new target, if applicable
			ObjEntity target = null;
			if (!"".equals(target_name)) {
				DataMap map = mediator.getCurrentDataMap();
				target = map.getObjEntity(target_name, true);
			}
			
			rel.setTargetEntity(target);
			RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
			mediator.fireObjRelationshipEvent(e);
		} else if (column == REL_CARDINALITY) {
			Boolean temp = (Boolean) aValue;
			rel.setToMany(temp.booleanValue());
			RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
			mediator.fireObjRelationshipEvent(e);
		}
		fireTableRowsUpdated(row, row);
	} 


	public void removeRow(int row) {
		if (row < 0)
			return;
		Relationship rel = getRelationship(row);
		RelationshipEvent e;
		e = new RelationshipEvent(eventSource, rel, entity, RelationshipEvent.REMOVE);
		mediator.fireObjRelationshipEvent(e);
		objectList.remove(row);
		entity.removeRelationship(rel.getName());
		fireTableRowsDeleted(row, row);
	}

	/** Relationship just needs to be removed from the model. 
	 *  It is already removed from the DataMap. */
	void removeRelationship(Relationship rel) {
		objectList.remove(rel);
		fireTableDataChanged();
	}

	public boolean isCellEditable(int row, int col) {
		return true;
	} 

}
