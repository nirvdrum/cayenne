package org.objectstyle.cayenne.gui.action;
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

import java.awt.event.ActionEvent;
import java.util.List;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.AbstractAction;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.util.Preferences;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.AddDataMapDialog;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;


/** Parent class for all Editor actions related to DataMap.
 */
public class CreateDataMapAction extends AbstractAction
{
	Mediator mediator;

	public CreateDataMapAction(Mediator temp_mediator) {
		mediator = temp_mediator;
	}
	
	/** Add data map to the current data node. */	
	protected void addDataMap() {
		DataNode node = mediator.getCurrentDataNode();
		List map_list = mediator.getCurrentDataDomain().getMapList();
		AddDataMapDialog dialog = new AddDataMapDialog(node, map_list);
		System.out.println("Node has " + node.getDataMaps().length + " maps");
		mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
	}
	
	/** Calls addDataMap() or creates new data map if no data node selected.*/
	protected void createDataMap() {
		// If have current data node, don't create new data map, add to it 
		// the existing one.
		if (mediator.getCurrentDataNode() != null) {
			addDataMap();
			return;
		}
		String relative_location = getMapLocation(mediator);
		if (null == relative_location)
			return;
		DataMap map = new DataMap(NameGenerator.getDataMapName());
		map.setLocation(relative_location);
		mediator.addDataMap(this, map);
	}
	
	/** Returns location relative to Project or null if nothing selected. */
	static String getMapLocation(Mediator mediator) {
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
            int ret_code = fc.showSaveDialog(Editor.getFrame());
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return relative_location;
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
            System.out.println("Error creating data map file, " + e.getMessage());
            e.printStackTrace();
        }
        return relative_location;
	}
	
	public void actionPerformed(ActionEvent e) {
		createDataMap();
	}
}