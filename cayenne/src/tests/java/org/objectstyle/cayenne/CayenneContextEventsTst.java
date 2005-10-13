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

import junit.framework.TestCase;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.graph.GraphEventListener;
import org.objectstyle.cayenne.graph.MockGraphEventListener;
import org.objectstyle.cayenne.opp.CommitMessage;
import org.objectstyle.cayenne.opp.MockOPPChannel;
import org.objectstyle.cayenne.opp.OPPChannel;

public class CayenneContextEventsTst extends TestCase {

    public void testDispatchEventsEnabled() {
        final EventManager manager = new EventManager(0);
        OPPChannel channel = new MockOPPChannel() {

            public EventManager getEventManager() {
                return manager;
            }

            public GraphDiff onCommit(CommitMessage message) {
                return new CompoundDiff();
            }
        };

        CayenneContext contextWithEvents = new CayenneContext(channel, true, true);
        assertTrue(contextWithEvents.isChangeEventsEnabled());
        assertTrue(contextWithEvents.isSyncEventsEnabled());

        final boolean[] flags1 = new boolean[1];
        GraphEventListener listener1 = new MockGraphEventListener() {

            public void graphChanged(GraphEvent event) {
                flags1[0] = true;
            }
        };

        ObjectContextUtils.listenForContextEvents(channel, listener1);

        // dummy change after commit started
        contextWithEvents.internalGraphManager().nodePropertyChanged(
                new Object(),
                "x",
                "y",
                "z");

        assertTrue(flags1[0]);
    }

    public void testDispatchEventsDisabled() {
        final EventManager manager = new EventManager(0);
        OPPChannel channel = new MockOPPChannel() {

            public EventManager getEventManager() {
                return manager;
            }

            public GraphDiff onCommit(CommitMessage message) {
                return new CompoundDiff();
            }
        };

        CayenneContext contextWithoutEvents = new CayenneContext(channel, false, false);
        assertFalse(contextWithoutEvents.isChangeEventsEnabled());
        assertFalse(contextWithoutEvents.isSyncEventsEnabled());

        final boolean[] flags2 = new boolean[1];
        GraphEventListener listener2 = new MockGraphEventListener() {

            public void graphChanged(GraphEvent event) {
                flags2[0] = true;
            }
        };
        ObjectContextUtils.listenForContextEvents(channel, listener2);

        // dummy change...
        contextWithoutEvents.internalGraphManager().nodePropertyChanged(
                new Object(),
                "x",
                "y",
                "z");

        assertFalse(flags2[0]);
    }
}