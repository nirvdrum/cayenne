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

package org.objectstyle.cayenne.project.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectTraversal;

/** 
 * Used for validating dirty elements in the Mediator.
 * If errors are found, displays them in the dialog window.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class Validator {
    protected Project project;
    protected List validationResults;
    protected int maxSeverity;

    /**
     * Creates a new validator initialized with the project.
     * 
     * @param project
     */
    public Validator(Project project) {
        this.project = project;
    }

    /**
     * Returns the project.
     * @return Project
     */
    public Project getProject() {
        return project;
    }

    /** 
     * Resets internal state. 
     * Called internally before starting validation.
     */
    protected void reset() {
        validationResults = new ArrayList();
        maxSeverity = ValidationResult.VALID;
    }

    /** 
     * Returns maximum severity level encountered during 
     * the last validation run. 
     */
    public int getMaxSeverity() {
        return maxSeverity;
    }

    /**
     * Registers validation result. 
     * Increases internally stored max severity if 
     * <code>result</code> parameter has a higher severity then the current value. 
     * Leaves current value unchanged otherwise.
     */
    public void registerValidated(int severity, String message, Object[] treeNodePath) {
        ValidationResult result = new ValidationResult(severity, message, treeNodePath);
        validationResults.add(result);
        if (maxSeverity < severity) {
            maxSeverity = severity;
        }
    }

    public void registerError(String message, Object[] treeNodePath) {
        registerValidated(ValidationResult.ERROR, message, treeNodePath);
    }

    public void registerWarning(String message, Object[] treeNodePath) {
        registerValidated(ValidationResult.WARNING, message, treeNodePath);
    }

    /** Return collection of ValidationDisplayHandler objects from last validation. */
    public List validationResults() {
        return validationResults;
    }

    /** 
     * Validates all project elements.
     * 
     * @return ValidationResult.VALID if no errors were found, 
     * or an error code of the error with the highest severity 
     * if there were errors.
     */
    public synchronized int validate() {
        reset();

        Iterator it = new ProjectTraversal(project).treeNodes();
        while (it.hasNext()) {
            TreeNodeValidator.validate((Object[]) it.next(), this);
        }

        return getMaxSeverity();
    }
}