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

package org.objectstyle.cayenne.access.types;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.objectstyle.cayenne.util.Util;

/**
 * Contains a map of ExtendedType objects, that serve as handlers for converting values
 * between Java application and JDBC layer.
 * <p>
 * Class uses singleton model, since mapping is usually shared within the application.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class ExtendedTypeMap {

    protected Map typeMap = new HashMap();
    protected DefaultType defaultType = new DefaultType();

    Constructor enumTypeConstructor;

    /**
     * Creates new ExtendedTypeMap, populating it with default JDBC-compatible types.
     */
    public ExtendedTypeMap() {
        // see if we can support enums
        try {
            Class enumTypeClass = Util
                    .getJavaClass("org.objectstyle.cayenne.access.types.EnumType");
            this.enumTypeConstructor = enumTypeClass.getConstructor(new Class[] {
                Class.class
            });
        }
        catch (Throwable th) {
            // no enums support... either Java 1.4 or Cayenne 1.5 extensions are absent
        }

        this.initDefaultTypes();
    }

    /**
     * Registers default extended types. This method is called from constructor.
     */
    protected void initDefaultTypes() {
        // register default types
        Iterator it = DefaultType.defaultTypes();
        while (it.hasNext()) {
            registerType(new DefaultType((String) it.next()));
        }
    }

    /**
     * Adds a new type to the list of registered types. If there is another type
     * registered for a class described by the <code>type</code> argument, the old
     * handler is overwriden by the new one.
     */
    public void registerType(ExtendedType type) {
        typeMap.put(type.getClassName(), type);
    }

    /**
     * Returns a default ExtendedType that is used to handle unmapped types.
     */
    public ExtendedType getDefaultType() {
        return defaultType;
    }

    /**
     * Returns a type registered for the class name. If no such type exists, returns the
     * default type. It is guaranteed that this method returns a non-null ExtendedType
     * instance. Note that for array types class name must be in the form 'MyClass[]'.
     */
    public ExtendedType getRegisteredType(String javaClassName) {
        ExtendedType type = (ExtendedType) typeMap.get(javaClassName);

        if (type != null) {
            return type;
        }

        type = getDefaultType(javaClassName);

        if (type != null) {
            // register to speed up future access
            registerType(type);
            return type;
        }

        return getDefaultType();
    }

    /**
     * Returns a type registered for the class name. If no such type exists, returns the
     * default type. It is guaranteed that this method returns a non-null ExtendedType
     * instance.
     */
    public ExtendedType getRegisteredType(Class javaClass) {
        String name = null;

        if (javaClass.isArray()) {
            // only support single dimensional arrays now
            name = javaClass.getComponentType() + "[]";
        }
        else {
            name = javaClass.getName();
        }

        return getRegisteredType(name);
    }

    /**
     * Removes registered ExtendedType object corresponding to <code>javaClassName</code>
     * parameter.
     */
    public void unregisterType(String javaClassName) {
        typeMap.remove(javaClassName);
    }

    /**
     * Returns array of Java class names supported by Cayenne for JDBC mapping.
     */
    public String[] getRegisteredTypeNames() {
        Set keys = typeMap.keySet();
        int len = keys.size();
        String[] types = new String[len];

        Iterator it = keys.iterator();
        for (int i = 0; i < len; i++) {
            types[i] = (String) it.next();
        }

        return types;
    }

    /**
     * Retruns default type for specific Java classes. This implementation supports
     * dynamically loading EnumType handlers for concrete Enum classes (assuming the
     * application runs under JDK1.5+).
     * 
     * @return a default type for a given class or null if a class has no default type
     *         mapping.
     * @since 1.2
     */
    protected ExtendedType getDefaultType(String javaClassName) {

        if (javaClassName == null) {
            return null;
        }

        // load enum type if possible
        if (enumTypeConstructor == null) {
            return null;
        }

        try {
            Class enumClass = Util.getJavaClass(javaClassName);

            // load EnumType via reflection as the source has to stay 1.4 compliant
            ExtendedType type = (ExtendedType) enumTypeConstructor
                    .newInstance(new Object[] {
                        enumClass
                    });

            return type;
        }
        catch (Throwable th) {
            // ignore exceptions...
            return null;
        }
    }
}
