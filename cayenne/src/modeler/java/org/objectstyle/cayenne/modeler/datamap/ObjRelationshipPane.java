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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.ObjEntityListener;
import org.objectstyle.cayenne.map.event.ObjRelationshipListener;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.control.ObjRelationshipInfoController;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;

/** 
 * Displays ObjRelationships for the current obj entity. 
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjRelationshipPane
    extends JPanel
    implements
        ActionListener,
        ObjEntityDisplayListener,
        ObjEntityListener,
        ObjRelationshipListener,
        ExistingSelectionProcessor,
        ListSelectionListener,
        TableModelListener {

    private static final Object[] deleteRules =
        new Object[] {
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
            };

    EventController mediator;

    CayenneTable table;
    JButton resolve;

    public ObjRelationshipPane(EventController temp_mediator) {
        super();
        mediator = temp_mediator;
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjRelationshipListener(this);
        // Set up graphical components
        init();
        // Add listeners		
        resolve.addActionListener(this);
    }

    private void init() {
        this.setLayout(new BorderLayout());
        table = new CayenneTable();
        resolve = new JButton("Edit Relationship");
        JPanel panel = PanelFactory.createTablePanel(table, new JButton[] { resolve });
        add(panel, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == resolve) {
            resolveRelationship();
        }
    }

    public void tableChanged(TableModelEvent e) {
        ObjRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            ObjRelationshipTableModel model;
            model = (ObjRelationshipTableModel) table.getModel();
            rel = model.getRelationship(table.getSelectedRow());
            if (rel.getTargetEntity() != null
                && ((ObjEntity) rel.getSourceEntity()).getDbEntity() != null
                && ((ObjEntity) rel.getTargetEntity()).getDbEntity() != null) {
                resolve.setEnabled(true);
            }
            else
                resolve.setEnabled(false);
        }
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationship(ObjRelationship rel) {
        if (rel == null) {
            return;
        }

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        java.util.List rels = model.getObjectList();
        int relPos = rels.indexOf(rel);
        if (relPos >= 0) {
            table.select(relPos);
        }
    }

    public void processExistingSelection() {
        ObjRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            ObjRelationshipTableModel model =
                (ObjRelationshipTableModel) table.getModel();
            rel = model.getRelationship(table.getSelectedRow());
            if (rel.getTargetEntity() != null
                && ((ObjEntity) rel.getSourceEntity()).getDbEntity() != null
                && ((ObjEntity) rel.getTargetEntity()).getDbEntity() != null) {
                resolve.setEnabled(true);
            }
            else
                resolve.setEnabled(false);

            // scroll table
            table.scroll(table.getSelectedRow(), 0);
        }
        else
            resolve.setEnabled(false);

        RelationshipDisplayEvent ev =
            new RelationshipDisplayEvent(
                this,
                rel,
                mediator.getCurrentObjEntity(),
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());
        mediator.fireObjRelationshipDisplayEvent(ev);
    }

    public void valueChanged(ListSelectionEvent e) {
        processExistingSelection();
    }

    private void resolveRelationship() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        new ObjRelationshipInfoController(mediator, model.getRelationship(row)).startup();

        // need to refresh selected row... do this by unselecting/selecting the row
        table.getSelectionModel().clearSelection();
        table.select(row);
    }

    /** Loads obj relationships into table. */
    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this)
            return;

        ObjEntity entity = (ObjEntity) e.getEntity();
        if (entity != null && e.isEntityChanged()) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree, 
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    /** 
     * Creates a lost of ObjEntity names.
     */
    private Object[] createObjEntityComboModel() {
        DataMap map = mediator.getCurrentDataMap();
        Collection objEntities = map.getObjEntities(true);
        int len = objEntities.size();
        Object[] names = objEntities.toArray();

        for (int i = 0; i < len; i++) {
            // substitute Entities with their names
            names[i] = ((Entity) names[i]).getName();
        }

        Arrays.sort(names);
        return names;
    }

    public void objEntityChanged(EntityEvent e) {
    }
    public void objEntityAdded(EntityEvent e) {
        reloadEntityList(e);
    }
    public void objEntityRemoved(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objRelationshipChanged(RelationshipEvent e) {
        table.select(e.getRelationship());
    }

    public void objRelationshipAdded(RelationshipEvent e) {
        rebuildTable((ObjEntity) e.getEntity());
        table.select(e.getRelationship());
    }

    public void objRelationshipRemoved(RelationshipEvent e) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getRelationship());
        model.removeRelationship(e.getRelationship());
        table.select(ind);
    }

    /** Refresh the list of obj entities (targets). 
      * Also refresh the table in case some obj relationships were deleted.*/
    private void reloadEntityList(EntityEvent e) {
        if (e.getSource() == this)
            return;
        // If current model added/removed, do nothing.
        if (mediator.getCurrentObjEntity() == e.getEntity())
            return;
        // If this is just loading new currentObjEntity, do nothing
        if (mediator.getCurrentObjEntity() == null)
            return;
        TableColumn col;
        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET);
        DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();
        JComboBox combo = (JComboBox) editor.getComponent();
        combo.setModel(new DefaultComboBoxModel(createObjEntityComboModel()));
        ObjRelationshipTableModel model;
        model = (ObjRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    protected void rebuildTable(ObjEntity ent) {
        ObjRelationshipTableModel model =
            new ObjRelationshipTableModel(ent, mediator, this);
        model.addTableModelListener(this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col =
            table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_NAME);
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET);
        col.setMinWidth(150);
        JComboBox combo =
            CayenneWidgetFactory.createComboBox(createObjEntityComboModel(), true);
        combo.setEditable(false);
        combo.setSelectedIndex(-1);
        DefaultCellEditor editor = new DefaultCellEditor(combo);
        editor.setClickCountToStart(1);
        col.setCellEditor(editor);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_CARDINALITY);
        col.setMinWidth(150);
        table.getSelectionModel().addListSelectionListener(this);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETERULE);
        col.setMinWidth(60);
        combo = CayenneWidgetFactory.createComboBox(deleteRules, false);
        combo.setEditable(false);
        combo.setSelectedIndex(0); //Default to the first value
        editor = new DefaultCellEditor(combo);
        editor.setClickCountToStart(1);
        col.setCellEditor(editor);
    }
}