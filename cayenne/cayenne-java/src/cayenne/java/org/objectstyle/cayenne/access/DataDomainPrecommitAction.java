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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * An action that analyzes uncommitted objects, generates primary keys and runs
 * validation.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainPrecommitAction {

    DataDomain domain;
    DataContext context;
    Collection deleted;
    Collection inserted;
    Map insertedByEntity;
    Collection updated;

    boolean precommit(DataDomain domain, DataContext context) throws ValidationException {

        this.context = context;
        this.domain = domain;

        insertedByEntity = null;
        inserted = null;
        updated = null;
        deleted = null;

        categorizeObjects();

        if (inserted == null && deleted == null && updated == null) {
            return context.getObjectStore().checkIndirectChanges();
        }

        // create PK BEFORE analyzing phantom updates to make sure we catch propagated PK
        // changes.
        createPrimaryKey();
        clearPhantomUpdates();

        if (inserted == null && deleted == null && updated == null) {
            return context.getObjectStore().checkIndirectChanges();
        }

        validateObjects();

        return true;
    }

    private void createPrimaryKey() {
        if (inserted != null) {

            PrimaryKeyHelper pkHelper = domain.primaryKeyHelper();

            List insertedEntities = new ArrayList(insertedByEntity.keySet());

            Collections.sort(insertedEntities, pkHelper.getObjEntityComparator());
            Iterator i = insertedEntities.iterator();
            while (i.hasNext()) {
                ObjEntity currentEntity = (ObjEntity) i.next();
                List dataObjects = (List) insertedByEntity.get(currentEntity);
                pkHelper.createPermIdsForObjEntity(currentEntity, dataObjects);
            }
        }
    }

    private void clearPhantomUpdates() {
        if (updated != null) {
            Iterator it = updated.iterator();
            while (it.hasNext()) {

                DataObject object = (DataObject) it.next();
                if (checkPhantomModification(object)) {
                    it.remove();

                    object.setPersistenceState(PersistenceState.COMMITTED);

                    // not good - direct access of protected ivar of ObjectContext....
                    context.getObjectStore().retainedSnapshotMap.remove(object
                            .getObjectId());
                }
            }

            if (updated.isEmpty()) {
                updated = null;
            }
        }
    }

    private void categorizeObjects() {

        Iterator allIt = context.getObjectStore().getObjectIterator();
        while (allIt.hasNext()) {
            DataObject object = (DataObject) allIt.next();
            switch (object.getPersistenceState()) {
                case PersistenceState.NEW:
                    categorizeNew(object);
                    break;
                case PersistenceState.MODIFIED:
                    categorizeModified(object);
                    break;
                case PersistenceState.DELETED:
                    categorizeDeleted(object);
                    break;
            }
        }
    }

    private void categorizeNew(DataObject object) {
        if (inserted == null) {
            inserted = new ArrayList();
            insertedByEntity = new HashMap();
        }

        inserted.add(object);

        ObjEntity e = domain.getEntityResolver().lookupObjEntity(object);
        List list = (List) insertedByEntity.get(e);
        if (list == null) {
            list = new ArrayList();
            insertedByEntity.put(e, list);
        }

        list.add(object);
    }

    private void categorizeModified(DataObject object) {
        if (updated == null) {
            updated = new ArrayList();
        }
        updated.add(object);
    }

    private void categorizeDeleted(DataObject object) {
        if (deleted == null) {
            deleted = new ArrayList();
        }
        deleted.add(object);
    }

    private void validateObjects() {
        if (context.isValidatingObjectsOnCommit()) {
            ValidationResult validationResult = new ValidationResult();

            if (deleted != null) {
                Iterator it = deleted.iterator();
                while (it.hasNext()) {
                    DataObject dataObject = (DataObject) it.next();
                    dataObject.validateForDelete(validationResult);
                }
            }

            if (inserted != null) {
                Iterator it = inserted.iterator();
                while (it.hasNext()) {
                    DataObject dataObject = (DataObject) it.next();
                    dataObject.validateForInsert(validationResult);
                }
            }

            if (updated != null) {
                Iterator it = updated.iterator();
                while (it.hasNext()) {
                    DataObject dataObject = (DataObject) it.next();
                    dataObject.validateForUpdate(validationResult);
                }
            }

            if (validationResult.hasFailures()) {
                throw new ValidationException(validationResult);
            }
        }
    }

    private boolean checkPhantomModification(DataObject object) {

        DataContext context = object.getDataContext();
        DataRow committedSnapshot = context.getObjectStore().getSnapshot(
                object.getObjectId());
        if (committedSnapshot == null) {
            return false;
        }

        DataRow currentSnapshot = context.currentSnapshot(object);

        // treat null and absent key as two diffrent things...

        if (currentSnapshot.size() != committedSnapshot.size()) {
            return false;
        }

        Iterator currentIt = currentSnapshot.entrySet().iterator();
        while (currentIt.hasNext()) {
            Map.Entry entry = (Map.Entry) currentIt.next();
            Object newValue = entry.getValue();
            Object oldValue = committedSnapshot.get(entry.getKey());
            if (!Util.nullSafeEquals(oldValue, newValue)) {
                return false;
            }
        }

        // original snapshot can have extra keys that are missing in the
        // current snapshot; process those
        Iterator committedIt = committedSnapshot.entrySet().iterator();
        while (committedIt.hasNext()) {

            Map.Entry entry = (Map.Entry) committedIt.next();

            // committed snapshot has null value, skip it
            if (entry.getValue() == null) {
                continue;
            }

            if (!currentSnapshot.containsKey(entry.getKey())) {
                return false;
            }
        }

        return true;
    }
}
