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
package org.objectstyle.cayenne.util;

import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

import org.apache.log4j.*;
import org.apache.oro.text.perl.*;
import org.xml.sax.*;

/**
 *  Utility methods sink.
 */
public class Util {
    private static Logger logObj = Logger.getLogger(Util.class);

    private static final Perl5Util regexUtil = new Perl5Util();

    /** Makes up for the lack of file copying utilities in Java */
    public static boolean copy(File from, File to) {
        BufferedInputStream fin = null;
        BufferedOutputStream fout = null;
        try {
            int bufSize = 8 * 1024;
            fin = new BufferedInputStream(new FileInputStream(from), bufSize);
            fout = new BufferedOutputStream(new FileOutputStream(to), bufSize);
            copyPipe(fin, fout, bufSize);
        } catch (IOException ioex) {
            return false;
        } catch (SecurityException sx) {
            return false;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException cioex) {}
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException cioex) {}
            }
        }
        return true;
    }

    /** Save URL contents to a file */
    public static boolean copy(URL from, File to) {
        BufferedInputStream urlin = null;
        BufferedOutputStream fout = null;
        try {
            int bufSize = 8 * 1024;
            urlin =
                new BufferedInputStream(from.openConnection().getInputStream(), bufSize);
            fout = new BufferedOutputStream(new FileOutputStream(to), bufSize);
            copyPipe(urlin, fout, bufSize);
        } catch (IOException ioex) {
            return false;
        } catch (SecurityException sx) {
            return false;
        } finally {
            if (urlin != null) {
                try {
                    urlin.close();
                } catch (IOException cioex) {}
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException cioex) {}
            }
        }
        return true;
    }

    private static void copyPipe(InputStream in, OutputStream out, int bufSizeHint)
        throws IOException {
        int read = -1;
        byte[] buf = new byte[bufSizeHint];
        while ((read = in.read(buf, 0, bufSizeHint)) >= 0) {
            out.write(buf, 0, read);
        }
        out.flush();
    }

    /** Improved File.delete method that allows recursive directory deletion. */
    public static boolean delete(String filePath, boolean recursive) {
        File file = new File(filePath);
        if (!file.exists())
            return true;

        if (!recursive || !file.isDirectory())
            return file.delete();

        String[] list = file.list();
        for (int i = 0; i < list.length; i++) {
            if (!delete(filePath + File.separator + list[i], true))
                return false;
        }

        return file.delete();
    }

    public static String substBackslashes(String str) {
        if (str == null) {
            return null;
        }

        return regexUtil.match("/\\\\/", str)
            ? regexUtil.substitute("s/\\\\/\\//g", str)
            : str;
    }

    /** Compare two objects just like "equals" would. Unlike Object.equals,
    * this method allows any of the 2 objects to be null. */
    public static boolean nullSafeEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null)
            return true;
        else if (obj1 != null)
            return obj1.equals(obj2);
        else
            return obj2.equals(obj1);
    }

    /**
     * Returns true, if the String is null or an empty string.
     */
    public static boolean isEmptyString(String str) {
        return str == null || str.length() == 0;
    }

    /** Create object copy using serialization mechanism. */
    public static Object cloneViaSerialization(Serializable obj) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(obj);
        out.close();

        ObjectInputStream in =
            new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Object objCopy = in.readObject();
        in.close();
        return objCopy;
    }

    /** Creates an XMLReader with default feature set. Note that all objectstyle
      * internal XML parsers should probably use XMLReader obtained via this
      * method for consistency sake, and can customize feature sets as needed. */
    public static XMLReader createXmlReader()
        throws SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        // Create a JAXP SAXParser
        SAXParser saxParser = spf.newSAXParser();

        // Get the encapsulated SAX XMLReader
        XMLReader reader = saxParser.getXMLReader();

        // set default features
        reader.setFeature("http://xml.org/sax/features/namespaces", true);

        return reader;
    }

    /** Returns package information for the <code>className</code>
      * parameter as a path separated with forward slash ('/').
      * For example for class a.b.c.ClassName "a/b/c" will be returned.
      * Method is used to lookup resources that are located in package subdirectories. */
    public static String getPackagePath(String className) {
        if (regexUtil.match("/\\./", className)) {
            String path = regexUtil.substitute("s/\\./\\//g", className);
            return path.substring(0, path.lastIndexOf("/"));
        } else {
            return "";
        }
    }

    /**
     * Utility method to extract extension from of file name.
     */
    public static String extractFileExtension(String fileName) {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0 && dotInd < fileName.length())
            ? fileName.substring(dotInd + 1)
            : null;
    }

    /**
     * Utility method to remove extension from a file name.
     */
    public static String stripFileExtension(String fileName) {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0) ? fileName.substring(0, dotInd) : fileName;
    }

    /** 
     * Encodes a string so that it can be used as an attribute value in an XML document.
     * Will do conversion of the greater/less signs, quotes and ampersands.
     */
    public static String encodeXmlAttribute(String str) {
        if (str == null)
            return null;

        int len = str.length();
        if (len == 0)
            return str;

        StringBuffer encoded = new StringBuffer();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '<')
                encoded.append("&lt;");
            else if (c == '\"')
                encoded.append("&quot;");
            else if (c == '>')
                encoded.append("&gt;");
            else if (c == '\'')
                encoded.append("&apos;");
            else if (c == '&')
                encoded.append("&amp;");
            else
                encoded.append(c);
        }

        return encoded.toString();
    }
}
