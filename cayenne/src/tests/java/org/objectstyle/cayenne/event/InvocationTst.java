
package org.objectstyle.cayenne.event;

import java.lang.reflect.Method;
import java.util.HashSet;

import org.apache.log4j.BasicConfigurator;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

import junit.framework.Assert;

public class InvocationTst extends CayenneTestCase
{
	private Method _method;
	
	static
	{
		BasicConfigurator.configure();	
	}

	public InvocationTst(String arg0) throws NoSuchMethodException
	{
		super(arg0);
		_method = this.getClass().getMethod("method", null);
	}

	public void testReflexive() throws Exception
	{
		Invocation inv = new Invocation(this, _method);
		
		Assert.assertEquals(inv, inv);
	}

	public void testSymmetric() throws Exception
	{
		Invocation inv = new Invocation(this, _method);
		Invocation inv2 = new Invocation(this, _method);
		
		Assert.assertEquals(inv, inv2);
		Assert.assertEquals(inv2, inv);
	}

	public void testTransitive() throws Exception
	{
		Invocation inv = new Invocation(this, _method);
		Invocation inv2 = new Invocation(this, _method);
		Invocation inv3 = new Invocation(this, _method);
		
		Assert.assertEquals(inv, inv2);
		Assert.assertEquals(inv2, inv3);
		Assert.assertEquals(inv, inv3);
	}
		
	public void testNull()
	{
		Invocation inv = new Invocation(this, _method);
		Assert.assertTrue(inv.equals(null) == false);
	}
	
	public void testDifferentMethods() throws Exception 
	{
		Method m2 = this.getClass().getMethod("anotherMethod", null);

		Invocation inv = new Invocation(this, _method);
		Invocation inv2 = new Invocation(this, m2);

		Assert.assertTrue(inv.equals(inv2) == false);
	}

	public void testAddToSet() throws Exception
	{
		HashSet set = new HashSet();
		
		Invocation inv = new Invocation(this, _method);
		set.add(inv);
		set.add(inv);
		Assert.assertEquals(1, set.size());
	}
	
	public void testAddTwo()
	{
		HashSet set = new HashSet();
		
		Invocation inv = new Invocation(this, _method);
		Invocation inv2 = new Invocation(this, _method);
		
		set.add(inv);
		set.add(inv2);
		Assert.assertEquals(1, set.size());
	}

	public void testGarbageCollection() throws NoSuchMethodException
	{
		// create an invocation with an observer that will be garbage collected
		Invocation inv = new Invocation(new String(), String.class.getMethod("toString", null));
		
		// (hopefully) make the observer go away
		System.gc();
		System.gc();

		Assert.assertEquals(false, inv.fire(new ObserverEvent(this)));
	}
	
	// these methods exist for the test of Invocation equality
	public void method()
	{
	}
	
	public void anotherMethod()
	{
	}
}
