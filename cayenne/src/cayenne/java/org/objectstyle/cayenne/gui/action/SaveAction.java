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
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.DomainHelper;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.GuiDataSource;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.FileSystemViewDecorator;
import org.objectstyle.cayenne.gui.util.XmlFilter;
import org.objectstyle.cayenne.gui.validator.*;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;

/** 
 * Parent class for all Editor actions related to saving project.
 * 
 * @author Misha Shengaout
 */
public class SaveAction extends CayenneAction {
	static Logger logObj = Logger.getLogger(SaveAction.class.getName());

	public static final String ACTION_NAME = "Save";

	protected HashMap tempLookup = new HashMap();

	public SaveAction() {
		super(ACTION_NAME);
	}

	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK);
	}

	public String getIconName() {
		return "icon-save.gif";
	}

	/** 
	 * Saves project and related files. Saving is done to temporary files, 
	 * and only on successful save, master files are replaced with new versions. 
	 */
	protected void saveAll() throws Exception {
		tempLookup.clear();

		Mediator mediator = getMediator();
		Iterator iter = mediator.getDirtyDataMaps().iterator();
		while (iter.hasNext()) {
			saveDataMap((DataMap) iter.next());
		}

		iter = mediator.getDirtyDataNodes().iterator();
		while (iter.hasNext()) {
			DataNode node = (DataNode) iter.next();
			// If using direct connection, save into separate file
			if (node.getDataSourceFactory().equals(DataSourceFactory.DIRECT_FACTORY)) {
				saveDataNode(node);
			}
		}

		saveProject();
		replaceMasterFiles();
	}

	/**
	 * Replaces master files with fresh temporary files.
	 */
	protected void replaceMasterFiles() throws Exception {
		Iterator it = tempLookup.keySet().iterator();
		while (it.hasNext()) {
			File tmp = (File) it.next();
			File master = (File) tempLookup.get(tmp);
			if (master.exists()) {
				if (!master.delete()) {
					throw new IOException("Unable to remove old master file " + master);
				}
			}

			if (!tmp.renameTo(master)) {
				throw new IOException("Unable to move " + tmp + " to " + master);
			}
		}
	}

	/**
	 * Attempts to remove all temporary files.
	 */
	protected void cleanTempFiles() {
		Iterator it = tempLookup.keySet().iterator();
		while (it.hasNext()) {
			File tmp = (File) it.next();
			tmp.delete();
		}
	}

	/** 
	 * Creates temporary file for the master file.
	 */
	protected File tempFileForFile(File f) throws IOException {
		File parent = f.getParentFile();
		String name = f.getName();

		if (name == null || name.length() < 3) {
			name = "modeler";
		}

		File tmp = File.createTempFile(name, null, parent);
		tempLookup.put(tmp, f);

		return tmp;
	}

	protected void saveProject() throws Exception {
		Mediator mediator = getMediator();
		File file = tempFileForFile(mediator.getConfig().getProjFile());
		String masterPath = ((File) tempLookup.get(file)).getAbsolutePath();

		FileWriter fw = new FileWriter(file);

		try {
			DomainHelper.storeDomains(new PrintWriter(fw), mediator.getDomains());
			Editor.getFrame().addToLastProjList(masterPath);
		} finally {
			fw.flush();
			fw.close();
		}
	}

	/** Save data source info if data source factory is DIRECT_FACTORY. */
	protected void saveDataNode(DataNode node) throws Exception {
		Mediator mediator = getMediator();
		File projDir = new File(mediator.getConfig().getProjDir());
		File file = tempFileForFile(new File(projDir, node.getDataSourceLocation()));

		FileWriter fw = new FileWriter(file);
		try {
			PrintWriter pw = new PrintWriter(fw);
			try {
				GuiDataSource src = (GuiDataSource) node.getDataSource();
				DomainHelper.storeDataNode(pw, src.getDataSourceInfo());
			} finally {
				pw.close();
			}
		} finally {
			fw.close();
		}
	}

	/** Save data map to the file. */
	protected void saveDataMap(DataMap map) throws Exception {
		File projDir = new File(getMediator().getConfig().getProjDir());
		File file = tempFileForFile(new File(projDir, map.getLocation()));

		MapLoader saver = new MapLoader();
		FileWriter fw = new FileWriter(file);

		try {
			PrintWriter pw = new PrintWriter(fw);
			try {
				saver.storeDataMap(pw, map);
			} finally {
				pw.close();
			}
		} finally {
			fw.close();
		}
	}

	/**
	 * This method is synchronized to prevent problems on double-clicking "save".
	 */
	public synchronized void performAction(ActionEvent e) {
		performAction(ErrorMsg.WARNING);
	}

	public synchronized void performAction(int warningLevel) {
		Mediator mediator = getMediator();
		Validator val = new Validator(mediator);
		int validationCode = val.validate();

		// If no serious errors, perform save.
		if (validationCode < ErrorMsg.ERROR) {
			try {
				saveAll();
			} catch (Exception ex) {
				cleanTempFiles();
				throw new CayenneRuntimeException("Error on save", ex);
			}

			mediator.getDirtyDataMaps().clear();
			mediator.getDirtyDomains().clear();
			mediator.getDirtyDataNodes().clear();
			mediator.setDirty(false);
		}

		// If there were errors or warnings at validation, display them
		if (validationCode >= warningLevel) {
			new ValidatorDialog(
				Editor.getFrame(),
				mediator,
				val.getErrorMessages(),
				validationCode).setVisible(true);
		}
	}
}