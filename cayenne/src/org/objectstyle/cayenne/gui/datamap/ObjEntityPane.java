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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.EntityWrapper;
import org.objectstyle.cayenne.map.*;

/** 
 * Detail view of the ObjEntity properties. 
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class ObjEntityPane extends JPanel
implements DocumentListener, ActionListener
, ObjEntityDisplayListener, ExistingSelectionProcessor
{
	Mediator mediator;
	
	JTextField	name;
	String		oldName;
	JTextField	className;
	JPanel		dbPane;
	JComboBox	dbName;
	
	/** Cludge to prevent marking data map as dirty during initial load. */
	private boolean ignoreChange = false;
	
	
	public ObjEntityPane(Mediator temp_mediator) {
		super();		
		mediator = temp_mediator;		
		mediator.addObjEntityDisplayListener(this);
		
		// Create and layout components
		init();
		
		// Add listeners
		name.getDocument().addDocumentListener(this);
		className.getDocument().addDocumentListener(this);
		dbName.addActionListener(this);
	}

	private void init() {
		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		JLabel nameLbl = new JLabel("Entity name: ");
		name = new JTextField(25);
		
		JLabel classNameLbl	= new JLabel("Class name: ");
		className = new JTextField(25);
		
		JLabel dbNameLbl = new JLabel("Table name:");
		dbName 	= new JComboBox();
		dbName.setBackground(Color.WHITE);
		
		
		Component[] leftCol = new Component[] {
			nameLbl, classNameLbl, dbNameLbl
		};
		
		Component[] rightCol = new Component[] {
			name, className, dbName
		};
		
		JPanel formPanel = PanelFactory.createForm(leftCol, rightCol, 5,5,5,5);
		Spring pad = Spring.constant(5);
		add(formPanel);
		SpringLayout.Constraints cons = layout.getConstraints(formPanel);
		cons.setY(pad);
		cons.setX(pad);
	}

	public void processExistingSelection()
	{
		EntityDisplayEvent e;
		e = new EntityDisplayEvent(this, mediator.getCurrentObjEntity()
			, mediator.getCurrentDataMap(), mediator.getCurrentDataDomain());
		mediator.fireObjEntityDisplayEvent(e);
	}
	

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		// Change db entity for current obj entity
		if (src == dbName) {
			ObjEntity entity = mediator.getCurrentObjEntity();
			DbEntity db_entity;
			EntityWrapper wrap;
			wrap = (EntityWrapper)dbName.getSelectedItem();
			db_entity = (DbEntity)wrap.getEntity();
			entity.setDbEntity(db_entity);
			mediator.fireObjEntityEvent(new EntityEvent(this, entity));
		}
	}

	private void createDbEntity() {
		// Create DbEntity and add it to DataMap
		DbEntity entity = (DbEntity)NamedObjectFactory.createObject(DbEntity.class, mediator.getCurrentDataMap());
		mediator.getCurrentObjEntity().setDbEntity(entity);
		mediator.getCurrentDataMap().addDbEntity(entity);
		EntityEvent event = new EntityEvent(this, mediator.getCurrentObjEntity());
		mediator.fireObjEntityEvent(event);

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
			String classText = className.getText();
			if(classText != null && classText.trim().length() == 0) {
				classText = null;
			}
			
			current_entity.setClassName(classText);
			EntityEvent event = new EntityEvent(this, current_entity);
			mediator.fireObjEntityEvent(event);
		}
	}
	
	public void currentObjEntityChanged(EntityDisplayEvent e) {
		ObjEntity entity = (ObjEntity)e.getEntity();
		if (null == entity  || e.isEntityChanged() == false) 
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
	
	/** 
	 * Creates DefaultComboBoxModel from the list of DbEntities.
	 * Model contains <code>DbEntityWrapper's</code>.
	 * 
	 * @param select DbEntity to make selected. If null, empty 
	 * element is selected.
	 */
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
		}

		DefaultComboBoxModel model = new DefaultComboBoxModel(combo_entries);
		model.setSelectedItem(selected_entry);
		return model;
	}
}