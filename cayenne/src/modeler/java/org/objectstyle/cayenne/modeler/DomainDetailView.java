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

package org.objectstyle.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.util.Util;

/** 
 * Detail view of the Data Domain
 * 
 * @author Michael Misha Shengaout 
 */
public class DomainDetailView
    extends JPanel
    implements DocumentListener, DomainDisplayListener {

    protected EventController mediator;
    protected JTextField name;
    protected String oldName;

    /** Cludge to prevent marking domain as dirty during initial load. */
    private boolean ignoreChange = false;

    public DomainDetailView(EventController mediator) {
        super();
        this.mediator = mediator;
        mediator.addDomainDisplayListener(this);
        // Create and layout components
        init();
        // Add listeners
        name.getDocument().addDocumentListener(this);
    }

    private void init() {
        this.setLayout(new BorderLayout());
        this.name = new JTextField(25);
        this.add(
            PanelFactory.createForm(
                new Component[] { new JLabel("Domain name: ")},
                new Component[] { name },
                5,
                5,
                5,
                5));
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
        if (ignoreChange) {
            return;
        }

        DataDomain domain = mediator.getCurrentDataDomain();
        String newName = name.getText();
        String aName = domain.getName();

        // If name hasn't changed, do nothing
        if (Util.nullSafeEquals(newName, aName)) {
            return;
        }

        domain.setName(newName);

        Project project = Editor.getProject();
        if (project instanceof ApplicationProject) {
            ((ApplicationProject) project).getConfiguration().removeDomain(aName);
            ((ApplicationProject) project).getConfiguration().addDomain(domain);
        }

        DomainEvent event = new DomainEvent(this, domain, aName);
        mediator.fireDomainEvent(event);
        oldName = newName;
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        DataDomain domain = e.getDomain();
        if (null == domain)
            return;
        oldName = domain.getName();
        ignoreChange = true;
        name.setText(oldName);
        ignoreChange = false;
    }
}