/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler.util;

import java.awt.Component;
import java.util.Vector;

import javax.swing.JMenu;

import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;

/**
 * Menu that contains a list of previously used files.
 * It is built from CayenneModeler preferences by calling 
 * <code>rebuildFromPreferences</code>.
 * 
 * @author Andrei Adamchik
 */
public class RecentFileMenu extends JMenu {

	/**
	 * Constructor for RecentFileMenu.
	 */
	public RecentFileMenu(String s) {
		super(s);
	}

	/**
	 * @see javax.swing.JMenu#add(JMenuItem)
	 */
	public RecentFileMenuItem add(RecentFileMenuItem menuItem) {
		return (RecentFileMenuItem) super.add(menuItem);
	}

	/** 
	 * Rebuilds internal menu items list with the files stored in
	 * CayenneModeler properences.
	 */
	public void rebuildFromPreferences() {
		ModelerPreferences pref = ModelerPreferences.getPreferences();		
		Vector arr = pref.getVector(ModelerPreferences.LAST_PROJ_FILES);
		while (arr.size() > 4) {
			arr.remove(arr.size() - 1);
		}

		// readd menus
		Component[] comps = getMenuComponents();
		int curSize = comps.length;
        int prefSize = arr.size(); 
        
		for (int i = 0; i < prefSize; i++) {
			String name = (String) arr.get(i);

			if (i < curSize) {
				// update existing one
				RecentFileMenuItem item = (RecentFileMenuItem) comps[i];
				item.setText(name);
			} else {
				// add a new one
				RecentFileMenuItem item = new RecentFileMenuItem(name);
				item.setAction(
                CayenneModelerFrame.getFrame().getAction(OpenProjectAction.getActionName()));
				add(item);
			}
		}

		// remove any hanging items
		for (int i = curSize - 1; i >= prefSize; i--) {
            remove(i);
		}
	}
}
