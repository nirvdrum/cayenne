/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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

package org.objectstyle.cayenne.modeler;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;
import org.objectstyle.cayenne.project.CayenneUserDir;

import com.jgoodies.plaf.plastic.PlasticXPLookAndFeel;

/** 
 * Main class responsible for starting CayenneModeler.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class Main {
    private static Logger logObj = Logger.getLogger(Main.class);

    /**
     * Main method that starts the CayenneModeler.
     */
    public static void main(String[] args) {
        // if configured, redirect all logging to the log file
        configureLogging();

        // get preferences
        ModelerPreferences prefs = ModelerPreferences.getPreferences();

        // get L&F
        String laf = prefs.getString(ModelerPreferences.EDITOR_LAFNAME);
        
        try {
        	// set L&F
            UIManager.setLookAndFeel(laf);
        } catch (Exception e) {
            logObj.warn("Could not set selected LookAndFeel '" + laf + "'" + "- using default.");
        } finally {
			// JGoodies plastic L&F is default
			laf = PlasticXPLookAndFeel.class.getName();
			
			// re-try with default
			try {
				UIManager.setLookAndFeel(laf);
			} catch (Exception e) {
				// give up
			}

			// remember L&F
            prefs.setProperty(
                ModelerPreferences.EDITOR_LAFNAME,
                UIManager.getLookAndFeel().getClass().getName());
        }

        // check jdk version
        try {
            Class.forName("javax.swing.SpringLayout");
        } catch (Exception ex) {
            logObj.fatal("CayenneModeler requires JDK 1.4.");
            logObj.fatal(
                "Found : '"
                    + System.getProperty("java.version")
                    + "' at "
                    + System.getProperty("java.home"));

            JOptionPane.showMessageDialog(
                null,
                "Unsupported JDK at "
                    + System.getProperty("java.home")
                    + ". Set JAVA_HOME to the JDK1.4 location.",
                "Unsupported JDK Version",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        CayenneModelerFrame frame = new CayenneModelerFrame();

        logObj.info("Started CayenneModeler.");

        // load project if filename is supplied as an argument
        if (args.length == 1) {
            File f = new File(args[0]);
            if (f.isDirectory()) {
                f = new File(f, Configuration.DEFAULT_DOMAIN_FILE);
            }

            if (f.isFile() && Configuration.DEFAULT_DOMAIN_FILE.equals(f.getName())) {
                OpenProjectAction openAction =
                    (OpenProjectAction) frame.getAction(
                        OpenProjectAction.getActionName());
                openAction.openProject(f);
            }
        }
    }

    /** 
     * Configures Log4J appenders to perform logging to 
     * $HOME/.cayenne/modeler.log.
     */
    public static void configureLogging() {
        // read default Cayenne log configuration
        Configuration.configureCommonLogging();

        // get preferences
        ModelerPreferences prefs = ModelerPreferences.getPreferences();

        // check whether to set up logging to a file
        boolean logfileEnabled =
            prefs.getBoolean(ModelerPreferences.EDITOR_LOGFILE_ENABLED, true);
        prefs.setProperty(
            ModelerPreferences.EDITOR_LOGFILE_ENABLED,
            String.valueOf(logfileEnabled));

        if (logfileEnabled) {
            try {
                // use logfile from preferences or default
                String defaultPath = getLogFile().getPath();
                String logfilePath =
                    prefs.getString(ModelerPreferences.EDITOR_LOGFILE, defaultPath);
                File logfile = new File(logfilePath);

                if (logfile != null) {
                    if (!logfile.exists()) {
                        if (!logfile.createNewFile()) {
                            logObj.warn("Can't create log file, ignoring.");
                            return;
                        }
                    }

                    // remember working path
                    prefs.setProperty(ModelerPreferences.EDITOR_LOGFILE, logfilePath);

                    // replace appenders to just log to a file.
                    Logger p1 = logObj;
                    Logger p2 = null;
                    while ((p2 = (Logger) p1.getParent()) != null) {
                        p1 = p2;
                    }

                    Layout layout =
                        new PatternLayout("CayenneModeler %-5p [%t %d{MM-dd HH:mm:ss}] %c: %m%n");
                    p1.removeAllAppenders();
                    p1.addAppender(
                        new FileAppender(layout, logfile.getCanonicalPath(), true));
                }
            } catch (IOException ioex) {
                logObj.warn("Error setting logging.", ioex);
            }
        }
    }

    /** 
     * Returns a file correspinding to $HOME/.cayenne/modeler.log
     */
    public static File getLogFile() {
        if (!CayenneUserDir.getInstance().canWrite()) {
            return null;
        }

        return CayenneUserDir.getInstance().resolveFile("modeler.log");
    }
}