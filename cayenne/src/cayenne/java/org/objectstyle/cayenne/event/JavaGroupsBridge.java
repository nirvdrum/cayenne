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
 */
public class JavaGroupsBridge extends EventBridge implements MessageListener {
    private static Logger logObj = Logger.getLogger(JavaGroupsBridge.class);

    protected byte[] state;
    protected Channel channel;
    protected PullPushAdapter adapter;

    public JavaGroupsBridge(EventSubject localSubject) {
        super(localSubject);
    }

    public JavaGroupsBridge(EventSubject localSubject, String externalSubject) {
        super(localSubject, externalSubject);
    }

    // TODO: Meaning of "state" in JGroups is not yet clear to me

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
        // TODO: using default properties... need to do some
        // *serious* research to figure out the best transport settings

        // at the very minumum we should allow configuring UDP port and address
        channel = new JChannel();

    /*    channel.setOpt(Channel.VIEW, Boolean.FALSE);
        channel.setOpt(Channel.SUSPECT, Boolean.FALSE);
        channel.setOpt(Channel.BLOCK, Boolean.FALSE);
        channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.FALSE); */

        // Important - discard messages from self
        channel.setOpt(Channel.LOCAL, Boolean.FALSE);
        
        logObj.info("connecting...");
        channel.connect(externalSubject);
        logObj.info("connected...");
        
        if (receivesExternalEvents()) {
            adapter = new PullPushAdapter(channel, this);
        }
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
