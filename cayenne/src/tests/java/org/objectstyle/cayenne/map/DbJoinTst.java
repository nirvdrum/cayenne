package org.objectstyle.cayenne.map;

import junit.framework.TestCase;

/**
 * @author Andrei Adamchik
 */
public class DbJoinTst extends TestCase {

    public void testRelationship() throws Exception {
        DbJoin join = new DbJoin(null);
        assertNull(join.getRelationship());

        DbRelationship relationship = new DbRelationship("abc");
        join.setRelationship(relationship);
        assertSame(relationship, join.getRelationship());
    }

    public void testEquals1() throws Exception {
        DbJoin join = new DbJoin(null);
        assertEquals(join, join);
    }

    public void testEquals2() throws Exception {
        DbJoin join1 = new DbJoin(null);
        DbJoin join2 = new DbJoin(null);
        assertEquals(join1, join2);
    }

    public void testEquals3() throws Exception {
        DbRelationship relationship = new DbRelationship("abc");

        DbJoin join1 = new DbJoin(relationship);
        DbJoin join2 = new DbJoin(relationship);
        assertEquals(join1, join2);
    }

    public void testEquals4() throws Exception {
        DbRelationship relationship = new DbRelationship("abc");

        DbJoin join1 = new DbJoin(relationship, "a", "b");
        DbJoin join2 = new DbJoin(relationship, "a", "b");
        DbJoin join3 = new DbJoin(relationship, "a", "c");
        assertEquals(join1, join2);
        assertFalse(join1.equals(join3));
    }
    
    public void testHashCode1() throws Exception {
        DbJoin join1 = new DbJoin(null);
        DbJoin join2 = new DbJoin(null);
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    public void testHashCode2() throws Exception {
        DbRelationship relationship = new DbRelationship("abc");

        DbJoin join1 = new DbJoin(relationship);
        DbJoin join2 = new DbJoin(relationship);
        DbJoin join3 = new DbJoin();
        assertEquals(join1.hashCode(), join2.hashCode());
        assertFalse(join1.hashCode() == join3.hashCode());
    }

    public void testHashCode3() throws Exception {
        DbRelationship relationship = new DbRelationship("abc");

        DbJoin join1 = new DbJoin(relationship, "a", "b");
        DbJoin join2 = new DbJoin(relationship, "a", "b");
        DbJoin join3 = new DbJoin(relationship, "a", "c");
        assertEquals(join1.hashCode(), join2.hashCode());
        assertFalse(join1.hashCode() == join3.hashCode());
    }

}