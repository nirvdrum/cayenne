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
import java.io.File;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.swing.JFileChooser;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.gui.ModelerPreferences;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.util.FileSystemViewDecorator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.util.NamedObjectFactory;

/**
 * Action that creates new DataMap in the project.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class CreateDataMapAction extends CayenneAction {
    static Logger logObj = Logger.getLogger(CreateDataMapAction.class.getName());

    public static final String ACTION_NAME = "Create DataMap";

    public CreateDataMapAction() {
        super(ACTION_NAME);
    }

    public String getIconName() {
        return "icon-datamap.gif";
    }

    /** Calls addDataMap() or creates new data map if no data node selected.*/
    protected void createDataMap() {
        Mediator mediator = getMediator();
        String relative_location = getMapLocation(mediator);
        if (null == relative_location) {
            return;
        }

        DataDomain currentDomain = mediator.getCurrentDataDomain();
        DataMap map =
            (DataMap) NamedObjectFactory.createObject(DataMap.class, currentDomain);
        map.setLocation(relative_location);
        mediator.addDataMap(this, map);
    }

    /** Returns location relative to Project or null if nothing selected. */
    static String getMapLocation(Mediator mediator) {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        String init_dir = (String) pref.getProperty(ModelerPreferences.LAST_DIR);
        // Data map file
        File file = null;

        try {

            File proj_dir = Editor.getProject().getProjectDir();
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
            if (ret_code != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            file = fc.getSelectedFile();
            if (!file.exists()) {
                file.createNewFile();
            }

            return Editor.getProject().resolveSymbolicName(file);
        } catch (Exception e) {
            logObj.warn("Error creating data map file.", e);
        }
        return null;
    }

    public void performAction(ActionEvent e) {
        createDataMap();
    }
}