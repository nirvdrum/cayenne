package org.objectstyle.cayenne.gui.datamap;
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.objectstyle.cayenne.gui.Editor;

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Used to select schema of the database. */
public class ChooseSchemaDialog extends JDialog
implements ActionListener
{
	public static final int SELECT 	= 0;
	public static final int CANCEL 	= 1;

	private ArrayList schemaList = new ArrayList();
	private String	  schemaName = null;
	
	JComboBox schemaSelect;
	JButton select		= new JButton("Select");
	JButton cancel		= new JButton("Cancel");
	private int choice  = CANCEL;

	public ChooseSchemaDialog(ArrayList schema_list)
	{
		super(Editor.getFrame(), "Select Schema", true);
		schemaList = schema_list;		
		init();
		setSize(380, 150);
		Point point = Editor.getFrame().getLocationOnScreen();
		this.setLocation(point);
	}// End ChooseSchemaDialog
	
	/** Set up the graphical components. */
	private void init() {
		getContentPane().setLayout(new BorderLayout());
		
		// Name text field
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
		JLabel label = new JLabel("Schema list: ");
		Object[] arr = new Object[schemaList.size()];
		arr = schemaList.toArray(arr);
		schemaSelect = new JComboBox(arr);
		temp.add(label);
		temp.add(Box.createHorizontalStrut(5));
		temp.add(schemaSelect);
		temp.add(Box.createHorizontalGlue());
		getContentPane().add(temp, BorderLayout.NORTH);		
				
		temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
		temp.add(select);
		temp.add(Box.createRigidArea(new Dimension(6, 0)));
		temp.add(Box.createHorizontalGlue());
		temp.add(cancel);
		temp.add(Box.createRigidArea(new Dimension(6, 0)));
		temp.add(Box.createHorizontalGlue());
		getContentPane().add(temp, BorderLayout.SOUTH);
				
		select.addActionListener(this);
		cancel.addActionListener(this);
	}// End init()
	
	
	public String getSchemaName() {
		if (getChoice() != SELECT)
			return null;
		return schemaName;
	}


	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == select) {
			processSelect();
		} else if (src == cancel) {
			processCancel();
		}
	}

	public int getChoice() {
		return choice;
	}

	private void processSelect() {
		schemaName = (String)schemaSelect.getSelectedItem();
		choice = SELECT;
		hide();
	}

	private void processCancel() {
		schemaName = null;
		choice = CANCEL;
		hide();
	}	
}