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

import org.objectstyle.cayenne.util.Util;

/**
 * Represents a file in a Cayenne project.
 * File is characterized by a symbolic OS-neutral 
 * name, that can be resolved to a file in the file system
 * by calling <code>Project.resolveFile()</code>.
 * 
 * @author Andrei Adamchik
 */
public abstract class ProjectFile {
    protected String name;
    protected String extension;
    protected boolean objectModified;

    /**
     * Utility method to extract extension out of file name.
     */
    public static String extractExtension(String fullName) {
     	int dotInd = fullName.lastIndexOf('.');
    	
    	// if dot is in the first position,
    	// we are dealing with a hidden file rather than an extension
        return (dotInd > 0 && dotInd < fullName.length()) ? fullName.substring(dotInd + 1) : null;
    }


    /**
     * Utility method to remove extension out of a file name.
     */
    public static String stripExtension(String fullName) {
    	int dotInd = fullName.lastIndexOf('.');
    	
    	// if dot is in the first position,
    	// we are dealing with a hidden file rather than an extension
        return (dotInd > 0) ? fullName.substring(0, dotInd) : fullName;
    }

    
    /**
     * Constructor for ProjectFile.
     */
    public ProjectFile(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }
    
    /**
     * Returns the objectModified.
     * @return boolean
     */
    public boolean isObjectModified() {
        return objectModified;
    }


    /**
     * Sets the objectModified.
     * @param objectModified The objectModified to set
     */
    public void setObjectModified(boolean objectModified) {
        this.objectModified = objectModified;
    }
    

    public String getFileName() {
        return (extension != null) ? name + '.' + extension : name;
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
    public abstract void saveToFile(File f) throws Exception;
    
    /**
     * Saves ProjectFile's underlying object to a temporary 
     * file, returning this file to the caller. If any problems are 
     * encountered during saving, a ProjectException is thrown.
     */
    public File saveTemp() throws ProjectException {
    	return null;
    }
    
    /**
     * Finishes saving the underlying object.
     */
    public void saveCommit(File tempFile) {
    	
    }
    
    /**
     * Cleans up after unsuccessful or canceled save attempt.
     */
    public void saveUndo(File tempFile) {
    	
    }
    
    /**
     * Returns true if renaming a file is required 
     * as a part of save operation.
     */
    public boolean isRenamed() {
    	return Util.nullSafeEquals(getObjectName(), name);
    }
    
    public boolean needsSave() {
    	return isObjectModified() || isRenamed();
    }    
}
