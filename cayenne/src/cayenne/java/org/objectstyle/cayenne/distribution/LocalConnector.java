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

import org.objectstyle.cayenne.client.CayenneClientException;
import org.objectstyle.cayenne.util.Util;

/**
 * A "local" connector used mainly to enable testing of all parts of a distributed
 * application in a single Java virtual machine. To better reproduce test conditions, it
 * can optionally serialize all messages passed through it, and then pass a deserialized
 * version to the handler.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class LocalConnector extends BaseConnector {

    public static final int NO_SERIALIZATION = 0;
    public static final int JAVA_SERIALIZATION = 1;
    public static final int HESSIAN_SERIALIZATION = 2;

    protected ClientMessageHandler handler;
    protected int serializationPolicy;

    /**
     * Creates LocalConnector with specified handler and no serialization.
     */
    public LocalConnector(ClientMessageHandler handler) {
        this(handler, NO_SERIALIZATION);
    }

    /**
     * Creates a LocalConnector with specified handler and serialization policy. Valid
     * policies are defined as final static int field in this class.
     */
    public LocalConnector(ClientMessageHandler handler, int serializationPolicy) {
        this.handler = handler;

        // convert invalid policy to NO_SER..
        this.serializationPolicy = serializationPolicy == JAVA_SERIALIZATION
                || serializationPolicy == HESSIAN_SERIALIZATION
                ? serializationPolicy
                : NO_SERIALIZATION;
    }

    public boolean isSerializingMessages() {
        return serializationPolicy == JAVA_SERIALIZATION
                || serializationPolicy == HESSIAN_SERIALIZATION;
    }

    public ClientMessageHandler getHandler() {
        return handler;
    }

    /**
     * Does nothing.
     */
    protected void beforeSendMessage(ClientMessage message) {
        // noop
    }

    /**
     * Dispatches a message to an internal handler.
     */
    protected Object doSendMessage(ClientMessage message) throws CayenneClientException {

        ClientMessage processedMessage;

        try {
            switch (serializationPolicy) {
                case HESSIAN_SERIALIZATION:
                    processedMessage = (ClientMessage) HessianConnector
                            .cloneViaHessianSerialization(message);
                    break;

                case JAVA_SERIALIZATION:
                    processedMessage = (ClientMessage) Util
                            .cloneViaSerialization(message);
                    break;

                default:
                    processedMessage = message;
            }

            return processedMessage.onReceive(handler);
        }
        catch (Exception ex) {
            throw new CayenneClientException("Error sending message", ex);
        }

    }
}
