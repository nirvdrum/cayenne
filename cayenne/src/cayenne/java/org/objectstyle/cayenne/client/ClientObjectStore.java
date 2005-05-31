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
package org.objectstyle.cayenne.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;

/**
 * A helper class for ClientObjectContext that tracks object changes.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ClientObjectStore implements Serializable {

    Map dirtyObjects;

    ClientObjectStore() {
        this.dirtyObjects = new HashMap();
    }

    boolean hasChanges() {
        return !dirtyObjects.isEmpty();
    }

    /**
     * Return a subset of registered objects that are in a certian persistence state.
     * Collection is returned by copy.
     */
    synchronized Collection objectsInState(int state) {
        if (dirtyObjects.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List filteredObjects = new ArrayList();

        Iterator it = dirtyObjects.values().iterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            if (object.getPersistenceState() == state) {
                filteredObjects.add(object);
            }
        }

        return filteredObjects;
    }

    /**
     * Registers an object if it requires tracking (i.e. it was modified, inserted or
     * deleted.
     */
    synchronized void trackObject(Persistent object) {
        switch (object.getPersistenceState()) {
            case PersistenceState.NEW:
            case PersistenceState.MODIFIED:
            case PersistenceState.DELETED:
                dirtyObjects.put(object.getObjectId(), object);
                break;
        }
    }

    /**
     * Returns all registered "dirty" objects.
     */
    synchronized Collection getDirtyObjects() {
        return dirtyObjects.values();
    }

    /**
     * Called at the end of a commit cycle. Updates "dirty" objects state to reflect
     * successful commit.
     * 
     * @param ObjectIds with attached replacement ids. Usually those are for newly
     *            inserted objects, but may also include ids of modified objects.
     */
    synchronized void objectsCommitted(Collection ids) {
        // process replacement ids
        Iterator it = ids.iterator();
        while (it.hasNext()) {
            ObjectId id = (ObjectId) it.next();

            Persistent object = (Persistent) dirtyObjects.get(id);
            if (object != null) {

                // sanity check
                if (object.getPersistenceState() == PersistenceState.DELETED) {
                    throw new CayenneClientException(
                            "Recieved object id of a deleted object: " + id);
                }

                if (id.isReplacementIdAttached()) {
                    object.setObjectId(id.createReplacementId());

                    // if we processed replacement id, no need to drag it further ...
                    // just kick it out of ObjectStore...
                    dirtyObjects.remove(id);
                    object.setPersistenceState(PersistenceState.COMMITTED);
                }
            }
        }

        // process remaining objects... all must be deleted or modified..
        Iterator dirtyIt = dirtyObjects.entrySet().iterator();
        while (dirtyIt.hasNext()) {
            Map.Entry entry = (Map.Entry) dirtyIt.next();
            Persistent object = (Persistent) entry.getValue();

            switch (object.getPersistenceState()) {
                case PersistenceState.DELETED:
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.MODIFIED:
                    // sanity check
                    if (object.getObjectId().isTemporary()) {
                        throw new CayenneClientException(
                                "Modified object temporary id wasn't updated: " + object);
                    }
                    object.setPersistenceState(PersistenceState.COMMITTED);
                    break;
                // a new object wouldn't get here under normal circumstances, but as this
                // is theoretically possible, do a check as well...
                case PersistenceState.NEW:
                    if (object.getObjectId().isTemporary()) {
                        // somehow a new object didn't get an id...
                        throw new CayenneClientException(
                                "New object temporary id wasn't updated: " + object);
                    }
                    object.setPersistenceState(PersistenceState.COMMITTED);
                    break;
                default:
                    throw new CayenneClientException(
                            "Deleted or modified object expected, got: " + object);
            }
        }

        dirtyObjects.clear();
    }
}