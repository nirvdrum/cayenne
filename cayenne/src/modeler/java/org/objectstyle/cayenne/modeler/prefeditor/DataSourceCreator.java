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
package org.objectstyle.cayenne.modeler.prefeditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.objectstyle.cayenne.dba.db2.DB2Adapter;
import org.objectstyle.cayenne.dba.firebird.FirebirdAdapter;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.objectstyle.cayenne.dba.mysql.MySQLAdapter;
import org.objectstyle.cayenne.dba.openbase.OpenBaseAdapter;
import org.objectstyle.cayenne.dba.oracle.OracleAdapter;
import org.objectstyle.cayenne.dba.postgres.PostgresAdapter;
import org.objectstyle.cayenne.dba.sqlserver.SQLServerAdapter;
import org.objectstyle.cayenne.dba.sybase.SybaseAdapter;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.swing.CayenneController;
import org.objectstyle.cayenne.modeler.util.DbAdapterInfo;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;

/**
 * @author Andrei Adamchik
 */
public class DataSourceCreator extends CayenneController {

    private static final String NO_ADAPTER = "Custom / Undefined";

    // these should probably be a part of adapter-associated metadata..
    private static final Map defaultDrivers = new HashMap();
    private static final Map defaultUrls = new HashMap();

    static {
        defaultDrivers.put(
                OracleAdapter.class.getName(),
                "oracle.jdbc.driver.OracleDriver");
        defaultDrivers.put(
                SybaseAdapter.class.getName(),
                "com.sybase.jdbc2.jdbc.SybDriver");
        defaultDrivers.put(MySQLAdapter.class.getName(), "com.mysql.jdbc.Driver");
        defaultDrivers.put(DB2Adapter.class.getName(), "com.ibm.db2.jcc.DB2Driver");
        defaultDrivers.put(HSQLDBAdapter.class.getName(), "org.hsqldb.jdbcDriver");
        defaultDrivers.put(PostgresAdapter.class.getName(), "org.postgresql.Driver");
        defaultDrivers.put(
                FirebirdAdapter.class.getName(),
                "org.firebirdsql.jdbc.FBDriver");
        defaultDrivers.put(OpenBaseAdapter.class.getName(), "com.openbase.jdbc.ObDriver");
        defaultDrivers.put(
                SQLServerAdapter.class.getName(),
                "com.microsoft.jdbc.sqlserver.SQLServerDriver");

        defaultUrls.put(
                OracleAdapter.class.getName(),
                "jdbc:oracle:thin:@host:1521:database");
        defaultUrls.put(
                SybaseAdapter.class.getName(),
                "jdbc:sybase:Tds:host:port/database");
        defaultUrls.put(MySQLAdapter.class.getName(), "jdbc:mysql://host/database");
        defaultUrls.put(DB2Adapter.class.getName(), "jdbc:db2://host:port/database");
        defaultUrls
                .put(HSQLDBAdapter.class.getName(), "jdbc:hsqldb:hsql://host/database");
        defaultUrls.put(
                PostgresAdapter.class.getName(),
                "jdbc:postgresql://host:5432/database");
        defaultUrls.put(
                FirebirdAdapter.class.getName(),
                "jdbc:firebirdsql:host/port:/path/to/file.gdb");
        defaultUrls.put(OpenBaseAdapter.class.getName(), "jdbc:openbase://host/database");
        defaultUrls
                .put(
                        SQLServerAdapter.class.getName(),
                        "jdbc:microsoft:sqlserver://host;databaseName=database;SelectMethod=cursor");
    }

    protected DataSourceCreatorView view;
    protected PreferenceEditor editor;
    protected Domain domain;
    protected boolean canceled;
    protected Map dataSources;

    public DataSourceCreator(DataSourcePreferences parentController, Map dataSources) {
        super(parentController);
        this.view = new DataSourceCreatorView();
        this.editor = parentController.getEditor();
        this.domain = parentController.getDataSourceDomain();
        this.dataSources = dataSources;

        DefaultComboBoxModel model = new DefaultComboBoxModel(DbAdapterInfo
                .getStandardAdapters());
        model.insertElementAt(NO_ADAPTER, 0);
        this.view.getAdapters().setModel(model);
        this.view.getAdapters().setSelectedIndex(0);

        String suggestion = "DataSource0";
        for (int i = 1; i <= dataSources.size(); i++) {
            suggestion = "DataSource" + i;
            if (!dataSources.containsKey(suggestion)) {
                break;
            }
        }

        this.view.getDataSourceName().setText(suggestion);
        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelAction();
            }
        });

        view.getOkButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                okAction();
            }
        });

    }

    public void okAction() {
        if (getName() == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "Enter DataSource Name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else if (dataSources.containsKey(getName())) {
            JOptionPane.showMessageDialog(
                    view,
                    "'" + getName() + "' is already in use, enter a different name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else {
            canceled = false;
            view.dispose();
        }
    }

    public void cancelAction() {
        canceled = true;
        view.dispose();
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    public DBConnectionInfo startupAction() {
        // this should handle closing via ESC
        canceled = true;

        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();

        view.show();
        return createDataSource();
    }

    public String getName() {
        String name = view.getDataSourceName().getText();
        return (name.length() > 0) ? name : null;
    }

    protected DBConnectionInfo createDataSource() {
        if (canceled) {
            return null;
        }

        DBConnectionInfo dataSource = (DBConnectionInfo) editor.createDetail(
                domain,
                getName(),
                DBConnectionInfo.class);

        Object adapter = view.getAdapters().getSelectedItem();
        if (NO_ADAPTER.equals(adapter)) {
            adapter = null;
        }

        if (adapter != null) {
            String adapterString = adapter.toString();
            dataSource.setDbAdapter(adapterString);

            // guess adapter defaults...
            dataSource.setJdbcDriver((String) defaultDrivers.get(adapterString));
            dataSource.setUrl((String) defaultUrls.get(adapterString));
        }

        return dataSource;
    }
}