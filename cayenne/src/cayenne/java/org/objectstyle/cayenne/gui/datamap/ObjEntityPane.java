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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Logger;

import javax.swing.*;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.event.EntityDisplayEvent;
import org.objectstyle.cayenne.gui.event.EntityEvent;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.gui.util.CayenneTextField;
import org.objectstyle.cayenne.gui.util.EntityWrapper;
import org.objectstyle.cayenne.gui.util.MapUtil;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.util.NamedObjectFactory;

/** 
 * Detail view of the ObjEntity properties. 
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class ObjEntityPane
	extends JPanel
	implements
		ActionListener,
		ObjEntityDisplayListener,
		ExistingSelectionProcessor,
		ItemListener {

	static Logger logObj = Logger.getLogger(ObjEntityPane.class.getName());

	protected Mediator mediator;
	protected JTextField name;
	protected JTextField className;
	protected JPanel dbPane;
	protected JComboBox dbName;
	protected JButton tableLabel;
	protected JCheckBox readOnly;

	/** Cludge to prevent marking data map as dirty during initial load. */
	private boolean ignoreChange = false;

	public ObjEntityPane(Mediator mediator) {
		this.mediator = mediator;
		mediator.addObjEntityDisplayListener(this);

		// Create and layout components
		init();

		// Add listeners
		InputVerifier inputCheck = new FieldVerifier();
		name.setInputVerifier(inputCheck);
		className.setInputVerifier(inputCheck);

		dbName.addActionListener(this);
		tableLabel.addActionListener(this);
	}

	private void init() {
		setLayout(new BorderLayout());

		JLabel nameLbl = new JLabel("Entity name: ");
		name = new CayenneTextField(25);

		JLabel classNameLbl = new JLabel("Class name: ");
		className = new CayenneTextField(25);

		tableLabel = PanelFactory.createLabelButton("Table name:");

		dbName = new JComboBox();
		dbName.setBackground(Color.WHITE);

		JLabel checkLabel = new JLabel("Read-only: ");
		readOnly = new JCheckBox();
		readOnly.addItemListener(this);

		Component[] leftCol =
			new Component[] { nameLbl, classNameLbl, tableLabel, checkLabel };

		Component[] rightCol =
			new Component[] { name, className, dbName, readOnly };

		add(
			PanelFactory.createForm(leftCol, rightCol, 5, 5, 5, 5),
			BorderLayout.NORTH);
	}

	public void processExistingSelection() {
		EntityDisplayEvent e =
			new EntityDisplayEvent(
				this,
				mediator.getCurrentObjEntity(),
				mediator.getCurrentDataMap(),
				mediator.getCurrentDataDomain());
		mediator.fireObjEntityDisplayEvent(e);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src == dbName) {
			// Change DbEntity for current ObjEntity
			ObjEntity entity = mediator.getCurrentObjEntity();
			EntityWrapper wrap = (EntityWrapper) dbName.getSelectedItem();
			entity.setDbEntity((DbEntity) wrap.getEntity());
			mediator.fireObjEntityEvent(new EntityEvent(this, entity));
		} else if (tableLabel == src) {
			// Jump to DbEntity of the current ObjEntity
			DbEntity entity = mediator.getCurrentObjEntity().getDbEntity();
			if (entity != null) {
				DataDomain dom = mediator.getCurrentDataDomain();
				DataMap map = dom.getMapForDbEntity(entity.getName());
				mediator.fireDbEntityDisplayEvent(
					new EntityDisplayEvent(this, entity, map, dom));
			}
		}
	}

	private void createDbEntity() {
		// Create DbEntity and add it to DataMap
		DbEntity entity =
			(DbEntity) NamedObjectFactory.createObject(
				DbEntity.class,
				mediator.getCurrentDataMap());
		mediator.getCurrentObjEntity().setDbEntity(entity);
		mediator.getCurrentDataMap().addDbEntity(entity);
		EntityEvent event =
			new EntityEvent(this, mediator.getCurrentObjEntity());
		mediator.fireObjEntityEvent(event);

		EntityWrapper wrapper = new EntityWrapper(entity);

		// Add DbEntity to dropdown in alphabetical order
		DefaultComboBoxModel model = (DefaultComboBoxModel) dbName.getModel();
		EntityWrapper wrap = new EntityWrapper(entity);
		model.insertElementAt(wrap, model.getSize());
		model.setSelectedItem(wrap);
		mediator.fireDbEntityEvent(
			new EntityEvent(this, entity, EntityEvent.ADD));

	}

	public void currentObjEntityChanged(EntityDisplayEvent e) {
		ObjEntity entity = (ObjEntity) e.getEntity();
		if (entity == null || !e.isEntityChanged()) {
			return;
		}

		ignoreChange = true;
		name.setText(entity.getName());
		className.setText(
			entity.getClassName() != null ? entity.getClassName() : "");
		readOnly.setSelected(entity.isReadOnly());

		// Display DbEntity name in select box
		dbName.setModel(createComboBoxModel(entity.getDbEntity()));
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
		while (iter.hasNext()) {
			DbEntity entity = (DbEntity) iter.next();
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
	
	/**
	 * @see java.awt.event.ItemListener#itemStateChanged(ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {	
		if(ignoreChange) {
			return;
		}
			
		if (e.getSource() == readOnly) {
			ObjEntity ent = mediator.getCurrentObjEntity();
			if(ent != null) {
				ent.setReadOnly(readOnly.isSelected());
				mediator.fireObjEntityEvent(new EntityEvent(this, ent));
			}
		}
	}
	

	class FieldVerifier extends InputVerifier {
		public boolean verify(JComponent input) {
			if (input == name) {
				return verifyName();
			} else if (input == className) {
				return verifyClassName();
			} else {
				return true;
			}
		}

		protected boolean verifyName() {
			String text = name.getText();
			if (text != null && text.trim().length() == 0) {
				text = null;
			}

			DataMap map = mediator.getCurrentDataMap();
			ObjEntity ent = mediator.getCurrentObjEntity();

			ObjEntity matchingEnt = map.getObjEntity(text);

			if (matchingEnt == null) {
				// completely new name, set new name for entity
				EntityEvent e = new EntityEvent(this, ent, ent.getName());
				MapUtil.setObjEntityName(map, ent, text);
				mediator.fireObjEntityEvent(e);
				return true;
			} else if (matchingEnt == ent) {
				// no name changes, just return
				return true;
			} else {
				// there is an entity with the same name
				return false;
			}
		}

		protected boolean verifyClassName() {
			String classText = className.getText();
			if (classText != null && classText.trim().length() == 0) {
				classText = null;
			}

			ObjEntity ent = mediator.getCurrentObjEntity();

			if (!org
				.objectstyle
				.cayenne
				.util
				.Util
				.nullSafeEquals(ent.getClassName(), classText)) {

				ent.setClassName(classText);
				mediator.fireObjEntityEvent(new EntityEvent(this, ent));
			}

			return true;
		}
	}
}