/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.modeler.CayenneDialog;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.PanelFactory;

/** 
 * Wizard for generating the database from the data map.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class GenerateDbDialog
    extends CayenneDialog
    implements ActionListener, ItemListener {
    private static Logger logObj = Logger.getLogger(GenerateDbDialog.class);

    private static final int GENERATEDB_WIDTH = 380;
    private static final int GENERATEDB_HEIGHT = 190;

    protected DataSourceInfo dsi;
    protected DbAdapter adapter;
    protected DbGenerator gen;

    protected JTextArea sql;
    protected JButton generate = new JButton("Generate");
    protected JButton cancel = new JButton("Close");
    protected JButton saveSql = new JButton("Save SQL");

    protected JCheckBox dropTables;
    protected JCheckBox createTables;
    protected JCheckBox createFK;
    protected JCheckBox createPK;
    protected JCheckBox dropPK;

    public GenerateDbDialog(DataSourceInfo dsi, DbAdapter adapter, DbGenerator gen) {
        super(CayenneModelerFrame.getFrame(), "Generate Database Schema", true);
        
        this.dsi = dsi;
        this.adapter = adapter;
        this.gen = gen;

        init();

        dropTables.addItemListener(this);
        createTables.addItemListener(this);
        createFK.addItemListener(this);
        createPK.addItemListener(this);
        dropPK.addItemListener(this);

        generate.addActionListener(this);
        saveSql.addActionListener(this);
        cancel.addActionListener(this);

        setSize(GENERATEDB_WIDTH, GENERATEDB_HEIGHT);
        this.pack();
        this.centerWindow();
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void init() {
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        dropTables = new JCheckBox("Drop Tables");
        dropTables.setSelected(gen.shouldDropTables());

        createTables = new JCheckBox("Create Tables");
        createTables.setSelected(gen.shouldCreateTables());

        createFK = new JCheckBox("Create FK Support");
        if (!adapter.supportsFkConstraints()) {
            createFK.setEnabled(false);
        } else {
            createFK.setSelected(gen.shouldCreateFKConstraints());
        }

        createPK = new JCheckBox("Create Primary Key Support");
        createPK.setSelected(gen.shouldCreatePKSupport());

        dropPK = new JCheckBox("Drop Primary Key Support");
        dropPK.setSelected(gen.shouldDropPKSupport());

        JPanel optionsPane =
            PanelFactory.createForm(
                new Component[] { dropTables, new JLabel(), dropPK },
                new Component[] { createTables, createFK, createPK });
        optionsPane.setBorder(BorderFactory.createTitledBorder("Options"));
        contentPane.add(optionsPane, BorderLayout.NORTH);

        sql = new JTextArea();
        sql.setRows(16);
        sql.setColumns(40);
        sql.setEditable(false);
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
            PanelFactory.createButtonPanel(new JButton[] { generate, saveSql, cancel });
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
        String batchTerminator = gen.getAdapter().getBatchTerminator();

        String lineEnd =
            (batchTerminator != null) ? "\n" + batchTerminator + "\n\n" : "\n\n";

        while (it.hasNext()) {
            buf.append(it.next()).append(lineEnd);
        }
        sql.setText(buf.toString());
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (generate == src) {
            generateDBSchema();
        } else if (src == saveSql) {
            storeSQL();
        } else if (cancel == src) {
            setVisible(false);
            dispose();
        }
    }

    protected void generateDBSchema() {
        try {
            gen.runGenerator(dsi);
            JOptionPane.showMessageDialog(this, "Schema Generation Complete.");

        } catch (Exception ex) {
            StringBuffer buffer = new StringBuffer("Schema Generation Error - " + ex.getMessage());
            if (ex instanceof SQLException) {
                SQLException exception = (SQLException) ex;
                while ((exception = exception.getNextException()) != null) {
                    buffer.append(" - ").append(exception.getMessage());
                    logObj.log(Level.INFO, "Nested exception", exception);
                }
            }
            logObj.log(Level.INFO, "Main exception", ex);

            JOptionPane.showMessageDialog(
                this,
                buffer.toString());
            return;
        }
    }

    protected void storeSQL() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save SQL Script");

        File projectDir = CayenneModelerFrame.getProject().getProjectDirectory();

        if (projectDir != null) {
            fc.setCurrentDirectory(projectDir);
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
                logObj.error(ex);
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
        if (e.getSource() == dropTables) {
            gen.setShouldDropTables(dropTables.isSelected());
        } else if (e.getSource() == createTables) {
            gen.setShouldCreateTables(createTables.isSelected());
        } else if (e.getSource() == createFK) {
            gen.setShouldCreateFKConstraints(createFK.isSelected());
        } else if (e.getSource() == createPK) {
            gen.setShouldCreatePKSupport(createPK.isSelected());
        } else if (e.getSource() == dropPK) {
            gen.setShouldDropPKSupport(dropPK.isSelected());
        }

        initStatements();
    }

}