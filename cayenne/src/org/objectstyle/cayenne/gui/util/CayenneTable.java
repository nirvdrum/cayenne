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
package org.objectstyle.cayenne.gui.util;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import javax.swing.text.Document;

/**  Contains cludge to the JTable. It updates model every time
  *  data is changed in the TextField editor. Also,
  *  If user escapes (presses ESC), returns value in the model
  *  to its original.
  * 
  * @author Michael Misha Shengaout
  * @author Andrei Adamchik
  */
public class CayenneTable extends JTable {
	public CayenneTable() {
		super();
		// Replace "cancel" action with custom table cancel
		new CayenneTableCancelAction();
	}

	protected void createDefaultEditors() {
		super.createDefaultEditors();
		TextFieldCellEditor temp = new TextFieldCellEditor();
		setDefaultEditor(Object.class, temp);
		setDefaultEditor(String.class, temp);
		CayenneBooleanRenderer temp1 = new CayenneBooleanRenderer();
		this.setDefaultRenderer(Boolean.class, temp1);
	}

    public CayenneTableModel getCayenneModel() {
    	return (CayenneTableModel)getModel();
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

	protected static class TextFieldCellEditor
		extends DefaultCellEditor
		implements TableCellEditor, DocumentListener {
		private int row;
		private int col;
		private String originalValue;
		private TableModel tableModel;

		public TextFieldCellEditor() {
			super(new JTextField());
			JTextField field =
				(JTextField) TextFieldCellEditor.this.getComponent();
			field.getDocument().addDocumentListener(this);
		}

		public Component getTableCellEditorComponent(
			JTable table,
			Object value,
			boolean isSelected,
			int temp_row,
			int temp_col) {
			row = temp_row;
			col = temp_col;
			originalValue = value != null ? value.toString() : "";
			tableModel = table.getModel();

			String text = value != null ? value.toString() : "";
			JTextField field =
				(JTextField) TextFieldCellEditor.this.getComponent();
			field.setText(text);
			return field;
		}

		public void cancelCellEditing() {
			JTextField field =
				(JTextField) TextFieldCellEditor.this.getComponent();
			if (!originalValue.equals(field.getText()))
				tableModel.setValueAt(originalValue.toString(), row, col);
			super.cancelCellEditing();
		}

		public void insertUpdate(DocumentEvent e) {
			textFieldChanged(e);
		}
		public void changedUpdate(DocumentEvent e) {
			textFieldChanged(e);
		}
		public void removeUpdate(DocumentEvent e) {
			textFieldChanged(e);
		}

		private void textFieldChanged(DocumentEvent e) {
			Document doc = e.getDocument();
			JTextField field =
				(JTextField) TextFieldCellEditor.this.getComponent();
			String text = field.getText();
			Object obj = tableModel.getValueAt(row, col);
			String model_text = "";
			if (null != obj)
				model_text = obj.toString();
			// If initial loading of data, ignore.
			if (model_text.equals(text))
				return;
			tableModel.setValueAt(text, row, col);
		}
	}

	static private class CayenneBooleanRenderer
		extends JCheckBox
		implements TableCellRenderer {
		public CayenneBooleanRenderer() {
			super();
			setHorizontalAlignment(JLabel.CENTER);
		}

		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				super.setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			TableModel model = table.getModel();
			setSelected((value != null && ((Boolean) value).booleanValue()));
			setEnabled(model.isCellEditable(row, column));
			return this;
		}
	}

	private class CayenneTableCancelAction extends AbstractAction {
		Action nextAction;

		/** Creates chain of responsibilities. */
		public CayenneTableCancelAction() {
			nextAction = getActionMap().get("cancel");
			getActionMap().put("cancel", this);
		}

		public void setNextAction(Action a) {
			nextAction = a;
		}

		public void actionPerformed(ActionEvent e) {
			// Stop whatever editing may be taking place
			int col_index = getEditingColumn();
			if (col_index >= 0) {
				TableColumn col = getColumnModel().getColumn(col_index);
				col.getCellEditor().cancelCellEditing();
			}
			nextAction.actionPerformed(e);
		}
	}
}
