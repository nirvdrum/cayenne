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

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.event.ProcedureEvent;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class ProcedureDetailView extends JPanel implements ProcedureDisplayListener {
    protected EventController eventController;
    protected JTextField name;
    protected JTextField schema;

    public ProcedureDetailView(EventController eventController) {

        this.eventController = eventController;

        init();

        eventController.addProcedureDisplayListener(this);
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        schema.setInputVerifier(inputCheck);
    }

    protected void init() {
        this.setLayout(new BorderLayout());
        this.name = CayenneWidgetFactory.createTextField();
        this.schema = CayenneWidgetFactory.createTextField();

        this.add(
            PanelFactory.createForm(
                new Component[] {
                    CayenneWidgetFactory.createLabel("Procedure name: "),
                    CayenneWidgetFactory.createLabel("Schema: ")},
                new Component[] { name, schema },
                5,
                5,
                5,
                5));
    }

    /**
     * Invoked when currently selected Procedure object is changed.
     */
    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure == null || !e.isProcedureChanged()) {
            return;
        }

        name.setText(procedure.getName());
        schema.setText(procedure.getSchema());
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else if (input == schema) {
                return verifySchema();
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

            DataMap map = eventController.getCurrentDataMap();
            Procedure procedure = eventController.getCurrentProcedure();

            Procedure matchingProcedure = map.getProcedure(text);

            if (matchingProcedure == null) {
                // completely new name, set new name for entity
                ProcedureEvent e =
                    new ProcedureEvent(this, procedure, procedure.getName());
                MapUtil.setProcedureName(map, procedure, text);
                eventController.fireProcedureEvent(e);
                return true;
            }
            else if (matchingProcedure == procedure) {
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

            Procedure procedure = eventController.getCurrentProcedure();

            if (!Util.nullSafeEquals(procedure.getSchema(), text)) {
                procedure.setSchema(text);
                eventController.fireProcedureEvent(new ProcedureEvent(this, procedure));
            }

            return true;
        }

    }
}
