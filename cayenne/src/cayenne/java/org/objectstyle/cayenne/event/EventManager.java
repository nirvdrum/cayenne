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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

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
	private static final EventManager _instance = new EventManager();

	private HashMap _subjects;

	public static EventManager getDefaultManager() {
		return _instance;
	}
	
	public EventManager() {
		super();
		_subjects = new HashMap();
	}

	public void addListener(EventListener listener,
							String methodName,
							Class eventParameterClass,
							EventSubject subject)
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

		Set listenersForSubject = this.listenersForSubject(subject);
		if (listenersForSubject == null) {
			this.addSubject(subject);
			listenersForSubject = this.listenersForSubject(subject);
		}

		listenersForSubject.add(inv);
	}

	/**
	 * @return <code>true</code> if <code>listener</code> could be removed for
	 * all existing subjects, else returns <code>false</code>.
	 */
	public boolean removeListener(EventListener listener) {
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
	 * @return <code>true</code> if <code>listener</code> could be removed for
	 * the given subject, else returns <code>false</code>.
	 */
	public boolean removeListener(EventListener listener, EventSubject subject) {
		boolean didRemove = false;

		if ((listener != null) && (subject != null)) {
			Set listenersForSubject = this.listenersForSubject(subject);

			if ((listenersForSubject != null) && (listenersForSubject.isEmpty() == false)) {
				// remove all invocations with the given target
				Iterator listenerIter = listenersForSubject.iterator();
				while (listenerIter.hasNext()) {
					Invocation inv = (Invocation)listenerIter.next();
					if (inv.getTarget() == listener) {
						listenerIter.remove();
						didRemove = true;
					}
				}
			}
		}

		return didRemove;
	}

	public void postEvent(EventObject event, EventSubject subject) {
		// get current listeners for subject
		Set listenersForSubject = this.listenersForSubject(subject);

		if ((listenersForSubject != null) && (listenersForSubject.isEmpty() == false)) {
			// used to collect all invalid invocations in order to remove
			// them at the end of this posting cycle
			List invalidInvocations = null;
			Object[] eventArgument = new Object[]{event};
			Iterator iter = listenersForSubject.iterator();
			while (iter.hasNext()) {
				Invocation inv = (Invocation)iter.next();

				// fire invocation, detect if anything went wrong
				if (inv.fire(eventArgument) == false) {
					if (invalidInvocations == null) {
						invalidInvocations = new ArrayList();
					}

					invalidInvocations.add(inv);
				}
			}

			// clear out all invalid invocations
			if (invalidInvocations != null) {
				listenersForSubject.removeAll(invalidInvocations);
			}
		}
	}

	// register a new subject and a corresponding listener queue
	private void addSubject(EventSubject subject) {
		// make sure we don't add a subject twice, losing the old observers
		if (_subjects.get(subject) == null) {
			_subjects.put(subject, new HashSet());
		}
	}

	// remove a subject and its listener queue
	private void removeSubject(EventSubject subject) {
		_subjects.remove(subject);
	}

	// return all registered listeners (Invocations) for the subject
	private Set listenersForSubject(EventSubject subject) {
		return (Set)_subjects.get(subject);
	}
}
