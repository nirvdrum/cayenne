package org.objectstyle.cayenne.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import org.objectstyle.cayenne.conf.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.gui.datamap.*;



/** Window for the Cayenne Modeler.
  * Responsibilities include coordination of enabling/disabling of
  * menu and toolbar. */
public class Editor extends JFrame
implements ActionListener
, DomainDisplayListener, DataNodeDisplayListener, DataMapDisplayListener
{

    EditorView view;
    Mediator mediator;

    JMenuBar  menuBar    = new JMenuBar();
    JMenu  fileMenu    = new JMenu("File");
    JMenuItem  createProjectMenu  = new JMenuItem("New Project");
    JMenuItem createDomainMenu = new JMenuItem("New Domain");
    JMenuItem createDataMapMenu = new JMenuItem("New Data Map");
    JMenuItem createDataSourceMenu= new JMenuItem("New Data Source");
    JMenuItem createObjEntityMenu = new JMenuItem("New Object Entity");
    JMenuItem createDbEntityMenu = new JMenuItem("New DB Entity");
    JMenuItem  openProjectMenu  = new JMenuItem("Open Project");
    JMenuItem  openDataMapMenu  = new JMenuItem("Open Data Map");
    JMenuItem  closeProjectMenu  = new JMenuItem("Close Project");
    JMenuItem saveMenu    = new JMenuItem("Save");
    JMenuItem saveAsMenu    = new JMenuItem("Save As");
    JMenuItem saveAllMenu   = new JMenuItem("Save All");
    JMenuItem  exitMenu    = new JMenuItem("Exit");
    JMenu  toolMenu   = new JMenu("Tools");
    JMenuItem importDbMenu  = new JMenuItem("Reverse engineer database");
    JMenuItem generateMenu  = new JMenuItem("Generate Classes");

    //Create a file chooser
    final JFileChooser fileChooser   = new JFileChooser();
    XmlFilter xmlFilter    			 = new XmlFilter();

	private static Editor frame;

    private Editor() {
        super("Cayenne Modeler");

        init();
		disableMenu();

        createProjectMenu.addActionListener(this);
        createDomainMenu.addActionListener(this);
        createDataMapMenu.addActionListener(this);
        createDataSourceMenu.addActionListener(this);
        createObjEntityMenu.addActionListener(this);
        createDbEntityMenu.addActionListener(this);
        openProjectMenu.addActionListener(this);
        openDataMapMenu.addActionListener(this);
        closeProjectMenu.addActionListener(this);
        saveMenu.addActionListener(this);
        saveAsMenu.addActionListener(this);
        saveAllMenu.addActionListener(this);
        exitMenu.addActionListener(this);

        importDbMenu.addActionListener(this);
        generateMenu.addActionListener(this);

	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	setSize(600, 550);
    }

	public static Editor getFrame() {
		if (null == frame) {
			frame = new Editor();
		}
		return frame;
	}

    private void init() {
        getContentPane().setLayout(new BorderLayout());

        // Setup menu bar
        setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(toolMenu);

        fileMenu.add(createProjectMenu);
        fileMenu.add(createDomainMenu);
        fileMenu.add(createDataMapMenu);
        fileMenu.add(createDataSourceMenu);
        fileMenu.add(createObjEntityMenu);
        fileMenu.add(createDbEntityMenu);
        fileMenu.addSeparator();
        fileMenu.add(openProjectMenu);
        fileMenu.add(openDataMapMenu);
        fileMenu.addSeparator();
        fileMenu.add(closeProjectMenu);
        fileMenu.add(saveMenu);
        fileMenu.add(saveAsMenu);
        fileMenu.add(saveAllMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitMenu);

        toolMenu.add(importDbMenu);
        toolMenu.add(generateMenu);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == createProjectMenu) {
            createProject();
        } else if (src == openProjectMenu) {
            openProject();
        } else if (src == closeProjectMenu) {
            getContentPane().remove(view);
            view = null;
            mediator = null;
            disableMenu();
        } else if (src == saveAsMenu) {
            saveAs();
        } else if (src == createProjectMenu) {
            createProject();
        } else if (src == createDomainMenu) {
            createDomain();
        } else if (src == createDataMapMenu) {
        	mediator.addDataMap(this, new DataMap(DataMapWrapper.sessionUniqueDomainName()));
        } else if (src == createObjEntityMenu) {
        	createObjEntity();
        } else if (src == createDbEntityMenu) {
        	createDbEntity();
        } else if (src == saveMenu) {
            save();
        } else if (src == importDbMenu) {
            importDb();
        } else if (src == generateMenu) {
            generateClasses();
        }
    }

	private void generateClasses() {
		GenerateClassDialog dialog;
		dialog = new GenerateClassDialog(this, mediator);
		dialog.show();
		dialog.dispose();
	}



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
			}catch (SQLException e) {
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
			}
			catch (Exception e) {
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
		ChooseSchemaDialog dialog = new ChooseSchemaDialog(schemas);
		dialog.show();
		if (dialog.getChoice() == ChooseSchemaDialog.CANCEL)
			return;
		String schema_name = dialog.getSchemaName();
		if (schema_name != null && schema_name.length() == 0)
			schema_name = null;
		DataMap map;
		try {
			map = loader.createDataMapFromDB(schema_name);
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error reverse engineering database"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		System.out.println("Num of db entities in the map: " + map.getDbEntities().length);
	    mediator.fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.ADD));
	    mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(this, map
	    												, mediator.getCurrentDataDomain()));
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
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            fileChooser.setFileFilter(new ProjFileFilter());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Choose new project directory");
            int ret_code = fileChooser.showOpenDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fileChooser.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            if (!file.exists())
            	file.createNewFile();
            File proj_file = new File(file.getAbsolutePath() 
            							+ File.separator 
            							+ "cayenne.xml");
            if (!proj_file.exists())
            	proj_file.createNewFile();
			
			System.out.println("proj file path is " 
					+ proj_file.getAbsolutePath());
			
			FileWriter fw = new FileWriter(proj_file);
			PrintWriter pw = new PrintWriter(fw, true);
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<domains>");
			pw.println("</domains>");
			pw.flush();
			pw.close();
			fw.close();
            GuiConfiguration.setProjFile(proj_file);
			Configuration.initSharedConfig("org.objectstyle.cayenne.gui.GuiConfiguration");
            GuiConfiguration config;
            config = (GuiConfiguration)Configuration.getSharedConfig();
            MediatorImpl mediator = new MediatorImpl(config);
            project(mediator);
        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Open specified cayenne.xml file. */
    private void openProject() {
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            fileChooser.setFileFilter(new ProjFileFilter());
            fileChooser.setDialogTitle("Choose project file (cayenne.xml)");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int ret_code = fileChooser.showOpenDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fileChooser.getSelectedFile();
	
			System.out.println("File path is " + file.getAbsolutePath());
            GuiConfiguration.setProjFile(file);
			Configuration.initSharedConfig("org.objectstyle.cayenne.gui.GuiConfiguration");
            GuiConfiguration config;
            config = (GuiConfiguration)Configuration.getSharedConfig();
            MediatorImpl mediator = new MediatorImpl(config);
            project(mediator);

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
        createDomainMenu.setEnabled(true);
        closeProjectMenu.setEnabled(true);
		saveAsMenu.setEnabled(true);		
        this.validate();
    }

	private void saveAs() {
		// If there is current data map, save it
		if (mediator.getCurrentDataMap() != null)
			saveAsMap(mediator.getCurrentDataMap());
	}

	/** Save data map to a different location. 
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	private void saveAsMap(DataMap map) {
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
            fc.setDialogTitle("Save data map - " + map.getName());
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showOpenDialog(this);
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
	
	private void save() {
		if (mediator.getCurrentDataMap() != null) {
			saveDataMap(mediator.getCurrentDataMap());
		} 
		else 
			saveProject();
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
				saveAsMap(map);
				return;
			}
			MapLoader saver = new MapLoaderImpl();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			saver.storeDataMap(pw, map);
			pw.close();
			fw.close();
		} catch (Exception e) {
		}
	}
	
	private void createDomain() {
		DataDomain domain = new DataDomain(DataDomainWrapper.sessionUniqueDomainName());
		mediator.getConfig().addDomain(domain);
		mediator.fireDomainEvent(new DomainEvent(this, domain, DomainEvent.ADD));
		mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, domain));
	}
		
	
	public void currentDomainChanged(DomainDisplayEvent e){
		enableDomainMenu();
	}

	public void currentDataNodeChanged(DataNodeDisplayEvent e){
		enableDomainMenu();
	}

	public void currentDataMapChanged(DataMapDisplayEvent e){
		enableDataMapMenu();
	}

    /** Disables all menu  for the case when no project is open.
      * The only menu-s never disabled are "New Project", "Open Project" 
      * and "Exit". */
    private void disableMenu() {
        createDomainMenu.setEnabled(false);
        createDataMapMenu.setEnabled(false);
        createDataSourceMenu.setEnabled(false);
        createObjEntityMenu.setEnabled(false);
        createDbEntityMenu.setEnabled(false);
        openDataMapMenu.setEnabled(false);
        closeProjectMenu.setEnabled(false);
        saveMenu.setEnabled(false);
        saveAsMenu.setEnabled(false);
        saveAllMenu.setEnabled(false);

        importDbMenu.setEnabled(false);
        generateMenu.setEnabled(false);
    }

	private void enableDomainMenu() {
		disableMenu();
		createDataMapMenu.setEnabled(true);
        createDomainMenu.setEnabled(true);
		createDataSourceMenu.setEnabled(true);
		closeProjectMenu.setEnabled(true);
        openDataMapMenu.setEnabled(true);
        if (mediator.isDirty())
	        saveMenu.setEnabled(true);
        importDbMenu.setEnabled(true);
	}
	
	private void enableDataMapMenu() {
		saveAsMenu.setEnabled(true);		
		enableDomainMenu();
        createObjEntityMenu.setEnabled(true);
        createDbEntityMenu.setEnabled(true);
        generateMenu.setEnabled(true);
	}
	
    public static void main(String[] args) 
    {
    	JFrame frame = getFrame();
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



class EditorView extends JPanel 
implements ObjEntityDisplayListener, DbEntityDisplayListener
, DomainDisplayListener {
    Mediator mediator;

    private static final int INIT_DIVIDER_LOCATION = 170;

    private static final String EMPTY_VIEW    = "Empty";
    private static final String DOMAIN_VIEW   = "Domain";
    private static final String NODE_VIEW   = "Node";
    private static final String DATA_MAP_VIEW = "DataMap";
    private static final String OBJ_VIEW    = "ObjView";
    private static final String DB_VIEW    = "DbView";


    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    BrowseView treePanel;
    JPanel detailPanel = new JPanel();
    JPanel emptyPanel = new JPanel();
    // DomainDetailView domainView;
    JPanel nodeView = new JPanel();
    JPanel dataMapView = new JPanel();
    ObjDetailView objDetailView;
    DbDetailView dbDetailView;
    CardLayout detailLayout;

    public EditorView(Mediator temp_mediator) {
        super(new BorderLayout());
        mediator = temp_mediator;

        add(splitPane, BorderLayout.CENTER);
        treePanel = new BrowseView(temp_mediator);
        splitPane.setLeftComponent(treePanel);
        splitPane.setRightComponent(detailPanel);
  		splitPane.setDividerLocation(170);

        detailLayout = new CardLayout();
        detailPanel.setLayout(detailLayout);

		JPanel temp = new JPanel();
		detailPanel.add(temp, EMPTY_VIEW);
		//domainView = new DomainDetailView(temp_mediator);
		//detailPanel.add(domainView, DOMAIN_VIEW);
		
        objDetailView = new ObjDetailView(temp_mediator);
        detailPanel.add(objDetailView, OBJ_VIEW);
        dbDetailView = new DbDetailView(temp_mediator);
        detailPanel.add(dbDetailView, DB_VIEW);
        
        mediator.addDomainDisplayListener(this);
        mediator.addObjEntityDisplayListener(this);
        mediator.addDbEntityDisplayListener(this);
    }

   	public void currentDomainChanged(DomainDisplayEvent e)
   	{
   		detailLayout.show(detailPanel, DOMAIN_VIEW);
   	}

    
   	public void currentObjEntityChanged(EntityDisplayEvent e)
   	{
   		detailLayout.show(detailPanel, OBJ_VIEW);
   	}

   	public void currentDbEntityChanged(EntityDisplayEvent e)
   	{
   		detailLayout.show(detailPanel, DB_VIEW);
   	}
}

/** Limits access to the specified root directory and its sub-directories.
  * If root directory is null, uses system file system view. */
class FileSystemViewDecorator extends FileSystemView {
	FileSystemView system;
	File rootDir;
	
	public FileSystemViewDecorator(File root_dir) {
		system = FileSystemView.getFileSystemView();
		rootDir = root_dir;
	}
	
	public boolean isRoot(File f) {
		if (rootDir != null)
			return f.equals(rootDir);
		else 
			return system.isRoot(f);
	}
	
	public Boolean isTraversable(File f) 
	{ return system.isTraversable(f); }	
	public String getSystemDisplayName(File f) 
	{ return system.getSystemDisplayName(f); }
	public String getSystemTypeDescription(File f) 
	{ return system.getSystemTypeDescription(f); }
	public Icon getSystemIcon(File f) 
	{ return system.getSystemIcon(f); }
	public boolean isParent(File folder, File file) 
	{ return system.isParent(folder, file);}
	public File getChild(File parent, String fileName) 
	{ return system.getChild(parent, fileName); }
	public boolean isFileSystem(File f) 
	{ return system.isFileSystem(f); }
	public File createNewFolder(File containingDir) throws IOException
	{ return system.createNewFolder(containingDir); }
	
	public boolean isHiddenFile(File f) 
	{ return system.isHiddenFile(f);}
	public boolean isFileSystemRoot(File dir) 
	{ 		
		if (rootDir != null)
			return dir.equals(rootDir);
		else return system.isFileSystemRoot(dir);
	}
	public boolean isDrive(File dir) 
	{ return system.isDrive(dir); }
	
	public boolean isFloppyDrive(File dir) 
	{ return system.isFloppyDrive(dir); }
	public boolean isComputerNode(File dir) 
	{ return system.isComputerNode(dir); }
	public File[] getRoots()
	{ 
		if (rootDir != null)
			return new File[]{rootDir};
		else 
			return system.getRoots();
	}
	public File getHomeDirectory()
	{
		if (rootDir != null)
			return rootDir;
		else 
			return system.getHomeDirectory();
	}
	public File getDefaultDirectory()
	{
		if (rootDir != null)
			return rootDir;
		else 
			return system.getDefaultDirectory();
	}
	
	public File createFileObject(File dir, String filename)	
	{ return system.createFileObject(dir, filename); }
	
	public File createFileObject(String path)
	{ return system.createFileObject(path); }
	
	public File[] getFiles(File dir,boolean useFileHiding) 
	{ return system.getFiles(dir, useFileHiding); }
	
	public File getParentDirectory(File dir)
	{ return system.getParentDirectory(dir); }
	
	protected File createFileSystemRoot(File f)
	{ return super.createFileSystemRoot(f); }
}