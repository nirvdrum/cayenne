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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.property.BaseClassDescriptor;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.FieldProperty;
import org.objectstyle.cayenne.property.ListProperty;
import org.objectstyle.cayenne.property.ValueHolderProperty;

/**
 * A ClassDescriptor describing a persistent bean based on ObjEntity.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EntityDescriptor extends BaseClassDescriptor {

    protected ObjEntity entity;

    /**
     * Creates and compiles a class descriptor for a given entity. A second optional
     * 'superclassDescriptor' parameter should be used if an entity has a super-entity.
     */
    public EntityDescriptor(ObjEntity entity, ClassDescriptor superclassDescriptor) {
        super(superclassDescriptor);
        this.entity = entity;

        compile();
    }

    /**
     * Returns ObjEntity described by this object.
     */
    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Prepares the descriptor. A descriptor must be compiled before it can operate.
     * "compile" is internally called in constructor and also after deserialization.
     */
    protected void compile() {
        if (entity == null) {
            throw new IllegalStateException(
                    "Entity is not initialized, can't index descriptor.");
        }

        // init class
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        this.objectClass = entity.getJavaClass(loader);

        // init property descriptors...
        Map allDescriptors = new HashMap();
        compileAttributes(loader, allDescriptors);
        compileRelationships(loader, allDescriptors);

        this.declaredProperties = allDescriptors;
        this.declaredPropertiesRef = Collections.unmodifiableMap(declaredProperties);
    }

    /**
     * Implements an attributes compilation step. Called internally from "compile".
     */
    protected void compileAttributes(ClassLoader loader, Map allDescriptors) {

        // only include this entity attributes and skip superclasses...
        Iterator it = entity.getDeclaredAttributes().iterator();
        while (it.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) it.next();

            FieldProperty property = new FieldProperty(
                    objectClass,
                    attribute.getName(),
                    attribute.getJavaClass(loader));
            allDescriptors.put(attribute.getName(), property);
        }
    }

    /**
     * Implements a relationships compilation step. Maps each to-one relationship to a
     * ValueHolderProperty and to-many - to CollectionProperty. Called internally from
     * "compile" method.
     */
    protected void compileRelationships(ClassLoader loader, Map allDescriptors) {

        // only include this entity relationships and skip superclasses...
        Iterator it = entity.getDeclaredRelationships().iterator();
        while (it.hasNext()) {

            ObjRelationship relationship = (ObjRelationship) it.next();

            if (relationship.isToMany()) {
                ListProperty property = new ListProperty(objectClass, relationship
                        .getName());
                allDescriptors.put(relationship.getName(), property);
            }
            else {
                ValueHolderProperty property = new ValueHolderProperty(
                        objectClass,
                        relationship.getName());
                allDescriptors.put(relationship.getName(), property);
            }
        }
    }

    /**
     * Overrides toString method of Object to provide a meaningful description.
     */
    public String toString() {
        String entityName = (entity != null) ? entity.getName() : null;
        String className = (objectClass != null) ? objectClass.getName() : null;
        return new ToStringBuilder(this).append("entity", entityName).append(
                "objectClass",
                className).toString();
    }

    /**
     * Deserialization method that recompiles the descriptor.
     */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();
        compile();
    }
}
