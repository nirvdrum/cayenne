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

import org.apache.log4j.Logger;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.AttributeEvent;
import org.objectstyle.cayenne.modeler.util.*;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.NamedObjectFactory;

/** 
 * Model for the Object Entity attributes and for Obj to 
 * DB Attribute Mapping tables. Allows adding/removing attributes,
 * modifying the types and the names.
 * 
 * @author Michael Misha Shengaout. 
 * @author Andrei Adamchik
 */
public class ObjAttributeTableModel extends CayenneTableModel {
	// Columns
	static final int OBJ_ATTRIBUTE = 0;
	static final int OBJ_ATTRIBUTE_TYPE = 1;
	static final int DB_ATTRIBUTE = 2;
	static final int DB_ATTRIBUTE_TYPE = 3;

	static final Logger logObj =
		Logger.getLogger(ObjAttributeTableModel.class.getName());

	protected ObjEntity entity;
	protected DbEntity dbEntity;

	public ObjAttributeTableModel(
		ObjEntity entity,
		EventController mediator,
		Object eventSource) {

		super(mediator, eventSource, entity.getAttributeList());
		this.entity = entity;
		this.dbEntity = entity.getDbEntity();
	}

	/**
	 * Returns ObjAttribute class.
	 */
	public Class getElementsClass() {
		return ObjAttribute.class;
	}

	public DbEntity getDbEntity() {
		return dbEntity;
	}

	public ObjAttribute getAttribute(int row) {
		return (row >= 0 && row < objectList.size())
			? (ObjAttribute) objectList.get(row)
			: null;
	}

	/** Refreshes DbEntity to current db entity within ObjEntity.*/
	public void resetDbEntity() {
		if (dbEntity == entity.getDbEntity()) {
			return;
		}

		boolean wasShowing = isShowingDb();
		dbEntity = entity.getDbEntity();
		boolean isShowing = isShowingDb();

		if (wasShowing != isShowing) {
			fireTableStructureChanged();
		}

		fireTableDataChanged();
	}

	public boolean isShowingDb() {
		return dbEntity != null;
	}

	public int getColumnCount() {
		// If showing only obj attributes, show 2 columns 
		// otherwise show 5 additional columns (7 total) for Db Attributes.
		return (isShowingDb()) ? 4 : 2;
	}

	public String getColumnName(int column) {
		if (column == OBJ_ATTRIBUTE)
			return "Obj Attribute";
		else if (column == OBJ_ATTRIBUTE_TYPE)
			return "Type";
		else if (column == DB_ATTRIBUTE)
			return "Db Attribute";
		else if (column == DB_ATTRIBUTE_TYPE)
			return "Type";
		else
			return "";
	}

	public Object getValueAt(int row, int column) {
		ObjAttribute attrib = getAttribute(row);

		// If name column
		if (column == OBJ_ATTRIBUTE) {
			return attrib.getName();
		}
		// If type column
		else if (column == OBJ_ATTRIBUTE_TYPE) {
			return attrib.getType();
		} else {
			DbAttribute db_attrib = attrib.getDbAttribute();
			if (null == db_attrib)
				return null;
			else if (column == DB_ATTRIBUTE)
				return db_attrib.getName();
			else if (column == DB_ATTRIBUTE_TYPE) {
				return TypesMapping.getSqlNameByType(db_attrib.getType());
			}
		}
		return "";
	}

	public void setUpdatedValueAt(Object aValue, int row, int column) {
		
		ObjAttribute attrib = getAttribute(row);
		String text = (aValue != null) ? ((String) aValue).trim() : "";

		// If "Obj Name" column
		if (column == OBJ_ATTRIBUTE) {
			String old_name = attrib.getName();
			MapUtil.setAttributeName(attrib, text);
			AttributeEvent e =
				new AttributeEvent(eventSource, attrib, entity, old_name);
			mediator.fireObjAttributeEvent(e);
			fireTableCellUpdated(row, column);
		}
		// If Obj "Type" column
		else if (column == OBJ_ATTRIBUTE_TYPE) {

			String type = (text.length() == 0) ? null : text;
			attrib.setType(type);
			mediator.fireObjAttributeEvent(
				new AttributeEvent(
					eventSource,
					attrib,
					entity,
					AttributeEvent.CHANGE));
			fireTableCellUpdated(row, column);
		} else {
			DbAttribute db_attrib = attrib.getDbAttribute();
			if (column == DB_ATTRIBUTE) {
				// If db attrib exist, associate it with obj attribute
				if (text.length() > 0) {
					db_attrib = (DbAttribute) dbEntity.getAttribute(text);
					attrib.setDbAttribute(db_attrib);
				}
				// If name is erased, remove db attribute from obj attribute.
				else if (db_attrib != null && text.length() == 0) {
					attrib.setDbAttribute(null);
				}
			} else {
				return;
			}
			fireTableRowsUpdated(row, row);
		}
		AttributeEvent ev = new AttributeEvent(eventSource, attrib, entity);
		mediator.fireObjAttributeEvent(ev);
	}

	/** Attribute just needs to be removed from the model. 
	 *  It is already removed from the DataMap. */
	public void removeAttribute(Attribute attrib) {
		objectList.remove(attrib);
		fireTableDataChanged();
	}

	public boolean isCellEditable(int row, int col) {
		// Check if allow obj editing
		if (col == OBJ_ATTRIBUTE || col == OBJ_ATTRIBUTE_TYPE)
			return true;
		else {
			// Allow choosing different db attributes
			if (col == DB_ATTRIBUTE)
				return true;
			// Don't allow editing db attribute parameters
			else
				return false;
		}
	}
}
