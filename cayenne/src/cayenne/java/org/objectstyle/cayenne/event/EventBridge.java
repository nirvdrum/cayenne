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

import java.util.EventListener;

import org.objectstyle.cayenne.CayenneException;

/**
 * A bridge between Cayenne EventManager and other possible event sources.
 * Prime example of using EventBridge is for routing events dispatched locally by EventManager
 * to the remote JVMs via some transport mechanism (e.g. JMS). EventBridge maintains two 
 * event subjects. A "local" subject - to communicate with local EventManager, and a "remote"
 * subject - to work with an external events interface.
 * 
 * <p>Application can register multiple EventBridge instances with EventManager using
 * static <code>install</code> method. "Outgoing" events will trigger 
 * <code>processOutgoingEvent</code> method. "Incoming" events will be posted under 
 * specified subject via EventManager.</p>
 * 
 * <p>If a subclass needs to prepare itself to receive incoming events it should
 * override <code>init()</code> method.</p>
 * 
 * <p>This class is an example of the <a href="http://en.wikipedia.org/wiki/Bridge_pattern">"bridge"</a> 
 * design pattern, hence the name.
 * </p>
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public abstract class EventBridge implements EventListener {
    protected String externalSubject;
    protected EventSubject localSubject;

    /**
     * Returns a String subject used to post distributed events.
     */
    public String getExternalSubject() {
        return externalSubject;
    }

    /**
     * Returns a subject used for events within the local JVM.
     */
    public EventSubject getLocalSubject() {
        return localSubject;
    }

    /**
     * Installs itself as the bridge between local VM shared EventManager 
     * and remote event mechanism.  Override this method, calling "super" first, 
     * if a concrete implementation needs special setup to start accepting 
     * external events.
     */
    public void install(EventSubject localSubject, String externalSubject)
        throws CayenneException {

        this.externalSubject = externalSubject;
        this.localSubject = localSubject;

        try {
            EventManager.getDefaultManager().addListener(
                this,
                "processOutgoingEvent",
                CayenneEvent.class,
                localSubject);
        }
        catch (NoSuchMethodException e) {
            // this shouldn't happen
            throw new CayenneException(
                "Error registering EventBridge of class '"
                    + this.getClass().getName()
                    + "'.",
                e);
        }
    }

    public void uninstall() {
        EventManager.getDefaultManager().removeListener(this);
    }

    /**
     * Helper method for sucblasses to post an event obtained from a remote source.
     */
    protected void postLocalEvent(CayenneEvent event) {
        EventManager.getDefaultManager().postEvent(event, localSubject);
    }

    /**
     * Sends a distributed event via mechanism implemented by a concrete
     * subclass of DistributedNotificationAdapter.
     */
    public void processOutgoingEvent(CayenneEvent event) {
        if (event.getSource() == this) {
            return;
        }

        sendRemoteEvent(event);
    }

    /**
     * Sends a distributed event. Mechanism is implementation dependent. 
     */
    protected abstract void sendRemoteEvent(CayenneEvent event);
}
