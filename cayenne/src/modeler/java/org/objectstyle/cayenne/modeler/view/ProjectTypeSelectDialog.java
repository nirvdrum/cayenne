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
package org.objectstyle.cayenne.modeler.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.objectstyle.cayenne.modeler.util.ModelerStrings;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SwingView;

/**
 * Wizard for selecting a type for the new project.
 * 
 * @author Andrei Adamchik
 */
public class ProjectTypeSelectDialog extends SPanel {

    /**
     * Constructor for ProjectTypeSelectView.
     */
    public ProjectTypeSelectDialog() {
        init();
    }

    /**
     * Lays out the dialog.
     */
    protected void init() {
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle(ModelerUtil.buildTitle("New Project"));

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));
        mainPanel.add(buildProjectPanel("cayenne.modeler.project.app", false));
        mainPanel.add(buildProjectPanel("cayenne.modeler.project.map", false));

        add(mainPanel, BorderLayout.CENTER);
        add(
            buildButtonPanel("cayenne.modeler.project.cancel", false),
            BorderLayout.SOUTH);
    }

    /**
     * Creates a subpanel for a project type. If <code>disabled</code> is
     * true, panel is rendered inactive, so that a project of this type
     * could not be created.
     */
    protected JPanel buildProjectPanel(String propsPrefix, boolean disabled) {
        String name = ModelerStrings.getString(propsPrefix + ".name");
        String desc = ModelerStrings.getString(propsPrefix + ".desc");

        JPanel panel = new JPanel(new BorderLayout());
        Border border = BorderFactory.createEmptyBorder(5, 3, 3, 3);
        panel.setBorder(BorderFactory.createTitledBorder(border, name));

        // set description
        JTextArea descArea = new JTextArea(5, 50);
        descArea.setText(desc);
        descArea.setFont(descArea.getFont().deriveFont((float)11));
        descArea.setEditable(false);
        descArea.setEnabled(!disabled);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(panel.getBackground());

        JScrollPane scroll =
            new JScrollPane(
                descArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scroll, BorderLayout.CENTER);

        // add button
        panel.add(buildButtonPanel(propsPrefix, disabled), BorderLayout.SOUTH);

        return panel;
    }

    protected JPanel buildButtonPanel(String propsPrefix, boolean disabled) {
        SAction action = new SAction(propsPrefix + ".button");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        SButton button = new SButton(action);
        // button.setFont(button.getFont().deriveFont(Font.PLAIN, 12));
        button.setEnabled(!disabled);
        buttonPanel.add(button);

        return buttonPanel;
    }
}
