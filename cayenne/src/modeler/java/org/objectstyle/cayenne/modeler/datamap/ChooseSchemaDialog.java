/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DbLoader;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.modeler.CayenneDialog;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.PanelFactory;

/** 
 * Dialog that allows to select schema of the database. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ChooseSchemaDialog
    extends CayenneDialog
    implements ActionListener {
    public static final int SELECT = 0;
    public static final int CANCEL = 1;

    protected List schemaList;

    protected JComboBox schemaSelect;
    protected JTextField tabeNamePatternField;
    protected JButton select = new JButton("Continue");
    protected JButton cancel = new JButton("Cancel");
    protected int choice = CANCEL;

    /** 
     * Creates and initializes new ChooseSchemaDialog.
     */
    public ChooseSchemaDialog(List schemaList, DataSourceInfo dsi) {
        super(Editor.getFrame(), "Schema Selector", true);
        setResizable(false);

        this.schemaList = schemaList;

        init(dsi.getUserName());

        select.addActionListener(this);
        cancel.addActionListener(this);

        this.pack();

        // display dialog in the center
        this.centerWindow();
    }

    /** Sets up the graphical components. */
    protected void init(String userName) {
        getContentPane().setLayout(new BorderLayout());

        tabeNamePatternField = new JTextField(10);
        tabeNamePatternField.setText(DbLoader.WILDCARD);

        Component[] left = null;
        Component[] right = null;
        JPanel buttons =
            PanelFactory.createButtonPanel(new JButton[] { select, cancel });

        // optionally create schema selector
        if (schemaList != null && schemaList.size() > 0) {
            schemaSelect =
                new JComboBox(
                    schemaList.toArray(new Object[schemaList.size()]));
            schemaSelect.setBackground(Color.WHITE);

            // select schema belonging to the user
            if (userName != null) {
                Iterator schemas = schemaList.iterator();
                while (schemas.hasNext()) {
                    String schema = (String) schemas.next();
                    if (userName.equalsIgnoreCase(schema)) {
                        schemaSelect.setSelectedItem(schema);
                        break;
                    }
                }
            }

            left =
                new Component[] {
                    new JLabel("Table Name Pattern: "),
                    new JLabel("Schemas: "),
                    new JLabel()};

            right =
                new Component[] { tabeNamePatternField, schemaSelect, buttons };
        } else {
            left =
                new Component[] {
                    new JLabel("Table Name Pattern: "),
                    new JLabel()};

            right = new Component[] { tabeNamePatternField, buttons };
        }

        JPanel panel = PanelFactory.createForm(left, right, 5, 5, 5, 5);
        getContentPane().add(panel, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == select) {
            processSelect();
        } else if (src == cancel) {
            processCancel();
        }
    }

    public int getChoice() {
        return choice;
    }

    private void processSelect() {
        choice = SELECT;
        hide();
    }

    private void processCancel() {
        choice = CANCEL;
        hide();
    }

    public String getSchemaName() {
        if (getChoice() != SELECT || schemaSelect == null) {
            return null;
        }

        String schema = (String) schemaSelect.getSelectedItem();
        if ("".equals(schema)) {
            schema = null;
        }

        return schema;
    }

    /**
     * Returns the tableNamePattern.
     */
    public String getTableNamePattern() {
        if (getChoice() != SELECT) {
            return null;
        }

        String pattern = tabeNamePatternField.getText();
        if ("".equals(pattern)) {
            pattern = null;
        }

        return pattern;
    }
}