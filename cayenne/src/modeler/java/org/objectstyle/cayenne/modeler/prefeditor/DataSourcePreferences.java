package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.conn.DriverDataSource;
import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.swing.CayenneController;
import org.objectstyle.cayenne.modeler.util.DbAdapterInfo;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;
import org.objectstyle.cayenne.pref.PreferenceException;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DataSourcePreferences extends CayenneController {

    protected DataSourcePreferencesView view;
    protected PreferenceEditor editor;
    protected DBConnectionInfo currentDataSource;
    protected Map dataSources;
    protected Map bindings;

    public DataSourcePreferences(PreferenceDialog parentController) {
        super(parentController);

        this.view = new DataSourcePreferencesView();
        this.editor = parentController.getEditor();
        this.bindings = new HashMap();
        this.dataSources = new HashMap();

        // init view data
        Collection sources = getDataSourceDomain().getPreferenceDetails(
                DBConnectionInfo.class);

        int len = sources.size();
        Object[] keys = new Object[len + 1];
        keys[0] = "";
        Iterator it = sources.iterator();
        for (int i = 1; i <= len; i++) {
            DBConnectionInfo info = (DBConnectionInfo) it.next();
            keys[i] = info.getKey();
            dataSources.put(keys[i], info);
        }

        Arrays.sort(keys);
        view.getDataSources().setModel(new DefaultComboBoxModel(keys));

        DefaultComboBoxModel model = new DefaultComboBoxModel(DbAdapterInfo
                .getStandardAdapters());
        model.insertElementAt("", 0);
        view.getDataSourceEditor().getAdapters().setModel(model);
        view.getDataSourceEditor().getAdapters().setSelectedIndex(0);

        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        view.getAddDataSource().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newDataSourceAction();
            }
        });

        view.getRemoveDataSource().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeDataSourceAction();
            }
        });

        view.getTestDataSource().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                testDataSourceAction();
            }
        });

        view.getDataSources().addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                editDataSourceAction();
            }
        });

        DocumentDataObjectBinding urlBinding = new DocumentDataObjectBinding("url");
        urlBinding.attach(view.getDataSourceEditor().getUrl());
        bindings.put(urlBinding.property, urlBinding);

        DocumentDataObjectBinding passwordBinding = new DocumentDataObjectBinding(
                "password");
        passwordBinding.attach(view.getDataSourceEditor().getPassword());
        bindings.put(passwordBinding.property, passwordBinding);

        DocumentDataObjectBinding userNameBinding = new DocumentDataObjectBinding(
                "userName");
        userNameBinding.attach(view.getDataSourceEditor().getUserName());
        bindings.put(userNameBinding.property, userNameBinding);

        DocumentDataObjectBinding driverBinding = new DocumentDataObjectBinding(
                "jdbcDriver");
        driverBinding.attach(view.getDataSourceEditor().getDriver());
        bindings.put(driverBinding.property, driverBinding);

        ComboDataObjectBinding adapterBinding = new ComboDataObjectBinding("dbAdapter");
        adapterBinding.attach(view.getDataSourceEditor().getAdapters());
        bindings.put(adapterBinding.property, adapterBinding);
    }

    public Domain getDataSourceDomain() {
        return editor.editableInstance(getApplication().getApplicationPreferences());
    }

    public PreferenceEditor getEditor() {
        return editor;
    }

    public void newDataSourceAction() {

        DataSourceCreator creatorWizard = new DataSourceCreator(this, dataSources);
        DBConnectionInfo dataSource = creatorWizard.startupAction();

        if (dataSource != null) {
            dataSources.put(creatorWizard.getName(), dataSource);

            Object[] keys = dataSources.keySet().toArray();
            Arrays.sort(keys);
            view.getDataSources().setModel(new DefaultComboBoxModel(keys));
            view.getDataSources().setSelectedItem(creatorWizard.getName());
            editDataSourceAction();
        }
    }

    public void removeDataSourceAction() {
        Object selected = view.getDataSources().getSelectedItem();
        if (selected != null) {
            editor.deleteDetail(getDataSourceDomain(), selected.toString());
            dataSources.remove(selected);
            Object[] keys = dataSources.keySet().toArray();
            Arrays.sort(keys);
            view.getDataSources().setModel(new DefaultComboBoxModel(keys));
            view.getDataSources().setSelectedItem("");
            editDataSourceAction();
        }
    }

    public void editDataSourceAction() {
        Object selected = view.getDataSources().getSelectedItem();
        currentDataSource = (DBConnectionInfo) dataSources.get(selected);
        DataSourceEditorView subview = view.getDataSourceEditor();

        // update bindings
        Iterator it = bindings.values().iterator();
        while (it.hasNext()) {
            DataObjectBinding binding = (DataObjectBinding) it.next();
            binding.setObject(currentDataSource);
        }

        subview.setEnabled(currentDataSource != null);
    }

    public void testDataSourceAction() {
        if (currentDataSource == null) {
            return;
        }

        if (currentDataSource.getJdbcDriver() == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "No JDBC Driver specified",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentDataSource.getUrl() == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "No Database URL specified",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Class driverClass = ModelerClassLoader.getClassLoader().loadClass(
                    currentDataSource.getJdbcDriver());
            Driver driver = (Driver) driverClass.newInstance();

            // connect via Cayenne DriverDataSource - it addresses some driver issues...
            Connection c = new DriverDataSource(
                    driver,
                    currentDataSource.getUrl(),
                    currentDataSource.getUserName(),
                    currentDataSource.getPassword()).getConnection();
            try {
                c.close();
            }
            catch (SQLException e) {
                // i guess we can ignore this...
            }

            JOptionPane.showMessageDialog(
                    null,
                    "Connected Successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            JOptionPane.showMessageDialog(null, "Error connecting to DB: "
                    + th.getLocalizedMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    abstract class DataObjectBinding {

        DataObject object;
        String property;
        boolean disableUpdate;

        DataObjectBinding(String property) {
            this.property = property;
        }

        void setObject(DataObject object) {
            this.object = object;

            this.disableUpdate = true;

            try {
                updateView(object != null ? object.readProperty(property) : null);
            }
            finally {
                disableUpdate = false;
            }
        }

        abstract void updateView(Object value);
    }

    class ComboDataObjectBinding extends DataObjectBinding implements ItemListener {

        JComboBox comboBox;

        ComboDataObjectBinding(String property) {
            super(property);
        }

        void updateView(Object value) {
            if (value != null) {
                this.comboBox.setSelectedItem(value.toString());
            }
            else {
                this.comboBox.setSelectedIndex(-1);
            }
        }

        void attach(JComboBox comboBox) {
            this.comboBox = comboBox;
            comboBox.addItemListener(this);
        }

        public void itemStateChanged(ItemEvent e) {
            if (disableUpdate || object == null) {
                return;
            }

            Object item = e.getItem();
            object.writeProperty(property, item);
        }
    }

    class DocumentDataObjectBinding extends DataObjectBinding implements DocumentListener {

        JTextComponent textField;

        DocumentDataObjectBinding(String property) {
            super(property);
        }

        void updateView(Object value) {
            this.textField.setText(value != null ? value.toString() : null);
        }

        void attach(JTextComponent textField) {
            textField.getDocument().addDocumentListener(this);
            this.textField = textField;
        }

        public void changedUpdate(DocumentEvent e) {
            writeChange(e);
        }

        public void insertUpdate(DocumentEvent e) {
            writeChange(e);
        }

        public void removeUpdate(DocumentEvent e) {
            writeChange(e);
        }

        void writeChange(DocumentEvent e) {
            if (disableUpdate || object == null) {
                return;
            }

            try {
                String text = e.getDocument().getText(0, e.getDocument().getLength());
                object.writeProperty(property, text.length() > 0 ? text : null);
            }
            catch (BadLocationException ex) {
                throw new PreferenceException("Invalid text location", ex);
            }
        }
    }

}