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
package org.objectstyle.cayenne.gui.validator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.gui.CayenneDialog;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.event.Mediator;

/** 
 * Dialog for displaying validation errors.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ValidatorDialog
	extends CayenneDialog
	implements ListSelectionListener, ActionListener {

	protected Mediator mediator;
	protected List errMsg;
	protected JTable messages;
	protected JButton closeBtn;


	public ValidatorDialog(
		Editor editor,
		Mediator mediator,
		List errMsg,
		int severity) {
			
		super(editor, "Validation Errors", false);
		this.mediator = mediator;
		this.errMsg = errMsg;

		init();

		messages.getSelectionModel().addListSelectionListener(this);
		closeBtn.addActionListener(this);

        this.pack();
        this.centerWindow();	
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	protected void init() {
		getContentPane().setLayout(new BorderLayout());

		ValidatorTableModel model = new ValidatorTableModel(errMsg);
		messages = new JTable(model);
		messages.setRowHeight(25);
		messages.setRowMargin(3);
		messages.setBackground(new Color(255, 200, 200));
		messages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		messages.setCellSelectionEnabled(false);
		messages.setRowSelectionAllowed(true);
		TableColumn col = messages.getColumnModel().getColumn(0);
		col.setPreferredWidth(50);
		col = messages.getColumnModel().getColumn(1);
		col.setPreferredWidth(220);

		//Create the scroll pane and add the table to it. 
		JScrollPane scrollPane = new JScrollPane(messages);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

        closeBtn = new JButton("Close");
		JPanel panel = PanelFactory.createButtonPanel(new JButton[] {closeBtn});
		getContentPane().add(panel, BorderLayout.SOUTH);
	}

	public void valueChanged(ListSelectionEvent e) {
		if (messages.getSelectedRow() >= 0) {
			ValidatorTableModel model =
				(ValidatorTableModel) messages.getModel();
			ErrorMsg obj = model.getValue(messages.getSelectedRow());
			obj.displayField(mediator, super.getParentEditor());
		}
	}

	public void actionPerformed(ActionEvent e) {
		this.setVisible(false);
		this.dispose();
	}
}

class ValidatorTableModel extends AbstractTableModel {
	List errMsg;

	public ValidatorTableModel(List err_msg) {
		errMsg = err_msg;
	}

	public String getColumnName(int col) {
		if (col == 0)
			return "Severity";
		else if (col == 1)
			return "Error Message";
		else
			return "";
	}

	public int getRowCount() {
		return errMsg.size();
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		ErrorMsg msg = (ErrorMsg) errMsg.get(row);
		if (col == 0) {
			if (msg.getSeverity() == ErrorMsg.ERROR)
				return "ERROR";
			else
				return "WARNING";
		} else if (col == 1) {
			return msg.getMessage();
		} else
			return "";
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public ErrorMsg getValue(int row) {
		return (ErrorMsg) errMsg.get(row);
	}
}