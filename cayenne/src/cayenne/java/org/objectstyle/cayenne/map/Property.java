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
package org.objectstyle.cayenne.map;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * A descriptor of a simple bean property.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// using java.beans.PropertyDescriptor is also possible, but it seems to have lots of
// baggage coming with it. As we need to subclass it, it becomes even uglier...
public class Property implements Serializable {

    protected String propertyName;
    protected Class propertyType;
    protected Method readMethod;
    protected Method writeMethod;

    static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }

        char chars[] = s.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    /**
     * Creates a descriptor for a simple bean property.
     */
    public Property(String propertyName, Class beanClass) {
        // sanity check
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }

        if (beanClass == null) {
            throw new IllegalArgumentException("Null beanClass");
        }

        this.propertyName = propertyName;

        // find getters and setters
        String base = capitalize(propertyName);
        this.readMethod = findGetter("get" + base, beanClass);

        if (readMethod != null) {
            this.writeMethod = findMatchingSetter("set" + base, beanClass, readMethod
                    .getReturnType());
        }
        else {
            this.writeMethod = findSetter("set" + base, beanClass);
        }

        // sanity check
        if (readMethod == null && writeMethod == null) {
            throw new CayenneRuntimeException("Property does not exist: '"
                    + propertyName
                    + "'. Bean class: "
                    + beanClass.getName());
        }

        // find property class...
        this.propertyType = findPropertyType(readMethod, writeMethod);
    }

    /**
     * Creates a property descriptor of a specified type.
     */
    public Property(String propertyName, Class beanClass, Class propertyType) {
        this(propertyName, beanClass);

        if (propertyType != null
                && !propertyType.getName().equals(this.propertyType.getName())) {

            throw new CayenneRuntimeException("Invalid property type. Expected '"
                    + propertyType.getName()
                    + "' got '"
                    + this.propertyType.getName()
                    + "'. Property: "
                    + beanClass.getName()
                    + "."
                    + propertyName);
        }
    }

    protected Class findPropertyType(Method getter, Method setter) {
        if (getter != null) {
            return getter.getReturnType();
        }
        else if (setter != null) {
            return setter.getParameterTypes()[0];
        }
        else {
            throw new CayenneRuntimeException(
                    "Can't determine property type. No getter or setter exist. Property: "
                            + propertyName);
        }
    }

    protected Method findGetter(String methodName, Class beanClass) {

        try {
            return beanClass.getMethod(methodName, null);
        }
        catch (SecurityException e) {
            return null;
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Locates a setter by name. The first public void method with a single parameter
     * matching the name will be considered a setter. This method is used to locate rare
     * write-only properties.
     */
    protected Method findSetter(String methodName, Class beanClass) {

        Method[] methods = beanClass.getMethods();

        for (int i = 0; i < methods.length; i++) {
            // check name
            if (methodName.equals(methods[i].getName())) {
                // check modifiers
                int modifiers = methods[i].getModifiers();
                if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                    // check return type and parameters
                    if (Void.TYPE.equals(methods[i].getReturnType())
                            && methods[i].getParameterTypes().length == 1) {
                        return methods[i];
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds a setter for a known property type.
     */
    protected Method findMatchingSetter(
            String methodName,
            Class beanClass,
            Class propertyType) {

        try {
            return beanClass.getMethod(methodName, new Class[] {
                propertyType
            });
        }
        catch (SecurityException e) {
            return null;
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class getPropertyType() {
        return propertyType;
    }
}
