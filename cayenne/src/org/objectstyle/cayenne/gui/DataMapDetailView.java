package org.objectstyle.cayenne.gui;
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
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.TitledBorder;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Detail view of the DataNode and DataSourceInfo
 * @author Michael Misha Shengaout */
public class DataMapDetailView extends JPanel 
implements DocumentListener, ActionListener, DataMapDisplayListener
{
	Mediator mediator;
	
	JLabel		nameLabel;
	JTextField	name;
	String		oldName;
	
	JLabel		locationLabel;
	JTextField	location;
	JButton		fileBtn;	
	
	/** Cludge to prevent marking map as dirty during initial load. */
	private boolean ignoreChange = false;
	
	public DataMapDetailView(Mediator temp_mediator) {
		super();		
		mediator = temp_mediator;
		mediator.addDataMapDisplayListener(this);
		// Create and layout components
		init();
		// Add listeners
		location.getDocument().addDocumentListener(this);
		name.getDocument().addDocumentListener(this);
		fileBtn.addActionListener(this);
	}

	private void init(){
		BorderLayout layout = new BorderLayout();
		this.setLayout(layout);
		nameLabel 		= new JLabel("Data map name: ");
		name 			= new JTextField(20);
		locationLabel 	= new JLabel("Location: ");
		location 		= new JTextField(25);
		location.setEditable(false);
		fileBtn			= new JButton("...");

		JPanel fileChooser = this.formatFileChooser(location, fileBtn);

		Component[] left_comp = new Component[2];
		left_comp[0] = nameLabel;
		left_comp[1] = locationLabel;

		Component[] right_comp = new Component[2];
		right_comp[0] = name;
		right_comp[1] = fileChooser;

		JPanel temp = PanelFactory.createForm(left_comp, right_comp, 5,5,5,5);
		add(temp, BorderLayout.CENTER);
	}
	
	private JPanel formatFileChooser(JTextField fld, JButton btn) {
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout());
		panel.add(fld, BorderLayout.CENTER);
		panel.add(btn, BorderLayout.EAST);
		
		return panel;
	}

	public void insertUpdate(DocumentEvent e)  { textFieldChanged(e); }
	public void changedUpdate(DocumentEvent e) { textFieldChanged(e); }
	public void removeUpdate(DocumentEvent e)  { textFieldChanged(e); }

	private void textFieldChanged(DocumentEvent e) {
		if (ignoreChange)
			return;
		DataMap map = mediator.getCurrentDataMap();
		DataMapEvent event;
		if (e.getDocument() == name.getDocument()) {
			String new_name = name.getText();
			// If name hasn't changed, do nothing
			if (oldName != null && new_name.equals(oldName))
				return;
			map.setName(new_name);
			event = new DataMapEvent(this, map, oldName);
			mediator.fireDataMapEvent(event);
			oldName = new_name;
		}// End changedName
		else if (e.getDocument() == location.getDocument()) {
			if (map.getLocation().equals(location.getText()))
				return;
			map.setLocation(location.getText());
			event = new DataMapEvent(this, map);
			mediator.fireDataMapEvent(event);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		
		if (src == fileBtn) {
			selectMapLocation();
		}
	}// End actionPerformed()
	
	private void selectMapLocation() {
		DataMap map = mediator.getCurrentDataMap();
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
            File proj_dir = null;
            if (proj_dir_str != null)
            	proj_dir = new File(proj_dir_str);
            JFileChooser fc;
            FileSystemViewDecorator file_view;
            file_view = new FileSystemViewDecorator(proj_dir);
            fc = new JFileChooser(file_view);
            fc.setFileFilter(new DirectoryFilter());
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setDialogTitle("Data Map location");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY );
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
			System.out.println("DataMapDetailView::selectMapLocation(), "
								+"File path is " + file.getAbsolutePath());
			// Create new file
			if (!file.exists())
				file.createNewFile();
			// Determine and set new data map location
			String new_file_location = file.getAbsolutePath();
			String relative_location;
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
			 	relative_location = new_file_location;
			else
				relative_location 
					= new_file_location.substring(proj_dir_str.length() + 1);
			map.setLocation(relative_location);
			location.setText(relative_location);
            // Map location changed
			mediator.fireDataMapEvent(new DataMapEvent(this, map));

        } catch (Exception e) {
            System.out.println("Error setting map location, " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	public void currentDataMapChanged(DataMapDisplayEvent e) {
		DataMap map = e.getDataMap();
		if (null == map)
			return;
		oldName = map.getName();
		ignoreChange = true;
		name.setText(oldName);
		location.setText(map.getLocation());
		ignoreChange = false;
	}

} // End class DataMapDetailView
