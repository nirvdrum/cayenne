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
package org.objectstyle.cayenne.modeler.dialog.prefs;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @author Andrei Adamchik
 */
public class ConfigureClasspathController extends BasicController {
    static final Logger logObj = Logger.getLogger(ConfigureClasspathController.class);

    public static final String SAVE_CONTROL =
        "cayenne.modeler.configClasspath.save.button";
    public static final String CANCEL_CONTROL =
        "cayenne.modeler.configClasspath.cancel.button";
    public static final String ADDJAR_CONTROL =
        "cayenne.modeler.configClasspath.addjar.button";
    public static final String ADDDIR_CONTROL =
        "cayenne.modeler.configClasspath.adddir.button";
    public static final String REMOVE_CONTROL =
        "cayenne.modeler.configClasspath.remove.button";

    public ConfigureClasspathController() {
        setModel(new ConfigureClasspathModel(ModelerClassLoader.getClassLoader()));
    }

    /**
     * Creates and runs the classpath dialog.
     */
    public void startup() {
        setView(new ConfigureClasspathDialog());
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONTROL)) {
            saveClasspath();
        }
        else if (control.matchesID(ADDJAR_CONTROL)) {
            addJarOrZip();
        }
        else if (control.matchesID(ADDDIR_CONTROL)) {
            addClassDirectory();
        }
        else if (control.matchesID(REMOVE_CONTROL)) {
            removeEntry();
        }
    }

    /**
     * Saves changes made to CLASSPATH.
     */
    protected void saveClasspath() {
        ConfigureClasspathModel model = (ConfigureClasspathModel) getModel();
        try {
            model.save();
        }
        catch (MalformedURLException urlEx) {
            logObj.info("Error saving ClassPath", urlEx);
            JOptionPane.showMessageDialog(
                Application.getFrame(),
                urlEx.getMessage(),
                "Error saving ClassPath",
                JOptionPane.ERROR_MESSAGE);
        }
        catch (IOException ioEx) {
            logObj.info("Error saving ClassPath", ioEx);
            JOptionPane.showMessageDialog(
                Application.getFrame(),
                ioEx.getMessage(),
                "Error saving ClassPath",
                JOptionPane.ERROR_MESSAGE);
        }

        shutdown();
    }

    protected void addJarOrZip() {
        chooseClassEntry(
            FileFilters.getClassArchiveFilter(),
            "Select JAR or ZIP File.",
            JFileChooser.FILES_ONLY);
    }

    protected void addClassDirectory() {
        chooseClassEntry(
            null,
            "Select Java Class Directory.",
            JFileChooser.DIRECTORIES_ONLY);
    }

    protected void removeEntry() {
        ConfigureClasspathModel model = (ConfigureClasspathModel) getModel();
        model.removeSelectedEntry();
    }

    protected void chooseClassEntry(FileFilter filter, String title, int selectionMode) {

        // guess start directory
        File startDir = null;
        String lastUsed =
            (String) ModelerPreferences.getPreferences().getProperty(
                ModelerPreferences.LAST_CLASSPATH_SELECTION_DIR);
                
        if (lastUsed != null) {
            startDir = new File(lastUsed);
            if (startDir.isFile()) {
                startDir = startDir.getParentFile();
            }
        }

        if (startDir == null) {
            lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_DIR);
            if (lastUsed != null) {
                startDir = new File(lastUsed);
            }
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(selectionMode);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);

        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
        }

        chooser.setDialogTitle(title);

        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        File selected = null;
        int result = chooser.showOpenDialog((Component) this.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();

            // Set preferences
            File lastDir = selected.getParentFile();
            ModelerPreferences.getPreferences().setProperty(
                ModelerPreferences.LAST_CLASSPATH_SELECTION_DIR,
                lastDir.getAbsolutePath());
            ModelerPreferences.getPreferences().setProperty(
                ModelerPreferences.LAST_DIR,
                lastDir.getAbsolutePath());
        }

        if (selected != null) {
            // add file to the model
            ConfigureClasspathModel model = (ConfigureClasspathModel) getModel();
            model.addEntry(selected);
        }
    }
}
