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
package org.objectstyle.cayenne.modeler.datamap;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.AttributeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjAttributeDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjRelationshipDisplayListener;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;

/** 
 * Tabbed ObjEntity editor panel.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjDetailView
    extends JPanel
    implements
        ChangeListener,
        ObjEntityDisplayListener,
        ObjRelationshipDisplayListener,
        ObjAttributeDisplayListener {

    protected EventController mediator;
    protected JTabbedPane tab;
    protected ObjEntityPane entityPanel;
    protected ObjAttributePane attributesPanel;
    protected ObjRelationshipPane relationshipsPanel;

    public ObjDetailView(EventController mediator) {
        super();
        this.mediator = mediator;
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjAttributeDisplayListener(this);
        mediator.addObjRelationshipDisplayListener(this);

        setLayout(new BorderLayout());
        tab = new JTabbedPane();
        tab.setTabPlacement(JTabbedPane.TOP);
        add(tab, BorderLayout.CENTER);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables 
        // must be wrapped in a scroll pane

        entityPanel = new ObjEntityPane(mediator);
        tab.addTab("Entity", new JScrollPane(entityPanel));
        attributesPanel = new ObjAttributePane(mediator);
        tab.addTab("Attributes", attributesPanel);
        relationshipsPanel = new ObjRelationshipPane(mediator);
        tab.addTab("Relationships", relationshipsPanel);

        tab.addChangeListener(this);
    }

    /** 
     * Notifies a child that it is has been selected.
     */
    public void stateChanged(ChangeEvent e) {
        // find source view
        Component selected = tab.getSelectedComponent();
        while (selected instanceof JScrollPane) {
            selected = ((JScrollPane) selected).getViewport().getView();
        }

        ExistingSelectionProcessor proc = (ExistingSelectionProcessor) selected;
        proc.processExistingSelection();
    }

    /** If entity is null hides it's contents, otherwise makes it visible. */
    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getEntity() == null)
            tab.setVisible(false);
        else {
            if (e.isTabReset()) {
                tab.setSelectedIndex(0);
            }

            tab.setVisible(true);
        }
    }

    public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        Relationship rel = e.getRelationship();
        if (rel instanceof ObjRelationship) {
            relationshipsPanel.selectRelationship((ObjRelationship) rel);
        }

        // Display relationship tab
        tab.setSelectedIndex(2);
    }

    public void currentObjAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update relationship selection
        Attribute attr = e.getAttribute();
        if (attr instanceof ObjAttribute) {
            attributesPanel.selectAttribute((ObjAttribute) attr);
        }

        // Display attribute tab
        tab.setSelectedIndex(1);
    }
}