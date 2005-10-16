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

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * An OPPChannel implementation that accesses an OPP server via an OPPConnection.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class OPPServerChannel implements OPPChannel {

    private static final Log logger = LogFactory.getLog(OPPServerChannel.class);

    protected OPPConnection connection;
    protected EventManager eventManager;

    EventBridge serverEventBridge;

    /**
     * Creates a new channel accessing OPP server via provided connection. Channel will
     * use a muktithreaded EventManager by default.
     */
    public OPPServerChannel(OPPConnection connection) {
        this(connection, new EventManager(2));
    }

    public OPPServerChannel(OPPConnection connection, EventManager eventManager)
            throws CayenneRuntimeException {

        this.connection = connection;
        this.eventManager = eventManager;

        if (eventManager != null) {
            startBridge();
        }
        else {
            logger.info("Channel has no EventManager, ignoring.");
        }
    }

    void startBridge() {
        EventBridge bridge = connection.getServerEventBridge();
        if (bridge != null) {

            try {
                bridge.startup(eventManager, EventBridge.RECEIVE_EXTERNAL, this);
            }
            catch (Exception e) {
                throw new CayenneRuntimeException(
                        "Error starting EventBridge " + bridge,
                        e);
            }

            this.serverEventBridge = bridge;
        }
        else {
            // let it go ...
            logger.info("Remote service doesn't support channel events.");
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public List onSelectQuery(SelectMessage message) {
        return (List) send(message, List.class);
    }

    public int[] onUpdateQuery(UpdateMessage message) {
        return (int[]) send(message, int[].class);
    }

    public QueryResponse onGenericQuery(GenericQueryMessage message) {
        return (QueryResponse) send(message, QueryResponse.class);
    }

    public GraphDiff onSync(SyncMessage message) {
        return (GraphDiff) send(message, GraphDiff.class);
    }

    public EntityResolver onBootstrap(BootstrapMessage message) {
        return (EntityResolver) send(message, EntityResolver.class);
    }

    /**
     * Sends a message via connector, getting a result as an instance of a specific class.
     * 
     * @throws org.objectstyle.cayenne.client.CayenneClientException if an underlying
     *             connector exception occured, or a result is not of expected type.
     */
    protected Object send(OPPMessage message, Class resultClass) {
        Object result = connection.sendMessage(message);

        if (result != null && !resultClass.isInstance(result)) {
            String resultString = new ToStringBuilder(result).toString();
            throw new CayenneRuntimeException("Expected result type: "
                    + resultClass.getName()
                    + ", actual: "
                    + resultString);
        }

        return result;
    }
}