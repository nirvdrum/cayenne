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

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

/**
 * Implementation of EventBridge that passes and receives events via JMS 
 * (Java Messaging Service). JMSBridge uses "publish/subscribe" model for communication
 * with external agents.
 * 
 * @author Andrei Adamchik
 */
public class JMSBridge extends EventBridge implements MessageListener {
    private static Logger logObj = Logger.getLogger(JMSBridge.class);

    protected String topicConnectionFactoryName;

    protected TopicConnection connection;
    protected TopicSession session;
    protected TopicPublisher publisher;
    protected TopicSubscriber subscriber;

    public JMSBridge(
        EventSubject localSubject,
        String externalSubject,
        String topicConnectionFactoryName) {
        super(localSubject, externalSubject);
        this.topicConnectionFactoryName = topicConnectionFactoryName;
    }

    /**
     * JMS MessageListener implementation. Injects received events to the EventManager
     * local event queue.
     */
    public void onMessage(Message message) {

        try {
            if (!(message instanceof ObjectMessage)) {
                if (logObj.isDebugEnabled()) {
                    logObj.debug(
                        "Unsupported message, ignoring: "
                            + message.getJMSCorrelationID());
                }

                return;
            }

            ObjectMessage objectMessage = (ObjectMessage) message;
            CayenneEvent event = messageObjectToEvent(objectMessage.getObject());
            if (event != null) {
                if (logObj.isDebugEnabled()) {
                    logObj.debug(
                        "Received CayenneEvent: "
                            + event.getClass().getName()
                            + ", id: "
                            + message.getJMSCorrelationID());
                }

                eventManager.postEvent(event, localSubject);
            }

        } catch (Exception ex) {
            if (logObj.isDebugEnabled()) {
                logObj.info("Exception while processing message: ", ex);
            } else if (logObj.isInfoEnabled()) {
                logObj.info("Exception while processing message: " + ex.getMessage());
            }
        }
    }

    public String getTopicConnectionFactoryName() {
        return topicConnectionFactoryName;
    }

    /**
     * Starts up JMS machinery for "publish/subscribe" model.
     */
    protected void startupExternal() throws Exception {
        Context jndiContext = new InitialContext();
        TopicConnectionFactory connectionFactory =
            (TopicConnectionFactory) jndiContext.lookup(topicConnectionFactoryName);

        this.connection = connectionFactory.createTopicConnection();
        this.session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = null;

        try {
            topic = (Topic) jndiContext.lookup(externalSubject);
        } catch (NameNotFoundException ex) {
            // can't find topic, try to create it
            topic = topicNotFound(jndiContext, ex);

            if (topic == null) {
                throw ex;
            }
        }

        this.publisher = session.createPublisher(topic);
        this.subscriber = session.createSubscriber(topic);
        this.subscriber.setMessageListener(this);
        this.connection.start();
    }

    /**
     * Attempts to create missing Topic. Since Topic creation is JMS-implementation specific,
     * this task is left to subclasses. Current implementation simply rethrows the exception.
     */
    protected Topic topicNotFound(Context jndiContext, NamingException ex)
        throws Exception {
        throw ex;
    }

    /**
     * Closes all resources used to communicate via JMS.
     */
    protected void shutdownExternal() throws Exception {
        Exception lastException = null;

        try {
            publisher.close();
        } catch (Exception ex) {
            lastException = ex;
        }

        try {
            subscriber.close();
        } catch (Exception ex) {
            lastException = ex;
        }

        try {
            session.close();
        } catch (Exception ex) {
            lastException = ex;
        }

        try {
            connection.close();
        } catch (Exception ex) {
            lastException = ex;
        }

        publisher = null;
        subscriber = null;
        session = null;
        connection = null;
        if (lastException != null) {
            throw lastException;
        }
    }

    protected void sendExternalEvent(CayenneEvent localEvent) throws Exception {
        ObjectMessage message =
            session.createObjectMessage(eventToMessageObject(localEvent));
        publisher.publish(message);
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
