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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.ConfigStatus;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.map.DataMap;

/**
 * @author Andrei Adamchik
 */
public class ApplicationProject extends Project {
    protected ProjectConfiguration config;

    /**
     * Constructor for ApplicationProject.
     * 
     * @param projectFile
     */
    public ApplicationProject(File projectFile) {
        super(projectFile);
    }

    /**
     * Initializes internal <code>config</code> object and then calls super.
     */
    protected void postInit(File projectFile) {
        try {
            File f = (projectFile != null) ? projectFile.getCanonicalFile() : null;
            config = new ProjectConfiguration(f);
        } catch (IOException e) {
            throw new ProjectException("Error creating ApplicationProject.", e);
        }
        super.postInit(projectFile);
    }

    /**
    * Returns Cayenne configuration object associated with this project. 
    */
    public Configuration getConfig() {
        return (Configuration) config;
    }

    /**
    * Sets Cayenne configuration object associated with this project. 
    */
    public void setConfig(ProjectConfiguration config) {
        this.config = config;
    }

    public void checkForUpgrades() {
        if (hasRenamedFiles()) {
            upgradeMessages.add("Some files require renaming");
        }
    }

    /**
     * @see org.objectstyle.cayenne.project.Project#getChildren()
     */
    public List getChildren() {
        return new ArrayList(this.getConfig().getDomains());
    }

    /**
    * Returns appropriate ProjectFile or null if object does not require 
    * a file of its own. In case of ApplicationProject, the nodes 
    * that require separate filed are: the project itself, each DataMap, each 
    * driver DataNode.
    */
    public ProjectFile projectFileForObject(Object obj) {
        if (requiresProjectFile(obj)) {
            return new ApplicationProjectFile(this);
        } else if (requiresMapFile(obj)) {
            return new DataMapFile(this, (DataMap) obj);
        } else if (requiresNodeFile(obj)) {
            return new DataNodeFile(this, (DataNode) obj);
        }

        return null;
    }

    protected boolean requiresProjectFile(Object obj) {
        return obj == this;
    }

    protected boolean requiresMapFile(Object obj) {
        return obj instanceof DataMap;
    }

    protected boolean requiresNodeFile(Object obj) {
        if (obj instanceof DataNode) {
            DataNode node = (DataNode) obj;

            // only driver datasource factory requires a file
            if (DriverDataSourceFactory
                .class
                .getName()
                .equals(node.getDataSourceFactory())) {
                return true;
            }
        }

        return false;
    }
    

    public ConfigStatus getLoadStatus() {
        return (config != null) ? config.getLoadStatus() : new ConfigStatus();
    }
}
