/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.io.IOException;

import org.apache.log4j.Logger;


/**
 * Opens a URL in the system default browser. Code is based on 
 * <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">JavaWorld
 * article</a> by Steven Spencer.
 * 
 * @author Andrei Adamchik
 */
public class BrowserControl {
	private static Logger logObj = Logger.getLogger(BrowserControl.class);	

	// Used to identify the windows platform.
	private static final String WIN_ID = "Windows";
	// The default system browser under windows.
	private static final String WIN_PATH = "rundll32";
	// The flag to display a url.
	private static final String WIN_FLAG = "url.dll,FileProtocolHandler";
	// The default browser under unix.
	// private static final String UNIX_PATH = "netscape";
	// The flag to display a url.
	// private static final String UNIX_FLAG = "-remote openURL";
	
	
	/**
	 * Display a file in the system browser.  If you want to display a
	 * file, you must include the absolute path name.
	 *
	 * @param url the file's url (the url must start with either "http://"
	 * or "file://").
	 */
	public static void displayURL(String url) {
		boolean windows = isWindowsPlatform();
		String cmd = null;
		try {
			if (windows) {
				// cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
				cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
				Runtime.getRuntime().exec(cmd);
			} else {
				// Under Unix, Netscape has to be running for the "-remote"
				// command to work.  So, we try sending the command and
				// check for an exit value.  If the exit command is 0,
				// it worked, otherwise we need to start the browser.
				// cmd = 'netscape -remote openURL(http://www.javaworld.com)'
			/*	cmd = UNIX_PATH + " " + UNIX_FLAG + "(" + url + ")";
				Process p = Runtime.getRuntime().exec(cmd);
				try {
					// wait for exit code -- if it's 0, command worked,
					// otherwise we need to start the browser up.
					int exitCode = p.waitFor();
					if (exitCode != 0) {
						// Command failed, start up the browser
						// cmd = 'netscape http://www.javaworld.com'
						cmd = UNIX_PATH + " " + url;
						p = Runtime.getRuntime().exec(cmd);
					}
				} catch (InterruptedException x) {
					System.err.println(
						"Error bringing up browser, cmd='" + cmd + "'");
					System.err.println("Caught: " + x);
				} */
			}
		} catch (IOException ex) {
			// couldn't exec browser
			logObj.debug("Could not invoke browser, command=" + cmd, ex);
		}
	}
	/**
	 * Try to determine whether this application is running under Windows
	 * or some other platform by examing the "os.name" property.
	 *
	 * @return true if this application is running under a Windows OS
	 */
	public static boolean isWindowsPlatform() {
		String os = System.getProperty("os.name");
		if (os != null && os.startsWith(WIN_ID))
			return true;
		else
			return false;

	}
}