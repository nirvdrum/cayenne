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
package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * @author Andrei Adamchik
 */
public class PackageMenuAction extends CayenneAction {
    public static final String ACTION_NAME =
        "Set Package Name for Obj Entities";

    /**
     * Constructor for PackageMenuAction.
     */
    public PackageMenuAction() {
        super(ACTION_NAME);
    }

    /**
     * @see org.objectstyle.cayenne.modeler.action.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        setPackageName();
    }

    private void setPackageName() {
        DataMap map = getMediator().getCurrentDataMap();
        if (map == null) {
            return;
        }
        String package_name;
        package_name =
            JOptionPane.showInputDialog(Editor.getFrame(), "Enter the new package name");
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
        Iterator entities = map.getObjEntitiesAsList().iterator();
        while (entities.hasNext()) {
        	ObjEntity entity = (ObjEntity)entities.next();
            String name = entity.getClassName();

            // there may be entities with no class selected yet
            if (name == null) {
                continue;
            }

            int idx = name.lastIndexOf('.');
            if (idx > 0) {
                name =
                    (idx == name.length() - 1)
                        ? entity.getName()
                        : name.substring(idx + 1);
            }
			entity.setClassName(package_name + name);
        }
        getMediator().fireDataMapEvent(new DataMapEvent(this, map));
    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataMap.class) != null;
    }
}
