package org.objectstyle.cayenne.access;

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
}
