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
package org.objectstyle.cayenne.modeler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.project.CayenneUserDir;

/**
 * A facade to CayenneModeler specialized ClassLoader that allows to load jar files dynamically 
 * from non-classpath locations. Singleton instance of ModelerClassLoader can be obtained by 
 * calling static method {@link #getClassLoader()}. Default list of locations is loaded 
 * from ~/.cayenne/modeler.classpath. When ModelerClassLoader's URL list is modified, 
 * it is being saved back to this file. 
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ModelerClassLoader {
    private static Logger logObj = Logger.getLogger(ModelerClassLoader.class);

    public static final String CLASSPATH_FILE = "modeler.classpath";

    // create class loader with default classpath file location
    private static final ModelerClassLoader defaultLoader =
        new ModelerClassLoader(CayenneUserDir.getInstance().resolveFile(CLASSPATH_FILE));

    protected File classpathFile;
    protected long loadedAt;

    private FileClassLoader classLoader;
    protected List pathFiles;

    /**
     * Returns a singleton ModelerClassLoader.
     */
    public static ModelerClassLoader getClassLoader() {
        return defaultLoader;
    }

    protected ModelerClassLoader(File classpathFile) {
        this.classpathFile = classpathFile;
        this.pathFiles = new ArrayList(15);
        load();
    }

    /**
     * Returns class for a given name.
     */
    public Class loadClass(String className) throws ClassNotFoundException {
        return nonNullClassLoader().loadClass(className);
    }

    public synchronized void addFile(File file) throws MalformedURLException {
        file = file.getAbsoluteFile();

        if (pathFiles.contains(file)) {
            return;
        }

        if (classLoader != null) {
            classLoader.addURL(file.toURL());
        }

        pathFiles.add(file);
        logObj.debug("Added CLASSPATH entry...: " + file.getAbsolutePath());
        store();
    }

    public synchronized void removeFile(File file) throws MalformedURLException {
        if (pathFiles.remove(file)) {
            // must reinit class loader
            classLoader = null;
            store();
        }
    }

    private synchronized FileClassLoader nonNullClassLoader() {
        if (classLoader == null) {
            classLoader = new FileClassLoader(getClass().getClassLoader(), pathFiles);
        }

        return classLoader;
    }

    protected synchronized void load() {
        if (classpathFile == null || loadedAt > classpathFile.lastModified()) {
            return;
        }

        // reset
        pathFiles.clear();
        classLoader = null;

        // read one line at a time and create URLs
        try {
            BufferedReader in = new BufferedReader(new FileReader(classpathFile));

            try {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();

                    // skip comments
                    if (line.startsWith("#") || line.startsWith("//")) {
                        continue;
                    }

                    File file = new File(line);
                    if (file.canRead()) {
                        addFile(file);
                    }
                    else {
                        logObj.info("Invalid CLASSPATH entry, ignoring...: " + line);
                    }
                }
            }
            finally {
                in.close();
            }
        }
        catch (IOException ioex) {
            logObj.warn("Error reading classpath file: " + classpathFile, ioex);
        }
    }

    protected synchronized void store() {

    }

    static class FileClassLoader extends URLClassLoader {
        FileClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        FileClassLoader(ClassLoader parent, List files) {
            this(parent);

            Iterator it = files.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();

                // I guess here we have to quetly ignore invalid URLs...
                try {
                    addURL(file.toURL());
                }
                catch (MalformedURLException ex) {
                }
            }
        }

        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
