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
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import org.objectstyle.cayenne.ConfigException;
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

	public static final String RESOURCE_PATH = "org/objectstyle/cayenne/gui/";
	private static final String TITLE = "CayenneModeler";

	/** 
	 * Label that indicates as a part of the title that
	 * the project has unsaved changes. 
	 */
	private static final String DIRTY_STRING = "* - ";

	EditorView view;
	Mediator mediator;
	ActionMap actionMap = new ActionMap();

	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	JMenuItem openProjectMenu = new JMenuItem("Open Project");
	JMenuItem createProjectMenu = new JMenuItem("New Project");
	JMenuItem closeProjectMenu = new JMenuItem("Close Project");
	JMenuItem saveMenu = new JMenuItem("Save");
	JMenuItem exitMenu = new JMenuItem("Exit");
	ArrayList lastOpenProjMenus = new ArrayList();

	JMenu projectMenu = new JMenu("Project");
	JMenuItem createDomainMenu = new JMenuItem("Create Domain");
	JMenuItem createDataSourceMenu = new JMenuItem("Create Data Source");
	JMenuItem createObjEntityMenu = new JMenuItem("Create Object Entity");
	JMenuItem createDbEntityMenu = new JMenuItem("Create DB Entity");
	JMenuItem addDataMapMenu = new JMenuItem("Link Data Map to Node");
	JMenuItem synchObjEntityMenu = new JMenuItem("Synchronize with DbEntity");

	JMenu toolMenu = new JMenu("Tools");
	JMenuItem importDbMenu = new JMenuItem("Reverse Engineer Database");
	JMenuItem importEOMMenu = new JMenuItem("Import EOModel");
	JMenuItem generateMenu = new JMenuItem("Generate Classes");
	JMenuItem generateDbMenu = new JMenuItem("Generate Database");
	JMenuItem setPackageMenu =
		new JMenuItem("Set Package Name for Obj Entities");

	JMenu helpMenu = new JMenu("Help");
	JMenuItem aboutMenu = new JMenuItem("About");

	JToolBar toolBar = new JToolBar();
	JButton createDomainBtn;
	JButton createDataSourceBtn;
	JButton createObjEntityBtn;
	JButton createDbEntityBtn;

	Properties props;

	final JFileChooser fileChooser = new JFileChooser();
	XmlFilter xmlFilter = new XmlFilter();

	private static Editor frame;

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

		// these are legacey methods being refactored out
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

		CayenneAction createMapAction = new CreateDataMapAction();
		actionMap.put(createMapAction.getKey(), createMapAction);

		CayenneAction removeAction = new RemoveAction();
		actionMap.put(removeAction.getKey(), removeAction);
	}

	protected JMenuItem buildMenu(String actionKey, KeyStroke stroke) {
		JMenuItem item = new JMenuItem(getAction(actionKey));
		item.setAccelerator(stroke);
		return item;
	}

	protected JButton buildButton(String actionKey) {
		Action action = getAction(actionKey);
		return new CayenneToolbarButton(action);
	}

	protected void init() {
		getContentPane().setLayout(new BorderLayout());

		// Setup menu bar
		setJMenuBar(menuBar);
		menuBar.add(fileMenu);
		menuBar.add(projectMenu);
		menuBar.add(toolMenu);
		menuBar.add(helpMenu);

		fileMenu.add(createProjectMenu);
		fileMenu.add(openProjectMenu);
		fileMenu.add(closeProjectMenu);
		fileMenu.addSeparator();
		fileMenu.add(saveMenu);
		fileMenu.addSeparator();
		fileMenu.addSeparator();
		fileMenu.add(exitMenu);
		reloadLastProjList();

		projectMenu.add(createDomainMenu);
		projectMenu.add(buildMenu(CreateDataMapAction.ACTION_NAME, null));
		projectMenu.add(createDataSourceMenu);
		projectMenu.add(createObjEntityMenu);
		projectMenu.add(createDbEntityMenu);
		projectMenu.addSeparator();
		projectMenu.add(addDataMapMenu);
		projectMenu.add(synchObjEntityMenu);
		projectMenu.addSeparator();
		projectMenu.add(
			buildMenu(
				RemoveAction.ACTION_NAME,
				KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK)));

		toolMenu.add(importDbMenu);
		toolMenu.add(importEOMMenu);
		toolMenu.addSeparator();
		toolMenu.add(generateMenu);
		toolMenu.add(generateDbMenu);
		toolMenu.addSeparator();
		toolMenu.add(setPackageMenu);

		helpMenu.add(aboutMenu);

		initToolBar();

		/* ClassLoader cl = Editor.class.getClassLoader();
		URL url = cl.getResource("org/objectstyle/gui/images/frameicon16.gif");
		Image icon = Toolkit.getDefaultToolkit().createImage(url);
		this.setIconImage(icon);
		*/
	}

	protected void initActions() {
		// create and assign actions

		CayenneAction addMapAction = new AddDataMapAction();
		actionMap.put(addMapAction.getKey(), addMapAction);
		addDataMapMenu.addActionListener(addMapAction);

		CayenneAction saveAction = new SaveAction();
		actionMap.put(saveAction.getKey(), saveAction);
		saveMenu.addActionListener(saveAction);
		saveMenu.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		CayenneAction importDbAction = new ImportDbAction();
		actionMap.put(importDbAction.getKey(), importDbAction);
		importDbMenu.addActionListener(importDbAction);

		CayenneAction importEOModelAction = new ImportEOModelAction();
		actionMap.put(importEOModelAction.getKey(), importEOModelAction);
		importEOMMenu.addActionListener(importEOModelAction);

		CayenneAction genDbAction = new GenerateDbAction();
		actionMap.put(genDbAction.getKey(), genDbAction);
		generateDbMenu.addActionListener(genDbAction);

		CayenneAction entSynchAction = new EntitySynchAction();
		actionMap.put(entSynchAction.getKey(), entSynchAction);
		synchObjEntityMenu.addActionListener(entSynchAction);

		// "legacy" code - need to hook up all menus and toolbars with actions 
		disableMenu();
		closeProjectMenu.setEnabled(false);
		createDomainMenu.setEnabled(false);
		createDomainBtn.setEnabled(false);

		createProjectMenu.addActionListener(this);
		createDomainMenu.addActionListener(this);
		createDataSourceMenu.addActionListener(this);
		createObjEntityMenu.addActionListener(this);
		createDbEntityMenu.addActionListener(this);

		openProjectMenu.addActionListener(this);
		openProjectMenu.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		closeProjectMenu.addActionListener(this);
		exitMenu.addActionListener(this);

		generateMenu.addActionListener(this);
		setPackageMenu.addActionListener(this);
		aboutMenu.addActionListener(this);

		createDomainBtn.addActionListener(this);
		createDataSourceBtn.addActionListener(this);
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

	private void reloadLastProjList() {
		// Get list of last opened proj files and trim it down to 4
		Preferences pref = Preferences.getPreferences();
		Vector arr = pref.getVector(Preferences.LAST_PROJ_FILES);
		while (arr.size() > 4)
			arr.remove(arr.size() - 1);
		for (int i = 0; i < arr.size(); i++) {
			if (lastOpenProjMenus.size() <= i) {
				JMenuItem item = new JMenuItem((String) arr.get(i));
				fileMenu.insert(item, lastOpenProjMenus.size() + 6);
				lastOpenProjMenus.add(item);
				item.addActionListener(this);
			} else {
				JMenuItem item = (JMenuItem) lastOpenProjMenus.get(i);
				item.setText((String) arr.get(i));
			}
		}
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
		while (arr.size() > 4)
			arr.remove(arr.size() - 1);
		pref.remove(Preferences.LAST_PROJ_FILES);
		Iterator iter = arr.iterator();
		while (iter.hasNext())
			pref.addProperty(Preferences.LAST_PROJ_FILES, iter.next());
	}

	private void initToolBar() {
		ClassLoader cl = Editor.class.getClassLoader();

		ImageIcon newIcon =
			new ImageIcon(
				cl.getResource(RESOURCE_PATH + "images/icon-new.gif"));
		JButton newBtn = new JButton(newIcon);
		newBtn.setToolTipText("new project");
		toolBar.add(newBtn);

		ImageIcon openIcon =
			new ImageIcon(
				cl.getResource(RESOURCE_PATH + "images/icon-open.gif"));
		JButton openBtn = new JButton(openIcon);
		openBtn.setToolTipText("open project");
		toolBar.add(openBtn);

		ImageIcon saveIcon =
			new ImageIcon(
				cl.getResource(RESOURCE_PATH + "images/icon-save.gif"));
		JButton saveBtn = new JButton(saveIcon);
		saveBtn.setToolTipText("save project");
		toolBar.add(saveBtn);

		toolBar.add(buildButton(RemoveAction.ACTION_NAME));

		toolBar.addSeparator();

		URL url = cl.getResource(RESOURCE_PATH + "images/icon-dom.gif");
		ImageIcon domainIcon = new ImageIcon(url);
		createDomainBtn = new JButton(domainIcon);
		createDomainBtn.setToolTipText("create new domain");
		toolBar.add(createDomainBtn);

		url = cl.getResource(RESOURCE_PATH + "images/icon-node.gif");
		ImageIcon nodeIcon = new ImageIcon(url);
		createDataSourceBtn = new JButton(nodeIcon);
		createDataSourceBtn.setToolTipText("create new data node");
		toolBar.add(createDataSourceBtn);

		toolBar.add(buildButton(CreateDataMapAction.ACTION_NAME));

		url = cl.getResource(RESOURCE_PATH + "images/icon-dbentity.gif");
		ImageIcon dbEntityIcon = new ImageIcon(url);
		createDbEntityBtn = new JButton(dbEntityIcon);
		createDbEntityBtn.setToolTipText("create new db entity");
		toolBar.add(createDbEntityBtn);

		url = cl.getResource(RESOURCE_PATH + "images/icon-objentity.gif");
		ImageIcon objEntityIcon = new ImageIcon(url);
		createObjEntityBtn = new JButton(objEntityIcon);
		createObjEntityBtn.setToolTipText("create new obj entity");
		toolBar.add(createObjEntityBtn);

		ImageIcon attrIcon =
			new ImageIcon(
				cl.getResource(RESOURCE_PATH + "images/icon-attribute.gif"));
		JButton attrBtn = new JButton(attrIcon);
		attrBtn.setToolTipText("create attribute");
		toolBar.add(attrBtn);

		ImageIcon relIcon =
			new ImageIcon(
				cl.getResource(RESOURCE_PATH + "images/icon-relationship.gif"));
		JButton relBtn = new JButton(relIcon);
		relBtn.setToolTipText("create relationship");
		toolBar.add(relBtn);

		getContentPane().add(toolBar, BorderLayout.NORTH);
	}

	/** Singleton implementation of getting Editor window. */
	public static Editor getFrame() {
		return frame;
	}

	/**
	 * Reads properties from file "gui.properties" bundled with Cayenne.
	 */
	protected Properties loadProperties() throws IOException {
		Properties props = new Properties();
		InputStream in =
			this.getClass().getClassLoader().getResourceAsStream(
				RESOURCE_PATH + "gui.properties");
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
			if (!title.startsWith(DIRTY_STRING)) {
				setTitle(DIRTY_STRING + title);
			}
		} else {
			if (title.startsWith(DIRTY_STRING)) {
				setTitle(
					title.substring(DIRTY_STRING.length(), title.length()));
			}
		}
	}

	/** Return false if cancel closing the window, true otherwise. */
	private boolean checkSaveOnClose() {
		if (mediator != null && mediator.isDirty()) {
			int ret_code =
				JOptionPane.showConfirmDialog(
					this,
					"You have unsaved data. " + "Do you want to save it?");
			if (ret_code == JOptionPane.CANCEL_OPTION)
				return false;
			else if (ret_code == JOptionPane.YES_OPTION)
				actionMap.get("SaveAll").actionPerformed(
					new ActionEvent(
						this,
						ActionEvent.ACTION_PERFORMED,
						"SaveAll"));
		}
		return true;
	}

	private void exitEditor() {
		if (!checkSaveOnClose())
			return;
		Preferences.getPreferences().storePreferences(Editor.this);
		Editor.this.setVisible(false);
		System.exit(0);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			Object src = e.getSource();

			if (src == createProjectMenu) {
				createProject();
			} else if (src == openProjectMenu) {
				openProject();
			} else if (src == closeProjectMenu) {
				closeProject();
			} else if (src == createProjectMenu) {
				createProject();
			} else if (src == createDomainMenu || src == createDomainBtn) {
				createDomain();
			} else if (
				src == createDataSourceMenu || src == createDataSourceBtn) {
				createDataNode();
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
			} else if (lastOpenProjMenus.contains(src)) {
				// uncomment this line to provide debugging 
				// information during driver loading
				// Configuration.setLogLevel(Level.SEVERE);

				openProject(((JMenuItem) src).getText());
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

	/** Returns true if successfully closed project, false otherwise. */
	private boolean closeProject() {
		if (!checkSaveOnClose()) {
			return false;
		}

		reloadLastProjList();
		getContentPane().remove(view);
		view = null;
		mediator = null;
		repaint();
		disableMenu();

		closeProjectMenu.setEnabled(false);
		createDomainMenu.setEnabled(false);
		createDomainBtn.setEnabled(false);

		getAction(RemoveAction.ACTION_NAME).setName("Remove");

		// Take path of the proj away from the title
		this.setTitle(TITLE);
		return true;
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

	private void createProject() {
		Preferences pref = Preferences.getPreferences();
		String init_dir = (String) pref.getProperty(Preferences.LAST_DIR);
		try {
			// Get the project file name (always cayenne.xml)
			boolean finished = false;
			File file = null;
			File proj_file = null;
			while (!finished) {
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setDialogTitle("Choose project location");
				if (null != init_dir) {
					File init_dir_file = new File(init_dir);
					if (init_dir_file.exists())
						fileChooser.setCurrentDirectory(init_dir_file);
				}
				int ret_code = fileChooser.showSaveDialog(this);
				if (ret_code != JFileChooser.APPROVE_OPTION)
					return;
				file = fileChooser.getSelectedFile();
				if (!file.exists())
					file.createNewFile();
				proj_file = new File(file, ProjectFileFilter.PROJ_FILE_NAME);
				if (proj_file.exists()) {
					int ret =
						JOptionPane.showConfirmDialog(
							this,
							"There is already "
								+ "project in this folder. Overwrite?");
					if (ret == JOptionPane.YES_OPTION) {
						finished = true;
					} else if (ret == JOptionPane.CANCEL_OPTION) {
						return;
					}
				} else {
					finished = true;
				}
			} // End while
			// Save and close (if needed) currently open project.
			if (mediator != null) {
				if (false == closeProject())
					return;
			}
			// Save dir path to the preferences
			pref.setProperty(Preferences.LAST_DIR, file.getAbsolutePath());
			try {
				GuiConfiguration.initSharedConfig(proj_file, false);
			} catch (ConfigException e) {
				logObj.warning(e.getMessage());
			}
			GuiConfiguration config = GuiConfiguration.getGuiConfig();
			Mediator mediator = Mediator.getMediator(config);
			project(mediator);
			// Set title to contain proj file path
			this.setTitle(TITLE + " - " + proj_file.getAbsolutePath());
		} catch (Exception e) {
			System.out.println("Error loading project file, " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void openProject(String file_path) {
		if (null == file_path || file_path.trim().length() == 0)
			return;
		File file = new File(file_path);
		if (!file.exists()) {
			JOptionPane.showMessageDialog(
				this,
				"Project file " + file_path + " does not exist.");
			return;
		}
		openProject(file);
	}

	/** Open specified project file. File must already exist. */
	private void openProject(File file) {
		// Save and close (if needed) currently open project.
		if (mediator != null) {
			if (false == closeProject())
				return;
		}
		Preferences pref = Preferences.getPreferences();
		String init_dir = (String) pref.getProperty(Preferences.LAST_DIR);
		try {
			// Save dir path to the preferences
			pref.setProperty(Preferences.LAST_DIR, file.getParent());
			addToLastProjList(file.getAbsolutePath());
			// Initialize gui configuration
			GuiConfiguration.initSharedConfig(file);
			GuiConfiguration config = GuiConfiguration.getGuiConfig();
			Mediator mediator = Mediator.getMediator(config);
			project(mediator);
			// Set title to contain proj file path
			this.setTitle(TITLE + " - " + file.getAbsolutePath());

		} catch (Exception e) {
			System.out.println("Error loading project file, " + e.getMessage());
			e.printStackTrace();
		}
	}

	/** Opens cayenne.xml file using file chooser. */
	private void openProject() {
		Preferences pref = Preferences.getPreferences();
		String init_dir = (String) pref.getProperty(Preferences.LAST_DIR);
		try {
			// Get the project file name (always cayenne.xml)
			File file = null;
			fileChooser.setFileFilter(new ProjectFileFilter());
			fileChooser.setDialogTitle("Choose project file (cayenne.xml)");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (null != init_dir) {
				File init_dir_file = new File(init_dir);
				if (init_dir_file.exists())
					fileChooser.setCurrentDirectory(init_dir_file);
			}
			int ret_code = fileChooser.showOpenDialog(this);
			if (ret_code != JFileChooser.APPROVE_OPTION)
				return;
			file = fileChooser.getSelectedFile();
			openProject(file);
		} catch (Exception e) {
			System.out.println("Error loading project file, " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void project(Mediator mediator) {
		this.mediator = mediator;

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

		createDomainMenu.setEnabled(true);
		createDomainBtn.setEnabled(true);
		closeProjectMenu.setEnabled(true);

		this.validate();
	}

	private void createDomain() {
		DataDomain domain =
			(DataDomain) NamedObjectFactory.createObject(
				DataDomain.class,
				mediator.getConfig());
		mediator.getConfig().addDomain(domain);
		mediator.fireDomainEvent(
			new DomainEvent(this, domain, DomainEvent.ADD));
		mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, domain));
	}

	/** 
	 * Creates a new data node. Data node may consist of two pieces of information:
	 * <ul>
	 *   <li>Name/location</li>
	 *   <li>Database url/uid/password (for direct connection to DB).</li>
	 * </ul>
	 * 
	 * First piece of info is stored directly into the cayenne.xml.
	 * Second piece of data should be stored in the separate file
	 * if the factory requires it. 
	 */
	private void createDataNode() {
		DataDomain domain = mediator.getCurrentDataDomain();
		DataNode node =
			(DataNode) NamedObjectFactory.createObject(DataNode.class, domain);
		GuiDataSource src = new GuiDataSource(new DataSourceInfo());
		node.setDataSource(src);

		domain.addNode(node);
		mediator.fireDataNodeEvent(
			new DataNodeEvent(this, node, DataNodeEvent.ADD));
		mediator.fireDataNodeDisplayEvent(
			new DataNodeDisplayEvent(this, domain, node));
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
		enableDataMapMenu();
		getAction(RemoveAction.ACTION_NAME).setName("Remove ObjEntity");
	}

	public void currentDbEntityChanged(EntityDisplayEvent e) {
		enableDataMapMenu();
		getAction(RemoveAction.ACTION_NAME).setName("Remove DbEntity");
	}

	public void currentDbAttributeChanged(AttributeDisplayEvent e) {
		enableDataMapMenu();
		if (e.getAttribute() != null) {
			getAction(RemoveAction.ACTION_NAME).setName("Remove DbAttribute");
		}
	}

	public void currentObjAttributeChanged(AttributeDisplayEvent e) {
		enableDataMapMenu();
		if (e.getAttribute() != null) {
			getAction(RemoveAction.ACTION_NAME).setName("Remove ObjAttribute");
		}
	}

	public void currentDbRelationshipChanged(RelationshipDisplayEvent e) {
		enableDataMapMenu();
		if (e.getRelationship() != null) {
            getAction(RemoveAction.ACTION_NAME).setName("Remove DbRelationship");
		}
	}

	public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
		enableDataMapMenu();
		if (e.getRelationship() != null) {
            getAction(RemoveAction.ACTION_NAME).setName("Remove ObjRelationship");
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

		createDataSourceMenu.setEnabled(false);
		createObjEntityMenu.setEnabled(false);
		createDbEntityMenu.setEnabled(false);
		addDataMapMenu.setEnabled(false);
		synchObjEntityMenu.setEnabled(false);
 
		saveMenu.setEnabled(false);
	

		importDbMenu.setEnabled(false);
		importEOMMenu.setEnabled(false);
		generateMenu.setEnabled(false);
		generateDbMenu.setEnabled(false);
		setPackageMenu.setEnabled(false);

		createDataSourceBtn.setEnabled(false);
		createObjEntityBtn.setEnabled(false);
		createDbEntityBtn.setEnabled(false);
	}

	private void enableDomainMenu() {
		disableMenu();
		actionMap.get(CreateDataMapAction.ACTION_NAME).setEnabled(true);
		getAction(RemoveAction.ACTION_NAME).setEnabled(true);

		createDataSourceMenu.setEnabled(true);
		closeProjectMenu.setEnabled(true);
		saveMenu.setEnabled(true);
		importDbMenu.setEnabled(true);
		importEOMMenu.setEnabled(true);

		createDomainBtn.setEnabled(true);
		createDataSourceBtn.setEnabled(true);
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
		synchObjEntityMenu.setEnabled(true);
	}

	private void enableDataNodeMenu() {
		enableDomainMenu();
		addDataMapMenu.setEnabled(true);
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
}