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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.MapObject;
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
import org.objectstyle.cayenne.util.XMLEncoder;

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

    private static final Object noInheritance =
        new MapObject("Direct Mapping to Table/View") {
        public void encodeAsXML(XMLEncoder encoder) {
        }
    };

    protected EventController mediator;
    protected JTextField name;
    protected JTextField className;
    protected JTextField superClassName;
    protected JComboBox dbEntityCombo;
    protected JComboBox superEntityCombo;
    protected JButton tableLabel;
    protected JCheckBox readOnly;
    protected JCheckBox optimisticLocking;

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

        dbEntityCombo = CayenneWidgetFactory.createComboBox();
        dbEntityCombo.setRenderer(CellRenderers.listRendererWithIcons());

        superEntityCombo = CayenneWidgetFactory.createComboBox();
        superEntityCombo.setRenderer(CellRenderers.listRendererWithIcons());

        readOnly = new JCheckBox();
        optimisticLocking = new JCheckBox();

        tableLabel = CayenneWidgetFactory.createLabelButton("Table/View:");

        // assemble
        setLayout(new BorderLayout());
        FormLayout layout =
            new FormLayout("right:max(50dlu;pref), 3dlu, fill:max(170dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("ObjEntity Configuration");
        builder.append("ObjEntity Name:", name);
        builder.append("Inheritance:", superEntityCombo);
        builder.append(tableLabel, dbEntityCombo);

        builder.appendSeparator();

        builder.append("Java Class:", className);
        builder.append("Superclass:", superClassName);
        builder.append("Read-Only:", readOnly);
        builder.append("Optimistic Locking:", optimisticLocking);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        // initialize events processing and tracking of UI updates...

        mediator.addObjEntityDisplayListener(this);
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        className.setInputVerifier(inputCheck);
        superClassName.setInputVerifier(inputCheck);

        dbEntityCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Change DbEntity for current ObjEntity
                ObjEntity entity = mediator.getCurrentObjEntity();
                DbEntity dbEntity = (DbEntity) dbEntityCombo.getSelectedItem();

                if (dbEntity != entity.getDbEntity()) {
                    entity.setDbEntity(dbEntity);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        superEntityCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Change super-entity
                MapObject superEntity = (MapObject) superEntityCombo.getSelectedItem();
                String name =
                    (superEntity == noInheritance) ? null : superEntity.getName();

                ObjEntity entity = mediator.getCurrentObjEntity();

                if (!Util.nullSafeEquals(name, entity.getSuperEntityName())) {
                    entity.setSuperEntityName(name);

                    // if a super-entity selected, disable table selection
                    // and also update parent DbEntity selection...
                    activateFields(name == null);
                    dbEntityCombo.getModel().setSelectedItem(entity.getDbEntity());
                    superClassName.setText(entity.getSuperClassName());

                    // fire both EntityEvent and EntityDisplayEvent;
                    // the later is to update attribute and relationship display

                    DataDomain domain = mediator.getCurrentDataDomain();
                    DataMap map = mediator.getCurrentDataMap();

                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                    mediator.fireObjEntityDisplayEvent(
                        new EntityDisplayEvent(this, entity, map, domain));
                }
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

        optimisticLocking.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setLockType(
                        optimisticLocking.isSelected()
                            ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                            : ObjEntity.LOCK_TYPE_NONE);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });
    }

    /**
     * Updates the view from the current model state.
     * Invoked when a currently displayed ObjEntity is changed.
     */
    private void initFromModel(final ObjEntity entity) {
        name.setText(entity.getName());
        superClassName.setText(
            entity.getSuperClassName() != null ? entity.getSuperClassName() : "");
        className.setText(entity.getClassName() != null ? entity.getClassName() : "");
        readOnly.setSelected(entity.isReadOnly());

        // TODO: fix inheritance - we should allow to select optimistic
        // lock if superclass is not already locked, 
        // otherwise we must keep this checked in but not editable.
        optimisticLocking.setSelected(
            entity.getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC);

        // init DbEntities 
        DataMap map = mediator.getCurrentDataMap();
        Object[] dbEntities = map.getDbEntities().toArray();
        Arrays.sort(dbEntities, Comparators.getDataMapChildrenComparator());

        DefaultComboBoxModel dbModel = new DefaultComboBoxModel(dbEntities);
        dbModel.setSelectedItem(entity.getDbEntity());
        dbEntityCombo.setModel(dbModel);

        // if a super-entity selected, disable table selection
        activateFields(entity.getSuperEntityName() == null);

        // init ObjEntities for inheritance
        Predicate inheritanceFilter = new Predicate() {
            public boolean evaluate(Object object) {
                    // do not show this entity or any of the subentities
    if (entity == object) {
                    return false;
                }

                if (object instanceof ObjEntity) {
                    return !((ObjEntity) object).isSubentityOf(entity);
                }

                return false;
            }
        };

        Object[] objEntities =
            CollectionUtils.select(map.getObjEntities(), inheritanceFilter).toArray();
        Arrays.sort(objEntities, Comparators.getDataMapChildrenComparator());
        Object[] finalObjEntities = new Object[objEntities.length + 1];
        finalObjEntities[0] = noInheritance;
        System.arraycopy(objEntities, 0, finalObjEntities, 1, objEntities.length);

        DefaultComboBoxModel superEntityModel =
            new DefaultComboBoxModel(finalObjEntities);
        superEntityModel.setSelectedItem(entity.getSuperEntity());
        superEntityCombo.setModel(superEntityModel);
    }

    private void activateFields(boolean active) {
        superClassName.setEnabled(active);
        superClassName.setEditable(active);
        dbEntityCombo.setEnabled(active);
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

            if (ent != null
                && !Util.nullSafeEquals(ent.getSuperClassName(), parentClassText)) {
                ent.setSuperClassName(parentClassText);
                mediator.fireObjEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }
    }
}