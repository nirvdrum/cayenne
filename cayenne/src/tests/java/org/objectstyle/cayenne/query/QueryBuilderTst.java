package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class QueryBuilderTst extends BasicTestCase {
    protected QueryBuilder builder;

    protected void setUp() throws Exception {
        builder = new QueryBuilder() {
            public Query getQuery() {
                return null;
            }
        };
    }

    public void testSetName() throws Exception {
        builder.setName("aaa");
        assertEquals("aaa", builder.name);
    }

    public void testSetRootDbEntity() throws Exception {
        DataMap map = new DataMap("map");
        DbEntity entity = new DbEntity("DB1");
        map.addDbEntity(entity);

        builder.setRoot(map, QueryBuilder.DB_ENTITY_ROOT, "DB1");
        assertSame(entity, builder.root);
    }

    public void testSetRootObjEntity() throws Exception {
        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("OBJ1");
        map.addObjEntity(entity);

        builder.setRoot(map, QueryBuilder.OBJ_ENTITY_ROOT, "OBJ1");
        assertSame(entity, builder.root);
    }
    
    public void testSetRootDataMap() throws Exception {
        DataMap map = new DataMap("map");

        builder.setRoot(map, QueryBuilder.DATA_MAP_ROOT, null);
        assertSame(map, builder.root);
    }
}
