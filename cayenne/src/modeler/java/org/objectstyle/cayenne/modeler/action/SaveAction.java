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
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.validator.ValidationDisplayHandler;
import org.objectstyle.cayenne.modeler.validator.ValidatorDialog;
import org.objectstyle.cayenne.modeler.view.ProjectOpener;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.validator.Validator;

/** 
 * Parent class for all Editor actions related to saving project.
 * 
 * @author Misha Shengaout
 */
public class SaveAction extends CayenneAction {
    private static Logger logObj = Logger.getLogger(SaveAction.class);

    public static final String ACTION_NAME = "Save";

    protected ProjectOpener fileChooser = new ProjectOpener();

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
    protected boolean saveAll() throws Exception {
        Project p = Editor.getProject();

        if (p.isLocationUndefined()) {
            File projectDir = fileChooser.newProjectDir(Editor.getFrame(), p);
            if (projectDir == null) {
                return false;
            }

            p.setProjectDir(projectDir);
        }

        p.save();
        Editor.getFrame().updateTitle();
        Editor.getFrame().addToLastProjList(p.getMainFile().getAbsolutePath());
        return true;
    }

    /**
     * This method is synchronized to prevent problems on double-clicking "save".
     */
    public synchronized void performAction(ActionEvent e) {
        performAction(ValidationDisplayHandler.WARNING);
    }

    public synchronized void performAction(int warningLevel) {
        EventController mediator = getMediator();
        Validator val = Editor.getFrame().getProject().getValidator();
        int validationCode = val.validate();

        // If no serious errors, perform save.
        if (validationCode < ValidationDisplayHandler.ERROR) {
            try {
                if (!saveAll()) {
                    return;
                }
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error on save", ex);
            }

            mediator.setDirty(false);
        }

        // If there were errors or warnings at validation, display them
        if (validationCode >= warningLevel) {
            ValidatorDialog.showDialog(Editor.getFrame(), mediator, val);
        }
    }

    /**
    * Returns <code>true</code> if path contains a Project object 
    * and the project is modified.
    */
    public boolean enableForObjectPath(Object[] path) {
        if (path == null) {
            return false;
        }

        for (int i = 0; i < path.length; i++) {
            if (path[i] instanceof Project) {
                return ((Project)path[i]).isModified();
            }
        }

        return false;
    }
}