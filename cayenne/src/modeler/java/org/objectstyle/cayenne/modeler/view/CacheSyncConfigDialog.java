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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import org.objectstyle.cayenne.modeler.control.CacheSyncConfigController;
import org.objectstyle.cayenne.modeler.model.CacheSyncTypesModel;
import org.scopemvc.core.Control;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.extras.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class CacheSyncConfigDialog extends SPanel {
    public static final String EMPTY_CARD_KEY = "Empty";

    protected JPanel configPanel;

    public CacheSyncConfigDialog() {
        initView();
    }

    protected void initView() {
        setDisplayMode(SwingView.MODAL_DIALOG);
        this.setLayout(new BorderLayout());
        this.setTitle("Configure Remote Cache Synchronization");

        SComboBox type = new SComboBox();
        type.setSelector(CacheSyncTypesModel.NOTIFICATION_TYPES_SELECTOR);
        type.setSelectionSelector(CacheSyncTypesModel.FACTORY_LABEL_SELECTOR);

        SButton saveButton = new SButton(CacheSyncConfigController.SAVE_CONFIG_CONTROL);
        SButton cancelButton =
            new SButton(CacheSyncConfigController.CANCEL_CONFIG_CONTROL);

        // buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // type form
        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Notification Transport Type:", type);

        // config panel
        configPanel = new JPanel(new CardLayout());
        addCard(new JPanel(), EMPTY_CARD_KEY);

        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(configPanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);

        showCard(EMPTY_CARD_KEY);
    }

    public Control getCloseControl() {
        return new Control(CacheSyncConfigController.CANCEL_CONFIG_CONTROL);
    }

    public void addCard(Component card, String key) {
        configPanel.add(card, key);
    }

    public void showCard(String key) {
        ((CardLayout) configPanel.getLayout()).show(configPanel, key);
    }
}
