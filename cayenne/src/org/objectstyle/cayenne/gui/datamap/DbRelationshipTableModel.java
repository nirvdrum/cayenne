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

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import org.objectstyle.cayenne.gui.util.XmlFilter;

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;


/** Table model for DbRelationship table.
 *  @author Michael Misha Shengaout. */
class DbRelationshipTableModel extends AbstractTableModel {

	Mediator mediator;
	/** The pane to use as source of AttributeEvents. */
	Object src;

	private DbEntity entity;
	private java.util.List relList;
	
	// Columns
	public static final int NAME 		= 0;
	public static final int TARGET 		= 1;
	public static final int FOREIGN_KEY = 2;
	public static final int CARDINALITY = 3;
	
	/** To provide reference to String class. */	
	private String	string = new String();
	/** To provide reference to Boolean class. */	
	private Boolean bool = new Boolean(false);
	
	public DbRelationshipTableModel(DbEntity temp_entity
	, Mediator temp_mediator, Object temp_src)
	{
		entity = temp_entity;
		relList = entity.getRelationshipList();
		mediator = temp_mediator;
		src = temp_src;
	}

	public int getRowCount() {
		return relList.size();
	}
	
	public int getColumnCount(){
		return 4;
	}
	
	public String getColumnName(int col)
	{		
		switch (col)
		{
			case NAME:
				return "Name";
			case TARGET:
				return "Target";
			case FOREIGN_KEY:
				return "Foreign key";
			case CARDINALITY:
				return "To many";
			default:
				return "";
		}// End switch
	}

	public Class getColumnClass(int col) {
		switch (col) {
			case FOREIGN_KEY:
			case CARDINALITY:
				return bool.getClass();
			default:
				return string.getClass();
		}
	}
	


	
	public DbRelationship getRelationship(int row) {
		if (row < 0 || row >= relList.size())
			return null;
		DbRelationship rel = (DbRelationship)relList.get(row);
		return rel;		
	}
	
	public Object getValueAt(int row, int col)
	{
		if (null == relList || relList.size() <= row)
			return "";
		DbRelationship rel = getRelationship(row);
		if (rel == null)
			return "";
		switch (col)
		{
			case NAME:
				return rel.getName() != null ? rel.getName() : "";
			case TARGET:
				DbEntity temp = (DbEntity)rel.getTargetEntity();
				if (null != temp)
					return temp.getName();
				else
					return "";
			case FOREIGN_KEY:
				return new Boolean(rel.isToDependentPK());
			case CARDINALITY:
				return new Boolean(rel.isToMany());
			default:
				return "";
		}// End switch
	}
	
	
    
    public void setValueAt(Object aValue, int row, int column) {
		DbRelationship rel = (DbRelationship)relList.get(row);
		// If name column
		if (column == NAME) {
			String text = (String)aValue;
			String old_name = rel.getName();
			GuiFacade.setDbRelationshipName(entity, rel, text);
			RelationshipEvent e = new RelationshipEvent(src, rel, entity, old_name);
			mediator.fireDbRelationshipEvent(e);
			fireTableCellUpdated(row, column);
		}
		// If target column
		else if (column == TARGET) {
			String target_name = aValue.toString();
			if (target_name == null)
				target_name = "";
			target_name = target_name.trim();
			// Set new target, if applicable
			DbEntity target = null;
			if ("".equals(target_name))
				target = null;
			else
			 	target = mediator.getCurrentDataMap().getDbEntity(target_name);
			rel.setTargetEntity(target);
			RelationshipEvent e = new RelationshipEvent(src, rel, entity);
			mediator.fireDbRelationshipEvent(e);
		}
		else if (column == FOREIGN_KEY) {
			Boolean temp = (Boolean)aValue;
			rel.setToDependentPK(temp.booleanValue());
		}
		else if (column == CARDINALITY) {
			Boolean temp = (Boolean)aValue;
			rel.setToMany(temp.booleanValue());
			RelationshipEvent e = new RelationshipEvent(src, rel, entity);
			mediator.fireDbRelationshipEvent(e);
		}
        fireTableRowsUpdated(row, row);
    }// End setValueAt()

	
	/** Don't allow adding more than one new attributes. 
	 * @return true if new row was added, false if not. */
	public boolean addRow() {
		String name = NameGenerator.getDbRelationshipName();
		DbRelationship temp = new DbRelationship(name);
		temp.setSourceEntity(entity);
		relList.add(temp);
		entity.addRelationship(temp);
		RelationshipEvent e;
		e = new RelationshipEvent(src, temp, entity, RelationshipEvent.ADD);
		mediator.fireDbRelationshipEvent(e);
		fireTableDataChanged();
		return true;
	}

	public void removeRow(int row) {
		if (row < 0)
			return;
		System.out.println("DbRelationshipTableModel::removeRow()");
		Relationship rel = (Relationship)relList.get(row);
		RelationshipEvent e;
		e = new RelationshipEvent(src, rel, entity, RelationshipEvent.REMOVE);
		mediator.fireDbRelationshipEvent(e);
		relList.remove(row);
		String name = rel.getName();
		entity.removeRelationship(name);
		fireTableRowsDeleted(row, row);
	}	

	public boolean isCellEditable(int row, int col) {
		return true;
	}// End isCellEditable()
}// End DbRelationshipTableModel
