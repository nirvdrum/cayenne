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
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.JNDIDataSourceFactory;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.PreferenceField;
import org.objectstyle.cayenne.project.ProjectDataSource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Detail view of the DataNode and DataSourceInfo.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DataNodeView
    extends JPanel
    implements DocumentListener, ActionListener, DataNodeDisplayListener {
    private static Logger logObj = Logger.getLogger(DataNodeView.class);

    protected EventController mediator;
    protected DataNode node;

    protected JLabel nameLabel;
    protected JTextField name;
    protected String oldName;

    protected JLabel locationLabel;
    protected JTextField location;

    protected JLabel jndiLabel;
    protected JTextField jndiLocation;

    protected JLabel factoryLabel;
    protected JComboBox factory;

    protected JLabel adapterLabel;
    protected JComboBox adapter;

    protected JLabel userNameLabel;
    protected PreferenceField userName;
    protected JLabel passwordLabel;
    protected JPasswordField password;
    protected JLabel driverLabel;
    protected PreferenceField driver;
    protected JLabel urlLabel;
    protected PreferenceField url;
    protected JLabel minConnectionsLabel;

    // FIXME!!! Need to restrict only to numbers
    protected JTextField minConnections;
    protected JLabel maxConnectionsLabel;
    // FIXME!!! Need to restrict only to numbers
    protected JTextField maxConnections;

    protected JPanel driverPanel;

    /** Cludge to prevent marking domain as dirty during initial load. */
    private boolean ignoreChange;

    public DataNodeView(EventController temp_mediator) {
        super();
        mediator = temp_mediator;
        mediator.addDataNodeDisplayListener(this);
        // Create and layout components
        init();

        // Add listeners
        location.getDocument().addDocumentListener(this);
        name.getDocument().addDocumentListener(this);
        userName.getDocument().addDocumentListener(this);
        password.getDocument().addDocumentListener(this);
        driver.getDocument().addDocumentListener(this);
        url.getDocument().addDocumentListener(this);
        minConnections.getDocument().addDocumentListener(this);
        maxConnections.getDocument().addDocumentListener(this);
        factory.addActionListener(this);
        adapter.addActionListener(this);
    }

    protected void init() {
        // create widgets
        nameLabel = CayenneWidgetFactory.createLabel("DataNode Name:");
        name = CayenneWidgetFactory.createTextField();

        locationLabel = CayenneWidgetFactory.createLabel("Location:");
        location = CayenneWidgetFactory.createTextField();
        location.setEditable(false);

        factoryLabel = CayenneWidgetFactory.createLabel("DataSource Factory:");
        factory = CayenneWidgetFactory.createComboBox();
        factory.setEditable(true);
        DefaultComboBoxModel model =
            new DefaultComboBoxModel(
                new String[] {
                    JNDIDataSourceFactory.class.getName(),
                    DriverDataSourceFactory.class.getName()});
        factory.setModel(model);
        factory.setSelectedIndex(-1);

        adapterLabel = CayenneWidgetFactory.createLabel("DB Adapter:");
        adapter =
            CayenneWidgetFactory.createComboBox(
                DbAdapter.availableAdapterClassNames,
                false);
        adapter.setEditable(true);
        adapter.setSelectedIndex(-1);

        userNameLabel = CayenneWidgetFactory.createLabel("User Name:");
        userName =
            CayenneWidgetFactory.createPreferenceField(ModelerPreferences.USER_NAME);
        userName.addActionListener(this);
        passwordLabel = CayenneWidgetFactory.createLabel("Password:");
        password = new JPasswordField(20);
        driverLabel = CayenneWidgetFactory.createLabel("Driver Class:");
        driver =
            CayenneWidgetFactory.createPreferenceField(ModelerPreferences.JDBC_DRIVER);
        driver.addActionListener(this);
        urlLabel = CayenneWidgetFactory.createLabel("Database URL:");
        url = CayenneWidgetFactory.createPreferenceField(ModelerPreferences.DB_URL);
        url.addActionListener(this);
        minConnectionsLabel = CayenneWidgetFactory.createLabel("Min Connections:");
        minConnections = CayenneWidgetFactory.createTextField();
        maxConnectionsLabel = CayenneWidgetFactory.createLabel("Max Connections:");
        maxConnections = CayenneWidgetFactory.createTextField();

        // assemble
        this.setLayout(new BorderLayout());

        DefaultFormBuilder topPanelBuilder =
            new DefaultFormBuilder(
                new FormLayout("right:max(70dlu;pref), 3dlu, fill:max(200dlu;pref)", ""));
        topPanelBuilder.setDefaultDialogBorder();

        topPanelBuilder.appendSeparator("DataNode Configuration");
        topPanelBuilder.append(nameLabel, name);
        topPanelBuilder.append(factoryLabel, factory);
        topPanelBuilder.append(locationLabel, location);
        topPanelBuilder.append(adapterLabel, adapter);

        add(topPanelBuilder.getPanel(), BorderLayout.NORTH);

        DefaultFormBuilder driverPanelBuilder =
            new DefaultFormBuilder(
                new FormLayout("right:max(70dlu;pref), 3dlu, fill:max(200dlu;pref)", ""));
        driverPanelBuilder.setDefaultDialogBorder();

        driverPanelBuilder.appendSeparator("Data Source Info");
        driverPanelBuilder.append(userNameLabel, userName);
        driverPanelBuilder.append(passwordLabel, password);
        driverPanelBuilder.append(driverLabel, driver);
        driverPanelBuilder.append(urlLabel, url);
        driverPanelBuilder.append(minConnectionsLabel, minConnections);
        driverPanelBuilder.append(maxConnectionsLabel, maxConnections);

        driverPanel = driverPanelBuilder.getPanel();
        add(driverPanel, BorderLayout.CENTER);
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
        if (ignoreChange || node == null) {
            return;
        }

        ProjectDataSource src = (ProjectDataSource) node.getDataSource();
        DataSourceInfo info = src.getDataSourceInfo();

        if (e.getDocument() == name.getDocument()) {

            String newName = name.getText();
            // If name hasn't changed, do nothing
            if (oldName != null && oldName.equals(newName)) {
                return;
            }

            node.setName(newName);

            mediator.getCurrentDataDomain().removeDataNode(oldName);
            mediator.getCurrentDataDomain().addNode(node);

            mediator.fireDataNodeEvent(new DataNodeEvent(this, node, oldName));
            oldName = newName;

        }
        else if (e.getDocument() == location.getDocument()) {

            if (node.getDataSourceLocation() != null
                && node.getDataSourceLocation().equals(location.getText()))
                return;
            node.setDataSourceLocation(location.getText());
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == userName.getDocument()) {

            String nameStr =
                (userName.getText().trim().length() > 0)
                    ? userName.getText().trim()
                    : null;
            info.setUserName(nameStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == driver.getDocument()) {

            String driverStr =
                (driver.getText().trim().length() > 0) ? driver.getText().trim() : null;
            info.setJdbcDriver(driverStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == url.getDocument()) {

            String urlStr =
                (url.getText().trim().length() > 0) ? url.getText().trim() : null;
            info.setDataSourceUrl(urlStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == password.getDocument()) {

            char[] pwd = password.getPassword();
            String pwdStr = (pwd != null && pwd.length > 0) ? new String(pwd) : null;

            info.setPassword(pwdStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        }
        else if (e.getDocument() == minConnections.getDocument()) {

            if (minConnections.getText().trim().length() > 0)
                info.setMinConnections(Integer.parseInt(minConnections.getText()));
            else
                info.setMinConnections(0);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        }
        else if (e.getDocument() == maxConnections.getDocument()) {

            if (maxConnections.getText().trim().length() > 0)
                info.setMaxConnections(Integer.parseInt(maxConnections.getText()));
            else
                info.setMaxConnections(0);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        }

    }

    public void actionPerformed(ActionEvent e) {
        if (ignoreChange || node == null) {
            return;
        }

        Object src = e.getSource();
        DataNode aNode = mediator.getCurrentDataNode();

        // node factory changed
        if (src == factory) {
            String ele = (String) factory.getModel().getSelectedItem();
            if (ele != null && ele.trim().length() > 0) {
                if (ele.equals(DriverDataSourceFactory.class.getName())) {
                    location.setEditable(false);
                    showDiverInfo(true);
                }
                else {
                    location.setEditable(true);
                    showDiverInfo(false);
                }

                if (!ele.equals(aNode.getDataSourceFactory())) {
                    aNode.setDataSourceFactory(ele);
                    mediator.setDirty(true);
                }
            }
            else {
                if (aNode.getDataSourceFactory() != null) {
                    aNode.setDataSourceFactory(null);
                    mediator.setDirty(true);
                }
            }

        }
        else if (src == adapter) {
            // DBAdapter changed
            String adapterName = (String) adapter.getModel().getSelectedItem();

            // if (!Util.nullSafeEquals(currentName, adapterName)) {
            // instantiate new adapter if needed
            DbAdapter newAdapter = null;
            if (adapterName != null && adapterName.trim().length() > 0) {
                try {
                    newAdapter =
                        (DbAdapter) Class
                            .forName(adapterName)
                            .getDeclaredConstructors()[0]
                            .newInstance(
                            new Object[0]);
                }
                catch (Exception ex) {
                    logObj.warn("Error.", ex);
                    adapter.setSelectedIndex(-1);
                    return;
                }
            }

            mediator.getCurrentDataNode().setAdapter(newAdapter);
            mediator.setDirty(true);
            //   }
        }
        else if (src == driver) {
            ignoreChange = true;
            driver.storePreferences();
            ignoreChange = false;
        }
        else if (src == url) {
            ignoreChange = true;
            url.storePreferences();
            ignoreChange = false;
        }
        else if (src == userName) {
            ignoreChange = true;
            userName.storePreferences();
            ignoreChange = false;
        }
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        node = e.getDataNode();

        if (node == null) {
            return;
        }

        ProjectDataSource src = (ProjectDataSource) node.getDataSource();
        oldName = node.getName();
        ignoreChange = true;
        name.setText(oldName);
        populateFactory(node.getDataSourceFactory());
        DbAdapter adapter = node.getAdapter();
        if (adapter != null)
            populateDbAdapter(adapter.getClass().getName().trim());
        else
            populateDbAdapter("");
        DataSourceInfo info = src.getDataSourceInfo();
        populateDataSourceInfo(info);
        // Must be last in order not to be reset when data src factory is set.
        location.setText(node.getDataSourceLocation());
        ignoreChange = false;
    }

    private void populateDbAdapter(String selected_class) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) adapter.getModel();
        if (selected_class != null && selected_class.length() > 0) {
            boolean found = false;
            for (int i = 0; i < model.getSize(); i++) {
                String ele = (String) model.getElementAt(i);
                if (ele.equals(selected_class)) {
                    model.setSelectedItem(ele);
                    found = true;
                    break;
                }
            }

            if (!found) {
                model.addElement(selected_class);
                model.setSelectedItem(selected_class);
            }
        }
    }

    protected void populateFactory(String selected_class) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) factory.getModel();
        if (selected_class != null && selected_class.length() > 0) {
            boolean found = false;
            for (int i = 0; i < model.getSize(); i++) {
                String ele = (String) model.getElementAt(i);
                if (ele.equals(selected_class)) {
                    model.setSelectedItem(ele);
                    found = true;
                    // If direct connection, 
                    // show File button and disable text field.
                    // Otherwise hide File button and enable text field.
                    if (selected_class.equals(DriverDataSourceFactory.class.getName())) {
                        location.setEditable(false);
                        showDiverInfo(true);
                    }
                    else {
                        location.setEditable(true);
                        showDiverInfo(false);
                    }

                    break;
                }
            }

            if (!found) {
                model.addElement(selected_class);
                model.setSelectedItem(selected_class);
            }
        } // End if there is factory to select
        else
            model.setSelectedItem(null);
    }

    protected void populateDataSourceInfo(DataSourceInfo info) {
        userName.setText(info.getUserName());
        password.setText(info.getPassword());
        driver.setText(info.getJdbcDriver());
        url.setText(info.getDataSourceUrl());
        minConnections.setText(String.valueOf(info.getMinConnections()));
        maxConnections.setText(String.valueOf(info.getMaxConnections()));
    }

    protected void showDiverInfo(boolean show) {

        if (show && driverPanel.isVisible()) {
            ProjectDataSource src = (ProjectDataSource) node.getDataSource();
            populateDataSourceInfo(src.getDataSourceInfo());
        }
        driverPanel.setVisible(show);
    }
}
