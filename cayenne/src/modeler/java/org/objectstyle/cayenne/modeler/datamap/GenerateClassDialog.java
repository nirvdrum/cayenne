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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.modeler.*;
import org.objectstyle.cayenne.modeler.event.Mediator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Dialog for generating Java classes from the DataMap.
 *  
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class GenerateClassDialog
	extends CayenneDialog
	implements ActionListener {
	Mediator mediator;

	File outputFolder = null;

	private JTable table;
	private JLabel generatePairLabel;
	private JCheckBox generatePair;
	private JLabel folderLabel;
	private JTextField folder;
	private JButton chooseFolder;
	private JButton generate;
	private JButton selectAll;
	private JButton deselectAll;
	private JButton cancel;

	private JFileChooser fileChooser = new JFileChooser();

	public GenerateClassDialog(Editor win, Mediator temp_mediator) {
		super(win, "Generate Java Classes", true);
		mediator = temp_mediator;

		init();
		setSize(500, 400);
		centerWindow();

		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

		chooseFolder.addActionListener(this);
		generate.addActionListener(this);
		cancel.addActionListener(this);
		selectAll.addActionListener(this);
		deselectAll.addActionListener(this);
	}

	private void init() {
		getContentPane().setLayout(new BorderLayout());

		table = new JTable();
		DataMap map = mediator.getCurrentDataMap();
		table.setModel(new GenerateClassTableModel(map));
		TableColumn col = table.getColumnModel().getColumn(0);
		col.setMinWidth(280);

		Box generate_pair_box = Box.createHorizontalBox();
		generatePairLabel = new JLabel("Generate parent/child class pairs");
		generatePair = new JCheckBox();
		generatePair.setSelected(true);
		generate_pair_box.add(Box.createHorizontalStrut(2));
		generate_pair_box.add(generatePairLabel);
		generate_pair_box.add(Box.createHorizontalStrut(7));
		generate_pair_box.add(generatePair);
		generate_pair_box.add(Box.createHorizontalStrut(2));

		Box folder_box = Box.createHorizontalBox();
		folderLabel = new JLabel("Output directory:");
		folder = new JTextField();

		ModelerPreferences pref = ModelerPreferences.getPreferences();
		String startDir =
			(String) pref.getProperty(ModelerPreferences.LAST_GENERATED_CLASSES_DIR);
		if (startDir != null) {
			outputFolder = new File(startDir);
			folder.setText(startDir);
		}
		folder.setEditable(false);
		chooseFolder = new JButton("Choose");
		folder_box.add(Box.createHorizontalStrut(2));
		folder_box.add(folderLabel);
		folder_box.add(Box.createHorizontalStrut(7));
		folder_box.add(folder);
		folder_box.add(Box.createHorizontalStrut(3));
		folder_box.add(chooseFolder);
		folder_box.add(Box.createHorizontalStrut(2));

		generate = new JButton("Generate");
		selectAll = new JButton("Select All");
		deselectAll = new JButton("Deselect All");
		cancel = new JButton("Cancel");

		JPanel panel =
			PanelFactory.createTablePanel(
				table,
				new JComponent[] { generate_pair_box, folder_box },
				new JButton[] { generate, selectAll, deselectAll, cancel });
		getContentPane().add(panel, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src == cancel) {
			hide();
		} else if (src == generate) {
			generateCode();
		} else if (src == chooseFolder) {
			ModelerPreferences pref = ModelerPreferences.getPreferences();
			String init_dir =
				(String) pref.getProperty(
					ModelerPreferences.LAST_GENERATED_CLASSES_DIR);
			if (null == init_dir)
				init_dir = (String) pref.getProperty(ModelerPreferences.LAST_DIR);
			if (null != init_dir) {
				File init_dir_file = new File(init_dir);
				if (init_dir_file.exists())
					fileChooser.setCurrentDirectory(init_dir_file);
			}
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int ret_code = fileChooser.showOpenDialog(this);
			if (ret_code == JFileChooser.APPROVE_OPTION) {
				outputFolder = fileChooser.getSelectedFile();
				folder.setText(outputFolder.getAbsolutePath());
				// Set preferences
				pref.setProperty(
					ModelerPreferences.LAST_GENERATED_CLASSES_DIR,
					outputFolder.getAbsolutePath());
			}
		} else if (src == selectAll) {
			GenerateClassTableModel model;
			model = (GenerateClassTableModel) table.getModel();
			model.selectAll(true);
		} else if (src == deselectAll) {
			GenerateClassTableModel model;
			model = (GenerateClassTableModel) table.getModel();
			model.selectAll(false);
		}
	}

	private void generateCode() {
		if (outputFolder == null) {
			JOptionPane.showMessageDialog(
				this,
				"Enter directory for source files");
			chooseFolder.requestFocus(true);
			return;
		}
		if (!outputFolder.exists()) {
			JOptionPane.showMessageDialog(
				this,
				"Directory " + outputFolder.getName() + " does not exist");
			chooseFolder.requestFocus(true);
			return;
		}
		if (!outputFolder.isDirectory()) {
			JOptionPane.showMessageDialog(
				this,
				"Choose directory rather than file");
			chooseFolder.requestFocus(true);
			return;
		}

		List selected =
			((GenerateClassTableModel) table.getModel()).getSelected();
		DefaultClassGenerator generator = new DefaultClassGenerator(selected);
		generator.setDestDir(outputFolder);
		generator.setMakePairs(generatePair.isSelected());

		try {
			generator.execute();
			JOptionPane.showMessageDialog(this, "Class generation finished");
			hide();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(
				this,
				"Error generating classes - " + e.getMessage());
		}
	}
}

class GenerateClassTableModel extends AbstractTableModel {
	DataMap map;

	ObjEntity[] entities;
	boolean[] selected;

	Boolean tempBoolean = new Boolean(false);
	String tempString = new String();

	public GenerateClassTableModel(DataMap temp_map) {
		map = temp_map;
		entities = temp_map.getObjEntities();
		selected = new boolean[entities.length];
		// Deselect entities without class names.
		for (int i = 0; i < selected.length; i++) {
			if (entities[i].getClassName() != null
				&& entities[i].getClassName().trim().length() > 0)
				selected[i] = true;
			else
				selected[i] = false;
		}
	}

	public Class getColumnClass(int col) {
		switch (col) {
			case 0 :
				return tempString.getClass();
			default :
				return tempBoolean.getClass();
		}
	}

	public int getRowCount() {
		return entities.length;
	}

	public int getColumnCount() {
		return 2;
	}

	public String getColumnName(int column) {
		if (column == 0)
			return "Entity";
		else
			return "";
	}

	public Object getValueAt(int row, int column) {
		if (column == 0) {
			return entities[row].getName();
		} else if (column == 1) {
			return new Boolean(selected[row]);
		} else
			return "";

	} // End getValueAt()

	public void selectAll(boolean sel_val) {
		for (int i = 0; i < selected.length; i++) {
			if (entities[i].getClassName() != null
				&& entities[i].getClassName().trim().length() > 0)
				selected[i] = sel_val;
			else
				selected[i] = false;
		}
		fireTableDataChanged();
	}

	public void setValueAt(Object aValue, int row, int column) {
		if (column == 1) {
			Boolean temp = (Boolean) aValue;
			selected[row] = temp.booleanValue();
		}
	} // End setValueAt()

	public boolean isCellEditable(int row, int col) {
		if (col == 1)
			if (entities[row].getClassName() != null
				&& entities[row].getClassName().trim().length() > 0)
				return true;
			else
				return false;
		else
			return false;
	} // End isCellEditable()

	/** 
	 * Returns a list containing selected ObjEntities.
	 */
	public List getSelected() {
		List selectedEnts = new ArrayList();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] == true) {
				selectedEnts.add(entities[i]);
			}
		}
		return selectedEnts;
	}
}
