package org.objectstyle.cayenne.exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ParametrizedExpressionTst extends CayenneTestCase {
    /**
     * Constructor for ParametrizedExpressionTst.
     * @param name
     */
    public ParametrizedExpressionTst(String name) {
        super(name);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     * 
     * @throws Exception
     */
    public void testCopy1() throws Exception {
        Expression e1 = ExpressionFactory.matchExp("k1", "v1");
        e1 = e1.orExp(ExpressionFactory.matchExp("k2", "v2"));
        e1 = e1.orExp(ExpressionFactory.matchExp("k3", "v3"));

        Expression e2 = e1.expWithParams(new HashMap(), true);

        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     *
     * @throws Exception
     */
    public void testCopy2() throws Exception {
        Expression andExp = ExpressionFactory.matchExp("k1", "v1");
        andExp = andExp.andExp(ExpressionFactory.matchExp("k2", "v2"));
        andExp = andExp.andExp(ExpressionFactory.matchExp("k3", "v3"));

        List exprs = new ArrayList();
        exprs.add(andExp);
        exprs.add(ExpressionFactory.matchExp("k1", "v1"));

        Expression e1 = ExpressionFactory.joinExp(Expression.OR, exprs);
        Expression e2 = e1.expWithParams(new HashMap(), true);

        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     *
     * @throws Exception
     */
    public void testFailOnMissingParams() throws Exception {
        Expression e1 =
            ExpressionFactory.matchExp("k1", new ExpressionParam("test"));
        e1 = e1.orExp(ExpressionFactory.matchExp("k2", "v2"));
        e1 = e1.orExp(ExpressionFactory.matchExp("k3", "v3"));

        try {
            e1.expWithParams(new HashMap(), false);
            fail("Parameter was missing, but no exception was thrown.");
        } catch (ExpressionException ex) {
            // exception expected
        }
    }

    public void testParams1() throws Exception {
        Expression e1 =
            ExpressionFactory.matchExp("k1", new ExpressionParam("test"));

        Map map = new HashMap();
        map.put("test", "xyz");
        Expression e2 = e1.expWithParams(map, false);
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals(Expression.EQUAL_TO, e2.getType());
        assertEquals("xyz", e2.getOperand(1));
    }

    public void testNoParams1() throws Exception {
        Expression e1 =
            ExpressionFactory.matchExp("k1", new ExpressionParam("test"));

        Expression e2 = e1.expWithParams(new HashMap(), true);

        // all expression nodes must be pruned
        assertNull(e2);
    }

    public void testNoParams2() throws Exception {
        List list = new ArrayList();
        list.add(
            ExpressionFactory.matchExp("k1", new ExpressionParam("test1")));
        list.add(
            ExpressionFactory.matchExp("k2", new ExpressionParam("test2")));
        list.add(
            ExpressionFactory.matchExp("k3", new ExpressionParam("test3")));
        list.add(
            ExpressionFactory.matchExp("k4", new ExpressionParam("test4")));
        Expression e1 = ExpressionFactory.joinExp(Expression.OR, list);

        Map params = new HashMap();
        params.put("test2", "abc");
        params.put("test3", "xyz");
        Expression e2 = e1.expWithParams(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertTrue(
            "Not a list expression: " + e2,
            e2 instanceof ListExpression);

        ListExpression le = (ListExpression) e2;
        assertEquals(2, le.getOperandCount());

        Expression k2 = (Expression) le.getOperand(0);
        assertEquals("abc", k2.getOperand(1));

        Expression k3 = (Expression) le.getOperand(1);
        assertEquals("xyz", k3.getOperand(1));
    }

    public void testNoParams3() throws Exception {
        List list = new ArrayList();
        list.add(
            ExpressionFactory.matchExp("k1", new ExpressionParam("test1")));
        list.add(
            ExpressionFactory.matchExp("k2", new ExpressionParam("test2")));
        list.add(
            ExpressionFactory.matchExp("k3", new ExpressionParam("test3")));
        list.add(
            ExpressionFactory.matchExp("k4", new ExpressionParam("test4")));
        Expression e1 = ExpressionFactory.joinExp(Expression.OR, list);

        Map params = new HashMap();
        params.put("test4", "123");
        Expression e2 = e1.expWithParams(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertTrue("List expression: " + e2, !(e2 instanceof ListExpression));

        assertEquals(2, e2.getOperandCount());
        assertEquals("123", e2.getOperand(1));
        assertEquals("k4", ((Expression)e2.getOperand(0)).getOperand(0));
    }
}
