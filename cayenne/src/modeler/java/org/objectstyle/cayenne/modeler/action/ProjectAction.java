/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.control.ModelerController;
import org.objectstyle.cayenne.modeler.view.UnsavedChangesDialog;
import org.objectstyle.cayenne.project.ProjectPath;
import org.scopemvc.core.Control;

/**
 * @author Andrei Adamchik
 */
public class ProjectAction extends CayenneAction {
	private static Logger logObj = Logger.getLogger(ProjectAction.class);
	
	public static String getActionName() {
		return "Close Project";
	}

    public ProjectAction() {
        super(getActionName());
    }

    /**
     * Constructor for ProjectAction.
     * @param name
     */
    public ProjectAction(String name) {
        super(name);
    }

    /**
     * Closes current project.
     */
    public void performAction(ActionEvent e) {
        closeProject();
    }

    /** Returns true if successfully closed project, false otherwise. */
    public boolean closeProject() {
        if (!checkSaveOnClose()) {
            return false;
        }

        CayenneModelerFrame.getFrame().getController().handleControl(
            new Control(ModelerController.PROJECT_CLOSED_ID));
        
        logObj.info("Closed project.");
        return true;
    }

    /** 
     * Returns false if cancel closing the window, true otherwise. 
     */
    public boolean checkSaveOnClose() {
        EventController mediator = getMediator();
        if (mediator != null && mediator.isDirty()) {
            UnsavedChangesDialog dialog = new UnsavedChangesDialog(CayenneModelerFrame.getFrame());
            dialog.show();

            if (dialog.shouldCancel()) {
            	// discard changes and DO NOT close
                return false;
            } else if (dialog.shouldSave()) {
                // save changes and close
                ActionEvent e =
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "SaveAll");
                CayenneModelerFrame.getFrame().getAction(SaveAction.getActionName()).actionPerformed(e);
				if(mediator.isDirty()) {
					// save was canceled... do not close
					return false;
				}
            }
        }

        return true;
    }

    /**
     * Always returns true.
     */
    public boolean enableForPath(ProjectPath path) {
        return true;
    }
}
