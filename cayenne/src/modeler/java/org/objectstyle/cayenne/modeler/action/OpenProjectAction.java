/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.dialog.ErrorDebugDialog;
import org.objectstyle.cayenne.modeler.util.RecentFileMenuItem;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectException;

/**
 * @author Andrei Adamchik
 */
public class OpenProjectAction extends ProjectAction {

    private static Logger logObj = Logger.getLogger(OpenProjectAction.class);

    protected ProjectOpener fileChooser;

    public static String getActionName() {
        return "Open Project";
    }

    /**
     * Constructor for OpenProjectAction.
     */
    public OpenProjectAction(Application application) {
        super(getActionName(), application);
        this.fileChooser = new ProjectOpener();
    }

    public String getIconName() {
        return "icon-open.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK);
    }

    public void performAction(ActionEvent e) {
        // Save and close (if needed) currently open project.
        if (getProjectController() != null && !closeProject()) {
            return;
        }
        File f = null;
        if (e.getSource() instanceof RecentFileMenuItem) {
            RecentFileMenuItem menu = (RecentFileMenuItem) e.getSource();
            f = menu.getFile();
        }

        if (f == null) {
            openProject();
        }
        else {
            openProject(f);
        }
    }

    /**
     * Opens cayenne.xml file using file chooser dialog.
     */
    public void openProject() {
        try {
            // Get the project file name (always cayenne.xml)
            File file = fileChooser.openProjectFile(Application.getFrame());
            if (file != null) {
                openProject(file);
            }
        }
        catch (Exception e) {
            logObj.warn("Error loading project file.", e);
        }
    }

    /** Opens specified project file. File must already exist. */
    public void openProject(File file) {
        // Using fresh ModelerClassLoader, as we need to support custom adapters
        ConfigurationHack.setResourceLoader(getApplication()
                .getClassLoadingService()
                .getClassLoader());

        try {
            getApplication().getFrameController().addToLastProjListAction(
                    file.getAbsolutePath());

            Project project = Project.createProject(file);
            getProjectController().setProject(project);

            // if upgrade was canceled
            if (project.isUpgradeNeeded() && !processUpgrades(project)) {
                closeProject();
            }
            else {
                getApplication().getFrameController().projectOpenedAction(project);
            }
        }
        catch (Exception ex) {
            logObj.warn("Error loading project file.", ex);
            ErrorDebugDialog.guiWarning(ex, "Error loading project");
        }
    }

    protected boolean processUpgrades(Project project) throws ProjectException {
        // must really concat all messages, this is a temp hack...
        String msg = (String) project.getUpgradeMessages().get(0);
        // need an upgrade
        int returnCode = JOptionPane.showConfirmDialog(
                Application.getFrame(),
                "Project needs an upgrade to a newer version. " + msg + ". Upgrade?",
                "Upgrade Needed",
                JOptionPane.YES_NO_OPTION);
        if (returnCode == JOptionPane.NO_OPTION) {
            return false;
        }

        // perform upgrade
        logObj.info("Will upgrade project " + project.getMainFile());
        project.upgrade();
        return true;
    }

    static final class ConfigurationHack extends DefaultConfiguration {

        // TODO: get rid of this once we are out of the API freeze..
        static void setResourceLoader(ClassLoader loader) {
            Configuration.resourceLoader = loader;
        }
    }
}