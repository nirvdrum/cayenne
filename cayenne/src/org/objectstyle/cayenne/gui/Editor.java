package org.objectstyle.cayenne.gui;
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


//import java.awt.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.collections.ExtendedProperties;

import org.objectstyle.util.Preferences;
import org.objectstyle.cayenne.conf.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.gui.datamap.*;
import org.objectstyle.cayenne.gui.validator.*;


/** Window for the Cayenne Modeler.
  * Responsibilities include coordination of enabling/disabling of
  * menu and toolbar.
  * @author Michael Misha Shengaout */
public class Editor extends JFrame
implements ActionListener
, DomainDisplayListener, DataNodeDisplayListener, DataMapDisplayListener
, ObjEntityDisplayListener, DbEntityDisplayListener
{
	static Logger logObj = Logger.getLogger(Editor.class.getName());

	private static final String TITLE = "Cayenne modeler";
	/** To indicate in title that the proj is dirty. */
	private static final String DIRTY_STRING = "* - ";

    EditorView view;
    Mediator mediator;
    /** The object last selected in BrowseView. */
    Object context;

    JMenuBar  menuBar    	= new JMenuBar();
    JMenu  fileMenu    			= new JMenu("File");
    JMenuItem  openProjectMenu  = new JMenuItem("Open Project");
    JMenuItem  createProjectMenu= new JMenuItem("New Project");
    JMenuItem  closeProjectMenu = new JMenuItem("Close Project");
    JMenuItem saveMenu   		= new JMenuItem("Save");
    JMenuItem  exitMenu    		= new JMenuItem("Exit");
    ArrayList lastOpenProjMenus = new ArrayList();
/*
	JMenu editMenu			= new JMenu("Edit");
	JMenuItem cutMenu		= new JMenu("Cut");
	JMenuItem copyMenu		= new JMenu("Copy");
	JMenuItem pasteMenu		= new JMenu("Paste");
*/
	JMenu projectMenu				= new JMenu("Project");
    JMenuItem createDomainMenu 		= new JMenuItem("Create Domain");
    JMenuItem createDataMapMenu 	= new JMenuItem("Create Data Map");
    JMenuItem createDataSourceMenu	= new JMenuItem("Create Data Source");
    JMenuItem createObjEntityMenu 	= new JMenuItem("Create Object Entity");
    JMenuItem createDbEntityMenu 	= new JMenuItem("Create DB Entity");
    JMenuItem removeMenu 			= new JMenuItem("Remove");

    JMenu  toolMenu   		= new JMenu("Tools");
    JMenuItem importDbMenu  = new JMenuItem("Reverse engineer database");
    JMenuItem generateMenu  = new JMenuItem("Generate Classes");
    JMenuItem generateDbMenu  = new JMenuItem("Generate Database");
    JMenuItem setPackageMenu= new JMenuItem("Set Package Name For Obj Entities");

    JMenu helpMenu				= new JMenu("Help");
    JMenuItem aboutMenu  		= new JMenuItem("About");

	JToolBar toolBar		= new JToolBar();
	JButton  createDomainBtn;
	JButton  createDataMapBtn;
	JButton  createDataSourceBtn;
	JButton  createObjEntityBtn;
	JButton  createDbEntityBtn;
	JButton	 removeBtn;

    //Create a file chooser
    final JFileChooser fileChooser   = new JFileChooser();
    XmlFilter xmlFilter    			 = new XmlFilter();

	private static Editor frame;

    private Editor() {
        super(TITLE);

		frame = this;

        init();
		disableMenu();
		closeProjectMenu.setEnabled(false);
        createDomainMenu.setEnabled(false);
        createDomainBtn.setEnabled(false);

        createProjectMenu.addActionListener(this);
        createDomainMenu.addActionListener(this);
        createDataMapMenu.addActionListener(this);
        createDataSourceMenu.addActionListener(this);
        createObjEntityMenu.addActionListener(this);
        createDbEntityMenu.addActionListener(this);
        removeMenu.addActionListener(this);
        removeMenu.setAccelerator(KeyStroke.getKeyStroke(
				        	KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        openProjectMenu.addActionListener(this);
        openProjectMenu.setAccelerator(KeyStroke.getKeyStroke(
				        	KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        closeProjectMenu.addActionListener(this);
        saveMenu.addActionListener(this);
        saveMenu.setAccelerator(KeyStroke.getKeyStroke(
				        	KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        exitMenu.addActionListener(this);

        importDbMenu.addActionListener(this);
        generateMenu.addActionListener(this);
        generateDbMenu.addActionListener(this);
        setPackageMenu.addActionListener(this);
        aboutMenu.addActionListener(this);

		createDomainBtn.addActionListener(this);
		createDataMapBtn.addActionListener(this);
		createDataSourceBtn.addActionListener(this);
		createObjEntityBtn.addActionListener(this);
		createDbEntityBtn.addActionListener(this);
		removeBtn.addActionListener(this);

	    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    	setSize(650, 550);

    	this.addWindowListener(new WindowAdapter() {
    		public void windowClosing(WindowEvent e) {
    			exitEditor();
    		}
    	});
    }


    private void init() {
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
        projectMenu.add(createDataMapMenu);
        projectMenu.add(createDataSourceMenu);
        projectMenu.add(createObjEntityMenu);
        projectMenu.add(createDbEntityMenu);
        projectMenu.addSeparator();
        projectMenu.add(removeMenu);

        toolMenu.add(importDbMenu);
        toolMenu.add(generateMenu);
        toolMenu.add(generateDbMenu);
        toolMenu.addSeparator();
        toolMenu.add(setPackageMenu);

        helpMenu.add(aboutMenu);

        initToolBar();
    }


    private void reloadLastProjList() {
		// Get list of last opened proj files and trim it down to 4
    	Preferences pref = Preferences.getPreferences();
       	Vector arr = pref.getVector(Preferences.LAST_PROJ_FILES);
		while (arr.size() > 4)
			arr.remove(arr.size()-1);
		for (int i = 0; i < arr.size(); i++) {
			if (lastOpenProjMenus.size() <= i) {
				JMenuItem item = new JMenuItem((String)arr.get(i));
				fileMenu.insert(item, lastOpenProjMenus.size()+6);
				lastOpenProjMenus.add(item);
				item.addActionListener(this);
			} else {
				JMenuItem item = (JMenuItem)lastOpenProjMenus.get(i);
				item.setText((String)arr.get(i));
			}
		}
    }

	/** Adds path to the list of last opened projects in preferences. */
	private void addToLastProjList(String path) {
    	Preferences pref = Preferences.getPreferences();
       	Vector arr = pref.getVector(Preferences.LAST_PROJ_FILES);
		// Add proj path to the preferences
		// Prevent duplicate entries.
		if (arr.contains(path)) {
			arr.remove(path);
		}
		arr.insertElementAt(path, 0);
		while (arr.size() > 4)
			arr.remove(arr.size()-1);
		pref.remove(Preferences.LAST_PROJ_FILES);
		Iterator iter = arr.iterator();
		while (iter.hasNext())
			pref.addProperty(Preferences.LAST_PROJ_FILES, iter.next());
	}


    private void initToolBar() {
    	String path = "org/objectstyle/gui/";

    	ClassLoader cl = BrowseView.BrowseViewRenderer.class.getClassLoader();
    	URL url = cl.getResource(path + "images/domain24_grey.gif");
        ImageIcon domainIcon = new ImageIcon(url);
        createDomainBtn = new JButton(domainIcon);
        createDomainBtn.setToolTipText("Create new domain");
        toolBar.add(createDomainBtn);

    	url = cl.getResource(path + "images/map24_grey.gif");
    	ImageIcon mapIcon = new ImageIcon(url);
    	createDataMapBtn = new JButton(mapIcon);
    	createDataMapBtn.setToolTipText("Create new data map");
        toolBar.add(createDataMapBtn);

    	url = cl.getResource(path + "images/node24_grey.gif");
    	ImageIcon nodeIcon = new ImageIcon(url);
    	createDataSourceBtn = new JButton(nodeIcon);
    	createDataSourceBtn.setToolTipText("Create new data node");
        toolBar.add(createDataSourceBtn);

    	url = cl.getResource(path + "images/objentity24_grey.gif");
    	ImageIcon objEntityIcon = new ImageIcon(url);
    	createObjEntityBtn = new JButton(objEntityIcon);
    	createObjEntityBtn.setToolTipText("Create new obj entity");
        toolBar.add(createObjEntityBtn);

    	url = cl.getResource(path + "images/dbentity24_grey.gif");
    	ImageIcon dbEntityIcon = new ImageIcon(url);
    	createDbEntityBtn = new JButton(dbEntityIcon);
    	createDbEntityBtn.setToolTipText("Create new db entity");
        toolBar.add(createDbEntityBtn);

    	url = cl.getResource(path + "images/remove24_grey.gif");
    	ImageIcon removeIcon = new ImageIcon(url);
    	removeBtn = new JButton(removeIcon);
    	removeBtn.setToolTipText("Remove current");
        toolBar.add(removeBtn);

    	getContentPane().add(toolBar, BorderLayout.NORTH);
    }


	/** Singleton implementation of getting Editor window. */
	public static Editor getFrame() {
		return frame;
	}

	/** Adds asterisk to the title of the window to indicate it is dirty. */
	public void setDirty(boolean dirty_flag) {
		String title = getTitle();
		if (dirty_flag) {
			if (!title.startsWith(DIRTY_STRING))
				setTitle(DIRTY_STRING + title);
		} else {
			if (title.startsWith(DIRTY_STRING))
				setTitle(title.substring(DIRTY_STRING.length(), title.length()));
		}
	}

	/** Return false if cancel closing the window, true otherwise. */
	private boolean checkSaveOnClose() {
		if (mediator != null && mediator.isDirty())
		{
			int ret_code = JOptionPane.showConfirmDialog(this
											, "You have unsaved data. "
											+ "Do you want to save it?");
			if (ret_code == JOptionPane.CANCEL_OPTION)
				return false;
			else if (ret_code == JOptionPane.YES_OPTION)
				saveAll();
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
        } else if (src == createDataMapMenu || src == createDataMapBtn) {
        	this.createDataMap();
    	} else if (src == createDataSourceMenu || src == createDataSourceBtn) {
    		createDataNode();
        } else if (src == createObjEntityMenu || src == createObjEntityBtn) {
        	createObjEntity();
        } else if (src == createDbEntityMenu || src == createDbEntityBtn) {
        	createDbEntity();
        } else if (src == removeMenu || src == removeBtn) {
        	remove();
        } else if (src == saveMenu) {
            saveAll();
        } else if (src == importDbMenu) {
            importDb();
        } else if (src == setPackageMenu) {
        	// Set the same package name for all obj entities.
            setPackageName();
        } else if (src == generateMenu) {
            generateClasses();
        } else if (src == generateDbMenu) {
            generateDb();
        } else if (src == exitMenu) {
        	exitEditor();
        } else if (src == aboutMenu) {
        	AboutDialog win = new AboutDialog(this);
        } else if (lastOpenProjMenus.contains(src)) {
        	openProject(((JMenuItem)src).getText());
        }
    }

	private void setPackageName()
	{
		DataMap map = mediator.getCurrentDataMap();
		if (map == null) {
			return;
		}
		String package_name;
		package_name = JOptionPane.showInputDialog(this
												, "Enter the new package name");
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
		ObjEntity [] entities = map.getObjEntities();
		for (int i = 0; i < entities.length; i++) {
			String name = entities[i].getClassName();			
			int idx = name.lastIndexOf('.');
			if (idx > 0) {
				if (idx == name.length() -1)
					name = NameGenerator.getObjEntityName();
				else
					name = name.substring(idx+1);
			}
			entities[i].setClassName(package_name + name);
		}// End for()
		mediator.fireDataMapEvent(new DataMapEvent(this, map));
	}

	private void remove()
	{
		int ret;
		if (context instanceof DataDomain) {
			ret = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to remove "
				+ ((DataDomain)context).getName() + " data domain?"
				, "Cayenne", JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION)
				mediator.removeDomain(this, (DataDomain)context);
		} else if (context instanceof DataNode) {
			ret = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to remove "
				+ ((DataNode)context).getName() + " data node?"
				, "Cayenne", JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION)
				mediator.removeDataNode(this, (DataNode)context);
		} else if (context instanceof DataMap) {
			ret = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to remove "
				+ ((DataMap)context).getName() + " data map?"
				, "Cayenne", JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION)
				removeDataMap();
		} else if (context instanceof DbEntity) {
			ret = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to remove "
				+ ((DbEntity)context).getName() + " db entity?"
				, "Cayenne", JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION)
				mediator.removeDbEntity(this, (DbEntity)context);
		} else if (context instanceof ObjEntity) {
			ret = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to remove "
				+ ((ObjEntity)context).getName() + " obj entity?"
				, "Cayenne", JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION)
				mediator.removeObjEntity(this, (ObjEntity)context);
		}

	}
	
	/** Removing data map either from node or from everywhere based on context.*/
	private void removeDataMap() {
		DataNode node = mediator.getCurrentDataNode();
		DataMap map = mediator.getCurrentDataMap();
		if (null == node)
			mediator.removeDataMap(this, (DataMap)context);
		else {
			DataMap[] maps = node.getDataMaps();
			boolean found = false;
			for (int i = 0; i < maps.length; i++) {
				if (map == maps[i]) {
					found = true;
					break;
				}
			}
			if (!found)
				return;
			DataMap[] new_maps = new DataMap[maps.length-1];
			int new_map_idx = 0;
			for (int i = 0; i < maps.length; i++) {
				if (map == maps[i])
					continue;
				new_maps[new_map_idx] = maps[i];
			}
			node.setDataMaps(new_maps);
			mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
		}
	}

	private void generateClasses() {
		GenerateClassDialog dialog;
		dialog = new GenerateClassDialog(this, mediator);
		dialog.show();
		dialog.dispose();
	}

	/** Returns true if successfully closed project, false otherwise. */
	private boolean closeProject()
	{
		if (false == checkSaveOnClose())
			return false;
		reloadLastProjList();
        getContentPane().remove(view);
        view = null;
        mediator = null;
        repaint();
        disableMenu();

        closeProjectMenu.setEnabled(false);
        createDomainMenu.setEnabled(false);
        createDomainBtn.setEnabled(false);
        removeMenu.setText("Remove");
        removeBtn.setToolTipText("Remove");
        // Take path of the proj away from the title
		this.setTitle(TITLE);
		return true;
	}

	private void generateDb() {
        DataSourceInfo dsi = new DataSourceInfo();
        Connection conn = null;
        DbAdapter adapter = null;
        // Get connection
        while (conn == null) {
	        InteractiveLogin loginObj = InteractiveLogin.getGuiLoginObject(dsi);
	        loginObj.collectLoginInfo();
	        // connect
	        dsi = loginObj.getDataSrcInfo();
	        if (null == dsi) {
	        	return;
	        }
	        if (dsi.getAdapterClass() == null 
	        	|| dsi.getAdapterClass().trim().length() == 0) {
	        	JOptionPane.showMessageDialog(this, "Must specify DB Adapter");
	        	continue;
	        }
	        try {
		        Driver driver = (Driver)Class.forName(dsi.getJdbcDriver()).newInstance();
		        conn = DriverManager.getConnection(
		              					dsi.getDataSourceUrl(),
		                   				dsi.getUserName(),
		                   				dsi.getPassword());
		        adapter = (DbAdapter)Class.forName(dsi.getAdapterClass()).newInstance();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error Connecting to the Database"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (InstantiationException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error creating DbAdapter"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error creating DbAdapter"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error creating DbAdapter"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			}
		}// End while()

		DataMap map = mediator.getCurrentDataMap();
		try {
			GenerateDbDialog dialog;
			dialog = new GenerateDbDialog(mediator, conn, adapter);
		} 
		finally {
			try {
				conn.close();
			} catch (Exception e) {e.printStackTrace();}
		}
	}// End generateDb()




	private void importDb() {
        DataSourceInfo dsi = new DataSourceInfo();
        Connection conn = null;
        // Get connection
        while (conn == null) {
	        InteractiveLogin loginObj = InteractiveLogin.getGuiLoginObject(dsi);
	        loginObj.collectLoginInfo();
	        // connect
	        dsi = loginObj.getDataSrcInfo();
	        if (null == dsi) {
	        	return;
	        }
	        try {
		        Driver driver = (Driver)Class.forName(dsi.getJdbcDriver()).newInstance();
		        conn = DriverManager.getConnection(
		              					dsi.getDataSourceUrl(),
		                   				dsi.getUserName(),
		                   				dsi.getPassword());
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				SQLException ex = e.getNextException();
				if (ex != null) {
					System.out.println(ex.getMessage());
				}
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error Connecting to the Database"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error loading driver"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			}
		}// End while()

		ArrayList schemas;
		DbLoader loader = new DbLoader(conn);
		try {
			schemas = loader.getSchemas();
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error loading schemas"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		String schema_name = null;
		if (schemas.size() != 0) {
			ChooseSchemaDialog dialog = new ChooseSchemaDialog(schemas);
			dialog.show();
			if (dialog.getChoice() == ChooseSchemaDialog.CANCEL) {
				dialog.dispose();
				return;
			}
			schema_name = dialog.getSchemaName();
			dialog.dispose();
		}
		if (schema_name != null && schema_name.length() == 0)
			schema_name = null;
		DataMap map;
		try {
			map = mediator.getCurrentDataMap();
			if (map != null )
				loader.loadDataMapFromDB(schema_name, null, map);
			else
				map = loader.createDataMapFromDB(schema_name);
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error reverse engineering database"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		// If this is adding to existing data map, remove it
		// and re-add to the BroseView
		if (mediator.getCurrentDataMap() != null) {
			mediator.fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.REMOVE));
			mediator.fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.ADD));
			mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(this
												, map
												, mediator.getCurrentDataDomain()
												, mediator.getCurrentDataNode()));
		} else {
			mediator.addDataMap(this, map);
		}
	}


	private void createObjEntity() {
		ObjEntity entity = EntityWrapper.createObjEntity();
		mediator.getCurrentDataMap().addObjEntity(entity);
		mediator.fireObjEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
		mediator.fireObjEntityDisplayEvent(
				new EntityDisplayEvent(this, entity
									, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain()
									, mediator.getCurrentDataNode()));
	}

	private void createDbEntity() {
		DbEntity entity = EntityWrapper.createDbEntity();
		mediator.getCurrentDataMap().addDbEntity(entity);
		mediator.fireDbEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
		mediator.fireDbEntityDisplayEvent(
				new EntityDisplayEvent(this, entity
									, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain()
									, mediator.getCurrentDataNode()));
	}

    private void createProject() {
    	Preferences pref = Preferences.getPreferences();
       	String init_dir = (String)pref.getProperty(Preferences.LAST_DIR);
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            fileChooser.setFileFilter(new ProjFileFilter());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Choose new project directory");
            if (null != init_dir) {
            	File init_dir_file = new File(init_dir);
            	if (init_dir_file.exists())
            		fileChooser.setCurrentDirectory(init_dir_file);
            }
            int ret_code = fileChooser.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fileChooser.getSelectedFile();
            if (!file.exists())
            	file.createNewFile();
			// Save and close (if needed) currently open project.
    		if (mediator != null) {
    			if (false == closeProject())
    				return;
    		}
            // Save dir path to the preferences
            pref.setProperty(Preferences.LAST_DIR, file.getParent());
            // Create project file (cayenne.xml)
            File proj_file = new File(file.getAbsolutePath()
            							+ File.separator
            							+ "cayenne.xml");
            if (!proj_file.exists())
            	proj_file.createNewFile();
            addToLastProjList(proj_file.getAbsolutePath());

			FileWriter fw = new FileWriter(proj_file);
			PrintWriter pw = new PrintWriter(fw, true);
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<domains>");
			pw.println("</domains>");
			pw.flush();
			pw.close();
			fw.close();
            GuiConfiguration.initSharedConfig(proj_file);
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
			JOptionPane.showMessageDialog(this,
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
       	String init_dir = (String)pref.getProperty(Preferences.LAST_DIR);
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
    /** Open cayenne.xml file using file chooser. */
    private void openProject() {
    	Preferences pref = Preferences.getPreferences();
       	String init_dir = (String)pref.getProperty(Preferences.LAST_DIR);
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            fileChooser.setFileFilter(new ProjFileFilter());
            fileChooser.setDialogTitle("Choose project file (cayenne.xml)");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (null != init_dir) {
            	File init_dir_file = new File(init_dir);
            	if (init_dir_file.exists())
            		fileChooser.setCurrentDirectory(init_dir_file);
            }
            int ret_code = fileChooser.showOpenDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fileChooser.getSelectedFile();
            openProject(file);
        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void project(Mediator temp_mediator) {
        mediator = temp_mediator;

        view = new EditorView(mediator);
        getContentPane().add(view, BorderLayout.CENTER);

        mediator.addDomainDisplayListener(this);
        mediator.addDataNodeDisplayListener(this);
        mediator.addDataMapDisplayListener(this);
        mediator.addObjEntityDisplayListener(this);
        mediator.addDbEntityDisplayListener(this);

        createDomainMenu.setEnabled(true);
        createDomainBtn.setEnabled(true);
        closeProjectMenu.setEnabled(true);

        this.validate();
    }


	/** Save data map to a different location.
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	private void saveMapAs(DataMap map) {
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
            File proj_dir = null;
            if (proj_dir_str != null)
            	proj_dir = new File(proj_dir_str);
            JFileChooser fc;
            FileSystemViewDecorator file_view;
            file_view = new FileSystemViewDecorator(proj_dir);
            fc = new JFileChooser(file_view);
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setDialogTitle("Save data map - " + map.getName());
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            String old_loc = map.getLocation();
            // Get absolute path for old location
            if (null != proj_dir)
            	old_loc = proj_dir + File.separator + old_loc;
			// Create new file
			if (!file.exists())
				file.createNewFile();
			MapLoader saver = new MapLoaderImpl();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			saver.storeDataMap(pw, map);
			pw.close();
			fw.close();
			// Determine and set new data map name
			String new_file_name = file.getName();
			String new_name;
			int index = new_file_name.indexOf(".");
			if (index >= 0)
				new_name = new_file_name.substring(0, index);
			else
				new_name = new_file_name;
			map.setName(new_name);
			// Determine and set new data map location
			String new_file_location = file.getAbsolutePath();
			String relative_location;
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
			 	relative_location = new_file_location;
			else
				relative_location
					= new_file_location.substring(proj_dir_str.length() + 1);
			map.setLocation(relative_location);
            // If data map already exists, delete old location after saving new
            if (null != old_loc) {
            	System.out.println("Old location is " + old_loc);
            	File old_loc_file = new File(old_loc);
            	if (old_loc_file.exists()) {
            		System.out.println("Deleting old file");
            		old_loc_file.delete();
            	}
            }
            // Map location changed - mark current domain dirty
			mediator.fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.CHANGE));

        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
	}


	/** Save data node (DataSourceInfo) to a different location.
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	private void saveNodeAs(DataNode node) {
		GuiDataSource src = (GuiDataSource)node.getDataSource();
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
            File proj_dir = null;
            if (proj_dir_str != null)
            	proj_dir = new File(proj_dir_str);
            JFileChooser fc;
            FileSystemViewDecorator file_view;
            file_view = new FileSystemViewDecorator(proj_dir);
            fc = new JFileChooser(file_view);
            fc.setFileFilter(xmlFilter);
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setDialogTitle("Save data node - " + node.getName());
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            String old_loc = node.getDataSourceLocation();
            // Get absolute path for old location
            if (null != proj_dir)
            	old_loc = proj_dir + File.separator + old_loc;
			// Create new file
			if (!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			DomainHelper.storeDataNode(pw, src.getDataSourceInfo());
			pw.close();
			fw.close();
			// Determine and set new data map location
			String new_file_location = file.getAbsolutePath();
			String relative_location;
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
			 	relative_location = new_file_location;
			else
				relative_location
					= new_file_location.substring(proj_dir_str.length() + 1);
			node.setDataSourceLocation(relative_location);
            // If data map already exists, delete old location after saving new
            if (null != old_loc) {
            	System.out.println("Old location is " + old_loc);
            	File old_loc_file = new File(old_loc);
            	if (old_loc_file.exists()) {
            		System.out.println("Deleting old file");
            		old_loc_file.delete();
            	}
            }
            // Map location changed - mark current domain dirty
			mediator.fireDataNodeEvent(new DataNodeEvent(this, node, DataNodeEvent.CHANGE));

        } catch (Exception e) {
            System.out.println("Error saving DataNode " + node.getName() +": " + e.getMessage());
            e.printStackTrace();
        }
	}

	private void save() {
		if (mediator.getCurrentDataMap() != null) {
			saveDataMap(mediator.getCurrentDataMap());
		}
		else
			saveProject();
	}

	private void saveAll() {
		Validator val = new Validator(mediator);
		int ret_code = val.validate();
		// If no errors or no serious errors, save.
		if (ret_code == ErrorMsg.NO_ERROR || ret_code == ErrorMsg.WARNING) {
			Iterator iter = mediator.getDirtyDataMaps().iterator();
			while (iter.hasNext()) {
				DataMap map = (DataMap)iter.next();
				saveDataMap(map);
			}// End saving maps
			mediator.getDirtyDataMaps().clear();

			iter = mediator.getDirtyDataNodes().iterator();
			while (iter.hasNext()) {
				DataNode node = (DataNode)iter.next();
				logObj.fine("Editor::saveAll(), node name "
									+ node.getName() + ", factory "
									+ node.getDataSourceFactory());
				// If using direct connection, save into separate file
				if (node.getDataSourceFactory().equals(DataSourceFactory.DIRECT_FACTORY)) {
					logObj.fine("Editor::saveAll(), saving node name "
									+ node.getName());
					saveDataNode(node);
				}
			}// End saving DataNode-s
			saveProject();
			mediator.getDirtyDomains().clear();
			mediator.getDirtyDataNodes().clear();

			mediator.setDirty(false);
		}
		// If there were errors or warnings at validation, display them
		if (ret_code == ErrorMsg.ERROR || ret_code == ErrorMsg.WARNING) {
			ValidatorDialog dialog;
			dialog = new ValidatorDialog(this, mediator
								, val.getErrorMessages(), ret_code);
			dialog.setVisible(true);
		}

	}

	private void saveProject() {
		File file = mediator.getConfig().getProjFile();
		System.out.println("Saving project to " + file.getAbsolutePath());
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			DomainHelper.storeDomains(new PrintWriter(fw), mediator.getDomains());
			fw.flush();
			fw.close();
			mediator.getDirtyDomains().clear();
			if (mediator.getDirtyDataMaps().size() <=0
				&& mediator.getDirtyDataNodes().size() <=0 )
			{
				mediator.setDirty(false);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/** Save data map to the file. */
	private void saveDataMap(DataMap map) {
		try {
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
			file = new File(proj_dir_str + File.separator + map.getLocation());
			if (!file.exists()) {
				saveMapAs(map);
				return;
			}
			MapLoader saver = new MapLoaderImpl();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			saver.storeDataMap(pw, map);
			pw.close();
			fw.close();
			/* Causes problem with concurrent update
			mediator.getDirtyDataMaps().remove(map);
			if (mediator.getDirtyDataMaps().size() <=0
				&& mediator.getDirtyDataNodes().size() <=0
				&& mediator.getDirtyDomains().size() <= 0)
			{
				mediator.setDirty(false);
			}
			*/
		} catch (Exception e) {}
	}


	/** Save data source info if data source factory is DIRECT_FACTORY. */
	private void saveDataNode(DataNode node) {
		try {
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
			file = new File(proj_dir_str + File.separator + node.getDataSourceLocation());
			if (!file.exists()) {
				System.out.println("Editor::saveDataNode(), "
									+"calling save as for node name "
									+ node.getName());
				saveNodeAs(node);
				return;
			}
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			GuiDataSource src = (GuiDataSource)node.getDataSource();
			System.out.println("Editor::saveDataNode(), node name "
								+ node.getName());
			DomainHelper.storeDataNode(pw, src.getDataSourceInfo());
			pw.close();
			fw.close();
		} catch (Exception e) {
            System.out.println("SaveDataNode(), Error saving DataNode "
            				+ node.getName()  +": " + e.getMessage());
            e.printStackTrace();
		}
	}

	private void createDomain() {
		DataDomain domain = new DataDomain(NameGenerator.getDomainName());
		mediator.getConfig().addDomain(domain);
		mediator.fireDomainEvent(new DomainEvent(this, domain, DomainEvent.ADD));
		mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, domain));
	}

	/** Creates a new data node. Data node may consist of two pieces of information:
	  * 1. Name/location
	  * 2. Database url/uid/password (for direct connection to DB).
	  * First piece of info is stored directly into the cayenne.xml.
	  * Second piece of data should be stored in the separate file
	  * if the factory requires it. */
	private void createDataNode() {
		DataNode node = new DataNode(NameGenerator.getDataNodeName());
		GuiDataSource src;
		src = new GuiDataSource(new DataSourceInfo());
		node.setDataSource(src);
		DataDomain domain = mediator.getCurrentDataDomain();
		domain.addNode(node);
		mediator.fireDataNodeEvent(new DataNodeEvent(this, node, DataNodeEvent.ADD));
		mediator.fireDataNodeDisplayEvent(new DataNodeDisplayEvent(this, domain, node));
	}


	private void addDataMap() {
		DataNode node = mediator.getCurrentDataNode();
		List map_list = mediator.getCurrentDataDomain().getMapList();
		AddDataMapDialog dialog = new AddDataMapDialog(node, map_list);
		System.out.println("Node has " + node.getDataMaps().length + " maps");
		mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
	}

	private void createDataMap() {
		// If have current data node, don't create new data map, add to it 
		// the existing one.
		if (mediator.getCurrentDataNode() != null) {
			addDataMap();
			return;
		}
    	Preferences pref = Preferences.getPreferences();
       	String init_dir = (String)pref.getProperty(Preferences.LAST_DIR);
       	// Data map file
   	    File file = null;
   	    // Map location relative to proj dir
   	    String relative_location = null;
        try {
            String proj_dir_str = mediator.getConfig().getProjDir();
            File proj_dir = null;
            if (proj_dir_str != null)
            	proj_dir = new File(proj_dir_str);
            JFileChooser fc;
            FileSystemViewDecorator file_view;
            file_view = new FileSystemViewDecorator(proj_dir);
            // Get the data map file name
            fc = new JFileChooser(file_view);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setDialogTitle("Enter data map file name");
            if (null != init_dir) {
            	File init_dir_file = new File(init_dir);
            	if (init_dir_file.exists())
            		fc.setCurrentDirectory(init_dir_file);
            }
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
            if (!file.exists())
            	file.createNewFile();
			String new_file_location = file.getAbsolutePath();
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
			 	relative_location = new_file_location;
			else
				relative_location
					= new_file_location.substring(proj_dir_str.length() + 1);
        } catch (Exception e) {
            System.out.println("Error lcreating data map file, " + e.getMessage());
            e.printStackTrace();
        }
		DataMap map = new DataMap(NameGenerator.getDataMapName());
		map.setLocation(relative_location);
		mediator.addDataMap(this, map);
	}


	public void currentDomainChanged(DomainDisplayEvent e){
		enableDomainMenu();
		createDataMapMenu.setText("Create Data Map");
		createDataMapBtn.setToolTipText("Create data map");
		removeMenu.setText("Remove Domain");
		removeBtn.setToolTipText("Remove current domain");
		context = e.getDomain();
	}

	public void currentDataNodeChanged(DataNodeDisplayEvent e){
		enableDomainMenu();
		createDataMapMenu.setText("Add Data Map");
		createDataMapBtn.setToolTipText("Add data map");
		removeMenu.setText("Remove Data Node");
		removeBtn.setToolTipText("Remove curent data node");
		context = e.getDataNode();
	}

	public void currentDataMapChanged(DataMapDisplayEvent e){
		enableDataMapMenu();
		removeMenu.setText("Remove Data Map");
		removeBtn.setToolTipText("Remove current data map");
		context = e.getDataMap();
	}

   	public void currentObjEntityChanged(EntityDisplayEvent e)
   	{
		enableDataMapMenu();
		if (mediator.getCurrentDataNode() == null) {
			createDataMapMenu.setText("Create Data Map");
			createDataMapBtn.setToolTipText("Create data map");
		} else {
			createDataMapMenu.setText("Add Data Map");
			createDataMapBtn.setToolTipText("Add data map");
		}
		removeMenu.setText("Remove Obj Entity");
		removeBtn.setToolTipText("Remove current obj entity");
		context = e.getEntity();
   	}


   	public void currentDbEntityChanged(EntityDisplayEvent e)
   	{
		enableDataMapMenu();
		if (mediator.getCurrentDataNode() == null) {
			createDataMapMenu.setText("Create Data Map");
			createDataMapBtn.setToolTipText("Create data map");
		} else {
			createDataMapMenu.setText("Add Data Map");
			createDataMapBtn.setToolTipText("Add data map");
		}
		removeMenu.setText("Remove Db Entity");
		removeBtn.setToolTipText("Remove current db entity");
		context = e.getEntity();
   	}


    /** Disables all menu  for the case when no project is open.
      * The only menu-s never disabled are "New Project", "Open Project"
      * and "Exit". */
    private void disableMenu() {
        createDataMapMenu.setEnabled(false);
        createDataSourceMenu.setEnabled(false);
        createObjEntityMenu.setEnabled(false);
        createDbEntityMenu.setEnabled(false);

        saveMenu.setEnabled(false);
        removeMenu.setEnabled(false);

        importDbMenu.setEnabled(false);
        generateMenu.setEnabled(false);
        generateDbMenu.setEnabled(false);
		setPackageMenu.setEnabled(false);

        createDomainBtn.setEnabled(false);
        createDataMapBtn.setEnabled(false);
        createDataSourceBtn.setEnabled(false);
        createObjEntityBtn.setEnabled(false);
        createDbEntityBtn.setEnabled(false);
        removeBtn.setEnabled(false);

    }

	private void enableDomainMenu() {
		disableMenu();
		createDataMapMenu.setEnabled(true);
		createDataSourceMenu.setEnabled(true);
		closeProjectMenu.setEnabled(true);
	    saveMenu.setEnabled(true);
        importDbMenu.setEnabled(true);
        removeMenu.setEnabled(true);

		createDataMapBtn.setEnabled(true);
        createDomainBtn.setEnabled(true);
		createDataSourceBtn.setEnabled(true);
        removeBtn.setEnabled(true);
	}

	private void enableDataMapMenu() {
		enableDomainMenu();
		setPackageMenu.setEnabled(true);
        createObjEntityMenu.setEnabled(true);
        createDbEntityMenu.setEnabled(true);
        generateMenu.setEnabled(true);
        generateDbMenu.setEnabled(true);

        createObjEntityBtn.setEnabled(true);
        createDbEntityBtn.setEnabled(true);
	}

    public static void main(String[] args)
    {
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
    	frame.setLocation((screenSize.width - frameSize.width) / 2
    					 ,(screenSize.height - frameSize.height) / 2);
   		frame.setVisible(true);
   	}
}

class AddDataMapDialog extends JDialog implements ActionListener
{
	private static final int WIDTH  = 380;
	private static final int HEIGHT = 150;

	DataNode node;

	private JList list;
	private JButton add = new JButton("Add");
	private JButton cancel = new JButton("Cancel");
	
	public AddDataMapDialog(DataNode temp_node, List map_list) {
		super(Editor.getFrame(), "Add data maps to the data node", true);

		DataMap[] maps = temp_node.getDataMaps();
		if (map_list.size() == maps.length) {
			dispose();
			return;
		}

		node = temp_node;
		
		getContentPane().setLayout(new BorderLayout());
		
		list = new JList(populate(temp_node, map_list));
		getContentPane().add(list, BorderLayout.CENTER);
		
		JPanel temp = new JPanel(new FlowLayout(FlowLayout.CENTER));
		temp.add(add);
		temp.add(cancel);
		add.addActionListener(this);
		cancel.addActionListener(this);
		getContentPane().add(temp, BorderLayout.SOUTH);

		setSize(WIDTH, HEIGHT);
		JFrame frame = Editor.getFrame();
		Point point = frame.getLocationOnScreen();
		int width = frame.getWidth();
		int x = (width - WIDTH)/2;
		int height = frame.getHeight();
		int y = (height - HEIGHT)/2;
		
		point.setLocation(point.x + x, point.y + y);
		this.setLocation(point);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	private Vector populate(DataNode temp_node, List map_list) {
		DataMap[] maps = temp_node.getDataMaps();
		Vector new_maps = new Vector();
		Iterator iter = map_list.iterator();
		while(iter.hasNext()) {
			DataMap map = (DataMap)iter.next();
			System.out.println("map " + map.getName());
			boolean found = false;
			for (int i = 0; maps != null && i < maps.length; i++) {
				if (map == maps[i]) {
					found = true;
					break;
				}
			}// End for()
			if (!found)
				new_maps.add(new DataMapWrapper(map));
		}// End while()
		return new_maps;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == add) {
			Object[] sel = list.getSelectedValues();
			DataMap [] old_maps = node.getDataMaps();
			DataMap[] new_maps = new DataMap[old_maps.length + sel.length];
			for (int i = 0; i < old_maps.length; i++) {
				new_maps[i] = old_maps[i];
			}
			for (int i = 0; i < sel.length; i++) {
				new_maps[i + old_maps.length] = ((DataMapWrapper)sel[i]).getDataMap();
			}
			node.setDataMaps(new_maps);
		}// End add
		setVisible(false);
		dispose();
	}
}