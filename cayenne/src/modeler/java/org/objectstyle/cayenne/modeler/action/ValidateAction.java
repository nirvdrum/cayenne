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
 
 package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.validator.ValidationDisplayHandler;
import org.objectstyle.cayenne.modeler.validator.ValidatorDialog;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.project.validator.Validator;

/**
 * UI action that performs full project validation.
 * 
 * @author Craig Miskell
 */
public class ValidateAction extends CayenneAction {

	public static String getActionName() {
		return "Validate Project";
	}

	public ValidateAction() {
		super(getActionName());
	}

	/**
	 * Validates project for possible conflits and incomplete mappings.
	 */
	public void performAction(ActionEvent e) {
		EventController mediator = getMediator();
		Validator val = Editor.getProject().getValidator();
		int validationCode = val.validate();

		// If there were errors or warnings at validation, display them
		if (validationCode >= ValidationDisplayHandler.WARNING) {
			ValidatorDialog.showDialog(Editor.getFrame(), mediator, val);
		}
		else {
			ValidatorDialog.showValidationSuccess(Editor.getFrame(), mediator, val);
		}
	}
	
	/**
	* Returns <code>true</code> if path contains a Project object 
	* and the project is modified.
	*/
	public boolean enableForPath(ProjectPath path) {
		if (path == null) {
			return false;
		}

		Project project = (Project)path.firstInstanceOf(Project.class);
		return project != null;
	}
}
