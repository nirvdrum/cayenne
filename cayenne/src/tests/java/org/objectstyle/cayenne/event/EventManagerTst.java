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
import java.util.EventObject;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

import junit.framework.Assert;

public class EventManagerTst
	extends CayenneTestCase
	implements EventListener
{
	private static final Logger log = Logger.getLogger(EventManagerTst.class);

	// used for counting received events on the class
	public static int _numberOfReceivedEventsForClass;

	// used for counting received events per listener instance
	public int _numberOfReceivedEvents;

	// the event manager used for testing
	private EventManager _eventManager;

	public EventManagerTst(String arg0) {
		super(arg0);
	}

	public void setUp() throws Exception {
		_eventManager = new EventManager();
		_numberOfReceivedEvents = 0;
		_numberOfReceivedEventsForClass = 0;
	}

	public void testNullListener() throws Exception {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
			_eventManager.addListener(null, null, null, subject);
			Assert.fail();
		}

		catch (IllegalArgumentException ia) {
			// expected
		}
	}

	public void testNullNotification() throws Exception {
		// null notification
		try {
			_eventManager.addListener(this, "testNullObserver", CayenneEvent.class, null);
			Assert.fail();
		}

		catch (IllegalArgumentException e) {	
			// expected
		}

		// invalid event class
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "");		
			_eventManager.addListener(this, "testNullObserver", null, subject);
			Assert.fail();
		}

		catch (IllegalArgumentException e) {
			// expected
		}

		// empty string notification
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "");		
			_eventManager.addListener(this, "testNullObserver", CayenneEvent.class, subject);
			Assert.fail();
		}

		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testNonexistingMethod() {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
			_eventManager.addListener(this, "thisMethodDoesNotExist", CayenneEvent.class, subject);
			Assert.fail();
		}

		catch (NoSuchMethodException e) {
			// expected
		}
	}

	public void testInvalidArgumentTypes() {
		try {
			EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
			_eventManager.addListener(this, "seeTheWrongMethod", CayenneEvent.class, subject);
			Assert.fail();
		}

		catch (NoSuchMethodException e) {
			// expected
		}
	}

	public void testNonretainedListener() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(new EventManagerTst(""), "seeNotification", CayenneEvent.class, subject);

		// (hopefully) make the listener go away
		System.gc();
		System.gc();

		_eventManager.postEvent(new CayenneEvent(this), subject);
		Assert.assertEquals(0, _numberOfReceivedEventsForClass);
	}

	public void testValidSubclassOfRegisteredEventClass() throws Exception  {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
		_eventManager.postEvent(new MyCayenneEvent(this), subject);

		Assert.assertEquals(1, _numberOfReceivedEvents);
	}

	public void testWrongRegisteredEventClass() throws Exception {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");

		// we register a method that takes a CayenneEvent or subclass thereof.. 
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

		// ..but post a subclass of EventObject that is not compatible with CayenneEvent
		_eventManager.postEvent(new EventObject(this), subject);

		Assert.assertEquals(0, _numberOfReceivedEvents);
	}

	public void testSuccessfulNotificationDefaultSender() throws Exception {
		EventManagerTst listener1 = this;
		EventManagerTst listener2 = new EventManagerTst("#2");

		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(listener1, "seeNotification", CayenneEvent.class, subject);
		_eventManager.addListener(listener2, "seeNotification", CayenneEvent.class, subject);

		_eventManager.postEvent(new CayenneEvent(this), subject);
		
		Assert.assertEquals(1, listener1._numberOfReceivedEvents);
		Assert.assertEquals(1, listener2._numberOfReceivedEvents);
		Assert.assertEquals(2, _numberOfReceivedEventsForClass);
	}

	public void testSuccessfulNotificationIndividualSender() throws Exception {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject, this);
		_eventManager.postEvent(new CayenneEvent(this), subject);

		Assert.assertEquals(1, this._numberOfReceivedEvents);
		Assert.assertEquals(1, _numberOfReceivedEventsForClass);
	}

	public void testSuccessfulNotificationIndividualSenderTwice() throws Exception {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject, this);
		_eventManager.postEvent(new CayenneEvent(this), subject);

		Assert.assertEquals(2, this._numberOfReceivedEvents);
		Assert.assertEquals(2, _numberOfReceivedEventsForClass);
	}

	public void testSuccessfulNotificationBothDefaultAndIndividualSender() throws Exception {
		EventManagerTst listener1 = this;
		EventManagerTst listener2 = new EventManagerTst("#2");

		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(listener1, "seeNotification", CayenneEvent.class, subject, listener1);
		_eventManager.addListener(listener2, "seeNotification", CayenneEvent.class, subject);

		_eventManager.postEvent(new CayenneEvent(this), subject);

		Assert.assertEquals(1, listener1._numberOfReceivedEvents);
		Assert.assertEquals(1, listener2._numberOfReceivedEvents);
		Assert.assertEquals(2, _numberOfReceivedEventsForClass);
	}

	public void testRemoveOnEmptyList() {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		Assert.assertEquals(false, _eventManager.removeListener(this, subject));
	}

	public void testRemoveOnNullSubject() {
		Assert.assertEquals(false, _eventManager.removeListener(this, null));
	}

	public void testRemoveFromDefaultQueue() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
		Assert.assertTrue(_eventManager.removeListener(this, subject));
		Assert.assertEquals(false, _eventManager.removeListener(this));
	}

	public void testRemoveSpecificQueue() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject, this);
		Assert.assertTrue(_eventManager.removeListener(this, subject));
		Assert.assertEquals(false, _eventManager.removeListener(this));
	}

	public void testRemoveSpecificSender() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject, this);
		Assert.assertTrue(_eventManager.removeListener(this, subject, this));
		Assert.assertEquals(false, _eventManager.removeListener(this));
	}

	public void testRemoveAll() throws NoSuchMethodException {
		EventSubject subject1 = EventSubject.getSubject(this.getClass(), "XXX1");
		EventSubject subject2 = EventSubject.getSubject(this.getClass(), "XXX2");
		EventSubject subject3 = EventSubject.getSubject(this.getClass(), "XXX3");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject1);
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject2);
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject3, this);

		Assert.assertTrue(_eventManager.removeListener(this));
		Assert.assertEquals(false, _eventManager.removeListener(this));
		Assert.assertEquals(false, _eventManager.removeListener(this, subject1));
		Assert.assertEquals(false, _eventManager.removeListener(this, subject2));
		Assert.assertEquals(false, _eventManager.removeListener(this, subject3));
	}

	public void testSubjectGarbageCollection() throws NoSuchMethodException {
		EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
		_eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

		// let go of the subject & (hopefully) release queue
		subject = null;
		System.gc();
		System.gc();

		Assert.assertEquals(false, _eventManager.removeListener(this));
	}


	// notification method
	public void seeNotification(CayenneEvent event) {
		log.debug("seeNotification. source: " + event.getSource().getClass().getName());
		_numberOfReceivedEvents++;
		_numberOfReceivedEventsForClass++;
	}

	public void seeTheWrongMethod(int hansi) {
		log.debug("seeTheWrongMethod: " + hansi);
	}
}

// dummy class to test for incompatible events 
class MyCayenneEvent extends CayenneEvent {
	public MyCayenneEvent(EventListener l) {
		super(l);
	}
}

