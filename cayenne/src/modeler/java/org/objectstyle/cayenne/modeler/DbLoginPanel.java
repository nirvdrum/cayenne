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
package org.objectstyle.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.modeler.util.PreferenceField;

/**
 * Database login panel.
 * 
 * @author Andrei Adamchik
 * @author Misha Shengauot
 */
public class DbLoginPanel extends CayenneDialog implements ActionListener {
	private static Logger logObj = Logger.getLogger(DbLoginPanel.class);

	protected DataSourceInfo dataSrcInfo;

	protected PreferenceField unInput;
	protected JPasswordField pwdInput;
	protected PreferenceField drInput;
	protected PreferenceField urlInput;
	protected PreferenceField adapterInput;

	protected JButton ok;
	protected JButton cancel;

	public DbLoginPanel(Editor frame) {
		super(frame, "Driver And Login Information", true);
		setResizable(false);

		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());

		JPanel messagePanel = initMessagePanel();
		pane.add(messagePanel, BorderLayout.NORTH);

		// input fields go here
		JPanel inputPanel = initInputArea();
		pane.add(inputPanel, BorderLayout.CENTER);

		// buttons go here
		JPanel buttonsPanel = initButtons();
		this.getRootPane().setDefaultButton(ok);
		pane.add(buttonsPanel, BorderLayout.SOUTH);

		this.pack();
		this.centerWindow();
	}

	protected void disableVKEvents(JTextComponent txtField) {
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		Keymap map = txtField.getKeymap();
		map.removeKeyStrokeBinding(enter);
	}

	protected void disableVKEvents(PreferenceField prefField) {
		// I have no idea how to trap "ENTER" hit on a combo box
		// this should be something faily easy.
		// Anyone can implement that to trigger this Dialog default button?
	}

	public DataSourceInfo getDataSrcInfo() {
		return dataSrcInfo;
	}

	public void setDataSrcInfo(DataSourceInfo dataSrcInfo) {
		this.dataSrcInfo = dataSrcInfo;
		if (dataSrcInfo != null) {
			unInput.setText(dataSrcInfo.getUserName());
			pwdInput.setText(dataSrcInfo.getPassword());
			drInput.setText(dataSrcInfo.getJdbcDriver());
			urlInput.setText(dataSrcInfo.getDataSourceUrl());
			adapterInput.setText(dataSrcInfo.getAdapterClass());
		}
	}

	protected JPanel initInputArea() {
		// user name line
		JLabel unLabel = new JLabel("User Name:");
		unInput = new PreferenceField(ModelerPreferences.USER_NAME);
		disableVKEvents(unInput);

		// password line
		JLabel pwdLabel = new JLabel("Password:");
		pwdInput = new JPasswordField(25);
		disableVKEvents(pwdInput);

		// JDBC driver line
		JLabel drLabel = new JLabel("JDBC Driver Class:");
		drInput = new PreferenceField(ModelerPreferences.JDBC_DRIVER);
		disableVKEvents(drInput);

		// Database URL line
		JLabel urlLabel = new JLabel("Database URL:");
		urlInput = new PreferenceField(ModelerPreferences.DB_URL);
		disableVKEvents(urlInput);

		// Adapter class line
		JLabel adapterLabel = new JLabel("RDBMS Adapter:");
		String[] adapter_arr = {
			DbAdapter.JDBC,
			DbAdapter.SYBASE,
			DbAdapter.MYSQL,
			DbAdapter.ORACLE,
			DbAdapter.POSTGRES
		};
		adapterInput =
			new PreferenceField(
				ModelerPreferences.RDBMS_ADAPTER,
				Arrays.asList(adapter_arr));
		disableVKEvents(adapterInput);

		Component[] left =
			new Component[] {
				unLabel,
				pwdLabel,
				drLabel,
				urlLabel,
				adapterLabel };

		Component[] right =
			new Component[] {
				unInput,
				pwdInput,
				drInput,
				urlInput,
				adapterInput };

		return PanelFactory.createJDK13Form(left, right, 5, 5, 5, 5);
	}

	private JPanel initButtons() {
		// buttons
		ok = new JButton("Ok");
		ok.setActionCommand("ok");

		cancel = new JButton("Cancel");
		cancel.setActionCommand("cancel");

		ok.addActionListener(this);
		cancel.addActionListener(this);

		return PanelFactory.createButtonPanel(new JButton[] { ok, cancel });
	}

	protected JPanel initMessagePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
		JLabel lbl = new JLabel("Enter JDBC Information");
		lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 18));
		lbl.setForeground(Color.red);
		panel.add(lbl);
		return panel;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("ok") && dataSrcInfo != null) {
			// populate DataSourceInfo with text values
			String un = unInput.getText();
			if (un != null && un.length() == 0)
				un = null;
			dataSrcInfo.setUserName(un);

			char[] pwd = pwdInput.getPassword();
			String pwdStr =
				(pwd != null && pwd.length > 0) ? new String(pwd) : null;
			dataSrcInfo.setPassword(pwdStr);

			String dr = drInput.getText();
			if (dr != null && dr.length() == 0)
				dr = null;
			dataSrcInfo.setJdbcDriver(dr);

			String url = urlInput.getText();
			if (url != null && url.length() == 0)
				url = null;
			dataSrcInfo.setDataSourceUrl(url);

			String adapter = adapterInput.getText();
			if (adapter != null && adapter.length() == 0)
				adapter = null;
			dataSrcInfo.setAdapterClass(adapter);

			// set some reasonable pool size
			if (dataSrcInfo.getMinConnections() <= 0)
				dataSrcInfo.setMinConnections(1);

			if (dataSrcInfo.getMaxConnections()
				< dataSrcInfo.getMinConnections())
				dataSrcInfo.setMaxConnections(dataSrcInfo.getMinConnections());

			unInput.storePreferences();
			drInput.storePreferences();
			urlInput.storePreferences();
			adapterInput.storePreferences();
			ModelerPreferences.getPreferences().storePreferences();
		} else if (e.getSource() == cancel) {
			setDataSrcInfo(null);
		}
		this.hide();
	}
}
