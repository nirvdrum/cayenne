package org.objectstyle.cayenne.wocompat;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class EOModelProcessorInheritanceTst extends BasicTestCase {
    protected EOModelProcessor processor;

    public void setUp() throws Exception {
        processor = new EOModelProcessor();
    }

    public void testLoadAbstractEntity() throws Exception {
        DataMap map = processor.loadEOModel("test-resources/inheritance.eomodeld");

        ObjEntity abstractE = map.getObjEntity("AbstractEntity");
        assertNotNull(abstractE);
        assertNull(abstractE.getDbEntityName());
        assertEquals("AbstractEntityClass", abstractE.getClassName());
    }

    public void testLoadConcreteEntity() throws Exception {
        DataMap map = processor.loadEOModel("test-resources/inheritance.eomodeld");

        ObjEntity concreteE = map.getObjEntity("ConcreteEntityOne");
        assertNotNull(concreteE);
        assertEquals("AbstractEntity", concreteE.getSuperEntityName());
        
        assertEquals("CONCRETE_ENTITY_ONE", concreteE.getDbEntityName());
        assertEquals("ConcreteEntityClass", concreteE.getClassName());
        assertEquals("AbstractEntityClass", concreteE.getSuperClassName());
    }
}
