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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DbLoader;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for selecting database reverse-engineering parameters.
 */
public class ChooseSchemaDialog extends CayenneDialog {

    public static final int CANCEL = 0;
    public static final int SELECT = 1;

    protected JLabel schemaLabel;
    protected JComboBox schemaSelect;
    protected JTextField tabeNamePatternField;
    protected JButton select;
    protected JButton cancel;
    protected int choice;

    /**
     * Creates and initializes new ChooseSchemaDialog.
     */
    public ChooseSchemaDialog(Collection schemas, String dbUserName) {
        super(CayenneModelerFrame.getFrame(), "DB Reengineering Options", true);

        init();
        initController();
        initFromModel(schemas, dbUserName);

        this.pack();
        this.centerWindow();
    }

    /** Sets up the graphical components. */
    protected void init() {

        // create widgets...
        select = new JButton("Continue");
        cancel = new JButton("Cancel");
        schemaSelect = CayenneWidgetFactory.createComboBox();
        tabeNamePatternField = CayenneWidgetFactory.createTextField();

        // assemble
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:max(150dlu;pref)",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Table Name Pattern:", tabeNamePatternField);
        schemaLabel = builder.append("Schemas:", schemaSelect);

        JPanel buttons = PanelFactory.createButtonPanel(new JButton[] {
                select, cancel
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setResizable(false);
    }

    protected void initController() {
        select.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processSelect();
            }
        });

        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processCancel();
            }
        });
    }

    protected void initFromModel(Collection schemas, String dbUserName) {
        this.choice = CANCEL;
        this.tabeNamePatternField.setText(DbLoader.WILDCARD);

        boolean showSchemaSelector = schemas != null && !schemas.isEmpty();
        schemaSelect.setVisible(showSchemaSelector);
        schemaLabel.setVisible(showSchemaSelector);

        if (showSchemaSelector) {

            schemaSelect.setModel(new DefaultComboBoxModel(schemas.toArray()));

            // select schema belonging to the user
            if (dbUserName != null) {
                Iterator it = schemas.iterator();
                while (it.hasNext()) {
                    String schema = (String) it.next();
                    if (dbUserName.equalsIgnoreCase(schema)) {
                        schemaSelect.setSelectedItem(schema);
                        break;
                    }
                }
            }
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

    /**
     * Returns selected schema.
     */
    public String getSchemaName() {
        String schema = (String) schemaSelect.getSelectedItem();
        return "".equals(schema) ? null : schema;
    }

    /**
     * Returns the tableNamePattern.
     */
    public String getTableNamePattern() {
        return "".equals(tabeNamePatternField.getText()) ? null : tabeNamePatternField
                .getText();
    }
}