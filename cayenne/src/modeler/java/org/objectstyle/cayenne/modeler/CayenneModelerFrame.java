/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;

import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.modeler.action.AboutAction;
import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.action.ConfigureClasspathAction;
import org.objectstyle.cayenne.modeler.action.CreateAttributeAction;
import org.objectstyle.cayenne.modeler.action.CreateDataMapAction;
import org.objectstyle.cayenne.modeler.action.CreateDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDomainAction;
import org.objectstyle.cayenne.modeler.action.CreateNodeAction;
import org.objectstyle.cayenne.modeler.action.CreateObjEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateQueryAction;
import org.objectstyle.cayenne.modeler.action.CreateRelationshipAction;
import org.objectstyle.cayenne.modeler.action.CreateStoredProcedureAction;
import org.objectstyle.cayenne.modeler.action.DerivedEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.ExitAction;
import org.objectstyle.cayenne.modeler.action.GenerateClassesAction;
import org.objectstyle.cayenne.modeler.action.GenerateDbAction;
import org.objectstyle.cayenne.modeler.action.ImportDataMapAction;
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
import org.objectstyle.cayenne.modeler.editor.*;
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
import org.objectstyle.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureParameterDisplayListener;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;
import org.objectstyle.cayenne.modeler.util.ModelerStrings;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.objectstyle.cayenne.modeler.util.RecentFileMenu;
import org.objectstyle.cayenne.project.Project;
import org.scopemvc.core.Control;
import org.scopemvc.util.UIStrings;

/** 
 * Main frame of CayenneModeler. Responsibilities include 
 * coordination of enabling/disabling of menu and toolbar.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class CayenneModelerFrame
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
        QueryDisplayListener,
        ProcedureDisplayListener,
        ProcedureParameterDisplayListener {

    /** 
     * Label that indicates as a part of the title that
     * the project has unsaved changes. 
     */
    public static final String DIRTY_STRING = "* - ";

    protected static CayenneModelerFrame frame;

    protected EditorView view;
    protected RecentFileMenu recentFileMenu = new RecentFileMenu("Recent Files");
    protected TopController controller;

    private ModelerPreferences prefs;

    /** Returns an editor singleton object. */
    public static CayenneModelerFrame getFrame() {
        return frame;
    }

    /**
     * Returns a project that is currently a current project of an 
     * Editor singleton instance. This will be changed if CayenneModeler
     * ever starts supporting multiple open projects.
     */
    public static Project getProject() {
        return getFrame().controller.getTopModel().getCurrentProject();
    }

    public CayenneModelerFrame() {
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
                ((ExitAction) getAction(ExitAction.getActionName())).exit();
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (e.getComponent() == CayenneModelerFrame.this) {
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_WIDTH,
                        String.valueOf(CayenneModelerFrame.this.getWidth()));
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_HEIGHT,
                        String.valueOf(CayenneModelerFrame.this.getHeight()));
                }
            }

            public void componentMoved(ComponentEvent e) {
                if (e.getComponent() == CayenneModelerFrame.this) {
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_X,
                        String.valueOf(CayenneModelerFrame.this.getX()));
                    prefs.setProperty(
                        ModelerPreferences.EDITOR_FRAME_Y,
                        String.valueOf(CayenneModelerFrame.this.getY()));
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
        return controller.getTopModel().getAction(key);
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

        fileMenu.add(getAction(NewProjectAction.getActionName()).buildMenu());
        fileMenu.add(getAction(OpenProjectAction.getActionName()).buildMenu());
        fileMenu.add(getAction(ProjectAction.getActionName()).buildMenu());
        fileMenu.add(getAction(ImportDataMapAction.getActionName()).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(getAction(SaveAction.getActionName()).buildMenu());
        fileMenu.addSeparator();

        recentFileMenu.rebuildFromPreferences();
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(getAction(ExitAction.getActionName()).buildMenu());

        projectMenu.add(getAction(ValidateAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(CreateDomainAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateNodeAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDataMapAction.getActionName()).buildMenu());

        projectMenu.add(getAction(CreateObjEntityAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDbEntityAction.getActionName()).buildMenu());
        projectMenu.add(
            getAction(CreateDerivedDbEntityAction.getActionName()).buildMenu());
        projectMenu.add(
            getAction(CreateStoredProcedureAction.getActionName()).buildMenu());
        projectMenu.add(
                   getAction(CreateQueryAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(ObjEntitySyncAction.getActionName()).buildMenu());
        projectMenu.add(getAction(DerivedEntitySyncAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(RemoveAction.getActionName()).buildMenu());

        toolMenu.add(getAction(ImportDbAction.getActionName()).buildMenu());
        toolMenu.add(getAction(ImportEOModelAction.getActionName()).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(GenerateClassesAction.getActionName()).buildMenu());
        toolMenu.add(getAction(GenerateDbAction.getActionName()).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(PackageMenuAction.getActionName()).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(ConfigureClasspathAction.getActionName()).buildMenu());

        helpMenu.add(getAction(AboutAction.getActionName()).buildMenu());
    }

    protected void initStatusBar() {
        StatusBarView statusBar = new StatusBarView();
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        controller.setStatusBarView(statusBar);
    }

    /** Initializes main toolbar. */
    protected void initToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.add(getAction(NewProjectAction.getActionName()).buildButton());
        toolBar.add(getAction(OpenProjectAction.getActionName()).buildButton());
        toolBar.add(getAction(SaveAction.getActionName()).buildButton());
        toolBar.add(getAction(RemoveAction.getActionName()).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateDomainAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateNodeAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateDataMapAction.getActionName()).buildButton());
        
        toolBar.addSeparator();
        
        toolBar.add(getAction(CreateDbEntityAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateDerivedDbEntityAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateStoredProcedureAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateObjEntityAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateAttributeAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateRelationshipAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateQueryAction.getActionName()).buildButton());

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
            getAction(SaveAction.getActionName()).setEnabled(true);
            if (title == null || !title.startsWith(DIRTY_STRING)) {
                setTitle(DIRTY_STRING + title);
            }
        } else {
            getAction(SaveAction.getActionName()).setEnabled(false);
            if (title != null && title.startsWith(DIRTY_STRING)) {
                setTitle(title.substring(DIRTY_STRING.length(), title.length()));
            }
        }
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        enableDataNodeMenu();
        getAction(RemoveAction.getActionName()).setName("Remove DataNode");
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        enableDataMapMenu();
        getAction(RemoveAction.getActionName()).setName("Remove DataMap");
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        enableObjEntityMenu();
        getAction(RemoveAction.getActionName()).setName("Remove ObjEntity");
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        enableDbEntityMenu();
        getAction(RemoveAction.getActionName()).setName("Remove DbEntity");
    }

    public void currentQueryChanged(QueryDisplayEvent e) {
        enableDataMapMenu();

        if (e.getQuery() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove Query");
        }
    }
    
    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        enableProcedureMenu();

        if (e.getProcedure() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove Stored Procedure");
            getAction(CreateAttributeAction.getActionName()).setName(
                "Create Procedure Parameter");
        } else {
            getAction(CreateAttributeAction.getActionName()).setName("Create Attribute");
        }
    }

    public void currentDbAttributeChanged(AttributeDisplayEvent e) {
        enableDbEntityMenu();
        if (e.getAttribute() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove DbAttribute");
        }
    }

    public void currentProcedureParameterChanged(ProcedureParameterDisplayEvent e) {
        enableProcedureMenu();
        if (e.getProcedureParameter() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove Procedure Parameter");
        }

        if (e.getProcedure() != null) {
            getAction(CreateAttributeAction.getActionName()).setName(
                "Create Procedure Parameter");
        } else {
            getAction(CreateAttributeAction.getActionName()).setName("Create Attribute");
        }
    }

    public void currentObjAttributeChanged(AttributeDisplayEvent e) {
        enableObjEntityMenu();
        if (e.getAttribute() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove ObjAttribute");
        }
    }

    public void currentDbRelationshipChanged(RelationshipDisplayEvent e) {
        enableDbEntityMenu();
        if (e.getRelationship() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove DbRelationship");
        }
    }

    public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
        enableObjEntityMenu();
        if (e.getRelationship() != null) {
            getAction(RemoveAction.getActionName()).setName("Remove ObjRelationship");
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

        getAction(PackageMenuAction.getActionName()).setEnabled(true);
        getAction(GenerateClassesAction.getActionName()).setEnabled(true);
        getAction(CreateObjEntityAction.getActionName()).setEnabled(true);
        getAction(CreateDbEntityAction.getActionName()).setEnabled(true);
        getAction(CreateDerivedDbEntityAction.getActionName()).setEnabled(true);
        getAction(CreateQueryAction.getActionName()).setEnabled(true);
        getAction(CreateStoredProcedureAction.getActionName()).setEnabled(true);
        getAction(GenerateDbAction.getActionName()).setEnabled(true);

        // reset
        getAction(CreateAttributeAction.getActionName()).setName("Create Attribute");
    }

    private void enableObjEntityMenu() {
        enableDataMapMenu();
        getAction(ObjEntitySyncAction.getActionName()).setEnabled(true);
        getAction(CreateAttributeAction.getActionName()).setEnabled(true);
        getAction(CreateRelationshipAction.getActionName()).setEnabled(true);
    }

    private void enableDbEntityMenu() {
        enableDataMapMenu();
        getAction(CreateAttributeAction.getActionName()).setEnabled(true);
        getAction(CreateRelationshipAction.getActionName()).setEnabled(true);

        if (controller.getEventController().getCurrentDbEntity()
            instanceof DerivedDbEntity) {
            getAction(DerivedEntitySyncAction.getActionName()).setEnabled(true);
        }
    }

    private void enableProcedureMenu() {
        enableDataMapMenu();
        getAction(CreateAttributeAction.getActionName()).setEnabled(true);
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
            } else {
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