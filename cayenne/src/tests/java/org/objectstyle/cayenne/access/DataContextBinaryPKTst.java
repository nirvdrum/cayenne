package org.objectstyle.cayenne.access;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Types;
import java.util.List;

import org.objectstyle.art.BinaryPKTest1;
import org.objectstyle.art.BinaryPKTest2;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextBinaryPKTst extends CayenneTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        getDatabaseSetup().cleanTableData();
        context = createDataContext();
    }

    public void testInsertBinaryPK() throws Exception {
        if(!getDatabaseSetupDelegate().supportsBinaryPK()) {
            return;
        }
        
        // set alt PK generator for the duration of the test
        AdapterProxyHandler handler = initAdapterProxy();
        try {
            BinaryPKTest1 master =
                (BinaryPKTest1) context.createAndRegisterNewObject("BinaryPKTest1");
            master.setName("master1");

            BinaryPKTest2 detail =
                (BinaryPKTest2) context.createAndRegisterNewObject("BinaryPKTest2");
            detail.setDetailName("detail2");

            master.addToBinaryPKDetails(detail);

            context.commitChanges();
        }
        finally {
            // restore original adapter
            super.getNode().setAdapter(handler.getAdapter());
        }
    }

    public void testFetchRelationshipBinaryPK() throws Exception {
        if(!getDatabaseSetupDelegate().supportsBinaryPK()) {
            return;
        }
        
        // set alt PK generator for the duration of the test
        AdapterProxyHandler handler = initAdapterProxy();
        try {
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
        finally {
            // restore original adapter
            super.getNode().setAdapter(handler.getAdapter());
        }
    }

    private AdapterProxyHandler initAdapterProxy() {
        AdapterProxyHandler handler =
            new AdapterProxyHandler(super.getNode().getAdapter());

        DbAdapter adapterProxy =
            (DbAdapter) Proxy.newProxyInstance(
                DbAdapter.class.getClassLoader(),
                new Class[] { DbAdapter.class },
                handler);

        super.getNode().setAdapter(adapterProxy);
        return handler;
    }

    static class AdapterProxyHandler implements InvocationHandler {
        DbAdapter adapter;
        PkGenerator generator;

        AdapterProxyHandler(DbAdapter adapter) {
            this.adapter = adapter;
            BinaryPKGeneratorHandler handler =
                new BinaryPKGeneratorHandler(adapter.getPkGenerator());

            this.generator =
                (PkGenerator) Proxy.newProxyInstance(
                    PkGenerator.class.getClassLoader(),
                    new Class[] { PkGenerator.class },
                    handler);
        }

        public DbAdapter getAdapter() {
            return adapter;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

            if ("getPkGenerator".equals(method.getName())) {
                return generator;
            }

            return method.invoke(adapter, args);
        }

    }

    static class BinaryPKGeneratorHandler implements InvocationHandler {
        private static volatile long currentId = Long.MIN_VALUE;
        private static MessageDigest md;

        PkGenerator generator;

        static {
            try {
                md = MessageDigest.getInstance("MD5");
            }
            catch (NoSuchAlgorithmException e) {
                throw new CayenneRuntimeException("Can't initialize MessageDigest.", e);
            }
        }

        BinaryPKGeneratorHandler(PkGenerator generator) {
            this.generator = generator;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

            // use binary generator if the column is BINARY or VARBINARY
            if ("generatePkForDbEntity".equals(method.getName())
                && args != null
                && args.length == 2) {
                DbEntity entity = (DbEntity) args[1];

                List idColumns = entity.getPrimaryKey();
                if (idColumns.size() == 1) {
                    DbAttribute pk = (DbAttribute) idColumns.get(0);
                    if (pk.getType() == Types.BINARY
                        || pk.getType() == Types.VARBINARY) {
                        return generatePkForDbEntity((DataNode) args[0], entity);
                    }
                }
            }
            else if ("getPkCacheSize".equals(method.getName())) {
                return new Integer(1);
            }

            return method.invoke(generator, args);
        }

        /**
         * Uses a mock algorithm for unique binary string.
         */
        public Object generatePkForDbEntity(DataNode node, DbEntity ent)
            throws Exception {

            String stringId = ent.getName() + currentId++;
            return md.digest(stringId.getBytes());
        }
    }
}
