/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.project.CayenneUserDir;

/** 
 * ModelerPreferences class supports persistent user preferences. 
 * Preferences are saved in the user home directory in 
 * "<code>$HOME/.cayenne/modeler.preferences</code>" file.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ModelerPreferences extends ExtendedProperties {
    static final Logger logObj = Logger.getLogger(ModelerPreferences.class);

    /** Name of the preferences file. */
	public static final String PREFERENCES_NAME = "modeler.preferences";

	/** Name of the log file. */
	public static final String LOGFILE_NAME = "modeler.log";


    // Keys for the preference file.

    /** The directory of the cayenne project edited last. */
    public static final String LAST_DIR = "Editor.lastProject";

    /** The directory where the last EOModel was imported. */
    public static final String LAST_EOM_DIR = "Editor.lastEOModel";

    /** List of the last 4 opened project files. */
    public static final String LAST_PROJ_FILES = "Editor.lastSeveralProjectFiles";

    /** The directory of the last generated classes. */
    public static final String LAST_GENERATED_CLASSES_DIR =
        "gui.datamap.GenerateClassDialog.lastDir";

    /** User name */
    public static final String USER_NAME = "DbLoginPanel.unInput";

    /** JDBC Driver Class */
    public static final String JDBC_DRIVER = "DbLoginPanel.drInput";

    /** Database URL */
    public static final String DB_URL = "DbLoginPanel.urlInput";

    /** RDBMS Adapter */
    public static final String RDBMS_ADAPTER = "DbLoginPanel.adapterInput";

	/** GUI layout */
	public static final String EDITOR_LAFNAME = "Editor.lookAndFeel";
	public static final String EDITOR_THEMENAME = "Editor.theme";
	public static final String EDITOR_FRAME_WIDTH = "Editor.frameWidth";
	public static final String EDITOR_FRAME_HEIGHT = "Editor.frameHeight";
	public static final String EDITOR_FRAME_X  = "Editor.frameX";
	public static final String EDITOR_FRAME_Y = "Editor.frameY";
	public static final String EDITOR_TREE_WIDTH = "Editor.treeWidth";

	/** Log file */
	public static final String EDITOR_LOGFILE_ENABLED = "Editor.logfileEnabled";
	public static final String EDITOR_LOGFILE = "Editor.logfile";


	protected static ModelerPreferences sharedInstance;

    protected ModelerPreferences() {}

    /**
     * Returns Cayenne preferences singleton.
     */
    public static ModelerPreferences getPreferences() {
        if (sharedInstance == null) {
            sharedInstance = new ModelerPreferences();
            sharedInstance.loadPreferences();
        }
        
        return sharedInstance;
    }

    /** 
     * Returns preferences directory <code>$HOME/.cayenne</code>. 
     * If such directory does not exist, it is created as a side 
     * effect of this method.
     */
    public File preferencesDirectory() {
        return CayenneUserDir.getInstance().getDirectory();
    }

    /** 
     * Saves preferences. Preferences stored in
     * <code>$HOME/.cayenne/modeler.preferences</code> file. 
     */
    public void storePreferences() {
        File prefFile = new File(preferencesDirectory(), PREFERENCES_NAME);
        try {
            if (!prefFile.exists()) {
                logObj.debug(
                    "Cannot save preferences - file " + prefFile + " does not exist");
                return;
            }
            save(new FileOutputStream(prefFile), "");
        } catch (IOException e) {
            logObj.debug("Error saving preferences: ", e);
        }
    }

    /** 
     * Loads preferences from <code>$HOME/.cayenne/modeler.preferences</code> 
     * file.
     */
    public void loadPreferences() {
        try {
            File prefsFile = new File(preferencesDirectory(), PREFERENCES_NAME);
            if (!prefsFile.exists()) {
                if (!prefsFile.createNewFile()) {
                    logObj.warn("Can't create preferences file " + prefsFile);
                }
            }

            load(new FileInputStream(prefsFile));
        } catch (IOException e) {
            logObj.warn("Error creating preferences file.", e);
        }
    }

}