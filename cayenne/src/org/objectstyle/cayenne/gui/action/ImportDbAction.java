package org.objectstyle.cayenne.gui.action;
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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.*;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.conf.*;
import org.objectstyle.util.Preferences;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.GuiDataSource;
import org.objectstyle.cayenne.gui.InteractiveLogin;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.datamap.ChooseSchemaDialog;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.gui.validator.*;


/** Action to Import DB structure into a data map.
 */
public class ImportDbAction extends AbstractAction
{
	static Logger logObj = Logger.getLogger(ImportDbAction.class.getName());
	
	Mediator mediator;

	public ImportDbAction(Mediator temp_mediator) {
		mediator = temp_mediator;
	}

	protected void importDb() {
        DataSourceInfo dsi = new DataSourceInfo();
        Connection conn = null;
        // Get connection
        while (conn == null) {
	        InteractiveLogin loginObj = InteractiveLogin.getGuiLoginObject(dsi);
	        loginObj.collectLoginInfo();
	        // connect
	        dsi = loginObj.getDataSrcInfo();
	        if (null == dsi) {
	        	return;
	        }
	        try {
		        Driver driver = (Driver)Class.forName(dsi.getJdbcDriver()).newInstance();
		        conn = DriverManager.getConnection(
		              					dsi.getDataSourceUrl(),
		                   				dsi.getUserName(),
		                   				dsi.getPassword());
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				SQLException ex = e.getNextException();
				if (ex != null) {
					System.out.println(ex.getMessage());
				}
				e.printStackTrace();
				JOptionPane.showMessageDialog(Editor.getFrame()
							, e.getMessage(), "Error Connecting to the Database"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Editor.getFrame()
							, e.getMessage(), "Error loading driver"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			}
		}// End while()

		ArrayList schemas;
		DbLoader loader = new DbLoader(conn);
		try {
			schemas = loader.getSchemas();
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Editor.getFrame()
							, e.getMessage(), "Error loading schemas"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		String schema_name = null;
		if (schemas.size() != 0) {
			ChooseSchemaDialog dialog = new ChooseSchemaDialog(schemas);
			dialog.show();
			if (dialog.getChoice() == ChooseSchemaDialog.CANCEL) {
				dialog.dispose();
				return;
			}
			schema_name = dialog.getSchemaName();
			dialog.dispose();
		}
		if (schema_name != null && schema_name.length() == 0)
			schema_name = null;
		DataMap map;
		try {
			map = mediator.getCurrentDataMap();
			if (map != null )
				loader.loadDataMapFromDB(schema_name, null, map);
			else
				map = loader.createDataMapFromDB(schema_name);
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Editor.getFrame()
							, e.getMessage(), "Error reverse engineering database"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		// If this is adding to existing data map, remove it
		// and re-add to the BroseView
		if (mediator.getCurrentDataMap() != null) {
			mediator.fireDataMapEvent(new DataMapEvent(Editor.getFrame(), map, DataMapEvent.REMOVE));
			mediator.fireDataMapEvent(new DataMapEvent(Editor.getFrame(), map, DataMapEvent.ADD));
			mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(Editor.getFrame()
												, map
												, mediator.getCurrentDataDomain()
												, mediator.getCurrentDataNode()));
		} else {
			mediator.addDataMap(Editor.getFrame(), map);
		}
	}

	
	public void actionPerformed(ActionEvent e) {
		importDb();
	}
}

