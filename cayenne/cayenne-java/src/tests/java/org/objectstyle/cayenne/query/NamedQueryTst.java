package org.objectstyle.cayenne.query;

import junit.framework.TestCase;

import org.objectstyle.cayenne.opp.hessian.HessianUtil;
import org.objectstyle.cayenne.util.Util;


public class NamedQueryTst extends TestCase {

    public void testName() {
        NamedQuery query = new NamedQuery("abc");
        assertEquals("abc", query.getName());
        
        query.setName("xyz");
        assertEquals("xyz", query.getName());
    }
    
    public void testSerializability() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = Util.cloneViaSerialization(o);
        
        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;
        
        assertNotSame(o, c1);
        assertEquals(o.getName(), c1.getName());
    }
    
    public void testSerializabilityWithHessian() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = HessianUtil.cloneViaHessianSerialization(o);
        
        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;
        
        assertNotSame(o, c1);
        assertEquals(o.getName(), c1.getName());
    }
}
