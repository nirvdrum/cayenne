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
package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.access.DbLoader;
import org.objectstyle.cayenne.access.DbLoaderDelegate;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.InteractiveLogin;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.datamap.ChooseSchemaDialog;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapEvent;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.EntityEvent;
import org.objectstyle.cayenne.modeler.event.ModelerEvent;
import org.objectstyle.cayenne.modeler.util.YesNoToAllDialog;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Action that imports database structure into a DataMap.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ImportDbAction extends CayenneAction {
    static Logger logObj = Logger.getLogger(ImportDbAction.class.getName());
    public static final String ACTION_NAME = "Reengineer Database Schema";

    public ImportDbAction() {
        super(ACTION_NAME);
    }

    public void importDb() {
        EventController mediator = getMediator();
        DataSourceInfo dsi = new DataSourceInfo();
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
            DbLoader loader = new DbLoader(conn, adapter, new LoaderDelegate(mediator));
            List schemas = loadSchemas(loader);
            if (schemas == null) {
                return;
            }

            String schemaName = null;
            if (schemas.size() != 0) {
                ChooseSchemaDialog dialog = new ChooseSchemaDialog(schemas);
                dialog.show();
                if (dialog.getChoice() == ChooseSchemaDialog.CANCEL) {
                    dialog.dispose();
                    return;
                }
                schemaName = dialog.getSchemaName();
                dialog.dispose();
            }
            if (schemaName != null && schemaName.length() == 0) {
                schemaName = null;
            }

            DataMap map = loadMap(loader, schemaName);
            if (map == null) {
                return;
            }

            processMapUpdate(map);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logObj.warn("Error closing connection.", e);
            }
        }
    }

    public DbAdapter createAdapter(DataSourceInfo dsi) {
        // load adapter
        try {
            return (DbAdapter) Class.forName(dsi.getAdapterClass()).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                Editor.getFrame(),
                e.getMessage(),
                "Error loading adapter",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public Connection openConnection(DataSourceInfo dsi) {
        try {
            Class.forName(dsi.getJdbcDriver()).newInstance();
            return DriverManager.getConnection(
                dsi.getDataSourceUrl(),
                dsi.getUserName(),
                dsi.getPassword());
        } catch (SQLException e) {
            logObj.info(e.getMessage());
            SQLException ex = e.getNextException();
            if (ex != null) {
                System.out.println(ex.getMessage());
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                Editor.getFrame(),
                e.getMessage(),
                "Error Connecting to the Database",
                JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                Editor.getFrame(),
                e.getMessage(),
                "Error loading driver",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public List loadSchemas(DbLoader loader) {
        try {
            return loader.getSchemas();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                Editor.getFrame(),
                e.getMessage(),
                "Error loading schemas",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void processMapUpdate(DataMap map) {
        EventController mediator = getMediator();

        // If this is adding to existing data map, remove it
        // and re-add to the BroseView
        if (mediator.getCurrentDataMap() != null) {
            mediator.fireDataMapEvent(
                new DataMapEvent(Editor.getFrame(), map, ModelerEvent.CHANGE));
            mediator.fireDataMapDisplayEvent(
                new DataMapDisplayEvent(
                    Editor.getFrame(),
                    map,
                    mediator.getCurrentDataDomain(),
                    mediator.getCurrentDataNode()));
        } else {
            mediator.addDataMap(Editor.getFrame(), map);
        }
    }

    public DataMap loadMap(DbLoader loader, String schemaName) {
        EventController mediator = getMediator();
        try {
            DataMap map = mediator.getCurrentDataMap();
            if (map != null) {
                loader.loadDataMapFromDB(schemaName, null, map);
                return map;
            } else {
                return loader.createDataMapFromDB(schemaName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                Editor.getFrame(),
                e.getMessage(),
                "Error reverse engineering database",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void performAction(ActionEvent e) {
        importDb();
    }

    class LoaderDelegate implements DbLoaderDelegate {
        protected EventController mediator;
        protected int duplicate = YesNoToAllDialog.UNDEFINED;

        public LoaderDelegate(EventController mediator) {
            this.mediator = mediator;
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

            YesNoToAllDialog dialog =
                new YesNoToAllDialog(
                    "Duplicate Table Name",
                    "DataMap already contains DBEntity for table '"
                        + ent.getName()
                        + "'. Overwrite?");
            int code = dialog.getStatus();
            dialog.dispose();

            if (YesNoToAllDialog.YES_TO_ALL == code) {
                duplicate = YesNoToAllDialog.YES_TO_ALL;
                return true;
            } else if (YesNoToAllDialog.NO_TO_ALL == code) {
                duplicate = YesNoToAllDialog.NO_TO_ALL;
                return false;
            } else if (YesNoToAllDialog.YES == code) {
                return true;
            } else if (YesNoToAllDialog.NO == code) {
                return false;
            } else {
                throw new CayenneException("Should stop DB import.");
            }
        }
        /**
         * @see org.objectstyle.cayenne.access.DbLoaderDelegate#dbEntityAdded(DbEntity)
         */
        public void dbEntityAdded(DbEntity ent) {
            mediator.fireDbEntityEvent(new EntityEvent(this, ent, EntityEvent.ADD));
        }

        /**
         * @see org.objectstyle.cayenne.access.DbLoaderDelegate#objEntityAdded(ObjEntity)
         */
        public void objEntityAdded(ObjEntity ent) {
            mediator.fireObjEntityEvent(new EntityEvent(this, ent, EntityEvent.ADD));
        }
        /**
         * @see org.objectstyle.cayenne.access.DbLoaderDelegate#dbEntityRemoved(DbEntity)
         */
        public void dbEntityRemoved(DbEntity ent) {
            mediator.fireDbEntityEvent(
                new EntityEvent(Editor.getFrame(), ent, EntityEvent.REMOVE));
        }

        /**
         * @see org.objectstyle.cayenne.access.DbLoaderDelegate#objEntityRemoved(ObjEntity)
         */
        public void objEntityRemoved(ObjEntity ent) {
            mediator.fireObjEntityEvent(
                new EntityEvent(Editor.getFrame(), ent, EntityEvent.REMOVE));
        }
    }
}
