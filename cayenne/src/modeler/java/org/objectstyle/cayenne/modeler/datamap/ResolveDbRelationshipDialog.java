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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DataMapException;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.CayenneDialog;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.objectstyle.cayenne.project.NamedObjectFactory;

import com.jgoodies.forms.extras.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Editor for DbRelationship and its DbAttributePair's.
 * Also allows specifying the reverse relationship. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ResolveDbRelationshipDialog
    extends CayenneDialog
    implements ActionListener {

    protected DataMap map;
    protected java.util.List originalList;
    protected java.util.List dbRelList;
    protected DbEntity start;
    protected DbEntity end;
    protected DbRelationship dbRel;
    protected boolean isDbRelNew;
    protected DbRelationship reverseDbRel;
    protected boolean isReverseDbRelNew;

    protected JLabel reverseNameLabel =
        CayenneWidgetFactory.createLabel("Reverse Relationship:");
    protected JLabel reverseCheckLabel =
        CayenneWidgetFactory.createLabel("Create Reverse:");
    protected JTextField name = CayenneWidgetFactory.createTextField(30);
    protected JTextField reverseName = CayenneWidgetFactory.createTextField(30);
    protected JCheckBox hasReverseDbRel = new JCheckBox("", false);
    protected CayenneTable table;
    protected JButton add = new JButton("Add");
    protected JButton remove = new JButton("Remove");
    protected JButton save = new JButton("Done");
    protected JButton cancel = new JButton("Cancel");

    private boolean cancelPressed;

    public ResolveDbRelationshipDialog(
        java.util.List relationships,
        DbEntity start,
        DbEntity end,
        boolean toMany) {

        super(CayenneModelerFrame.getFrame(), "", true);

        this.map = getMediator().getCurrentDataMap();
        this.originalList = relationships;
        this.start = start;
        this.end = end;

        // If DbRelationship does not exist, create it.
        if (relationships == null || relationships.size() == 0) {
            dbRelList = new ArrayList();
            dbRel =
                (DbRelationship) NamedObjectFactory.createRelationship(
                    start,
                    end,
                    toMany);
            dbRelList.add(dbRel);
            reverseDbRel = null;
            dbRel.setSourceEntity(start);
            dbRel.setTargetEntity(end);
            dbRel.setToMany(toMany);
            isReverseDbRelNew = true;
            isDbRelNew = true;
        }
        else {
            dbRelList = new ArrayList(relationships);
            dbRel = (DbRelationship) dbRelList.get(0);
            reverseDbRel = dbRel.getReverseRelationship();
            isReverseDbRelNew = (reverseDbRel == null);
            isDbRelNew = false;
        }

        init();

        add.addActionListener(this);
        remove.addActionListener(this);
        save.addActionListener(this);
        cancel.addActionListener(this);
        hasReverseDbRel.addActionListener(this);

        this.pack();
        this.centerWindow();
    }

    /** Set up the graphical components. */
    private void init() {
        setTitle("DbRelationship Info: " + start.getName() + " to " + end.getName());
        getContentPane().setLayout(new BorderLayout());

        if (!isReverseDbRelNew) {
            reverseCheckLabel.setText("Update Reverse:");
        }

        // If this is relationship of DbEntity to itself, disable 
        // reverse relationship check box
        if (start == end) {
            reverseName.setText("");
            reverseName.setEnabled(false);
            reverseNameLabel.setEnabled(false);
            hasReverseDbRel.setSelected(false);
            hasReverseDbRel.setEnabled(false);
            reverseCheckLabel.setEnabled(false);
        }
        // If reverse relationship doesn't exist, deselect checkbox 
        // and disable reverseName text field		
        else if (null == reverseDbRel) {
            reverseName.setText("");
            reverseName.setEnabled(false);
            reverseNameLabel.setEnabled(false);
            hasReverseDbRel.setSelected(false);
        }
        else {
            reverseNameLabel.setEnabled(true);
            reverseName.setEnabled(true);
            reverseName.setText(
                (reverseDbRel.getName() != null ? reverseDbRel.getName() : ""));
            hasReverseDbRel.setSelected(true);
        }

        name.setText((dbRel.getName() != null ? dbRel.getName() : ""));

        DefaultFormBuilder topBuilder =
            new DefaultFormBuilder(new FormLayout("right:max(50dlu;pref), 3dlu, left:max(250dlu;pref)", ""));
        topBuilder.setDefaultDialogBorder();

        topBuilder.append("Relationship:", name);
        topBuilder.append(reverseNameLabel, reverseName);
        topBuilder.append(reverseCheckLabel, hasReverseDbRel);
        getContentPane().add(topBuilder.getPanel(), BorderLayout.NORTH);

        // Attribute pane
        table = new AttributeTable();
        table.setModel(new DbAttributePairTableModel(dbRel, getMediator(), this, true));
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumn col = table.getColumnModel().getColumn(0);
        col.setMinWidth(150);
        JComboBox comboBox =
            CayenneWidgetFactory.createComboBox(
                ModelerUtil.getDbAttributeNames(getMediator(), start),
                true);

        comboBox.setEditable(false);
        col.setCellEditor(new DefaultCellEditor(comboBox));
        col = table.getColumnModel().getColumn(2);
        col.setMinWidth(150);
        comboBox =
            CayenneWidgetFactory.createComboBox(
                ModelerUtil.getDbAttributeNames(getMediator(), end),
                true);
        comboBox.setEditable(false);
        col.setCellEditor(new DefaultCellEditor(comboBox));

        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        // right buttons
        DefaultFormBuilder rightButtonsBuilder =
            new DefaultFormBuilder(new FormLayout("left:max(50dlu;pref)", ""));
        rightButtonsBuilder.setDefaultDialogBorder();

        rightButtonsBuilder.append(add);
        rightButtonsBuilder.append(remove);
        getContentPane().add(rightButtonsBuilder.getPanel(), BorderLayout.EAST);

        // bottom buttons
        getContentPane().add(
            PanelFactory.createButtonPanel(new JButton[] { save, cancel }),
            BorderLayout.SOUTH);
    }

    public List getDbRelationships() {
        return dbRelList;
    }

    public boolean isCancelPressed() {
        return cancelPressed;
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        DbAttributePairTableModel model = (DbAttributePairTableModel) table.getModel();

        if (src == add) {
            model.addRow(new DbAttributePair());
            table.select(model.getRowCount() - 1);
        }
        else if (src == remove) {
            stopEditing();
            int row = table.getSelectedRow();
            model.removeRow(model.getJoin(row));
        }
        else if (src == save) {
            cancelPressed = false;
            save();
        }
        else if (src == cancel) {
            dbRelList = originalList;
            cancelPressed = true;
            hide();
        }
        else if (src == hasReverseDbRel) {
            if (!hasReverseDbRel.isSelected()) {
                reverseName.setText("");
            }
            reverseName.setEnabled(hasReverseDbRel.isSelected());
            reverseNameLabel.setEnabled(hasReverseDbRel.isSelected());
        }
    }

    private void stopEditing() {
        // Stop whatever editing may be taking place
        int col_index = table.getEditingColumn();
        if (col_index >= 0) {
            TableColumn col = table.getColumnModel().getColumn(col_index);
            col.getCellEditor().stopCellEditing();
        }
    }

    private void save() {
        if (!name.getText().equals(dbRel.getName())) {
            String oldName = dbRel.getName();
            MapUtil.setRelationshipName(dbRel.getSourceEntity(), dbRel, name.getText());

            getMediator().fireDbRelationshipEvent(
                new RelationshipEvent(this, dbRel, dbRel.getSourceEntity(), oldName));
        }

        DbAttributePairTableModel model = (DbAttributePairTableModel) table.getModel();
        try {
            model.commit();
        }
        catch (DataMapException e) {
            e.printStackTrace();
            return;
        }

        // If new DbRelationship was created, add it to the source.
        if (isDbRelNew) {
            start.addRelationship(dbRel);
        }

        // check "to dep pk" setting,
        // maybe this is no longer valid
        if (dbRel.isToDependentPK() && !MapUtil.isValidForDepPk(dbRel)) {
            dbRel.setToDependentPK(false);
        }

        // If new reverse DbRelationship was created, add it to the target
        if (hasReverseDbRel.isSelected()) {
            if (reverseDbRel == null) {
                // Check if there is an existing relationship with the same joins
                reverseDbRel = dbRel.getReverseRelationship();
            }

            // If didn't find anything, create reverseDbRel
            if (reverseDbRel == null) {
                reverseDbRel = new DbRelationship();
                reverseDbRel.setSourceEntity(dbRel.getTargetEntity());
                reverseDbRel.setTargetEntity(dbRel.getSourceEntity());
                reverseDbRel.setToMany(!dbRel.isToMany());
            }

            java.util.List revJoins = getReverseJoins();
            reverseDbRel.setJoins(revJoins);

            // check if joins map to a primary key of this entity
            if (!dbRel.isToDependentPK()) {
                Iterator it = revJoins.iterator();
                if (it.hasNext()) {
                    boolean toDepPK = true;
                    while (it.hasNext()) {
                        DbAttributePair join = (DbAttributePair) it.next();
                        if (!join.getTarget().isPrimaryKey()) {
                            toDepPK = false;
                            break;
                        }
                    }

                    reverseDbRel.setToDependentPK(toDepPK);
                }
            }

            reverseDbRel.setName(reverseName.getText());
            if (isReverseDbRelNew) {
                end.addRelationship(reverseDbRel);
            }
        }

        getMediator().fireDbRelationshipEvent(
            new RelationshipEvent(this, dbRel, dbRel.getSourceEntity()));
        hide();
    }

    private List getReverseJoins() {
        List joins = dbRel.getJoins();

        if ((joins == null) || (joins.size() == 0)) {
            return Collections.EMPTY_LIST;
        }

        List reverseJoins = new ArrayList(joins.size());

        // Loop through the list of attribute pairs, create reverse pairs
        // and put them to the reverse list.
        for (int i = 0, numJoins = joins.size(); i < numJoins; i++) {
            DbAttributePair pair = (DbAttributePair) joins.get(i);
            reverseJoins.add(new DbAttributePair(pair.getTarget(), pair.getSource()));
        }

        return reverseJoins;
    }

    final class AttributeTable extends CayenneTable {
        final Dimension preferredSize = new Dimension(450, 200);

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }
}