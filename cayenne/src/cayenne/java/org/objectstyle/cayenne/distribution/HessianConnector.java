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
package org.objectstyle.cayenne.distribution;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import org.objectstyle.cayenne.client.CayenneClientException;
import org.objectstyle.cayenne.util.Util;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianProtocolException;

/**
 * A CayenneConnector that establishes connection to a remotely deployed HessianService.
 * It supports HTTP BASIC authentication. HessianConnector uses Hessian binary web service
 * protocol working over HTTP. For more info on Hessian see Cauch site at <a
 * href="http://www.caucho.com/resin-3.0/protocols/hessian.xtp">http://www.caucho.com/resin-3.0/protocols/hessian.xtp</a>.
 * HessianConnector supports logging of message traffic via Jakarta commons-logging API.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class HessianConnector extends BaseConnector {

    protected String url;
    protected String userName;
    protected String password;
    protected String sessionId;

    protected HessianService service;

    /**
     * A utility method that clones an object using Hessian serialization/deserialization
     * mechanism which is different from default Java serialization.
     */
    public static Object cloneViaHessianSerialization(Serializable object)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        HessianOutput out = new HessianOutput(bytes);
        out.writeObject(object);

        byte[] data = bytes.toByteArray();

        HessianInput in = new HessianInput(new ByteArrayInputStream(data));
        return in.readObject();
    }

    /**
     * A shortcut for HessianConnector(String,String,String) used when no HTTP basic
     * authentication is required.
     */
    public HessianConnector(String url) {
        this(url, null, null);
    }

    /**
     * Creates a HessianConnector initializing it with a service URL. User name and
     * password are needed only if basic authentication is used. Otherwise they can be
     * null. URL on the other hand is required. Null URL would cause an
     * IllegalArgumentException.
     */
    public HessianConnector(String url, String userName, String password) {
        if (url == null) {
            throw new IllegalArgumentException("URL of Cayenne service is null.");
        }

        this.url = url;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Returns a URL of Cayenne service used by this connector.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns user name that is used for basic authentication when connecting to the
     * cayenne server.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns password that is used for basic authentication when connecting to the
     * cayenne server.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Establishes server session if needed.
     */
    protected void beforeSendMessage(ClientMessage message) throws CayenneClientException {
        // for now only support session-based communications...
        if (sessionId == null) {
            connect();
        }
    }

    /**
     * Sends a message to remote Cayenne Hessian service.
     */
    protected Object doSendMessage(ClientMessage message) throws CayenneClientException {
        try {
            return service.processMessage(sessionId, message);
        }
        catch (Throwable th) {
            th = unwindThrowable(th);
            String errorMessage = buildExceptionMessage("Remote error", th);
            throw new CayenneClientException(errorMessage, th);
        }
    }

    /**
     * Establishes a session with remote service.
     */
    protected synchronized void connect() throws CayenneClientException {
        if (this.sessionId != null) {
            return;
        }

        long t0 = 0;
        if (logger.isInfoEnabled()) {
            t0 = System.currentTimeMillis();
            StringBuffer log = new StringBuffer("Connecting to [");
            if (userName != null) {
                log.append(userName);

                if (password != null) {
                    log.append(":*******");
                }

                log.append("@");
            }

            log.append(url);
            log.append("]");
            logger.info(log.toString());
        }

        // init service proxy...
        HessianProxyFactory factory = new HessianProxyFactory();
        factory.setUser(userName);
        factory.setPassword(password);
        try {
            this.service = (HessianService) factory.create(HessianService.class, url);
        }
        catch (Throwable th) {
            th = unwindThrowable(th);
            String message = buildExceptionMessage("URL error", th);
            throw new CayenneClientException(message, th);
        }

        // create server session...
        try {
            this.sessionId = service.establishSession();

            if (logger.isInfoEnabled()) {
                long time = System.currentTimeMillis() - t0;
                logger.info("=== Connected, session id: "
                        + sessionId
                        + " - took "
                        + time
                        + " ms.");
            }
        }
        catch (Throwable th) {
            th = unwindThrowable(th);
            String message = buildExceptionMessage(
                    "Error establishing remote session",
                    th);
            throw new CayenneClientException(message, th);
        }
    }

    String buildExceptionMessage(String message, Throwable th) {

        StringBuffer buffer = new StringBuffer(message);
        buffer.append(". URL - ").append(url);

        String thMessage = th.getMessage();
        if (!Util.isEmptyString(thMessage)) {
            buffer.append("; CAUSE - ").append(thMessage);
        }

        return buffer.toString();
    }

    /**
     * Utility method to get exception cause. Implements special handling of Hessian
     * exceptions.
     */
    Throwable unwindThrowable(Throwable th) {
        if (th instanceof HessianProtocolException) {
            Throwable cause = ((HessianProtocolException) th).getRootCause();

            if (cause != null) {
                return unwindThrowable(cause);
            }
        }
        else if (th instanceof HessianRuntimeException) {
            Throwable cause = ((HessianRuntimeException) th).getRootCause();

            if (cause != null) {
                return unwindThrowable(cause);
            }
        }

        return Util.unwindException(th);
    }
}