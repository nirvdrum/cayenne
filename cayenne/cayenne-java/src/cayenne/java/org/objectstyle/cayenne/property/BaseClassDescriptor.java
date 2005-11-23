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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * A superclass of Cayenne ClassDescriptors. Defines all main bean descriptor parameters
 * and operations. Subclasses would provide methods to initialize the descriptor.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class BaseClassDescriptor implements ClassDescriptor {

    protected ClassDescriptor superclassDescriptor;

    // compiled properties ... all declared as transient
    protected transient Class objectClass;
    protected transient Map declaredProperties;

    /**
     * Creates an uncompiled BaseClassDescriptor. Subclasses may add a call to "compile"
     * in the constructor after finishing their initialization.
     */
    public BaseClassDescriptor(ClassDescriptor superclassDescriptor) {
        this.superclassDescriptor = superclassDescriptor;
    }

    /**
     * Returns true if a descriptor is initialized and ready for operation.
     */
    public boolean isValid() {
        return objectClass != null && declaredProperties != null;
    }

    public Class getObjectClass() {
        return objectClass;
    }

    /**
     * Returns a read-only collection of property names mapped in this descriptor.
     */
    public Collection getDeclaredPropertyNames() {
        return declaredProperties != null ? Collections
                .unmodifiableCollection(declaredProperties.keySet()) : null;
    }

    public Collection getPropertyNames() {
        if (getSuperclassDescriptor() == null) {
            return getDeclaredPropertyNames();
        }

        // TODO: cache this?
        Collection allNames = new ArrayList(getDeclaredPropertyNames());
        allNames.addAll(getSuperclassDescriptor().getPropertyNames());

        return allNames;
    }

    /**
     * Recursively looks up property descriptor in this class descriptor and all
     * superclass descriptors.
     */
    public Property getProperty(String propertyName) {
        Property property = getDeclaredProperty(propertyName);

        if (property == null && superclassDescriptor != null) {
            property = superclassDescriptor.getProperty(propertyName);
        }

        return property;
    }

    public Property getDeclaredProperty(String propertyName) {
        return (Property) declaredProperties.get(propertyName);
    }

    /**
     * Returns a descriptor of the mapped superclass or null if the descriptor's entity
     * sits at the top of inheritance hierarchy.
     */
    public ClassDescriptor getSuperclassDescriptor() {
        return superclassDescriptor;
    }

    /**
     * Creates a new instance of a class described by this object.
     */
    public Object createObject() {
        if (objectClass == null) {
            throw new NullPointerException(
                    "Null objectClass. Descriptor wasn't initialized properly.");
        }

        try {
            return objectClass.newInstance();
        }
        catch (Throwable e) {
            throw new CayenneRuntimeException("Error creating object of class '"
                    + objectClass.getName()
                    + "'", e);
        }
    }

    /**
     * Invokes 'prepareForAccess' of a super descriptor and then invokes
     * 'prepareForAccess' of each declared property.
     */
    public void prepareForAccess(Object object) throws PropertyAccessException {

        // do super first
        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().prepareForAccess(object);
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            property.prepareForAccess(object);
        }
    }

    /**
     * Invokes 'copyProperties' of a super descriptor and then invokes 'copyProperty' of
     * each declared property.
     */
    public void copyProperties(Object from, Object to) throws PropertyAccessException {

        // do super first
        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().copyProperties(from, to);
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            property.copyValue(from, to);
        }
    }
}