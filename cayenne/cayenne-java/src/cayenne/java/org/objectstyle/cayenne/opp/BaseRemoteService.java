/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.opp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.service.ClientServerChannel;
import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * A generic implementation of an OPPRemoteService. Subclasses can be customized to work
 * with different remoting mechanisms, such as Hessian or JAXRPC.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class BaseRemoteService implements OPPRemoteService {

    private static final Logger logObj = Logger.getLogger(BaseRemoteService.class);

    public static final String EVENT_BRIDGE_FACTORY_PROPERTY = "cayenne.OPPRemoteService.EventBridge.factory";

    protected Map sessionChannels;
    protected Map sharedSessions;

    protected DataDomain domain;

    protected String eventBridgeFactoryName;
    protected Map eventBridgeParameters;

    public String getEventBridgeFactoryName() {
        return eventBridgeFactoryName;
    }

    public Map getEventBridgeParameters() {
        return eventBridgeParameters != null ? Collections
                .unmodifiableMap(eventBridgeParameters) : Collections.EMPTY_MAP;
    }

    /**
     * A method that sets up a service, initializing Cayenne stack. Should be invoked by
     * subclasses from their appropriate service lifecycle methods.
     */
    protected void initService(Map properties) throws CayenneRuntimeException {

        // start Cayenne service
        logObj.debug(this.getClass().getName() + " is starting");

        initCayenneStack(properties);
        initEventBridgeParameters(properties);

        this.sessionChannels = new HashMap();
        this.sharedSessions = new HashMap();

        logObj.debug(getClass().getName() + " started");
    }

    /**
     * Shuts down this service. Should be invoked by subclasses from their appropriate
     * service lifecycle methods.
     */
    protected void destroyService() {
        this.sessionChannels = null;
        this.sharedSessions = null;

        logObj.debug(getClass().getName() + " destroyed");
    }

    public OPPRemoteSession establishSession() {
        logObj.debug("Session requested by client");

        OPPRemoteSession session = createSession(false);

        logObj.debug("Established client session: " + session);
        return session;
    }

    public OPPRemoteSession establishSharedSession(String name) {
        logObj.debug("Shared session requested by client. Group name: " + name);

        if (name == null) {
            throw new CayenneRuntimeException("Invalid shared session name: " + name);
        }

        OPPRemoteSession session;

        synchronized (sessionChannels) {
            session = (OPPRemoteSession) sharedSessions.get(name);

            if (session == null || sessionChannels.get(session.getSessionId()) == null) {
                session = createSession(true);
                session.setName(name);
                logObj.debug("Created new shared session:" + session);
            }
            else {
                logObj.debug("Found existing shared session:" + session);
            }

            sharedSessions.put(name, session);
        }

        return session;
    }

    public Object processMessage(String sessionId, OPPMessage command) throws Throwable {

        logObj.debug("processMessage, sessionId: " + sessionId);

        OPPChannel handler;
        synchronized (sessionChannels) {
            handler = (OPPChannel) sessionChannels.get(sessionId);
        }

        if (handler == null) {
            throw new CayenneRuntimeException("Invalid sessionId: " + sessionId);
        }

        // intercept and log exceptions
        try {
            return command.dispatch(handler);
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            logObj.info("error processing message", th);
            throw th;
        }
    }

    OPPRemoteSession createSession(boolean enableEvents) {

        // do not add EventBridge to the session if 'enableEvents' is false

        String id = makeId();
        OPPRemoteSession session = (enableEvents) ? new OPPRemoteSession(
                id,
                eventBridgeFactoryName,
                eventBridgeParameters) : new OPPRemoteSession(id);

        // block server-side channel events - clients will communicate their changes in a
        // peer-to-peer fashion.
        OPPChannel channel = new ClientServerChannel(domain, false);

        // TODO (Andrus, 10/15/2005) This will result in a memory leak as there is no
        // session timeout; must attach context to the HttpSession. Not entirely sure how
        // this would work with Hessian. One thing I found so far is that getting thread
        // HttpRequest can be done by calling "ServiceContext.getRequest()"

        // or create our own TimeoutMap ... it will be useful in million other places

        synchronized (sessionChannels) {
            sessionChannels.put(session.getSessionId(), channel);
        }

        return session;
    }

    String makeId() {
        byte[] bytes = IDUtil.pseudoUniqueByteSequence(32);

        // use safe encoding... not that it matters to Hessian, but it is more readable
        // this way..
        final String digits = "0123456789ABCDEF";

        StringBuffer buffer = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            buffer.append(digits.charAt((b >>> 4) & 0xF));
            buffer.append(digits.charAt(b & 0xF));
        }

        return buffer.toString();
    }

    /**
     * Sets up Cayenne stack.
     */
    protected void initCayenneStack(Map properties) {
        Configuration cayenneConfig = new DefaultConfiguration(
                Configuration.DEFAULT_DOMAIN_FILE);

        try {
            cayenneConfig.initialize();
            cayenneConfig.didInitialize();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error starting Cayenne", ex);
        }

        // TODO (Andrus 10/15/2005) this assumes that mapping has a single domain...
        // do something about multiple domains
        this.domain = cayenneConfig.getDomain();
    }

    /**
     * Initializes EventBridge parameters for remote clients peer-to-peer communications.
     */
    protected void initEventBridgeParameters(Map properties) {
        String eventBridgeFactoryName = (String) properties
                .get(BaseRemoteService.EVENT_BRIDGE_FACTORY_PROPERTY);

        if (eventBridgeFactoryName != null) {

            Map eventBridgeParameters = new HashMap(properties);
            eventBridgeParameters.remove(BaseRemoteService.EVENT_BRIDGE_FACTORY_PROPERTY);

            this.eventBridgeFactoryName = eventBridgeFactoryName;
            this.eventBridgeParameters = eventBridgeParameters;
        }
    }
}
