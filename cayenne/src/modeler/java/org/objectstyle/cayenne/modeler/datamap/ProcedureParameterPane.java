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

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;

/**
 * @author Andrei Adamchik
 */
public class ProcedureParameterPane
    extends JPanel
    implements ProcedureDisplayListener, ExistingSelectionProcessor {
    protected EventController eventController;

    protected JTable table;

    public ProcedureParameterPane(EventController eventController) {
        this.eventController = eventController;

        init();

        eventController.addProcedureDisplayListener(this);
    }

    protected void init() {
        setLayout(new BorderLayout());

        // Create table with two columns and no rows.
        table = new CayenneTable();
        JPanel panel = PanelFactory.createTablePanel(table, new JButton[] {
        });
        add(panel, BorderLayout.CENTER);
    }

    public void processExistingSelection() {
        ProcedureDisplayEvent e =
            new ProcedureDisplayEvent(
                this,
                eventController.getCurrentProcedure(),
                eventController.getCurrentDataMap(),
                eventController.getCurrentDataDomain());
        eventController.fireProcedureDisplayEvent(e);
    }

    /**
      * Invoked when currently selected Procedure object is changed.
      */
    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure != null && e.isProcedureChanged()) {
            rebuildTable(procedure);
        }
    }

    protected void rebuildTable(Procedure procedure) {
        ProcedureParameterTableModel model =
            new ProcedureParameterTableModel(procedure, eventController, this);

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        // name column tweaking
        TableColumn nameColumn =
            table.getColumnModel().getColumn(ProcedureParameterTableModel.PARAMETER_NAME);
        nameColumn.setMinWidth(150);

        // types column tweaking
        TableColumn typesColumn =
            table.getColumnModel().getColumn(ProcedureParameterTableModel.PARAMETER_TYPE);
        typesColumn.setMinWidth(90);

        JComboBox typesEditor =
            CayenneWidgetFactory.createComboBox(TypesMapping.getDatabaseTypes(), true);
        typesEditor.setEditable(true);
        typesColumn.setCellEditor(new DefaultCellEditor(typesEditor));

        // direction column tweaking
        TableColumn directionColumn =
            table.getColumnModel().getColumn(
                ProcedureParameterTableModel.PARAMETER_DIRECTION);
        directionColumn.setMinWidth(90);

        JComboBox directionEditor =
            CayenneWidgetFactory.createComboBox(
                ProcedureParameterTableModel.PARAMETER_DIRECTION_NAMES,
                false);
        directionEditor.setEditable(true);
        directionColumn.setCellEditor(new DefaultCellEditor(directionEditor));
    }

}
