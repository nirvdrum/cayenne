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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.modeler.*;
import org.objectstyle.cayenne.modeler.event.Mediator;
import org.objectstyle.cayenne.modeler.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.util.*;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.NamedObjectFactory;

/** 
 * Editor for DbRelationship and its DbAttributePair's.
 * Also allows specifying the reverse relationship. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ResolveDbRelationshipDialog
	extends CayenneDialog
	implements ActionListener {

	private DataMap map;
	private java.util.List originalList;
	private java.util.List dbRelList;
	private DbEntity start;
	private DbEntity end;
	private DbRelationship dbRel;
	private boolean isDbRelNew;
	private DbRelationship reverseDbRel;
	private boolean isReverseDbRelNew;

	JLabel reverseNameLabel = new JLabel("Reverse Relationship:");
	JLabel reverseCheckLabel = new JLabel("Create Reverse:");
	JTextField name = new JTextField(20);
	JTextField reverseName = new JTextField(20);
	JCheckBox hasReverseDbRel = new JCheckBox("", false);
	JTable table = new CayenneTable();
	JButton add = new JButton("Add");
	JButton remove = new JButton("Remove");
	JButton save = new JButton("Save");
	JButton cancel = new JButton("Cancel");

	private boolean cancelPressed;

	public ResolveDbRelationshipDialog(
		java.util.List relationships,
		DbEntity start,
		DbEntity end,
		boolean toMany) {

		super(Editor.getFrame(), "", true);

		this.map = getMediator().getCurrentDataMap();
		this.originalList = relationships;
		this.start = start;
		this.end = end;

		// If DbRelationship does not exist, create it.
		if (relationships == null || relationships.size() == 0) {
			dbRelList = new ArrayList();
			dbRel =
				(DbRelationship) NamedObjectFactory.createRelationship(
					start,
					end,
					toMany);
			dbRelList.add(dbRel);
			reverseDbRel = null;
			dbRel.setSourceEntity(start);
			dbRel.setTargetEntity(end);
			dbRel.setToMany(toMany);
			isReverseDbRelNew = true;
			isDbRelNew = true;
		} else {
			dbRelList = new ArrayList(relationships);
			dbRel = (DbRelationship) dbRelList.get(0);
			reverseDbRel = dbRel.getReverseRelationship();
			isReverseDbRelNew = (reverseDbRel == null);
			isDbRelNew = false;
		}

		init();

		this.pack();
		this.centerWindow();
	}

	/** Set up the graphical components. */
	private void init() {
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		if (!isReverseDbRelNew) {
			reverseCheckLabel.setText("Update Reverse:");
		}

		// If this is relationship of DbEntity to itself, disable 
		// reverse relationship check box
		if (start == end) {
			reverseName.setText("");
			reverseName.setEnabled(false);
			reverseNameLabel.setEnabled(false);
			hasReverseDbRel.setSelected(false);
			hasReverseDbRel.setEnabled(false);
			reverseCheckLabel.setEnabled(false);
		}
		// If reverse relationship doesn't exist, deselect checkbox 
		// and disable reverseName text field		
		else if (null == reverseDbRel) {
			reverseName.setText("");
			reverseName.setEnabled(false);
			reverseNameLabel.setEnabled(false);
			hasReverseDbRel.setSelected(false);
		} else {
			reverseNameLabel.setEnabled(true);
			reverseName.setEnabled(true);
			reverseName.setText(
				(reverseDbRel.getName() != null ? reverseDbRel.getName() : ""));
			hasReverseDbRel.setSelected(true);
		}
		hasReverseDbRel.addActionListener(this);

		name.setText((dbRel.getName() != null ? dbRel.getName() : ""));

		Component[] left =
			new Component[] {
				new JLabel("Relationship: "),
				reverseNameLabel,
				reverseCheckLabel };

		Component[] right = new Component[] { name, reverseName, hasReverseDbRel };

		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(PanelFactory.createForm(left, right, 5, 5, 5, 5));
		getContentPane().add(panel);

		// Attribute pane
		DbAttributePairTableModel model =
			new DbAttributePairTableModel(dbRel, getMediator(), this, true);
		table.setModel(model);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scroll_pane = new JScrollPane(table);
		scroll_pane.setPreferredSize(new Dimension(600, 100));
		getContentPane().add(scroll_pane, BorderLayout.CENTER);

		TableColumn col = table.getColumnModel().getColumn(0);
		col.setMinWidth(150);
		JComboBox comboBox =
			new JComboBox(ModelerUtil.getDbAttributeNames(getMediator(), start));
		comboBox.setEditable(false);
		col.setCellEditor(new DefaultCellEditor(comboBox));
		col = table.getColumnModel().getColumn(1);
		col.setMinWidth(150);
		col = table.getColumnModel().getColumn(2);
		col.setMinWidth(150);
		comboBox = new JComboBox(ModelerUtil.getDbAttributeNames(getMediator(), end));
		comboBox.setEditable(false);
		col.setCellEditor(new DefaultCellEditor(comboBox));
		col = table.getColumnModel().getColumn(3);
		col.setMinWidth(150);

		setTitle("DbRelationship Info: " + start.getName() + " to " + end.getName());

		JPanel buttons =
			PanelFactory.createButtonPanel(new JButton[] { add, remove, save, cancel });
		getContentPane().add(buttons, BorderLayout.SOUTH);

		add.addActionListener(this);
		remove.addActionListener(this);
		save.addActionListener(this);
		cancel.addActionListener(this);
	}

	public java.util.List getDbRelList() {
		return dbRelList;
	}

	public boolean isCancelPressed() {
		return cancelPressed;
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		DbAttributePairTableModel model = (DbAttributePairTableModel) table.getModel();

		if (src == add) {
			model.addRow();
		} else if (src == remove) {
			stopEditing();
			int row = table.getSelectedRow();
			if (row >= 0)
				model.removeRow(row);
		} else if (src == save) {
			cancelPressed = false;
			save();
		} else if (src == cancel) {
			dbRelList = originalList;
			cancelPressed = true;
			hide();
		} else if (src == hasReverseDbRel) {
			if (!hasReverseDbRel.isSelected()) {
				reverseName.setText("");
			}
			reverseName.setEnabled(hasReverseDbRel.isSelected());
			reverseNameLabel.setEnabled(hasReverseDbRel.isSelected());
		}
	}

	private void stopEditing() {
		// Stop whatever editing may be taking place
		int col_index = table.getEditingColumn();
		if (col_index >= 0) {
			TableColumn col = table.getColumnModel().getColumn(col_index);
			col.getCellEditor().stopCellEditing();
		}
	}

	private void save() {
		if (!name.getText().equals(dbRel.getName())) {
			String oldName = dbRel.getName();
			MapUtil.setRelationshipName(dbRel.getSourceEntity(), dbRel, name.getText());

			getMediator().fireDbRelationshipEvent(
				new RelationshipEvent(this, dbRel, dbRel.getSourceEntity(), oldName));
		}

		DbAttributePairTableModel model = (DbAttributePairTableModel) table.getModel();
		try {
			model.commit();
		} catch (DataMapException e) {
			e.printStackTrace();
			return;
		}

		// If new DbRelationship was created, add it to the source.
		if (isDbRelNew) {
			start.addRelationship(dbRel);
		}

		// check "to dep pk" setting,
		// maybe this is no longer valid
		if (dbRel.isToDependentPK() && !MapUtil.isValidForDepPk(dbRel)) {
			dbRel.setToDependentPK(false);
		}

		// If new reverse DbRelationship was created, add it to the target
		if (hasReverseDbRel.isSelected()) {
			if (reverseDbRel == null) {
				// Check if there is an existing relationship with the same joins
				reverseDbRel = dbRel.getReverseRelationship();
			}

			// If didn't find anything, create reverseDbRel
			if (reverseDbRel == null) {
				reverseDbRel = new DbRelationship();
				reverseDbRel.setSourceEntity(dbRel.getTargetEntity());
				reverseDbRel.setTargetEntity(dbRel.getSourceEntity());
				reverseDbRel.setToMany(!dbRel.isToMany());
			}

			java.util.List revJoins = getReverseJoins();
			reverseDbRel.setJoins(revJoins);

			// check if joins map to a primary key of this entity
			if (!dbRel.isToDependentPK()) {
				Iterator it = revJoins.iterator();
				if (it.hasNext()) {
					boolean toDepPK = true;
					while (it.hasNext()) {
						DbAttributePair join = (DbAttributePair) it.next();
						if (!join.getTarget().isPrimaryKey()) {
							toDepPK = false;
							break;
						}
					}

					reverseDbRel.setToDependentPK(toDepPK);
				}
			}

			reverseDbRel.setName(reverseName.getText());
			if (isReverseDbRelNew) {
				end.addRelationship(reverseDbRel);
			}
		}

		getMediator().fireDbRelationshipEvent(
			new RelationshipEvent(this, dbRel, dbRel.getSourceEntity()));
		hide();
	}

	private java.util.List getReverseJoins() {
		java.util.List rev_list = new ArrayList();
		java.util.List list =
			(dbRel.getJoins() != null ? dbRel.getJoins() : new ArrayList());
		Iterator iter = list.iterator();
		// Loop through the list of attribute pairs, create reverse pairs
		// and put them to the reverse list.
		while (iter.hasNext()) {
			DbAttributePair pair = (DbAttributePair) iter.next();
			DbAttributePair rev_pair;
			rev_pair = new DbAttributePair(pair.getTarget(), pair.getSource());
			rev_list.add(rev_pair);
		}
		return rev_list;
	}

}