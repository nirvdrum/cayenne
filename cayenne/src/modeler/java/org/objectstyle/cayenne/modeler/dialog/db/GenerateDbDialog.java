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
package org.objectstyle.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Driver;
import java.util.Iterator;

import javax.sql.DataSource;
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
import javax.swing.border.EmptyBorder;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.DriverDataSource;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.swing.CayenneDialog;
import org.objectstyle.cayenne.modeler.swing.PanelFactory;
import org.objectstyle.cayenne.util.Util;

/**
 * Wizard for generating the database from the data map.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class GenerateDbDialog extends CayenneDialog {

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
        super(Application.getFrame(), "Generate Database Schema", true);

        this.dsi = dsi;
        this.adapter = adapter;
        this.gen = gen;

        init();
        initController();
        initStatements();
    }

    private void initController() {
        dropTables.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                gen.setShouldDropTables(dropTables.isSelected());
                initStatements();
            }
        });
        createTables.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                gen.setShouldCreateTables(createTables.isSelected());
                initStatements();
            }
        });
        createFK.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                gen.setShouldCreateFKConstraints(createFK.isSelected());
                initStatements();
            }
        });
        createPK.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                gen.setShouldCreatePKSupport(createPK.isSelected());
                initStatements();
            }
        });
        dropPK.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                gen.setShouldDropPKSupport(dropPK.isSelected());
                initStatements();
            }
        });

        generate.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                generateDBSchema();
            }
        });

        saveSql.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                storeSQL();
            }
        });

        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void init() {

        // create widgets
        dropTables = new JCheckBox("Drop Tables");
        dropTables.setSelected(gen.shouldDropTables());

        createTables = new JCheckBox("Create Tables");
        createTables.setSelected(gen.shouldCreateTables());

        createFK = new JCheckBox("Create FK Support");
        if (!adapter.supportsFkConstraints()) {
            createFK.setEnabled(false);
        }
        else {
            createFK.setSelected(gen.shouldCreateFKConstraints());
        }

        createPK = new JCheckBox("Create Primary Key Support");
        createPK.setSelected(gen.shouldCreatePKSupport());

        dropPK = new JCheckBox("Drop Primary Key Support");
        dropPK.setSelected(gen.shouldDropPKSupport());

        sql = new JTextArea(16, 40);
        sql.setEditable(false);
        sql.setLineWrap(true);
        sql.setWrapStyleWord(true);

        // assemble...
        JPanel optionsPane = new JPanel(new GridLayout(3, 2));
        optionsPane.add(dropTables);
        optionsPane.add(createTables);
        optionsPane.add(new JLabel());
        optionsPane.add(createFK);
        optionsPane.add(dropPK);
        optionsPane.add(createPK);

        JPanel optionsHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsHolder.add(optionsPane);
        optionsHolder.setBorder(BorderFactory.createTitledBorder("Options"));

        JPanel sqlTextPanel = new JPanel(new BorderLayout());
        sqlTextPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        sqlTextPanel.add(new JScrollPane(
                sql,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(optionsHolder, BorderLayout.NORTH);
        contentPane.add(sqlTextPanel, BorderLayout.CENTER);
        contentPane.add(PanelFactory.createButtonPanel(new JButton[] {
                generate, saveSql, cancel
        }), BorderLayout.SOUTH);

        // set dialog parameters
        this.setSize(GENERATEDB_WIDTH, GENERATEDB_HEIGHT);
        this.pack();
        this.centerWindow();
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    /**
     * Builds a list of SQL statements to run.
     */
    protected void initStatements() {
        // convert them to string representation for display
        StringBuffer buf = new StringBuffer();
        Iterator it = gen.configuredStatements().iterator();
        String batchTerminator = gen.getAdapter().getBatchTerminator();

        String lineEnd = (batchTerminator != null)
                ? "\n" + batchTerminator + "\n\n"
                : "\n\n";

        while (it.hasNext()) {
            buf.append(it.next()).append(lineEnd);
        }
        sql.setText(buf.toString());
    }

    /**
     * Action that performs schema operations via DbGenerator.
     */
    protected void generateDBSchema() {
        try {
            // use modeler custom class loader
            Class driverClass = ModelerClassLoader.getClassLoader().loadClass(
                    dsi.getJdbcDriver());
            Driver driver = (Driver) driverClass.newInstance();
            DataSource dataSource = new DriverDataSource(
                    driver,
                    dsi.getDataSourceUrl(),
                    dsi.getUserName(),
                    dsi.getPassword());
            gen.runGenerator(dataSource);

            JOptionPane.showMessageDialog(this, "Schema Generation Complete.");
        }
        catch (ClassNotFoundException e) {
            logObj.warn("Error loading driver. Classpath: "
                    + System.getProperty("java.class.path"), e);
            JOptionPane.showMessageDialog(
                    this,
                    "Driver class not found: " + dsi.getJdbcDriver(),
                    "Schema Generation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        catch (NoClassDefFoundError e) {
            logObj.warn("Error loading driver. Classpath: "
                    + System.getProperty("java.class.path"), e);
            JOptionPane.showMessageDialog(
                    this,
                    "Driver class not found: " + e.getMessage(),
                    "Schema Generation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        catch (Throwable ex) {
            Throwable rootCause = Util.unwindException(ex);
            logObj.warn("Database connection problem", rootCause);
            JOptionPane.showMessageDialog(
                    this,
                    "Database connection problem: " + rootCause.getMessage(),
                    "Schema Generation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void storeSQL() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save SQL Script");

        File projectDir = Application.getProject().getProjectDirectory();

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
            }
            catch (IOException ex) {
                logObj.error(ex);
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error writing into file - "
                        + ex.getMessage());
            }
        }
    }

}