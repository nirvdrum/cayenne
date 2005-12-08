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

import java.util.List;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.opp.BootstrapMessage;
import org.objectstyle.cayenne.opp.QueryMessage;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.ObjectSelectMessage;
import org.objectstyle.cayenne.opp.SyncMessage;

/**
 * An OPPChannel adapter for CayenneContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO (Andrus 10/17/2005) - make public once it is ready for prime time
class CayenneContextChannel implements OPPChannel {

    protected CayenneContext context;

    CayenneContextChannel(CayenneContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Null context");
        }

        this.context = context;
    }

    /**
     * Returns an EventManager used by the underlying CayenneContext's channel.
     */
    public EventManager getEventManager() {
        return (context.getChannel() != null)
                ? context.getChannel().getEventManager()
                : null;
    }

    // TODO (Andrus, 10/11/2005) not sure if should skip the parent and go directly to its
    // channel for most query messages.

    public List onSelectObjects(ObjectSelectMessage message) {
        return context.performQuery(message.getQuery());
    }

    public QueryResponse onQuery(QueryMessage message) {
        return context.performGenericQuery(message.getQuery());
    }

    public GraphDiff onSync(SyncMessage message) {
        // TODO (Andrus 10/12/2005) ObjectContext=to-ObjectContext channel is unfinished.
        throw new CayenneRuntimeException("Not implemented yet.");

        //        
        // context.internalGraphManager().processSyncWithChild(
        // message.getSenderChanges());
        // return context.doCommitChanges();
    }

    public EntityResolver onBootstrap(BootstrapMessage message) {
        return context.getEntityResolver();
    }
}
