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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.TitledBorder;

import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Detail view of the DataNode and DataSourceInfo
 * @author Michael Misha Shengaout */
public class DataNodeDetailView extends JPanel 
implements DocumentListener, DataNodeDisplayListener
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
	
	public DataNodeDetailView(Mediator temp_mediator) {
		super();		
		mediator = temp_mediator;
		mediator.addDataNodeDisplayListener(this);
		// Create and layout components
		init();
		// Add listeners
		name.getDocument().addDocumentListener(this);
	}

	private void init(){
		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		nameLabel 		= new JLabel("Data node name: ");
		name 			= new JTextField(20);
		locationLabel 	= new JLabel("Location: ");
		location 		= new JTextField(25);
		fileBtn			= new JButton("...");
		factoryLabel 	= new JLabel("Data source factory:");
		factory 		= new JComboBox();
		
		JPanel fileChooser = this.formatFileChooser(location, fileBtn);

		Component[] left_comp = new Component[3];
		left_comp[0] = nameLabel;
		left_comp[1] = locationLabel;
		left_comp[2] = factoryLabel;

		Component[] right_comp = new Component[3];
		right_comp[0] = name;
		right_comp[1] = location;
		right_comp[2] = fileChooser;

		JPanel temp = PanelFactory.createForm(left_comp, right_comp, 5,5,5,5);
		Spring pad = Spring.constant(5);
		Spring ySpring = pad;
		add(temp);
		SpringLayout.Constraints cons = layout.getConstraints(temp);
		cons.setY(ySpring);
		cons.setX(pad);
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
		String new_name = name.getText();
		// If name hasn't changed, do nothing
		if (oldName != null && new_name.equals(oldName))
			return;
		DataDomain domain = mediator.getCurrentDataDomain();
		domain.setName(new_name);
		DomainEvent event;
		event = new DomainEvent(this, domain, oldName);
		mediator.fireDomainEvent(event);
		oldName = new_name;

	}
	
	public void currentDataNodeChanged(DataNodeDisplayEvent e) {
		DataNode node = e.getDataNode();
		oldName = node.getName();
		name.setText(oldName);
	}
}

class DataSourceInfoPane extends JPanel 
implements DataNodeDisplayListener
{
	Mediator mediator;
	
	private JLabel userNameLabel;
	private JTextField userName;
	private JLabel	passwordLabel;
	private JPasswordField password;
	private JLabel	repeatPasswordLabel;
	private JPasswordField repeatPassword;
	private JLabel jdbcDriverLabel;
	private JTextField jdbcDriver;
	private JLabel dataSourceURLLabel;
	private JTextField dataSourceURL;
	
	public DataSourceInfoPane(Mediator temp_mediator)
	{
		mediator = temp_mediator;
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		Component[] left_comp = new Component[5];
		left_comp[0] = userNameLabel;
		left_comp[1] = passwordLabel;
		left_comp[2] = repeatPasswordLabel;
		left_comp[3] = jdbcDriverLabel;
		left_comp[4] = dataSourceURLLabel;

		Component[] right_comp = new Component[5];
		right_comp[0] = userName;
		right_comp[1] = password;
		right_comp[2] = repeatPassword;
		left_comp[3] = jdbcDriver;
		left_comp[4] = dataSourceURL;
		
		JPanel temp = PanelFactory.createForm(left_comp, right_comp, 5, 5, 5, 5);
		TitledBorder border = BorderFactory.createTitledBorder("Connection info");
		border.setTitleJustification(TitledBorder.LEFT);
		temp.setBorder(border);
		
		this.add(temp, BorderLayout.CENTER);
	}
	
	public void currentDataNodeChanged(DataNodeDisplayEvent e)
	{
	}
	
	private void populatePanel(DataNode node) {
		this.setEnabled(true);
	}
	
	private void clearPanel() {
		userName.setText("");
		password.setText("");
		repeatPassword.setText("");
		dataSourceURL.setText("");
		this.setEnabled(false);
	}
}