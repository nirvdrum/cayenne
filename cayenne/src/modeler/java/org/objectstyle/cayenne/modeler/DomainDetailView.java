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
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.control.CacheSyncConfigController;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.modeler.validator.ValidatorDialog;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.util.Util;

import com.jgoodies.forms.extras.DefaultFormBuilder;
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
    protected JTextField cacheExpiration;
    protected JCheckBox localUpdates;
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
        this.setLayout(new BorderLayout());

        this.name = CayenneWidgetFactory.createTextField();
        this.cacheSize = CayenneWidgetFactory.createTextField(10);
        this.cacheExpiration = CayenneWidgetFactory.createTextField(10);
        this.localUpdates = new JCheckBox();
        this.remoteUpdates = new JCheckBox();
        this.configRemoteUpdates = new JButton("Configure");
        configRemoteUpdates.setEnabled(false);

        FormLayout layout = new FormLayout("right:max(50dlu;pref), 3dlu, left:max(20dlu;pref), 3dlu, left:150", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        
        builder.appendSeparator("DataDomain Info");
        builder.append("DataDomain Name:", name, 3);
        
        builder.appendSeparator("Cache Configuration");
        builder.append("Max. Number of Objects:", cacheSize, 3);
        builder.append("Entry Expiration, sec.:", cacheExpiration, 3);
        builder.append("Local Change Notifications:", localUpdates, 3);
        builder.append("Remote Change Notifications:", remoteUpdates, configRemoteUpdates);

        this.add(builder.getPanel());
    }

    protected void initController() {
        eventController.addDomainDisplayListener(this);

        // set InputVeryfier for text fields
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
        cacheSize.setInputVerifier(inputCheck);
        cacheExpiration.setInputVerifier(inputCheck);

        // add action listener to checkboxes
        localUpdates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = localUpdates.isSelected() ? "true" : "false";
                setDomainProperty(
                    DataRowStore.OBJECT_STORE_NOTIFICATION_PROPERTY,
                    value,
                    Boolean.toString(DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT));
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
               new CacheSyncConfigController().startup();
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

        cacheExpiration.setText(
            getDomainProperty(
                DataRowStore.SNAPSHOT_EXPIRATION_PROPERTY,
                Long.toString(DataRowStore.SNAPSHOT_EXPIRATION_DEFAULT)));

        localUpdates.setSelected(
            getDomainBooleanProperty(
                DataRowStore.OBJECT_STORE_NOTIFICATION_PROPERTY,
                Boolean.toString(DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT)));

        remoteUpdates.setSelected(
            getDomainBooleanProperty(
                DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT)));
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else if (input == cacheSize) {
                return verifyCacheSize();
            }
            else if (input == cacheExpiration) {
                return verifyCacheExpiration();
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

        protected boolean verifyCacheExpiration() {
            String text = cacheExpiration.getText().trim();

            if (text.length() > 0) {
                try {
                    Integer.parseInt(text);
                }
                catch (NumberFormatException ex) {
                    return validationWarning(cacheExpiration);
                }
            }

            setDomainProperty(
                DataRowStore.SNAPSHOT_EXPIRATION_PROPERTY,
                text,
                Long.toString(DataRowStore.SNAPSHOT_EXPIRATION_DEFAULT));
            return validationSuccess(cacheExpiration);
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