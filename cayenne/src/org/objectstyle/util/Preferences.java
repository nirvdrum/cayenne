package org.objectstyle.util;

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

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.collections.ExtendedProperties;


public class Preferences extends ExtendedProperties {
    static final Logger logObj = Logger.getLogger(Preferences.class.getName());
    
	/** Directory for preferences in User home. */
	private static final String PREF_DIR = "cayenne";
	/** Name of the preferences file in the CAYENNE_PREF_DIR.
	  * General standard for keys in the preferences:
	  * Use class name (optionally with the package name) and 
	  * the name of the field which uses this preference. */
	private static final String PREF = ".preferences";

	/* Keys for the preference file. */

	/** The directory of the cayenne project edited last. */
	public static final String LAST_DIR = "Editor.lastProject";
	/** User name */
	public static final String USER_NAME = "DbLoginPanel.unInput";
	/** JDBC Driver Class */
	public static final String JDBC_DRIVER = "DbLoginPanel.drInput";
	/** Database URL */
	public static final String DB_URL = "DbLoginPanel.urlInput";
	/** RDBMS Adapter */
	public static final String RDBMS_ADAPTER = "DbLoginPanel.adapterInput";
	
	private static Preferences preferences;


	private Preferences() {super();}
	
	public static Preferences getPreferences() {
		if (null == preferences) {
			preferences = new Preferences();
			preferences.loadPreferences(null);
		}
		return preferences;
	}
	
	/** Store preferences.
	  * Preferences stored to User Home\cayenne\.preferences file. 
	  * @param frame Used for JOptionPane dialogs. If null, default frame used.*/
	public void storePreferences(JFrame frame) {
		logObj.fine("Storing preferences");
		String home_dir = System.getProperty("user.home");
		if (null == home_dir)
			home_dir = "";
		String pref_dir = home_dir + File.separator + PREF_DIR 
						+ File.separator + PREF;
		File pref_file = new File(pref_dir);
		try {
			if (!pref_file.exists()) {
				logObj.fine("Cannot save preferences - file " 
									+ pref_dir + " does not exist");
				return;
			}
			save(new FileOutputStream(pref_file), "");
		} catch (IOException e) {
			logObj.log(Level.INFO, "Error saving preferences: ", e.getMessage());
		}
	}
	
	/** Store preferences.
	  * Preferences stored to User Home\cayenne\.preferences file. 
	  * @param frame Used for JOptionPane dialogs. If null, default frame used.*/
	public void loadPreferences(JFrame frame) {
		String home_dir = System.getProperty("user.home");
		if (null == home_dir) {
			JOptionPane.showMessageDialog(frame
							, "User home directory is not specified. "
							+ " Loading from current directory");
			home_dir = "";
		}
		String pref_dir = home_dir + File.separator + PREF_DIR;
		logObj.fine("Preferences dir path is " + pref_dir);
		File pref_dir_file = new File(pref_dir);
		try {
			if (!pref_dir_file.exists()) {
				if (false == pref_dir_file.mkdir()) {
					JOptionPane.showMessageDialog(frame
							, "Error creating preferences directory. ");
					return;
				}
			}
			String pref_file_name = pref_dir + File.separator + PREF;
			File pref_file = new File(pref_file_name);
			if (!pref_file.exists()) {
				if (false == pref_file.createNewFile()) {
					JOptionPane.showMessageDialog(frame
							, "Error creating preferences file. ");
					return;
				}
			}
			load(new FileInputStream(pref_file));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame
							, "Error loading preferences. Preferences ignored. ");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
}// End class Preferences