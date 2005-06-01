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
package org.objectstyle.cayenne.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.distribution.ClientMessage;
import org.objectstyle.cayenne.distribution.HessianService;
import org.objectstyle.cayenne.util.IDUtil;

import com.caucho.services.server.Service;

/**
 * A default implementation of HessianService service protocol. Supports client sessions.
 * For more info on Hessian see http://www.caucho.com/resin-3.0/protocols/hessian.xtp. See
 * {@link org.objectstyle.cayenne.distribution.HessianService}for deployment
 * configuration examples.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class HessianServiceHandler implements HessianService, Service {

    private static final Logger logObj = Logger.getLogger(HessianServiceHandler.class);

    protected Map commandHandlers;
    protected DataDomain domain;

    /**
     * Hessian service lifecycle method that performs Cayenne initialization.
     */
    public void init(ServletConfig config) throws ServletException {

        // start Cayenne service
        logObj.debug("CayenneHessianService is starting");

        Configuration cayenneConfig = new DefaultConfiguration(
                Configuration.DEFAULT_DOMAIN_FILE);

        try {
            cayenneConfig.initialize();
            cayenneConfig.didInitialize();
        }
        catch (Exception ex) {
            throw new ServletException("Error starting Cayenne", ex);
        }

        // TODO: handle multiple domains..
        this.domain = cayenneConfig.getDomain();
        this.commandHandlers = Collections.synchronizedMap(new HashMap());
        logObj.debug("CayenneHessianService started");
    }

    /**
     * Hessian Service lifecycle method.
     */
    public void destroy() {
        logObj.debug("CayenneHessianService destroyed");
        this.commandHandlers = null;
    }

    public String establishSession(String userName, String password) {
        logObj.debug("CayenneHessianService - session requested by client");

        // TODO: for now ignore user name/password...

        String id = makeId();

        synchronized (commandHandlers) {
            commandHandlers.put(id, makeHandler());
        }

        logObj.debug("CayenneHessianService - established client session: " + id);
        return id;
    }

    public Object processCommand(String sessionId, ClientMessage command) {
        logObj.debug("invokeRemote, sessionId: " + sessionId);

        CommandHandler handler;
        synchronized (commandHandlers) {
            handler = (CommandHandler) commandHandlers.get(sessionId);
        }

        // TODO: expire sessions...
        if (handler == null) {
            throw new CayenneRuntimeException("Invalid sessionId: " + sessionId);
        }

        return handler.processCommand(command);
    }

    /**
     * Assembles default handler.
     */
    private CommandHandler makeHandler() {
        DataContext dataContext = this.domain.createDataContext();
        ObjectContext objectContext = new ServerObjectContext(dataContext);
        return new CommandHandler(objectContext);
    }

    private String makeId() {
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
}