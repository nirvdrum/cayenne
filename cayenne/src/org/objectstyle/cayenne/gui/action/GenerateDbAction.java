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
package org.objectstyle.cayenne.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.gui.*;
import org.objectstyle.cayenne.gui.datamap.GenerateDbDialog;

/** 
 * Action that generates database tables from a DataMap.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class GenerateDbAction extends CayenneAction {
	static Logger logObj = Logger.getLogger(GenerateDbAction.class.getName());
	public static final String ACTION_NAME = "Generate Database Schema";

	public GenerateDbAction() {
		super(ACTION_NAME);
	}

	public void performAction(ActionEvent e) {
		generateDb();
	}

	protected void generateDb() {
		DataSourceInfo dsi = null;
		DbAdapter adapter = null;

		// Get connection info
		while (true) {
			dsi = getDataSourceInfo();
			if (dsi == null) {
				return;
			}
			if (dsi.getAdapterClass() == null
				|| dsi.getAdapterClass().trim().length() == 0) {
				JOptionPane.showMessageDialog(
					Editor.getFrame(),
					"Must specify DB Adapter");
				continue;
			}
			try {
				adapter =
					(DbAdapter) Class
						.forName(dsi.getAdapterClass())
						.newInstance();
				break;
			} catch (InstantiationException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(
					Editor.getFrame(),
					e.getMessage(),
					"Error creating DbAdapter",
					JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(
					Editor.getFrame(),
					e.getMessage(),
					"Error creating DbAdapter",
					JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(
					Editor.getFrame(),
					e.getMessage(),
					"Error creating DbAdapter",
					JOptionPane.ERROR_MESSAGE);
				continue;
			}
		}
		GenerateDbDialog dialog = new GenerateDbDialog(dsi, adapter);
		dialog.show();
		dialog.dispose();
	}

	protected DataSourceInfo getDataSourceInfo() {
		DataNode currentNode = getMediator().getCurrentDataNode();
		DataSourceInfo dsi = null;
		if (currentNode != null) {
			dsi =
				((GuiDataSource) currentNode.getDataSource())
					.getDataSourceInfo()
					.cloneInfo();
			if (currentNode.getAdapter() != null) {
				dsi.setAdapterClass(
					currentNode.getAdapter().getClass().getName());
			}
		} else {
			dsi = new DataSourceInfo();
		}

		InteractiveLogin loginObj = InteractiveLogin.getGuiLoginObject(dsi);
		loginObj.collectLoginInfo();
		return loginObj.getDataSrcInfo();

	}
}
