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
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.gui.event.AttributeEvent;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.util.CayenneTableModel;
import org.objectstyle.cayenne.gui.util.MapUtil;
import org.objectstyle.cayenne.map.*;

/** 
 * Model for DbEntity attributes. Allows adding/removing 
 * attributes, modifying types and names.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbAttributeTableModel extends CayenneTableModel {
	// Columns
	private static final int DB_ATTRIBUTE_NAME = 0;
	private static final int DB_ATTRIBUTE_TYPE = 1;
	private static final int DB_ATTRIBUTE_PRIMARY_KEY = 2;
	private static final int DB_ATTRIBUTE_MANDATORY = 3;
	private static final int DB_ATTRIBUTE_MAX = 4;
	private static final int DB_ATTRIBUTE_PRECISION = 5;

	protected DbEntity entity;

	public DbAttributeTableModel(
		DbEntity entity,
		Mediator mediator,
		Object eventSource) {

		this(entity, mediator, eventSource, entity.getAttributeList());
		this.entity = entity;
	}
	
   public DbAttributeTableModel(
		DbEntity entity,
		Mediator mediator,
		Object eventSource,
		java.util.List objectList) {

		super(mediator, eventSource, objectList);
	}

    public int nameColumnInd() {
    	return DB_ATTRIBUTE_NAME;
    }
    
    public int typeColumnInd() {
    	return DB_ATTRIBUTE_TYPE;
    }
    
    public int mandatoryColumnInd() {
    	return DB_ATTRIBUTE_MANDATORY;
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

	public String getColumnName(int col) {
		switch(col) {
			case DB_ATTRIBUTE_NAME: return "Name";
			case DB_ATTRIBUTE_TYPE: return "Type";
			case DB_ATTRIBUTE_PRIMARY_KEY: return "PK";
			case DB_ATTRIBUTE_PRECISION: return "Precision";
			case DB_ATTRIBUTE_MANDATORY: return "Mandatory";
			case DB_ATTRIBUTE_MAX: return "Max Length";
			default: return "";
		}
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
		DbAttribute attr = getAttribute(row);

		if (attr == null) {
			return "";
		}

		switch (column) {
			case DB_ATTRIBUTE_NAME :
				return getAttributeName(attr);
			case DB_ATTRIBUTE_TYPE :
				return getAttributeType(attr);
			case DB_ATTRIBUTE_PRIMARY_KEY :
				return isPrimaryKey(attr);
			case DB_ATTRIBUTE_PRECISION :
				return getPrecision(attr);
			case DB_ATTRIBUTE_MANDATORY :
				return isMandatory(attr);
			case DB_ATTRIBUTE_MAX :
				return getMaxLength(attr);
			default :
				return "";
		}
	}

	public void setValueAt(Object newVal, int row, int col) {
		DbAttribute attr = getAttribute(row);
		if (null == attr) {
			return;
		}

		AttributeEvent e = new AttributeEvent(eventSource, attr, entity);

		switch (col) {
			case DB_ATTRIBUTE_NAME :
				e.setOldName(attr.getName());
				setAttributeName((String) newVal, attr);
				fireTableCellUpdated(row, col);
				break;
			case DB_ATTRIBUTE_TYPE :
				setAttributeType((String) newVal, attr);
				break;
			case DB_ATTRIBUTE_PRIMARY_KEY :
				setPrimaryKey((Boolean) newVal, attr);
				fireTableCellUpdated(row, DB_ATTRIBUTE_MANDATORY);
				break;
			case DB_ATTRIBUTE_PRECISION :
				setPrecision((String) newVal, attr);
				break;
			case DB_ATTRIBUTE_MANDATORY :
				setMandatory((Boolean) newVal, attr);
				break;
			case DB_ATTRIBUTE_MAX :
				setMaxLength((String) newVal, attr);
				break;
		}

		mediator.fireDbAttributeEvent(e);
	}


	public String getMaxLength(DbAttribute attr) {
		return (attr.getMaxLength() >= 0)
			? String.valueOf(attr.getMaxLength())
			: "";
	}

	public String getAttributeName(DbAttribute attr) {
		return attr.getName();
	}

	public String getAttributeType(DbAttribute attr) {
		return TypesMapping.getSqlNameByType(attr.getType());
	}

	public String getPrecision(DbAttribute attr) {
		return (attr.getPrecision() >= 0)
			? String.valueOf(attr.getPrecision())
			: "";
	}

	public Boolean isPrimaryKey(DbAttribute attr) {
		return (attr.isPrimaryKey()) ? Boolean.TRUE : Boolean.FALSE;
	}

	public Boolean isMandatory(DbAttribute attr) {
		return (attr.isMandatory()) ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public void setMaxLength(String newVal, DbAttribute attr) {
		if (newVal == null || newVal.trim().length() <= 0) {
			attr.setMaxLength(-1);
		} else {
			try {
				attr.setMaxLength(Integer.parseInt(newVal));
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(
					null,
					"Invalid Max Length value - "
						+ newVal
						+ ", only numbers are allowed",
					"Invalid Maximum Length",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
	}

	public void setAttributeName(String newVal, DbAttribute attr) {
		String newName = newVal.trim();
		String oldName = attr.getName();
		MapUtil.setAttributeName(attr, newName);
	}

	public void setAttributeType(String newVal, DbAttribute attr) {
		attr.setType(TypesMapping.getSqlTypeByName(newVal));
	}

	public void setPrecision(String newVal, DbAttribute attr) {
		if (newVal == null || newVal.trim().length() <= 0) {
			attr.setPrecision(-1);
		} else {
			try {
				attr.setPrecision(Integer.parseInt(newVal));
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(
					null,
					"Invalid precision value - "
						+ newVal
						+ ", only numbers are allowed",
					"Invalid Precision Value",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void setPrimaryKey(Boolean newVal, DbAttribute attr) {
		attr.setPrimaryKey(newVal.booleanValue());
		if (newVal.booleanValue()) {
			attr.setMandatory(true);
		}
	}

	public void setMandatory(Boolean newVal, DbAttribute attr) {
		attr.setMandatory(newVal.booleanValue());
	}


	public boolean isCellEditable(int row, int col) {
		DbAttribute attrib = getAttribute(row);
		if (null == attrib)
			return false;
		else if (col == mandatoryColumnInd()) {
			if (attrib.isPrimaryKey()) {
				return false;
			}
		}
		return true;
	}
}
