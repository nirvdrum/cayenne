package org.objectstyle.cayenne.gui;

import javax.swing.Icon;
import java.io.*;
import javax.swing.filechooser.*;


/** Limits access to the specified root directory and its sub-directories.
  * If root directory is null, uses system file system view. */
public class FileSystemViewDecorator extends FileSystemView {
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