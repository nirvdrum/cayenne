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
package org.objectstyle.cayenne.modeler.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;

/**  
 * Common superclass of tables used in Cayenne. Contains some common configuration
 * settings and utility methods.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class CayenneTable extends JTable {
    private static Logger logObj = Logger.getLogger(CayenneTable.class);

    protected void createDefaultEditors() {
        super.createDefaultEditors();
        DefaultCellEditor temp = new DefaultCellEditor(new JTextField());
        setDefaultEditor(Object.class, temp);
        setDefaultEditor(String.class, temp);
    }

    public CayenneTableModel getCayenneModel() {
        return (CayenneTableModel) getModel();
    }

    public void select(Object row) {
        if (row == null) {
            return;
        }

        CayenneTableModel model = getCayenneModel();
        int ind = model.getObjectList().indexOf(row);

        if (ind >= 0) {
            getSelectionModel().setSelectionInterval(ind, ind);
        }
    }

    public void select(int index) {
        CayenneTableModel model = getCayenneModel();
        if (index >= model.getObjectList().size()) {
            index = model.getObjectList().size() - 1;
        }

        if (index >= 0) {
            getSelectionModel().setSelectionInterval(index, index);
        }
    }

    /**
     * @see javax.swing.event.CellEditorListener#editingStopped(ChangeEvent)
     */
    public void editingStopped(ChangeEvent e) {
        super.editingStopped(e);

        // only go down one row if we are editing text
        int row = getSelectedRow();
        if (row >= 0
            && this.getRowCount() > 0
            && getSelectedTextComponent() != null) {
            row++;

            if (row >= this.getRowCount()) {
                row = 0;
            }
            select(row);
        }
    }

    public JTextComponent getSelectedTextComponent() {
        int row = getSelectedRow();
        int column = getSelectedColumn();
        if (row < 0 || column < 0) {
            return null;
        }

        TableCellEditor editor = this.getCellEditor(row, column);
        if (editor instanceof DefaultCellEditor) {
            Component comp = ((DefaultCellEditor) editor).getComponent();
            if (comp instanceof JTextComponent) {
                return (JTextComponent) comp;
            }
        }
        return null;
    }

    /**
     * @see javax.swing.JTable#editCellAt(int, int, EventObject)
     */
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean edit = super.editCellAt(row, column, e);

        if (edit) {
            JTextComponent t = getSelectedTextComponent();
            if (t != null) {
                if (!t.isFocusOwner()) {
                    t.requestFocus();
                }
                t.setCaretPosition(t.getDocument().getLength());
            }
        }
        return edit;
    }

    /**
     * Scrolls view if it is located in a JViewport, so that the specified cell
     * is displayed in the center.
     */
    public void scroll(int rowIndex, int vColIndex) {
        if (!(getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) getParent();
        Rectangle rect = getCellRect(rowIndex, vColIndex, true);
        Rectangle viewRect = viewport.getViewRect();

        if (viewRect.intersects(rect)) {
            return;
        }

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);
    }
}
