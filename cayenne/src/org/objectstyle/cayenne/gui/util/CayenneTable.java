package org.objectstyle.cayenne.gui.util;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.event.*;


/**  Contains cludge to the JTable. It updates model every time
  *  data is changed in the TextField editor. Also,
  *  If user escapes (presses ESC), returns value in the model
  *  to its original.
  *  @author Michael Misha Shengaout
  */
public class CayenneTable extends JTable
{
	public CayenneTable() {
		super();
		// Replace "cancel" action with my table cancel
		new CayenneTableCancelAction();
	}
	
    protected void createDefaultEditors() {
    	super.createDefaultEditors();
    	TextFieldCellEditor temp = new TextFieldCellEditor();
    	setDefaultEditor(Object.class, temp);
    	setDefaultEditor(String.class, temp);
    }
    
	protected static class TextFieldCellEditor extends DefaultCellEditor
	implements TableCellEditor, DocumentListener
	{
		private int row;
		private int col;
		private String originalValue;
		private TableModel tableModel;
		
		public TextFieldCellEditor() {
			super(new JTextField());
			JTextField field = (JTextField)TextFieldCellEditor.this.getComponent();
			field.getDocument().addDocumentListener(this);
		}
		
		public Component getTableCellEditorComponent(JTable table, Object value
								,boolean isSelected, int temp_row, int temp_col)
		{
			row = temp_row;
			col = temp_col;
			originalValue = value != null ? value.toString() : "";
			tableModel = table.getModel();
			
			String text = value != null ? value.toString() : "";
			JTextField field = (JTextField)TextFieldCellEditor.this.getComponent();
			field.setText(text);
			return field;
		}
	
		public void cancelCellEditing() {
			JTextField field = (JTextField)TextFieldCellEditor.this.getComponent();
			if (!originalValue.equals(field.getText()))
				tableModel.setValueAt(originalValue.toString(), row, col);
			super.cancelCellEditing();
		}
	
		public void insertUpdate(DocumentEvent e)  { textFieldChanged(e); }
		public void changedUpdate(DocumentEvent e) { textFieldChanged(e); }
		public void removeUpdate(DocumentEvent e)  { textFieldChanged(e); }
	
		private void textFieldChanged(DocumentEvent e) {
			Document doc = e.getDocument();
			JTextField field = (JTextField)TextFieldCellEditor.this.getComponent();
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

	private class CayenneTableCancelAction extends AbstractAction
	{
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
			if (col_index >=0) {
				TableColumn col = getColumnModel().getColumn(col_index);
					col.getCellEditor().cancelCellEditing();
			}
			nextAction.actionPerformed(e);
		}
	}	
}

