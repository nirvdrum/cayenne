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
package org.objectstyle.cayenne.gui;

import java.io.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.conf.DomainHelper;

/**
 * Subclass of Configuration that uses System CLASSPATH to locate resources.
 * It also stores project file location.
 *
 * @author Misha Shengaout
 */
public class GuiConfiguration extends DefaultConfiguration {
	static Logger logObj =
		Logger.getLogger(DefaultConfiguration.class.getName());

	private File projFile;
	private static GuiConfiguration guiConfig;

	private GuiConfiguration() {
	}

	public static void initSharedConfig(File proj_file) throws Exception {
		initSharedConfig(proj_file, true);
	}

	/** Create configuration obj and initialize its state.
	 * @param proj_file Project file to work with
	 * @param do_init If true, parse project file*/
	public static void initSharedConfig(File proj_file, boolean do_init)
		throws Exception {
		guiConfig = new GuiConfiguration();
		guiConfig.projFile = proj_file;
		if (do_init)
			guiConfig.init();
	}

	/** Should never be called before initSharedConfig(). */
	public static GuiConfiguration getGuiConfig() {
		return guiConfig;
	}

	public File getProjFile() {
		return projFile;
	}

	/** Returns project directory (without terminating separator). */
	public String getProjDir() {
		if (null == projFile)
			return null;
		return projFile.getParent();
	}

	/** Returns domain configuration as a stream or null if it
	  * can not be found. */
	public InputStream getDomainConfig() {
		try {
			if (null != projFile && projFile.exists() && projFile.isFile())
				return new FileInputStream(projFile);
			else
				super.getDomainConfig();
		} catch (Exception e) {
			logObj.log(Level.WARNING, "Error", e);
		}
		return null;
	}

	/** Returns DataMap configuration from a specified location or null if it
	  * can not be found. */
	public InputStream getMapConfig(String location) {
		try {
			if (null == projFile)
				return super.getMapConfig(location);
			String file_name =
				projFile.getParent() + projFile.separator + location;
			File map_file = new File(file_name);
			if (map_file.exists())
				return new FileInputStream(map_file);
			else
				return super.getMapConfig(location);
		} catch (Exception e) {
			logObj.log(Level.WARNING, "Error", e);
		}
		return null;
	}

	/** 
	 * Initializes all Cayenne resources. Loads all configured domains and their
	 * data maps, initializes all domain Nodes and their DataSources using
	 * GuiDataSourceFactory. 
	 */
	public void init() throws java.lang.Exception {
		InputStream in = getDomainConfig();
		if (in == null)
			throw new ConfigException(
				"Domain configuration file \""
					+ DOMAIN_FILE
					+ "\" is not found.");

		DomainHelper helper = new DomainHelper(this, getLogLevel());
		if (!helper.loadDomains(in, new GuiDataSourceFactory())) {
			throw new ConfigException("Failed to load domain and/or its maps/nodes.");
		}

		Iterator it = helper.getDomains().iterator();
		while (it.hasNext()) {
			addDomain((DataDomain) it.next());
		}
	}

}
