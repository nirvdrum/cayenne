package org.objectstyle.cayenne.access;

import java.util.HashMap;
import java.util.Map;

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
            DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    public void testConstructorWithProperties() {
        Map props = new HashMap();
        props.put(
            DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
            String.valueOf(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT));

        DataRowStore cache = new DataRowStore("cacheXYZ", props);
        assertEquals("cacheXYZ", cache.getName());
        assertEquals(
            !DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
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
}
