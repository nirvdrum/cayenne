package org.objectstyle.cayenne.tools;
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

import java.io.*;

import org.xml.sax.InputSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.objectstyle.cayenne.map.*;

/** Ant task to perform class generation from data map.
 *
 *  @author Andrei Adamchik
 */
public class CayenneGenerator extends Task {
    protected File map;
    protected File destDir;
    protected File superDestDir;
    protected boolean overwrite;
    protected boolean usepkgpath = true;
    protected boolean makepairs = true;
    protected File template;
    protected File supertemplate;
    protected String superpkg;

    /** Classes are generated only for files that are older 
     * than the map. <code>mapTimestamp</code> is used to track that. 
     */
    protected long mapTimestamp;

    /** The method executing the task. It will be called
     *  by ant framework. */
    public void execute() throws BuildException {
        validateAttributes();

        try {
            mapTimestamp = map.lastModified();
            DataMap dataMap = loadDataMap();
            AntClassGenerator gen = new AntClassGenerator(dataMap);

            if (makepairs) {
                String t = getTemplateForPairs();
                String st = getSupertemplateForPairs();

                // overwrite superclass package
                if (superpkg != null) {
                    gen.setSuperPkg(superpkg);
                }

                gen.generateClassPairs(t, st, MapClassGenerator.SUPERCLASS_PREFIX);
            }
            else {
                String t = getTemplateForSingles();
                gen.generateSingleClasses(t);
            }
        }
        catch (Exception ex) {
            super.log("Error generating classes.");
            throw new BuildException("Error generating classes.", ex);
        }
    }

    /** 
     *  Returns template file path for Java class 
     *  when generating single classes. 
     */
    protected String getTemplateForSingles() throws IOException {
        return (template != null)
            ? template.getCanonicalPath()
            : MapClassGenerator.SINGLE_CLASS_TEMPLATE;
    }

    /** 
     *  Returns template file path for Java subclass 
     *  when generating class pairs. 
     */
    protected String getTemplateForPairs() throws IOException {
        return (template != null)
            ? template.getCanonicalPath()
            : MapClassGenerator.SUBCLASS_TEMPLATE;
    }

    /** 
     *  Returns template file path for Java superclass 
     *  when generating class pairs. 
     */
    protected String getSupertemplateForPairs() throws IOException {
        return (supertemplate != null)
            ? supertemplate.getCanonicalPath()
            : MapClassGenerator.SUPERCLASS_TEMPLATE;
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoaderImpl().loadDataMap(in);
    }

    /** Validates atttribute combinatins. Throws BuildException if
     *  attributes are invalid. 
     */
    protected void validateAttributes() throws BuildException {
        if (map == null) {
            throw new BuildException("'map' attribute is missing.");
        }

        if (destDir == null) {
            throw new BuildException("'destDir' attribute is missing.");
        }

        if (!map.canRead()) {
            throw new BuildException("Can't read the map from " + map);
        }

        if (!destDir.isDirectory()) {
            throw new BuildException("'destDir' is not a directory.");
        }

        if (!destDir.canWrite()) {
            throw new BuildException("Do not have write permissions for " + destDir);
        }

        if (template != null && !template.canRead()) {
            throw new BuildException("Can't read template from " + template);
        }

        if (makepairs && superDestDir != null) {
            if (!superDestDir.isDirectory()) {
                throw new BuildException("'superDestDir' is not a directory.");
            }

            if (!superDestDir.canWrite()) {
                throw new BuildException("Do not have write permissions for " + superDestDir);
            }
        }

        if (makepairs && supertemplate != null && !supertemplate.canRead()) {
            throw new BuildException("Can't read super template from " + supertemplate);
        }
    }

    /**
     * Sets the map.
     * @param map The map to set
     */
    public void setMap(File map) {
        this.map = map;
    }

    /**
     * Sets the destDir.
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Sets the <code>superDestDir</code> property.
     */
    public void setSuperDestDir(File superDestDir) {
        this.superDestDir = superDestDir;
    }

    /**
     * Sets <code>overwrite</code> property.
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Sets <code>makepairs</code> property.
     */
    public void setMakepairs(boolean makepairs) {
        this.makepairs = makepairs;
    }

    /**
     * Sets <code>template</code> property.
     */
    public void setTemplate(File template) {
        this.template = template;
    }

    /**
     * Sets <code>supertemplate</code> property.
     */
    public void setSupertemplate(File supertemplate) {
        this.supertemplate = supertemplate;
    }

    /**
     * Sets <code>usepkgpath</code> property.
     */
    public void setUsepkgpath(boolean usepkgpath) {
        this.usepkgpath = usepkgpath;
    }

    /**
     * Sets <code>superpkg</code> property.
     */
    public void setSuperpkg(String superpkg) {
        this.superpkg = superpkg;
    }

    /** Concrete subclass of MapClassGenerator that performs the actual
      * class generation. */
    class AntClassGenerator extends MapClassGenerator {

        public AntClassGenerator(DataMap map) {
            super(map);
        }

        public void closeWriter(Writer out) throws Exception {
            out.close();
        }

        public Writer openWriter(ObjEntity entity, String pkgName, String className)
            throws Exception {
            return (className.startsWith(SUPERCLASS_PREFIX))
                ? writerForSuperclass(entity, pkgName, className)
                : writerForClass(entity, pkgName, className);
        }

        private Writer writerForSuperclass(
            ObjEntity entity,
            String pkgName,
            String className)
            throws Exception {

            File destFile = (superDestDir != null) ? superDestDir : destDir;
            File dest = new File(destFile.getPath(), className + ".java");

            // ignore newer files
            if (dest.exists()
                && dest.lastModified() > CayenneGenerator.this.mapTimestamp) {
                return null;
            }

            log("Generating class file: " + dest.getCanonicalPath());
            return new FileWriter(dest);
        }

        private Writer writerForClass(
            ObjEntity entity,
            String pkgName,
            String className)
            throws Exception {

            File dest = new File(destDir.getPath(), className + ".java");
            if (dest.exists()) {

                // no overwrite of subclasses
                if (makepairs) {
                    return null;
                }

                // skip if said so
                if (!overwrite) {
                    return null;
                }

                // ignore newer files
                if (dest.lastModified() > CayenneGenerator.this.mapTimestamp) {
                    return null;
                }
            }

            log("Generating class file :" + dest.getCanonicalPath());
            return new FileWriter(dest);
        }

        /** Returns a File object corresponding to a directory where files
         *  that belong to <code>pkgName</code> package should reside. 
         *  Creates any missing diectories below <code>destDir</code>.
         */
        private File mkpath(String basePath, String pkgName) throws Exception {
            File dest = getProject().resolveFile(basePath);

            if (pkgName == null) {
                return dest;
            }

            String path = pkgName.replace('.', File.separatorChar);
            File fullPath = new File(dest, path);
            if (!fullPath.isDirectory() && !fullPath.mkdirs()) {
                throw new Exception("Error making path: " + fullPath);
            }

            return fullPath;
        }
    }
}