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
package org.objectstyle.cayenne.modeler.control;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.action.CreateDataMapAction;
import org.objectstyle.cayenne.modeler.action.CreateDomainAction;
import org.objectstyle.cayenne.modeler.action.CreateNodeAction;
import org.objectstyle.cayenne.modeler.action.ImportDataMapAction;
import org.objectstyle.cayenne.modeler.action.ImportDbAction;
import org.objectstyle.cayenne.modeler.action.ImportEOModelAction;
import org.objectstyle.cayenne.modeler.action.ProjectAction;
import org.objectstyle.cayenne.modeler.action.RemoveAction;
import org.objectstyle.cayenne.modeler.action.SaveAction;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.project.ProjectPath;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @author Andrei Adamchik
 */
public class ActionController extends ModelerController {

    /**
     * Constructor for ActionController.
     */
    public ActionController(ModelerController parent) {
        super(parent);
    }

    /**
     * Performs control handling, invoking appropriate action method.
     */
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(PROJECT_OPENED_ID)) {
            projectOpened();
        } else if (control.matchesID(PROJECT_CLOSED_ID)) {
            projectClosed();
        } else if (control.matchesID(DATA_DOMAIN_SELECTED_ID)) {
            domainSelected((DataDomain) control.getParameter());
        }
    }

    protected void domainSelected(DataDomain domain) {
        enableDataDomainActions(domain);
        updateRemoveAction(domain);
    }

    protected void projectOpened() {
        enableProjectActions();
        updateRemoveAction(null);
    }

    protected void projectClosed() {
        disableAllActions();
		getAction(ValidateAction.getActionName()).setEnabled(false);
		getAction(ProjectAction.getActionName()).setEnabled(false);
        getAction(SaveAction.getActionName()).setEnabled(false);
        getAction(CreateDomainAction.getActionName()).setEnabled(false);

        updateRemoveAction(null);
    }

    /**
     * Updates the name of "Remove" action based on the current model state.
     */
    protected void updateRemoveAction(Object selected) {
        String name = null;

        if (selected == null) {
            name = "Remove";
        } else if (selected instanceof DataDomain) {
            name = "Remove DataDomain";
        } else if (selected instanceof DataMap) {
            name = "Remove DataMap";
        } else if (selected instanceof DbEntity) {
            name = "Remove DbEntity";
        } else if (selected instanceof ObjEntity) {
            name = "Remove ObjEntity";
        } else if (selected instanceof DbAttribute) {
            name = "Remove DbAttribute";
        } else if (selected instanceof ObjAttribute) {
            name = "Remove ObjAttribute";
        } else if (selected instanceof DbRelationship) {
            name = "Remove DbRelationship";
        } else if (selected instanceof ObjRelationship) {
            name = "Remove ObjRelationship";
        } else {
            name = "Remove";
        }

        getAction(RemoveAction.getActionName()).setName(name);
    }

    /**
     * Returns an action object for the specified key.
     */
    protected CayenneAction getAction(String key) {
        return (CayenneAction) getTopModel().getActionMap().get(key);
    }

    /** 
     * Disables all controlled actions.
     */
    protected void disableAllActions() {
        // disable everything we can
        Object[] keys = getTopModel().getActionMap().allKeys();
        int len = keys.length;
        for (int i = 0; i < len; i++) {

            // "save" button has its own rules
            if (keys[i].equals(SaveAction.getActionName())) {
                continue;
            }

            getTopModel().getActionMap().get(keys[i]).setEnabled(false);
        }
    }

    /**
     * Configures actions to support an active project.
     */
    protected void enableProjectActions() {
        disableAllActions();
        getAction(CreateDomainAction.getActionName()).setEnabled(true);
		getAction(ProjectAction.getActionName()).setEnabled(true);
		getAction(ValidateAction.getActionName()).setEnabled(true);
    }

    /**
     * Updates actions "on/off" state for the selected project path.
     */
    protected void updateActions(ProjectPath objectPath) {
        Object[] keys = getTopModel().getActionMap().allKeys();
        int len = keys.length;
        for (int i = 0; i < len; i++) {
            CayenneAction action =
                (CayenneAction) getTopModel().getActionMap().get(keys[i]);

            action.setEnabled(action.enableForPath(objectPath));
        }
    }

    /**
     * Configures actions to support an active DataDomain.
     */
    protected void enableDataDomainActions(DataDomain domain) {
        enableProjectActions();

        if (domain != null) {
            getAction(ImportDataMapAction.getActionName()).setEnabled(true);
            getAction(CreateDataMapAction.getActionName()).setEnabled(true);
            getAction(RemoveAction.getActionName()).setEnabled(true);
            getAction(CreateNodeAction.getActionName()).setEnabled(true);
            getAction(ImportDbAction.getActionName()).setEnabled(true);
            getAction(ImportEOModelAction.getActionName()).setEnabled(true);
        }
    }
}
