package org.objectstyle.cayenne.access;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextRefetchTst extends CayenneTestCase {

    public void testRefetchTempId() {
        MockQueryEngine engine = new MockQueryEngine(getDomain());
        MockDataDomain domain = new MockDataDomain(engine);

        DataContext context = domain.createDataContext();
        ObjectId tempID = new TempObjectId(Artist.class);

        try {
            context.refetchObject(tempID);
            fail("Refetching temp ID must have generated an error.");
        }
        catch (CayenneRuntimeException ex) {
            // expected ... but check that no queries were run
            assertEquals("Refetching temp id correctly failed, "
                    + "but DataContext shouldn't have run a query", 0, engine
                    .getRunCount());
        }
    }

}