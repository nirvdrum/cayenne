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

import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Detail view of the DataNode and DataSourceInfo
 * @author Michael Misha Shengaout */
public class DataNodeDetailView extends JPanel 
implements DocumentListener, ActionListener, DataNodeDisplayListener
{
	Mediator mediator;
	
	JLabel		nameLabel;
	JTextField	name;
	String		oldName;
	
	JLabel		locationLabel;
	JTextField	location;
	JButton		fileBtn;	
	
	JLabel		factoryLabel;
	JComboBox	factory;

	JLabel		adapterLabel;
	JComboBox	adapter;
	
	JLabel			userNameLabel;
	JTextField		userName;
	JLabel			passwordLabel;
	JPasswordField	password;
	JLabel			driverLabel;
	JTextField		driver;
	JLabel			urlLabel;
	JTextField		url;
	JLabel			minConnectionsLabel;
	// FIXME!!! Need to restrict only to numbers
	JTextField		minConnections;
	JLabel			maxConnectionsLabel;
	// FIXME!!! Need to restrict only to numbers
	JTextField		maxConnections;
	
	public DataNodeDetailView(Mediator temp_mediator) {
		super();		
		mediator = temp_mediator;
		mediator.addDataNodeDisplayListener(this);
		// Create and layout components
		init();
		// Add listeners
		location.getDocument().addDocumentListener(this);
		name.getDocument().addDocumentListener(this);
		userName.getDocument().addDocumentListener(this);
		password.getDocument().addDocumentListener(this);
		driver.getDocument().addDocumentListener(this);
		url.getDocument().addDocumentListener(this);
		minConnections.getDocument().addDocumentListener(this);
		maxConnections.getDocument().addDocumentListener(this);
		factory.addActionListener(this);
		adapter.addActionListener(this);
		fileBtn.addActionListener(this);
	}

	private void init(){
		GridBagLayout layout = new GridBagLayout();
		this.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 100;
		constraints.weighty = 100;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.NORTHWEST;

		nameLabel 		= new JLabel("Data node name: ");
		name 			= new JTextField(20);
		locationLabel 	= new JLabel("Location: ");
		location 		= new JTextField(25);
		fileBtn			= new JButton("...");
		factoryLabel 	= new JLabel("Data source factory:");
		factory 		= new JComboBox();
		factory.setEditable(true);
		adapterLabel 	= new JLabel("DB query adapter:");
		adapter 		= new JComboBox();
		adapter.setEditable(true);
		
		JPanel fileChooser = this.formatFileChooser(location, fileBtn);

		Component[] left_comp = new Component[4];
		left_comp[0] = nameLabel;
		left_comp[1] = locationLabel;
		left_comp[2] = factoryLabel;
		left_comp[3] = adapterLabel;

		Component[] right_comp = new Component[4];
		right_comp[0] = name;
		right_comp[1] = fileChooser;
		right_comp[2] = factory;
		right_comp[3] = adapter;

		JPanel temp = PanelFactory.createForm(left_comp, right_comp, 5,5,5,5);
		add(temp, constraints);
		
		userNameLabel 	= new JLabel("User name: ");
		userName		= new JTextField(20);
		passwordLabel	= new JLabel("Password: ");
		password		= new JPasswordField(20);
		driverLabel		= new JLabel("Driver class: ");
		driver			= new JTextField(30);
		urlLabel		= new JLabel("Database URL: ");
		url				= new JTextField(30);
		minConnectionsLabel = new JLabel("Min connections: ");
		minConnections		= new JTextField(5);
		maxConnectionsLabel	= new JLabel("Max connections: ");
		maxConnections		= new JTextField(5);
		
		left_comp = new Component[6];
		left_comp[0] = userNameLabel;
		left_comp[1] = passwordLabel;
		left_comp[2] = driverLabel;
		left_comp[3] = urlLabel;
		left_comp[4] = minConnectionsLabel;
		left_comp[5] = maxConnectionsLabel;

		right_comp = new Component[6];
		right_comp[0] = userName;
		right_comp[1] = password;
		right_comp[2] = driver;
		right_comp[3] = url;
		right_comp[4] = minConnections;
		right_comp[5] = maxConnections;

		temp = PanelFactory.createForm(left_comp, right_comp, 5,5,5,5);
		TitledBorder border;
		border = BorderFactory.createTitledBorder("Data Source Info");
		temp.setBorder(border);
		constraints.gridheight = 2;
		constraints.gridy = 1;
		add(temp, constraints);
		
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
		DataNode node = mediator.getCurrentDataNode();
		GuiDataSource src = (GuiDataSource)node.getDataSource();
		DataSourceInfo info = src.getDataSourceInfo();
		DataNodeEvent event;
		if (e.getDocument() == name.getDocument()) {
			String new_name = name.getText();
			// If name hasn't changed, do nothing
			if (oldName != null && new_name.equals(oldName))
				return;
			node.setName(new_name);
			event = new DataNodeEvent(this, node, oldName);
			mediator.fireDataNodeEvent(event);
			oldName = new_name;
		}// End changedName
		else if (e.getDocument() == location.getDocument()) {
			node.setDataSourceLocation(location.getText());
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		} else if (e.getDocument() == userName.getDocument()) {
			info.setUserName(userName.getText());
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		} else if (e.getDocument() == password.getDocument()) {
			String pswd = new String(password.getPassword());
			info.setPassword(pswd);
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		} else if (e.getDocument() == driver.getDocument()) {
			info.setJdbcDriver(driver.getText());
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		} else if (e.getDocument() == url.getDocument()) {
			info.setDataSourceUrl(url.getText());
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		} else if (e.getDocument() == minConnections.getDocument()) {
			if (minConnections.getText().trim().length() > 0)
				info.setMinConnections(Integer.parseInt(minConnections.getText()));
			else
				info.setMinConnections(0);
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		} else if (e.getDocument() == maxConnections.getDocument()) {
			if (maxConnections.getText().trim().length() > 0)
				info.setMaxConnections(Integer.parseInt(maxConnections.getText()));
			else
				info.setMaxConnections(0);
			event = new DataNodeEvent(this, node);
			mediator.fireDataNodeEvent(event);
		}

	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		
		if (src == factory) {
			String ele = (String)factory.getModel().getSelectedItem();
			if (null != ele && ele.trim().length() > 0) {
				if (ele.equals(DataSourceFactory.DIRECT_FACTORY))
					fileBtn.setEnabled(true);
				else 
					fileBtn.setEnabled(false);
				mediator.getCurrentDataNode().setDataSourceFactory(ele);
			}
			else 
				mediator.getCurrentDataNode().setDataSourceFactory(null);
		} else if (src == adapter) {
			String ele = (String)adapter.getModel().getSelectedItem();
			DbAdapter adapt = null; 
			try {
				if (ele != null && ele.trim().length() > 0)
					adapt = (DbAdapter)Class.forName(ele).getDeclaredConstructors()[0].newInstance(new Object[0]);
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
				adapter.setSelectedIndex(-1);
				return;
			}
			mediator.getCurrentDataNode().setAdapter(adapt);
		} else if (src == fileBtn) {
			selectNodeLocation();
		}
	}// End actionPerformed()
	
	private void selectNodeLocation() {
		DataNode node = mediator.getCurrentDataNode();
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
            fc.setDialogTitle("Data Node location");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY );
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            String old_loc = node.getDataSourceLocation();
            // Get absolute path for old location
            if (null != proj_dir)
            	old_loc = proj_dir + File.separator + old_loc;
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
			node.setDataSourceLocation(relative_location);
			location.setText(relative_location);
            // Node location changed - mark current domain dirty
			mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	public void currentDataNodeChanged(DataNodeDisplayEvent e) {
		System.out.println("In currentDataNodeChanged() 1.0");
		DataNode node = e.getDataNode();
		GuiDataSource src = (GuiDataSource)node.getDataSource();
		oldName = node.getName();
		name.setText(oldName);
		location.setText(node.getDataSourceLocation());
		populateFactory(node.getDataSourceFactory());
		DbAdapter adapter = node.getAdapter();
		if (null != adapter)
			populateDbAdapter(adapter.getClass().getName().trim());
		else populateDbAdapter("");
		DataSourceInfo info = src.getDataSourceInfo();
		populateDataSourceInfo(info);
		System.out.println("In currentDataNodeChanged() 2.0");
	}
	
	private void populateDbAdapter(String selected_class){
		DefaultComboBoxModel model;
		String[] arr 
			= {DbAdapter.JDBC
			  ,DbAdapter.SYBASE
			  ,DbAdapter.MYSQL
			  ,DbAdapter.ORACLE
			  ,DbAdapter.POSTGRES};
		model = new DefaultComboBoxModel(arr);
		if (selected_class != null && selected_class.length() > 0) {
			boolean found = false;
			for (int i = 0; i < model.getSize(); i++)  {
				String ele = (String)model.getElementAt(i);
				if (ele.equals(selected_class)) {
					model.setSelectedItem(ele);
					found = true;
					break;
				}
			}// End for()
			// In case if there is unknown factory
			if (!found) {
				model.addElement(selected_class);
				model.setSelectedItem(selected_class);
			}
		}// End if there is factory to select
		adapter.setModel(model);
	}
	
	private void populateFactory(String selected_class) {
		DefaultComboBoxModel model;
		String[] arr 
			= {DataSourceFactory.JNDI_FACTORY
			  ,DataSourceFactory.DIRECT_FACTORY};
		model = new DefaultComboBoxModel(arr);
		if (selected_class != null && selected_class.length() > 0) {
			boolean found = false;
			for (int i = 0; i < model.getSize(); i++)  {
				String ele = (String)model.getElementAt(i);
				if (ele.equals(selected_class)) {
					model.setSelectedItem(ele);
					found = true;
					break;
				}
			}// End for()
			// In case if there is unknown factory
			if (!found) {
				model.addElement(selected_class);
				model.setSelectedItem(selected_class);
			}
		}// End if there is factory to select
		factory.setModel(model);
	}
	
	
	private void populateDataSourceInfo(DataSourceInfo info) {
		userName.setText(info.getUserName());
		password.setText(info.getPassword());
		driver.setText(info.getJdbcDriver());
		url.setText(info.getDataSourceUrl());
		minConnections.setText(String.valueOf(info.getMinConnections()));
		maxConnections.setText(String.valueOf(info.getMaxConnections()));
	}//end populateDataSourceInfo()
	
} // End class DataNodeDetailView


