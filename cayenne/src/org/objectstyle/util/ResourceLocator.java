package org.objectstyle.util;
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
import java.net.URL;
import java.util.logging.Logger;


/** Utility class to find resources (usually files) using most common Java approaches.
  * Some ideas were inspired by Velocity ResourceLoader classes (Copyright: Apache 
  * Software Foundation).
  *
  * @author Andrei Adamchik
  */
public class ResourceLocator {
    static Logger logObj = Logger.getLogger(ResourceLocator.class.getName());


    /** Returns a resource as InputStream if it is found in CLASSPATH. 
      * Returns null otherwise. Lookup is normally performed in all JAR and
      * ZIP files and directories available to CLassLoader. */
    public static InputStream findResourceInClasspath(String name) {
        try {
            URL url = ResourceLocator.class.getClassLoader().getResource(name);
            return (url != null) ? url.openStream() : null;
        } catch(IOException ioex) {
            return null;
        }
    }


    /** Returns a resource as InputStream if it is found in the filesystem. 
      * Returns null otherwise. Lookup is first performed relative to the user 
      * home directory (as defined by "user.home" system property), and then
      * relative to the current directory. */
    public static InputStream findResourceInFileSystem(String name) {
        try {
            // look in home directory
            String homeDirPath = System.getProperty("user.home") + File.separator + name;
            File file = new File(homeDirPath);

            if(file.canRead())
                return new FileInputStream(file);

            // look in current directory
            String curDirPath = '.' + File.separator + name;
            file = new File(curDirPath);
            if(file.canRead())
                return new FileInputStream(file);
            
        } catch(IOException ioex) {
            return null;
        }
        
        return null;
    }
    
    public static File findFileInFileSystem(String name) {
        // look in home directory
        String homeDirPath = System.getProperty("user.home") + File.separator + name;
        File file = new File(homeDirPath);

        if(file.canRead())
            return file;

        // look in current directory
        String curDirPath = '.' + File.separator + name;
        file = new File(curDirPath);
        if(file.canRead())
            return file;
        return null;
    }
    
    public static URL findURLInClasspath(String name) {
        URL url = ResourceLocator.class.getClassLoader().getResource(name);
        return (url != null) ? url : null;
    }
}
