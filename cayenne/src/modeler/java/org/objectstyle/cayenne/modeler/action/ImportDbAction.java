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
package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbLoader;
import org.objectstyle.cayenne.access.DbLoaderDelegate;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.DriverDataSource;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.InteractiveLogin;
import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.dialog.db.ChooseSchemaDialog;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.util.YesNoToAllDialog;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.project.ProjectDataSource;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * Action that imports database structure into a DataMap.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ImportDbAction extends CayenneAction {

    private static final Logger logObj = Logger.getLogger(ImportDbAction.class);

    // TODO: this is a temp hack... need to delegate to DbAdapter, or configurable in
    // preferences...
    private static final Collection excludedTables = Arrays.asList(new Object[] {
            "AUTO_PK_SUPPORT", "auto_pk_support"
    });

    public static String getActionName() {
        return "Reengineer Database Schema";
    }

    public ImportDbAction() {
        super(getActionName());
    }

    public void importDb() {
        EventController mediator = getMediator();
        DataNode currentNode = mediator.getCurrentDataNode();

        // try a node that belongs to the current DataMap ...
        if (currentNode == null) {
            DataMap map = mediator.getCurrentDataMap();
            if (map != null) {
                currentNode = mediator.getCurrentDataDomain().lookupDataNode(map);
            }
        }

        DataSourceInfo dsi = null;
        if (currentNode != null) {
            dsi = ((ProjectDataSource) currentNode.getDataSource())
                    .getDataSourceInfo()
                    .cloneInfo();
            if (currentNode.getAdapter() != null) {
                dsi.setAdapterClassName(currentNode.getAdapter().getClass().getName());
            }
        }
        else {
            dsi = new DataSourceInfo();
        }

        Connection conn = null;
        DbAdapter adapter = null;

        // Get connection
        while (conn == null) {
            InteractiveLogin loginObj = InteractiveLogin.getGuiLoginObject(dsi);
            loginObj.collectLoginInfo();

            // run login panel
            dsi = loginObj.getDataSrcInfo();
            if (dsi == null) {
                return;
            }

            // load adapter
            adapter = createAdapter(dsi);
            if (adapter == null) {
                continue;
            }

            // open connection
            conn = openConnection(dsi);
            if (conn == null) {
                continue;
            }
        }

        try {
            LoaderDelegate delegate = new LoaderDelegate(dsi, mediator);
            DbLoader loader = new DbLoader(conn, adapter, delegate);
            List schemas = loadSchemas(loader);
            if (schemas == null) {
                return;
            }

            ChooseSchemaDialog dialog = new ChooseSchemaDialog(schemas, dsi.getUserName());
            dialog.show();
            if (dialog.getChoice() == ChooseSchemaDialog.CANCEL) {
                return;
            }

            String schemaName = dialog.getSelectedSchema();
            String tableNamePattern = dialog.getTableNamePattern();
            dialog.dispose();

            DataMap map = loadMap(loader, schemaName, tableNamePattern);
            if (map != null) {
                processMapUpdate(map);
            }
        }
        finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            }
            catch (SQLException e) {
                logObj.warn("Error closing connection.", e);
            }
        }
    }

    public DbAdapter createAdapter(DataSourceInfo dsi) {
        // load adapter
        try {
            Class adapterClass = ModelerClassLoader.getClassLoader().loadClass(
                    dsi.getAdapterClassName());

            return (DbAdapter) adapterClass.newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    CayenneModelerFrame.getFrame(),
                    e.getMessage(),
                    "Error loading adapter",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public Connection openConnection(DataSourceInfo dsi) {
        String driverClassName = dsi.getJdbcDriver();

        try {
            // load driver via custom ClassLoader
            Class driverClass = ModelerClassLoader.getClassLoader().loadClass(
                    driverClassName);
            Driver driver = (Driver) driverClass.newInstance();

            // connect via Cayenne DriverDataSource - it addresses some driver issues...
            return new DriverDataSource(
                    driver,
                    dsi.getDataSourceUrl(),
                    dsi.getUserName(),
                    dsi.getPassword()).getConnection();
        }
        catch (SQLException e) {
            logObj.warn("Can't open database connection.", e);
            SQLException ex = e.getNextException();
            if (ex != null) {
                logObj.warn("Nested Exception: ", ex);
            }
            JOptionPane.showMessageDialog(
                    CayenneModelerFrame.getFrame(),
                    e.getMessage(),
                    "Error Connecting to the Database",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        catch (ClassNotFoundException e) {
            logObj.warn("Error loading driver. Classpath: "
                    + System.getProperty("java.class.path"), e);
            JOptionPane.showMessageDialog(
                    CayenneModelerFrame.getFrame(),
                    "Class not found: " + driverClassName,
                    "Error Loading Driver",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        catch (Exception e) {
            logObj.warn("Error Connecting to the Database", e);
            JOptionPane.showMessageDialog(
                    CayenneModelerFrame.getFrame(),
                    e.getMessage(),
                    "Error Connecting to the Database",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public List loadSchemas(DbLoader loader) {
        try {
            return loader.getSchemas();
        }
        catch (SQLException e) {
            logObj.warn("Error loading schemas", e);
            JOptionPane.showMessageDialog(
                    CayenneModelerFrame.getFrame(),
                    e.getMessage(),
                    "Error loading schemas",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void processMapUpdate(DataMap map) {
        EventController mediator = getMediator();

        if (mediator.getCurrentDataMap() != null) {
            mediator.fireDataMapEvent(new DataMapEvent(
                    CayenneModelerFrame.getFrame(),
                    map,
                    MapEvent.CHANGE));
            mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(CayenneModelerFrame
                    .getFrame(), map, mediator.getCurrentDataDomain(), mediator
                    .getCurrentDataNode()));
        }
        else {
            mediator.addDataMap(CayenneModelerFrame.getFrame(), map);
        }
    }

    public DataMap loadMap(DbLoader loader, String schemaName, String tableNamePattern) {
        EventController mediator = getMediator();
        try {
            DataMap map = mediator.getCurrentDataMap();
            if (map != null) {
                loader.loadDataMapFromDB(schemaName, tableNamePattern, map);
                return map;
            }
            else {
                map = loader.createDataMapFromDB(schemaName, tableNamePattern);

                // fix map name
                map.setName(NamedObjectFactory.createName(DataMap.class, mediator
                        .getCurrentDataDomain()));

                return map;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    CayenneModelerFrame.getFrame(),
                    e.getMessage(),
                    "Error reverse engineering database",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void performAction(ActionEvent e) {
        importDb();
    }

    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataDomain.class) != null;
    }

    class LoaderDelegate implements DbLoaderDelegate {

        protected EventController mediator;
        protected int duplicate = YesNoToAllDialog.UNDEFINED;
        protected boolean existingMap;
        protected String userName;

        public LoaderDelegate(DataSourceInfo dsi, EventController mediator) {
            this.mediator = mediator;
            this.existingMap = mediator.getCurrentDataMap() != null;
            this.userName = dsi.getUserName();
        }

        /**
         * @see org.objectstyle.cayenne.access.DbLoaderDelegate#overwriteDbEntity(DbEntity)
         */
        public boolean overwriteDbEntity(DbEntity ent) throws CayenneException {
            // the decision may have been made already
            if (duplicate == YesNoToAllDialog.YES_TO_ALL) {
                return true;
            }

            if (duplicate == YesNoToAllDialog.NO_TO_ALL) {
                return false;
            }

            YesNoToAllDialog dialog = new YesNoToAllDialog(
                    "Duplicate Table Name",
                    "DataMap already contains DBEntity for table '"
                            + ent.getName()
                            + "'. Overwrite?");
            int code = dialog.getStatus();
            dialog.dispose();

            if (YesNoToAllDialog.YES_TO_ALL == code) {
                duplicate = YesNoToAllDialog.YES_TO_ALL;
                return true;
            }
            else if (YesNoToAllDialog.NO_TO_ALL == code) {
                duplicate = YesNoToAllDialog.NO_TO_ALL;
                return false;
            }
            else if (YesNoToAllDialog.YES == code) {
                return true;
            }
            else if (YesNoToAllDialog.NO == code) {
                return false;
            }
            else {
                throw new CayenneException("Should stop DB import.");
            }
        }

        public void dbEntityAdded(DbEntity entity) {
            // TODO: hack to prevent PK tables from being visible... this should really be
            // delegated to DbAdapter to decide...
            if (excludedTables.contains(entity.getName()) && entity.getDataMap() != null) {
                entity.getDataMap().removeDbEntity(entity.getName());
            }
            else if (existingMap) {
                mediator
                        .fireDbEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
            }
        }

        public void objEntityAdded(ObjEntity entity) {
            if (existingMap) {
                mediator
                        .fireObjEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
            }
        }

        public void dbEntityRemoved(DbEntity entity) {
            if (existingMap) {
                mediator.fireDbEntityEvent(new EntityEvent(
                        CayenneModelerFrame.getFrame(),
                        entity,
                        EntityEvent.REMOVE));
            }
        }

        public void objEntityRemoved(ObjEntity entity) {
            if (existingMap) {
                mediator.fireObjEntityEvent(new EntityEvent(CayenneModelerFrame
                        .getFrame(), entity, EntityEvent.REMOVE));
            }
        }

        public void setSchema(DbEntity entity, String schema) {
            entity.setSchema(useSchema(schema) ? schema : null);
        }

        /**
         * Schema should not be used if the user is the owner of this schema.
         */
        protected boolean useSchema(String schema) {
            return userName == null || !userName.equalsIgnoreCase(schema);
        }
    }
}