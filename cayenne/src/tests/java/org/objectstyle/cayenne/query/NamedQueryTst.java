package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.unit.CayenneTestCase;


public class NamedQueryTst extends CayenneTestCase {

    public void testName() {
        NamedQuery query = new NamedQuery("abc");
        assertEquals("abc", query.getName());
        
        query.setName("xyz");
        assertEquals("xyz", query.getName());
    }
}
