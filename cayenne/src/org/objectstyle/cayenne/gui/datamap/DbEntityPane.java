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

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;

/** 
 * Detail view of the DbEntity properties. 
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DbEntityPane
	extends JPanel
	implements
		DocumentListener,
		DbEntityDisplayListener,
		ExistingSelectionProcessor,
		ActionListener {
	Mediator mediator;

	JTextField name;
	String oldName;
	JTextField catalog;
	JTextField schema;
	JComboBox parentEntities;
	JLabel parentLabel;

	/** 
	 * Cludge to prevent marking data map as dirty 
	 * during initial load. 
	 */
	private boolean ignoreChange = false;

	public DbEntityPane(Mediator temp_mediator) {
		super();
		mediator = temp_mediator;
		mediator.addDbEntityDisplayListener(this);
		// Create and layout components
		init();
		// Add listeners
		name.getDocument().addDocumentListener(this);
		catalog.getDocument().addDocumentListener(this);
		schema.getDocument().addDocumentListener(this);
		parentEntities.addActionListener(this);
	}

	private void init() {
		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		JLabel nameLabel = new JLabel("Entity name: ");
		name = new JTextField(25);

		JLabel catalogLabel = new JLabel("Catalog: ");
		catalog = new JTextField(25);

		JLabel schemaLabel = new JLabel("Schema: ");
		schema = new JTextField(25);

		parentLabel = new JLabel("Parent entity: ");
		parentLabel.setEnabled(false);
		parentEntities = new JComboBox();
		parentEntities.setEditable(false);
		parentEntities.setEnabled(false);

		Component[] leftCol =
			new Component[] {
				nameLabel,
				catalogLabel,
				schemaLabel,
				parentLabel };

		Component[] rightCol =
			new Component[] { name, catalog, schema, parentEntities };

		JPanel temp = PanelFactory.createForm(leftCol, rightCol, 5, 5, 5, 5);
		Spring pad = Spring.constant(5);
		add(temp);
		SpringLayout.Constraints cons = layout.getConstraints(temp);
		cons.setY(pad);
		cons.setX(pad);

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
		if (ignoreChange)
			return;
		Document doc = e.getDocument();
		DataMap map = mediator.getCurrentDataMap();
		DbEntity current_entity = mediator.getCurrentDbEntity();
		if (doc == name.getDocument()) {
			// Change the name of the current db entity
			GuiFacade.setDbEntityName(
				map,
				(DbEntity) current_entity,
				name.getText());
			// Make sure new name is sent out to all listeners.
			EntityEvent event = new EntityEvent(this, current_entity, oldName);
			oldName = name.getText();
			mediator.fireDbEntityEvent(event);
		} else if (doc == catalog.getDocument()) {
			current_entity.setCatalog(catalog.getText());
		} else if (doc == schema.getDocument()) {
			current_entity.setSchema(schema.getText());
		}
	}

	public void processExistingSelection() {
		EntityDisplayEvent e;
		e =
			new EntityDisplayEvent(
				this,
				mediator.getCurrentDbEntity(),
				mediator.getCurrentDataMap(),
				mediator.getCurrentDataDomain());
		mediator.fireDbEntityDisplayEvent(e);
	}

	public void currentDbEntityChanged(EntityDisplayEvent e) {
		DbEntity entity = (DbEntity) e.getEntity();
		if (null == entity || !e.isEntityChanged()) {
			return;
		}

		ignoreChange = true;
		name.setText(entity.getName());
		oldName = entity.getName();
		catalog.setText(entity.getCatalog() != null ? entity.getCatalog() : "");
		schema.setText(entity.getSchema() != null ? entity.getSchema() : "");
		ignoreChange = false;

		if (entity instanceof DerivedDbEntity) {
			parentLabel.setEnabled(true);
			parentEntities.setEnabled(true);
			java.util.List ents =
				mediator.getCurrentDataMap().getDbEntityNames(true);
			ents.remove(entity.getName());
			DefaultComboBoxModel model =
				new DefaultComboBoxModel(ents.toArray());
			DbEntity parent = ((DerivedDbEntity) entity).getParentEntity();
			if (parent != null) {
				model.setSelectedItem(parent.getName());
			}

			parentEntities.setModel(model);
		} else {
			parentLabel.setEnabled(false);
			parentEntities.setEnabled(false);
			parentEntities.setSelectedIndex(-1);
		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == parentEntities) {
			DbEntity current = mediator.getCurrentDbEntity();

			if (current instanceof DerivedDbEntity) {
				String name = (String) parentEntities.getSelectedItem();
				DbEntity ent =
					(name != null)
						? mediator.getCurrentDataMap().getDbEntity(name, true)
						: null;
				((DerivedDbEntity) current).setParentEntity(ent);

				EntityEvent event = new EntityEvent(this, current);
				mediator.fireDbEntityEvent(event);
			}
		}
	}

}