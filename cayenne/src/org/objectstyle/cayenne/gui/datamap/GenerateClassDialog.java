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
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import org.objectstyle.util.Preferences;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.PanelFactory;


/** Wizard for generating the classes from the data map. */
public class GenerateClassDialog extends JDialog
implements ActionListener
{
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
	
	
	public GenerateClassDialog(JFrame win, Mediator temp_mediator)
	{
		super(win, "Generate classes", true);
		mediator = temp_mediator;

		init();
		setSize(400, 400);
		Point point = win.getLocationOnScreen();
		setLocation(point);
		
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
		
		JPanel panel = PanelFactory.createTablePanel(table
											, new JComponent[]{generate_pair_box
															, folder_box}
											, new JButton[]{generate
															, selectAll
															, deselectAll
															, cancel});
		getContentPane().add(panel, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		
		if (src == cancel) {
			hide();
		} else if (src == generate) {
			generateCode();
		} else if (src == chooseFolder) {
	    	Preferences pref = Preferences.getPreferences();
    	   	String init_dir = (String)pref.getProperty(Preferences.LAST_GENERATED_CLASSES_DIR);
    	   	if (null == init_dir)
    	   		init_dir = (String)pref.getProperty(Preferences.LAST_DIR);
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
				pref.setProperty(Preferences.LAST_GENERATED_CLASSES_DIR, outputFolder.getAbsolutePath());
			}
		} else if (src == selectAll) {
			GenerateClassTableModel model;
			model = (GenerateClassTableModel)table.getModel();
			model.selectAll(true);
		} else if (src == deselectAll) {
			GenerateClassTableModel model;
			model = (GenerateClassTableModel)table.getModel();
			model.selectAll(false);
		}
	}
	
	private void generateCode() {
		GenerateClassTableModel model;
		model = (GenerateClassTableModel)table.getModel();
		Map to_generate = model.getSelectedEntities();
		Generator generator;
		String file_name = folder.getText();
		if (outputFolder == null) {
			JOptionPane.showMessageDialog(this, "Enter directory for source files");
			chooseFolder.requestFocus(true);
			return;
		}
		if (!outputFolder.exists()) {
			JOptionPane.showMessageDialog(this, "Directory " 
								+ outputFolder.getName() + " does not exist");
			chooseFolder.requestFocus(true);
			return;
		}
		if (!outputFolder.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Choose directory rather than file");
			chooseFolder.requestFocus(true);
			return;
		}
		generator = new Generator(mediator.getCurrentDataMap()
								, to_generate, outputFolder);
		try {
			if (generatePair.isSelected())
				generator.generateClassPairs();
			else
				generator.generateSingleClasses();
			JOptionPane.showMessageDialog(this, "Class generation finished");
			hide();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error generating classes - " 
				+ e.getMessage());
		}
	}
}// End GenerateClassDialog


class GenerateClassTableModel extends AbstractTableModel 
{
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
		for (int i = 0; i < selected.length; i++) 
		{
			if (entities[i].getClassName() != null 
				&& entities[i].getClassName().trim().length() > 0)
				selected[i] = true;
			else
				selected[i] = false;
		}
	}

	public Class getColumnClass(int col) {
		switch (col) {
			case 0:
				return tempString.getClass();
			default:
				return tempBoolean.getClass();
		}
	}
	
	public int getRowCount() {
		return entities.length;
	}
	
	public int getColumnCount()
	{
		return 2;
	}
	
	public String getColumnName(int column) {
		if (column == 0)
			return "Entity";
		else 
			return "";
	}

	public Object getValueAt(int row, int column)
	{
		if (column == 0) {
			return entities[row].getName();
		} else if (column == 1) {
			return new Boolean(selected[row]);
		} else return "";
		
		
	}// End getValueAt()
	
	public void selectAll(boolean sel_val) {
		for (int i = 0; i < selected.length; i++) 
		{
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
    		Boolean temp = (Boolean)aValue;
    		selected[row] = temp.booleanValue();
    	}
    }// End setValueAt()


	public boolean isCellEditable(int row, int col) {
		if (col == 1)
			if (entities[row].getClassName() != null 
				&& entities[row].getClassName().trim().length() > 0)
				return true;
			else 
				return false;
		else 
			return false;
	}// End isCellEditable()
	
	/** Returns map with the selected ObjEntities.
	  * The key is obj entity and value - its name. */
	public Map getSelectedEntities() {
		Hashtable hash = new Hashtable();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] == true) {
				hash.put(entities[i], entities[i].getName());
			}
		}// End for()
		return hash;
	}
} // End GenerateClassTableModel


/** Class actually opening and closing streams for the files.*/
class Generator extends MapClassGenerator
{
	/** Entities for which to generate code. */
	Map toGenerate;
	DataMap map;
	File folder;
	
	public Generator(DataMap temp_map, Map entities, File temp_folder) {
		super(temp_map);
		map = temp_map;
		toGenerate = entities;
		folder = temp_folder;
		if (!folder.isDirectory() || !folder.exists())
			throw new CayenneRuntimeException(folder.getName() 
									+ " does not exist or is not a directory");
	}
	
	/** FIXME: package name is ignored, need better handling of this. */
    public Writer openWriter(ObjEntity entity, String pkgName, String className) throws Exception
    {
    	try {
			if (!toGenerate.containsKey(entity))
				return null;
			File class_file = new File(folder, className + ".java");
			if (!class_file.exists())
				if (!class_file.createNewFile())
					return null;
			FileWriter writer = new FileWriter(class_file);
			return writer;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
    }


    /** Closes writer after class code has been successfully written by ClassGenerator. */
    public void closeWriter(Writer out) throws Exception {
    	if (null == out)
    		return;
    	out.flush();
    	out.close();
    }

}