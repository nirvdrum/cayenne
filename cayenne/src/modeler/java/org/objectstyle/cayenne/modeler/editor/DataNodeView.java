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
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.JNDIDataSourceFactory;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.DbAdapterInfo;
import org.objectstyle.cayenne.modeler.util.PreferenceField;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextFieldAdapter;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.project.ProjectDataSource;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel for DataNode configuration.
 */
public class DataNodeView extends JPanel implements DocumentListener {

    protected EventController mediator;
    protected DataNode node;

    protected TextFieldAdapter name;

    protected JTextField location;
    protected JComboBox factory;
    protected JComboBox adapter;

    protected PreferenceField userName;
    protected JPasswordField password;
    protected PreferenceField driver;
    protected PreferenceField url;

    protected JTextField minConnections;
    protected JTextField maxConnections;

    protected JPanel driverPanel;

    // Cludge to prevent marking domain as dirty during initial load.
    private boolean ignoreChange;

    public DataNodeView(EventController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initView() {
        // create widgets

        name = new TextFieldAdapter(CayenneWidgetFactory.createTextField()) {

            protected void initModel(String text) {
                setDataNodeName(text);
            }
        };

        location = CayenneWidgetFactory.createTextField();
        location.setEditable(false);

        factory = CayenneWidgetFactory.createComboBox();
        factory.setEditable(true);

        adapter = CayenneWidgetFactory.createComboBox();
        adapter.setEditable(true);

        userName = CayenneWidgetFactory
                .createPreferenceField(ModelerPreferences.USER_NAME);

        password = new JPasswordField(20);
        driver = CayenneWidgetFactory
                .createPreferenceField(ModelerPreferences.JDBC_DRIVER);

        url = CayenneWidgetFactory.createPreferenceField(ModelerPreferences.DB_URL);

        minConnections = CayenneWidgetFactory.createTextField();
        maxConnections = CayenneWidgetFactory.createTextField();

        // assemble

        DefaultFormBuilder topPanelBuilder = new DefaultFormBuilder(new FormLayout(
                "right:max(70dlu;pref), 3dlu, fill:200dlu",
                ""));
        topPanelBuilder.setDefaultDialogBorder();

        topPanelBuilder.appendSeparator("DataNode Configuration");
        topPanelBuilder.append("DataNode Name:", name.getTextComponent());
        topPanelBuilder.append("DataSource Factory", factory);
        topPanelBuilder.append("Location:", location);
        topPanelBuilder.append("DB Adapter:", adapter);

        DefaultFormBuilder driverPanelBuilder = new DefaultFormBuilder(new FormLayout(
                "right:max(70dlu;pref), 3dlu, fill:200dlu",
                ""));
        driverPanelBuilder.setDefaultDialogBorder();

        driverPanelBuilder.appendSeparator("Data Source Info");
        driverPanelBuilder.append("User Name:", userName);
        driverPanelBuilder.append("Password:", password);
        driverPanelBuilder.append("Driver Class:", driver);
        driverPanelBuilder.append("Database URL:", url);
        driverPanelBuilder.append("Min. Connections:", minConnections);
        driverPanelBuilder.append("Max. Connections:", maxConnections);

        setLayout(new BorderLayout());
        add(topPanelBuilder.getPanel(), BorderLayout.NORTH);

        driverPanel = driverPanelBuilder.getPanel();
        add(driverPanel, BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addDataNodeDisplayListener(new DataNodeDisplayListener() {

            public void currentDataNodeChanged(DataNodeDisplayEvent e) {
                DataNode node = e.getDataNode();
                if (node != null) {
                    initFromModel(node);
                }
            }
        });

        location.getDocument().addDocumentListener(this);
        userName.getDocument().addDocumentListener(this);
        password.getDocument().addDocumentListener(this);
        driver.getDocument().addDocumentListener(this);
        url.getDocument().addDocumentListener(this);
        minConnections.getDocument().addDocumentListener(this);
        maxConnections.getDocument().addDocumentListener(this);

        factory.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setFactoryName((String) factory.getModel().getSelectedItem());
            }
        });

        adapter.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setAdapterName((String) adapter.getModel().getSelectedItem());
            }
        });

        userName.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ignoreChange = true;
                userName.storePreferences();
                ignoreChange = false;
            }
        });

        driver.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ignoreChange = true;
                driver.storePreferences();
                ignoreChange = false;
            }
        });
        url.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ignoreChange = true;
                url.storePreferences();
                ignoreChange = false;
            }
        });

    }

    private void initFromModel(DataNode node) {
        if (this.node == node) {
            return;
        }

        this.node = node;

        ignoreChange = true;

        name.setText(node.getName());

        initFactory(node.getDataSourceFactory());
        initForFactoryType(node.getDataSourceFactory());

        DbAdapter adapter = node.getAdapter();
        initDbAdapter(adapter != null ? adapter.getClass().getName().trim() : null);

        ProjectDataSource src = (ProjectDataSource) node.getDataSource();
        DataSourceInfo info = src.getDataSourceInfo();
        initDataSourceInfo(info);

        // Must be last in order not to be reset when data src factory is set.
        location.setText(node.getDataSourceLocation());

        ignoreChange = false;
    }

    private void initForFactoryType(String factoryName) {
        boolean showDriverInfo = DriverDataSourceFactory.class.getName().equals(
                factoryName);
        boolean makeLocationEditable = JNDIDataSourceFactory.class.getName().equals(
                factoryName);

        location.setEditable(makeLocationEditable);

        if (showDriverInfo) {
            ProjectDataSource src = (ProjectDataSource) node.getDataSource();
            initDataSourceInfo(src.getDataSourceInfo());
        }

        driverPanel.setVisible(showDriverInfo);
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

        if (e.getDocument() == location.getDocument()) {

            if (node.getDataSourceLocation() != null
                    && node.getDataSourceLocation().equals(location.getText()))
                return;
            node.setDataSourceLocation(location.getText());
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == userName.getDocument()) {

            String nameStr = (userName.getText().trim().length() > 0) ? userName
                    .getText()
                    .trim() : null;
            info.setUserName(nameStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == driver.getDocument()) {

            String driverStr = (driver.getText().trim().length() > 0) ? driver
                    .getText()
                    .trim() : null;
            info.setJdbcDriver(driverStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        }
        else if (e.getDocument() == url.getDocument()) {

            String urlStr = (url.getText().trim().length() > 0)
                    ? url.getText().trim()
                    : null;
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

    private void setDataNodeName(String newName) {
        if (newName == null || newName.trim().length() == 0) {
            throw new ValidationException("Enter name for DataNode");
        }

        DataNode node = mediator.getCurrentDataNode();

        if (node == null || newName.equals(node.getName())) {
            return;
        }

        // search for matching node name across domains, as currently they have to be
        // unique globally
        Configuration config = ((ApplicationProject) Application.getProject())
                .getConfiguration();

        DataNode matchingNode = null;

        Iterator it = config.getDomains().iterator();
        while (it.hasNext()) {
            DataDomain domain = (DataDomain) it.next();
            DataNode nextNode = domain.getNode(newName);

            if (nextNode == node) {
                continue;
            }

            if (nextNode != null) {
                matchingNode = nextNode;
                break;
            }
        }

        if (matchingNode != null) {
            // there is an entity with the same name
            throw new ValidationException("There is another DataNode named '"
                    + newName
                    + "'. Use a different name.");
        }

        // completely new name, set new name
        DataNodeEvent e = new DataNodeEvent(this, node, node.getName());
        ProjectUtil.setDataNodeName(mediator.getCurrentDataDomain(), node, newName);
        mediator.fireDataNodeEvent(e);
    }

    private void setFactoryName(String factoryName) {
        DataNode node = mediator.getCurrentDataNode();

        if (node == null) {
            return;
        }

        if (factoryName != null && factoryName.trim().length() == 0) {
            factoryName = null;
        }

        if (Util.nullSafeEquals(factoryName, node.getDataSourceFactory())) {
            return;
        }

        node.setDataSourceFactory(factoryName);
        mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        initForFactoryType(factoryName);
    }

    private void setAdapterName(String adapterName) {
        DataNode node = mediator.getCurrentDataNode();

        if (node == null) {
            return;
        }

        DbAdapter newAdapter = null;
        if (adapterName != null && adapterName.trim().length() > 0) {
            try {
                Class adapterClass = ModelerClassLoader.getClassLoader().loadClass(
                        adapterName);
                newAdapter = (DbAdapter) adapterClass.newInstance();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane
                        .showMessageDialog(
                                Application.getFrame(),
                                ex.getMessage(),
                                "Error loading adapter",
                                JOptionPane.ERROR_MESSAGE);
                DbAdapter oldAdapter = node.getAdapter();
                initDbAdapter(oldAdapter != null
                        ? oldAdapter.getClass().getName().trim()
                        : null);
                return;
            }
        }

        node.setAdapter(newAdapter);
        mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
    }

    private void initDbAdapter(String adapterClass) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(DbAdapterInfo
                .getStandardAdapters());

        if (adapterClass != null && adapterClass.trim().length() > 0) {
            boolean found = false;
            for (int i = 0; i < model.getSize(); i++) {
                String adapter = (String) model.getElementAt(i);
                if (adapter.equals(adapterClass)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                model.addElement(adapterClass);
            }

            model.setSelectedItem(adapterClass);
        }
        else {
            // needed to display an empty field with no selection
            model.setSelectedItem("");
        }

        adapter.setModel(model);
    }

    private void initFactory(String factoryName) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] {
                DriverDataSourceFactory.class.getName(),
                JNDIDataSourceFactory.class.getName(),
                "Some stuff...",
                "sadsad sad asd 234",
                "sadsad sad asd 324",
                "sadsad sad asd 324234",
                "sadsad sad asd 324",
                "sadsad sad asd 234234",
                "sadsad sad asd 2344234",
                "sadsad sad asd 3434",
                "sadsad sad asd 3434",
                "sadsad sad asd ",
                "sadsad sad sadasd "
        });

        if (factoryName != null && factoryName.length() > 0) {
            boolean found = false;
            for (int i = 0; i < model.getSize(); i++) {
                String factory = (String) model.getElementAt(i);
                if (factory.equals(factoryName)) {
                    found = true;
                    break;
                }
            }

            // custom factory...
            if (!found) {
                model.addElement(factoryName);
            }

            model.setSelectedItem(factoryName);
        }
        else {
            // needed to display an empty field with no selection
            model.setSelectedItem("");
        }

        factory.setModel(model);
    }

    private void initDataSourceInfo(DataSourceInfo info) {
        userName.setText(info.getUserName());
        password.setText(info.getPassword());
        driver.setText(info.getJdbcDriver());
        url.setText(info.getDataSourceUrl());
        minConnections.setText(String.valueOf(info.getMinConnections()));
        maxConnections.setText(String.valueOf(info.getMaxConnections()));
    }
}