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
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;

/**
 * Subclass of Configuration that uses System CLASSPATH to locate resources.
 * If Cayenne classes are loaded using a different ClassLoader from
 * the application classes, this configuration needs to be bootstrapped
 * by calling <code>Configuration.bootstrapSharedConfig(SomeClass.class)</code>.
 * 
 * <p>Alternatively, DefaultConfiguration can be initialized with a config file directly.
 * </p>
 *
 * @author Andrei Adamchik
 */
public class DefaultConfiguration extends Configuration {
	private static Logger logObj = Logger.getLogger(DefaultConfiguration.class);

	protected ResourceLocator locator;
	protected File projectFile;

	public DefaultConfiguration() {
		// configure CLASSPATH-only locator
		locator = new ResourceLocator();
		locator.setSkipAbsolutePath(true);
		locator.setSkipClasspath(false);
		locator.setSkipCurrentDirectory(true);
		locator.setSkipHomeDirectory(true);

		// add the current Configuration subclass' package as additional path.
		if (!(this.getClass().equals(DefaultConfiguration.class))) {
			locator.addClassPath(Util.getPackagePath(this.getClass().getName()));
		}

		// Configuration superclass statically defines what 
		// ClassLoader to use for resources. This
		// allows applications to control where resources 
		// are loaded from.
		locator.setClassLoader(Configuration.getResourceLoader());
	}

	/**
	 * Creates configuration object that uses provided file 
	 * for the main project file, ignoring any other lookup strategies.
	 */
	public DefaultConfiguration(File projectFile) {
		this();
		this.projectFile = projectFile;
		this.locator.addFilesystemPath(this.projectDirectory().getPath());
	}

	public ResourceLocator getResourceLocator() {
		return locator;
	}

	public File projectFile() {
		return projectFile;
	}

	public File projectDirectory() {
		File pfile = this.projectFile();
		if (pfile != null) {
			return pfile.getParentFile();
		}
		else {
			return null;
		}
	}

	/** Returns domain configuration as a stream or null if it
	  * can not be found. This method will look for "cayenne.xml"
	  * file in locations accessible to ClassLoader (in Java CLASSPATH).
	  * This can be a standalone file or an entry in a JAR file. */
	public InputStream getDomainConfiguration() {
		File pfile = this.projectFile();
		if (pfile != null) {
			try {
				return new FileInputStream(pfile);
			}
			catch (Throwable ex) {
	            logObj.warn("Error opening project file.", ex);
	            throw new ConfigurationException("Error opening project file.", ex);
			}
		}
		else {
			return locator.findResourceStream(DEFAULT_DOMAIN_FILE);
		}
	}

	/** Returns DataMap configuration from a specified location or null if it
	  * can not be found. This method will look for resource identified by
	  * <code>location</code> in places accessible to ClassLoader (in Java CLASSPATH).
	  * This can be a standalone file or an entry in a JAR file. */
	public InputStream getMapConfiguration(String location) {
		try {
			File dir = this.projectDirectory();
			if (dir != null) {
				return new FileInputStream(new File(dir, location));
			}
		} catch (Throwable ex) {
            logObj.warn("Error opening map file.", ex);
            throw new ConfigurationException("Error opening map file.", ex);
		}

		return locator.findResourceStream(location);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf
			.append('[')
			.append(this.getClass().getName())
			.append(": classloader=")
			.append(locator.getClassLoader())
			.append(']');
		return buf.toString();
	}
}