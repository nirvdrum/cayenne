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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.gui.*;
import org.objectstyle.cayenne.gui.util.FileSystemViewDecorator;
import org.objectstyle.cayenne.map.DbGenerator;

/** 
 * Wizard for generating the database from the data map.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class GenerateDbDialog extends CayenneDialog implements ActionListener, ItemListener  {
	static Logger logObj = Logger.getLogger(Editor.class.getName());

	private static final int WIDTH = 380;
	private static final int HEIGHT = 190;

	protected Connection conn;
	protected DbAdapter adapter;
	protected DbGenerator gen;

	protected JTextArea sql;
	protected JButton generate = new JButton("Generate");
	protected JButton cancel = new JButton("Cancel");
	protected JButton saveSql = new JButton("Save SQL");

	/** Drop the existing tables in the database. */
	private JCheckBox dropTables;

	public GenerateDbDialog(Connection conn, DbAdapter adapter) {
		super(Editor.getFrame(), "Generate Database", true);
		if (getMediator().getCurrentDataMap() == null) {
			throw new IllegalStateException(
				"Must have current data map to " + "allow db generation");
		}

		this.conn = conn;
		this.adapter = adapter;
		this.gen = new DbGenerator(adapter, getMediator().getCurrentDataMap());

		init();

		dropTables.addActionListener(this);
		generate.addActionListener(this);
		saveSql.addActionListener(this);
		cancel.addActionListener(this);

		setSize(WIDTH, HEIGHT);
		this.pack();
		this.centerWindow();
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	private void init() {
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		JPanel optionsPane =
			new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
		optionsPane.setBorder(BorderFactory.createTitledBorder("Options"));
		dropTables = new JCheckBox("Drop Tables");
		optionsPane.add(dropTables);
		contentPane.add(optionsPane, BorderLayout.NORTH);

		sql = new JTextArea();
		sql.setRows(16);
		sql.setColumns(40);
		sql.setEditable(true);
		sql.setLineWrap(true);
		sql.setWrapStyleWord(true);

		JScrollPane scrollPanel =
			new JScrollPane(
				sql,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel sqlTextPanel = new JPanel(new BorderLayout());
		sqlTextPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sqlTextPanel.add(scrollPanel, BorderLayout.CENTER);
		contentPane.add(sqlTextPanel, BorderLayout.CENTER);

		JPanel btnPanel =
			PanelFactory.createButtonPanel(
				new JButton[] { generate, cancel, saveSql });
		contentPane.add(btnPanel, BorderLayout.SOUTH);

		initStatements();
	}

	/** 
	 * Builds a list of SQL statements to run.
	 */
	protected void initStatements() {
		// convert them to string representation for display
		StringBuffer buf = new StringBuffer();
		Iterator it = gen.configuredStatements().iterator();
		while (it.hasNext()) {
			buf.append(it.next()).append("\n\n");
		}
		sql.setText(buf.toString());
	}
	

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (generate == src) {
			generateDBSchema();
			setVisible(false);
			dispose();
		} else if (src == saveSql) {
			storeSQL();
		} else if (cancel == src) {
			setVisible(false);
			dispose();
		}
	}

	protected void generateDBSchema() {
		gen.setShouldDropTables(dropTables.isSelected());
		
		try {
			gen.runGenerator(conn);
		} catch (SQLException ex) {

			SQLException exception = ex;
			while ((exception = exception.getNextException()) != null) {
				logObj.log(Level.INFO, "Nested exception", exception);
			}
			logObj.log(Level.INFO, "Main exception", ex);

			JOptionPane.showMessageDialog(
				this,
				"Error creating database - " + ex.getMessage());
			return;
		}
	}

	protected void storeSQL() {
		String projDirStr = getMediator().getConfig().getProjDir();
		File projDir = null;
		if (projDirStr != null) {
			projDir = new File(projDirStr);
		}

		JFileChooser fc;
		FileSystemViewDecorator fileView = new FileSystemViewDecorator(projDir);
		fc = new JFileChooser(fileView);
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		fc.setDialogTitle("Create database");
		if (null != projDir) {
			fc.setCurrentDirectory(projDir);
		}

		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				File file = fc.getSelectedFile();
				FileWriter fw = new FileWriter(file);
				PrintWriter pw = new PrintWriter(fw);
				pw.print(sql.getText());
				pw.flush();
				pw.close();
			} catch (IOException ex) {
				logObj.severe(ex.getMessage());
				ex.printStackTrace();
				JOptionPane.showMessageDialog(
					this,
					"Error writing into file - " + ex.getMessage());
			}
		}
	}

	/**
	 * @see java.awt.event.ItemListener#itemStateChanged(ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == dropTables) {
			gen.setShouldDropTables(dropTables.isSelected());
			initStatements();
		}
	}


}