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

package org.objectstyle.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataRowStore;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.modeler.control.CacheSyncConfigController;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.modeler.validator.ValidatorDialog;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.util.Util;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Panel for editing DataDomain.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DomainDetailView extends JPanel implements DomainDisplayListener {
    private static Logger logObj = Logger.getLogger(DomainDetailView.class);

    protected EventController eventController;

    protected JTextField name;
    protected JTextField cacheSize;
    protected JCheckBox objectValidation;
    protected JCheckBox externalTransactions;
    protected JCheckBox sharedCache;
    protected JCheckBox remoteUpdates;
    protected JButton configRemoteUpdates;

    public DomainDetailView(EventController eventController) {
        this.eventController = eventController;

        // Create and layout components
        initView();

        // hook up listeners to widgets 
        initController();
    }

    protected void initView() {

        // create widgets
        this.name = CayenneWidgetFactory.createTextField();
        this.objectValidation = new JCheckBox();
        this.externalTransactions = new JCheckBox();
        this.cacheSize = CayenneWidgetFactory.createTextField(10);
        this.sharedCache = new JCheckBox();
        this.remoteUpdates = new JCheckBox();
        this.configRemoteUpdates = new JButton("Configure");
        configRemoteUpdates.setEnabled(false);

        // assemble
        this.setLayout(new BorderLayout());
        FormLayout layout =
            new FormLayout(
                "right:max(50dlu;pref), 3dlu, left:max(20dlu;pref), 3dlu, left:150",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataDomain Configuration");
        builder.append("DataDomain Name:", name, 3);
        builder.append("Child DataContexts Validate Objects:", objectValidation, 3);
        builder.append("Container-Managed Transactions:", externalTransactions, 3);

        builder.appendSeparator("Cache Configuration");
        builder.append("Max. Number of Objects:", cacheSize, 3);
        builder.append("Use Shared Cache:", sharedCache, 3);
        builder.append(
            "Remote Change Notifications:",
            remoteUpdates,
            configRemoteUpdates);

        this.add(builder.getPanel());
    }

    protected void initController() {
        eventController.addDomainDisplayListener(this);

        // set InputVeryfier for text fields
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        cacheSize.setInputVerifier(inputCheck);

        // add action listener to checkboxes
        objectValidation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = objectValidation.isSelected() ? "true" : "false";
                setDomainProperty(
                    DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                    value,
                    Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT));
            }
        });

        externalTransactions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = externalTransactions.isSelected() ? "true" : "false";
                setDomainProperty(
                    DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                    value,
                    Boolean.toString(DataDomain.USING_EXTERNAL_TRANSACTIONS_DEFAULT));
            }
        });

        sharedCache.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = sharedCache.isSelected() ? "true" : "false";
                setDomainProperty(
                    DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                    value,
                    Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT));

                // turning off shared cache should result in disabling remote events

                remoteUpdates.setEnabled(sharedCache.isSelected());

                if (!sharedCache.isSelected()) {
                    // uncheck remote updates... 
                    remoteUpdates.setSelected(false);
                }

                // depending on final remote updates status change button status
                configRemoteUpdates.setEnabled(remoteUpdates.isSelected());
            }
        });

        remoteUpdates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = remoteUpdates.isSelected() ? "true" : "false";

                // update config button state
                configRemoteUpdates.setEnabled(remoteUpdates.isSelected());

                setDomainProperty(
                    DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                    value,
                    Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT));
            }
        });

        configRemoteUpdates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new CacheSyncConfigController(eventController).startup();
            }
        });
    }

    /**
     * Helper method that updates domain properties. If a value equals to default, 
     * null value is used instead.
     */
    protected void setDomainProperty(
        String property,
        String value,
        String defaultValue) {

        DataDomain domain = eventController.getCurrentDataDomain();
        if (domain == null) {
            return;
        }

        // no empty strings
        if ("".equals(value)) {
            value = null;
        }

        // use NULL for defaults
        if (value != null && value.equals(defaultValue)) {
            value = null;
        }

        Map properties = domain.getProperties();
        Object oldValue = properties.get(property);
        if (!Util.nullSafeEquals(value, oldValue)) {
            properties.put(property, value);

            DomainEvent e = new DomainEvent(this, domain);
            eventController.fireDomainEvent(e);
        }
    }

    public String getDomainProperty(String property, String defaultValue) {
        DataDomain domain = eventController.getCurrentDataDomain();
        if (domain == null) {
            return null;
        }

        String value = (String) domain.getProperties().get(property);
        return value != null ? value : defaultValue;
    }

    public boolean getDomainBooleanProperty(String property, String defaultValue) {
        return "true".equalsIgnoreCase(getDomainProperty(property, defaultValue));
    }

    /**
     * Invoked on domain selection event. Updates view with
     * the values from the currently selected domain.
     */
    public void currentDomainChanged(DomainDisplayEvent e) {
        DataDomain domain = e.getDomain();
        if (null == domain) {
            return;
        }

        // extract values from the new domain object
        name.setText(domain.getName());

        cacheSize.setText(
            getDomainProperty(
                DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY,
                Integer.toString(DataRowStore.SNAPSHOT_CACHE_SIZE_DEFAULT)));

        objectValidation.setSelected(
            getDomainBooleanProperty(
                DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT)));

        externalTransactions.setSelected(
            getDomainBooleanProperty(
                DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                Boolean.toString(DataDomain.USING_EXTERNAL_TRANSACTIONS_DEFAULT)));

        sharedCache.setSelected(
            getDomainBooleanProperty(
                DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT)));

        remoteUpdates.setSelected(
            getDomainBooleanProperty(
                DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT)));
        remoteUpdates.setEnabled(sharedCache.isSelected());
        configRemoteUpdates.setEnabled(
            remoteUpdates.isEnabled() && remoteUpdates.isSelected());
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else if (input == cacheSize) {
                return verifyCacheSize();
            }
            else {
                return true;
            }
        }

        protected boolean verifyName() {
            String text = name.getText();
            if (text == null || text.trim().length() == 0) {
                text = "";
            }

            Configuration configuration =
                ((ApplicationProject) CayenneModelerFrame.getProject())
                    .getConfiguration();
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

        protected boolean verifyCacheSize() {
            String text = cacheSize.getText().trim();

            if (text.length() > 0) {
                try {
                    Integer.parseInt(text);
                }
                catch (NumberFormatException ex) {
                    return validationWarning(cacheSize);
                }
            }

            setDomainProperty(
                DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY,
                text,
                Integer.toString(DataRowStore.SNAPSHOT_CACHE_SIZE_DEFAULT));
            return validationSuccess(cacheSize);
        }

        protected boolean validationWarning(JTextField field) {
            field.setBackground(ValidatorDialog.WARNING_COLOR);
            return false;
        }

        protected boolean validationSuccess(JTextField field) {
            field.setBackground(Color.WHITE);
            return true;
        }
    }
}