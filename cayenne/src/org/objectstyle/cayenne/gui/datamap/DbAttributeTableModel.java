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
import javax.swing.*;
import javax.swing.table.*;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.dba.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;


/** Model for the Db Entity attributes.
 *  Allows adding/removing attributes, modifying the types and the names.
 *  Add/remove changes are cached and saved into DbEntity only when commit()
 *  is called.*/
class DbAttributeTableModel extends AbstractTableModel
{
	protected Mediator mediator;
	protected Object src;
	
	private DbEntity entity;
	private DataMap dataMap;
	/** Add/remove changes will be made not to entity but to attributeList.
	 *  The changes will be copied to entity in commit().
	 *  Assumption: the attributes themselves are passed by reference. 
	 *  Any modification to the attribute from the list (change in name, type) 
	 *  will be reflected in the entity. */
	private java.util.List attributeList;
	
	// Columns
	static final int DB_ATTRIBUTE_NAME = 0;
	static final int DB_ATTRIBUTE_TYPE = 1;
	static final int DB_ATTRIBUTE_PRIMARY_KEY = 2;
	static final int DB_ATTRIBUTE_PRECISION = 3;
	static final int DB_ATTRIBUTE_MANDATORY = 4;
	static final int DB_ATTRIBUTE_MAX = 5;

	/** To provide reference to String class. */	
	private String	string = new String();
	/** To provide reference to Boolean class. */	
	private Boolean bool = new Boolean(false);
	
	public DbAttributeTableModel(DbEntity temp_entity
	,Mediator temp_mediator, Object temp_src)
	{
		entity = temp_entity;
		mediator = temp_mediator;
		attributeList = entity.getAttributeList();
		src = temp_src;
		dataMap = temp_mediator.getCurrentDataMap();
	}
	
	public DbAttribute getAttribute(int row) {
		if (row < 0 || row >= attributeList.size())
			return null;
		DbAttribute attribute = (DbAttribute)attributeList.get(row);
		return attribute;
	}
	


	public int getRowCount() {
		return attributeList.size();
	}
	
	public int getColumnCount(){
		return 6;
	}
	
	public String getColumnName(int column) {
		if (column == DB_ATTRIBUTE_NAME)
			return "Name";
		else if (column == DB_ATTRIBUTE_TYPE)
			return "Type";
		else if (column == DB_ATTRIBUTE_PRIMARY_KEY) 
			return "PK";
		else if (column == DB_ATTRIBUTE_PRECISION) 
			return "Precision";
		else if (column == DB_ATTRIBUTE_MANDATORY) 
			return "Mandatory";
		else if (column == DB_ATTRIBUTE_MAX) 
			return "Max length";
		else return "";
	}
	
	public Class getColumnClass(int col) {
		switch (col) {
			case DB_ATTRIBUTE_PRIMARY_KEY:
			case DB_ATTRIBUTE_MANDATORY:
				return bool.getClass();
			default:
				return string.getClass();
		}
	}
	
	public Object getValueAt(int row, int column)
	{
		DbAttribute attrib = (DbAttribute)attributeList.get(row);
		if (null == attrib)
			return "";
		else if (column == DB_ATTRIBUTE_NAME)
			return attrib.getName();
		else if (column == DB_ATTRIBUTE_TYPE) {
			return TypesMapping.getSqlNameByType(attrib.getType());
		}
		else if (column == DB_ATTRIBUTE_PRIMARY_KEY) {
			if (attrib.isPrimaryKey())
				return new Boolean(true);
			else 
				return new Boolean(false);
		}
		else if (column == DB_ATTRIBUTE_PRECISION) {
			if (-1 == attrib.getPrecision())
				return "";
			else 
				return String.valueOf(attrib.getPrecision());
		}
		else if (column == DB_ATTRIBUTE_MANDATORY) {
			if (attrib.isMandatory())
				return new Boolean(true);
			else 
				return new Boolean(false);
		}
		else if (column == DB_ATTRIBUTE_MAX) {
			if (-1 == attrib.getMaxLength())
				return "";
			else 
				return String.valueOf(attrib.getMaxLength());
		}
		else {
			System.out.println("DbAttributeTableModel::getValueAt(), column "
							   + column + " does not exist");
			return "";
		}
	}// End getValueAt()
	
    
    public void setValueAt(Object aValue, int row, int column) {
		DbAttribute attrib = (DbAttribute)attributeList.get(row);
		AttributeEvent e = null;
		if (null == attrib) {
			JOptionPane.showMessageDialog(Editor.getFrame()
					, "The last edited value is not saved"
					, "Value not saved", JOptionPane.WARNING_MESSAGE);
			return;
		}
		else if (column == DB_ATTRIBUTE_NAME) {
			String new_name = ((String)aValue).trim();
			String old_name = attrib.getName();
			GuiFacade.setDbAttributeName(dataMap, attrib, new_name);
			e = new AttributeEvent(src, attrib, entity, old_name);
			mediator.fireDbAttributeEvent(e);
			fireTableCellUpdated(row, column);
		}// End DB_ATTRIBUTE column
		else if (column == DB_ATTRIBUTE_TYPE) {
			String type_str = (String)aValue;
			int type = TypesMapping.getSqlTypeByName(type_str);
			attrib.setType(type);
		}
		else if (column == DB_ATTRIBUTE_PRIMARY_KEY) {
			Boolean primary = (Boolean)aValue;
			attrib.setPrimaryKey(primary.booleanValue());
			if (primary.booleanValue())
				attrib.setMandatory(true);
			fireTableCellUpdated(row, DB_ATTRIBUTE_MANDATORY);
		}
		else if (column == DB_ATTRIBUTE_PRECISION) {
			String str = (String)aValue;
			if (null == str || str.trim().length() <=0) {
				// FIXME!! Change to static variable NOT_DEFINED
				attrib.setPrecision(-1);
			}
			else {
				try  {
					int precision = Integer.parseInt(str);
					attrib.setPrecision(precision);
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null, "Invalid precision value - " 
							+ aValue + ", only numbers are allowed"
							, "Invalid Precision value", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		else if (column == DB_ATTRIBUTE_MANDATORY) {
			Boolean mandatory = (Boolean)aValue;
			attrib.setMandatory(mandatory.booleanValue());
		}
		else if (column == DB_ATTRIBUTE_MAX) {
			String str = (String)aValue;
			if (null == str || str.trim().length() <=0) {
				// FIXME!! Change to static variable NOT_DEFINED
				attrib.setMaxLength(-1);
			}
			else {
				try  {
					int max_len = Integer.parseInt((String)aValue);
					attrib.setMaxLength(max_len);
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null, "Invalid Max Length value - " 
							+ aValue + ", only numbers are allowed"
							, "Invalid Maximum Length", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		if (null == e)
			e = new AttributeEvent(src, attrib, entity);
		mediator.fireDbAttributeEvent(e);
    }// End setValueAt()

	
	/** Add new attribute to the model and the table. 
	 *  Broadcast AttributeEvent.*/
	public void addRow() {
		String name = NameGenerator.getDbAttributeName();
		DbAttribute temp = new DbAttribute(name, java.sql.Types.OTHER, entity);
		AttributeEvent e;
		e = new AttributeEvent(src, temp, entity, AttributeEvent.ADD);
		attributeList.add(temp);
		entity.addAttribute(temp);
		mediator.fireDbAttributeEvent(e);
		fireTableDataChanged();
	}

	public void removeRow(int row) {
		if (row < 0)
			return;
		Attribute attrib = (Attribute)attributeList.get(row);
		AttributeEvent e;
		e = new AttributeEvent(src, attrib, entity, AttributeEvent.REMOVE);
		attributeList.remove(row);
		entity.removeAttribute(attrib.getName());
		mediator.fireDbAttributeEvent(e);
		fireTableDataChanged();
	}	

	/** Attribute just needs to be removed from the model. 
	 *  It is already removed from the DataMap. */
	void removeAttribute(Attribute attrib) {
		attributeList.remove(attrib);
		fireTableDataChanged();
	}


	public boolean isCellEditable(int row, int col) {
		DbAttribute attrib = (DbAttribute)attributeList.get(row);
		if (null == attrib)
			return false;
		else if (col == DB_ATTRIBUTE_MANDATORY) {
			if (attrib.isPrimaryKey())
				return false;
		}
		return true;
	}// End isCellEditable()

}// End DbAttributeTableModel
