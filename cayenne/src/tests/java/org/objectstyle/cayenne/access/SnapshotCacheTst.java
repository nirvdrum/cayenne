package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class SnapshotCacheTst extends CayenneTestCase {
    public void testConstructor() {
        SnapshotCache cache = new SnapshotCache("cacheXYZ");
        assertEquals("cacheXYZ", cache.getName());
        assertNotNull(cache.getSnapshotEventSubject());
        assertTrue(
            cache.getSnapshotEventSubject().getSubjectName().indexOf("cacheXYZ") >= 0);
    }

    public void testNotifyingObjectStores() {
        SnapshotCache cache = new SnapshotCache("cacheXYZ");

        // notifications are off by default - webapp behavior
        assertFalse(cache.isNotifyingObjectStores());

        cache.setNotifyingObjectStores(true);
        assertTrue(cache.isNotifyingObjectStores());
    }

    public void testStartReceiveingSnapshotEvents() {
        SnapshotCache cache = new SnapshotCache("cacheXYZ");

        // 1. notifications are on, listeners must be properly registered...
        cache.setNotifyingObjectStores(true);

        ObjectStore storeWithNotifications = new ObjectStore(cache);
        // successful removal of listener should signify that it was added 
        // in the first palce
        assertTrue(
            EventManager.getDefaultManager().removeListener(storeWithNotifications));

        // 2. notifications are off, listeners must not be registered...
        cache.setNotifyingObjectStores(false);

        ObjectStore storeWithoutNotifications = new ObjectStore(cache);
        // unsuccessful removal of listener should signify that it wasn't added 
        // in the first palce
        assertFalse(
            EventManager.getDefaultManager().removeListener(storeWithoutNotifications));
    }
}
