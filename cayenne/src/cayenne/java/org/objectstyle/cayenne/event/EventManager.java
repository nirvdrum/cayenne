/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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

import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.event.DispatchQueue.Dispatch;
import org.objectstyle.cayenne.util.Invocation;
import org.objectstyle.cayenne.util.Util;

/**
 * This class acts as bridge between an Object that wants to inform others about
 * its current state or a change thereof (Publisher) and a list of objects
 * interested in the Subject (Listeners).
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstaette
 * @author Andrei Adamchik
 */
public class EventManager extends Object {
    private static Logger logObj = Logger.getLogger(EventManager.class);

    private static final EventManager defaultManager = new EventManager();

    public static final int DEFAULT_DISPATCH_THREAD_COUNT = 5;

    // keeps weak references to subjects
    protected Map subjects;
    protected List eventQueue;

    /**
     * This method will return the shared 'default' EventManager.
     * 
     * @return EventManager the shared EventManager instance
     */
    public static EventManager getDefaultManager() {
        return defaultManager;
    }

    public EventManager() {
        this(DEFAULT_DISPATCH_THREAD_COUNT);
    }

    /**
     * Default constructor for new EventManager instances, in case you need one.
     */
    public EventManager(int dispatchThreadCount) {
        super();
        this.subjects = Collections.synchronizedMap(new WeakHashMap());
        this.eventQueue = Collections.synchronizedList(new LinkedList());

        if (dispatchThreadCount <= 0) {
            dispatchThreadCount = DEFAULT_DISPATCH_THREAD_COUNT;
        }

        // start dispatch threads
        for (int i = 0; i < dispatchThreadCount; i++) {
            new DispatchThread("EventDispatchThread-" + i).start();
        }
    }

    /**
     * Register	an <code>EventListener</code> for events sent by any sender.
     * 
     * @throws NoSuchMethodException if <code>methodName</code> is not found
     * @see #addListener(EventListener, String, Class, EventSubject, Object)
     */
    public void addListener(
        EventListener listener,
        String methodName,
        Class eventParameterClass,
        EventSubject subject)
        throws NoSuchMethodException {

        this.addListener(listener, methodName, eventParameterClass, subject, null);
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
    public void addListener(
        EventListener listener,
        String methodName,
        Class eventParameterClass,
        EventSubject subject,
        Object sender)
        throws NoSuchMethodException {

        if (listener == null) {
            throw new IllegalArgumentException("Listener must not be null.");
        }

        if (eventParameterClass == null) {
            throw new IllegalArgumentException("Event class must not be null.");
        }

        if (subject == null) {
            throw new IllegalArgumentException("Subject must not be null.");
        }
        
        logObj.debug("adding listener: " + listener.getClass().getName() + "." + methodName);

        Invocation invocation = new Invocation(listener, methodName, eventParameterClass);
        dispatchQueueForSubject(subject, true).addInvocation(invocation, sender);
    }

    /**
     * Unregister the specified listener from all event subjects handled by this
     * <code>EventManager</code> instance.
     * 
     * @param listener the object to be unregistered
     * @return <code>true</code> if <code>listener</code> could be removed for
     * any existing subjects, else returns <code>false</code>.
     */
    public boolean removeListener(EventListener listener) {
        if (listener == null) {
            return false;
        }

        boolean didRemove = false;

        synchronized (subjects) {
            if (!subjects.isEmpty()) {
                Iterator subjectIter = subjects.keySet().iterator();
                while (subjectIter.hasNext()) {
                    didRemove
                        |= this.removeListener(listener, (EventSubject) subjectIter.next());
                }
            }
        }

        return didRemove;
    }

    /**
     * Removes all listeners for a given subject.
     */
    public boolean removeAllListeners(EventSubject subject) {
        if (subject != null) {
            synchronized (subjects) {
                return subjects.remove(subject) != null;
            }
        }

        return false;
    }

    /**
     * Unregister the specified listener for the events about the given subject.
     * 
     * @param listener the object to be unregistered
     * @param subject the subject from which the listener is to be unregistered
     * @return <code>true</code> if <code>listener</code> could be removed for
     * the given subject, else returns <code>false</code>.
     */
    public boolean removeListener(EventListener listener, EventSubject subject) {
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
    public boolean removeListener(
        EventListener listener,
        EventSubject subject,
        Object sender) {

        if (listener == null || subject == null) {
            return false;
        }

        DispatchQueue subjectQueue = dispatchQueueForSubject(subject, false);
        if (subjectQueue == null) {
            return false;
        }

        return subjectQueue.removeInvocations(listener, sender);
    }

    /**
     * Sends an event to all registered objects about a particular subject.
     * Event is sent asynchronously. Event is queued by EventManager, and then
     * this method returns. Later an event dispatch thread wakes up and dispatches 
     * the event.
     * 
     * @param event the event to be posted to the observers
     * @param subject the subject about which observers will be notified
     * @throws IllegalArgumentException if event or subject are null
     */
    public void postEvent(EventObject event, EventSubject subject) {
        postEvent(event, subject, false);
    }

    /**
     * Sends an event to all registered objects about a particular subject.
     * <code>dispatchImmediately</code> argument 
     * 
     * @param event the event to be posted to the observers
     * @param subject the subject about which observers will be notified
     * @param dispatchImmediately control whether the even dispatched from
     * this method, blocking this thread until all listeners have processed 
     * the event, or should be queued for asynchronous notification.
     * 
     * @throws IllegalArgumentException if event or subject are null
     */
    public void postEvent(
        EventObject event,
        EventSubject subject,
        boolean dispatchImmediately) {

        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        if (subject == null) {
            throw new IllegalArgumentException("subject must not be null");
        }

        Dispatch dispatch = new Dispatch(event, subject);

        if (dispatchImmediately) {
            dispatchEvent(dispatch);
        }
        else {

            // add dispatch to the queue and return
            synchronized (eventQueue) {
                eventQueue.add(dispatch);
                eventQueue.notifyAll();
            }
        }
    }

    private void dispatchEvent(Dispatch dispatch) {
        DispatchQueue dispatchQueue = dispatchQueueForSubject(dispatch.subject, false);
        if (dispatchQueue != null) {
            dispatchQueue.dispatchEvent(dispatch);
        }
    }

    // returns a subject's mapping from senders to registered listener invocations
    private DispatchQueue dispatchQueueForSubject(EventSubject subject, boolean create) {
        synchronized (subjects) {
            DispatchQueue listenersStore = (DispatchQueue) subjects.get(subject);
            if (create && listenersStore == null) {
                listenersStore = new DispatchQueue();
                subjects.put(subject, listenersStore);
            }
            return listenersStore;
        }
    }

    final class DispatchThread extends Thread {
        public DispatchThread(String name) {
            super(name);
            setDaemon(true);
            logObj.debug("starting event dispatch thread: " + name);
        }

        public void run() {
            while (true) {

                // get event from the queue, if the queue
                // is empty, just wait
                Dispatch dispatch = null;

                synchronized (EventManager.this.eventQueue) {
                    if (EventManager.this.eventQueue.size() > 0) {
                        dispatch = (Dispatch) EventManager.this.eventQueue.remove(0);
                    }
                    else {
                        try {
                            EventManager.this.eventQueue.wait();
                        }
                        catch (InterruptedException e) {
                            // ignore interrupts...
                            logObj.info("DispatchThread was interrupted.", e);
                        }
                    }
                }

                // dispatch outside of synchronized block
                if (dispatch != null) {

                    // this try/catch is needed to prevent DispatchThread
                    // from dying on dispatch errors
                    try {
                        EventManager.this.dispatchEvent(dispatch);
                    }
                    catch (Throwable th) {
                       // ignoring exception
                       logObj.debug("Event dispatch error, ignoring.", Util.unwindException(th));
                    }
                }
            }
        }
    }
}
