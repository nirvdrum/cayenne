package org.objectstyle.cayenne.event;

import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class EventBridgeTst extends CayenneTestCase {

    public void testInstall() throws Exception {
        TestBridge bridge = new TestBridge();
        EventSubject subject =
            EventSubject.getSubject(EventBridgeTst.class, "testInstall");

        try {
            bridge.install(subject, "testInstall:externalSubject");

            assertEquals(subject, bridge.getLocalSubject());
            assertEquals("testInstall:externalSubject", bridge.getExternalSubject());
        }
        finally {
            bridge.uninstall();
        }
    }

    public void testLocalEvents() throws Exception {
        TestBridge bridge = new TestBridge();
        EventSubject subject =
            EventSubject.getSubject(EventBridgeTst.class, "testInstall");

        try {
            bridge.install(subject, "testInstall:externalSubject");
            SnapshotEvent event = new SnapshotEvent(this, this, null, null);

            EventManager.getDefaultManager().postEvent(event, subject);
            assertSame(event, bridge.lastLocalEvent);
        }
        finally {
            bridge.uninstall();
        }
    }

    class TestBridge extends EventBridge {
        CayenneEvent lastLocalEvent;

        protected void sendRemoteEvent(CayenneEvent event) {
            lastLocalEvent = event;
        }
    }
}
