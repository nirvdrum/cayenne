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
package org.objectstyle.cayenne.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
  * Utility class to find resources. (Resources are usually files).
  * Lookup is done using preconfigured strategy.
  * 
  * @author Andrei Adamchik
  */
public class ResourceLocator {
	static Logger logObj = Logger.getLogger(ResourceLocator.class.getName());

	protected boolean skipHomeDir;
	protected boolean skipCurDir;
	protected boolean skipClasspath;
	protected boolean skipAbsPath;
	protected ClassLoader classLoader;
	protected static Level logLevel = Level.FINER;

	/** Returns a resource as InputStream if it is found in CLASSPATH. 
	  * Returns null otherwise. Lookup is normally performed in all JAR and
	  * ZIP files and directories available to CLassLoader. */
	public static InputStream findResourceInClasspath(String name) {
		try {
			URL url = findURLInClasspath(name);
			return (url != null) ? url.openStream() : null;
		} catch (IOException ioex) {
			return null;
		}
	}

	/** Returns a resource as InputStream if it is found in the filesystem. 
	  * Returns null otherwise. Lookup is first performed relative to the user 
	  * home directory (as defined by "user.home" system property), and then
	  * relative to the current directory. */
	public static InputStream findResourceInFileSystem(String name) {
		try {
			File f = findFileInFileSystem(name);
			return (f != null) ? new FileInputStream(f) : null;
		} catch (IOException ioex) {
			return null;
		}
	}

	/** Looks up a file in the filesystem. 
	 *  First looks in the user home directory, then in the current directory.
	 *  
	 *  @return file object matching the name, or null if file can not be found
	 *  or if it is not readable.
	 * 
	 *  @see #findFileInHomeDir(String)
	 *  @see #findFileInCurDir(String)
	 */
	public static File findFileInFileSystem(String name) {
		File f = findFileInHomeDir(name);
		return (f != null) ? f : findFileInCurDir(name);
	}

	/** Looks up a file in the user home directory.
	 *  
	 *  @return file object matching the name, or null if file can not be found
	 *  or if it is not readable.
	 */
	public static File findFileInHomeDir(String name) {
		// look in home directory
		String homeDirPath =
			System.getProperty("user.home") + File.separator + name;
		File file = new File(homeDirPath);
		return file.exists() && file.canRead() ? file : null;
	}

	/** Looks up a file in the current directory.
	 *  
	 *  @return file object matching the name, or null if file can not be found
	 *  or if it is not readable.
	 */
	public static File findFileInCurDir(String name) {
		// look in current directory
		File file = new File('.' + File.separator + name);
		return file.exists() && file.canRead() ? file : null;
	}

	/** Looks up for resource using this class ClassLoader. */
	public static URL findURLInClasspath(String name) {
		return findURLInClassLoader(
			name,
			ResourceLocator.class.getClassLoader());
	}

	/** Looks up for resource using specified ClassLoader . */
	public static URL findURLInClassLoader(String name, ClassLoader loader) {
		return loader.getResource(name);
	}

	/** 
	 * Returns a base URL as a String from which this class was loaded.
	 * This is normally a JAR or a file URL, but it is ClassLoader dependent.
	 */
	public static String classBaseUrl(Class aClass) {
		String pathToClass = aClass.getName().replace('.', '/') + ".class";
		URL selfUrl = aClass.getClassLoader().getResource(pathToClass);
		if (selfUrl == null) {
			return null;
		}

		String urlString = selfUrl.toExternalForm();
		return urlString.substring(
			0,
			urlString.length() - pathToClass.length());
	}

	/** Creates new ResourceLocator with default lookup policy including
	 *  user home directory, current directory and CLASSPATH. */
	public ResourceLocator() {
		setClassLoader(this.getClass().getClassLoader());
	}

	/** Returns resource URL using lookup strategy configured for this object or
	 *  null if no readable resource can be found for name. */
	public InputStream findResourceStream(String name) {
		URL url = findResource(name);

		if (logObj.isLoggable(logLevel)) {
			logObj.log(logLevel, "Resource URL: " + url);
		}
		
		if (url == null) {
			return null;
		}

		try {
			return url.openStream();
		} catch (IOException ioex) {
			logObj.log(logLevel, "Error reading URL, ignoring", ioex);
			return null;
		}
	}

	/** 
	 * Returns resource URL using lookup strategy configured 
	 * for this object or null if no readable resource 
	 * can be found for name. 
	 */
	public URL findResource(String name) {
		if (!isSkipAbsPath()) {
			File f = new File(name);
			if (f.isAbsolute() && f.exists()) {
				try {
					return f.toURL();
				} catch (MalformedURLException ex) {
					// ignoring
					logObj.log(logLevel, "Malformed url, ignoring.", ex);
				}
			}
		}

		if (!isSkipHomeDir()) {
			File f = findFileInHomeDir(name);
			if (f != null) {

				try {
					return f.toURL();
				} catch (MalformedURLException ex) {
					// ignoring
					logObj.log(logLevel, "Malformed url, ignoring", ex);
				}
			}
		}

		if (!isSkipCurDir()) {
			File f = findFileInCurDir(name);
			if (f != null) {

				try {
					return f.toURL();
				} catch (MalformedURLException ex) {
					// ignoring
					logObj.log(logLevel, "Malformed url, ignoring", ex);
				}
			}
		}

		if (!isSkipClasspath()) {
			return findURLInClassLoader(name, classLoader);
		}

		return null;
	}

	/** Returns resource URL using lookup strategy configured for this object or
	 *  null if no readable resource can be found for name. Resource returned is
	 *  assumed to be a directory, so URL returned will be in a directory format 
	 *  (with "/" at the end. */
	public URL findDirectoryResource(String name) {
		URL url = findResource(name);
		if (url == null) {
			return null;
		}

		String urlSt = url.toExternalForm();

		try {
			return (urlSt.endsWith("/")) ? url : new URL(urlSt + "/");
		} catch (MalformedURLException ex) {
			ex.printStackTrace();

			// ignoring...
			return null;
		}
	}

	/**
	 * Returns true if no lookups are performed in the user home directory.
	 */
	public boolean isSkipHomeDir() {
		return skipHomeDir;
	}

	/**
	 * Sets "skipHomeDir" property.
	 */
	public void setSkipHomeDir(boolean skipHomeDir) {
		this.skipHomeDir = skipHomeDir;
	}

	/**
	 * Returns true if no lookups are performed in the current directory.
	 */
	public boolean isSkipCurDir() {
		return skipCurDir;
	}

	/**
	 * Sets "skipCurDir" property.
	 */
	public void setSkipCurDir(boolean skipCurDir) {
		this.skipCurDir = skipCurDir;
	}

	/**
	 * Returns true if no lookups are performed in the classpath.
	 */
	public boolean isSkipClasspath() {
		return skipClasspath;
	}

	/**
	 * Sets "skipClasspath" property.
	 */
	public void setSkipClasspath(boolean skipClasspath) {
		this.skipClasspath = skipClasspath;
	}

	/**
	 * Returns the ClassLoader associated with this ResourceLocator.
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/** 
	 * Sets ClassLoader used to locate resources. If null parameter
	 * is passed, ClassLoader of ResourceLocator class will be used.
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader =
			(classLoader != null)
				? classLoader
				: this.getClass().getClassLoader();
	}

	/**
	 * Returns true if no lookups are performed using path as absolute path.
	 */
	public boolean isSkipAbsPath() {
		return skipAbsPath;
	}

	/**
	 * Sets "skipAbsPath" property.
	 */
	public void setSkipAbsPath(boolean skipAbsPath) {
		this.skipAbsPath = skipAbsPath;
	}

	/**
	 * Returns the logLevel.
	 * @return Level
	 */
	public static Level getLogLevel() {
		return logLevel;
	}


	/**
	 * Sets the logLevel.
	 * @param logLevel The logLevel to set
	 */
	public static void setLogLevel(Level logLevel) {
		ResourceLocator.logLevel = logLevel;
	}


}