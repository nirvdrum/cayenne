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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * Helper class that implements DataObject deletion strategy.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
class ObjectDeleteHandler {

    int oldState;
    DataObject object;
    DataContext dataContext;

    ObjectDeleteHandler(DataObject object) {
        this.object = object;
        this.dataContext = object.getDataContext();
        this.oldState = object.getPersistenceState();
    }

    /**
     * Deletes internal DataObject from its DataContext, processing delete rules.
     */
    boolean performDelete() throws DeleteDenyException {
        if (oldState == PersistenceState.DELETED
                || oldState == PersistenceState.TRANSIENT) {

            // Drop out... especially in case of DELETED we might be about to get
            // into a horrible recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return false;
        }

        // must resolve HOLLOW objects before delete... needed
        // to process relationships and optimistric locking...
        object.resolveFault();

        if (oldState == PersistenceState.NEW) {
            deleteNew();
        }
        else {
            deletePersistent();
        }

        return true;
    }

    private void deletePersistent() throws DeleteDenyException {

        // duplicating some code from ObjectStore.retainSnapshot, as we have to do it
        // in two phases, extracting snapshot here, but retaining it after the
        // successful deletion only...
        DataRow snapshot = dataContext.getObjectStore().getCachedSnapshot(object
                .getObjectId());
        if (snapshot == null) {
            snapshot = dataContext.currentSnapshot(object);
        }

        // We cannot delay setting state to deleted, as Cascade deletes might cause
        // recursion, and the "deleted" state is the best way we have of noticing that and
        // bailing out (see above)

        object.setPersistenceState(PersistenceState.DELETED);
        processDeleteRules();

        // must retain snapshot for optimistic locking to work ...
        dataContext.getObjectStore().retainSnapshot(object, snapshot);
    }

    private void deleteNew() throws DeleteDenyException {
        object.setPersistenceState(PersistenceState.TRANSIENT);
        processDeleteRules();

        // if an object was NEW, we must throw it out of the ObjectStore

        dataContext.getObjectStore().objectsUnregistered(Collections
                .singletonList(object));
        object.setDataContext(null);
    }

    private void processDeleteRules() throws DeleteDenyException {
        ObjEntity entity = dataContext.getEntityResolver().lookupObjEntity(object);
        Iterator it = entity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) it.next();

            boolean processFlattened = relationship.isFlattened()
                    && relationship.isToDependentEntity();

            // first check for no action... bail out if no flattened processing is needed
            if (relationship.getDeleteRule() == DeleteRule.NO_ACTION && !processFlattened) {
                continue;
            }

            List relatedObjects = Collections.EMPTY_LIST;
            if (relationship.isToMany()) {

                List toMany = (List) object.readNestedProperty(relationship.getName());

                if (toMany.size() > 0) {
                    // Get a copy of the list so that deleting objects doesn't
                    // result in concurrent modification exceptions
                    relatedObjects = new ArrayList(toMany);
                }
            }
            else {
                Object relatedObject = object.readNestedProperty(relationship.getName());

                if (relatedObject != null) {
                    relatedObjects = Collections.singletonList(relatedObject);
                }
            }

            // no related object, bail out
            if (relatedObjects.size() == 0) {
                continue;
            }

            // process DENY rule first...
            if (relationship.getDeleteRule() == DeleteRule.DENY) {
                object.setPersistenceState(oldState);

                String message = relatedObjects.size() == 1
                        ? "1 related object"
                        : relatedObjects.size() + " related objects";
                throw new DeleteDenyException(object, relationship, message);
            }

            // process flattened with dependent join tables...
            // joins must be removed even if they are non-existent or ignored in the
            // object graph
            if (processFlattened) {
                ObjectStore objectStore = dataContext.getObjectStore();
                Iterator iterator = relatedObjects.iterator();
                while (iterator.hasNext()) {
                    DataObject relatedObject = (DataObject) iterator.next();
                    objectStore.flattenedRelationshipUnset(object,
                            relationship,
                            relatedObject);
                }
            }

            // process remaining rules
            switch (relationship.getDeleteRule()) {
                case DeleteRule.NO_ACTION:
                    break;
                case DeleteRule.NULLIFY:
                    ObjRelationship inverseRelationship = relationship
                            .getReverseRelationship();

                    if (inverseRelationship == null) {
                        // nothing we can do here
                        break;
                    }

                    if (inverseRelationship.isToMany()) {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.removeToManyTarget(inverseRelationship
                                    .getName(), object, true);
                        }
                    }
                    else {
                        // Inverse is to-one - find all related objects and
                        // nullify the reverse relationship
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.setToOneTarget(inverseRelationship.getName(),
                                    null,
                                    true);
                        }
                    }

                    break;
                case DeleteRule.CASCADE:
                    // Delete all related objects
                    Iterator iterator = relatedObjects.iterator();
                    while (iterator.hasNext()) {
                        DataObject relatedObject = (DataObject) iterator.next();
                        new ObjectDeleteHandler(relatedObject).performDelete();
                    }

                    break;
                default:
                    object.setPersistenceState(oldState);
                    throw new CayenneRuntimeException("Invalid delete rule "
                            + relationship.getDeleteRule());
            }
        }
    }
}