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

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.gui.action.*;
import org.objectstyle.cayenne.gui.datamap.GenerateClassDialog;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.NamedObjectFactory;
import org.objectstyle.cayenne.util.Preferences;

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
		ActionListener,
		DomainDisplayListener,
		DataNodeDisplayListener,
		DataMapDisplayListener,
		ObjEntityDisplayListener,
		DbEntityDisplayListener,
		ObjAttributeDisplayListener,
		DbAttributeDisplayListener,
		ObjRelationshipDisplayListener,
		DbRelationshipDisplayListener {
	static Logger logObj = Logger.getLogger(Editor.class.getName());

	private static final String TITLE = "CayenneModeler";

	/** 
	 * Label that indicates as a part of the title that
	 * the project has unsaved changes. 
	 */
	private static final String DIRTY_STRING = "* - ";

	protected EditorView view;
	protected Mediator mediator;
	protected ActionMap actionMap;

	RecentFileMenu recentFileMenu = new RecentFileMenu("Recent Files");
	JMenuItem closeProjectMenu = new JMenuItem("Close Project");
	JMenuItem exitMenu = new JMenuItem("Exit");

	JMenuItem createObjEntityMenu = new JMenuItem("Create Object Entity");
	JMenuItem createDbEntityMenu = new JMenuItem("Create DB Entity");

	JMenuItem importDbMenu = new JMenuItem("Reverse Engineer Database");
	JMenuItem importEOMMenu = new JMenuItem("Import EOModel");
	JMenuItem generateMenu = new JMenuItem("Generate Classes");
	JMenuItem generateDbMenu = new JMenuItem("Generate Database");
	JMenuItem setPackageMenu =
		new JMenuItem("Set Package Name for Obj Entities");

	JMenuItem aboutMenu = new JMenuItem("About");

	JButton createObjEntityBtn;
	JButton createDbEntityBtn;

	Properties props;

	final JFileChooser fileChooser = new JFileChooser();
	XmlFilter xmlFilter = new XmlFilter();

	private static Editor frame;

	/** Singleton implementation of getting Editor window. */
	public static Editor getFrame() {
		return frame;
	}

	public Editor() {
		super(TITLE);

		frame = this;

		try {
			props = loadProperties();
		} catch (IOException ioex) {
			logObj.log(Level.SEVERE, "error", ioex);
			// ignoring
			props = new Properties();
		}

		initEmptyActions();

		// these are legacy methods being refactored out
		init();
		initActions();

	}

	/**
	 * Returns an action object associated with the key.
	 */
	public CayenneAction getAction(String key) {
		return (CayenneAction) actionMap.get(key);
	}

	protected void initEmptyActions() {
		// build action map
		actionMap = new ActionMap();

		CayenneAction newProjectAction = new NewProjectAction();
		newProjectAction.setEnabled(true);
		actionMap.put(newProjectAction.getKey(), newProjectAction);

		CayenneAction openProjectAction = new OpenProjectAction();
		openProjectAction.setEnabled(true);
		actionMap.put(openProjectAction.getKey(), openProjectAction);

		CayenneAction saveAction = new SaveAction();
		actionMap.put(saveAction.getKey(), saveAction);

		CayenneAction removeAction = new RemoveAction();
		actionMap.put(removeAction.getKey(), removeAction);

		CayenneAction createDomainAction = new CreateDomainAction();
		actionMap.put(createDomainAction.getKey(), createDomainAction);

		CayenneAction createNodeAction = new CreateNodeAction();
		actionMap.put(createNodeAction.getKey(), createNodeAction);

		CayenneAction createMapAction = new CreateDataMapAction();
		actionMap.put(createMapAction.getKey(), createMapAction);



		CayenneAction addMapToNodeAction = new AddDataMapAction();
		actionMap.put(addMapToNodeAction.getKey(), addMapToNodeAction);
		
		CayenneAction entSynchAction = new EntitySynchAction();
		actionMap.put(entSynchAction.getKey(), entSynchAction);
	}

	protected void init() {
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
		fileMenu.add(closeProjectMenu);
		fileMenu.addSeparator();
		fileMenu.add(getAction(SaveAction.ACTION_NAME).buildMenu());
		fileMenu.addSeparator();

		recentFileMenu.rebuildFromPreferences();
		fileMenu.add(recentFileMenu);

		fileMenu.addSeparator();
		fileMenu.add(exitMenu);

		projectMenu.add(getAction(CreateDomainAction.ACTION_NAME).buildMenu());
		projectMenu.add(getAction(CreateNodeAction.ACTION_NAME).buildMenu());
		projectMenu.add(getAction(CreateDataMapAction.ACTION_NAME).buildMenu());

		projectMenu.add(createObjEntityMenu);
		projectMenu.add(createDbEntityMenu);
		projectMenu.addSeparator();
		projectMenu.add(getAction(AddDataMapAction.ACTION_NAME).buildMenu());
		projectMenu.add(getAction(EntitySynchAction.ACTION_NAME).buildMenu());
		projectMenu.addSeparator();
		projectMenu.add(getAction(RemoveAction.ACTION_NAME).buildMenu());

		toolMenu.add(importDbMenu);
		toolMenu.add(importEOMMenu);
		toolMenu.addSeparator();
		toolMenu.add(generateMenu);
		toolMenu.add(generateDbMenu);
		toolMenu.addSeparator();
		toolMenu.add(setPackageMenu);

		helpMenu.add(aboutMenu);

		/* ClassLoader cl = Editor.class.getClassLoader();
		URL url = cl.getResource("org/objectstyle/gui/images/frameicon16.gif");
		Image icon = Toolkit.getDefaultToolkit().createImage(url);
		this.setIconImage(icon);
		*/

		initToolBar();
	}

	protected void initActions() {
		// create and assign actions
		CayenneAction importDbAction = new ImportDbAction();
		actionMap.put(importDbAction.getKey(), importDbAction);
		importDbMenu.addActionListener(importDbAction);

		CayenneAction importEOModelAction = new ImportEOModelAction();
		actionMap.put(importEOModelAction.getKey(), importEOModelAction);
		importEOMMenu.addActionListener(importEOModelAction);

		CayenneAction genDbAction = new GenerateDbAction();
		actionMap.put(genDbAction.getKey(), genDbAction);
		generateDbMenu.addActionListener(genDbAction);


		// "legacy" code - need to hook up all menus and toolbars with actions 
		disableMenu();
		closeProjectMenu.setEnabled(false);

		createObjEntityMenu.addActionListener(this);
		createDbEntityMenu.addActionListener(this);

		closeProjectMenu.addActionListener(this);
		exitMenu.addActionListener(this);

		generateMenu.addActionListener(this);
		setPackageMenu.addActionListener(this);
		aboutMenu.addActionListener(this);

		createObjEntityBtn.addActionListener(this);
		createDbEntityBtn.addActionListener(this);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(650, 550);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exitEditor();
			}
		});
	}

	/** Adds path to the list of last opened projects in preferences. */
	public void addToLastProjList(String path) {
		Preferences pref = Preferences.getPreferences();
		Vector arr = pref.getVector(Preferences.LAST_PROJ_FILES);
		// Add proj path to the preferences
		// Prevent duplicate entries.
		if (arr.contains(path)) {
			arr.remove(path);
		}

		arr.insertElementAt(path, 0);
		while (arr.size() > 4) {
			arr.remove(arr.size() - 1);
		}

		pref.remove(Preferences.LAST_PROJ_FILES);
		Iterator iter = arr.iterator();
		while (iter.hasNext()) {
			pref.addProperty(Preferences.LAST_PROJ_FILES, iter.next());
		}
	}

	protected void initToolBar() {
		ClassLoader cl = Editor.class.getClassLoader();

		JToolBar toolBar = new JToolBar();
		toolBar.add(getAction(NewProjectAction.ACTION_NAME).buildButton());
		toolBar.add(getAction(OpenProjectAction.ACTION_NAME).buildButton());
		toolBar.add(getAction(SaveAction.ACTION_NAME).buildButton());
		toolBar.add(getAction(RemoveAction.ACTION_NAME).buildButton());

		toolBar.addSeparator();

		toolBar.add(getAction(CreateDomainAction.ACTION_NAME).buildButton());
        toolBar.add(getAction(CreateNodeAction.ACTION_NAME).buildButton());
		toolBar.add(getAction(CreateDataMapAction.ACTION_NAME).buildButton());

		URL url =
			cl.getResource(
				CayenneAction.RESOURCE_PATH + "images/icon-dbentity.gif");
		ImageIcon dbEntityIcon = new ImageIcon(url);
		createDbEntityBtn = new JButton(dbEntityIcon);
		createDbEntityBtn.setToolTipText("create new db entity");
		toolBar.add(createDbEntityBtn);

		url =
			cl.getResource(
				CayenneAction.RESOURCE_PATH + "images/icon-objentity.gif");
		ImageIcon objEntityIcon = new ImageIcon(url);
		createObjEntityBtn = new JButton(objEntityIcon);
		createObjEntityBtn.setToolTipText("create new obj entity");
		toolBar.add(createObjEntityBtn);

		ImageIcon attrIcon =
			new ImageIcon(
				cl.getResource(
					CayenneAction.RESOURCE_PATH + "images/icon-attribute.gif"));
		JButton attrBtn = new JButton(attrIcon);
		attrBtn.setToolTipText("create attribute");
		toolBar.add(attrBtn);

		ImageIcon relIcon =
			new ImageIcon(
				cl.getResource(
					CayenneAction.RESOURCE_PATH
						+ "images/icon-relationship.gif"));
		JButton relBtn = new JButton(relIcon);
		relBtn.setToolTipText("create relationship");
		toolBar.add(relBtn);

		getContentPane().add(toolBar, BorderLayout.NORTH);
	}

	public void projectClosed() {
		recentFileMenu.rebuildFromPreferences();
		getContentPane().remove(view);
		view = null;
		setMediator(null);

		disableMenu();

		closeProjectMenu.setEnabled(false);
		getAction(RemoveAction.ACTION_NAME).setName("Remove");
		getAction(SaveAction.ACTION_NAME).setEnabled(false);
		getAction(CreateDomainAction.ACTION_NAME).setEnabled(false);

		// repaint is needed, since there is a trace from menu left on the screen
		repaint();

		setProjectTitle(null);
	}

	public void projectOpened() {
		view = new EditorView(mediator);
		getContentPane().add(view, BorderLayout.CENTER);

		mediator.addDomainDisplayListener(this);
		mediator.addDataNodeDisplayListener(this);
		mediator.addDataMapDisplayListener(this);
		mediator.addObjEntityDisplayListener(this);
		mediator.addDbEntityDisplayListener(this);
		mediator.addObjAttributeDisplayListener(this);
		mediator.addDbAttributeDisplayListener(this);
		mediator.addObjRelationshipDisplayListener(this);
		mediator.addDbRelationshipDisplayListener(this);

		getAction(CreateDomainAction.ACTION_NAME).setEnabled(true);
		getAction(SaveAction.ACTION_NAME).setEnabled(false);
		closeProjectMenu.setEnabled(true);

		this.validate();
	}

	/**
	 * Reads properties from file "gui.properties" bundled with Cayenne.
	 */
	protected Properties loadProperties() throws IOException {
		Properties props = new Properties();
		InputStream in =
			this.getClass().getClassLoader().getResourceAsStream(
				CayenneAction.RESOURCE_PATH + "gui.properties");
		if (in != null) {
			try {
				props.load(in);
			} finally {
				in.close();
			}
		}

		return props;
	}

	/**
	 * Returns a property for <code>propName</code>.
	 */
	public String getProperty(String propName) {
		return props.getProperty(propName);
	}

	/** 
	 * Returns an instance of FileChooser used by all Modeler
	 * components.
	 */
	public JFileChooser getFileChooser() {
		return fileChooser;
	}

	/** 
	 * Adds asterisk to the title of the window to indicate 
	 * it is dirty. 
	 */
	public void setDirty(boolean flag) {
		String title = getTitle();
		if (flag) {
			getAction(SaveAction.ACTION_NAME).setEnabled(true);
			if (!title.startsWith(DIRTY_STRING)) {
				setTitle(DIRTY_STRING + title);
			}
		} else {
			getAction(SaveAction.ACTION_NAME).setEnabled(false);
			if (title.startsWith(DIRTY_STRING)) {
				setTitle(
					title.substring(DIRTY_STRING.length(), title.length()));
			}
		}
	}

	private void exitEditor() {
		if (!((ProjectAction) getAction(NewProjectAction.ACTION_NAME))
			.checkSaveOnClose()) {
			return;
		}

		Preferences.getPreferences().storePreferences(Editor.this);
		Editor.this.setVisible(false);
		System.exit(0);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			Object src = e.getSource();

			if (src == closeProjectMenu) {
				((ProjectAction) getAction(NewProjectAction.ACTION_NAME))
					.closeProject();
			} else if (
				src == createObjEntityMenu || src == createObjEntityBtn) {
				createObjEntity();
			} else if (src == createDbEntityMenu || src == createDbEntityBtn) {
				createDbEntity();
			} else if (src == setPackageMenu) {
				// Set the same package name for all obj entities.
				setPackageName();
			} else if (src == generateMenu) {
				generateClasses();
			} else if (src == exitMenu) {
				exitEditor();
			} else if (src == aboutMenu) {
				AboutDialog win = new AboutDialog(this);
			}
		} catch (Exception ex) {
			GUIErrorHandler.guiException(ex);
		}
	}

	private void setPackageName() {
		DataMap map = mediator.getCurrentDataMap();
		if (map == null) {
			return;
		}
		String package_name;
		package_name =
			JOptionPane.showInputDialog(this, "Enter the new package name");
		if (null == package_name || package_name.trim().length() == 0)
			return;
		// Append period to the end of package name, if it is not there.
		if (package_name.charAt(package_name.length() - 1) != '.')
			package_name = package_name + ".";
		// If user cancelled, just return
		if (null == package_name)
			return;
		// Go through all obj entities in the current data map and
		// set their package names.
		ObjEntity[] entities = map.getObjEntities();
		for (int i = 0; i < entities.length; i++) {
			String name = entities[i].getClassName();
			int idx = name.lastIndexOf('.');
			if (idx > 0) {
				name =
					(idx == name.length() - 1)
						? entities[i].getName()
						: name.substring(idx + 1);
			}
			entities[i].setClassName(package_name + name);
		}
		mediator.fireDataMapEvent(new DataMapEvent(this, map));
	}

	private void generateClasses() {
		GenerateClassDialog dialog;
		dialog = new GenerateClassDialog(this, mediator);
		dialog.show();
		dialog.dispose();
	}

	private void createObjEntity() {
		ObjEntity entity =
			(ObjEntity) NamedObjectFactory.createObject(
				ObjEntity.class,
				mediator.getCurrentDataMap());
		mediator.getCurrentDataMap().addObjEntity(entity);
		mediator.fireObjEntityEvent(
			new EntityEvent(this, entity, EntityEvent.ADD));
		mediator.fireObjEntityDisplayEvent(
			new EntityDisplayEvent(
				this,
				entity,
				mediator.getCurrentDataMap(),
				mediator.getCurrentDataNode(),
				mediator.getCurrentDataDomain()));
	}

	private void createDbEntity() {
		DbEntity entity =
			(DbEntity) NamedObjectFactory.createObject(
				DbEntity.class,
				mediator.getCurrentDataMap());

		mediator.getCurrentDataMap().addDbEntity(entity);
		mediator.fireDbEntityEvent(
			new EntityEvent(this, entity, EntityEvent.ADD));
		mediator.fireDbEntityDisplayEvent(
			new EntityDisplayEvent(
				this,
				entity,
				mediator.getCurrentDataMap(),
				mediator.getCurrentDataNode(),
				mediator.getCurrentDataDomain()));
	}

	public void currentDomainChanged(DomainDisplayEvent e) {
		if (e.getDomain() == null) {
			disableMenu();
			return;
		}
		enableDomainMenu();
		getAction(RemoveAction.ACTION_NAME).setName("Remove Domain");
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
			getAction(RemoveAction.ACTION_NAME).setName(
				"Remove DbRelationship");
		}
	}

	public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
		enableObjEntityMenu();
		if (e.getRelationship() != null) {
			getAction(RemoveAction.ACTION_NAME).setName(
				"Remove ObjRelationship");
		}
	}

	/** 
	 * Disables all menu  for the case when no project is open.
	 * The only menus that are never disabled are:
	 * <ul>
	 * <li>"New Project"</li> 
	 * <li>"Open Project"</li>
	 * <li>"Exit"</li>
	 * </ul> 
	 */
	private void disableMenu() {
		getAction(CreateDataMapAction.ACTION_NAME).setEnabled(false);
		getAction(RemoveAction.ACTION_NAME).setEnabled(false);
		getAction(AddDataMapAction.ACTION_NAME).setEnabled(false);
        getAction(CreateNodeAction.ACTION_NAME).setEnabled(false);
		
		createObjEntityMenu.setEnabled(false);
		createDbEntityMenu.setEnabled(false);

		getAction(EntitySynchAction.ACTION_NAME).setEnabled(false);

		importDbMenu.setEnabled(false);
		importEOMMenu.setEnabled(false);
		generateMenu.setEnabled(false);
		generateDbMenu.setEnabled(false);
		setPackageMenu.setEnabled(false);

		
		createObjEntityBtn.setEnabled(false);
		createDbEntityBtn.setEnabled(false);
	}

	private void enableDomainMenu() {
		disableMenu();
		getAction(CreateDataMapAction.ACTION_NAME).setEnabled(true);
		getAction(RemoveAction.ACTION_NAME).setEnabled(true);
		getAction(CreateDomainAction.ACTION_NAME).setEnabled(true);
        getAction(CreateNodeAction.ACTION_NAME).setEnabled(true);

		closeProjectMenu.setEnabled(true);
		importDbMenu.setEnabled(true);
		importEOMMenu.setEnabled(true);
	}

	private void enableDataMapMenu() {
		if (mediator.getCurrentDataNode() != null)
			enableDataNodeMenu();
		else
			enableDomainMenu();
		setPackageMenu.setEnabled(true);
		createObjEntityMenu.setEnabled(true);
		createDbEntityMenu.setEnabled(true);
		generateMenu.setEnabled(true);
		generateDbMenu.setEnabled(true);

		createObjEntityBtn.setEnabled(true);
		createDbEntityBtn.setEnabled(true);
	}
	
	private void enableObjEntityMenu() {
		enableDataMapMenu();
		getAction(EntitySynchAction.ACTION_NAME).setEnabled(true);
	}
	
	private void enableDbEntityMenu() {
		enableDataMapMenu();
	}
	

	private void enableDataNodeMenu() {
		enableDomainMenu();
		getAction(AddDataMapAction.ACTION_NAME).setEnabled(true);
	}

	public static void main(String[] args) {
		JFrame frame = new Editor();
		//Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		frame.setLocation(
			(screenSize.width - frameSize.width) / 2,
			(screenSize.height - frameSize.height) / 2);
		frame.setVisible(true);
	}

	/**
	 * Returns current CayenneModeler mediator.
	 */
	public Mediator getMediator() {
		return mediator;
	}

	public void setMediator(Mediator mediator) {
		this.mediator = mediator;
	}

	public void setProjectTitle(String projectPath) {
		if (projectPath != null) {
			this.setTitle(TITLE + " - " + projectPath);
		} else {
			this.setTitle(TITLE);
		}
	}
}