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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.AttributeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DbAttributeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbRelationshipDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;

public class DbDetailView
    extends JPanel
    implements
        ChangeListener,
        DbEntityDisplayListener,
        DbRelationshipDisplayListener,
        DbAttributeDisplayListener {

    protected EventController mediator;
    protected JTabbedPane tab;
    protected DbEntityPane entityPanel;
    protected DbAttributePane attributesPanel;
    protected DbRelationshipPane relationshipsPanel;

    public DbDetailView(EventController mediator) {
        super();
        this.mediator = mediator;
        mediator.addDbEntityDisplayListener(this);
        mediator.addDbAttributeDisplayListener(this);
        mediator.addDbRelationshipDisplayListener(this);

        setLayout(new BorderLayout());
        tab = new JTabbedPane();
        tab.setTabPlacement(JTabbedPane.TOP);
        add(tab, BorderLayout.CENTER);
        entityPanel = new DbEntityPane(mediator);
        tab.addTab("Entity", entityPanel);
        attributesPanel = new DbAttributePane(mediator);
        tab.addTab("Attributes", attributesPanel);
        relationshipsPanel = new DbRelationshipPane(mediator);
        tab.addTab("Relationships", relationshipsPanel);

        tab.addChangeListener(this);
    }

    /** Handle focus when tab changes. */
    public void stateChanged(ChangeEvent e) {
        ExistingSelectionProcessor proc;
        proc = (ExistingSelectionProcessor) tab.getSelectedComponent();
        proc.processExistingSelection();
    }

    /** If entity is null hides it's contents, otherwise makes it visible. */
    public void currentDbEntityChanged(EntityDisplayEvent e) {
        if (e.getEntity() == null)
            tab.setVisible(false);
        else {
            if (e.isTabReset())
                tab.setSelectedIndex(0);
            tab.setVisible(true);
        }
    }

    public void currentDbRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        Relationship rel = e.getRelationship();
        if (rel instanceof DbRelationship) {
            relationshipsPanel.selectRelationship((DbRelationship) rel);
        }

        // Display relationship tab
        tab.setSelectedIndex(2);
    }

    public void currentDbAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;
            
        // update relationship selection
        Attribute attr = e.getAttribute();
        if (attr instanceof DbAttribute) {
            attributesPanel.selectAttribute((DbAttribute) attr);
        }
        
        
        // Display attribute tab
        tab.setSelectedIndex(1);
    }
}