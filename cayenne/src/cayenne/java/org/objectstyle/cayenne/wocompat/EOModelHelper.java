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
package org.objectstyle.cayenne.wocompat;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.wocompat.parser.Parser;

/**
 *  Helper class used by EOModelProcessor. During creation, it loads
 *  EOModel from the specified location and parses EOModel files storing them
 *  internally as maps. EOModelProcessor will use this information to create 
 *  DataMap instance.
 */
public class EOModelHelper {
    private static final ResourceLocator locator = new ResourceLocator();

    private Parser plistParser = new Parser();
    protected URL modelUrl;
    protected Map entityIndex;
    protected Map entityClassIndex;
    protected DataMap dataMap;

    static {
        // configure locator 
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(false);
        locator.setSkipHomeDirectory(true);
        locator.setSkipAbsolutePath(false);
    }

    /** 
     *  Creates helper instance and tries to locate 
     *  EOModel and load index file. 
     */
    public EOModelHelper(String path) throws Exception {
        // configure URL
        modelUrl = findModelUrl(path);

        // configure name
        dataMap = new DataMap(findModelName(path));

        // load index file
        List modelIndex = (List) loadModelIndex().get("entities");

        // load entity indices
        entityIndex = new HashMap();
        entityClassIndex = new HashMap();

        Iterator it = modelIndex.iterator();
        while (it.hasNext()) {
            Map info = (Map) it.next();
            String name = (String) info.get("name");
            entityIndex.put(name, loadEntityIndex(name));
            entityClassIndex.put(name, info.get("className"));
        }
    }

    /** Performs Objective C data types conversion to Java types.
     * 
     *  @return String representation for Java type corresponding 
     *  to String representation of Objective C type. 
     */
    public String javaTypeForEOModelerType(String type) {
        if(type == null) {
        	return null;
        }
        
        if (type.equals("NSString"))
            return "java.lang.String";
        if (type.equals("NSNumber"))
            return "java.lang.Integer";
        if (type.equals("NSCalendarDate"))
            return "java.sql.Date";
        if (type.equals("NSDecimalNumber"))
            return "java.math.BigDecimal";
        if (type.equals("NSData"))
            return "byte[]";

        throw new IllegalArgumentException("Unknown data type: " + type);
    }

    /** Returns a DataMap associated with this helper. */
    public DataMap getDataMap() {
        return dataMap;
    }

    /** Returns EOModel location as URL. */
    public URL getModelUrl() {
        return modelUrl;
    }

    /** Returns an iterator of model names. */
    public Iterator modelNames() {
        return entityClassIndex.keySet().iterator();
    }

    /** Returns an info map for the entity called <code>entityName</code>. */
    public Map entityInfo(String entityName) {
        return (Map) entityIndex.get(entityName);
    }

    /** Returns an info map for the entity called <code>entityName</code>. */
    public String entityClass(String entityName) {
        return (String) entityClassIndex.get(entityName);
    }

    /** Loads EOModel index and returns it as a map. */
    protected Map loadModelIndex() throws Exception {
        InputStream indexIn = openIndexStream();
        try {
            plistParser.ReInit(indexIn);
            return (Map) plistParser.propertyList();
        }
        finally {
            indexIn.close();
        }
    }

    /** Loads EOEntity information and returns it as a map. */
    protected Map loadEntityIndex(String entityName) throws Exception {
        InputStream entIn = openEntityStream(entityName);
        try {
            plistParser.ReInit(entIn);
            return (Map) plistParser.propertyList();
        }
        finally {
            entIn.close();
        }
    }

    /** Returns EOModel name based on its path. */
    protected String findModelName(String path) {
        // strip trailing slashes
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }

        // strip path components
        int i1 = path.lastIndexOf("/");
        int i2 = path.lastIndexOf("\\");
        int i = (i1 > i2) ? i1 : i2;
        if (i >= 0) {
            path = path.substring(i + 1);
        }

        // strip .eomodeld suffix
        if (path.endsWith(".eomodeld")) {
            path = path.substring(0, path.length() - ".eomodeld".length());
        }

        return path;
    }

    /** Returns a URL of the EOModel directory. Throws exception if it 
     *  can't be found. */
    protected URL findModelUrl(String path) {
        if (!path.endsWith(".eomodeld")) {
            path += ".eomodeld";
        }

        URL base = locator.findDirectoryResource(path);
        if (base == null) {
            throw new IllegalArgumentException("Can't find EOModel: " + path);
        }
        return base;
    }

    /** 
     * Returns InputStream to read an EOModel index file.
     */
    protected InputStream openIndexStream() throws Exception {
        return new URL(modelUrl, "index.eomodeld").openStream();
    }

    /** Returns InputStream to read an EOEntity plist file.
     * 
     * @param entityName name of EOEntity to be loaded.
     * 
     * @return InputStream to read an EOEntity plist file or null if
     * <code>entityname.plist</code> file can not be located.
     */
    protected InputStream openEntityStream(String entityName) throws Exception {
        return new URL(modelUrl, entityName + ".plist").openStream();
    }
}