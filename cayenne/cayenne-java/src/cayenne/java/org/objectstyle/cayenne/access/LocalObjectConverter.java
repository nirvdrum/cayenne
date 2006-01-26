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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.property.ClassDescriptor;

/**
 * A helper class for transferring parts of object graphs between ObjectContexts. Exactly
 * which parts of an graph need to be transferred is defined by the PrefetchTreeNode
 * object passed in constructor.
 * <p>
 * Note that if source objects are not registered with an ObjectContext, they will be
 * automatically pulled in the target context. This is useful when a target ObjectContext
 * is attached to a channel that is not a context itself.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: support prefetches...
class LocalObjectConverter {

    List targetObjects;
    ObjectContext targetContext;

    LocalObjectConverter(List objects, ObjectContext targetContext) {

        this.targetObjects = new ArrayList(objects.size());
        this.targetContext = targetContext;

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Persistent sourceObject = (Persistent) it.next();
            targetObjects.add(targetObject(sourceObject));
        }
    }

    /**
     * Returns a list of the objects transferred to the target context.
     */
    List getTargetObjects() {
        return targetObjects;
    }

    Object targetObject(Persistent sourceObject) {
        ObjectId id = sourceObject.getObjectId();

        // sanity check
        if (id == null) {
            throw new CayenneRuntimeException("An object has no ObjectId: "
                    + sourceObject);
        }

        GraphManager graphManager = targetContext.getGraphManager();

        // note that on-the-spot ClassDescriptor lookup below is needed as even if all
        // objects where fetched as a part of the same query, they may belong to
        // different subclasses

        Persistent cachedObject = (Persistent) graphManager.getNode(id);

        // 1. use cached object
        if (cachedObject != null) {

            // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
            if (cachedObject.getPersistenceState() != PersistenceState.MODIFIED
                    && cachedObject.getPersistenceState() != PersistenceState.DELETED) {

                ClassDescriptor descriptor = getClassDescriptor(id);
                descriptor.prepareForAccess(cachedObject);

                // TODO: Andrus, 1/24/2006 - this operation causes an unexpected fetch
                // on one-to-one relationship - investigate....
                descriptor.copyProperties(sourceObject, cachedObject);
            }
            return cachedObject;
        }
        // 2. use source as a target
        // 'null' ObjectContext can happen when the objects are fetched from the
        // channel that is not an ObjectContext
        else if (sourceObject.getObjectContext() == null
                || sourceObject.getObjectContext() == targetContext) {

            sourceObject.setPersistenceState(PersistenceState.COMMITTED);
            sourceObject.setObjectContext(targetContext);
            graphManager.registerNode(id, sourceObject);
            getClassDescriptor(id).prepareForAccess(sourceObject);
            return sourceObject;
        }
        // 3. create a copy of the source
        else {

            ClassDescriptor descriptor = getClassDescriptor(id);
            Persistent targetObject = (Persistent) descriptor.createObject();

            targetObject.setPersistenceState(PersistenceState.COMMITTED);
            targetObject.setObjectContext(targetContext);
            targetObject.setObjectId(id);
            graphManager.registerNode(id, targetObject);

            descriptor.prepareForAccess(targetObject);
            descriptor.copyProperties(sourceObject, targetObject);
            return targetObject;
        }
    }

    // ****** helper methods ******

    final ClassDescriptor getClassDescriptor(ObjectId id) {
        return targetContext
                .getEntityResolver()
                .lookupObjEntity(id.getEntityName())
                .getClassDescriptor();
    }
}
