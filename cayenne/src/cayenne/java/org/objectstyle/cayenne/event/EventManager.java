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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.collections.iterators.SingletonIterator;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.util.Invocation;

/**
 * This class acts as bridge between an Object that wants to inform others about
 * its current state or a achnage thereof (Publisher) and a list of objects
 * interested in the Subject (Listeners).
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstätte
 */
public class EventManager extends Object {
	private static final Logger log = Logger.getLogger(EventManager.class);
	private static final EventManager _defaultManager = new EventManager();

	// keeps weak references to subjects
	private Map _subjects;

	/**
	 * This method will return the shared 'default' EventManager.
	 * 
	 * @return EventManager the shared EventManager instance
	 */
	public static EventManager getDefaultManager() {
		return _defaultManager;
	}

	/**
	 * Default constructor for new EventManager instances, in case you need one.
	 */
	public EventManager() {
		super();
		_subjects = new WeakHashMap();
	}

	/**
	 * Register	an <code>EventListener</code> for events sent by any sender.
	 * 
	 * @throws NoSuchMethodException if <code>methodName</code> is not found
	 * @see #addListener(EventListener, String, Class, EventSubject, Object)
	 */
	synchronized public void addListener(EventListener listener,
											String methodName,
											Class eventParameterClass,
											EventSubject subject)
		throws NoSuchMethodException {
		this.addListener(listener, methodName, eventParameterClass, subject, this);
	}

	/**
	 * Register	an <code>EventListener</code> for events sent by a specific
	 * sender.
	 * 
	 * @param listener the object to be notified about events
	 * @param methodName the name of the listener method to be invoked
	 * @param eventParameterClass the class of the single event argument passed
	 * to <code>methodName</code>
	 * @param subject the event subject that the listener is interested in
	 * @param sender the object whose events the listener is interested in;
	 * <code>null</code> means 'any sender'.
	 * @throws NoSuchMethodException if <code>methodName</code> is not found
	 */
	synchronized public void addListener(EventListener listener,
											String methodName,
											Class eventParameterClass,
											EventSubject subject,
											Object sender)
		throws NoSuchMethodException {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}

		if (eventParameterClass == null) {
			throw new IllegalArgumentException("event class must not be null");
		}

		if (subject == null) {
			throw new IllegalArgumentException("subject must not be null");
		}

		Invocation inv = new Invocation(listener, methodName, eventParameterClass);

		Map subjectQueues = this.invocationQueuesForSubject(subject);
		if (subjectQueues == null) {
			// make sure the subject can be associated with invocation queues
			subjectQueues = new WeakHashMap();
			_subjects.put(subject, subjectQueues);
		}

		Set queueForSender = this.invocationQueueForSubjectAndSender(subject, sender);
		if (queueForSender == null) {
			// create a new listener 'queue'; must keep strong references
			queueForSender = new HashSet();
			subjectQueues.put(sender, queueForSender);
		}

		queueForSender.add(inv);
	}

	/**
	 * Unregister the specified listener from all event subjects handled by this
	 * <code>EventManager</code> instance.
	 * 
	 * @param listener the object to be unregistered
	 * @return <code>true</code> if <code>listener</code> could be removed for
	 * any existing subjects, else returns <code>false</code>.
	 */
	synchronized public boolean removeListener(EventListener listener) {
		boolean didRemove = false;

		if ((_subjects.isEmpty() == false) && (listener != null)) {
			Iterator subjectIter = _subjects.keySet().iterator();
			while (subjectIter.hasNext()) {
				didRemove |= this.removeListener(listener, (EventSubject)subjectIter.next());
			}
		}

		return didRemove;
	}

	/**
	 * Unregister the specified listener for the events about the given subject.
	 * 
	 * @param listener the object to be unregistered
	 * @param subject the subject from which the listener is to be unregistered
	 * @return <code>true</code> if <code>listener</code> could be removed for
	 * the given subject, else returns <code>false</code>.
	 */
	synchronized public boolean removeListener(EventListener listener,
												EventSubject subject) {
		return this.removeListener(listener, subject, null);
	}

	/**
	 * Unregister the specified listener for the events about the given subject
	 * and the given sender.
	 * 
	 * @param listener the object to be unregistered
	 * @param subject the subject from which the listener is to be unregistered
	 * @param sender the object whose events the listener was interested in;
	 * <code>null</code> means 'any sender'.
	 * @return <code>true</code> if <code>listener</code> could be removed for
	 * the given subject, else returns <code>false</code>.
	 */
	synchronized public boolean removeListener(EventListener listener,
												EventSubject subject,
												Object sender) {
		boolean didRemove = false;

		if ((listener != null) && (subject != null)) {
			Map subjectQueues = this.invocationQueuesForSubject(subject);
			if (subjectQueues != null) {
				Iterator queueIter;
				
				// remove only listeners for sender?
				if (sender != null) {
					Set senderQueue = this.invocationQueueForSubjectAndSender(subject, sender);
					queueIter = new SingletonIterator(senderQueue);
				}
				else {
					queueIter = subjectQueues.values().iterator();
				}

				// iterate over all invocation queues for this subject
				while (queueIter.hasNext()) {
					Set invocations = (Set)queueIter.next();
					if ((invocations != null) && (invocations.isEmpty() == false)) {
						// remove all invocations with the given target
						Iterator invIter = invocations.iterator();
						while (invIter.hasNext()) {
							Invocation inv = (Invocation)invIter.next();
							if (inv.getTarget() == listener) {
								invIter.remove();
								didRemove = true;
							}
						}
					}
				}
			}
		}
	
		return didRemove;
	}

	/**
	 * Sends an event to all registered objects about a particular subject.
	 * 
	 * @param event the event to be posted to the observers
	 * @param subject the subject about which observers will be notified
	 * @throws IllegalArgumentException if event or subject are null
	 */
	synchronized public void postEvent(EventObject event, EventSubject subject) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}

		if (subject == null) {
			throw new IllegalArgumentException("subject must not be null");
		}

		// collect listener invocations for subject
		Set specificInvocations = this.invocationQueueForSubjectAndSender(subject, event.getSource());
		Set defaultInvocations = this.invocationQueueForSubjectAndSender(subject, this);
		Set[] invocationQueues = new Set[]{specificInvocations, defaultInvocations};
		Object[] eventArgument = new Object[]{event};

		for (int i = 0; i < invocationQueues.length; i++) {
			Set currentQueue = invocationQueues[i];
			if ((currentQueue != null) && (currentQueue.isEmpty() == false)) {
				// used to collect all invalid invocations in order to remove
				// them at the end of this posting cycle
				List invalidInvocations = null;
				Iterator iter = currentQueue.iterator();
				while (iter.hasNext()) {
					Invocation inv = (Invocation)iter.next();
					Class[] invParamTypes = inv.getParameterTypes();
	
					// we only process event listeners which take exactly
					// one argument in their registered methods: the passed
					// event or a valid subclass thereof
					if ((invParamTypes != null)
						&& (invParamTypes.length == 1)
						&& (invParamTypes[0].isAssignableFrom(event.getClass()))) {
						// fire invocation, detect if anything went wrong
						// (e.g. GC'ed invocation targets)
						if (inv.fire(eventArgument) == false) {
							if (invalidInvocations == null) {
								invalidInvocations = new ArrayList();
							}
		
							invalidInvocations.add(inv);
						}
					}
				}
	
				// clear out all invalid invocations
				if (invalidInvocations != null) {
					currentQueue.removeAll(invalidInvocations);
				}
			}
		}
	}

	// returns a subject's mapping from senders to registered listener invocations
	private Map invocationQueuesForSubject(EventSubject subject) {
		return (Map)_subjects.get(subject);
	}

	// returns the registered listener invocations for a particular sender;
	// the owning event manager instance is used as default sender
	private Set invocationQueueForSubjectAndSender(EventSubject subject, Object sender) {
		if (sender == null) {
			sender = this;
		}

		Map subjectEntries = this.invocationQueuesForSubject(subject);
		Set queue = null;

		if (subjectEntries != null) {
			queue = (Set)subjectEntries.get(sender);
		}

		return queue;
	}

}

