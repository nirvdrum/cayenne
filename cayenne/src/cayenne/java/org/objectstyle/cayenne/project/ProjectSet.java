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

package org.objectstyle.cayenne.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a set of projects.
 * 
 * @author Andrei Adamchik
 */
public class ProjectSet {
    protected List projects = Collections.synchronizedList(new ArrayList());
    protected Project currentProject;

    /**
     * Returns the first project matching the name, if any.
     */
    public Project getProject(String name) {
    	if(name == null) {
    		return null;
    	}
    	
        synchronized (projects) {
            Iterator it = projects.iterator();
            while (it.hasNext()) {
            	Project p = (Project)it.next();
            	if(name.equals(p.getName())) {
            		return p;
            	}
            }
        }
        return null;
    }

    public void addProject(Project project) {
        if (project == null) {
            throw new NullPointerException("Null project.");
        }

        synchronized (projects) {
            projects.add(project);
        }
    }

    public void removeProject(Project project) {
        synchronized (projects) {
            projects.remove(project);
            if (project == currentProject) {
                currentProject = null;
            }
        }
    }

    public List allProjects() {
        synchronized (projects) {
            ArrayList list = new ArrayList();
            list.addAll(projects);
            return list;
        }
    }

    /**
     * Returns the currentProject.
     * @return Project
     */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
     * Sets the currentProject.
     * @param currentProject The currentProject to set
     */
    public void setCurrentProject(Project currentProject) {
        synchronized (projects) {
            this.currentProject = currentProject;
            if (currentProject != null && !projects.contains(currentProject)) {
                projects.add(currentProject);
            }
        }
    }

    /**
     * Factory method to create new projects.
     */
    public Project createProject(String name, File f, boolean makeCurrent) {
        Project project = new Project(name, f);

        addProject(project);
        if (makeCurrent) {
            setCurrentProject(project);
        }

        return project;
    }
}
