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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;
import org.javagroups.Channel;
import org.javagroups.JChannel;
import org.javagroups.Message;
import org.javagroups.MessageListener;
import org.javagroups.blocks.PullPushAdapter;

/**
 * Implementation of EventBridge that passes and receives events via JavaGroups 
 * communication software.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class JavaGroupsBridge extends EventBridge implements MessageListener {
    private static Logger logObj = Logger.getLogger(JavaGroupsBridge.class);

    public static final String MCAST_ADDRESS_DEFAULT = "228.0.0.5";
    public static final String MCAST_PORT_DEFAULT = "22222";

    public static final String MCAST_ADDRESS_PROPERTY = "cayenne.javagroupsbridge.mcast.address";
    public static final String MCAST_PORT_PROPERTY = "cayenne.javagroupsbridge.mcast.port";

    /**
     * Defines a property for JavaGroups XML configuration file. Example file can be found at
     * <a href="http://www.filip.net/javagroups/javagroups-protocol.xml">http://www.filip.net/javagroups/javagroups-protocol.xml</a>.
     */
    public static final String JGROUPS_CONFIG_URL_PROPERTY =
        "javagroupsbridge.config.url";

    // TODO: Meaning of "state" in JGroups is not yet clear to me
    protected byte[] state;

    protected Channel channel;
    protected PullPushAdapter adapter;
    protected Map properties;

    /**
     * Creates new instance of JavaGroupsBridge.
     * 
     * @param localSubject an EventSubject for the local EventManager.
     * @param properties a map of properties defining bridge configuration parameters. Supported 
     * property values (also defined as static variables of JavaGroupsBridge): javagroupsbridge.mcast.address, 
     * javagroupsbridge.mcast.port, javagroupsbridge.config.url.
     */
    public JavaGroupsBridge(EventSubject localSubject, Map properties) {
        this(localSubject, convertToExternalSubject(localSubject), properties);
    }

    public JavaGroupsBridge(
        EventSubject localSubject,
        String externalSubject,
        Map properties) {

        super(localSubject, externalSubject);

        // prevent any further checks for nulls
        this.properties = (properties != null) ? properties : Collections.EMPTY_MAP;
    }

    public byte[] getState() {
        return state;
    }

    public void setState(byte[] state) {
        this.state = state;
    }

    /**
     * Implementation of org.javagroups.MessageListener - a callback method to process
     * incoming messages.
     */
    public void receive(Message message) {
        try {
            if (logObj.isDebugEnabled()) {
                logObj.debug("Received Message from: " + message.getSrc());
            }

            CayenneEvent event = messageObjectToEvent((Serializable) message.getObject());
            if (event != null) {
                if (logObj.isDebugEnabled()) {
                    logObj.debug("Received CayenneEvent: " + event.getClass().getName());
                }

                onExternalEvent(event);
            }
        } catch (Exception ex) {
            logObj.info("Exception while processing message: ", ex);
        }

    }

    protected void startupExternal() throws Exception {
        // TODO: need to do more research to figure out the best default transport settings
        // to avoid fragmentation, etc.

        // if config file is set as a property, use it, otherwise use a default
        // set of properties, trying to configure multicast address and port

        String configURL = (String) properties.get(JGROUPS_CONFIG_URL_PROPERTY);
        if (configURL != null) {
            logObj.debug("creating channel with configuration from " + configURL);
            channel = new JChannel(configURL);
        } else {
            String configString = buildConfigString(properties);
            logObj.debug("creating channel with properties: " + configString);
            channel = new JChannel(configString);
        }

        // Important - discard messages from self
        channel.setOpt(Channel.LOCAL, Boolean.FALSE);
        channel.connect(externalSubject);
        logObj.debug("channel connected.");

        if (receivesExternalEvents()) {
            adapter = new PullPushAdapter(channel, this);
        }
    }

    /**
     * Creates JavaGroups configuration String, obtgaining
     * multicast port and address from properties if possible. 
     */
    protected String buildConfigString(Map properties) {
        String address = (String) properties.get(MCAST_ADDRESS_PROPERTY);
        if (address == null) {
            address = MCAST_ADDRESS_DEFAULT;
        }

        String port = (String) properties.get(MCAST_PORT_PROPERTY);
        if (port == null) {
            port = MCAST_PORT_DEFAULT;
        }

        return "UDP(mcast_addr="
            + address
            + ";mcast_port="
            + port
            + ";ip_ttl=32):"
            + "PING(timeout=3000;num_initial_members=6):"
            + "FD(timeout=3000):"
            + "VERIFY_SUSPECT(timeout=1500):"
            + "pbcast.NAKACK(gc_lag=10;retransmit_timeout=600,1200,2400,4800):"
            + "pbcast.STABLE(desired_avg_gossip=10000):"
            + "FRAG:"
            + "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;"
            + "shun=true;print_local_addr=true)";
    }

    protected void shutdownExternal() throws Exception {
        try {
            if (adapter != null) {
                adapter.stop();
            }

            channel.close();
        } finally {
            adapter = null;
            channel = null;
        }
    }

    protected void sendExternalEvent(CayenneEvent localEvent) throws Exception {
        logObj.debug("Sending event remotely: " + localEvent);
        Message message = new Message(null, null, eventToMessageObject(localEvent));
        channel.send(message);
    }

    /**
     * Converts CayenneEvent to a serializable object that will be sent via JMS. 
     * Default implementation simply returns the event, but subclasses can customize
     * this behavior.
     */
    protected Serializable eventToMessageObject(CayenneEvent event) throws Exception {
        return event;
    }

    /**
     * Converts a Serializable instance to CayenneEvent. Returns null if the object
     * is not supported. Default implementation simply tries to cast the object to
     * CayenneEvent, but subclasses can customize this behavior.
     */
    protected CayenneEvent messageObjectToEvent(Serializable object) throws Exception {
        return (object instanceof CayenneEvent) ? (CayenneEvent) object : null;
    }
}
