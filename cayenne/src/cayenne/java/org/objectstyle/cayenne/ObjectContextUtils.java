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
package org.objectstyle.cayenne;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.graph.GraphEventListener;
import org.objectstyle.cayenne.opp.OPPChannel;

/**
 * Various ObjectContext-related utility methods.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ObjectContextUtils {

    /**
     * Utility method that wraps a GraphChangeHandler into GraphEventListener, so that any
     * GraphEvents posted by any ObjectContext that shares an OPPChannel, are channeled to
     * this handler.
     * <p>
     * <b>WARNING: Cayenne event mechanism does not require explicit unregistering of
     * listeners. It uses weak references and would remove the listeners once they go out
     * of scope. This means that you must hold on to the returned listener (e.g. store it
     * as an instance variable). Otherwise it may be removed prematurely.</b>
     * </p>
     * 
     * @return null if an OPPChannel doesn't have an EventManager and therefore does not
     *         support events, or an event listener that was created to pass events to the
     *         GraphChangeHandler.
     */
    public static GraphEventListener listenForContextEvents(
            OPPChannel channel,
            final GraphChangeHandler handler) {

        GraphEventListener listener = new GraphEventListener() {

            public void graphChanged(GraphEvent event) {
                if (event.getDiff() != null) {
                    event.getDiff().apply(handler);
                }
            }
        };
        return listenForContextEvents(channel, listener) ? listener : null;
    }

    /**
     * Utility method that wraps a GraphChangeHandler into GraphEventListener, so that any
     * GraphEvents posted by the specified ObjectContext are channeled to this handler.
     * <p>
     * <b>WARNING: Cayenne event mechanism does not require explicit unregistering of
     * listeners. It uses weak references and would remove the listeners once they go out
     * of scope. This means that you must hold on to the returned listener (e.g. store it
     * as an instance variable). Otherwise it may be removed prematurely.</b>
     * </p>
     * 
     * @return null if an ObjectContext doesn't have an EventManager or explicitly does
     *         not support events, or an event listener that was created to pass events to
     *         the GraphChangeHandler.
     */
    public static GraphEventListener listenForContextEvents(
            ObjectContext context,
            final GraphChangeHandler handler) {

        GraphEventListener listener = new GraphEventListener() {

            public void graphChanged(GraphEvent event) {
                if (event.getDiff() != null) {
                    event.getDiff().apply(handler);
                }
            }
        };
        return listenForContextEvents(context, listener) ? listener : null;
    }

    /**
     * Utility method that sets up a GraphChangeListener to be notified when GraphEvents
     * occur in any ObjectContext that shares a given OPPChannel.
     * 
     * @return false if an OPPChannel doesn't have an EventManager and therefore does not
     *         support events.
     */
    public static boolean listenForContextEvents(
            OPPChannel channel,
            GraphEventListener listener) {

        EventManager manager = channel.getEventManager();

        if (manager == null) {
            return false;
        }

        manager.addListener(
                listener,
                "graphChanged",
                GraphEvent.class,
                ObjectContext.GRAPH_CHANGE_SUBJECT);

        return true;
    }

    /**
     * Utility method that sets up a GraphEventListener to be notified when GraphEvents
     * occur in a specific ObjectContext.
     * 
     * @return false if an ObjectContext doesn't have an EventManager or does not support
     *         events explicitly.
     */
    public static boolean listenForContextEvents(
            ObjectContext context,
            GraphEventListener listener) {

        if (context.getChannel() == null
                || context.getChannel().getEventManager() == null) {
            return false;
        }

        if (context instanceof CayenneContext
                && !((CayenneContext) context).isGraphEventsEnabled()) {
            return false;
        }

        context.getChannel().getEventManager().addListener(
                listener,
                "graphChanged",
                GraphEvent.class,
                ObjectContext.GRAPH_CHANGE_SUBJECT,
                context);

        return true;
    }

    /**
     * Utility method that sets up a GraphChangeListener to be notified when OPPChannel
     * posts an event.
     * 
     * @return false if an OPPChannel doesn't have an EventManager and therefore does not
     *         support events.
     */
    public static boolean listenForChannelEvents(
            OPPChannel channel,
            GraphEventListener listener) {

        EventManager manager = channel.getEventManager();

        if (manager == null) {
            return false;
        }

        manager.addListener(
                listener,
                "graphChanged",
                GraphEvent.class,
                OPPChannel.REMOTE_GRAPH_CHANGE_SUBJECT,
                channel);

        return true;
    }

    /**
     * Utility method that wraps a GraphChangeHandler into GraphEventListener, so that any
     * GraphEvents posted by the OPPChannel re channeled to this handler.
     * <p>
     * <b>WARNING: Cayenne event mechanism does not require explicit unregistering of
     * listeners. It uses weak references and would remove the listeners once they go out
     * of scope. This means that you must hold on to the returned listener (e.g. store it
     * as an instance variable). Otherwise it may be removed prematurely.</b>
     * </p>
     * 
     * @return null if an OPPChannel doesn't have an EventManager and therefore does not
     *         support events, or an event listener that was created to pass events to the
     *         GraphChangeHandler.
     */
    public static GraphEventListener listenForChannelEvents(
            OPPChannel channel,
            final GraphChangeHandler handler) {

        GraphEventListener listener = new GraphEventListener() {

            public void graphChanged(GraphEvent event) {
                if (event.getDiff() != null) {
                    event.getDiff().apply(handler);
                }
            }
        };
        return listenForChannelEvents(channel, listener) ? listener : null;
    }

    // not for instantiation
    private ObjectContextUtils() {
    }
}
