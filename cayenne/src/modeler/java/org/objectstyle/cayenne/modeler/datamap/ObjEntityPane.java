/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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

package org.objectstyle.cayenne.modeler.datamap;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.EntityWrapper;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.util.Util;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Detail view of the ObjEntity properties. 
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class ObjEntityPane
    extends JPanel
    implements ActionListener, ObjEntityDisplayListener, ExistingSelectionProcessor, ItemListener {

    protected EventController mediator;
    protected JTextField name;
    protected JTextField className;
    protected JTextField superClassName;
    protected JPanel dbPane;
    protected JComboBox dbName;
    protected JButton tableLabel;
    protected JCheckBox readOnly;

    /** Cludge to prevent marking data map as dirty during initial load. */
    private boolean ignoreChange = false;

    public ObjEntityPane(EventController mediator) {
        this.mediator = mediator;
        mediator.addObjEntityDisplayListener(this);

        // Create and layout components
        init();

        // Add listeners
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        className.setInputVerifier(inputCheck);
        superClassName.setInputVerifier(inputCheck);

        dbName.addActionListener(this);
        tableLabel.addActionListener(this);
    }

    private void init() {
        // create widgets
        name = CayenneWidgetFactory.createTextField();
        superClassName = CayenneWidgetFactory.createTextField();
        className = CayenneWidgetFactory.createTextField();
        dbName = CayenneWidgetFactory.createComboBox();

        readOnly = new JCheckBox();
        readOnly.addItemListener(this);
        tableLabel = CayenneWidgetFactory.createLabelButton("Table name:");

        // assemble
        setLayout(new BorderLayout());
        FormLayout layout =
            new FormLayout("right:max(50dlu;pref), 3dlu, fill:max(170dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("ObjEntity Configuration");
        builder.append("ObjEntity Name:", name);
        builder.append("Java Class:", className);
        builder.append("Superclass:", superClassName);
        builder.append(tableLabel, dbName);
        builder.append("Read-only:", readOnly);

        add(builder.getPanel(), BorderLayout.CENTER);
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
        }
        else if (tableLabel == src) {
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

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        ObjEntity entity = (ObjEntity) e.getEntity();
        if (entity == null || !e.isEntityChanged()) {
            return;
        }

        ignoreChange = true;
        name.setText(entity.getName());
        superClassName.setText(
            entity.getSuperClassName() != null ? entity.getSuperClassName() : "");
        className.setText(entity.getClassName() != null ? entity.getClassName() : "");
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
        Iterator iter = map.getDbEntities().iterator();
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
        if (ignoreChange) {
            return;
        }

        if (e.getSource() == readOnly) {
            ObjEntity ent = mediator.getCurrentObjEntity();
            if (ent != null) {
                ent.setReadOnly(readOnly.isSelected());
                mediator.fireObjEntityEvent(new EntityEvent(this, ent));
            }
        }
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else if (input == superClassName) {
                return verifySuperClassName();
            }
            else if (input == className) {
                return verifyClassName();
            }
            else {
                return true;
            }
        }

        protected boolean verifyName() {
            String text = name.getText();
            if (text == null || text.trim().length() == 0) {
                text = "";
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
            }
            else if (matchingEnt == ent) {
                // no name changes, just return
                return true;
            }
            else {
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

            if (!Util.nullSafeEquals(ent.getClassName(), classText)) {

                ent.setClassName(classText);
                mediator.fireObjEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }
        protected boolean verifySuperClassName() {
            String parentClassText = superClassName.getText();
            if (parentClassText != null && parentClassText.trim().length() == 0) {
                parentClassText = null;
            }

            ObjEntity ent = mediator.getCurrentObjEntity();

            if (!Util.nullSafeEquals(ent.getSuperClassName(), parentClassText)) {
                ent.setSuperClassName(parentClassText);
                mediator.fireObjEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }
    }
}