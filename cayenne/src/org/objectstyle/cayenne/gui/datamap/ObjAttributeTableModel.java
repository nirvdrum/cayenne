package org.objectstyle.cayenne.gui.datamap;
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

import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.*;

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.dba.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;


/** Model for the Object Entity attributes and for Obj to DB Attribute Mapping tables.
 *  Allows adding/removing attributes, modifying the types and the names.
 *  Add/remove changes are cached and saved into ObjEntity only when commit()
 *  is called.
 *  @author Michael Misha Shengaout. */
class ObjAttributeTableModel extends AbstractTableModel
{
	static final Logger logObj = Logger.getLogger(ObjAttributeTableModel.class.getName());
	
	Mediator mediator;
	/** The pane to use as source of AttributeEvents. */
	Object src;
	private ObjEntity entity;
	private DbEntity dbEntity = null;
	/** Add/remove changes will be made not to entity but to attributeList.
	 *  The changes will be copied to entity in commit().
	 *  Assumption: the attributes themselves are passed by reference. 
	 *  Any modification to the attribute from the list (change in name, type) 
	 *  will be reflected in the entity. */
	private java.util.List attributeList;
	/** Whether to show the mapping to the db attributes.*/
	private boolean showDb = false;
	
	// Columns
	static final int OBJ_ATTRIBUTE = 0;
	static final int OBJ_ATTRIBUTE_TYPE = 1;
	static final int DB_ATTRIBUTE = 2;
	static final int DB_ATTRIBUTE_TYPE = 3;
	
	public ObjAttributeTableModel(ObjEntity temp_entity
	, Mediator temp_mediator, Object temp_src)
	{
		entity = temp_entity;
		mediator = temp_mediator;
		dbEntity = entity.getDbEntity();
		src = temp_src;
		attributeList = entity.getAttributeList();
		showDb = (entity.getDbEntity() != null);
	}
	
	public DbEntity getDbEntity(){ 
		return dbEntity;
	}

	public ObjAttribute getAttribute(int row) {
		if (row < 0 || row >= attributeList.size())
			return null;
		ObjAttribute attribute = (ObjAttribute)attributeList.get(row);
		return attribute;
	}
	

	
	/** Refreshes DbEntity to current db entity within ObjEntity.*/
	public void resetDbEntity() {
		if (dbEntity == entity.getDbEntity())
			return;
		boolean change;
		change = (   dbEntity == null && entity.getDbEntity() != null
				  || dbEntity != null && entity.getDbEntity() == null);
		
		dbEntity = entity.getDbEntity();
		showDb = (null != dbEntity);
		if (change)
			fireTableStructureChanged();
		fireTableDataChanged();
	}
	
	public int getRowCount() {
		return attributeList.size();
	}
	
	public int getColumnCount()
	{
		// If showing only obj attributes, show 2 columns 
		// otherwise show 5 additional columns (7 total) for Db Attributes.
		if (!showDb)
			return 2;
		else
			return 4;
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
		else return "";
	}
		
	
	public Object getValueAt(int row, int column)
	{
		ObjAttribute attrib = (ObjAttribute)attributeList.get(row);
		// If name column
		if (column == OBJ_ATTRIBUTE)
			return attrib.getName();
		// If type column
		else if (column == OBJ_ATTRIBUTE_TYPE)
			return attrib.getType();
		else {
			DbAttribute db_attrib = attrib.getDbAttribute();
			if (null == db_attrib)
				return null;
			else if (column == DB_ATTRIBUTE)
				return db_attrib.getName();
			else if (column == DB_ATTRIBUTE_TYPE) {
				return TypesMapping.getSqlNameByType(db_attrib.getType());
			}
		}// End if DbAttrib column
		return "";
	}// End getValueAt()
	
    
    public void setValueAt(Object aValue, int row, int column) {
    	logObj.fine("setValue(), row = "+ row + ",attributeList size is " + attributeList.size());
		ObjAttribute attrib = (ObjAttribute)attributeList.get(row);
		String text = "";
		if (null != aValue)
			text = ((String)aValue).trim();
		// If "Obj Name" column
		if (column == OBJ_ATTRIBUTE) {
			String old_name = attrib.getName();
			GuiFacade.setObjAttributeName(mediator.getCurrentDataMap()
										, attrib, text);
			AttributeEvent e;
			e = new AttributeEvent(src, attrib, entity, old_name);
			mediator.fireObjAttributeEvent(e);
			fireTableCellUpdated(row, column);
		}
		// If Obj "Type" column
		else if (column == OBJ_ATTRIBUTE_TYPE) {
			attrib.setType((String)aValue);
			AttributeEvent e;
			e = new AttributeEvent(src, attrib, entity, AttributeEvent.CHANGE);
			mediator.fireObjAttributeEvent(e);
			fireTableCellUpdated(row, column);
		}
		else {
			DbAttribute db_attrib = attrib.getDbAttribute();
			if (column == DB_ATTRIBUTE) {
				// If db attrib exist, associate it with obj attribute
				if (text.length() > 0) {
					db_attrib = (DbAttribute)dbEntity.getAttribute(text);
					attrib.setDbAttribute(db_attrib);
				}
				// If name is erased, remove db attribute from obj attribute.
				else if (db_attrib != null && text.length() == 0) {
					attrib.setDbAttribute(null);
				}
			}// End DB_ATTRIBUTE column
			else {
				return;
			}
        	fireTableRowsUpdated(row, row);
		}// End DbAttribute columns
		AttributeEvent ev = new AttributeEvent(src, attrib, entity);
		mediator.fireObjAttributeEvent(ev);
    }// End setValueAt()

	
	/** Add new attribute to the model and the table. 
	 *  Broadcast AttributeEvent.*/
	public void addRow() {
		String name = NameGenerator.getObjAttributeName();
		ObjAttribute temp = new ObjAttribute(name, "", entity);
		AttributeEvent e;
		e = new AttributeEvent(src, temp, entity, AttributeEvent.ADD);
		attributeList.add(temp);		
		entity.addAttribute(temp);
		mediator.fireObjAttributeEvent(e);
		fireTableDataChanged();
	}

	public void removeRow(int row) {
		if (row < 0)
			return;
		Attribute attrib = (Attribute)attributeList.get(row);
		AttributeEvent e;
		e = new AttributeEvent(src, attrib, entity, AttributeEvent.REMOVE);
		attributeList.remove(row);
    	logObj.fine("Removed row " + row + ", attributeList size is " + attributeList.size());
		entity.removeAttribute(attrib.getName());
		mediator.fireObjAttributeEvent(e);
		fireTableDataChanged();
	}
	
	/** Attribute just needs to be removed from the model. 
	 *  It is already removed from the DataMap. */
	void removeAttribute(Attribute attrib) {
		attributeList.remove(attrib);
		fireTableDataChanged();
	}

	public boolean isCellEditable(int row, int col) {
		// Check if allow obj editing
		if (col == OBJ_ATTRIBUTE || col == OBJ_ATTRIBUTE_TYPE)
			return true;
		else {
			ObjAttribute attrib = (ObjAttribute)attributeList.get(row);
			DbAttribute db_attrib = attrib.getDbAttribute();
			// Allow choosing different db attributes
			if (col == DB_ATTRIBUTE)
				return true;
			// Don't allow editing db attribute parameters
			else 
				return false;
		}// End DbAttribute columns
	}// End isCellEditable()
}// End ObjAttributeTableModel

