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
package org.objectstyle.cayenne.gui.util;

import java.io.File;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

/** 
 * Limits access to the specified root directory and its sub-directories.
 * If root directory is null, uses system file system view. 
 */
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

	public Boolean isTraversable(File f) {
		return system.isTraversable(f);
	}
	
	public String getSystemDisplayName(File f) {
		return system.getSystemDisplayName(f);
	}
	
	public String getSystemTypeDescription(File f) {
		return system.getSystemTypeDescription(f);
	}
	
	public Icon getSystemIcon(File f) {
		return system.getSystemIcon(f);
	}
	
	public boolean isParent(File folder, File file) {
		return system.isParent(folder, file);
	}
	
	public File getChild(File parent, String fileName) {
		return system.getChild(parent, fileName);
	}
	
	public boolean isFileSystem(File f) {
		return system.isFileSystem(f);
	}
	
	public File createNewFolder(File containingDir) throws IOException {
		return system.createNewFolder(containingDir);
	}

	public boolean isHiddenFile(File f) {
		return system.isHiddenFile(f);
	}
	public boolean isFileSystemRoot(File dir) {
		if (rootDir != null)
			return dir.equals(rootDir);
		else
			return system.isFileSystemRoot(dir);
	}
	public boolean isDrive(File dir) {
		return system.isDrive(dir);
	}

	public boolean isFloppyDrive(File dir) {
		return system.isFloppyDrive(dir);
	}
	public boolean isComputerNode(File dir) {
		return system.isComputerNode(dir);
	}
	public File[] getRoots() {
		if (rootDir != null)
			return new File[] { rootDir };
		else
			return system.getRoots();
	}
	public File getHomeDirectory() {
		if (rootDir != null)
			return rootDir;
		else
			return system.getHomeDirectory();
	}
	public File getDefaultDirectory() {
		if (rootDir != null)
			return rootDir;
		else
			return system.getDefaultDirectory();
	}

	public File createFileObject(File dir, String filename) {
		return system.createFileObject(dir, filename);
	}

	public File createFileObject(String path) {
		return system.createFileObject(path);
	}

	public File[] getFiles(File dir, boolean useFileHiding) {
		return system.getFiles(dir, useFileHiding);
	}

	public File getParentDirectory(File dir) {
		return system.getParentDirectory(dir);
	}

	protected File createFileSystemRoot(File f) {
		return super.createFileSystemRoot(f);
	}
}