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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.util.CayenneTextField;
import org.objectstyle.cayenne.modeler.util.MapUtil;

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

        parentEntities.addActionListener(this);
        parentLabel.addActionListener(this);
    }

    private void init() {
        setLayout(new BorderLayout());

        JLabel nameLabel = new JLabel("Entity name: ");
        name = new CayenneTextField(25);

        schemaLabel = new JLabel("Schema: ");
        schema = new CayenneTextField(25);

        parentLabel = PanelFactory.createLabelButton("Parent entity: ");
        parentLabel.setEnabled(false);
        parentEntities = new JComboBox();
        parentEntities.setEditable(false);
        parentEntities.setEnabled(false);

        Component[] leftCol =
            new Component[] { nameLabel, schemaLabel, parentLabel };

        Component[] rightCol = new Component[] { name, schema, parentEntities };

        add(
            PanelFactory.createForm(leftCol, rightCol, 5, 5, 5, 5),
            BorderLayout.NORTH);
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
            java.util.List ents = new ArrayList();
            ents.add("");

            Iterator it =
                mediator
                    .getCurrentDataMap()
                    .getDbEntitiesAsList(true)
                    .iterator();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();
                if (!(ent instanceof DerivedDbEntity)) {
                    ents.add(ent.getName());
                }
            }

            DefaultComboBoxModel model =
                new DefaultComboBoxModel(ents.toArray());
            DbEntity parent = ((DerivedDbEntity) entity).getParentEntity();
            if (parent != null) {
                model.setSelectedItem(parent.getName());
            }

            parentEntities.setModel(model);
        } else {
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
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
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

        } else if (parentLabel == e.getSource()) {
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
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            } else if (input == schema) {
                return verifySchema();
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
            DbEntity ent = mediator.getCurrentDbEntity();

            DbEntity matchingEnt = map.getDbEntity(text);

            if (matchingEnt == null) {
                // completely new name, set new name for entity
                EntityEvent e = new EntityEvent(this, ent, ent.getName());
                MapUtil.setDbEntityName(map, ent, text);
                mediator.fireDbEntityEvent(e);
                return true;
            } else if (matchingEnt == ent) {
                // no name changes, just return
                return true;
            } else {
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

            if (!org
                .objectstyle
                .cayenne
                .util
                .Util
                .nullSafeEquals(ent.getSchema(), text)) {

                ent.setSchema(text);
                mediator.fireDbEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }
    }
}