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
package org.objectstyle.cayenne.access.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.objectstyle.cayenne.event.CayenneEvent;

/**
 * Event sent on modification of the SnapshotCache.  
 * 
 * @author Andrei Adamchik
 */
public abstract class SnapshotEvent extends CayenneEvent implements Serializable {

    public static SnapshotEvent createEvent(
        Object source,
        Map modifiedDiffs,
        Map insertedSnapshots,
        Collection deletedIds) {

        RootSnapshotEvent event = new RootSnapshotEvent(source);
        event.modifiedDiffs = modifiedDiffs;
        event.insertedSnapshots = insertedSnapshots;
        event.deletedIds = deletedIds;

        return event;
    }

    public static SnapshotEvent createEvent(Object source, SnapshotEvent rootEvent) {
        return new ChainedSnapshotEvent(source, rootEvent);
    }

    protected SnapshotEvent(Object source) {
        super(source);
    }

    public abstract Object getRootSource();

    public abstract Map modifiedDiffs();

    public abstract Map insertedSnapshots();

    public abstract Collection deletedIds();

    /**
     * Subclass of SnapshotEvent representing an event resent
     * as a result of receiving another event.
     */
    static class ChainedSnapshotEvent extends SnapshotEvent {
        protected SnapshotEvent rootEvent;

        ChainedSnapshotEvent(Object source, SnapshotEvent rootEvent) {
            super(source);
            this.rootEvent = rootEvent;
        }

        /**
          * Returns the source of the event that started this sequence of events.
          */
        public Object getRootSource() {
            return rootEvent.getRootSource();
        }

        public Map modifiedDiffs() {
            return rootEvent.modifiedDiffs();
        }

        public Map insertedSnapshots() {
            return rootEvent.insertedSnapshots();
        }

        public Collection deletedIds() {
            return rootEvent.deletedIds();
        }
    }

    /**
     * Subclass of SnapshotEvent representing an event
     * generated from scratch by the sender.
     */
    static class RootSnapshotEvent extends SnapshotEvent {
        protected Collection deletedIds;
        protected Map modifiedDiffs;
        protected Map insertedSnapshots;

        RootSnapshotEvent(Object source) {
            super(source);
        }

        /**
          * Returns the source of the event that started this sequence of events.
          */
        public Object getRootSource() {
            return getSource();
        }

        public Map modifiedDiffs() {
            return (modifiedDiffs != null) ? modifiedDiffs : Collections.EMPTY_MAP;
        }

        public Map insertedSnapshots() {
            return (insertedSnapshots != null)
                ? insertedSnapshots
                : Collections.EMPTY_MAP;
        }

        public Collection deletedIds() {
            return (deletedIds != null) ? deletedIds : Collections.EMPTY_LIST;
        }
    }
}
