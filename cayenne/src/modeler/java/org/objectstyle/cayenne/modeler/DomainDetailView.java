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

package org.objectstyle.cayenne.modeler;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.event.DomainEvent;

/** 
 * Detail view of the Data Domain
 * 
 * @author Michael Misha Shengaout 
 */
public class DomainDetailView extends JPanel 
implements DocumentListener, DomainDisplayListener
{
	EventController mediator;
	
	JLabel		nameLabel;
	JTextField	name;
	String		oldName;
	/** Cludge to prevent marking domain as dirty during initial load. */
	private boolean ignoreChange = false;
	
	public DomainDetailView(EventController temp_mediator) {
		super();		
		mediator = temp_mediator;
		mediator.addDomainDisplayListener(this);
		// Create and layout components
		init();
		// Add listeners
		name.getDocument().addDocumentListener(this);
	}

	private void init(){
		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		nameLabel 	= new JLabel("Domain name: ");
		name 		= new JTextField(20);

		Component[] left_comp = new Component[1];
		left_comp[0] = nameLabel;
		Component[] right_comp = new Component[1];
		right_comp[0] = name;
		JPanel temp = PanelFactory.createForm(left_comp, right_comp, 5,5,5,5);
		Spring pad = Spring.constant(5);
		Spring ySpring = pad;
		add(temp);
		SpringLayout.Constraints cons = layout.getConstraints(temp);
		cons.setY(ySpring);
		cons.setX(pad);
	}

	public void insertUpdate(DocumentEvent e)  { textFieldChanged(e); }
	public void changedUpdate(DocumentEvent e) { textFieldChanged(e); }
	public void removeUpdate(DocumentEvent e)  { textFieldChanged(e); }

	private void textFieldChanged(DocumentEvent e) {
		if (ignoreChange)
			return;
		String new_name = name.getText();
		DataDomain domain = mediator.getCurrentDataDomain();
		// If name hasn't changed, do nothing
		if (new_name.equals(domain.getName()))
			return;
		domain.setName(new_name);
		DomainEvent event;
		event = new DomainEvent(this, domain, oldName);
		mediator.fireDomainEvent(event);
		oldName = new_name;

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