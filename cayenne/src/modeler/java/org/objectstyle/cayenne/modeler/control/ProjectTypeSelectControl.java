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
package org.objectstyle.cayenne.modeler.control;

import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.view.ProjectTypeSelectDialog;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.project.DataMapProject;
import org.objectstyle.cayenne.project.Project;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @author Andrei Adamchik
 */
public class ProjectTypeSelectControl extends BasicController {
    public static final String CREATE_APP_PROJECT_CONTROL =
        "cayenne.modeler.project.app.button";
    public static final String CREATE_MAP_PROJECT_CONTROL =
        "cayenne.modeler.project.map.button";
    public static final String CANCEL_PROJECT_CREATE_CONTROL =
        "cayenne.modeler.project.cancel.button";

    /**
     * Constructor for ProjectTypeSelectControl.
     */
    public ProjectTypeSelectControl() {
        super();
    }

    /**
     * @see org.scopemvc.controller.basic.BasicController#startup()
     */
    public void startup() {
        setView(new ProjectTypeSelectDialog());
        super.startup();
    }
    
    /**
     * @see org.scopemvc.controller.basic.BasicController#doHandleControl(Control)
     */
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_PROJECT_CREATE_CONTROL)) {
            shutdown();
        } else if (control.matchesID(CREATE_APP_PROJECT_CONTROL)) {
            doCreateAppProject();
        } else if (control.matchesID(CREATE_MAP_PROJECT_CONTROL)) {
            doCreateMapProject();
        }
    }

    protected void doCreateAppProject() {
        Project project = new ApplicationProject(null);

        // send "Project Open" control to the main controller
        Editor.getFrame().getController().handleControl(
            new Control(ModelerController.PROJECT_OPENED_ID, project));

        shutdown();
    }

    protected void doCreateMapProject() {
        Project project = new DataMapProject(null);

        // send "Project Open" control to the main controller
        Editor.getFrame().getController().handleControl(
            new Control(ModelerController.PROJECT_OPENED_ID, project));

        shutdown();
    }
}
