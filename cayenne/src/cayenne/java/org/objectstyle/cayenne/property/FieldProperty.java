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
package org.objectstyle.cayenne.property;

import java.lang.reflect.Field;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Util;

/**
 * A property descriptor that provides access to an object property implemented as a
 * Field.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class FieldProperty implements PersistentProperty {

    protected String propertyName;
    protected Field field;

    public FieldProperty(Class beanClass, String propertyName) {
        this(beanClass, propertyName, null);
    }

    /**
     * Creates a descriptor for a simple bean property.
     */
    public FieldProperty(Class beanClass, String propertyName, Class propertyType) {
        // sanity check
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }

        if (beanClass == null) {
            throw new IllegalArgumentException("Null beanClass");
        }

        this.propertyName = propertyName;
        this.field = prepareField(beanClass, propertyName, propertyType);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class getPropertyType() {
        return field.getType();
    }

    /**
     * Does nothing.
     */
    public void willRead(Object object) throws PropertyAccessException {
        // noop
    }

    /**
     * Does nothing.
     */
    public void willWrite(Object object, Object newValue) throws PropertyAccessException {
        // noop
    }

    public Object directRead(Object object) throws PropertyAccessException {
        try {
            return field.get(object);
        }
        catch (Throwable th) {
            throw new PropertyAccessException(
                    "Error reading field: " + field.getName(),
                    this,
                    object,
                    th);
        }

    }

    public void directWrite(Object object, Object value) throws PropertyAccessException {
        try {
            field.set(object, value);
        }
        catch (Throwable th) {
            throw new PropertyAccessException(
                    "Error writing field: " + field.getName(),
                    this,
                    object,
                    th);
        }
    }

    /**
     * Finds a field for the property, ensuring that direct access via reflection is
     * possible.
     */
    protected Field prepareField(Class beanClass, String propertyName, Class propertyType) {
        Field field;

        // locate field
        try {
            field = lookupFieldInHierarchy(beanClass, propertyName);
        }
        catch (SecurityException e) {
            throw new CayenneRuntimeException("Error accessing field '"
                    + propertyName
                    + "' in class: "
                    + beanClass.getName(), e);
        }
        catch (NoSuchFieldException e) {
            throw new CayenneRuntimeException("No field '"
                    + propertyName
                    + "' is defined in class: "
                    + beanClass.getName(), e);
        }

        // set accessability
        if (!Util.isAccessible(field)) {
            field.setAccessible(true);
        }

        // check that the field is of expected class...
        if (propertyType != null) {
            if (!propertyType.getName().equals(field.getType().getName())) {
                throw new CayenneRuntimeException("Expected property type '"
                        + propertyType.getName()
                        + "', got '"
                        + field.getType().getName()
                        + "'. Property: "
                        + beanClass.getName()
                        + "."
                        + propertyName);
            }
        }

        return field;
    }

    /**
     * Recursively looks for a named field in a class hierarchy.
     * 
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    protected Field lookupFieldInHierarchy(Class beanClass, String fieldName)
            throws SecurityException, NoSuchFieldException {
        try {
            return beanClass.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e) {

            Class superClass = beanClass.getSuperclass();
            if (superClass == null || superClass.getName().equals(Object.class.getName())) {
                throw e;
            }

            return lookupFieldInHierarchy(superClass, fieldName);
        }
    }
}
