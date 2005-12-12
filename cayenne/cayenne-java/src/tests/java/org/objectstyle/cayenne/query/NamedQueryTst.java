package org.objectstyle.cayenne.query;

import junit.framework.TestCase;

import org.objectstyle.cayenne.opp.hessian.HessianUtil;
import org.objectstyle.cayenne.util.Util;

public class NamedQueryTst extends TestCase {

    public void testName() {
        NamedQuery query = new NamedQuery("abc");

        assertNull(query.getName());

        query.setName("123");
        assertEquals("123", query.getName());
    }

    public void testQueryName() {
        NamedQuery query = new NamedQuery("abc");
        assertEquals("abc", query.getQueryName());
        assertNull(query.getName());

        query.setQueryName("xyz");
        assertEquals("xyz", query.getQueryName());
        assertNull(query.getName());
    }

    public void testSerializability() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = Util.cloneViaSerialization(o);

        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;

        assertNotSame(o, c1);
        assertEquals(o.getName(), c1.getName());
        assertEquals(o.getQueryName(), c1.getQueryName());
    }

    public void testSerializabilityWithHessian() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = HessianUtil.cloneViaHessianSerialization(o);

        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;

        assertNotSame(o, c1);
        assertEquals(o.getQueryName(), c1.getQueryName());
    }
}
