/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.GenerateDbController;
import org.objectstyle.cayenne.modeler.validator.ValidatorDialog;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.SwingView;

/**
 * View for DbEntity validation errors display. Used
 * as a warning dialog for DB schema generation.
 * 
 * @author Andrei Adamchik
 */

// TODO: This class should behave just like validation dialog,
// i.e. allow to click on errors and go to the problem location.
// Need some reusable widget for that.
public class DbEntityValidationDialog extends SPanel {

    public DbEntityValidationDialog() {
        init();
    }

    private void init() {
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Generate DB Schema: Validation Messages");
        setLayout(new BorderLayout());

        // build entity table
        STable table = new STable() {
            protected final Dimension preferredSize = new Dimension(450, 300);
            public Dimension getPreferredScrollableViewportSize() {
                return preferredSize;
            }
        };
        
        table.setBackground(ValidatorDialog.WARNING_COLOR);
        table.setRowHeight(25);
        table.setRowMargin(3);
        table.setColumnNames(new String[] { "Name", "Problems" });
        table.setColumnSelectors(new String[] { "validatedObject.name", "message" });

        // make sure that long columns are not squeezed
        table.getColumnModel().getColumn(0).setMinWidth(100);
        table.getColumnModel().getColumn(1).setMinWidth(350);

        // build action buttons
        SButton continueButton =
            new SButton(new SAction(GenerateDbController.GENERATION_OPTIONS_CONTROL));
        continueButton.setEnabled(true);

        SButton cancelButton =
            new SButton(new SAction(GenerateDbController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);

        // assemble
        JPanel panel = PanelFactory.createTablePanel(table, new JComponent[] {
        }, new JButton[] { continueButton, cancelButton });

        JEditorPane message =
            new JEditorPane(
                "text/html",
                "<center>Some tables can't be created due to validation problems below.<br> "
                    + "If \"Continue\" is selected, problematic tables will be skiped.</center>");
        message.setBackground(this.getBackground());
        message.setEditable(false);
        message.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(message, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
    }
}
