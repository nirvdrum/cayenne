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
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.event.EventBridgeFactory;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;
import org.shiftone.cache.Cache;
import org.shiftone.cache.lru.LruCacheFactory;

/**
 * Represents a fixed size cache of DataRows keyed by ObjectId. 
 * 
 * <p><strong>Synchronization Note:</strong> DataRowStore synchronizes 
 * most operations on its own instance.</p>
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class DataRowStore implements Serializable {
    private static Logger logObj = Logger.getLogger(DataRowStore.class);

    // property keys
    public static final String SNAPSHOT_EXPIRATION_PROPERTY =
        "cayenne.DataRowStore.snapshot.expiration";
    public static final String SNAPSHOT_CACHE_SIZE_PROPERTY =
        "cayenne.DataRowStore.snapshot.size";
    public static final String OBJECT_STORE_NOTIFICATION_PROPERTY =
        "cayenne.DataRowStore.ObjectStore.notify";
    public static final String REMOTE_NOTIFICATION_PROPERTY =
        "cayenne.DataRowStore.remote.notify";
    public static final String EVENT_BRIDGE_FACTORY_PROPERTY =
        "cayenne.DataRowStore.EventBridge.factory";

    // default property values

    // default expiration time is 2 hours
    public static final long SNAPSHOT_EXPIRATION_DEFAULT = 2 * 60 * 60;
    public static final int SNAPSHOT_CACHE_SIZE_DEFAULT = 10000;
    public static final boolean OBJECT_STORE_NOTIFICATION_DEFAULT = true;
    public static final boolean REMOTE_NOTIFICATION_DEFAULT = false;

    // use String for class name, since JavaGroups may not be around,
    // causing CNF exceptions
    public static final String EVENT_BRIDGE_FACTORY_DEFAULT =
        "org.objectstyle.cayenne.event.JavaGroupsBridgeFactory";

    protected String name;
    protected Cache snapshots;
    protected boolean notifyingObjectStores;
    protected boolean notifyingRemoteListeners;
    protected EventBridge remoteNotificationsHandler;

    /**
     * Creates new named DataRowStore with default configuration.
     */
    public DataRowStore(String name) {
        this(name, Collections.EMPTY_MAP);
    }

    /**
     * Creates new DataRowStore with a specified name and a set of properties. If no properties
     * are defined, default values are used.
     * 
     * @param name DataRowStore name. Used to idenitfy this DataRowStore in events, etc. Can't be null.
     * @param properties Properties map used to configure DataRowStore parameters. Can be null.
     */
    public DataRowStore(String name, Map properties) {
        if (name == null) {
            throw new IllegalArgumentException("DataRowStore name can't be null.");
        }

        this.name = name;
        initFromProperties(properties);
    }

    protected void initFromProperties(Map properties) {
        ExtendedProperties propertiesWrapper = new ExtendedProperties();

        if (properties != null) {
            propertiesWrapper.putAll(properties);
        }

        long snapshotsExpiration =
            propertiesWrapper.getLong(
                SNAPSHOT_EXPIRATION_PROPERTY,
                SNAPSHOT_EXPIRATION_DEFAULT);

        int snapshotsCacheSize =
            propertiesWrapper.getInt(
                SNAPSHOT_CACHE_SIZE_PROPERTY,
                SNAPSHOT_CACHE_SIZE_DEFAULT);

        boolean notifyRemote =
            propertiesWrapper.getBoolean(
                REMOTE_NOTIFICATION_PROPERTY,
                REMOTE_NOTIFICATION_DEFAULT);

        boolean notifyObjectStores =
            propertiesWrapper.getBoolean(
                OBJECT_STORE_NOTIFICATION_PROPERTY,
                OBJECT_STORE_NOTIFICATION_DEFAULT);

        String eventBridgeFactory =
            propertiesWrapper.getString(
                EVENT_BRIDGE_FACTORY_PROPERTY,
                EVENT_BRIDGE_FACTORY_DEFAULT);

        if (logObj.isDebugEnabled()) {
            logObj.debug(
                "DataRowStore property "
                    + SNAPSHOT_EXPIRATION_PROPERTY
                    + " = "
                    + snapshotsExpiration);
            logObj.debug(
                "DataRowStore property "
                    + SNAPSHOT_CACHE_SIZE_PROPERTY
                    + " = "
                    + snapshotsCacheSize);
            logObj.debug(
                "DataRowStore property "
                    + REMOTE_NOTIFICATION_PROPERTY
                    + " = "
                    + notifyRemote);
            logObj.debug(
                "DataRowStore property "
                    + OBJECT_STORE_NOTIFICATION_PROPERTY
                    + " = "
                    + notifyObjectStores);
            logObj.debug(
                "DataRowStore property "
                    + EVENT_BRIDGE_FACTORY_PROPERTY
                    + " = "
                    + eventBridgeFactory);
        }

        // init ivars from properties
        this.notifyingRemoteListeners = notifyRemote;
        this.notifyingObjectStores = notifyObjectStores;
        this.snapshots =
            new LruCacheFactory().newInstance(
                snapshotsExpiration * 1000,
                snapshotsCacheSize);

        // init event bridge only if we are notifying remote listeners
        if (notifyingRemoteListeners) {
            try {
                EventBridgeFactory factory =
                    (EventBridgeFactory) Class.forName(eventBridgeFactory).newInstance();
                this.remoteNotificationsHandler =
                    factory.createEventBridge(getSnapshotEventSubject(), properties);
                remoteNotificationsHandler.startup(
                    EventManager.getDefaultManager(),
                    EventBridge.RECEIVE_LOCAL_EXTERNAL);
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error initializing DataRowStore.", ex);
            }
        }
    }

    /**
     * Shuts down any remote notification connections, and clears internal cache.
     */
    public void shutdown() {
        if (remoteNotificationsHandler != null) {
            try {
                remoteNotificationsHandler.shutdown();
            }
            catch (Exception ex) {
                logObj.info("Exception shutting down EventBridge.", ex);
            }
            remoteNotificationsHandler = null;
        }

        clear();
    }

    /**
     * Returns the name of this SnapshotCache. Name allows to create
     * EventSubjects for event notifications addressed to or sent from this
     * SnapshotCache.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns cached snapshot or null if no snapshot is currently cached for
     * the given ObjectId.
     */
    public synchronized DataRow getCachedSnapshot(ObjectId oid) {
        return (DataRow) snapshots.getObject(oid);
    }

    /**
     * Returns a snapshot for ObjectId. If snapshot is currently cached, it is
     * returned. If not, a provided QueryEngine is used to fetch it from the
     * database. If there is no database row for a given id, null is returned.
     */
    public synchronized DataRow getSnapshot(ObjectId oid, QueryEngine engine) {

        // try cache
        DataRow cachedSnapshot = getCachedSnapshot(oid);
        if (cachedSnapshot != null) {
            return cachedSnapshot;
        }

        // try getting it from database
        SelectQuery select = QueryUtils.selectObjectForId(oid);
        SelectObserver observer = new SelectObserver();
        engine.performQueries(Collections.singletonList(select), observer);
        List results = observer.getResults(select);

        if (results.size() > 1) {
            throw new CayenneRuntimeException(
                "More than 1 object found for ObjectId "
                    + oid
                    + ". Fetch matched "
                    + results.size()
                    + " objects.");
        }
        else if (results.size() == 0) {
            return null;
        }
        else {
            DataRow snapshot = (DataRow) results.get(0);
            snapshots.addObject(oid, snapshot);
            return snapshot;
        }
    }

    /**
     * Returns EventSubject used by this SnapshotCache to notify of snapshot
     * changes.
     */
    public EventSubject getSnapshotEventSubject() {
        return EventSubject.getSubject(this.getClass(), name);
    }

    /**
     * Unregisters an ObjectStore to stop receiving SnapshotChangeEvents.
     */
    public void stopReceivingSnapshotEvents(ObjectStore objectStore) {
        EventManager.getDefaultManager().removeListener(objectStore);
    }

    /**
     * Registers an ObjectStore to receive SnapshotChangeEvents. If <code>notifyingObjectStores</code>
     * property is false, this method skips the registration.
     */
    public boolean startReceivingSnapshotEvents(ObjectStore objectStore) {
        if (!isNotifyingObjectStores()) {
            return false;
        }

        EventManager.getDefaultManager().addListener(
            objectStore,
            "snapshotsChanged",
            SnapshotEvent.class,
            getSnapshotEventSubject(),
            this);
        logObj.debug("ObjectStore will listen for events: " + objectStore);
        return true;
    }

    /**
     * Expires and removes all stored snapshots without sending any
     * notification events.
     */
    public synchronized void clear() {
        snapshots.clear();
    }

    /**
     * Evicts a snapshot from cache without generating any SnapshotEvents.
     */
    public synchronized void forgetSnapshot(ObjectId id) {
        snapshots.remove(id);
    }

    /**
     * Processes changes made to snapshots. Modifies internal cache state, and
     * then sends the event to all listeners.
     */
    public void processSnapshotChanges(
        Object source,
        Map updatedSnapshots,
        Collection deletedSnapshotIds) {

        // update the internal cache, prepare snapshot event
        Map diffs = null;

        if (deletedSnapshotIds.isEmpty() && updatedSnapshots.isEmpty()) {
            logObj.warn("postSnapshotsChangeEvent.. bogus call... no changes.");
            return;
        }

        synchronized (this) {

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

                    ObjectId key = (ObjectId) it.next();
                    DataRow newSnapshot = (DataRow) updatedSnapshots.get(key);
                    DataRow oldSnapshot = (DataRow) snapshots.remove(key);

                    // generate diff for the updated event, if this not a new
                    // snapshot

                    // The following cases should be handled here:

                    // 1. There is no previously cached snapshot for a given id.
                    // 2. There was a previously cached snapshot for a given id,
                    //    but it expired from cache and was removed. Currently
                    //    handled as (1); what are the consequences of that?
                    // 3. There is a previously cached snapshot and it has the
                    //    *same version* as the "replacesVersion" property of the
                    //    new snapshot.
                    // 4. There is a previously cached snapshot and it has a
                    //    *different version* from "replacesVersion" property of
                    //    the new snapshot. It means that we don't know how to merge
                    //    the two (we don't even know which one is newer due to
                    //    multithreading). Just throw out this snapshot....

                    if (oldSnapshot != null) {
                        // case 4 above... have to throw out the snapshot since
                        // no good options exist to tell how to merge the two.
                        if (oldSnapshot.getVersion()
                            != newSnapshot.getReplacesVersion()) {
                            forgetSnapshot(key);
                            continue;
                        }

                        Map diff = buildSnapshotDiff(oldSnapshot, newSnapshot);
                        if (diff != null) {
                            if (diffs == null) {
                                diffs = new HashMap();
                            }

                            diffs.put(key, diff);
                        }
                    }

                    snapshots.addObject(key, newSnapshot);
                }
            }

        }

        // do not send bogus events... e.g. inserted objects are not counted
        if ((diffs != null && !diffs.isEmpty())
            || (deletedSnapshotIds != null && !deletedSnapshotIds.isEmpty())) {
            SnapshotEvent event =
                new SnapshotEvent(this, source, diffs, deletedSnapshotIds);
            if (logObj.isDebugEnabled()) {
                logObj.debug("postSnapshotsChangeEvent: " + event);
            }

            // notify listeners

            // IMPORTANT: do this asynchronously, since there is a
            // good chance of deadlocking when notifying object stores
            // that are themselves waiting to obtain the lock on this DataRowStore,
            // and are already being locked by calling threads

            // downside of asynchronous post is ambiguity on the receiving end...
            EventManager.getDefaultManager().postNonBlockingEvent(
                event,
                getSnapshotEventSubject());
        }
    }

    /**
     * Creates a map that contains only the keys that have values that differ
     * in the registered snapshot for a given ObjectId and a given snapshot. It
     * is assumed that key sets are the same in both snapshots (since they
     * should represent the same entity data). Returns null if no
     * differences are found.
     */
    protected DataRow buildSnapshotDiff(DataRow oldSnapshot, DataRow newSnapshot) {
        if (oldSnapshot == null) {
            return newSnapshot;
        }

        // build a diff...
        DataRow diff = null;

        Iterator keys = oldSnapshot.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            Object oldValue = oldSnapshot.get(key);
            Object newValue = newSnapshot.get(key);
            if (!Util.nullSafeEquals(oldValue, newValue)) {
                if (diff == null) {
                    diff = new DataRow(10);
                }
                diff.put(key, newValue);
            }
        }

        return diff;
    }

    public boolean isNotifyingRemoteListeners() {
        return notifyingRemoteListeners;
    }

    public void setNotifyingRemoteListeners(boolean notifyingRemoteListeners) {
        this.notifyingRemoteListeners = notifyingRemoteListeners;
    }

    /**
     * Returns a property that defines whether child ObjectStores are allowed
     * to register as SnapshotEventListeners. SnapshotEvents are still posted
     * via EventManager, even if this value is false. Rather this setting has
     * effect on {@link #startReceivingSnapshotEvents(ObjectStore) 
     * startReceivingSnapshotEvents(ObjectStore)} behavior.
     */
    public boolean isNotifyingObjectStores() {
        return notifyingObjectStores;
    }

    /**
     * Sets a property that defines whether child ObjectStores are allowed to
     * register as SnapshotEventListeners via
     * {@link #startReceivingSnapshotEvents(ObjectStore) 
     * startReceivingSnapshotEvents(ObjectStore)}
     */
    public void setNotifyingObjectStores(boolean b) {
        notifyingObjectStores = b;
    }
}
