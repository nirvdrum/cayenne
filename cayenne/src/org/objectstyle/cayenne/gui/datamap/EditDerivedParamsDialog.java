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
package org.objectstyle.cayenne.gui.datamap;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.objectstyle.cayenne.gui.*;
import org.objectstyle.cayenne.map.DerivedDbAttribute;

/**
 * Dialog window that alows selecting DbAttributes 
 * for derived attribute expression.
 *  
 * @author Andrei Adamchik
 */
public class EditDerivedParamsDialog
	extends CayenneDialog
	implements ActionListener {
	protected java.util.List params;

	protected JButton add = new JButton("Add");
	protected JButton remove = new JButton("Remove");
	protected JButton save = new JButton("Save");
	protected JButton cancel = new JButton("Cancel");

	/**
	 * Constructor for EditDerivedParamsDialog.
	 */
	public EditDerivedParamsDialog(DerivedDbAttribute attr) {
		super(Editor.getFrame(), "Edit Derived Attribute Parameters", true);

		// create a new collection to allow independent modifications
		params = new ArrayList(attr.getParams());

		init();
		pack();
		centerWindow();
	}

	protected void init() {
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());

		JPanel buttons =
			PanelFactory.createButtonPanel(
				new JButton[] { add, remove, save, cancel });
		pane.add(buttons, BorderLayout.SOUTH);

		add.addActionListener(this);
		remove.addActionListener(this);
		save.addActionListener(this);
		cancel.addActionListener(this);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == add) {
			addRow();
		} else if (src == remove) {
			removeRow();
		} else if (src == save) {
			save();
		} else if (src == cancel) {
			cancel();
		}
	}

	protected void removeRow() {

	}

	protected void addRow() {

	}

	protected void save() {
        hide();
	}

	protected void cancel() {
		hide();
	}
}
