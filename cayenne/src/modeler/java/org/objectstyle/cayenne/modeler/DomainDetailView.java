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

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.project.ApplicationProject;

/** 
 * Panel for editing DataDomain.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DomainDetailView extends JPanel implements DomainDisplayListener {
    protected EventController eventController;
    protected JTextField name;

    public DomainDetailView(EventController eventController) {
        this.eventController = eventController;

        // Create and layout components
        init();

        eventController.addDomainDisplayListener(this);
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
    }

    private void init() {
        this.setLayout(new BorderLayout());
        this.name = CayenneWidgetFactory.createTextField();
        this.add(
            PanelFactory.createForm(
                new Component[] { CayenneWidgetFactory.createLabel("Domain name: ")},
                new Component[] { name },
                5,
                5,
                5,
                5));
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        DataDomain domain = e.getDomain();
        if (null == domain) {
            return;
        }

        name.setText(domain.getName());
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else {
                return true;
            }
        }

        protected boolean verifyName() {
            String text = name.getText();
            if (text != null && text.trim().length() == 0) {
                text = null;
            }

            Configuration configuration =
                ((ApplicationProject) Editor.getProject()).getConfiguration();
            DataDomain domain = eventController.getCurrentDataDomain();

            DataDomain matchingDomain = configuration.getDomain(text);

            if (matchingDomain == null) {
                // completely new name, set new name for domain
                DomainEvent e = new DomainEvent(this, domain, domain.getName());
                MapUtil.setDataDomainName(configuration, domain, text);
                eventController.fireDomainEvent(e);
                return true;
            }
            else if (matchingDomain == domain) {
                // no name changes, just return
                return true;
            }
            else {
                // there is an entity with the same name
                return false;
            }
        }
    }
}