/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.objectstyle.cayenne.modeler.ModelerClassLoader;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;
import org.scopemvc.model.collection.ListModel;

/**
 * A Scope model for classpath configuration.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ConfigureClasspathModel extends BasicModel {
    public static final Selector CUSTOM_CLASSPATH_SELECTOR =
        Selector.fromString("customClasspath");
    public static final Selector SELECTED_ENTRY_SELECTOR =
        Selector.fromString("selectedEntry");

    protected ListModel customClasspath;
    protected File selectedEntry;
    protected ModelerClassLoader classLoader;

    public ConfigureClasspathModel(ModelerClassLoader classLoader) {
        this.classLoader = classLoader;
        this.customClasspath = new ListModel();
        this.customClasspath.addAll(classLoader.getPathFiles());
        this.customClasspath.addModelChangeListener(this);
    }

    public List getCustomClasspath() {
        return customClasspath;
    }

    public File getSelectedEntry() {
        return selectedEntry;
    }

    public void setSelectedEntry(File selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    public void save() throws MalformedURLException, IOException {
        classLoader.setPathFiles(customClasspath);
        classLoader.store();
    }

    /**
     * Adds a new file classpath entry above the currently selected
     * one. If no current selection exists, adds it at the end of the 
     * list.
     */
    public void addEntry(File newEntry) {
        newEntry = newEntry.getAbsoluteFile();

        synchronized (customClasspath) {
            // don't add duplicates
            if (customClasspath.contains(newEntry)) {
                return;
            }

            if (selectedEntry == null) {
                customClasspath.add(newEntry);
            }
            else {
                int index = customClasspath.indexOf(selectedEntry);
                if (index >= 0) {
                    customClasspath.add(index, newEntry);
                }
                else {
                    customClasspath.add(newEntry);
                }
            }
        }
    }

    public boolean removeSelectedEntry() {
        if (selectedEntry == null) {
            return false;
        }

        synchronized (customClasspath) {
            return customClasspath.remove(selectedEntry);
        }
    }
}
