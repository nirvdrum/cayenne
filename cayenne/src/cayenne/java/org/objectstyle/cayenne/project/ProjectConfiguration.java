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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.conf.DomainHelper;

/**
 * Subclass of Configuration used in the project model.
 *
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ProjectConfiguration extends DefaultConfiguration {
    static Logger logObj = Logger.getLogger(ProjectConfiguration.class.getName());

    /** Main project file. */
    protected File projectFile;

    public ProjectConfiguration(File projectFile) {
        this.projectFile = projectFile;
        try {
            init();
        } catch (Exception e) {
            throw new ProjectException("Error initializing configuration.", e);
        }
    }

    /** Returns project directory. */
    public File getProjectDir() {
    	String parent = (projectFile != null) ? projectFile.getParent() : null;
        return (parent != null) ? new File(parent) : null;
    }

    /** 
     * Returns domain configuration as a stream or null if it
     * can not be found. 
     */
    public InputStream getDomainConfig() {
        try {
            if (projectFile != null && projectFile.exists() && projectFile.isFile()) {
                return new FileInputStream(projectFile);
            }
        } catch (Exception ex) {
            logObj.warn("Error reading domain configuration", ex);
        }

        return null;
    }

    /** Returns DataMap configuration from a specified location or null if it
      * can not be found. */
    public InputStream getMapConfig(String location) {
        try {
            if (projectFile != null) {
                File mapFile = new File(projectFile.getParent(), location);
                if (mapFile.exists()) {
                    return new FileInputStream(mapFile);
                }
            }
        } catch (Exception ex) {
            logObj.warn("Error reading data map", ex);
        }
        return null;
    }

    /**
     * @see org.objectstyle.cayenne.conf.Configuration#getOverrideFactory()
     */
    protected DataSourceFactory getOverrideFactory() {
        try {
            return new ProjectDataSourceFactory(getProjectDir());
        } catch (Exception e) {
            throw new ProjectException("Error creating DataSourceFactory.", e);
        }
    }
    
    /**
     * Returns the projectFile.
     * @return File
     */
    public File getProjectFile() {
        return projectFile;
    }
}
