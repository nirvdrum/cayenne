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

package org.objectstyle.cayenne.gui.action;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.AbstractAction;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.Preferences;
import org.objectstyle.cayenne.conf.*;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.GuiDataSource;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.gui.validator.*;


/** 
 * Parent class for all Editor actions related to saving project.
 * 
 * @author Misha Shengaout
 */
public class SaveAction extends CayenneAction {
	static Logger logObj = Logger.getLogger(SaveAction.class.getName());

	public static final String ACTION_NAME = "Save";
		
	public SaveAction() {
		super(ACTION_NAME);
	}
	
	public String getIconName() {
		return "images/icon-save.gif";
	}
	
	protected void saveAll() {
		Mediator mediator = getMediator();
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
				// If using direct connection, save into separate file
				if (node.getDataSourceFactory().equals(DataSourceFactory.DIRECT_FACTORY)) {
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
			dialog = new ValidatorDialog(Editor.getFrame(), mediator
								, val.getErrorMessages(), ret_code);
			dialog.setVisible(true);
		}
	}


	protected void saveProject() {
		Mediator mediator = getMediator();
		File file = mediator.getConfig().getProjFile();
		System.out.println("Saving project to " + file.getAbsolutePath());
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
            Editor.getFrame().addToLastProjList(file.getAbsolutePath());
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



	/** Save data source info if data source factory is DIRECT_FACTORY. */
	protected void saveDataNode(DataNode node) {
		Mediator mediator = getMediator();
		
		try {
            String proj_dir_str = mediator.getConfig().getProjDir();
			File file = new File(proj_dir_str + File.separator + node.getDataSourceLocation());
			if (!file.exists()) {
				saveNodeAs(node);
				return;
			}
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			GuiDataSource src = (GuiDataSource)node.getDataSource();
			DomainHelper.storeDataNode(pw, src.getDataSourceInfo());
			pw.close();
			fw.close();
		} catch (Exception e) {
            e.printStackTrace();
		}
	}


	/** Save data node (DataSourceInfo) to a different location.
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	protected void saveNodeAs(DataNode node) {
		Mediator mediator = getMediator();
		GuiDataSource src = (GuiDataSource)node.getDataSource();
    	XmlFilter xmlFilter = new XmlFilter();
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
            int ret_code = fc.showSaveDialog(Editor.getFrame());
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




	/** Save data map to the file. */
	protected void saveDataMap(DataMap map) {
		try {
            File file = null;
            String proj_dir_str = getMediator().getConfig().getProjDir();
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
		} catch (Exception e) {}
	}

	/** Save data map to a different location.
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	protected void saveMapAs(DataMap map) {
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            String proj_dir_str = getMediator().getConfig().getProjDir();
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
            int ret_code = fc.showSaveDialog(Editor.getFrame());
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
			getMediator().fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.CHANGE));

        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	public void performAction(ActionEvent e) {
		saveAll();
	}
}