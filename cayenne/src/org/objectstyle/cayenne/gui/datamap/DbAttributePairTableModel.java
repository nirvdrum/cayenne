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

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.dba.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;


/** Model for editing DbAttributePair-s. Changes in the join attributes
 *  don't take place until commit() is called. Creation of the new
 *  DbAttributes is not allowed - user should choose from the existing ones.
*/
public class DbAttributePairTableModel extends AbstractTableModel
{
	Mediator mediator;
	Object src;
	
	private DbRelationship rel;
	private DbEntity source;
	private DbEntity target;
	private java.util.List joins = new ArrayList();

	/** Is the table editable. */
	private boolean editable = false;
	
	// Columns
	static final int SOURCE = 0;
	static final int SOURCE_TYPE = 1;
	static final int TARGET = 2;
	static final int TARGET_TYPE = 3;
	
	public DbAttributePairTableModel(DbRelationship temp_rel
	, Mediator temp_mediator, Object temp_src)
	{
		mediator = temp_mediator;
		src = temp_src;
		rel = temp_rel;
		source = (DbEntity)rel.getSourceEntity();
		target = (DbEntity)rel.getTargetEntity();
		if (rel.getJoins() != null) {
			Iterator iter = rel.getJoins().iterator();
			while(iter.hasNext()) {
				DbAttributePair pair = (DbAttributePair)iter.next();
				joins.add(pair);
			}
		}
	}

	public DbAttributePairTableModel(DbRelationship temp_rel
	,Mediator temp_mediator, Object temp_src, boolean temp_editable)
	{
		this(temp_rel, temp_mediator, temp_src);
		editable = temp_editable;
	}	

	/** Mode new attribute pairs from list to the DbRelationship. */
	public void commit() throws DataMapException {
		rel.setJoins(joins);
	}	
	
	public int getRowCount() {
		return joins.size();
	}
	
	public int getColumnCount()
	{
		return 4;
	}
	
	public String getColumnName(int column) {
		if (column == SOURCE)
			return "Source";
		else if (column == SOURCE_TYPE)
			return "Type";
		else if (column == TARGET)
			return "Target";
		else if (column == TARGET_TYPE)
			return "Type";
		else return "";
	}
	
	public Object getValueAt(int row, int column)
	{
		DbAttributePair pair = (DbAttributePair)joins.get(row);
		DbAttribute source_attr = pair.getSource();
		DbAttribute target_attr = pair.getTarget();

		if (column == SOURCE) {
			if (null == source_attr)
				return null;
			return source_attr.getName();
		}
		else if (column == SOURCE_TYPE) {
			if (null == source_attr)
				return null;
			return TypesMapping.getSqlNameByType(source_attr.getType());
		}
		else if (column == TARGET) {
			if (null == target_attr)
				return null;
			return target_attr.getName();
		}
		else if (column == TARGET_TYPE) {
			if (null == target_attr)
				return null;
			return TypesMapping.getSqlNameByType(target_attr.getType());
		}
		else return null;

	}// End getValueAt()
	
    
    public void setValueAt(Object aValue, int row, int column) {
		DbAttributePair pair = (DbAttributePair)joins.get(row);
		String value = (String)aValue;

		if (column == SOURCE) {
			if (null == source)
				return;
			DbAttribute attrib = (DbAttribute)source.getAttribute(value);
			if (null == attrib) {				
				return;
			}
			pair.setSource(attrib);
		}
		else if (column == TARGET) {
			if (null == target)
				return;
			DbAttribute attrib = (DbAttribute)target.getAttribute(value);
			if (null == attrib) {
				return;
			}
			pair.setTarget(attrib);
		}
		fireTableRowsUpdated(row, row);
    }// End setValueAt()

	
	/** Don't allow adding more than one new attributes. 
	 * @return true if new row was added, false if not. */
	public boolean addRow() {		
		DbAttributePair pair = new DbAttributePair(null, null);
		joins.add(pair);
		fireTableDataChanged();
		return true;
	}

	public void removeRow(int row) {
		if (row < 0)
			return;
		joins.remove(row);
		fireTableDataChanged();
	}	

	public boolean isCellEditable(int row, int col) {
		if (col == SOURCE) {
			if (rel.getSourceEntity() == null)
				return false;
			return (editable && true);		
		}
		else if (col == TARGET) {
			if (rel.getTargetEntity() == null)
				return false;
			return (editable && true);
		}
		return false;
	}// End isCellEditable()
}// End DbAttributePairTableModel

