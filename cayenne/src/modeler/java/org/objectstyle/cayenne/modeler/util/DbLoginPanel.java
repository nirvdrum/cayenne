/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.swing.CayenneDialog;
import org.objectstyle.cayenne.modeler.swing.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.swing.PanelFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Database login panel.
 */
public class DbLoginPanel extends CayenneDialog implements ActionListener {

    protected DataSourceInfo dataSrcInfo;

    protected PreferenceField unInput;
    protected JPasswordField pwdInput;
    protected PreferenceField drInput;
    protected PreferenceField urlInput;
    protected JComboBox adapterInput;

    protected JButton ok;
    protected JButton cancel;

    private static String COMMAND_OK = "ok";
    private static String COMMAND_CANCEL = "cancel";

    public DbLoginPanel(CayenneModelerFrame frame) {
        super(frame, "Driver And Login Information", true);
        this.setResizable(true);

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

        // closing the window means "Cancel"
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                ActionEvent ae = new ActionEvent(
                        this,
                        ActionEvent.ACTION_PERFORMED,
                        COMMAND_CANCEL);
                DbLoginPanel.this.actionPerformed(ae);
            }
        });

        this.pack();
        this.centerWindow();
    }

    protected void disableVKEvents(JTextComponent txtField) {
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        Keymap map = txtField.getKeymap();
        map.removeKeyStrokeBinding(enter);
    }

    protected void disableVKEvents(PreferenceField prefField) {
        // TODO: I have no idea how to trap "ENTER" hit on a combo box
        // this should be something fairly easy.
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
            adapterInput.setSelectedItem(dataSrcInfo.getAdapterClassName());
        }
    }

    protected JPanel initInputArea() {
        // create widgets

        unInput = CayenneWidgetFactory
                .createPreferenceField(ModelerPreferences.USER_NAME);
        disableVKEvents(unInput);

        pwdInput = new JPasswordField(25);
        disableVKEvents(pwdInput);

        drInput = CayenneWidgetFactory
                .createPreferenceField(ModelerPreferences.JDBC_DRIVER);
        disableVKEvents(drInput);

        urlInput = CayenneWidgetFactory.createPreferenceField(ModelerPreferences.DB_URL);
        disableVKEvents(urlInput);

        adapterInput = CayenneWidgetFactory.createComboBox(DbAdapterInfo
                .getStandardAdapters(), false);
        adapterInput.setEditable(true);

        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:max(250dlu;pref)",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append(CayenneWidgetFactory.createLabel("User Name:"), unInput);
        builder.append(CayenneWidgetFactory.createLabel("Password:"), pwdInput);
        builder.append(CayenneWidgetFactory.createLabel("JDBC Driver Class:"), drInput);
        builder.append(CayenneWidgetFactory.createLabel("Database URL:"), urlInput);
        builder.append(CayenneWidgetFactory.createLabel("RDBMS Adapter:"), adapterInput);

        return builder.getPanel();
    }

    private JPanel initButtons() {
        // buttons
        ok = new JButton("Ok");
        ok.setActionCommand(COMMAND_OK);

        cancel = new JButton("Cancel");
        cancel.setActionCommand(COMMAND_CANCEL);

        ok.addActionListener(this);
        cancel.addActionListener(this);

        return PanelFactory.createButtonPanel(new JButton[] {
                ok, cancel
        });
    }

    protected JPanel initMessagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JLabel lbl = CayenneWidgetFactory.createLabel("Enter JDBC Information");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 18));
        lbl.setForeground(Color.red);
        panel.add(lbl);
        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(COMMAND_OK) && dataSrcInfo != null) {
            // populate DataSourceInfo with text values
            String un = unInput.getText();
            if (un != null && un.length() == 0)
                un = null;
            dataSrcInfo.setUserName(un);

            char[] pwd = pwdInput.getPassword();
            String pwdStr = (pwd != null && pwd.length > 0) ? new String(pwd) : null;
            dataSrcInfo.setPassword(pwdStr);

            String dr = drInput.getText();
            if (dr != null && dr.length() == 0)
                dr = null;
            dataSrcInfo.setJdbcDriver(dr);

            String url = urlInput.getText();
            if (url != null && url.length() == 0)
                url = null;
            dataSrcInfo.setDataSourceUrl(url);

            String adapter = (String) adapterInput.getSelectedItem();
            if (adapter != null && adapter.length() == 0)
                adapter = null;
            dataSrcInfo.setAdapterClassName(adapter);

            // set some reasonable pool size
            if (dataSrcInfo.getMinConnections() <= 0)
                dataSrcInfo.setMinConnections(1);

            if (dataSrcInfo.getMaxConnections() < dataSrcInfo.getMinConnections())
                dataSrcInfo.setMaxConnections(dataSrcInfo.getMinConnections());

            unInput.storePreferences();
            drInput.storePreferences();
            urlInput.storePreferences();
            ModelerPreferences.getPreferences().storePreferences();
        }
        else if (e.getActionCommand().equals(COMMAND_CANCEL)) {
            this.setDataSrcInfo(null);
        }
        this.hide();
    }

}