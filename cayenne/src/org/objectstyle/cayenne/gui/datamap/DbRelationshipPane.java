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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.gui.CayenneActionPanel;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.CayenneTable;
import org.objectstyle.cayenne.map.*;

/** 
 * Displays DbRelationship's for the current DbEntity. 
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbRelationshipPane
	extends CayenneActionPanel
	implements
		DbEntityDisplayListener,
		DbEntityListener,
		DbRelationshipListener,
		ExistingSelectionProcessor,
		ListSelectionListener,
		TableModelListener {
	static Logger logObj = Logger.getLogger(DbRelationshipPane.class.getName());

	Mediator mediator;

	CayenneTable table;
	JButton resolve;

	public DbRelationshipPane(Mediator temp_mediator) {
		super();
		mediator = temp_mediator;
		mediator.addDbEntityDisplayListener(this);
		mediator.addDbEntityListener(this);
		mediator.addDbRelationshipListener(this);
		// Set up graphical components
		init();
		// Add listeners		
		resolve.addActionListener(this);
	}

	private void init() {
		setLayout(new BorderLayout());
		// Create table
		table = new CayenneTable();
		resolve = new JButton("Database Mapping");
		JPanel panel =
			PanelFactory.createTablePanel(table, new JButton[] { resolve });
		add(panel, BorderLayout.CENTER);
	}

	public void performAction(ActionEvent e) {
		Object src = e.getSource();
		if (src == resolve) {
			resolveRelationship();
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		processExistingSelection();
	}

	public void tableChanged(TableModelEvent e) {
		DbRelationship rel = null;
		if (table.getSelectedRow() >= 0) {
			DbRelationshipTableModel model =
				(DbRelationshipTableModel) table.getModel();
			rel = model.getRelationship(table.getSelectedRow());
			if (rel.getTargetEntity() != null)
				resolve.setEnabled(true);
			else
				resolve.setEnabled(false);
		}
	}

	public void processExistingSelection() {
		DbRelationship rel = null;
		if (table.getSelectedRow() >= 0) {
			DbRelationshipTableModel model;
			model = (DbRelationshipTableModel) table.getModel();
			rel = model.getRelationship(table.getSelectedRow());
			if (rel.getTargetEntity() != null)
				resolve.setEnabled(true);
			else
				resolve.setEnabled(false);
		} else
			resolve.setEnabled(false);

		RelationshipDisplayEvent ev =
			new RelationshipDisplayEvent(
				this,
				rel,
				mediator.getCurrentDbEntity(),
				mediator.getCurrentDataMap(),
				mediator.getCurrentDataDomain());

		mediator.fireDbRelationshipDisplayEvent(ev);
	}

	private void stopEditing() {
		// Stop whatever editing may be taking place
		int col_index = table.getEditingColumn();
		if (col_index >= 0) {
			TableColumn col = table.getColumnModel().getColumn(col_index);
			col.getCellEditor().stopCellEditing();
		}
	}

	private void resolveRelationship() {
		int row = table.getSelectedRow();
		if (-1 == row)
			return;
		// Get DbRelationship
		DbRelationshipTableModel model =
			(DbRelationshipTableModel) table.getModel();
		DbRelationship rel = model.getRelationship(row);
		DbEntity start = (DbEntity) rel.getSourceEntity();
		DbEntity end = (DbEntity) rel.getTargetEntity();

		java.util.List db_rel_list = new ArrayList();
		db_rel_list.add(rel);

		DataMap map = mediator.getCurrentDataMap();
		ResolveDbRelationshipDialog dialog =
			new ResolveDbRelationshipDialog(
				db_rel_list,
				start,
				end,
				rel.isToMany());
		dialog.setVisible(true);
		dialog.dispose();
	}

	/** Loads obj relationships into table. */
	public void currentDbEntityChanged(EntityDisplayEvent e) {
		DbEntity entity = (DbEntity) e.getEntity();
		if (entity != null && e.isEntityChanged()) {
			rebuildTable(entity);
		}

		// if an entity was selected on a tree, 
		// unselect currently selected row
		if (e.isUnselectAttributes()) {
			table.clearSelection();
		}
	}

	protected void rebuildTable(DbEntity dbEnt) {
		DbRelationshipTableModel model =
			new DbRelationshipTableModel(dbEnt, mediator, this);
		model.addTableModelListener(this);
		table.setModel(model);
		table.setRowHeight(25);
		table.setRowMargin(3);
		TableColumn col = table.getColumnModel().getColumn(model.NAME);
		col.setMinWidth(150);
		col = table.getColumnModel().getColumn(model.TARGET);
		col.setMinWidth(150);
		JComboBox combo = new JComboBox(createComboModel());
		combo.setEditable(false);
		col.setCellEditor(new DefaultCellEditor(combo));
		table.getSelectionModel().addListSelectionListener(this);
	}

	/** Create DefaultComboBoxModel with all obj entity names. */
	private DefaultComboBoxModel createComboModel() {
		DataMap map = mediator.getCurrentDataMap();
		Vector elements = new Vector();
		java.util.List db_entities = map.getDbEntitiesAsList(true);
		Iterator iter = db_entities.iterator();
		while (iter.hasNext()) {
			DbEntity entity = (DbEntity) iter.next();
			String name = entity.getName();
			elements.add(name);
		}

		DefaultComboBoxModel model = new DefaultComboBoxModel(elements);
		return model;
	}

	public void dbEntityChanged(EntityEvent e) {
	}

	public void dbEntityAdded(EntityEvent e) {
		reloadEntityList(e);
	}
	public void dbEntityRemoved(EntityEvent e) {
		reloadEntityList(e);
	}

	public void dbRelationshipChanged(RelationshipEvent e) {
		if (e.getSource() != this) {
			table.select(e.getRelationship());
			DbRelationshipTableModel model =
				(DbRelationshipTableModel) table.getModel();
			model.fireTableDataChanged();
		}
	}

	public void dbRelationshipAdded(RelationshipEvent e) {
		rebuildTable((DbEntity) e.getEntity());
		table.select(e.getRelationship());
	}

	public void dbRelationshipRemoved(RelationshipEvent e) {
		DbRelationshipTableModel model =
			(DbRelationshipTableModel) table.getModel();
		int ind = model.getObjectList().indexOf(e.getRelationship());
		model.removeRelationship(e.getRelationship());
		table.select(ind);
	}

	/** Refresh the list of db entities (targets). 
	  * Also refresh the table in case some db relationships were deleted.*/
	private void reloadEntityList(EntityEvent e) {
		if (e.getSource() == this)
			return;
		// If current model added/removed, do nothing.
		if (mediator.getCurrentDbEntity() == e.getEntity())
			return;
		// If this is just loading new currentDbEntity, do nothing
		if (mediator.getCurrentDbEntity() == null)
			return;
		TableColumn col =
			table.getColumnModel().getColumn(DbRelationshipTableModel.TARGET);
		DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();
		JComboBox combo = (JComboBox) editor.getComponent();
		combo.setModel(createComboModel());
		DbRelationshipTableModel model;
		model = (DbRelationshipTableModel) table.getModel();
		model.fireTableDataChanged();
		table.getSelectionModel().addListSelectionListener(this);
	}
}