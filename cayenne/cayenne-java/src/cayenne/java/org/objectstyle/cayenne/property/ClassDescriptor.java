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

import java.io.Serializable;
import java.util.Collection;

import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.graph.GraphManager;

/**
 * Provides access to a set of persistent properties of a Java Bean and methods for
 * manipulating such bean.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface ClassDescriptor extends Serializable {

    /**
     * Returns a bean class mapped by this descriptor.
     */
    Class getObjectClass();

    /**
     * Returns a descriptor of the mapped superclass or null if the descriptor's entity
     * sits at the top of inheritance hierarchy or no inheritance is mapped.
     */
    ClassDescriptor getSuperclassDescriptor();

    /**
     * Creates a new instance of a class described by this object.
     */
    Object createObject();

    /**
     * Prepares object properties for access. This may include injection of value holders
     * into the object and such.
     */
    void prepareForAccess(Object object) throws PropertyAccessException;

    /**
     * Merges object properties from one object to another, avoiding traversal of the
     * ArcProperties.
     */
    void shallowMerge(Object from, Object to) throws PropertyAccessException;

    /**
     * Takes an object and merges the provided ObjectContext. Merge operation is cascaded
     * to all accessible (i.e. resolved) related objects. Returns a merged instance
     * belonging to the provided context.
     * 
     * @param context ObjectContext to merge the object to.
     * @param object Object to merge
     * @param mergeMap an object map that provides a "context" of this operation, ensuring
     *            object uniquing. All ClassDescriptors and properties participating in
     *            the merge must register resolved objects in the mergeMap.
     */
    Object deepMerge(ObjectContext context, Object object, GraphManager mergeMap)
            throws PropertyAccessException;

    /**
     * A method similar to {@link #deepMerge(ObjectContext, Object, GraphManager)} that
     * merges object properties from one object to another known object. This method is
     * normally used internally by 'deepMerge' implementors to cascade property merging to
     * related objects.
     */
    void deepPropertyMerge(
            ObjectContext context,
            Object from,
            Object to,
            GraphManager mergeMap);

    /**
     * Returns a Java Bean property descriptor matching property name or null if no such
     * property is found. Lookup includes properties from this descriptor and all its
     * superclass decsriptors. Returned property maybe any one of simple, value holder or
     * collection properties.
     */
    Property getProperty(String propertyName);

    /**
     * Returns a Java Bean property descriptor matching property name or null if no such
     * property is found. Lookup DOES NOT including properties from the superclass
     * decsriptors. Returned property maybe any one of simple, value holder or collection
     * properties.
     */
    Property getDeclaredProperty(String propertyName);

    /**
     * Returns all property names mapped in this descriptor, not including properties from
     * the superclass decsriptors.
     */
    Collection getDeclaredPropertyNames();

    /**
     * Returns all property names mapped in this descriptor and all its superdescriptors.
     */
    Collection getPropertyNames();
}
