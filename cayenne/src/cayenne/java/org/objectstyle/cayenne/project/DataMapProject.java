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
package org.objectstyle.cayenne.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DataMapException;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.util.NamedObjectFactory;
import org.xml.sax.InputSource;

/**
 * Cayenne project that consists of a single DataMap.
 * 
 * @author Andrei Adamchik
 */
public class DataMapProject extends Project {
    protected DataMap map;

    /**
     * Constructor for MapProject.
     * 
     * @param projectFile
     */
    public DataMapProject(File projectFile) {
        super(projectFile);
    }

    /**
     * Does nothing.
     */
    public void checkForUpgrades() {
        // do nothing
    }

    /**
     * @see org.objectstyle.cayenne.project.Project#getRootNode()
     */
    public Object getRootNode() {
        return map;
    }

    /**
    * Initializes internal <code>map</code> object and then calls super.
    */
    protected void postInit(File projectFile) {
        if (projectFile != null) {
            try {
                InputStream in = new FileInputStream(projectFile.getCanonicalFile());
                map = new MapLoader().loadDataMap(new InputSource(in));

                String fileName = resolveSymbolicName(projectFile);
                String mapName =
                    (fileName != null && fileName.endsWith(DataMapFile.LOCATION_SUFFIX))
                        ? fileName.substring(0, fileName.length() - DataMapFile.LOCATION_SUFFIX.length())
                        : "UntitledMap";

                map.setName(mapName);
            } catch (IOException e) {
                throw new ProjectException("Error creating ApplicationProject.", e);
            } catch (DataMapException dme) {
                throw new ProjectException("Error creating ApplicationProject.", dme);
            }
        } else {
            map = (DataMap) NamedObjectFactory.createObject(DataMap.class, null);
        }

        super.postInit(projectFile);
    }

}
