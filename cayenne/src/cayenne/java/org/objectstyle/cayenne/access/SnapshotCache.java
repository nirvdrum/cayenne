/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.access;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.event.SnapshotEventListener;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.util.Util;
import org.shiftone.cache.Cache;
import org.shiftone.cache.CacheManager;

/**
 * @author Andrei Adamchik
 */
public class SnapshotCache implements Serializable {
    private static Logger logObj = Logger.getLogger(SnapshotCache.class);

    protected String name;
    protected Cache snapshots;

    /**
     * Creates new SnapshotCache, assigning it a specified name.
     */
    public SnapshotCache(String name) {
        if (name == null) {
            throw new IllegalArgumentException("SnapshotCache name can't be null.");
        }

        this.name = name;

        // TODO: these values will be configurable
        this.snapshots = CacheManager.getInstance().newCache(12 * 60 * 60 * 1000, 10000);
    }

    /**
     * Returns the name of this SnapshotCache. Name allows to create
     * EventSubjects for event notifications addressed to or sent from 
     * this SnapshotCache.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map getSnapshot(ObjectId oid) {
        return (Map) snapshots.getObject(oid);
    }

    /**
     * Returns EventSubject used by this SnapshotCache to notify of snapshot changes.
     */
    public EventSubject getSnapshotEventSubject() {
        return EventSubject.getSubject(this.getClass(), name);
    }

    protected void unregisterSnapshotEventListener(SnapshotEventListener listener) {
        EventManager.getDefaultManager().removeListener(listener);
    }

    protected void registerSnapshotEventListener(SnapshotEventListener listener) {
        try {
            EventManager.getDefaultManager().addListener(
                listener,
                "snapshotsChanged",
                SnapshotEvent.class,
                getSnapshotEventSubject(),
                this);
        }
        catch (NoSuchMethodException e) {
            logObj.warn("Error adding listener.", e);
            throw new CayenneRuntimeException("Error adding listener.", e);
        }
    }

    /**
     * Expires and removes all stored snapshots without sending any notification events.
     */
    public void clear() {
        snapshots.clear();
    }

    /**
     * Evicts a snapshot from cache without generating any SnapshotEvents.
     */
    public void forgetSnapshot(ObjectId id) {
        snapshots.remove(id);
    }

    /**
     * Processes a SnapshotEvent. Modifies internal cache state, and then
     * sends the event to all listeners. Outgoing event will have a source 
     * set ot this SnapshotCache.
     */
    public void processSnapshotChanges(
        Object source,
        Map updatedSnapshots,
        Collection deletedSnapshotIds) {

        // update the internal cache, prepare snapshot event
        Map diffs = null;

        // DELETED: evict deleted snapshots
        if (!deletedSnapshotIds.isEmpty()) {
            Iterator it = deletedSnapshotIds.iterator();
            while (it.hasNext()) {
                snapshots.remove(it.next());
            }
        }

        // MODIFIED: replace/add snapshots, generate diffs for event
        if (!updatedSnapshots.isEmpty()) {
            Iterator it = updatedSnapshots.keySet().iterator();
            while (it.hasNext()) {

                Object key = it.next();
                Map newSnapshot = (Map) updatedSnapshots.get(key);
                Map oldSnapshot = (Map) snapshots.remove(key);

                // generate diff for the updated event, if this not a new snapshot

                // TODO: added snapshots may be simply the newer copies of
                // previously expired snapshots... this means that
                // we also might send notifications for added ...
                // but sending notifications for truly new or recently fetched 
                // objects will result in serious performance degradation...

                // so we need to keep track of expired ids somehow...

                if (oldSnapshot != null) {
                    Map diff = buildSnapshotDiff((ObjectId) key, newSnapshot);
                    if (!diff.isEmpty()) {
                        if (diffs == null) {
                            diffs = new HashMap();
                        }

                        diffs.put(key, diff);
                    }
                }

                snapshots.addObject(key, newSnapshot);
            }
        }

        SnapshotEvent event =
            SnapshotEvent.createEvent(source, diffs, deletedSnapshotIds);
        if (logObj.isDebugEnabled()) {
            logObj.debug("postSnapshotsChangeEvent: " + event);
        }

        // now notify children;
        // create a chained event so that its source is SnapshotCache.
        EventManager.getDefaultManager().postEvent(
            SnapshotEvent.createEvent(this, event),
			getSnapshotEventSubject());
    }

    /**
      * Creates a map that contains only the keys that have values
      * that differ in the registered snapshot for a given ObjectId and
      * a given snapshot. It is assumed that key sets are the same in 
      * both snapshots (since they should represent the same entity data).
      * Returns an empty map if no differences are found.
      */
    protected Map buildSnapshotDiff(ObjectId oid, Map newSnapshot) {
        Map oldSnapshot = getSnapshot(oid);

        if (oldSnapshot == null) {
            return newSnapshot;
        }

        // build a diff...
        Map diff = null;

        Iterator keys = oldSnapshot.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            Object oldValue = oldSnapshot.get(key);
            Object newValue = newSnapshot.get(key);
            if (!Util.nullSafeEquals(oldValue, newValue)) {
                if (diff == null) {
                    diff = new HashMap();
                }
                diff.put(key, newValue);
            }
        }

        return (diff != null) ? diff : Collections.EMPTY_MAP;
    }
}
