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

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.PersistenceContext;
import org.objectstyle.cayenne.distribution.ClientMessageHandler;
import org.objectstyle.cayenne.distribution.CommitMessage;
import org.objectstyle.cayenne.distribution.NamedQueryMessage;
import org.objectstyle.cayenne.distribution.QueryMessage;
import org.objectstyle.cayenne.query.GenericSelectQuery;

/**
 * A server-side peer of a ClientDataContext. Client messages are processed via callback
 * methods defined in ClientMessageHandler interface.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ServerObjectContext extends ObjectDataContext implements
        ClientMessageHandler {

    ServerObjectContext(PersistenceContext parent) {
        super(parent);
    }

    public Object onCommit(CommitMessage message) {
        // TODO: sync from commit message...

        commitChanges();
        return null;
    }

    public Object onNamedQuery(NamedQueryMessage message) {
        if (message.isSelecting()) {
            return performQuery(message.getQueryName(), message.getParameters(), message
                    .isRefresh());
        }
        else {
            return performNonSelectingQuery(message.getQueryName(), message
                    .getParameters());
        }
    }

    public Object onQuery(QueryMessage message) {
        if (message.isSelecting()) {
            if (message.getQuery() instanceof GenericSelectQuery) {
                return performQuery((GenericSelectQuery) message.getQuery());
            }
            else {
                throw new CayenneRuntimeException(
                        "Expected a GenericSelectQuery for selecting query, got: "
                                + message.getQuery());
            }
        }
        else {
            return performNonSelectingQuery(message.getQuery());
        }
    }
}