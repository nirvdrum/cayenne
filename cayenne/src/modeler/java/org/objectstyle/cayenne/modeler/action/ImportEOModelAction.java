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

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapEvent;
import org.objectstyle.cayenne.modeler.util.EOModelFileFilter;
import org.objectstyle.cayenne.modeler.util.EOModelSelectFilter;
import org.objectstyle.cayenne.wocompat.EOModelProcessor;

/**
 * Action handler for WebObjects EOModel import function.
 * 
 * @author Andrei Adamchik
 */
public class ImportEOModelAction extends CayenneAction {
	static Logger logObj =
		Logger.getLogger(ImportEOModelAction.class.getName());

	public static final String ACTION_NAME = "Import EOModel";

	protected JFileChooser eoModelChooser;

	public ImportEOModelAction() {
		super(ACTION_NAME);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void performAction(ActionEvent event) {
		importEOModel();
	}

	/** 
	 * Lets user select an EOModel, then imports it as a DataMap.
	 */
	protected void importEOModel() {
		JFileChooser fileChooser = getEOModelChooser();
		int status = fileChooser.showOpenDialog(Editor.getFrame());

		if (status == JFileChooser.APPROVE_OPTION) {
			// save preferences
			File file = fileChooser.getSelectedFile();
			if (file.isFile()) {
				file = file.getParentFile();
			}

			ModelerPreferences.getPreferences().setProperty(
				ModelerPreferences.LAST_EOM_DIR,
				file.getParent());

			try {
				String path = file.getCanonicalPath();
				DataMap map = new EOModelProcessor().loadEOModel(path);
				addDataMap(map);
			} catch (Exception ex) {
				logObj.log(Level.INFO, "EOModel Loading Exception", ex);
			}

		}
	}

	/** 
	 * Adds DataMap into the project.
	 */
	protected void addDataMap(DataMap map) {
		DataMap currentMap = getMediator().getCurrentDataMap();
		EventController mediator = getMediator();

		if (currentMap != null) {
			// merge with existing map
			// loader.loadDataMapFromDB(schema_name, null, map);
			currentMap.mergeWithDataMap(map);
			map = currentMap;
		}

		// If this is adding to existing data map, remove it
		// and re-add to the BroseView
		if (currentMap != null) {
			mediator.fireDataMapEvent(
				new DataMapEvent(Editor.getFrame(), map, DataMapEvent.REMOVE));
			mediator.fireDataMapEvent(
				new DataMapEvent(Editor.getFrame(), map, DataMapEvent.ADD));
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

	/** 
	 * Returns EOModel chooser.
	 */
	public JFileChooser getEOModelChooser() {

		if (eoModelChooser == null) {
			eoModelChooser = new EOModelChooser("Select EOModel");
		}

		String startDir =
			ModelerPreferences.getPreferences().getString(ModelerPreferences.LAST_EOM_DIR);

		if (startDir == null) {
			startDir =
				ModelerPreferences.getPreferences().getString(ModelerPreferences.LAST_DIR);
		}

		if (startDir != null) {
			File startDirFile = new File(startDir);
			if (startDirFile.exists()) {
				eoModelChooser.setCurrentDirectory(startDirFile);
			}
		}

		return eoModelChooser;
	}

	/** 
	 * Custom file chooser that will pop up again if a bad directory is selected.
	 */
	class EOModelChooser extends JFileChooser {
		protected FileFilter selectFilter;
		protected JDialog cachedDialog;

		public EOModelChooser(String title) {
			super.setFileFilter(new EOModelFileFilter());
			super.setDialogTitle(title);
			super.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

			this.selectFilter = new EOModelSelectFilter();
		}

		public int showOpenDialog(Component parent) {
			int status = super.showOpenDialog(parent);
			if (status != JFileChooser.APPROVE_OPTION) {
				cachedDialog = null;
				return status;
			}

			// make sure invalid directory is not selected
			File file = this.getSelectedFile();
			if (selectFilter.accept(file)) {
				cachedDialog = null;
				return JFileChooser.APPROVE_OPTION;
			} else {
				if (file.isDirectory()) {
					this.setCurrentDirectory(file);
				}

				return this.showOpenDialog(parent);
			}
		}

		protected JDialog createDialog(Component parent)
			throws HeadlessException {

			if (cachedDialog == null) {
				cachedDialog = super.createDialog(parent);
			}
			return cachedDialog;
		}
	}
}
