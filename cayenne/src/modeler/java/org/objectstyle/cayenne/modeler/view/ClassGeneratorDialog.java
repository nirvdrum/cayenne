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
package org.objectstyle.cayenne.modeler.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.ClassGeneratorController;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.ScopeWidgetFactory;
import org.objectstyle.cayenne.modeler.validator.ValidatorDialog;
import org.scopemvc.core.PropertyManager;
import org.scopemvc.core.Selector;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SCheckBox;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.STableModel;
import org.scopemvc.view.swing.STextField;
import org.scopemvc.view.swing.SwingView;

/** 
 * Dialog for generating Java classes from the DataMap.
 *  
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class ClassGeneratorDialog extends SPanel {

    public ClassGeneratorDialog() {
        init();
    }

    private void init() {
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Generate Java Classes");
        setLayout(new BorderLayout());

        // build entity table
        STable table = new ClassGeneratorTable();
        table.setRowHeight(25);
        table.setRowMargin(3);
        ClassGeneratorModel model = new ClassGeneratorModel(table);
        model.setSelector("entities");
        model.setColumnNames(new String[] { "Entity", "Class", "Generate", "Problems" });
        model.setColumnSelectors(
            new String[] {
                "entity.name",
                "entity.className",
                "selected",
                "validationMessage" });

        table.setModel(model);

        // make sure that long columns are not squeezed
        table.getColumnModel().getColumn(1).setMinWidth(100);
        table.getColumnModel().getColumn(3).setMinWidth(250);

        // build superclass package
        JLabel superClassPackageLabel =
            CayenneWidgetFactory.createLabel("Superclass Package:");
        final STextField superClassPackage = ScopeWidgetFactory.createTextField();
        superClassPackage.setSelector("superClassPackage");

        // build pair checkbox
        JLabel generatePairLabel =
            CayenneWidgetFactory.createLabel("Generate parent/child class pairs:");
        SCheckBox generatePair = new SCheckBox() {
                // (de)activate the text field
    public void itemStateChanged(ItemEvent inEvent) {
                superClassPackage.setEnabled(
                    inEvent.getStateChange() == ItemEvent.SELECTED);
                super.itemStateChanged(inEvent);
            }
        };
        generatePair.setSelector("pairs");

        // build folder selector
        JLabel folderLabel = CayenneWidgetFactory.createLabel("Output directory:");
        JPanel folderPanel = new JPanel();
        folderPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        STextField folder = ScopeWidgetFactory.createTextField();
        folder.setSelector("outputDir");
        SAction chooseAction =
            new SAction(ClassGeneratorController.CHOOSE_LOCATION_CONTROL);
        SButton chooseButton = new SButton(chooseAction);
        chooseButton.setEnabled(true);
        folderPanel.add(folder);
        folderPanel.add(Box.createHorizontalStrut(5));
        folderPanel.add(chooseButton);

        // build action buttons
        SAction generateAction =
            new SAction(ClassGeneratorController.GENERATE_CLASSES_CONTROL);
        SButton generateButton = new SButton(generateAction);
        generateButton.setEnabled(true);

        SAction cancelAction = new SAction(ClassGeneratorController.CANCEL_CONTROL);
        SButton cancelButton = new SButton(cancelAction);
        cancelButton.setEnabled(true);

        // assemble
        JPanel formPanel =
            PanelFactory.createForm(
                new Component[] {
                    generatePairLabel,
                    superClassPackageLabel,
                    folderLabel },
                new Component[] { generatePair, superClassPackage, folderPanel });

        JPanel panel =
            PanelFactory.createTablePanel(
                table,
                new JComponent[] { formPanel },
                new JButton[] { generateButton, cancelButton });
        add(panel, BorderLayout.CENTER);
    }

    class ClassGeneratorModel extends STableModel {
        protected Selector enabledSelector = Selector.fromString("enabled");

        /**
         * Constructor for TableModel.
         * @param table
         */
        public ClassGeneratorModel(JTable table) {
            super(table);
        }

        public boolean isEnabledRow(int rowIndex) {
            // check if this is a failed row
            Object row = getElementAt(rowIndex);
            PropertyManager manager = getItemsManager();
            if (manager == null || row == null) {
                return false;
            }

            try {
                Boolean enabled = (Boolean) manager.get(row, enabledSelector);
                return enabled != null && enabled.booleanValue();
            }
            catch (Exception e) {
                return false;
            }
        }

        /**
         * @see javax.swing.table.TableModel#isCellEditable(int, int)
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // only checkbox is editable
            if (columnIndex != 2) {
                return false;
            }

            return isEnabledRow(rowIndex);
        }
    }

    class ClassGeneratorTable extends STable {
        protected final Dimension preferredSize = new Dimension(500, 300);

        protected DefaultTableCellRenderer problemRenderer;

        public ClassGeneratorTable() {
            problemRenderer = new ClassGeneratorProblemRenderer();
            problemRenderer.setBackground(ValidatorDialog.WARNING_COLOR);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            ClassGeneratorModel model = (ClassGeneratorModel) getModel();

            return (model.isEnabledRow(row))
                ? super.getCellRenderer(row, column)
                : problemRenderer;
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }

    class ClassGeneratorProblemRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

            if (value instanceof Boolean) {
                value = "";
            }

            return super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column);
        }

    }
}
