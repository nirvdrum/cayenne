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
package org.objectstyle.cayenne.conf;

import java.io.File;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.util.ResourceLocator;

/**
 * FileConfiguration loads a Cayenne configuraton file from a given
 * location in the file system.
 *
 * @author Holger Hoffstätte
 */
public class FileConfiguration extends DefaultConfiguration {
	private static Logger logObj = Logger.getLogger(FileConfiguration.class);

	/**
	 * The domain file used for this configuration
	 */
	protected File projectFile;

	/**
	 * Disabled default constructor to force {@link FileConfiguration#FileConfiguration(File)}
	 */
	private FileConfiguration() {
		super();
	}

	/**
	 * Creates a configuration that uses the provided file 
	 * as the main project file, ignoring any other lookup strategies.
	 * @throws ConfigurationException when projectFile is <code>null</code>,
	 * a directory or not readable.
	 * @see DefaultConfiguration#DefaultConfiguration()
	 */
	public FileConfiguration(File projectFile) {
		this();
		logObj.debug("using project file: " + projectFile);

		// set the project file
		this.setProjectFile(projectFile);

		// try again
		if (this.shouldInitialize()) {
			try {
				this.initialize();
			} catch (Exception ex) {
				throw new ConfigurationException(ex);
			}
		}
	}

	/**
	 * Only returns <code>true</code> when {@link #getProjectFile} does not
	 * return <code>null</code>. If so, also creates a {@link ResourceLocator}
	 * configured for files in the file system.
	 */
	protected boolean shouldInitialize() {
		// I can only initialize myself when I have a valid file
		if (this.getProjectFile() != null) {
			// create a new ResourceLocator suitable for plain files
			ResourceLocator l = new ResourceLocator();
			l.setSkipAbsolutePath(false);
			l.setSkipClasspath(true);
			l.setSkipCurrentDirectory(false);
			l.setSkipHomeDirectory(true);

			// normalize the file location & add it to the file search path
			File projectDirectory = this.getProjectDirectory();
			if (projectDirectory != null) {
				l.addFilesystemPath(projectDirectory);
			}
	
			// set the locator
			this.setResourceLocator(l);

			// go!
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Returns the main domain file used for this configuration. 
	 */
	public File getProjectFile() {
		return projectFile;
	}

	/**
	 * Sets the main domain file used for this configuration.
	 * @throws ConfigurationException if <code>projectFile</code> is null,
	 * a directory or not readable.
	 */
	protected void setProjectFile(File projectFile) {
		if (projectFile != null) {
			if (projectFile.isFile()) {
				this.projectFile = projectFile;
				this.setDomainConfigurationName(projectFile.getName());
			}
			else {
				throw new ConfigurationException("Project file: "
													+ projectFile
													+ " is a directory or not readable.");
			}
		}
		else {
			throw new ConfigurationException("Cannot use null as project file.");
		}
	}

	/**
	 * Returns the directory of the current project file as
	 * returned by {@link #getProjectFile}.
	 */
	public File getProjectDirectory() {
		File pfile = this.getProjectFile();
		if (pfile != null) {
			return pfile.getParentFile();
		}
		else {
			return null;
		}
	}

}
