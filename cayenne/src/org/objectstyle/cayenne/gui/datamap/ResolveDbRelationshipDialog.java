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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Edit DbRelationship and DbAttributePair-s for this DbRelationship.
 *  Also allows specifying the reverse relationship. */
public class ResolveDbRelationshipDialog extends JDialog
implements ActionListener
{
	Mediator mediator;
	
	private DataMap map;
	private List originalList;
	private List dbRelList;
	private DbEntity start;
	private DbEntity end;
	private DbRelationship dbRel;
	private boolean isDbRelNew = false;
	private DbRelationship reverseDbRel;
	private boolean isReverseDbRelNew = false;
	
	JTextField name				= new JTextField(20);
	JLabel reverseNameLabel 	= new JLabel("Reverse Relationship Name:");
	JTextField reverseName 		= new JTextField(20);
	JCheckBox  hasReverseDbRel 	= new JCheckBox("Create reverse relationship", false);
	JTable table		= new CayenneTable();
	JButton add			= new JButton("Add");
	JButton remove		= new JButton("Remove");
	JButton save		= new JButton("Save");
	JButton cancel		= new JButton("Cancel");
	//JButton reset		= new JButton("New");
	private boolean cancelPressed = false;

	public ResolveDbRelationshipDialog(Mediator temp_mediator, List db_rel_list
	, DbEntity temp_start, DbEntity temp_end, boolean to_many)
	{		
		super(Editor.getFrame(), "", true);
		mediator = temp_mediator;
		map = temp_mediator.getCurrentDataMap();
		originalList = db_rel_list;
		start = temp_start;
		end = temp_end;

		// If DbRelationship does not exist, create it.
		if (null == db_rel_list || db_rel_list.size() <= 0)  {
			dbRelList = new ArrayList();
			dbRel = new DbRelationship();
			dbRelList.add(dbRel);
			reverseDbRel = null;
			dbRel.setSourceEntity(start);
			dbRel.setTargetEntity(end);
			dbRel.setName(NameGenerator.getDbRelationshipName(temp_end, to_many));
			dbRel.setToMany(to_many);
			isReverseDbRelNew = true;
			isDbRelNew = true;
		}
		else {
			dbRelList = new ArrayList(db_rel_list);
			dbRel = (DbRelationship)dbRelList.get(0);
			reverseDbRel = dbRel.getReverseRelationship();
			if (null != reverseDbRel)
				isReverseDbRelNew = false;
			else 
				isReverseDbRelNew = true;
			isDbRelNew = false;
		}
		
		init();
		setSize(400, 300);
		Point point = Editor.getFrame().getLocationOnScreen();
		this.setLocation(point);
	}// End ResolveDbRelationshipDialog
	
	/** Set up the graphical components. */
	private void init() {
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
		// Name text field
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
		JLabel label = new JLabel("Name: ");
		temp.add(label);
		temp.add(Box.createHorizontalStrut(5));
		temp.add(name);
		temp.add(Box.createHorizontalStrut(12));
		temp.add(hasReverseDbRel);
		temp.add(Box.createHorizontalGlue());
		getContentPane().add(temp);		
		name.setText( (dbRel.getName() != null ? dbRel.getName() : "") );
		
		temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
		reverseNameLabel.setLabelFor(reverseName);
		temp.add(reverseNameLabel);
		temp.add(Box.createHorizontalStrut(5));
		temp.add(reverseName);
		temp.add(Box.createHorizontalGlue());
		getContentPane().add(temp);

		// If this is relationship of DbEntity to itself, disable 
		// reverse relationship check box
		if (start == end) {
			reverseName.setText("");
			reverseName.setEnabled(false);
			reverseNameLabel.setEnabled(false);
			hasReverseDbRel.setSelected(false);
			hasReverseDbRel.setEnabled(false);
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
			reverseName.setText( (reverseDbRel.getName() != null 
								? reverseDbRel.getName() 
								: "") );
			hasReverseDbRel.setSelected(true);
		}
		hasReverseDbRel.addActionListener(this);
		
		// Attribute pane
		DbAttributePairTableModel model;
		model = new DbAttributePairTableModel(dbRel, mediator, this, true);
		table.setModel(model);
		table.getSelectionModel().setSelectionMode(
										ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		
		JScrollPane scroll_pane = new JScrollPane(table);
		getContentPane().add(scroll_pane, BorderLayout.CENTER);

		TableColumn col = table.getColumnModel().getColumn(0);
		col.setMinWidth(150);
		JComboBox comboBox = new JComboBox(Util.getDbAttributeNames(mediator, start));
		comboBox.setEditable(false);
		col.setCellEditor(new DefaultCellEditor(comboBox));
		col = table.getColumnModel().getColumn(1);
		col.setMinWidth(150);
		col = table.getColumnModel().getColumn(2);
		col.setMinWidth(150);
		comboBox = new JComboBox(Util.getDbAttributeNames(mediator, end));
		comboBox.setEditable(false);
		col.setCellEditor(new DefaultCellEditor(comboBox));
		col = table.getColumnModel().getColumn(3);
		col.setMinWidth(150);

		temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
		temp.add(add);
		temp.add(Box.createRigidArea(new Dimension(6, 0)));
		temp.add(Box.createHorizontalGlue());
		temp.add(remove);
		temp.add(Box.createRigidArea(new Dimension(6, 0)));
		temp.add(Box.createHorizontalGlue());
		// Make label for save button Create or Update
		if (isDbRelNew) {
//			save.setText("Create");
			setTitle("Create relationship between table " 
				+ start.getName() + " and " + end.getName());
		}
		else {
			setTitle("Change relationship between table " 
				+ start.getName() + " and " + end.getName());
//			save.setText("Update");
		}
		temp.add(save);		
		temp.add(Box.createRigidArea(new Dimension(6, 0)));
		temp.add(Box.createHorizontalGlue());
		temp.add(cancel);
//		temp.add(Box.createRigidArea(new Dimension(6, 0)));
//		temp.add(Box.createHorizontalGlue());
//		temp.add(reset);
		temp.add(Box.createHorizontalGlue());
		getContentPane().add(temp, BorderLayout.SOUTH);
				
		add.addActionListener(this);
		remove.addActionListener(this);
		save.addActionListener(this);
		cancel.addActionListener(this);
//		reset.addActionListener(this);
	}// End init()
	
	
	public java.util.List getDbRelList() {
		return dbRelList;
	}


	public boolean isCancelPressed() {
		return cancelPressed;
	}
		
			
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		DbAttributePairTableModel model;
		model = (DbAttributePairTableModel)table.getModel();
		
		if (src == add){
			model.addRow();
		} 
		else if (src == remove) {
			stopEditing();
			int row = table.getSelectedRow();
			if (row >= 0) 
				model.removeRow(row);
		}
		else if (src == save) {
			cancelPressed = false;
			save();
		}
		else if (src == cancel) {
			dbRelList = originalList;
			cancelPressed = true;
			hide();
		}
		else if (src == hasReverseDbRel) {
			if (!hasReverseDbRel.isSelected())
				reverseName.setText("");
			reverseName.setEnabled(hasReverseDbRel.isSelected());
			reverseNameLabel.setEnabled(hasReverseDbRel.isSelected());
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
	


	private void save() {
		if (dbRel.getName() == null) {
			JOptionPane.showMessageDialog(Editor.getFrame()
										, "Enter Relationship Name");
			name.requestFocus(true);
			return;
		}
		DbAttributePairTableModel model;
		model = (DbAttributePairTableModel)table.getModel();
		if (model.getRowCount() == 0) {
			JOptionPane.showMessageDialog(Editor.getFrame()
										, "Enter join attributes ");
			table.requestFocus(true);		
			return;
		}
		
		try {
			model.commit();
		}
		catch (DataMapException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return;
		}
		// If new DbRelationship was created, add it to the source.
		if (isDbRelNew)
			start.addRelationship(dbRel);
			
		// If new reverse DbRelationship was created, add it to the target
		if (hasReverseDbRel.isSelected()) {
			if (reverseDbRel == null) {
				// Check if there is already anything from target to source
				Iterator iter;
				iter = dbRel.getTargetEntity().getRelationshipList().iterator();
				while(iter.hasNext()) {
					DbRelationship temp = (DbRelationship)iter.next();
					// If found candidate for reverse db relationship, break
					if (temp.getTargetEntity() == start && temp != dbRel) {
						reverseDbRel = temp;
						break;
					}
						
				}// End while()
			}// End if reverseDbRel is null
			
			// If didn't find anything, create reverseDbRel
			if (reverseDbRel == null) {
				reverseDbRel = new DbRelationship();
				reverseDbRel.setSourceEntity(dbRel.getTargetEntity());
				reverseDbRel.setTargetEntity(dbRel.getSourceEntity());
			}
			reverseDbRel.setJoins(getReverseJoins());
			reverseDbRel.setName(reverseName.getText());
			if (isReverseDbRelNew) {
				end.addRelationship(reverseDbRel);
			}
		}
		// Unblock the dialog
		hide();
	}

	private java.util.List getReverseJoins() {
		java.util.List rev_list = new ArrayList();
		java.util.List list = (dbRel.getJoins() != null
							?  dbRel.getJoins()
							:  new ArrayList());
		Iterator iter = list.iterator();
		// Loop through the list of attribute pairs, create reverse pairs
		// and put them to the reverse list.
		while(iter.hasNext()) {
			DbAttributePair pair = (DbAttributePair)iter.next();
			DbAttributePair rev_pair;
			rev_pair = new DbAttributePair(pair.getTarget(), pair.getSource());
			rev_list.add(rev_pair);
		}
		return rev_list;
	}
	
	
	
}