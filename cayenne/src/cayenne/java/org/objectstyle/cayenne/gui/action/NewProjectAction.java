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
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.gui.ModelerPreferences;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.Mediator;

/**
 * @author Andrei Adamchik
 */
public class NewProjectAction extends ProjectAction {
    static Logger logObj = Logger.getLogger(NewProjectAction.class.getName());

    public static final String ACTION_NAME = "New Project";

    public NewProjectAction() {
        super(ACTION_NAME);
    }

    public String getIconName() {
        return "icon-new.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK);
    }

    /**
     * @see org.objectstyle.cayenne.gui.action.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        newProject();
    }

    protected void newProject() {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        String startDir = (String) pref.getProperty(ModelerPreferences.LAST_DIR);
        try {
            boolean finished = false;
            File file = null;
            File projectFile = null;

            while (!finished) {
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Choose project location");
                if (startDir != null) {
                    File startDirFile = new File(startDir);
                    if (startDirFile.exists()) {
                        fileChooser.setCurrentDirectory(startDirFile);
                    }
                }

                int retCode = fileChooser.showSaveDialog(Editor.getFrame());
                if(retCode == JFileChooser.CANCEL_OPTION) {
                	return;
                }

                file = fileChooser.getSelectedFile();
                
                if (!file.exists()) {
                    file.mkdirs();
                } else if (!file.isDirectory()) {
                    JOptionPane.showMessageDialog(
                        Editor.getFrame(),
                        "Can't create directory " + file);
                    return;
                }
                
                projectFile = new File(file, Configuration.DOMAIN_FILE);
                if (projectFile.exists()) {
                    int ret =
                        JOptionPane.showConfirmDialog(
                            Editor.getFrame(),
                            "There is already " + "project in this folder. Overwrite?");
                    if (ret == JOptionPane.YES_OPTION) {
                        finished = true;
                    } else if (ret == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                } else {
                    finished = true;
                }
            }

            // Save and close (if needed) currently open project.
            if (getMediator() != null && !closeProject()) {
                return;
            }

            // Save dir path to the preferences
            pref.setProperty(ModelerPreferences.LAST_DIR, file.getAbsolutePath());
            setMediator(new Mediator());
            Editor.getFrame().projectOpened(projectFile);

            // Set title to contain proj file path
            Editor.getFrame().setProjectTitle(projectFile.getAbsolutePath());
        } catch (Exception e) {
            logObj.warn("Error loading project file.", e);
        }
    }
}
