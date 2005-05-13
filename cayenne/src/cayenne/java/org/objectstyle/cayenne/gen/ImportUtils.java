/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Methods for mangling strings.
 * 
 * @author Mike Kienenberger
 */
public class ImportUtils {

    public static final String importOrdering[] = new String[] {
        "java.", "javax.", "org.", "com." };

//    public static final String primitiveTypes[] = new String[] {
//        "long", "double", "byte", "boolean", "float", "short", "int" };

    protected Map importTypesMap = new HashMap();
    
    protected String packageName;
    
    public ImportUtils()
    {
        super();
    }
    
    public void addType(String typeName)
    {
        StringUtils stringUtils = StringUtils.getInstance();
        String typeClassName = stringUtils.stripPackageName(typeName);
        String typePackageName = stringUtils.stripClass(typeName);
        
        if (typePackageName.length() == 0)  return; // disallow non-packaged types (primatives, probably)
        if ("java.lang".equals(typePackageName))  return;
        if (packageName.equals(typePackageName))  return;
        
        // Can only have one type -- rest must use fqn
        if (importTypesMap.containsKey(typeClassName))  return;
        
        importTypesMap.put(typeClassName, typeName);
    }
    
    public void setPackage(String packageName)
    {
        this.packageName = packageName;
    }
    
    /**
     * Removes registered package and type name prefixes from java types 
     */
    public String formatJavaType(String typeName) {
        if (typeName != null) {
            StringUtils stringUtils = StringUtils.getInstance();
            String typeClassName = stringUtils.stripPackageName(typeName);
            
            if (importTypesMap.containsKey(typeClassName))
            {
                if (typeName.equals((String)importTypesMap.get(typeClassName)))  return typeClassName;
            }
            
            String typePackageName = stringUtils.stripClass(typeName);
            if ("java.lang".equals(typePackageName))  return typeClassName;
            if (packageName.equals(typePackageName))  return typeClassName;
        }

        return typeName;
    }
    
    public String generate()
    {
        StringBuffer outputBuffer = new StringBuffer();

        if (null != packageName)
        {
            outputBuffer.append("package ");
            outputBuffer.append(packageName);
            outputBuffer.append(';');
            outputBuffer.append(System.getProperty("line.separator"));
            outputBuffer.append(System.getProperty("line.separator"));
        }

        List typesList = new ArrayList(importTypesMap.values());
        Collections.sort(typesList, new Comparator() {

            public boolean equals(Object obj) {
                return this.equals(obj);
            }

            public int compare(Object o1, Object o2) {
                
                String s1 = (String)o1;
                String s2 = (String)o2;
                
                for (int index = 0; index < importOrdering.length; index++) {
                    String ordering = importOrdering[index];
                    
                    if ( (s1.startsWith(ordering)) && (!s2.startsWith(ordering)) )
                        return -1;
                    if ( (!s1.startsWith(ordering)) && (s2.startsWith(ordering)) )
                        return 1;
                }
                    
                return s1.compareTo(s2);
            }
        });
        
        String lastStringPrefix = null;
        Iterator typesIterator = typesList.iterator();
        while (typesIterator.hasNext()) {
            String typeName = (String)typesIterator.next();

            // Output another newline if we're in a different root package.
            // Find root package
            String thisStringPrefix = typeName;
            int dotIndex = typeName.indexOf('.');
            if (-1 != dotIndex)
            {
                thisStringPrefix = typeName.substring(0, dotIndex);
            }
            // if this isn't the first import,
            if (null != lastStringPrefix)
            {
                // and it's different from the last import
                if (false == thisStringPrefix.equals(lastStringPrefix))
                {
                    // output a newline
                    outputBuffer.append(System.getProperty("line.separator"));
                }
            }
            lastStringPrefix = thisStringPrefix;

            outputBuffer.append("import ");
            outputBuffer.append(typeName);
            outputBuffer.append(';');
            if (typesIterator.hasNext())
            {
                outputBuffer.append(System.getProperty("line.separator"));
            }
        }

        return outputBuffer.toString();
    }
}