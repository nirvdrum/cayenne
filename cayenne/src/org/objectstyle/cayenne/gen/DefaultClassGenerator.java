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

package org.objectstyle.cayenne.gen;

import java.io.*;
import java.util.List;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Extends MapClassGenerator to allow target-specific filesystem locations 
 * where the files should go. Adds "execute" method that performs class 
 * generation based on the internal state of this object.
 * 
 * @author Andrei Adamchik
 */
public class DefaultClassGenerator extends MapClassGenerator {
    protected File destDir;
    protected boolean overwrite;
    protected boolean usepkgpath = true;
    protected boolean makepairs = true;
    protected File template;
    protected File supertemplate;
    protected long timestamp = System.currentTimeMillis();

    public DefaultClassGenerator() {}

    /** 
     * Creates class generator and initializes it with DataMap.
     * This will ensure generation of classes for all ObjEntities
     * in the DataMap.
     */
    public DefaultClassGenerator(DataMap map) {
        this(map.getObjEntitiesAsList());
    }
    
    /** 
     * Creates class generator and initializes it with the list of ObjEntities
     * that will be used in class generation.
     */
    public DefaultClassGenerator(List objEntities) {
        super(objEntities);
    }

    /** Runs class generation. */
    public void execute() throws Exception {
        validateAttributes();

        if (makepairs) {
            String t = getTemplateForPairs();
            String st = getSupertemplateForPairs();
            generateClassPairs(t, st, MapClassGenerator.SUPERCLASS_PREFIX);
        }
        else {
            generateSingleClasses(getTemplateForSingles());
        }
    }

    /** 
     * Validates the state of this class generator. Throws exception if 
     * it is in inconsistent state. Called internally from "execute".
     */
    protected void validateAttributes() throws Exception {
        if (destDir == null) {
            throw new Exception("'destDir' attribute is missing.");
        }

        if (!destDir.isDirectory()) {
            throw new Exception("'destDir' is not a directory.");
        }

        if (!destDir.canWrite()) {
            throw new Exception("Do not have write permissions for " + destDir);
        }

        if (template != null && !template.canRead()) {
            throw new Exception("Can't read template from " + template);
        }

        if (makepairs && supertemplate != null && !supertemplate.canRead()) {
            throw new Exception("Can't read super template from " + supertemplate);
        }
    }

    /**
     * Sets the destDir.
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
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

        File dest = new File(mkpath(destDir, pkgName), className + ".java");

        // ignore newer files
        if (dest.exists() && !isOld(dest)) {
            return null;
        }

        return new FileWriter(dest);
    }

    private Writer writerForClass(
        ObjEntity entity,
        String pkgName,
        String className)
        throws Exception {

        File dest = new File(mkpath(destDir, pkgName), className + ".java");
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
            if (!isOld(dest)) {
                return null;
            }
        }

        return new FileWriter(dest);
    }

    /** 
     * Returns true if <code>file</code> parameter is older than internal 
     * timestamp of this class generator.
     */
    protected boolean isOld(File file) {
        return file.lastModified() <= getTimestamp();
    }

    /** 
     *  Returns a File object corresponding to a directory where files
     *  that belong to <code>pkgName</code> package should reside. 
     *  Creates any missing diectories below <code>dest</code>.
     */
    protected File mkpath(File dest, String pkgName) throws Exception {

        if (!usepkgpath || pkgName == null) {
            return dest;
        }

        String path = pkgName.replace('.', File.separatorChar);
        File fullPath = new File(dest, path);
        if (!fullPath.isDirectory() && !fullPath.mkdirs()) {
            throw new Exception("Error making path: " + fullPath);
        }

        return fullPath;
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

    /**
     * Returns internal timestamp of this generator used to make
     * decisions about overwriting individual files. 
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}