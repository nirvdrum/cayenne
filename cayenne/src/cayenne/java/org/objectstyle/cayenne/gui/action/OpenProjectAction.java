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
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.ErrorDebugDialog;
import org.objectstyle.cayenne.gui.GuiConfiguration;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.util.ProjectFileFilter;
import org.objectstyle.cayenne.gui.util.RecentFileMenuItem;
import org.objectstyle.cayenne.util.Preferences;

/**
 * @author Andrei Adamchik
 */
public class OpenProjectAction extends ProjectAction {
	static Logger logObj = Logger.getLogger(OpenProjectAction.class.getName());
	public static final String ACTION_NAME = "Open Project";

	/**
	 * Constructor for OpenProjectAction.
	 * @param name
	 */
	public OpenProjectAction() {
		super(ACTION_NAME);
	}

	public String getIconName() {
		return "icon-open.gif";
	}

	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK);
	}

	/**
	 * @see org.objectstyle.cayenne.gui.action.CayenneAction#performAction(ActionEvent)
	 */
	public void performAction(ActionEvent e) {
		File f = null;
		if (e.getSource() instanceof RecentFileMenuItem) {
			RecentFileMenuItem menu = (RecentFileMenuItem) e.getSource();
			f = menu.getFile();
		}

		if (f == null) {
			openProject();
		} else {
			openProject(f);
		}
	}

	/** Opens cayenne.xml file using file chooser. */
	protected void openProject() {
		Preferences pref = Preferences.getPreferences();
		String init_dir = (String) pref.getProperty(Preferences.LAST_DIR);
		try {
			// Get the project file name (always cayenne.xml)
			File file = null;
			fileChooser.setFileFilter(new ProjectFileFilter());
			fileChooser.setDialogTitle("Choose project file (cayenne.xml)");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (null != init_dir) {
				File init_dir_file = new File(init_dir);
				if (init_dir_file.exists())
					fileChooser.setCurrentDirectory(init_dir_file);
			}
			int ret_code = fileChooser.showOpenDialog(Editor.getFrame());
			if (ret_code != JFileChooser.APPROVE_OPTION)
				return;
			file = fileChooser.getSelectedFile();
			openProject(file);
		} catch (Exception e) {
			logObj.warn("Error loading project file.", e);
		}
	}

	/** Opens specified project file. File must already exist. */
	protected void openProject(File file) {
		// Save and close (if needed) currently open project.
		if (getMediator() != null && !closeProject()) {
			return;
		}
		Preferences pref = Preferences.getPreferences();
		try {
			// Save dir path to the preferences
			pref.setProperty(Preferences.LAST_DIR, file.getParent());
			Editor.getFrame().addToLastProjList(file.getAbsolutePath());

			// Initialize gui configuration
			// uncomment to debug GUI
			Configuration.setLoggingLevel(Level.INFO);

			setMediator(new Mediator());
			Editor.getFrame().projectOpened(file);
			Editor.getFrame().setProjectTitle(file.getAbsolutePath());

		} catch (Exception ex) {
			logObj.warn("Error loading project file.", ex);
			ErrorDebugDialog.guiWarning(ex, "Error loading project");
		}
	}
}
