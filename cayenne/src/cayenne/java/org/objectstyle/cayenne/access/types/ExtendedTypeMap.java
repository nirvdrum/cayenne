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

package org.objectstyle.cayenne.access.types;

import java.util.*;

import org.apache.log4j.*;

/** Handles both standard Java class mapping to JDBC types as well as
  * custom mapping.
  * Standard mapping is defined in JDBC documentation (for example at
  * <a href="http://java.sun.com/j2se/1.3/docs/guide/jdbc/getstart/mapping.html">
  * http://java.sun.com/j2se/1.3/docs/guide/jdbc/getstart/mapping.html</a>)
  * But it is often rather convenient to have an arbitrary class mapped to 
  * a JDBC type. For example java.util.Date can be used for mapping to DATE,
  * TIME and TIMESTAMP. 
  * 
  * <p>Class uses singleton model, since mapping is usually shared within
  * the application. </p>
  */
public class ExtendedTypeMap {
    static Logger logObj = Logger.getLogger(ExtendedTypeMap.class.getName());

    protected Map typeMap = new HashMap();
    protected DefaultType defaultType = new DefaultType();


    public ExtendedTypeMap() {
        initDefaultTypes();
    }

    /** Registers default extended types. This method is called from
      * constructor and exists mainly for the benefit of subclasses that
      * can override it and configure their own extended types. */
    protected void initDefaultTypes() {
        // register default types
        Iterator it = DefaultType.defaultTypes();
        while(it.hasNext()) {
            registerType(new DefaultType((String)it.next()));
        }

        // register java.util.Date handler
        registerType(new UtilDateType());
    }

    /** Adds new type to the list of registered types. */
    public void registerType(ExtendedType type) {
        typeMap.put(type.getClassName(), type);
    }

    public ExtendedType getDefaultType() {
        return defaultType;
    }

    public ExtendedType getRegisteredType(String javaClassName) {
        ExtendedType type = (ExtendedType)typeMap.get(javaClassName);
        return (type != null) ? type : defaultType;
    }

    /** 
     * Removes registered ExtendedType object corresponding to
     * <code>javaClassName</code> parameter. 
     */
    public void unregisterType(String javaClassName) {
        typeMap.remove(javaClassName);
    }

    /** 
     * Returns array of Java class names supported by Cayenne 
     * for JDBC mapping. 
     */
    public String[] getRegisteredTypeNames() {
        Set keys = typeMap.keySet();
        int len = keys.size();
        String[] types = new String[len];
        
        Iterator it = keys.iterator();
        for(int i = 0; i < len; i++) {
            types[i] = (String)it.next();
        }
        
        return types;
    }
}
