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

package org.objectstyle.cayenne.modeler.action;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.dialog.ErrorDebugDialog;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.wocompat.EOModelProcessor;

/**
 * Action handler for WebObjects EOModel import function.
 * 
 * @author Andrei Adamchik
 */
public class ImportEOModelAction extends CayenneAction {

    private static Logger logObj = Logger.getLogger(ImportEOModelAction.class);

    public static String getActionName() {
        return "Import EOModel";
    }

    protected JFileChooser eoModelChooser;

    public ImportEOModelAction() {
        super(getActionName());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void performAction(ActionEvent event) {
        importEOModel();
    }

    /**
     * Lets user select an EOModel, then imports it as a DataMap.
     */
    protected void importEOModel() {
        JFileChooser fileChooser = getEOModelChooser();
        int status = fileChooser.showOpenDialog(Application.getFrame());

        if (status == JFileChooser.APPROVE_OPTION) {
            // save preferences
            File file = fileChooser.getSelectedFile();
            if (file.isFile()) {
                file = file.getParentFile();
            }

            ModelerPreferences.getPreferences().setProperty(
                    ModelerPreferences.LAST_EOM_DIR,
                    file.getParent());

            try {
                String path = file.getCanonicalPath();
                DataMap map = new EOModelProcessor().loadEOModel(path);
                addDataMap(map);
            }
            catch (Exception ex) {
                logObj.log(Level.INFO, "EOModel Loading Exception", ex);
                ErrorDebugDialog.guiException(ex);
            }

        }
    }

    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataDomain.class) != null;
    }

    /**
     * Adds DataMap into the project.
     */
    protected void addDataMap(DataMap map) {
        DataMap currentMap = getMediator().getCurrentDataMap();
        EventController mediator = getMediator();

        if (currentMap != null) {
            // merge with existing map... have to memorize map state before and after
            // to do the right events

            Collection originalOE = new ArrayList(currentMap.getObjEntities());
            Collection originalDE = new ArrayList(currentMap.getDbEntities());

            currentMap.mergeWithDataMap(map);
            map = currentMap;

            // ostprocess changes
            Collection newOE = new ArrayList(currentMap.getObjEntities());
            Collection newDE = new ArrayList(currentMap.getDbEntities());

            EntityEvent entityEvent = new EntityEvent(
                    Application.getFrame(),
                    null);

            Collection addedOE = CollectionUtils.subtract(newOE, originalOE);
            Iterator it = addedOE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.ADD);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection removedOE = CollectionUtils.subtract(originalOE, newOE);
            it = removedOE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.REMOVE);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection addedDE = CollectionUtils.subtract(newDE, originalDE);
            it = addedDE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.ADD);
                mediator.fireDbEntityEvent(entityEvent);
            }

            Collection removedDE = CollectionUtils.subtract(originalDE, newDE);
            it = removedDE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.REMOVE);
                mediator.fireDbEntityEvent(entityEvent);
            }

            mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(Application
                    .getFrame(), map, mediator.getCurrentDataDomain(), mediator
                    .getCurrentDataNode()));
        }
        else {
            mediator.addDataMap(Application.getFrame(), map);
        }
    }

    /**
     * Returns EOModel chooser.
     */
    public JFileChooser getEOModelChooser() {

        if (eoModelChooser == null) {
            eoModelChooser = new EOModelChooser("Select EOModel");
        }

        String startDir = ModelerPreferences.getPreferences().getString(
                ModelerPreferences.LAST_EOM_DIR);

        if (startDir == null) {
            startDir = ModelerPreferences.getPreferences().getString(
                    ModelerPreferences.LAST_DIR);
        }

        if (startDir != null) {
            File startDirFile = new File(startDir);
            if (startDirFile.exists()) {
                eoModelChooser.setCurrentDirectory(startDirFile);
            }
        }

        return eoModelChooser;
    }

    /**
     * Custom file chooser that will pop up again if a bad directory is selected.
     */
    class EOModelChooser extends JFileChooser {

        protected FileFilter selectFilter;
        protected JDialog cachedDialog;

        public EOModelChooser(String title) {
            super.setFileFilter(FileFilters.getEOModelFilter());
            super.setDialogTitle(title);
            super.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            this.selectFilter = FileFilters.getEOModelSelectFilter();
        }

        public int showOpenDialog(Component parent) {
            int status = super.showOpenDialog(parent);
            if (status != JFileChooser.APPROVE_OPTION) {
                cachedDialog = null;
                return status;
            }

            // make sure invalid directory is not selected
            File file = this.getSelectedFile();
            if (selectFilter.accept(file)) {
                cachedDialog = null;
                return JFileChooser.APPROVE_OPTION;
            }
            else {
                if (file.isDirectory()) {
                    this.setCurrentDirectory(file);
                }

                return this.showOpenDialog(parent);
            }
        }

        protected JDialog createDialog(Component parent) throws HeadlessException {

            if (cachedDialog == null) {
                cachedDialog = super.createDialog(parent);
            }
            return cachedDialog;
        }
    }
}