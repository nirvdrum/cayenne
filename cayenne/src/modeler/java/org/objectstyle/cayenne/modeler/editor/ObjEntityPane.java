/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
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
    implements ObjEntityDisplayListener, ExistingSelectionProcessor {

    private static Logger logObj = Logger.getLogger(ObjEntityPane.class);

    protected EventController mediator;
    protected JTextField name;
    protected JTextField className;
    protected JTextField superClassName;
    protected JPanel dbPane;
    protected JComboBox dbName;
    protected JButton tableLabel;
    protected JCheckBox readOnly;

    public ObjEntityPane(EventController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = CayenneWidgetFactory.createTextField();
        superClassName = CayenneWidgetFactory.createTextField();
        className = CayenneWidgetFactory.createTextField();
        dbName = CayenneWidgetFactory.createComboBox();
        dbName.setRenderer(CellRenderers.listRendererWithIcons());

        readOnly = new JCheckBox();
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

    private void initController() {
        // initialize events processing and tracking of UI updates...

        mediator.addObjEntityDisplayListener(this);
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        className.setInputVerifier(inputCheck);
        superClassName.setInputVerifier(inputCheck);

        dbName.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Change DbEntity for current ObjEntity
                ObjEntity entity = mediator.getCurrentObjEntity();
                entity.setDbEntity((DbEntity) dbName.getSelectedItem());
                mediator.fireObjEntityEvent(new EntityEvent(this, entity));
            }
        });

        tableLabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Jump to DbEntity of the current ObjEntity
                DbEntity entity = mediator.getCurrentObjEntity().getDbEntity();
                if (entity != null) {
                    DataDomain dom = mediator.getCurrentDataDomain();
                    DataMap map = dom.getMapForDbEntity(entity.getName());
                    mediator.fireDbEntityDisplayEvent(
                        new EntityDisplayEvent(this, entity, map, dom));
                }
            }
        });

        readOnly.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setReadOnly(readOnly.isSelected());
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });
    }

    /**
     * Updates the view from the current model state.
     * Invoked when a currently displayed ObjEntity is changed.
     */
    private void initFromModel(ObjEntity entity) {
        name.setText(entity.getName());
        superClassName.setText(
            entity.getSuperClassName() != null ? entity.getSuperClassName() : "");
        className.setText(entity.getClassName() != null ? entity.getClassName() : "");
        readOnly.setSelected(entity.isReadOnly());

        DataMap map = mediator.getCurrentDataMap();
        Object[] entities = map.getDbEntities().toArray();
        Arrays.sort(entities, Comparators.getDataMapChildrenComparator());

        DefaultComboBoxModel model = new DefaultComboBoxModel(entities);
        model.setSelectedItem(entity.getDbEntity());
        dbName.setModel(model);
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

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        ObjEntity entity = (ObjEntity) e.getEntity();
        if (entity == null || !e.isEntityChanged()) {
            return;
        }

        initFromModel(entity);
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

            // "ent" may be null if we quit editing by changing tree selection
            if (ent != null && !Util.nullSafeEquals(ent.getClassName(), classText)) {
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

            if (ent != null && !Util.nullSafeEquals(ent.getSuperClassName(), parentClassText)) {
                ent.setSuperClassName(parentClassText);
                mediator.fireObjEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }
    }
}