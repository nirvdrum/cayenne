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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;

/**
 * Describes a model of Cayenne project. Project is a set of 
 * files in the filesystem describing storing Cayenne DataMaps,
 * DataNodes and other information.
 * 
 * <p>Project has a project directory, which is a canonical directory. 
 * All project files are relative to the project directory.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class Project {
    static Logger logObj = Logger.getLogger(Project.class);

    protected String name;
    protected Configuration config;
    protected File projectDir;
    protected List files = Collections.synchronizedList(new ArrayList());

    /**
     * Constructor for Project. <code>projectFile</code> must denote 
     * a file (existent or non-existent) in an existing directory. 
     * If projectFile has no parent directory, current directory is assumed.
     */
    public Project(String name, File projectFile) {
        this.name = name;

        File parent = projectFile.getParentFile();
        if (parent == null) {
            parent = new File(System.getProperty("user.dir"));
        }

        if (!parent.isDirectory()) {
            throw new ProjectException(
                "Project directory does not exist or is not a directory: " + parent);
        }

        try {
            this.projectDir = parent.getCanonicalFile();
            this.config = new ProjectConfiguration(projectFile.getCanonicalFile());
        } catch (IOException e) {
            throw new ProjectException("Error creating project.", e);
        }
    }

    public boolean removeFileForObject(Object obj) {
        ProjectFile f = findFile(obj);
        if (f == null) {
            return false;
        }

        files.remove(f);
        return true;
    }

    public ProjectFile findFile(Object obj) {
        if (obj == null) {
            return null;
        }

        // to avoid full scan, a map may be a better 
        // choice of collection here, 
        // though normally projects have very few files...
        Iterator it = files.iterator();
        while (it.hasNext()) {
            ProjectFile file = (ProjectFile) it.next();
            if (file.getObject() == obj) {
                return file;
            }
        }

        return null;
    }

    /**
     * Returns a canonical file built from symbolic name.
     */
    public File resolveFile(String symbolicName) {
        try {
            // substitute to Windows backslashes if needed
            if (File.separatorChar != '/') {
                symbolicName = symbolicName.replace('/', File.separatorChar);
            }
            return new File(projectDir, symbolicName).getCanonicalFile();
        } catch (IOException e) {
            // error converting path
            logObj.info("Can't convert to canonical form.", e);
            return null;
        }
    }

    public File resolveFile(ProjectFile file) {
        return resolveFile(file.getFileName());
    }

    /**
      * Returns a "symbolic" name of a file. Returns null if file 
      * is invalid. Symbolic name is a string path of a file relative
      * to the project directory. It is built in a platform independent
      * fashion.
      */
    public String resolveSymbolicName(File file) {
        String symbolicName = null;
        try {
            if (file.isAbsolute()) {
                // accept absolute files only when 
                // they are in the project directory
                String otherPath = file.getCanonicalFile().getPath();
                String thisPath = projectDir.getPath();

                // invalid absolute pathname, can't continue
                if (otherPath.length() + 1 <= thisPath.length()
                    || !otherPath.startsWith(thisPath)) {
                    return null;
                }

                symbolicName = otherPath.substring(thisPath.length() + 1);
            }

            // substitute Windows backslashes if needed
            if (File.separatorChar != '/') {
                symbolicName = symbolicName.replace(File.separatorChar, '/');
            }

            return symbolicName;

        } catch (IOException e) {
            // error converting path
            logObj.info("Can't convert to canonical form.", e);
            return null;
        }
    }

    /**
     * Returns Cayenne configuration object associated with this project. 
     */
    public Configuration getConfig() {
        return config;
    }
    
    /**
     * Sets Cayenne configuration object associated with this project. 
     */
    public void setConfig(Configuration config) {
        this.config = config;
    }
    

    /** 
     * Returns project directory. This is a directory where
     * project file is located.
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * Returns project name.
     */
    public String getName() {
        return name;
    }

    public DataDomain[] getDomains() {
        List domains = getConfig().getDomainList();
        if (domains == null) {
            return new DataDomain[0];
        }
        return (DataDomain[]) domains.toArray(new DataDomain[domains.size()]);
    }

    /**
     * Method getMainProjectFile.
     * @return File
     */
    public File getMainProjectFile() {
        return ((ProjectConfiguration) config).getProjectFile();
    }
}
