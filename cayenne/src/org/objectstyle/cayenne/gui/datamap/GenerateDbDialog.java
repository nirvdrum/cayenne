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
import java.util.logging.Logger;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.util.Preferences;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.util.FileSystemViewDecorator;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.PanelFactory;


/** Wizard for generating the database from the data map.
  * @author Michael Misha Shengaout */
public class GenerateDbDialog extends JDialog
implements ActionListener
{
	static Logger logObj = Logger.getLogger(Editor.class.getName());

	private static final int WIDTH  = 380;
	private static final int HEIGHT = 190;
	
	Mediator mediator;
	Connection conn;
	DbAdapter adapter;
	DbGenerator gen;
	
	private JPanel sqlTextPanel;
	private JTextArea sql;
	private JPanel btnPanel;
	private JButton generate = new JButton("Generate");
	private JButton cancel = new JButton("Cancel");
	private JButton saveSql = new JButton("Save SQL");
	
	private JPanel optionsPane;
	/** Drop the existing tables in the databases*/
	private JCheckBox dropTables;
	/** Generate SQL text only without*/
	// private JCheckBox sqlOnly;
	

	public GenerateDbDialog(Mediator temp_mediator, Connection temp_conn
	, DbAdapter temp_adapter)
	{
		super(Editor.getFrame(),"Generate Database", true);
		if (temp_mediator.getCurrentDataMap() == null)
			throw new IllegalStateException("Must have current data map to "
											+"allow db generation");
		mediator = temp_mediator;
		conn = temp_conn;
		adapter = temp_adapter;
		gen = new DbGenerator(conn, adapter);
		
		init();
		
		
		dropTables.addActionListener(this);
		//sqlOnly.addActionListener(this);
		generate.addActionListener(this);
		saveSql.addActionListener(this);
		cancel.addActionListener(this);
		
		setSize(WIDTH, HEIGHT);
		JFrame frame = Editor.getFrame();
		Point point = frame.getLocationOnScreen();
		int width = frame.getWidth();
		int x = (width - WIDTH)/2;
		int height = frame.getHeight();
		int y = (height - HEIGHT)/2;
		
		point.setLocation(point.x + x, point.y + y);
		this.setLocation(point);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	
	private void init()
	{
		getContentPane().setLayout(new GridLayout(2, 1));
		
		Border border = BorderFactory.createTitledBorder("Options");
		optionsPane = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
		optionsPane.setBorder(border);
		dropTables = new JCheckBox("Drop Tables");
		//sqlOnly = new JCheckBox("Generate SQL only");
		optionsPane.add(dropTables);
		//optionsPane.add(sqlOnly);
		getContentPane().add(optionsPane);
		
		sqlTextPanel = new JPanel(new BorderLayout());
		sql = new JTextArea();
		sql.setEditable(false);
		sql.setLineWrap(true);
		sql.setWrapStyleWord(true);
		JScrollPane temp = new JScrollPane(sql);
		temp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sqlTextPanel.add(temp, BorderLayout.CENTER);
		btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		btnPanel.add(generate);
		btnPanel.add(cancel);
		btnPanel.add(saveSql);
		sqlTextPanel.add(btnPanel, BorderLayout.SOUTH);
		getContentPane().add(sqlTextPanel);

		DbGenerator gen = new DbGenerator(conn, adapter);
		DataMap map = mediator.getCurrentDataMap();
		DbEntity[] arr = map.getDbEntities();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < arr.length; i++) {
			buf.append(gen.createTableQuery(arr[i])).append("\n");
			if (adapter.supportsFkConstraints()) {
				buf.append(gen.createFkConstraintsQueries(arr[i])).append("\n");
			}
		}
		sql.setText(buf.toString());
	}
	
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (generate == src) {
			try {
	        	gen.createTables(mediator.getCurrentDataMap(), dropTables.isSelected());
	        } catch (SQLException ex) {
	        	logObj.severe(ex.getMessage());
	        	SQLException exception = ex;
	        	while ((exception = exception.getNextException()) != null) {
	        		logObj.severe(exception.getMessage());
	        	}
	        	ex.printStackTrace();
	        	JOptionPane.showMessageDialog(this, "Error creating database - " 
	        									+ ex.getMessage());
	        	return;
	        }
            setVisible(false);
            dispose();
		}
		else if (src == saveSql) {
	    	try {
	            String proj_dir_str = mediator.getConfig().getProjDir();
	            File proj_dir = null;
	            if (proj_dir_str != null)
	            	proj_dir = new File(proj_dir_str);
	            JFileChooser fc;
	            FileSystemViewDecorator file_view;
	            file_view = new FileSystemViewDecorator(proj_dir);
	            fc = new JFileChooser(file_view);
	            fc.setDialogType(JFileChooser.SAVE_DIALOG);
	            fc.setDialogTitle("Create database");
	            if (null != proj_dir)
	            	fc.setCurrentDirectory(proj_dir);
	            int ret_code = fc.showSaveDialog(this);
	            if ( ret_code != JFileChooser.APPROVE_OPTION)
	                return;
	            File file = fc.getSelectedFile();
	            if (!file.exists())  {
	            	file.createNewFile();
	            	return;
	            }
				FileWriter fw = new FileWriter(file);
				PrintWriter pw = new PrintWriter(fw);
				pw.print(sql.getText());
				pw.flush();
				pw.close();
			} catch (IOException ex) {
	        	logObj.severe(ex.getMessage());
	        	ex.printStackTrace();
	        	JOptionPane.showMessageDialog(this, "Error writing into file - " 
	        									+ ex.getMessage());
	        	return;
			}
		}
		else if (cancel == src) {
            setVisible(false);
            dispose();
		} 
	}
	
}