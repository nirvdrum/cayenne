/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */ 

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

		Assert.assertEquals(false, inv.fire(new ObserverEvent(this, null)));
	}
	
	// these methods exist for the test of Invocation equality
	public void method()
	{
	}
	
	public void anotherMethod()
	{
	}
}
