package org.objectstyle.cayenne.access;

import org.apache.log4j.Logger;
import org.objectstyle.art.BinaryPKTest1;
import org.objectstyle.art.BinaryPKTest2;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextBinaryPKTst extends CayenneTestCase {
    private static Logger logObj = Logger.getLogger(DataContextBinaryPKTst.class);

    protected DataContext context;

    protected void setUp() throws Exception {
        getDatabaseSetup().cleanTableData();
        context = createDataContext();
    }

    public void testInsertBinaryPK() throws Exception {
        if (!getDatabaseSetupDelegate().supportsBinaryPK()) {
            return;
        }

        BinaryPKTest1 master =
            (BinaryPKTest1) context.createAndRegisterNewObject("BinaryPKTest1");
        master.setName("master1");

        BinaryPKTest2 detail =
            (BinaryPKTest2) context.createAndRegisterNewObject("BinaryPKTest2");
        detail.setDetailName("detail2");

        master.addToBinaryPKDetails(detail);

        context.commitChanges();

    }

    public void testFetchRelationshipBinaryPK() throws Exception {
        if (!getDatabaseSetupDelegate().supportsBinaryPK()) {
            return;
        }

        BinaryPKTest1 master =
            (BinaryPKTest1) context.createAndRegisterNewObject("BinaryPKTest1");
        master.setName("master1");

        BinaryPKTest2 detail =
            (BinaryPKTest2) context.createAndRegisterNewObject("BinaryPKTest2");
        detail.setDetailName("detail2");

        master.addToBinaryPKDetails(detail);

        context.commitChanges();

        // create new context
        context = createDataContext();
        BinaryPKTest2 fetchedDetail =
            (BinaryPKTest2) context.performQuery(
                new SelectQuery(BinaryPKTest2.class)).get(
                0);

        assertNotNull(fetchedDetail.readPropertyDirectly("toBinaryPKMaster"));

        BinaryPKTest1 fetchedMaster = fetchedDetail.getToBinaryPKMaster();
        assertNotNull(fetchedMaster);
        assertEquals(PersistenceState.HOLLOW, fetchedMaster.getPersistenceState());
        assertEquals("master1", fetchedMaster.getName());
    }
}
