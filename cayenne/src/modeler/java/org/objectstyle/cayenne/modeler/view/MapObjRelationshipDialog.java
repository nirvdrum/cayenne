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
import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.MapObjRelationshipController;
import org.objectstyle.cayenne.modeler.model.EntityRelationshipsModel;
import org.objectstyle.cayenne.modeler.model.MapObjRelationshipModel;
import org.scopemvc.core.Selector;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SLabel;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.STableModel;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.extras.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A view of the dialog for mapping an ObjRelationship to one or more
 * DbRelationships.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class MapObjRelationshipDialog extends SPanel {
    static final Logger logObj = Logger.getLogger(MapObjRelationshipDialog.class);

    protected STable pathTable;

    public MapObjRelationshipDialog() {
        init();
    }

    protected void init() {
        // create widgets 
        SButton saveButton =
            new SButton(new SAction(MapObjRelationshipController.SAVE_CONTROL));
        saveButton.setEnabled(true);

        SButton cancelButton =
            new SButton(new SAction(MapObjRelationshipController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);

        SButton newToOneButton =
            new SButton(new SAction(MapObjRelationshipController.NEW_TOONE_CONTROL));
        newToOneButton.setEnabled(true);
        SButton newToManyButton =
            new SButton(new SAction(MapObjRelationshipController.NEW_TOMANY_CONTROL));
        newToManyButton.setEnabled(true);

        SLabel objRelationshipLabel = new SLabel();
        Font boldFont = objRelationshipLabel.getFont().deriveFont(Font.BOLD);
        objRelationshipLabel.setFont(boldFont);
        objRelationshipLabel.setSelector("relationship.name");

        SLabel srcEntityLabel = new SLabel();
        srcEntityLabel.setFont(boldFont);
        srcEntityLabel.setSelector("relationship.sourceEntity.name");

        SLabel targetEntityLabel = new SLabel();
        targetEntityLabel.setFont(boldFont);
        targetEntityLabel.setSelector("relationship.targetEntity.name");

        DefaultFormBuilder builder =
            new DefaultFormBuilder(
                new FormLayout("right:max(50dlu;pref), 3dlu, left:max(180dlu;pref)", ""));
        builder.setDefaultDialogBorder();
        builder.append("ObjRelationship:", objRelationshipLabel);
        builder.append("Source ObjEntity:", srcEntityLabel);
        builder.append("Target ObjEntity:", targetEntityLabel);

        pathTable = new ObjRelationshipPathTable();
        STableModel pathTableModel = new STableModel(pathTable);
        pathTableModel.setSelector(MapObjRelationshipModel.DB_RELATIONSHIP_PATH_SELECTOR);
        pathTableModel.setColumnNames(new String[] { "DbRelationship Path" });
        pathTableModel.setColumnSelectors(
            new Selector[] {
                 EntityRelationshipsModel.RELATIONSHIP_DISPLAY_NAME_SELECTOR });

        pathTable.setModel(pathTableModel);
        pathTable.setSelectionSelector(
            MapObjRelationshipModel.SELECTED_PATH_COMPONENT_SELECTOR);
        pathTable.getColumn("DbRelationship Path").setCellEditor(
            RelationshipPicker.createEditor(this));

        // assemble
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Map ObjRelationship");
        setLayout(new BorderLayout());

        add(builder.getPanel(), BorderLayout.NORTH);
        add(new JScrollPane(pathTable), BorderLayout.CENTER);

        DefaultFormBuilder rightButtons =
            new DefaultFormBuilder(new FormLayout("left:max(50dlu;pref)", ""));
        rightButtons.setDefaultDialogBorder();

        rightButtons.append(newToOneButton);
        rightButtons.append(newToManyButton);
        add(rightButtons.getPanel(), BorderLayout.EAST);

        add(
            PanelFactory.createButtonPanel(new JButton[] { saveButton, cancelButton }),
            BorderLayout.SOUTH);
    }

    /**
     * Cancels any editing that might be going on in the path table.
     */
    public void cancelTableEditing() {
        int row = pathTable.getEditingRow();
        if (row < 0) {
            return;
        }

        int column = pathTable.getEditingColumn();
        if (column < 0) {
            return;
        }

        TableCellEditor editor = pathTable.getCellEditor(row, column);
        if (editor != null) {
            editor.cancelCellEditing();
        }
    }

    class ObjRelationshipPathTable extends STable {
        final Dimension preferredSize = new Dimension(300, 300);

        ObjRelationshipPathTable() {
            setRowHeight(25);
            setRowMargin(3);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }

    static final class RelationshipPicker extends DefaultCellEditor {
        JComboBox comboBox;
        SwingView view;

        static TableCellEditor createEditor(SwingView view) {
            JComboBox relationshipCombo = new JComboBox();
            relationshipCombo.setEditable(false);
            return new RelationshipPicker(view, relationshipCombo);
        }

        RelationshipPicker(SwingView view, JComboBox comboBox) {
            super(comboBox);
            this.comboBox = comboBox;
            this.view = view;
        }

        public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {

            // initialize combo box
            MapObjRelationshipModel model =
                (MapObjRelationshipModel) view.getBoundModel();

            EntityRelationshipsModel relationshipWrapper =
                (EntityRelationshipsModel) model.getDbRelationshipPath().get(row);

            DefaultComboBoxModel comboModel =
                new DefaultComboBoxModel(relationshipWrapper.getRelationshipNames());
            comboModel.setSelectedItem(value);
            comboBox.setModel(comboModel);

            // call super
            return super.getTableCellEditorComponent(
                table,
                value,
                isSelected,
                row,
                column);
        }
    }
}
