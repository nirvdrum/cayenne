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

import javax.swing.JButton;
import javax.swing.JScrollPane;

import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.control.ConfigureClasspathController;
import org.objectstyle.cayenne.modeler.model.ConfigureClasspathModel;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.STableModel;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.extras.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class ConfigureClasspathDialog extends SPanel {

    public ConfigureClasspathDialog() {
        init();
    }

    private void init() {
        // create widgets 
        SButton saveButton =
            new SButton(new SAction(ConfigureClasspathController.SAVE_CONTROL));
        saveButton.setEnabled(true);

        SButton cancelButton =
            new SButton(new SAction(ConfigureClasspathController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);

        SButton addJarButton =
            new SButton(new SAction(ConfigureClasspathController.ADDJAR_CONTROL));
        addJarButton.setEnabled(true);

        SButton addDirButton =
            new SButton(new SAction(ConfigureClasspathController.ADDDIR_CONTROL));
        addDirButton.setEnabled(true);

        SButton removeEntryButton =
            new SButton(new SAction(ConfigureClasspathController.REMOVE_CONTROL));
        removeEntryButton.setEnabled(true);

        STable table = new ConfigureClasspathTable();
        table.setSelectionSelector(ConfigureClasspathModel.SELECTED_ENTRY_SELECTOR);
        table.setRowMargin(3);

        STableModel model = new STableModel(table);
        model.setSelector(ConfigureClasspathModel.CUSTOM_CLASSPATH_SELECTOR);
        model.setColumnNames(new String[] { "Custom Classpath" });
        model.setColumnSelectors(new String[] { "absolutePath" });

        table.setModel(model);

        // assemble

        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Configure CayenneModeler ClassPath");
        setLayout(new BorderLayout());

        add(new JScrollPane(table), BorderLayout.CENTER);

        FormLayout layout = new FormLayout("fill:min(150dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append(addJarButton);
        builder.append(addDirButton);
        builder.append(removeEntryButton);
        add(builder.getPanel(), BorderLayout.EAST);

        add(
            PanelFactory.createButtonPanel(new JButton[] { saveButton, cancelButton }),
            BorderLayout.SOUTH);
    }

    class ConfigureClasspathTable extends STable {
        final Dimension preferredSize = new Dimension(500, 300);

        ConfigureClasspathTable() {
            setRowHeight(25);
            setRowMargin(3);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }
}
