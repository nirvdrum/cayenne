/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbKeyGenerator;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.util.Util;

/** 
 * Detail view of the DbEntity properties. 
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DbEntityPane
    extends JPanel
    implements ExistingSelectionProcessor, DbEntityDisplayListener, ActionListener {

    protected EventController mediator;

    protected JTextField name;
    protected JTextField schema;
    protected JComboBox parentEntities;
    protected JButton parentLabel;
    protected JLabel schemaLabel;

    protected JCheckBox customPKGenerator;
    protected JLabel customPKGeneratorLabel;
    protected JLabel customPKGeneratorNote;
    protected JLabel customPKGeneratorNameLabel;
    protected JLabel customPKSizeLabel;
    protected JTextField customPKName;
    protected JTextField customPKSize;

    /** 
     * Cludge to prevent marking data map as dirty 
     * during initial load. 
     */
    private boolean ignoreChange;

    public DbEntityPane(EventController mediator) {
        super();
        this.mediator = mediator;
        mediator.addDbEntityDisplayListener(this);

        // Create and layout components
        init();

        // Add listeners
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        schema.setInputVerifier(inputCheck);
        customPKName.setInputVerifier(inputCheck);
        customPKSize.setInputVerifier(inputCheck);

        parentEntities.addActionListener(this);
        parentLabel.addActionListener(this);
        customPKGenerator.addActionListener(this);
    }

    private void init() {
        setLayout(new BorderLayout());

        JLabel nameLabel = CayenneWidgetFactory.createLabel("DbEntity name: ");
        name = CayenneWidgetFactory.createTextField();

        schemaLabel = CayenneWidgetFactory.createLabel("Schema: ");
        schema = CayenneWidgetFactory.createTextField();

        parentLabel = CayenneWidgetFactory.createLabelButton("Parent DbEntity: ");
        parentLabel.setEnabled(false);
        parentEntities = CayenneWidgetFactory.createComboBox();
        parentEntities.setEditable(false);
        parentEntities.setEnabled(false);

        Component[] leftCol = new Component[] { nameLabel, schemaLabel, parentLabel };
        Component[] rightCol = new Component[] { name, schema, parentEntities };
        add(PanelFactory.createForm(leftCol, rightCol, 5, 5, 5, 5), BorderLayout.NORTH);

        JPanel pkGeneratorPanel = new JPanel(new BorderLayout());

        customPKGeneratorLabel =
            CayenneWidgetFactory.createLabel("Customize primary key generation");
        customPKGeneratorNote =
            CayenneWidgetFactory.createLabel(
                "(currently ignored by all adapters except Oracle)");
        customPKGeneratorNote.setFont(customPKGeneratorNote.getFont().deriveFont(10));

        customPKGenerator = new JCheckBox();

        leftCol = new Component[] { customPKGenerator, new JLabel()};
        rightCol = new Component[] { customPKGeneratorLabel, customPKGeneratorNote };
        pkGeneratorPanel.add(
            PanelFactory.createForm(leftCol, rightCol, 5, 5, 5, 5),
            BorderLayout.NORTH);

        customPKGeneratorNameLabel = CayenneWidgetFactory.createLabel("Database object name: ");
        customPKSizeLabel = CayenneWidgetFactory.createLabel("Cached PK Size: ");

        customPKName = CayenneWidgetFactory.createTextField();
        customPKSize = CayenneWidgetFactory.createTextField();

        leftCol = new Component[] { customPKGeneratorNameLabel, customPKSizeLabel };
        rightCol = new Component[] { customPKName, customPKSize };
        pkGeneratorPanel.add(
            PanelFactory.createForm(leftCol, rightCol, 5, 5, 5, 5),
            BorderLayout.CENTER);

        add(pkGeneratorPanel, BorderLayout.CENTER);
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
        schema.setText(entity.getSchema());
        ignoreChange = false;

        if (entity instanceof DerivedDbEntity) {
            updateState(true);

            // build a list consisting of non-derived entities
            java.util.List ents = new ArrayList(64);
            ents.add("");

            Iterator it = mediator.getCurrentDataMap().getDbEntities(true).iterator();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();
                if (!(ent instanceof DerivedDbEntity)) {
                    ents.add(ent.getName());
                }
            }

            DefaultComboBoxModel model = new DefaultComboBoxModel(ents.toArray());
            DbEntity parent = ((DerivedDbEntity) entity).getParentEntity();
            if (parent != null) {
                model.setSelectedItem(parent.getName());
            }

            parentEntities.setModel(model);
        }
        else {
            updateState(false);
            parentEntities.setSelectedIndex(-1);
        }
    }

    /**
     * Enables or disbales form fields depending on the
     * type of entity shown.
     */
    protected void updateState(boolean isDerivedEntity) {
        schemaLabel.setEnabled(!isDerivedEntity);
        schema.setEnabled(!isDerivedEntity);

        parentLabel.setEnabled(isDerivedEntity);
        parentEntities.setEnabled(isDerivedEntity);
        parentLabel.setVisible(isDerivedEntity);
        parentEntities.setVisible(isDerivedEntity);

        DbEntity entity = mediator.getCurrentDbEntity();
        updatePrimaryKeyGeneratorView(entity);
    }

    protected void updatePrimaryKeyGeneratorView(DbEntity entity) {
        DbKeyGenerator generator = entity.getPrimaryKeyGenerator();
        boolean isPKGeneratorCustomized = generator != null;

        customPKGenerator.setSelected(isPKGeneratorCustomized);

        customPKGeneratorNameLabel.setEnabled(isPKGeneratorCustomized);
        customPKSizeLabel.setEnabled(isPKGeneratorCustomized);

        customPKName.setEnabled(isPKGeneratorCustomized);
        customPKSize.setEnabled(isPKGeneratorCustomized);

        if (isPKGeneratorCustomized) {
            customPKName.setText(generator.getGeneratorName());
            customPKSize.setText(
                generator.getKeyCacheSize() != null
                    ? generator.getKeyCacheSize().toString()
                    : "0");
        }
        else {
            customPKName.setText("");
            customPKSize.setText("");
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == parentEntities) {
            DbEntity current = mediator.getCurrentDbEntity();

            if (current instanceof DerivedDbEntity) {
                DerivedDbEntity derived = (DerivedDbEntity) current;
                String name = (String) parentEntities.getSelectedItem();

                DbEntity ent =
                    (name != null && name.trim().length() > 0)
                        ? mediator.getCurrentDataMap().getDbEntity(name, true)
                        : null;

                if (ent != null && ent != derived.getParentEntity()) {
                    derived.setParentEntity(ent);
                    derived.resetToParentView();
                    MapUtil.cleanObjMappings(mediator.getCurrentDataMap());

                    EntityEvent event = new EntityEvent(this, current);
                    mediator.fireDbEntityEvent(event);
                }
            }

        }
        else if (parentLabel == e.getSource()) {
            DbEntity current = mediator.getCurrentDbEntity();

            if (current instanceof DerivedDbEntity) {
                DbEntity parent = ((DerivedDbEntity) current).getParentEntity();
                if (parent != null) {
                    DataDomain dom = mediator.getCurrentDataDomain();
                    DataMap map = dom.getMapForDbEntity(parent.getName());
                    mediator.fireDbEntityDisplayEvent(
                        new EntityDisplayEvent(this, parent, map, dom));
                }
            }
        }
        else if (customPKGenerator == e.getSource()) {
            DbEntity entity = mediator.getCurrentDbEntity();
            if (entity == null) {
                return;
            }

            if (customPKGenerator.isSelected()) {
                if (entity.getPrimaryKeyGenerator() == null) {
                    DbKeyGenerator generator = new DbKeyGenerator();
                    generator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
                    generator.setKeyCacheSize(
                        new Integer(JdbcPkGenerator.DEFAULT_PK_CACHE_SIZE));
                    entity.setPrimaryKeyGenerator(generator);
                }

                updatePrimaryKeyGeneratorView(entity);
            }
            else {
                entity.setPrimaryKeyGenerator(null);
                updatePrimaryKeyGeneratorView(entity);
            }
        }
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else if (input == schema) {
                return verifySchema();
            }
            else if (input == customPKSize) {
                return verifyCustomPKSize();
            }
            else if (input == customPKName) {
                return verifyCustomPKName();
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
            DbEntity ent = mediator.getCurrentDbEntity();

            DbEntity matchingEnt = map.getDbEntity(text);

            if (matchingEnt == null) {
                // completely new name, set new name for entity
                EntityEvent e = new EntityEvent(this, ent, ent.getName());
                MapUtil.setDbEntityName(map, ent, text);
                mediator.fireDbEntityEvent(e);
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

        protected boolean verifySchema() {
            String text = schema.getText();
            if (text != null && text.trim().length() == 0) {
                text = null;
            }

            DbEntity ent = mediator.getCurrentDbEntity();

            if (!Util.nullSafeEquals(ent.getSchema(), text)) {
                ent.setSchema(text);
                mediator.fireDbEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }

        protected boolean verifyCustomPKSize() {
            String text = customPKSize.getText();
            int cacheSize = 0;

            if (text != null && text.trim().length() > 0) {
                try {
                    cacheSize = Integer.parseInt(text);
                }
                catch (NumberFormatException nfex) {
                }
            }

            // erase any incorrect input
            if (cacheSize == 0) {
                customPKSize.setText("0");
            }

            DbEntity entity = mediator.getCurrentDbEntity();
            DbKeyGenerator generator = entity.getPrimaryKeyGenerator();

            if (generator != null
                && (generator.getKeyCacheSize() == null
                    || generator.getKeyCacheSize().intValue() != cacheSize)) {
                generator.setKeyCacheSize(new Integer(cacheSize));
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }

            return true;
        }

        protected boolean verifyCustomPKName() {
            String text = customPKName.getText();
            if (text != null && text.trim().length() == 0) {
                text = null;
            }

            DbEntity entity = mediator.getCurrentDbEntity();
            DbKeyGenerator generator = entity.getPrimaryKeyGenerator();

            if (generator != null && (!Util.nullSafeEquals(text, generator.getName()))) {
                generator.setGeneratorName(text);
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }
            return true;
        }
    }
}