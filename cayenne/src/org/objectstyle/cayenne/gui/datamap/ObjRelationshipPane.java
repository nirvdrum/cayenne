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


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.CayenneTable;
import org.objectstyle.cayenne.map.*;

/** Displays ObjRelationship-s for the current obj entity. 
  * @author Michael Misha Shengaout*/
public class ObjRelationshipPane extends JPanel
implements ActionListener, ObjEntityDisplayListener
, ObjEntityListener, ObjRelationshipListener
, ExistingSelectionProcessor, ListSelectionListener
, TableModelListener
{
	Mediator mediator;

	JTable 		table;
	JButton 	add;
	JButton 	resolve;
	
	public ObjRelationshipPane(Mediator temp_mediator) {
		super();
		mediator = temp_mediator;		
		mediator.addObjEntityDisplayListener(this);
		mediator.addObjEntityListener(this);
		mediator.addObjRelationshipListener(this);
		// Set up graphical components
		init();
		// Add listeners		
		add.addActionListener(this);
		resolve.addActionListener(this);
	}

	private void init()
	{
		this.setLayout(new BorderLayout());
		table = new CayenneTable();
		add = new JButton("Add");
		resolve	= new JButton("Database Mapping");
		JPanel panel = PanelFactory.createTablePanel(table
							, new JButton[]{add, resolve});
		add(panel, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		ObjRelationshipTableModel model;
		model = (ObjRelationshipTableModel)table.getModel();
		if (src == add) {
			model.addRow();
		} else if (src == resolve) {
			resolveRelationship();
		}
	}

	public void tableChanged(TableModelEvent e) {
		ObjRelationship rel = null;
		if (table.getSelectedRow() >= 0) {
			ObjRelationshipTableModel model;
			model = (ObjRelationshipTableModel)table.getModel();
			rel = model.getRelationship(table.getSelectedRow());
			if (rel.getTargetEntity() != null 
				&& ((ObjEntity)rel.getSourceEntity()).getDbEntity() != null
				&& ((ObjEntity)rel.getTargetEntity()).getDbEntity() != null)
			{
				resolve.setEnabled(true);
			}
			else
				resolve.setEnabled(false);
		}
	}

	public void processExistingSelection()
	{
		ObjRelationship rel = null;
		if (table.getSelectedRow() >= 0) {
			ObjRelationshipTableModel model;
			model = (ObjRelationshipTableModel)table.getModel();
			rel = model.getRelationship(table.getSelectedRow());
			if (rel.getTargetEntity() != null 
				&& ((ObjEntity)rel.getSourceEntity()).getDbEntity() != null
				&& ((ObjEntity)rel.getTargetEntity()).getDbEntity() != null)
			{
				resolve.setEnabled(true);
			}
			else
				resolve.setEnabled(false);
		} else
			resolve.setEnabled(false);
		RelationshipDisplayEvent ev;
		ev = new RelationshipDisplayEvent(this, rel
				, mediator.getCurrentObjEntity(), mediator.getCurrentDataMap()
				, mediator.getCurrentDataDomain());
		mediator.fireObjRelationshipDisplayEvent(ev);
	}

	public void valueChanged(ListSelectionEvent e) {
		processExistingSelection();
	}



	private void stopEditing() {
		// Stop whatever editing may be taking place
		int col_index = table.getEditingColumn();
		if (col_index >=0) {
			TableColumn col = table.getColumnModel().getColumn(col_index);
			col.getCellEditor().stopCellEditing();
		}
	}
	


	private void resolveRelationship() {
		int row = table.getSelectedRow();
		if (-1 == row)
			return;
		ObjRelationshipTableModel model;
		model = (ObjRelationshipTableModel)table.getModel();
		ObjRelationship rel = model.getRelationship(row);
		DbEntity start 	= ((ObjEntity)rel.getSourceEntity()).getDbEntity();
		DbEntity end 	= ((ObjEntity)rel.getTargetEntity()).getDbEntity();
		java.util.List db_rel_list = rel.getDbRelationshipList();
		DataMap map = mediator.getCurrentDataMap();

		// Choose the relationship to resolve this obj relationship
		ChooseDbRelationshipDialog dg;
		dg = new ChooseDbRelationshipDialog(mediator.getCurrentDataMap()
									, db_rel_list, start, end, rel.isToMany());
		dg.setVisible(true);
		if (ChooseDbRelationshipDialog.CANCEL == dg.getChoice())
			return;
		else if (ChooseDbRelationshipDialog.SELECT == dg.getChoice()) {
			copyDbRelationship(rel, dg.getDbRelationshipList());
			dg.dispose();
			return;
		}

		// If chose to create new db relationship or edit existing
		// display dialog for editing joins.
		if (ChooseDbRelationshipDialog.EDIT == dg.getChoice())
			db_rel_list = dg.getDbRelationshipList();
		else if (ChooseDbRelationshipDialog.NEW == dg.getChoice())
			db_rel_list = new ArrayList();
		ResolveDbRelationshipDialog dialog;
		dialog = new ResolveDbRelationshipDialog(mediator, db_rel_list
											, start, end, rel.isToMany());
		dialog.setVisible(true);
		// If user pressed "Save"
		if (!dialog.isCancelPressed())
			copyDbRelationship(rel, dialog.getDbRelList());
		dialog.dispose();
	}
	
	/** Set obj relationship to db relationships resolution.
	  * Clear old db relationships and put new ones in their place.*/
	private void copyDbRelationship (ObjRelationship rel, java.util.List list) {
		rel.removeAllDbRelationships();
		if (list == null) {
			return;
		}
		
		// Add DbRelationship to the ObjRedlationship list.
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			DbRelationship db_rel = (DbRelationship)iter.next();
			rel.addDbRelationship(db_rel);
		}
		
		mediator.fireObjRelationshipEvent(new RelationshipEvent(Editor.getFrame(), rel, rel.getSourceEntity()));
	}

	

	/** Loads obj relationships into table. */
	public void currentObjEntityChanged(EntityDisplayEvent e) {
		if (e.getSource() == this)
			return;
		ObjEntity entity = (ObjEntity)e.getEntity();
		if (null == entity || e.isEntityChanged() == false)
			return;
		ObjRelationshipTableModel model;
		model = new ObjRelationshipTableModel(entity,mediator, this);
		model.addTableModelListener(this);
		table.setModel(model);
		table.setRowHeight(25);
		table.setRowMargin(3);
		TableColumn col = table.getColumnModel().getColumn(model.REL_NAME);
		col.setMinWidth(150);
		col = table.getColumnModel().getColumn(model.REL_TARGET);
		col.setMinWidth(150);
		JComboBox combo = new JComboBox(createComboModel());
		combo.setEditable(false);
		combo.setSelectedIndex(-1);
		DefaultCellEditor editor = new DefaultCellEditor(combo);
		editor.setClickCountToStart(1);
		col.setCellEditor(editor);
		col = table.getColumnModel().getColumn(model.REL_CARDINALITY);
		col.setMinWidth(150);
		table.getSelectionModel().addListSelectionListener(this);
	}
	
	/** Create DefaultComboBoxModel with all obj entity names. */
	private DefaultComboBoxModel createComboModel() {
		DataMap map = mediator.getCurrentDataMap();
		Vector elements = new Vector();
		java.util.List obj_entities = map.getObjEntitiesAsList();
		Iterator iter = obj_entities.iterator();
		while(iter.hasNext()){
			ObjEntity entity  = (ObjEntity)iter.next();
			String name = entity.getName();
			elements.add(name);
		}// End while()

		DefaultComboBoxModel model = new DefaultComboBoxModel(elements);
		return model;
	}

	public void objEntityChanged(EntityEvent e){}
	public void objEntityAdded(EntityEvent e)
	{ reloadEntityList(e); }
	public void objEntityRemoved(EntityEvent e)
	{ reloadEntityList(e); }

	public void objRelationshipChanged(RelationshipEvent e){}
	public void objRelationshipAdded(RelationshipEvent e){}
	public void objRelationshipRemoved(RelationshipEvent e){
		ObjRelationshipTableModel model;
		model = (ObjRelationshipTableModel)table.getModel();
		model.removeRelationship(e.getRelationship());
	}



	/** Refresh the list of obj entities (targets). 
	  * Also refresh the table in case some obj relationships were deleted.*/
	private void reloadEntityList(EntityEvent e) {
		if (e.getSource() == this)
			return;
		// If current model added/removed, do nothing.
		if (mediator.getCurrentObjEntity() == e.getEntity())
			return;
		// If this is just loading new currentObjEntity, do nothing
		if (mediator.getCurrentObjEntity() == null)
			return;
		TableColumn col;
		col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET);
		DefaultCellEditor editor = (DefaultCellEditor)col.getCellEditor();
		JComboBox combo = (JComboBox)editor.getComponent();
		combo.setModel(createComboModel());
		ObjRelationshipTableModel model;
		model = (ObjRelationshipTableModel)table.getModel();
		model.fireTableDataChanged();
	}	
}