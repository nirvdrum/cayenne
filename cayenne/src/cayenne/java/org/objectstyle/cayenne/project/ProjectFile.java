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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.util.Util;

/**
 * ProjectFile is an adapter from an object in Cayenne project
 * to its representation in the file system.
 * 
 * @author Andrei Adamchik
 */
public abstract class ProjectFile {
	static Logger logObj = Logger.getLogger(ProjectFile.class);
	
    protected static final List fileTypes = new ArrayList();

    protected String location;
    protected File tempFile;
    protected Project project;

    static {
        fileTypes.add(new ApplicationProjectFile());
        fileTypes.add(new DataMapFile());
        fileTypes.add(new DataNodeFile());
    }

    /**
     * Returns a ProjectFile that can handle a given object,
     * or null if no such object can be created. This is a common
     * factory method that takes care of instantiating the right wrapper.
     */
    public static ProjectFile projectFileForObject(Project project, Object obj) {
        for (int i = 0; i < fileTypes.size(); i++) {
            ProjectFile f = (ProjectFile) fileTypes.get(i);
            if (f.canHandle(obj)) {
                return f.createProjectFile(project, obj);
            }
        }
        return null;
    }

    public ProjectFile() {}

    /**
     * Constructor for ProjectFile.
     */
    public ProjectFile(Project project, String location) {
        this.location = location;
        this.project = project;
    }

    /**
     * Builds a filename from the object name and "file suffix".
     */
    public String getLocation() {
        String oName = getObjectName();
        if (oName == null) {
            throw new NullPointerException("Null name.");
        }

        return oName + getLocationSuffix();
    }

    /**
    * Builds a filename from the initial name and extension.
    */
    public String getOldLocation() {
        if (location == null) {
            throw new NullPointerException("Null old name.");
        }
        return location;
    }

    /**
     * Returns suffix to append to object name when 
     * creating a file name. Default implementation 
     * returns empty string.
     */
    public String getLocationSuffix() {
        return "";
    }

    /**
     * Returns a project object associated with this file.
     */
    public abstract Object getObject();

    /**
     * Returns a name of associated object, that is also 
     * used as a file name.
     */
    public abstract String getObjectName();

    /**
     * Saves an underlying object to the file. 
     * The procedure is dependent on the type of
     * object and is implemented by concrete subclasses.
     */
    public abstract void save(PrintWriter out) throws Exception;

    /**
     * Returns true if this file wrapper can handle a
     * specified object.
     */
    public abstract boolean canHandle(Object obj);
    
   /**
     * Returns true if this file wrapper can handle an
     * internally stored object.
     */
    public boolean canHandleObject() {
    	return canHandle(getObject());
    }

    /**
     * Returns an instance of ProjectFile that will handle a 
     * wrapped object. This method is an example of "prototype"
     * pattern, used here due to the lack of Class inheritance in Java.
     */
    public abstract ProjectFile createProjectFile(Project project, Object obj);

    /**
     * Replaces internally stored filename with the current object name.
     */
    public void synchronizeLocation() {
        location = getLocation();
    }

    /**
     * This method is called by project to let file know that
     * it will be saved. Default implementation is a noop.
     */
    public void willSave() {}

    /**
     * Saves ProjectFile's underlying object to a temporary 
     * file, returning this file to the caller. If any problems are 
     * encountered during saving, an Exception is thrown.
     */
    public void saveTemp() throws Exception {
        // cleanup any previous temp files
        if (tempFile != null && tempFile.isFile()) {
            tempFile.delete();
            tempFile = null;
        }

        // check write permissions for the target final file...
        File finalFile = resolveFile();
        checkWritePermissions(finalFile);

        // ...but save to temp file first
        tempFile = tempFileForFile(finalFile);
        FileWriter fw = new FileWriter(tempFile);

        try {
            PrintWriter pw = new PrintWriter(fw);
            try {
                save(pw);
            } finally {
                pw.close();
            }
        } finally {
            fw.close();
        }
    }

    /**
     * Returns a file which is a canonical representation of the 
     * file to store a wrapped object. If an object was renamed, 
     * the <b>new</b> name is returned.
     */
    public File resolveFile() {
        return getProject().resolveFile(getLocation());
    }

    /**
     * Returns a file which is a canonical representation of the 
     * file to store a wrapped object. If an object was renamed, 
     * the <b>old</b> name is returned.
     */
    public File resolveOldFile() {
        return getProject().resolveFile(getOldLocation());
    }

    /**
     * Finishes saving the underlying object.
     */
    public File saveCommit() throws ProjectException {
        File finalFile = resolveFile();
        
        if (tempFile != null) {
            if (finalFile.exists()) {
                if (!finalFile.delete()) {
                    throw new ProjectException(
                        "Unable to remove old master file : " + finalFile);
                }
            }

            if (!tempFile.renameTo(finalFile)) {
                throw new ProjectException(
                    "Unable to move " + tempFile + " to " + finalFile);
            }

            tempFile = null;
        }
        
        return finalFile;
    }

    /**
     * Cleans up after unsuccessful or canceled save attempt.
     */
    public void saveUndo() {
        if (tempFile != null && tempFile.isFile()) {
            tempFile.delete();
            tempFile = null;
        }
    }

    /**
      * Returns the project.
      * @return Project
      */
    public Project getProject() {
        return project;
    }

    public boolean isRenamed() {
        return !Util.nullSafeEquals(location, getLocation());
    }

    /** 
     * Creates a temporary file for the master file.
     */
    protected File tempFileForFile(File f) throws IOException {
        File parent = f.getParentFile();
        String name = f.getName();

        if (name == null || name.length() < 3) {
            name = "cayenne-project";
        }
 
        if(!parent.exists()) {
        	if(!parent.mkdirs()) {
        		throw new IOException("Error creating directory tree: " + parent);
        	}
        }
         
        return File.createTempFile(name, null, parent);
    }

    protected void checkWritePermissions(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("Target file is a directory: " + file);
        }

        if (file.exists() && !file.canWrite()) {
            throw new IOException("Can't write to file: " + file);
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ProjectFile [").append(getClass().getName()).append("]: name = ");
        if (getObject() != null) {
            buf.append("*null*");
        } else {
            buf.append(getObjectName());
        }

        return buf.toString();
    }
}
