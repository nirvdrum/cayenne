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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.event.*;
import org.objectstyle.cayenne.modeler.util.*;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Detail view of the ObjEntity attributes.
 *  
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class ObjAttributePane
	extends JPanel
	implements
		ObjEntityDisplayListener,
		ObjEntityListener,
		ObjAttributeListener,
		ExistingSelectionProcessor,
		ListSelectionListener {
	static Logger logObj = Logger.getLogger(ObjAttributePane.class.getName());

	Mediator mediator;
	CayenneTable table;

	public ObjAttributePane(Mediator mediator) {
		super();
		this.mediator = mediator;
		mediator.addObjEntityDisplayListener(this);
		mediator.addObjEntityListener(this);
		mediator.addObjAttributeListener(this);

		// Create and layout components
		init();
	}

	private void init() {
		setLayout(new BorderLayout());
		// Create table with two columns and no rows.
		table = new CayenneTable();
		add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
	}

	public void processExistingSelection() {
		ObjAttribute rel = null;
		if (table.getSelectedRow() >= 0) {
			ObjAttributeTableModel model =
				(ObjAttributeTableModel) table.getModel();
			rel = model.getAttribute(table.getSelectedRow());
		}
		AttributeDisplayEvent ev =
			new AttributeDisplayEvent(
				this,
				rel,
				mediator.getCurrentObjEntity(),
				mediator.getCurrentDataMap(),
				mediator.getCurrentDataDomain());

		mediator.fireObjAttributeDisplayEvent(ev);
	}

	public void valueChanged(ListSelectionEvent e) {
		processExistingSelection();
	}

	public void objAttributeChanged(AttributeEvent e) {
		table.select(e.getAttribute());
	}

	public void objAttributeAdded(AttributeEvent e) {
		rebuildTable((ObjEntity) e.getEntity());
		table.select(e.getAttribute());
	}

	public void objAttributeRemoved(AttributeEvent e) {
		ObjAttributeTableModel model =
			(ObjAttributeTableModel) table.getModel();
		int ind = model.getObjectList().indexOf(e.getAttribute());
		model.removeRow(e.getAttribute());
		table.select(ind);
	}

	private void stopEditing() {
		// Stop whatever editing may be taking place
		int col_index = table.getEditingColumn();
		if (col_index >= 0) {
			TableColumn col = table.getColumnModel().getColumn(col_index);
			col.getCellEditor().stopCellEditing();
		}
	}

	public void currentObjEntityChanged(EntityDisplayEvent e) {
		if (e.getSource() == this)
			return;

		ObjEntity entity = (ObjEntity) e.getEntity();
		if (entity != null && e.isEntityChanged()) {
			rebuildTable(entity);
		}

		// if an entity was selected on a tree, 
		// unselect currently selected row
		if (e.isUnselectAttributes()) {
			table.clearSelection();
		}
	}

	protected void rebuildTable(ObjEntity ent) {
		ObjAttributeTableModel model =
			new ObjAttributeTableModel(ent, mediator, this);
		table.setModel(model);
		table.setRowHeight(25);
		table.setRowMargin(3);
		setUpTableStructure(model, ent);
		table.getSelectionModel().addListSelectionListener(this);
	}

	protected void setUpTableStructure(
		ObjAttributeTableModel model,
		ObjEntity entity) {

		TableColumn col = table.getColumnModel().getColumn(model.OBJ_ATTRIBUTE);
		col.setMinWidth(150);
		col = table.getColumnModel().getColumn(model.OBJ_ATTRIBUTE_TYPE);
		col.setMinWidth(150);

		JComboBox combo = new JComboBox(ModelerUtil.getRegisteredTypeNames());
		combo.setEditable(true);
		col.setCellEditor(new DefaultCellEditor(combo));

		// If DbEntity is specified, display Database info as well.
		if (entity.getDbEntity() != null) {
			col = table.getColumnModel().getColumn(model.DB_ATTRIBUTE);
			col.setMinWidth(150);
			combo =
				new JComboBox(
					ModelerUtil.getDbAttributeNames(
						mediator,
						mediator.getCurrentObjEntity().getDbEntity()));
			combo.setEditable(false);
			col.setCellEditor(new DefaultCellEditor(combo));
			col = table.getColumnModel().getColumn(model.DB_ATTRIBUTE_TYPE);
			col.setMinWidth(120);
		}
	}

	/** If DbEntity changed, refresh table.*/
	public void objEntityChanged(EntityEvent e) {
		if (e.getSource() == this) {
			return;
		}

		ObjAttributeTableModel model =
			(ObjAttributeTableModel) table.getModel();
		if (model.getDbEntity() != ((ObjEntity) e.getEntity()).getDbEntity()) {
			model.resetDbEntity();
			setUpTableStructure(model, (ObjEntity) e.getEntity());
		}
	}

	public void objEntityAdded(EntityEvent e) {
	}

	public void objEntityRemoved(EntityEvent e) {
	}

}