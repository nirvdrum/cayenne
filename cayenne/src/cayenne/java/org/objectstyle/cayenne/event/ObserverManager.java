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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class acts as bridge between an Object that wants to inform others about
 * its current state (Publisher) and a list of objects interested in the Subject
 * (Observers).
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstätte
 */
public class ObserverManager extends Object {
	private static final Logger log = Logger.getLogger(ObserverManager.class);
	private static final ObserverManager _instance = new ObserverManager();
	
	private HashMap _subjects;

	public static ObserverManager getDefaultManager() {
		return _instance;
	}
	
	public ObserverManager() {
		super();
		_subjects = new HashMap();
	}

	public void addObserver(ObserverEventListener observer,
							Class handledEventArgumentClass,
							String methodName,
							ObserverSubject subject)
		throws NoSuchMethodException {
		if (observer == null) {
			throw new IllegalArgumentException("observer must not be null");
		}

		if (handledEventArgumentClass == null) {
			throw new IllegalArgumentException("event class must not be null");
		}

		if (subject == null) {
			throw new IllegalArgumentException("subject must not be null");
		}

		Method method = observer.getClass().getMethod(methodName, new Class[] { handledEventArgumentClass });		
		ObserverInvocation inv = new ObserverInvocation(observer, method);

		Set observersForSubject = this.observersForSubject(subject);
		if (observersForSubject == null) {
			this.addSubject(subject);
			observersForSubject = this.observersForSubject(subject);
		}

		observersForSubject.add(inv);
	}

	public void postObserverEvent(ObserverSubject subject, ObserverEvent event) {
		// get current observers for subject
		Set observersForEvent = this.observersForSubject(subject);

		if (observersForEvent != null) {
			// used to collect all invalid invocations in order to remove
			// them at the end of this posting cycle
			List invalidInvocations = null;

			Iterator iter = observersForEvent.iterator();
			while (iter.hasNext()) {
				ObserverInvocation invocation = (ObserverInvocation)iter.next();

				// fire invocation, detect if anything went wrong
				if (invocation.fire(event) == false) {
					if (invalidInvocations == null) {
						invalidInvocations = new ArrayList();
					}

					invalidInvocations.add(invocation);
				}
			}

			// clear out all invalid invocations
			if (invalidInvocations != null) {
				observersForEvent.removeAll(invalidInvocations);
			}
		}
	}

	/**
	 * @return <code>true</code> if <code>observer</code> could be removed for
	 * all existing subjects, else returns <code>false</code>.
	 */
	public boolean removeObserver(Object observer) {
		boolean didRemove = false;

		if ((_subjects.isEmpty() == false) && (observer != null)) {
			Iterator subjectIter = _subjects.keySet().iterator();
			while (subjectIter.hasNext()) {
				didRemove |= this.removeObserver(observer, (ObserverSubject)subjectIter.next());
			}
		}

		return didRemove;
	}

	/**
	 * @return <code>true</code> if <code>observer</code> could be removed for
	 * the given subject, else returns <code>false</code>.
	 */
	public boolean removeObserver(Object observer, ObserverSubject subject) {
		boolean didRemove = false;

		if ((observer != null) && (subject != null)) {
			Set observersForSubject = this.observersForSubject(subject);

			if ((observersForSubject != null) && (observersForSubject.isEmpty() == false)) {
				// remove all invocations with the given target
				Iterator observerIter = observersForSubject.iterator();
				while (observerIter.hasNext()) {
					ObserverInvocation element = (ObserverInvocation)observerIter.next();
					if (element.getTarget() == observer) {
						observerIter.remove();
						didRemove = true;
					}
				}
			}
		}

		return didRemove;
	}

	/**
	 * add a new subject & its observer queue to the observer map
	 * @param subject the ObserverSubject to be added
	 */
	private void addSubject(ObserverSubject subject) {
		// make sure we don't add a subject twice, losing the old observers
		if (_subjects.get(subject) == null) {
			_subjects.put(subject, new HashSet());
		}
	}

	/**
	 * remove a subject & its observer queue from the observer map
	 * @param subject the ObserverSubject to be removed
	 */
	private void removeSubject(ObserverSubject subject) {
		_subjects.remove(subject);
	}

	/**
	 * @return Set containing all the observers for <code>subject</code>.
	 */
	private Set observersForSubject(ObserverSubject subject) {
		return (Set)_subjects.get(subject);
	}
}
