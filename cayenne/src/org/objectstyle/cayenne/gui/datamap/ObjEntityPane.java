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


import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** Detail view of the ObjEntity properties. 
 * @author Michael Misha Shengaout */
public class ObjEntityPane extends JPanel
implements DocumentListener, ActionListener, ObjEntityDisplayListener
{
	Mediator mediator;
	
	JLabel 		nameLbl;
	JTextField	name;
	String		oldName;
	JLabel		classNameLbl;
	JTextField	className;
	JButton 	remove;
	JPanel		dbPane;
	JLabel		dbNameLbl;
	JComboBox	dbName;
	JButton		dbNew;
	JButton		dbGenerate;
	/** Cludge to prevent marking data map as dirty during initial load. */
	private boolean ignoreChange = false;
	
	public ObjEntityPane(Mediator temp_mediator) {
		super();		
		mediator = temp_mediator;		
		mediator.addObjEntityDisplayListener(this);
		// Create and laout components
		init();
		// Add listeners
		name.getDocument().addDocumentListener(this);
		className.getDocument().addDocumentListener(this);
		remove.addActionListener(this);
		dbName.addActionListener(this);
		dbNew.addActionListener(this);
		dbGenerate.addActionListener(this);
	}

	private void init(){
		SpringLayout layout = new SpringLayout();
		setLayout(layout);

		Box temp = Box.createHorizontalBox();

		Spring pad = Spring.constant(5);
		Spring ySpring = pad;
		add(temp);
		SpringLayout.Constraints cons = layout.getConstraints(temp);
		cons.setY(ySpring);
		cons.setX(pad);

		JLabel nameLbl = new JLabel("Entity name: ");
		name = new JTextField(10);
		JLabel classNameLbl	= new JLabel("Class name: ");
		className = new JTextField(30);

		temp.add(nameLbl);
		temp.add(Box.createHorizontalStrut(5));
		temp.add(name);
		temp.add(Box.createHorizontalStrut(12));
		temp.add(classNameLbl);
		temp.add(Box.createHorizontalStrut(5));
		temp.add(className);
		temp.add(Box.createGlue());

		// DB Pane
		temp 	= Box.createHorizontalBox();
		add(temp);
		ySpring = Spring.sum(ySpring, cons.getConstraint("South"));
		cons = layout.getConstraints(temp);
		cons.setY(ySpring);
		cons.setX(pad);

		dbNameLbl	= new JLabel("Table name:");
		temp.add(dbNameLbl);
		temp.add(Box.createHorizontalStrut(5));
		dbName 	= new JComboBox();
		dbName.setBackground(Color.WHITE);
		temp.add(dbName);
		temp.add(Box.createHorizontalStrut(12));
		dbNew = new JButton("New");
		temp.add(dbNew);
		temp.add(Box.createHorizontalStrut(5));
		dbGenerate= new JButton("Generate");
		temp.add(dbGenerate);
		temp.add(Box.createGlue());

		remove = new JButton("Remove");
		JPanel temp_panel 	= new JPanel(new FlowLayout(FlowLayout.CENTER));
		add(temp_panel);
		ySpring = Spring.sum(Spring.sum(ySpring, pad), cons.getConstraint("South"));
		cons = layout.getConstraints(temp_panel);
		cons.setY(ySpring);
		cons.setX(pad);
		temp_panel.add(remove);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == remove) {
			mediator.removeObjEntity(this, mediator.getCurrentObjEntity());
		} 
		else if (src == dbNew) {
			createDbEntity();			
		} 
		// Change db entity for current obj entity
		else if (src == dbName) {
			ObjEntity entity = mediator.getCurrentObjEntity();
			DbEntity db_entity;
			EntityWrapper wrap;
			wrap = (EntityWrapper)dbName.getSelectedItem();
			db_entity = (DbEntity)wrap.getEntity();
			System.out.println("ObjEntityPane::actionPerformed(), " 
				+ "setting db entity to " 
				+ (db_entity != null ? db_entity.getName() : "") );
			entity.setDbEntity(db_entity);
			mediator.fireObjEntityEvent(new EntityEvent(this, entity));
		}
	}

	private void createDbEntity() {
		// Create DbEntity and add it to DataMap
		DbEntity entity = EntityWrapper.createDbEntity();
		mediator.getCurrentObjEntity().setDbEntity(entity);
		mediator.getCurrentDataMap().addDbEntity(entity);
		EntityWrapper wrapper = new EntityWrapper(entity);
		// Add DbEntity to drop-down in alphabetical order
		DefaultComboBoxModel model = (DefaultComboBoxModel)dbName.getModel();
		EntityWrapper wrap = new EntityWrapper(entity);
		model.insertElementAt(wrap, model.getSize());
		model.setSelectedItem(wrap);
		mediator.fireDbEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
	}

	public void insertUpdate(DocumentEvent e)  { textFieldChanged(e); }
	public void changedUpdate(DocumentEvent e) { textFieldChanged(e); }
	public void removeUpdate(DocumentEvent e)  { textFieldChanged(e); }

	private void textFieldChanged(DocumentEvent e) {
		if (ignoreChange)
			return;
		Document doc = e.getDocument();
		DataMap map = mediator.getCurrentDataMap();
		ObjEntity current_entity = mediator.getCurrentObjEntity();
		if (doc == name.getDocument()) {
			// Change the name of the current obj entity
			GuiFacade.setObjEntityName(map, (ObjEntity)current_entity, name.getText());
			// Make sure this name is propagated to wherever it needs to go
			EntityEvent event = new EntityEvent(this, current_entity, oldName);
			oldName = name.getText();
			mediator.fireObjEntityEvent(event);
		}
		else if (doc == className.getDocument()) {
			current_entity.setClassName(className.getText());
		}
	}
	
	public void currentObjEntityChanged(EntityDisplayEvent e) {
		ObjEntity entity = (ObjEntity)e.getEntity();
		if (null == entity) 
			return;
		ignoreChange = true;
		name.setText(entity.getName());
		oldName = entity.getName();
		className.setText(entity.getClassName() != null ? entity.getClassName() : "");

		// Display DbEntity name in select box
		DefaultComboBoxModel combo_model;
		combo_model = createComboBoxModel(entity.getDbEntity());
		dbName.setModel(combo_model);
		ignoreChange = false;
	}
	
	/** Creates DefaultComboBoxModel from the list of DbEntities.
	 *  Model contains DbEntityWrapper-s.
	 * @param select DbEntity to make selected. If null, empty element is selected. */
	private DefaultComboBoxModel createComboBoxModel(DbEntity select) {
		EntityWrapper selected_entry = null;

		Vector combo_entries = new Vector();		
		DataMap map = mediator.getCurrentDataMap();
		java.util.List entities = map.getDbEntitiesAsList();
		Iterator iter = entities.iterator();
		// First add empty element.
		EntityWrapper wrap = new EntityWrapper(null);
		selected_entry = wrap;
		combo_entries.add(wrap);
		while(iter.hasNext()){
			DbEntity entity  = (DbEntity)iter.next();
			wrap = new EntityWrapper(entity);
			if (select == entity) {
				selected_entry = wrap;
			}
			combo_entries.add(wrap);
		}// End while()

		DefaultComboBoxModel model = new DefaultComboBoxModel(combo_entries);
		model.setSelectedItem(selected_entry);
		return model;
	}
}