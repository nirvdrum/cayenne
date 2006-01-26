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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.query.Query;

/**
 * A Cayenne object facade to a persistent store. Instances of ObjectContext are used in
 * the application code to access Cayenne persistence features.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface ObjectContext extends Serializable {

    public static final EventSubject GRAPH_CHANGED_SUBJECT = EventSubject.getSubject(
            ObjectContext.class,
            "graphChanged");

    public static final EventSubject GRAPH_COMMIT_STARTED_SUBJECT = EventSubject
            .getSubject(ObjectContext.class, "graphCommitStarted");

    public static final EventSubject GRAPH_COMMITTED_SUBJECT = EventSubject.getSubject(
            ObjectContext.class,
            "graphCommitted");

    public static final EventSubject GRAPH_COMMIT_ABORTED_SUBJECT = EventSubject
            .getSubject(ObjectContext.class, "graphCommitAborted");

    public static final EventSubject GRAPH_ROLLEDBACK_SUBJECT = EventSubject.getSubject(
            ObjectContext.class,
            "graphRolledback");

    /**
     * Returns EntityResolver that stores all mapping information accessible by this
     * ObjectContext.
     */
    EntityResolver getEntityResolver();

    /**
     * Returns a collection of objects that are registered with this ObjectContext and
     * have a state PersistenceState.NEW
     */
    Collection newObjects();

    /**
     * Returns a collection of objects that are registered with this ObjectContext and
     * have a state PersistenceState.DELETED
     */
    Collection deletedObjects();

    /**
     * Returns a collection of objects that are registered with this ObjectContext and
     * have a state PersistenceState.MODIFIED
     */
    Collection modifiedObjects();

    /**
     * Returns a collection of MODIFIED, DELETED or NEW objects.
     */
    Collection uncommittedObjects();

    /**
     * Returns an object local to this ObjectContext and matching the ObjectId. If
     * <code>prototype</code> is not null, local object is refreshed with the prototype
     * values.
     * <p>
     * This method can do both "mapping" (i.e. finding an object with the same id in this
     * context) and "synchronization" (i.e. updating the state of the found object with
     * the state of the prototype object).
     * </p>
     */
    Persistent localObject(ObjectId id, Persistent prototype);

    /**
     * Creates a new persistent object scheduled to be inserted on next commit.
     */
    Persistent newObject(Class persistentClass);

    /**
     * Schedules a persistent object for deletion on next commit.
     */
    void deleteObject(Persistent object);

    /**
     * A callback method that child Persistent objects are expected to call from inside
     * the getter before returning a value of a persistent property. Such callback allows
     * ObjectContext to "inflate" unresolved objects on demand.
     */
    void prepareForAccess(Persistent object, String property);

    /**
     * A callback method that child Persistent objects are expected to call from inside
     * the setter after modifying a value of a persistent property.
     */
    void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue);

    /**
     * Performs a cascading commit of changes made in this ObjectContext and its parents.
     */
    void commitChanges();

    /**
     * Notifies parent ObjectContext about the changes made to objects of this
     * ObjectContext since the last sync. No commit occurs as a result.
     */
    void flushChanges();

    /**
     * Resets all changes made to the objects in the ObjectContext, and recursively all
     * parent contexts on the other end of the OPPChannel.
     * <h4>Difference Between "revertChanges" And "rollbackChanges"</h4>
     * <p>
     * Revert is an operation local to this ObjectContext, that doesn't affect parent
     * contexts on the other end of the OPPChannel. "Revert" means "undo all changes made
     * to this context to make the context look like its parent".
     * </p>
     * <p>
     * "Rollback" on the other hand undoes changes in the whole stack of ObjectContexts.
     * So "rollback" means "undo all changes in this context and its parents so that they
     * all look like they did after the last commit".
     * </p>
     */
    void rollbackChanges();

    /**
     * Resets changes made to the objects in the ObjectContext since the last sync with
     * OPPChannel.
     * <h4>Difference Between "revertChanges" And "rollbackChanges"</h4>
     * <p>
     * Revert is an operation local to this ObjectContext, that doesn't affect parent
     * contexts on the other end of the OPPChannel. "Revert" means "undo all changes made
     * to this context to make the context look like its parent".
     * </p>
     * <p>
     * "Rollback" on the other hand undoes changes in the whole stack of ObjectContexts.
     * So "rollback" means "undo all changes in this context and its parents so that they
     * all look like they did after the last commit".
     * </p>
     */
    void revertChanges();

    /**
     * Executes a selecting query, returning a list of persistent objects or data rows.
     */
    List performQuery(Query query);

    /**
     * Executes any kind of query providing the result in a form of QueryResponse.
     */
    QueryResponse performGenericQuery(Query query);

    /**
     * Returns GraphManager that manages object graph associated with this context.
     */
    GraphManager getGraphManager();

    /**
     * Returns an OPPChannel used by this context.
     */
    OPPChannel getChannel();
}