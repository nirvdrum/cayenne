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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.modeler.action.AboutAction;
import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.action.CreateAttributeAction;
import org.objectstyle.cayenne.modeler.action.CreateDataMapAction;
import org.objectstyle.cayenne.modeler.action.CreateDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDomainAction;
import org.objectstyle.cayenne.modeler.action.CreateNodeAction;
import org.objectstyle.cayenne.modeler.action.CreateObjEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateRelationshipAction;
import org.objectstyle.cayenne.modeler.action.CreateStoredProcedureAction;
import org.objectstyle.cayenne.modeler.action.DerivedEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.ExitAction;
import org.objectstyle.cayenne.modeler.action.GenerateClassesAction;
import org.objectstyle.cayenne.modeler.action.GenerateDbAction;
import org.objectstyle.cayenne.modeler.action.ImportDbAction;
import org.objectstyle.cayenne.modeler.action.ImportEOModelAction;
import org.objectstyle.cayenne.modeler.action.NewProjectAction;
import org.objectstyle.cayenne.modeler.action.ObjEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;
import org.objectstyle.cayenne.modeler.action.PackageMenuAction;
import org.objectstyle.cayenne.modeler.action.ProjectAction;
import org.objectstyle.cayenne.modeler.action.RemoveAction;
import org.objectstyle.cayenne.modeler.action.SaveAction;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.modeler.control.ModelerController;
import org.objectstyle.cayenne.modeler.control.TopController;
import org.objectstyle.cayenne.modeler.event.AttributeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbAttributeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbRelationshipDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjAttributeDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjRelationshipDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;
import org.objectstyle.cayenne.modeler.util.ModelerStrings;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.objectstyle.cayenne.modeler.util.RecentFileMenu;
import org.objectstyle.cayenne.modeler.view.StatusBarView;
import org.objectstyle.cayenne.project.CayenneUserDir;
import org.objectstyle.cayenne.project.Project;
import org.scopemvc.core.Control;
import org.scopemvc.util.UIStrings;

/** 
 * Main frame of CayenneModeler. Responsibilities include 
 * coordination of enabling/disabling of menu and toolbar.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class Editor
    extends JFrame
    implements
        DataNodeDisplayListener,
        DataMapDisplayListener,
        ObjEntityDisplayListener,
        DbEntityDisplayListener,
        ObjAttributeDisplayListener,
        DbAttributeDisplayListener,
        ObjRelationshipDisplayListener,
        DbRelationshipDisplayListener,
        ProcedureDisplayListener {
    private static Logger logObj = Logger.getLogger(Editor.class);

    /** 
     * Label that indicates as a part of the title that
     * the project has unsaved changes. 
     */
    public static final String DIRTY_STRING = "* - ";

    protected static Editor frame;

    protected EditorView view;
    protected RecentFileMenu recentFileMenu = new RecentFileMenu("Recent Files");
    protected TopController controller;

    private ModelerPreferences prefs;

    /** Returns an editor singleton object. */
    public static Editor getFrame() {
        return frame;
    }

    /**
     * Main method that starts the CayenneModeler.
     */
    public static void main(String[] args) {
        // if configured, redirect all logging to the log file
        configureLogging();

        // get preferences
        ModelerPreferences prefs = ModelerPreferences.getPreferences();

        // set L&F
        try {
            String laf = (String) prefs.getString(ModelerPreferences.EDITOR_LAFNAME);

            if (laf != null) {
                LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
                for (int i = 0; i < installed.length; i++) {
                    LookAndFeelInfo lif = installed[i];
                    if (lif.getName().equals(laf)) {
                        UIManager.setLookAndFeel(lif.getClassName());
                        break;
                    }
                }
            }
            else {
                // no L&F set - use native platform look
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        }
        catch (Exception e) {
            logObj.warn("Could not set selected LookAndFeel - using default.", e);
        }
        finally {
            // remember L&F in prefs
            prefs.setProperty(
                ModelerPreferences.EDITOR_LAFNAME,
                UIManager.getLookAndFeel().getName());
        }

        // check jdk version
        try {
            Class.forName("javax.swing.SpringLayout");
        }
        catch (Exception ex) {
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

        Editor frame = new Editor();

        logObj.info("Started CayenneModeler.");

        // load project if filename is supplied as an argument
        if (args.length == 1) {
            File f = new File(args[0]);
            if (f.isDirectory()) {
                f = new File(f, Configuration.DEFAULT_DOMAIN_FILE);
            }

            if (f.isFile() && Configuration.DEFAULT_DOMAIN_FILE.equals(f.getName())) {
                OpenProjectAction openAction =
                    (OpenProjectAction) frame.getAction(OpenProjectAction.ACTION_NAME);
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
            }
            catch (IOException ioex) {
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

    /**
     * Returns a project that is currently a current project of an 
     * Editor singleton instance. This will be changed if CayenneModeler
     * ever starts supporting multiple open projects.
     */
    public static Project getProject() {
        return getFrame().controller.getTopModel().getCurrentProject();
    }

    public Editor() {
        super(ModelerConstants.TITLE);

        controller = new TopController(this);
        frame = this;

        // force Scope to use CayenneModeler properties
        UIStrings.setPropertiesName(ModelerStrings.DEFAULT_MESSAGE_BUNDLE);

        ModelerContext.setupContext();

        initMenus();
        initToolbar();
        initStatusBar();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                ((ExitAction) getAction(ExitAction.ACTION_NAME)).exit();
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (e.getComponent() == Editor.this) {
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_WIDTH,
                        String.valueOf(Editor.this.getWidth()));
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_HEIGHT,
                        String.valueOf(Editor.this.getHeight()));
                }
            }

            public void componentMoved(ComponentEvent e) {
                if (e.getComponent() == Editor.this) {
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_X,
                        String.valueOf(Editor.this.getX()));
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_Y,
                        String.valueOf(Editor.this.getY()));
                }
            }
        });

        prefs = ModelerPreferences.getPreferences();

        int newWidth = prefs.getInt(ModelerPreferences.EDITOR_FRAME_WIDTH, 650);
        int newHeight = prefs.getInt(ModelerPreferences.EDITOR_FRAME_HEIGHT, 550);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (newHeight > screenSize.height) {
            newHeight = screenSize.height;
        }

        if (newWidth > screenSize.width) {
            newWidth = screenSize.width;
        }

        this.setSize(newWidth, newHeight);

        int defaultX = (screenSize.width - newWidth) / 2;
        int newX = prefs.getInt(ModelerPreferences.EDITOR_FRAME_X, defaultX);
        int defaultY = (screenSize.height - newHeight) / 2;
        int newY = prefs.getInt(ModelerPreferences.EDITOR_FRAME_Y, defaultY);

        frame.setLocation(newX, newY);
        frame.setVisible(true);

        this.controller.startup();
    }

    /**
     * Returns an action object associated with the key.
     */
    public CayenneAction getAction(String key) {
        return (CayenneAction) controller.getTopModel().getAction(key);
    }

    protected void initMenus() {
        getContentPane().setLayout(new BorderLayout());

        // Setup menu bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        JMenu projectMenu = new JMenu("Project");
        JMenu toolMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        menuBar.add(fileMenu);
        menuBar.add(projectMenu);
        menuBar.add(toolMenu);
        menuBar.add(helpMenu);

        fileMenu.add(getAction(NewProjectAction.ACTION_NAME).buildMenu());
        fileMenu.add(getAction(OpenProjectAction.ACTION_NAME).buildMenu());
        fileMenu.add(getAction(ProjectAction.ACTION_NAME).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(getAction(SaveAction.ACTION_NAME).buildMenu());
        fileMenu.add(getAction(ValidateAction.ACTION_NAME).buildMenu());
        fileMenu.addSeparator();

        recentFileMenu.rebuildFromPreferences();
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(getAction(ExitAction.ACTION_NAME).buildMenu());

        projectMenu.add(getAction(CreateDomainAction.ACTION_NAME).buildMenu());
        projectMenu.add(getAction(CreateNodeAction.ACTION_NAME).buildMenu());
        projectMenu.add(getAction(CreateDataMapAction.ACTION_NAME).buildMenu());

        projectMenu.add(getAction(CreateObjEntityAction.ACTION_NAME).buildMenu());
        projectMenu.add(getAction(CreateDbEntityAction.ACTION_NAME).buildMenu());
        projectMenu.add(getAction(CreateDerivedDbEntityAction.ACTION_NAME).buildMenu());
        projectMenu.add(getAction(CreateStoredProcedureAction.ACTION_NAME).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(ObjEntitySyncAction.ACTION_NAME).buildMenu());
        projectMenu.add(getAction(DerivedEntitySyncAction.ACTION_NAME).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(RemoveAction.ACTION_NAME).buildMenu());

        toolMenu.add(getAction(ImportDbAction.ACTION_NAME).buildMenu());
        toolMenu.add(getAction(ImportEOModelAction.ACTION_NAME).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(GenerateClassesAction.ACTION_NAME).buildMenu());
        toolMenu.add(getAction(GenerateDbAction.ACTION_NAME).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(PackageMenuAction.ACTION_NAME).buildMenu());

        helpMenu.add(getAction(AboutAction.ACTION_NAME).buildMenu());
    }

    protected void initStatusBar() {
        StatusBarView statusBar = new StatusBarView();
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        controller.setStatusBarView(statusBar);
    }

    /** Initializes main toolbar. */
    protected void initToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.add(getAction(NewProjectAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(OpenProjectAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(SaveAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(RemoveAction.ACTION_NAME).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateDomainAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateNodeAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateDataMapAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateDbEntityAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateDerivedDbEntityAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateStoredProcedureAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateObjEntityAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateAttributeAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateRelationshipAction.ACTION_NAME).buildButton());

        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    /** Adds path to the list of last opened projects in preferences. */
    public void addToLastProjList(String path) {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        Vector arr = pref.getVector(ModelerPreferences.LAST_PROJ_FILES);
        // Add proj path to the preferences
        // Prevent duplicate entries.
        if (arr.contains(path)) {
            arr.remove(path);
        }

        arr.insertElementAt(path, 0);
        while (arr.size() > 4) {
            arr.remove(arr.size() - 1);
        }

        pref.remove(ModelerPreferences.LAST_PROJ_FILES);
        Iterator iter = arr.iterator();
        while (iter.hasNext()) {
            pref.addProperty(ModelerPreferences.LAST_PROJ_FILES, iter.next());
        }
    }

    /** 
     * Adds asterisk to the title of the window to indicate 
     * it is dirty. 
     */
    public void setDirty(boolean flag) {
        String title = getTitle();
        if (flag) {
            getAction(SaveAction.ACTION_NAME).setEnabled(true);
            if (title == null || !title.startsWith(DIRTY_STRING)) {
                setTitle(DIRTY_STRING + title);
            }
        }
        else {
            getAction(SaveAction.ACTION_NAME).setEnabled(false);
            if (title != null && title.startsWith(DIRTY_STRING)) {
                setTitle(title.substring(DIRTY_STRING.length(), title.length()));
            }
        }
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        enableDataNodeMenu();
        getAction(RemoveAction.ACTION_NAME).setName("Remove DataNode");
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        enableDataMapMenu();
        getAction(RemoveAction.ACTION_NAME).setName("Remove DataMap");
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        enableObjEntityMenu();
        getAction(RemoveAction.ACTION_NAME).setName("Remove ObjEntity");
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        enableDbEntityMenu();
        getAction(RemoveAction.ACTION_NAME).setName("Remove DbEntity");
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        enableProcedureMenu();
        getAction(RemoveAction.ACTION_NAME).setName("Remove Stored Procedure");
    }

    public void currentDbAttributeChanged(AttributeDisplayEvent e) {
        enableDbEntityMenu();
        if (e.getAttribute() != null) {
            getAction(RemoveAction.ACTION_NAME).setName("Remove DbAttribute");
        }
    }

    public void currentObjAttributeChanged(AttributeDisplayEvent e) {
        enableObjEntityMenu();
        if (e.getAttribute() != null) {
            getAction(RemoveAction.ACTION_NAME).setName("Remove ObjAttribute");
        }
    }

    public void currentDbRelationshipChanged(RelationshipDisplayEvent e) {
        enableDbEntityMenu();
        if (e.getRelationship() != null) {
            getAction(RemoveAction.ACTION_NAME).setName("Remove DbRelationship");
        }
    }

    public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
        enableObjEntityMenu();
        if (e.getRelationship() != null) {
            getAction(RemoveAction.ACTION_NAME).setName("Remove ObjRelationship");
        }
    }

    private void enableDataMapMenu() {
        if (controller.getEventController().getCurrentDataNode() != null)
            enableDataNodeMenu();
        else {
            // Andrus: Temp hack till moved to controller
            controller.getActionController().handleControl(
                new Control(
                    ModelerController.DATA_DOMAIN_SELECTED_ID,
                    controller.getEventController().getCurrentDataDomain()));
        }

        getAction(PackageMenuAction.ACTION_NAME).setEnabled(true);
        getAction(GenerateClassesAction.ACTION_NAME).setEnabled(true);
        getAction(CreateObjEntityAction.ACTION_NAME).setEnabled(true);
        getAction(CreateDbEntityAction.ACTION_NAME).setEnabled(true);
        getAction(CreateDerivedDbEntityAction.ACTION_NAME).setEnabled(true);
        getAction(CreateStoredProcedureAction.ACTION_NAME).setEnabled(true);
        getAction(GenerateDbAction.ACTION_NAME).setEnabled(true);
    }

    private void enableObjEntityMenu() {
        enableDataMapMenu();
        getAction(ObjEntitySyncAction.ACTION_NAME).setEnabled(true);
        getAction(CreateAttributeAction.ACTION_NAME).setEnabled(true);
        getAction(CreateRelationshipAction.ACTION_NAME).setEnabled(true);
    }

    private void enableDbEntityMenu() {
        enableDataMapMenu();
        getAction(CreateAttributeAction.ACTION_NAME).setEnabled(true);
        getAction(CreateRelationshipAction.ACTION_NAME).setEnabled(true);

        if (controller.getEventController().getCurrentDbEntity()
            instanceof DerivedDbEntity) {
            getAction(DerivedEntitySyncAction.ACTION_NAME).setEnabled(true);
        }
    }

    private void enableProcedureMenu() {
        enableDataMapMenu();
        getAction(CreateAttributeAction.ACTION_NAME).setEnabled(true);
    }

    private void enableDataNodeMenu() {
        // Andrus: Temp hack till moved to controller
        controller.getActionController().handleControl(
            new Control(
                ModelerController.DATA_DOMAIN_SELECTED_ID,
                controller.getEventController().getCurrentDataDomain()));
    }

    public void updateTitle() {
        String title = null;
        Project project = getProject();

        if (project != null) {
            if (project.isLocationUndefined()) {
                title = "[New]";
            }
            else {
                title = project.getMainFile().getAbsolutePath();
            }
        }

        setTitle(ModelerUtil.buildTitle(title));
    }

    /**
     * Returns the right side view panel.
     * 
     * @return EditorView
     */
    public EditorView getView() {
        return view;
    }

    /**
     * Returns the controller.
     * 
     * @return TopController
     */
    public TopController getController() {
        return controller;
    }

    /**
     * Returns the recentFileMenu.
     * @return RecentFileMenu
     */
    public RecentFileMenu getRecentFileMenu() {
        return recentFileMenu;
    }

    /**
     * Sets the view.
     * @param view The view to set
     */
    public void setView(EditorView view) {
        this.view = view;
    }

}