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

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Detail view of the ObjEntity attributes. 
 * @author Michael Misha Shengaout */
public class ObjAttributePane extends JPanel
implements ActionListener, ObjEntityDisplayListener, ObjEntityListener
{
	Mediator mediator;

	JTable		table;
	JButton		add;
	JButton		remove;
	
	public ObjAttributePane(Mediator temp_mediator)
	{
		super();
		mediator = temp_mediator;		
		mediator.addObjEntityDisplayListener(this);
		mediator.addObjEntityListener(this);
		// Create and laout components
		init();		
		// Add listeners
		add.addActionListener(this);
		remove.addActionListener(this);
	}
	
	private void init()
	{
		setLayout(new BorderLayout());
		// Create table with two columns and no rows.
		table = new CayenneTable();
		add		= new JButton("Add");
		remove 	= new JButton("Remove");
		JPanel panel = PanelFactory.createTablePanel(table
												, new JButton[]{add, remove});
		add(panel, BorderLayout.CENTER);
	}
	
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		ObjAttributeTableModel model = (ObjAttributeTableModel)table.getModel();
		if (src == add) {
			model.addRow();
		} else if (src == remove) {
			stopEditing();
			// Remove the row.
			int row = table.getSelectedRow();
			if (row >= 0)
				model.removeRow(row);
		}
	}

	private void stopEditing() {
		// Stop whatever editing may be taking place
		int col_index = table.getEditingColumn();
		if (col_index >=0) {
			TableColumn col = table.getColumnModel().getColumn(col_index);
			col.getCellEditor().stopCellEditing();
		}
	}
	

	
	public void currentObjEntityChanged(EntityDisplayEvent e) {
		if (e.getSource() == this)
			return;
		ObjEntity entity = (ObjEntity)e.getEntity();
		if (null == entity)
			return;
		ObjAttributeTableModel model;
		// Show db attribute columns only if db entity is not null
		boolean show_db_cols = entity.getDbEntity() != null;
		model = new ObjAttributeTableModel(entity, mediator, this);
		table.setModel(model);
		table.setRowHeight(25);
		table.setRowMargin(3);
		setUpTableStructure(model, entity);
	}
	
	private void setUpTableStructure(ObjAttributeTableModel model
	, ObjEntity entity) {
		TableColumn col;
		col = table.getColumnModel().getColumn(model.OBJ_ATTRIBUTE);
		col.setMinWidth(150);
		col = table.getColumnModel().getColumn(model.OBJ_ATTRIBUTE_TYPE);
		col.setMinWidth(150);
		JComboBox combo = new JComboBox(Util.getRegisteredTypeNames());
		combo.setEditable(true);
		col.setCellEditor(new DefaultCellEditor(combo));
		// If DbEntity is specified, display Database info as well.
		if (entity.getDbEntity() != null) {
			col = table.getColumnModel().getColumn(model.DB_ATTRIBUTE);
			col.setMinWidth(150);
			combo = new JComboBox(Util.getDbAttributeNames(mediator
							, mediator.getCurrentObjEntity().getDbEntity() ));
			combo.setEditable(false);
			col.setCellEditor(new DefaultCellEditor(combo));
			col = table.getColumnModel().getColumn(model.DB_ATTRIBUTE_TYPE);
			col.setMinWidth(120);
		}
	}
	
	/** If DbEntity changed, refresh table.*/
	public void objEntityChanged(EntityEvent e) {
		if (e.getSource() == this)
			return;
		ObjAttributeTableModel model;
		model = (ObjAttributeTableModel)table.getModel();
		if (model.getDbEntity() != ((ObjEntity)e.getEntity()).getDbEntity()) {
			model.resetDbEntity();
			setUpTableStructure(model, (ObjEntity)e.getEntity());
		}
	}
	public void objEntityAdded(EntityEvent e){}
	public void objEntityRemoved(EntityEvent e){}
	
	
}