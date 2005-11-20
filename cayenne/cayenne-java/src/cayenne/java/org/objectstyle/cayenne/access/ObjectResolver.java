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
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;

/**
 * DataRows-to-objects converter for a specific ObjEntity.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectResolver {

    DataContext context;
    ObjEntity entity;
    EntityInheritanceTree inheritanceTree;
    boolean refreshObjects;
    boolean resolveInheritance;

    ObjectResolver(DataContext context, GenericSelectQuery query) {
        this(context, context.getEntityResolver().lookupObjEntity(query), query);
    }

    ObjectResolver(DataContext context, ObjEntity entity, GenericSelectQuery query) {
        this(context, entity, query.isRefreshingObjects(), query.isResolvingInherited());
    }

    ObjectResolver(DataContext context, ObjEntity entity, boolean refresh,
            boolean resolveInheritanceHierarchy) {

        // sanity check
        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("ObjEntity '"
                    + entity.getName()
                    + "' has no DbEntity.");
        }

        if (dbEntity.getPrimaryKey().size() == 0) {
            throw new CayenneRuntimeException("Won't be able to create ObjectId for '"
                    + entity.getName()
                    + "'. Reason: DbEntity '"
                    + dbEntity.getName()
                    + "' has no Primary Key defined.");
        }

        this.context = context;
        this.refreshObjects = refresh;
        this.entity = entity;
        this.inheritanceTree = context.getEntityResolver().lookupInheritanceTree(entity);
        this.resolveInheritance = (inheritanceTree != null)
                ? resolveInheritanceHierarchy
                : false;
    }

    /**
     * Processes a list of rows. This method does all needed internal snchronization and
     * object store updates.
     */
    List objectsFromDataRows(List rows) {
        if (rows == null || rows.size() == 0) {
            return new ArrayList(1);
        }

        List results = new ArrayList(rows.size());

        Iterator it = rows.iterator();

        // must do double sync...
        synchronized (context.getObjectStore()) {
            synchronized (context.getObjectStore().getDataRowCache()) {
                while (it.hasNext()) {
                    results.add(objectFromDataRow((DataRow) it.next()));
                }

                // now deal with snapshots
                context.getObjectStore().snapshotsUpdatedForObjects(
                        results,
                        rows,
                        refreshObjects);
            }
        }

        return results;
    }

    /**
     * Processes a single row. This method does not synchronize on ObjectStore and doesn't
     * send snapshot updates. These are resposnibilities of the caller.
     */
    Object objectFromDataRow(DataRow row) {

        // determine entity to use
        ObjEntity objectEntity = entity;

        if (resolveInheritance) {
            objectEntity = inheritanceTree.entityMatchingRow(row);

            // still null.... looks like inheritance qualifiers are messed up
            if (objectEntity == null) {
                objectEntity = entity;
            }
        }

        ObjectId anId = row.createObjectId(objectEntity);

        // this will create a HOLLOW object if it is not registered yet
        DataObject object = context.registeredObject(anId);

        // deal with object state
        int state = object.getPersistenceState();
        switch (state) {
            case PersistenceState.COMMITTED:
            case PersistenceState.MODIFIED:
            case PersistenceState.DELETED:
                // process the above only if refresh is requested...
                if (!refreshObjects) {
                    break;
                }
            case PersistenceState.HOLLOW:
                DataRowUtils.mergeObjectWithSnapshot(objectEntity, object, row);
            default:
                break;
        }

        object.setSnapshotVersion(row.getVersion());
        object.fetchFinished();
        return object;
    }

    ObjEntity getEntity() {
        return entity;
    }
}
