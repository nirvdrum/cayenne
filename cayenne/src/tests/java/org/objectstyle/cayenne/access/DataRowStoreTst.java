package org.objectstyle.cayenne.access;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataRowStoreTst extends CayenneTestCase {

    public void testDefaultConstructor() {
        DataRowStore cache = new DataRowStore("cacheXYZ");
        assertEquals("cacheXYZ", cache.getName());
        assertNotNull(cache.getSnapshotEventSubject());
        assertTrue(
            cache.getSnapshotEventSubject().getSubjectName().indexOf("cacheXYZ") >= 0);

        assertEquals(
            DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT,
            cache.isNotifyingObjectStores());
        assertEquals(
            DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    public void testConstructorWithProperties() {
        Map props = new HashMap();
        props.put(
            DataRowStore.OBJECT_STORE_NOTIFICATION_PROPERTY,
            String.valueOf(!DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT));
        props.put(
            DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
            String.valueOf(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT));

        DataRowStore cache = new DataRowStore("cacheXYZ", props);
        assertEquals("cacheXYZ", cache.getName());

        assertEquals(
            !DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT,
            cache.isNotifyingObjectStores());
        assertEquals(
            !DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    public void testNotifyingObjectStores() {
        DataRowStore cache = new DataRowStore("cacheXYZ");

        assertEquals(
            DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT,
            cache.isNotifyingObjectStores());

        cache.setNotifyingObjectStores(!DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT);
        assertEquals(
            !DataRowStore.OBJECT_STORE_NOTIFICATION_DEFAULT,
            cache.isNotifyingObjectStores());
    }

    public void testNotifyingRemoteListeners() {
        DataRowStore cache = new DataRowStore("cacheXYZ");

        assertEquals(
            DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());

        cache.setNotifyingRemoteListeners(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT);
        assertEquals(
            !DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    public void testStartReceiveingSnapshotEvents() {
        DataRowStore cache = new DataRowStore("cacheXYZ");

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
