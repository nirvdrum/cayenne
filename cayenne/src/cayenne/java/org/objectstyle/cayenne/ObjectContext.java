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

import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.query.QueryExecutionPlan;

/**
 * A Cayenne object facade to a persistence store. Instances of ObjectContext are used in
 * application code to access Cayenne persistence features.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface ObjectContext extends Serializable {

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
     * Creates a new persistent object scheduled to be inserted on next commit.
     */
    Persistent newObject(Class persistentClass);

    /**
     * Schedules a persistent object for deletion on next commit.
     */
    void deleteObject(Persistent object);

    /**
     * A callback method that is invoked whenver a persistent object property is about to
     * be read. Allows ObjectContext to resolve faults on demand.
     */
    void objectWillRead(Persistent object, String property);

    /**
     * A callback method that is invoked whenver a persistent object property is about to
     * be changed. Allows ObjectContext to track object changes.
     */
    void objectWillWrite(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue);

    /**
     * Commits changes made to this ObjectContext persistent objects. If an ObjectContext
     * is a part of an ObjectContext hierarchy, this method call triggers commit all the
     * way to the external data store.
     * 
     * @return GraphDiff that contains changes made to objects during commit. This
     *         includes things like generated ObjectIds, etc.
     */
    GraphDiff commit();

    /**
     * Executes a selecting query, returning a list of persistent objects or data rows.
     */
    List performSelectQuery(QueryExecutionPlan queryPlan);

    /**
     * Executes a non-selecting query returning an array of update counts.
     */
    int[] performUpdateQuery(QueryExecutionPlan queryPlan);

    /**
     * Executes any kind of query providing the result in a form of QueryResponse.
     */
    QueryResponse performGenericQuery(QueryExecutionPlan queryPlan);
}