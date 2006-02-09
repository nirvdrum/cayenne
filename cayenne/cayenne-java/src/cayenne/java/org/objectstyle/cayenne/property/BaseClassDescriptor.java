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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.PersistenceState;

/**
 * A superclass of Cayenne ClassDescriptors. Defines all main bean descriptor parameters
 * and operations. Subclasses would provide methods to initialize the descriptor.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class BaseClassDescriptor implements ClassDescriptor {

    static final Integer TRANSIENT_STATE = new Integer(PersistenceState.TRANSIENT);
    static final Integer HOLLOW_STATE = new Integer(PersistenceState.HOLLOW);
    static final Integer COMMITTED_STATE = new Integer(PersistenceState.COMMITTED);

    protected ClassDescriptor superclassDescriptor;

    // compiled properties ...
    protected Class objectClass;
    protected Map declaredProperties;
    protected PropertyAccessor objectIdProperty;
    protected PropertyAccessor objectContextProperty;
    protected PropertyAccessor persistenceStateProperty;
    protected Map subclassDescriptors;

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
        return objectClass != null
                && declaredProperties != null
                && objectIdProperty != null
                && objectContextProperty != null
                && persistenceStateProperty != null;
    }

    public Class getObjectClass() {
        return objectClass;
    }

    public ClassDescriptor resolveDescriptor(Class objectClass) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        if (subclassDescriptors == null) {
            return this;
        }

        ClassDescriptor subclassDescriptor = (ClassDescriptor) subclassDescriptors
                .get(objectClass.getName());

        // ascend via the class hierarchy (only doing it if there are multiple choices)
        if (subclassDescriptor == null) {
            Class currentClass = objectClass;
            while (subclassDescriptor == null
                    && (currentClass = currentClass.getSuperclass()) != null) {
                subclassDescriptor = (ClassDescriptor) subclassDescriptors
                        .get(currentClass.getName());
            }
        }

        return subclassDescriptor != null ? subclassDescriptor : this;
    }

    public Iterator getProperties() {
        Iterator declaredIt = IteratorUtils.unmodifiableIterator(declaredProperties
                .values()
                .iterator());

        if (getSuperclassDescriptor() == null) {
            return declaredIt;
        }
        else {
            return IteratorUtils.chainedIterator(
                    superclassDescriptor.getProperties(),
                    declaredIt);
        }
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

    public PropertyAccessor getObjectContextProperty() {
        return objectContextProperty;
    }

    public PropertyAccessor getObjectIdProperty() {
        return objectIdProperty;
    }

    public PropertyAccessor getPersistenceStateProperty() {
        return persistenceStateProperty;
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

    protected Object readObjectId(Object object) {
        return objectIdProperty.readPropertyDirectly(object);
    }

    /**
     * Creates a persistent object, initializaing it with an id and context (but not
     * registering it in the context).
     */
    protected Object makePersistentObject(ObjectContext context, Object id) {
        Object object = createObject();

        objectIdProperty.writePropertyDirectly(object, null, id);

        if (context != null) {
            objectContextProperty.writePropertyDirectly(object, null, context);
            persistenceStateProperty.writePropertyDirectly(object, null, HOLLOW_STATE);
            context.getGraphManager().registerNode(id, object);
        }
        else {
            persistenceStateProperty.writePropertyDirectly(object, null, TRANSIENT_STATE);
        }

        prepareForAccess(object);
        return object;
    }

    protected void writeObjectId(Object object, Object id) {
        objectIdProperty.writePropertyDirectly(object, null, id);
    }

    /**
     * Copies object properties from one object to another. Invokes 'shallowCopy' of a
     * super descriptor and then invokes 'shallowCopy' of each declared property.
     */
    public void shallowMerge(Object from, Object to) throws PropertyAccessException {

        // do super first
        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().shallowMerge(from, to);
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            property.shallowMerge(from, to);
        }
    }

    public Object deepMerge(Object object, ObjectGraphVisitor visitor)
            throws PropertyAccessException {

        Object id = readObjectId(object);
        if (id == null) {
            throw new PropertyAccessException(
                    "Object without a defined id can't be merged",
                    objectIdProperty,
                    object);
        }

        // object presence in the mergeMap indicates that it requires no further
        // processing
        Object targetObject = visitor.getVisitedObject(id);
        if (targetObject != null) {
            return targetObject;
        }

        if (visitor.getContext() != null) {
            targetObject = visitor.getContext().getGraphManager().getNode(id);
        }

        if (targetObject == null) {
            // TODO: (Andrus, 2/1/2006) note that here we always create a copy of an
            // object. This creates a major inefficiency when unattached objects are
            // deserialized from the remote channel, but this is needed for consistency
            // ... I can't see a better *generic* way of handling this issue, but maybe
            // when no prefetches are involved, we can take a shortcut and register an
            // object without copying?
            targetObject = makePersistentObject(visitor.getContext(), id);
        }

        visitor.objectVisited(id, targetObject);

        deepPropertyMerge(object, targetObject, visitor);
        return targetObject;
    }

    public void deepPropertyMerge(Object from, Object to, ObjectGraphVisitor visitor) {

        int state = ((Number) persistenceStateProperty.readPropertyDirectly(to))
                .intValue();

        // we can't override resolved properties of this object if it is dirty and by
        // 'deepMerge' operation definition we shouldn't touch faults ... hence, get out
        if (state == PersistenceState.NEW
                || state == PersistenceState.MODIFIED
                || state == PersistenceState.DELETED) {
            return;
        }

        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().deepPropertyMerge(from, to, visitor);
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            property.deepMerge(from, to, visitor);
        }

        if (state == PersistenceState.HOLLOW) {
            persistenceStateProperty.writePropertyDirectly(
                    to,
                    new Integer(state),
                    COMMITTED_STATE);
        }
    }

    public boolean visitProperties(PropertyVisitor visitor) {
        if (superclassDescriptor != null
                && !superclassDescriptor.visitProperties(visitor)) {
            return false;
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property next = (Property) it.next();
            if (!next.visit(visitor)) {
                return false;
            }
        }

        return true;
    }
}
