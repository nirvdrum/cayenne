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
package org.objectstyle.cayenne.gui.datamap;

import javax.swing.JOptionPane;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.AttributeEvent;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.gui.util.CayenneTableModel;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.NamedObjectFactory;

/** 
 * Model for DbEntity attributes. Allows adding/removing 
 * attributes, modifying types and names.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
class DbAttributeTableModel extends CayenneTableModel {
	// Columns
	static final int DB_ATTRIBUTE_NAME = 0;
	static final int DB_ATTRIBUTE_TYPE = 1;
	static final int DB_ATTRIBUTE_PRIMARY_KEY = 2;
	static final int DB_ATTRIBUTE_MANDATORY = 3;
	static final int DB_ATTRIBUTE_MAX = 4;
	static final int DB_ATTRIBUTE_PRECISION = 5;

	protected DbEntity entity;
	protected DataMap dataMap;

	public DbAttributeTableModel(
		DbEntity entity,
		Mediator mediator,
		Object eventSource) {

		super(mediator, eventSource, entity.getAttributeList());
		this.entity = entity;
		this.dataMap = mediator.getCurrentDataMap();
	}

	/**
	 * Returns DbAttribute class.
	 */
	public Class getElementsClass() {
		return DbAttribute.class;
	}

	/** 
	 * Returns the number of columns in the table.
	 */
	public int getColumnCount() {
		return 6;
	}

	public DbAttribute getAttribute(int row) {
		return (row >= 0 && row < objectList.size())
			? (DbAttribute) objectList.get(row)
			: null;
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
		else
			return "";
	}


	public Class getColumnClass(int col) {
		switch (col) {
			case DB_ATTRIBUTE_PRIMARY_KEY :
			case DB_ATTRIBUTE_MANDATORY : 
			    return Boolean.class;
			default :
				return String.class;
		}
	}


	public Object getValueAt(int row, int column) {
		DbAttribute attrib = getAttribute(row);
		
		if (attrib == null) {
			return "";
		} else if (column == DB_ATTRIBUTE_NAME)
			return attrib.getName();
		else if (column == DB_ATTRIBUTE_TYPE) {
			return TypesMapping.getSqlNameByType(attrib.getType());
		} else if (column == DB_ATTRIBUTE_PRIMARY_KEY) {
			return (attrib.isPrimaryKey()) ? Boolean.TRUE : Boolean.FALSE;
		} else if (column == DB_ATTRIBUTE_PRECISION) {
			return (attrib.getPrecision() == -1) ? "" : String.valueOf(attrib.getPrecision());
		} else if (column == DB_ATTRIBUTE_MANDATORY) {
			return (attrib.isMandatory()) ? Boolean.TRUE : Boolean.FALSE;
		} else if (column == DB_ATTRIBUTE_MAX) {
			return (attrib.getMaxLength() == -1) ? "" : String.valueOf(attrib.getMaxLength());
		} else {
			return "";
		}
	} 
	
	
	public void setValueAt(Object aValue, int row, int column) {
		DbAttribute attrib = getAttribute(row);
		AttributeEvent e = null;
		if (null == attrib) {
			JOptionPane.showMessageDialog(
				Editor.getFrame(),
				"The last edited value is not saved",
				"Value not saved",
				JOptionPane.WARNING_MESSAGE);
			return;
		} else if (column == DB_ATTRIBUTE_NAME) {
			String new_name = ((String) aValue).trim();
			String old_name = attrib.getName();
			MapUtil.setAttributeName(attrib, new_name);
			e = new AttributeEvent(eventSource, attrib, entity, old_name);
			mediator.fireDbAttributeEvent(e);
			fireTableCellUpdated(row, column);
		}
		else if (column == DB_ATTRIBUTE_TYPE) {
			String type_str = (String) aValue;
			int type = TypesMapping.getSqlTypeByName(type_str);
			attrib.setType(type);
		} else if (column == DB_ATTRIBUTE_PRIMARY_KEY) {
			Boolean primary = (Boolean) aValue;
			attrib.setPrimaryKey(primary.booleanValue());
			if (primary.booleanValue())
				attrib.setMandatory(true);
			fireTableCellUpdated(row, DB_ATTRIBUTE_MANDATORY);
		} else if (column == DB_ATTRIBUTE_PRECISION) {
			String str = (String) aValue;
			if (null == str || str.trim().length() <= 0) {
				// FIXME!! Change to static variable NOT_DEFINED
				attrib.setPrecision(-1);
			} else {
				try {
					int precision = Integer.parseInt(str);
					attrib.setPrecision(precision);
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(
						null,
						"Invalid precision value - "
							+ aValue
							+ ", only numbers are allowed",
						"Invalid Precision value",
						JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		} else if (column == DB_ATTRIBUTE_MANDATORY) {
			Boolean mandatory = (Boolean) aValue;
			attrib.setMandatory(mandatory.booleanValue());
		} else if (column == DB_ATTRIBUTE_MAX) {
			String str = (String) aValue;
			if (null == str || str.trim().length() <= 0) {
				// FIXME!! Change to static variable NOT_DEFINED
				attrib.setMaxLength(-1);
			} else {
				try {
					int max_len = Integer.parseInt((String) aValue);
					attrib.setMaxLength(max_len);
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(
						null,
						"Invalid Max Length value - "
							+ aValue
							+ ", only numbers are allowed",
						"Invalid Maximum Length",
						JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		if (null == e)
			e = new AttributeEvent(eventSource, attrib, entity);
		mediator.fireDbAttributeEvent(e);
	}

	/** Attribute just needs to be removed from the model. 
	 *  It is already removed from the DataMap. */
	public void removeAttribute(Attribute attrib) {
		objectList.remove(attrib);
		fireTableDataChanged();
	}

	public boolean isCellEditable(int row, int col) {
		DbAttribute attrib = getAttribute(row);
		if (null == attrib)
			return false;
		else if (col == DB_ATTRIBUTE_MANDATORY) {
			if (attrib.isPrimaryKey())
				return false;
		}
		return true;
	}

}
