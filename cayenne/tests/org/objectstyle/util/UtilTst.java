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

import junit.framework.*;
import java.util.logging.*;
import java.util.*;
import java.net.*;
import javax.naming.*;
import java.io.*;
import java.util.jar.*;


public class UtilTst extends TestCase {
    static Logger logObj = Logger.getLogger(UtilTst.class.getName());

    private File fTmpFileInCurrentDir;
    private String fTmpFileName;
    private File fTmpFileCopy;

    public UtilTst(String name) {
        super(name);
    }

    protected void setUp() throws java.lang.Exception {
        fTmpFileName =
            "." + File.separator + System.currentTimeMillis() + ".tmp";

        fTmpFileInCurrentDir = new File(fTmpFileName);

        // right some garbage to the temp file, so that it is not empty
        FileWriter fout = new FileWriter(fTmpFileInCurrentDir);
        fout.write("This is total grabage..");
        fout.close();

        fTmpFileCopy = new File(fTmpFileName + ".copy");
    }


    protected void tearDown() throws java.lang.Exception {
        if(!fTmpFileInCurrentDir.delete())
            throw new Exception("Error deleting temporary file: "
                                + fTmpFileInCurrentDir);

        if(fTmpFileCopy.exists() && !fTmpFileCopy.delete())
            throw new Exception("Error deleting temporary file: "
                                + fTmpFileCopy);

    }

    
    public void testCopyFile() throws java.lang.Exception {
        assertTrue("Temp file " + fTmpFileCopy + " is on the way, please delete it manually.", !fTmpFileCopy.exists());

        assertTrue(Util.copy(fTmpFileInCurrentDir, fTmpFileCopy));
        assertTrue(fTmpFileCopy.exists());
        assertEquals(fTmpFileCopy.length(), fTmpFileInCurrentDir.length());
    }


    public void testCopyFileUrl() throws java.lang.Exception {
        assertTrue("Temp file " + fTmpFileCopy + " is on the way, please delete it manually.", !fTmpFileCopy.exists());

        assertTrue(Util.copy(fTmpFileInCurrentDir.toURI().toURL(), fTmpFileCopy));
        assertTrue(fTmpFileCopy.exists());
        assertEquals(fTmpFileCopy.length(), fTmpFileInCurrentDir.length());
    }


    public void testCopyJarUrl() throws java.lang.Exception {
        URL fileInJar = ClassLoader.getSystemResource("test_resources/testfile1.txt");
        assertNotNull(fileInJar);
        assertTrue(fileInJar.toExternalForm().startsWith("jar:"));

        assertTrue(Util.copy(fileInJar, fTmpFileCopy));
        assertTrue(fTmpFileCopy.exists());

        // check file size in a jar
        InputStream in = null;
        try {
            in = fileInJar.openConnection().getInputStream();
            int len = 0;
            while(in.read() >= 0) {
                len++;
            }
            assertEquals(len, fTmpFileCopy.length());
        } catch(IOException ioex) {
            fail();
        } finally {
            if(in != null)
                in.close();
        }

    }


    public void testDeleteFile() throws java.lang.Exception {
        // delete file
        assertTrue("Temp file " + fTmpFileCopy + " is on the way, please delete it manually.", !fTmpFileCopy.exists());
        Util.copy(fTmpFileInCurrentDir, fTmpFileCopy);
        assertTrue(Util.delete(fTmpFileCopy.getPath(), false));

        // delete empty dir with no recursion
        String tmpDirName = "tmpdir_" + System.currentTimeMillis();
        File tmpDir = new File(tmpDirName);
        assertTrue(tmpDir.mkdir());
        assertTrue(Util.delete(tmpDirName, false));
        assertTrue(!tmpDir.exists());

        // delete dir with files with recurions
        assertTrue(tmpDir.mkdir());
        assertTrue(new File(tmpDir, "aaa").createNewFile());
        assertTrue(Util.delete(tmpDirName, true));
        assertTrue(!tmpDir.exists());

        // fail delete dir with files with no recurions
        assertTrue(tmpDir.mkdir());
        assertTrue(new File(tmpDir, "aaa").createNewFile());
        assertTrue(!Util.delete(tmpDirName, false));
        assertTrue(tmpDir.exists());
        assertTrue(Util.delete(tmpDirName, true));
        assertTrue(!tmpDir.exists());
    }



    public void testCloneViaSerialization() throws java.lang.Exception {
        // need a special subclass of Object to make "clone" method public
        SerializableObject o1 = new SerializableObject();
        Object o2 = Util.cloneViaSerialization(o1);
        assertEquals(o1, o2);
        assertTrue(o1 != o2);
    }
}
