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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

import junit.framework.Assert;

public class ObserverManagerTst extends CayenneTestCase
{
	private static final Logger log = Logger.getLogger(ObserverManagerTst.class);
	
	private ObserverManager _observerManager;
	private boolean _didReceiveNotification;

	static
	{
		BasicConfigurator.configure();	
	}
	
	public ObserverManagerTst(String arg0)
	{
		super(arg0);
	}
	
	public void setUp() throws Exception
	{
		_observerManager = new ObserverManager();
		_didReceiveNotification = false;
	}

	public void testNullObserver() throws NoSuchMethodException
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "hansi");
			_observerManager.addObserver(null, null, subject);
			Assert.fail();
		}

		catch (IllegalArgumentException ia)
		{
			// OK: argument is supposed to be illegal
		}
	}
	
	public void testNullNotification()
	{
		// null notification
		try
		{
			_observerManager.addObserver(this, "testNullObserver", null);
			Assert.fail();
		}
		catch (Exception e) 
		{	
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}
		
		// empty string notification
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "");		
			_observerManager.addObserver(this, "testNullObserver", subject);
			Assert.fail();
		}
		catch (Exception e)
		{
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}
	}
	
	public void testNonexistingMethod()
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "hansi");		
			_observerManager.addObserver(this, "thisMethodDoesNotExist", subject);
			Assert.fail();
		}
		catch (Exception e)
		{
			Assert.assertTrue(e instanceof NoSuchMethodException);
		}
	}
	
	public void testInvalidArgumentTypes()
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "hansi");		
			_observerManager.addObserver(this, "seeTheWrongMethod", subject);
			Assert.fail();
		}
		catch (Exception e)
		{
			Assert.assertTrue(e instanceof NoSuchMethodException);
		}
	}
	
	public void testNonretainedObserver() throws NoSuchMethodException
	{
		ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");
		_observerManager.addObserver(new ObserverManagerTst(""), "seeNotification", subject);
		
		System.gc();
		_observerManager.postObserverEvent(subject, this);
	}
	
	public void testSuccessfulNotification()
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");		
			_observerManager.addObserver(this, "seeNotification", subject);
			_observerManager.postObserverEvent(subject, this);
			
			Assert.assertTrue(_didReceiveNotification);
		}
		catch (Exception e)
		{
			log.error("testSuccessfulNotification", e);
			Assert.fail();
		}
	}
	
	public void testRemoveOnEmptyList()
	{
		ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");		
		_observerManager.removeObserver(this, subject);
	}
	
	public void testRemoveOnNullObserverSubject()
	{
		Assert.assertEquals(false, _observerManager.removeObserver(this, null));
	}
	
	public void testRemove() throws NoSuchMethodException
	{
		ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");		
		_observerManager.addObserver(this, "seeNotification", subject);
		
		boolean didRemove = _observerManager.removeObserver(this, subject);
		Assert.assertTrue(didRemove);
	}

	//-------------------------------------------------------------------------
	// notification method
	//-------------------------------------------------------------------------
	public void seeNotification(ObserverEvent event)
	{
		log.debug("seeNotification. publisher: " + event.getPublisher().getClass().getName());
		_didReceiveNotification = true;
	}
	
	public void seeTheWrongMethod(int hansi)
	{
		log.debug("seeTheWrongMethod: " + hansi);
	}
}
