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
import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.ZipUtil;

/**
 * Performs on the fly reconfiguration of Cayenne projects.
 *  
 * @author Andrei Adamchik
 */
public class ProjectConfigurator {
    private static Logger logObj = Logger.getLogger(ProjectConfigurator.class);
    protected ProjectConfigInfo info;

    public ProjectConfigurator(ProjectConfigInfo info) {
        this.info = info;
    }

    /**
     * Performs reconfiguration of the project.
     * 
     * @throws ProjectException
     */
    public void execute() throws ProjectException {
        File tmpDir = null;
        File tmpDest = null;
        try {
            // initialize default settings 
            if (info.getDestJar() == null) {
                info.setDestJar(info.getSourceJar());
            }

            // perform sanity check
            validate();

            // do the processing
            tmpDir = makeTempDirectory();
            ZipUtil.unzip(info.getSourceJar(), tmpDir);


            tmpDest = makeTempDestJar();
            ZipUtil.zip(tmpDest, tmpDir, tmpDir.listFiles(), '/');

            // finally, since everything goes well so far, rename temp file to final name
            if (info.getDestJar().exists() && !info.getDestJar().delete()) {
                throw new IOException(
                    "Can't delete old jar file: " + info.getDestJar());
            }

            if (!tmpDest.renameTo(info.getDestJar())) {
                throw new IOException(
                    "Error renaming: " + tmpDest + " to " + info.getDestJar());
            }
        } catch (Exception ex) {
            throw new ProjectException("Error performing reconfiguration.", ex);
        } finally {
            if (tmpDir != null) {
                cleanup(tmpDir);
            }

            if (tmpDest != null) {
                tmpDest.delete();
            }
        }
    }

    /**
     * Returns a temporary file for the destination jar.
     */
    protected File makeTempDestJar() throws IOException {
        File destFolder = info.getDestJar().getParentFile();
        if (destFolder != null && !destFolder.isDirectory()) {
            if (!destFolder.mkdirs()) {
                throw new IOException(
                    "Can't create directory: " + destFolder.getCanonicalPath());
            }
        }

        String baseName = "tmp_" + info.getDestJar().getName();

        // seeting upper limit on a number of tries, though normally we would expect
        // to succeed from the first attempt...
        for (int i = 0; i < 50; i++) {
            File tmpFile =
                (destFolder != null)
                    ? new File(destFolder, baseName + i)
                    : new File(baseName + i);
            if (!tmpFile.exists()) {
                return tmpFile;
            }
        }

        throw new IOException("Problems creating temporary file.");
    }

    /**
     *  Deletes a temporary directories and files created.
     */
    protected void cleanup(File dir) {
        if (!Util.delete(dir.getPath(), true)) {
            logObj.info("Can't delete temporary directory: " + dir);
        }
    }

    /**
     * Creates a temporary directory to unjar the jar file.
     * 
     * @return File
     * @throws IOException
     */
    protected File makeTempDirectory() throws IOException {
        File destFolder = info.getDestJar().getParentFile();
        if (destFolder != null && !destFolder.isDirectory()) {
            if (!destFolder.mkdirs()) {
                throw new IOException(
                    "Can't create directory: " + destFolder.getCanonicalPath());
            }
        }

        String baseName = info.getDestJar().getName();
        if (baseName.endsWith(".jar")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        // seeting upper limit on a number of tries, though normally we would expect
        // to succeed from the first attempt... 
        for (int i = 0; i < 50; i++) {
            File tmpDir =
                (destFolder != null)
                    ? new File(destFolder, baseName + i)
                    : new File(baseName + i);
            if (!tmpDir.exists()) {
                if (!tmpDir.mkdir()) {
                    throw new IOException(
                        "Can't create directory: " + tmpDir.getCanonicalPath());
                }

                return tmpDir;
            }
        }

        throw new IOException("Problems creating temporary directory.");
    }

    /**
     * Validates consistency of the reconfiguration information.
     */
    protected void validate() throws Exception {
        if (info == null) {
            throw new IllegalArgumentException("ProjectConfig info is not set.");
        }

        if (info.getSourceJar() == null) {
            throw new IllegalArgumentException("Source jar file is not set.");
        }

        if (!info.getSourceJar().isFile()) {
            throw new IOException(info.getSourceJar() + " is not a file.");
        }

        if (!info.getSourceJar().canRead()) {
            throw new IOException("Can't read file: " + info.getSourceJar());
        }
    }
}
