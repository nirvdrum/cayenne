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

import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;
import org.objectstyle.cayenne.access.*;


public class DbLoginPanel extends JDialog implements ActionListener {
    static Logger logObj = Logger.getLogger(DbLoginPanel.class.getName());


    private static void disableVKEvents(JTextField txtField) {
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        Keymap map = txtField.getKeymap();
        map.removeKeyStrokeBinding(enter);
    }


    protected DataSourceInfo dataSrcInfo;

    protected JTextField unInput;
    protected JPasswordField pwdInput;
    protected JTextField drInput;
    protected JTextField urlInput;
    protected JTextField adapterInput;

    protected JButton ok;
	protected JButton cancel;

    public DbLoginPanel(JFrame frame) {
        super(frame, "Driver And Login Information", true);
        setResizable(false);


        Container pane = this.getContentPane();
        pane.setLayout(new BorderLayout());

        JPanel messagePanel = new JPanel();
        initMessagePanel(messagePanel);
        pane.add(messagePanel, BorderLayout.NORTH);


        // input fields go here
        JPanel inputPanel = new JPanel();
        initInputArea(inputPanel);
        pane.add(inputPanel, BorderLayout.CENTER);


        // buttons go here
        JPanel buttonsPanel = new JPanel();
        this.getRootPane().setDefaultButton(initButtons(buttonsPanel));
        pane.add(buttonsPanel, BorderLayout.SOUTH);

        this.pack();

        // center the dialog on the screen
        int width = this.getWidth();
        int height = this.getHeight();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        this.setBounds(x, y, width, height);
    }



    public DataSourceInfo getDataSrcInfo() {
        return dataSrcInfo;
    }


    public void setDataSrcInfo(DataSourceInfo dataSrcInfo) {
        this.dataSrcInfo = dataSrcInfo;
        if(dataSrcInfo != null) {
            unInput.setText(dataSrcInfo.getUserName());
            pwdInput.setText(dataSrcInfo.getPassword());
            drInput.setText(dataSrcInfo.getJdbcDriver());
            urlInput.setText(dataSrcInfo.getDataSourceUrl());
            adapterInput.setText(dataSrcInfo.getAdapterClass());
        }
    }


    protected void initInputArea(JPanel panel) {
        panel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 10));

        GridBagLayout gbl = new GridBagLayout();
        panel.setLayout(gbl);

        GridBagConstraints cstr = new GridBagConstraints();
        cstr.ipadx = 3;
        cstr.ipady = 3;
        cstr.insets = new Insets(2, 2, 2, 2);

        // user name line
        JLabel unLabel = new JLabel("User Name:");
        cstr.gridwidth = 1;
        cstr.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(unLabel, cstr);
        panel.add(unLabel);

        unInput = new JTextField(25);
        disableVKEvents(unInput);
        cstr.gridwidth = GridBagConstraints.REMAINDER;
        cstr.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(unInput, cstr);
        panel.add(unInput);


        // password line
        JLabel pwdLabel = new JLabel("Password:");
        cstr.gridwidth = 1;
        cstr.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(pwdLabel, cstr);
        panel.add(pwdLabel);

        pwdInput = new JPasswordField(25);
        disableVKEvents(pwdInput);
        cstr.gridwidth = GridBagConstraints.REMAINDER;
        cstr.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(pwdInput, cstr);
        panel.add(pwdInput);

        // JDBC driver line
        JLabel drLabel = new JLabel("JDBC Driver Class:");
        cstr.gridwidth = 1;
        cstr.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(drLabel, cstr);
        panel.add(drLabel);

        drInput = new JTextField(25);
        disableVKEvents(drInput);
        cstr.gridwidth = GridBagConstraints.REMAINDER;
        cstr.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(drInput, cstr);
        panel.add(drInput);


        // Database URL line
        JLabel urlLabel = new JLabel("Database URL:");
        cstr.gridwidth = 1;
        cstr.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(urlLabel, cstr);
        panel.add(urlLabel);

        urlInput = new JTextField(25);
        disableVKEvents(urlInput);
        cstr.gridwidth = GridBagConstraints.REMAINDER;
        cstr.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(urlInput, cstr);
        panel.add(urlInput);


        // Adapter class line
        JLabel adapterLabel = new JLabel("RDBMS Adapter:");
        cstr.gridwidth = 1;
        cstr.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(adapterLabel, cstr);
        panel.add(adapterLabel);

        adapterInput = new JTextField(25);
        disableVKEvents(adapterInput);
        cstr.gridwidth = GridBagConstraints.REMAINDER;
        cstr.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(adapterInput, cstr);
        panel.add(adapterInput);
    }


    /** Returns default button */
    private JButton initButtons(JPanel panel) {
        panel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 7));
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // buttons
        ok = new JButton("Ok");
        ok.setActionCommand("ok");
        panel.add(ok);

        cancel = new JButton("Cancel");
        cancel.setActionCommand("cancel");
        panel.add(cancel);

        ok.addActionListener(this);
        cancel.addActionListener(this);

        return ok;
    }


    protected void initMessagePanel(JPanel panel) {
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JLabel lbl = new JLabel("Enter JDBC Information");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 18));
        lbl.setForeground(Color.red);
        panel.add(lbl);
    }


    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("ok") && dataSrcInfo != null) {
            // populate DataSourceInfo with text values
            String un = unInput.getText();
            if(un != null && un.length() == 0)
                un = null;
            dataSrcInfo.setUserName(un);

            char[] pwd = pwdInput.getPassword();
            String pwdStr = (pwd != null && pwd.length > 0) ? new String(pwd) : null;
            dataSrcInfo.setPassword(pwdStr);

            String dr = drInput.getText();
            if(dr != null && dr.length() == 0)
                dr = null;
            dataSrcInfo.setJdbcDriver(dr);

            String url = urlInput.getText();
            if(url != null && url.length() == 0)
                url = null;
            dataSrcInfo.setDataSourceUrl(url);

            String adapter = adapterInput.getText();
            if(adapter != null && adapter.length() == 0)
                adapter = null;
            dataSrcInfo.setAdapterClass(adapter);

            // set some reasonable pool size
            if(dataSrcInfo.getMinConnections() <= 0)
                dataSrcInfo.setMinConnections(1);

            if(dataSrcInfo.getMaxConnections() < dataSrcInfo.getMinConnections())
                dataSrcInfo.setMaxConnections(dataSrcInfo.getMinConnections());
        }
        else if (e.getSource() == cancel) {
        	setDataSrcInfo(null);
        }
        this.hide();
    }
}
