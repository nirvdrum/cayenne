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

import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * A handler of OPP (Object Persistence Protocol) messages.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface OPPChannel {

    /**
     * An EventManager subject used by OPPChannel to notify listeners of the graph object
     * changes on the remote end of the channel.
     */
    public static final EventSubject REMOTE_GRAPH_CHANGE_SUBJECT = EventSubject
            .getSubject(OPPChannel.class, "remote-graph-change");

    /**
     * Returns an EventManager that associated with this channel. Channel may return null
     * if EventManager is not available for any reason.
     */
    EventManager getEventManager();

    /**
     * Processes SelectMessage returning a result as list.
     */
    List onSelectQuery(SelectMessage message);

    /**
     * Processes an UpdateMessage returning update counts.
     */
    int[] onUpdateQuery(UpdateMessage message);

    /**
     * Processes a generic query message that can contain both updates and selects.
     */
    QueryResponse onGenericQuery(GenericQueryMessage message);

    /**
     * Processes CommitMessage returning a GraphDiff that describes changes to objects
     * made by the handler as a result of commit operation. Such changes can include
     * generated ObjectIds, any server-side commit logic, etc.
     */
    GraphDiff onCommit(CommitMessage message);

    /**
     * Processes BootstrapMessage returning EntityResolver with client ORM information.
     */
    EntityResolver onBootstrap(BootstrapMessage message);
}