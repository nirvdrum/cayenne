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

import java.util.EventListener;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

import junit.framework.Assert;

public class EventManagerTst
	extends CayenneTestCase
	implements EventListener
{
	private static final Logger log = Logger.getLogger(EventManagerTst.class);
	
	private EventManager _eventManager;
	private boolean _didReceiveNotification;
	
	public EventManagerTst(String arg0) {
		super(arg0);
	}

	public void setUp() throws Exception {
		_eventManager = new EventManager();
		_didReceiveNotification = false;
	}

	public void testNullListener() throws NoSuchMethodException {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
			_eventManager.addListener(null, null, null, subject);
			Assert.fail();
		}

		catch (IllegalArgumentException ia) {
			// OK: argument is supposed to be illegal
		}
	}

	public void testNullNotification() {
		// null notification
		try {
			_eventManager.addListener(this, CayenneEvent.class, "testNullObserver", null);
			Assert.fail();
		}

		catch (Exception e) {	
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}

		// invalid event class
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "");		
			_eventManager.addListener(this, null, "testNullObserver", subject);
			Assert.fail();
		}

		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}

		// empty string notification
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "");		
			_eventManager.addListener(this, CayenneEvent.class, "testNullObserver", subject);
			Assert.fail();
		}

		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}
	}

	public void testNonexistingMethod() {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");		
			_eventManager.addListener(this, CayenneEvent.class, "thisMethodDoesNotExist", subject);
			Assert.fail();
		}

		catch (Exception e) {
			Assert.assertTrue(e instanceof NoSuchMethodException);
		}
	}
	
	public void testInvalidArgumentTypes() {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");		
			_eventManager.addListener(this, CayenneEvent.class, "seeTheWrongMethod", subject);
			Assert.fail();
		}

		catch (Exception e) {
			Assert.assertTrue(e instanceof NoSuchMethodException);
		}
	}

	public void testNonretainedListener() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(new EventManagerTst(""), CayenneEvent.class, "seeNotification", subject);
		
		// (hopefully) make the listener go away
		System.gc();
		System.gc();

		_eventManager.postEvent(new CayenneEvent(this), subject);
	}
	
	public void testSuccessfulNotification() {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");		
			_eventManager.addListener(this, CayenneEvent.class, "seeNotification", subject);
			_eventManager.postEvent(new CayenneEvent(this), subject);
			
			Assert.assertTrue(_didReceiveNotification);
		}

		catch (Exception e) {
			log.error("testSuccessfulNotification", e);
			Assert.fail();
		}
	}

	public void testRemoveOnEmptyList() {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");		
		Assert.assertEquals(false, _eventManager.removeListener(this, subject));
	}

	public void testRemoveOnNullSubject() {
		Assert.assertEquals(false, _eventManager.removeListener(this, null));
	}

	public void testRemove() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");		
		_eventManager.addListener(this, CayenneEvent.class, "seeNotification", subject);
		Assert.assertTrue(_eventManager.removeListener(this, subject));
	}

	public void testRemoveAll() throws NoSuchMethodException {
		EventSubject subject1 = EventSubject.getSubject(this.getClass(), "XXX1");
		EventSubject subject2 = EventSubject.getSubject(this.getClass(), "XXX2");
		_eventManager.addListener(this, CayenneEvent.class, "seeNotification", subject1);
		_eventManager.addListener(this, CayenneEvent.class, "seeNotification", subject2);

		Assert.assertTrue(_eventManager.removeListener(this));
		Assert.assertEquals(false, _eventManager.removeListener(this, subject1));
		Assert.assertEquals(false, _eventManager.removeListener(this, subject2));
	}

	// notification method
	public void seeNotification(CayenneEvent event) {
		log.debug("seeNotification. source: " + event.getSource().getClass().getName());
		_didReceiveNotification = true;
	}

	public void seeTheWrongMethod(int hansi) {
		log.debug("seeTheWrongMethod: " + hansi);
	}
}
