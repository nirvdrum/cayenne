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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.util.Util;

/**
 * A query that selects persistent objects of a certain type. Supports expression
 * qualifier, multiple orderings and a number of other parameters that serve as runtime
 * hints to Cayenne on how to optimize the fetch and result processing.
 * <h3>Select vs. SelectQuery</h2>
 * <p>
 * <em>Select</em> is a newer "portable" version of traditional <em>SelectQuery</em>.
 * It doesn't implement Query (only QueryExecutionPlan) and can be serialized in a neutral
 * format. As a result it is ideal for use in CWS Client Tier (though it can be used in
 * the ORM tier just as well). At the moment there is no plan to deprecate SelectQuery in
 * favor of Select, but this is definitely such possibility in the future.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class Select implements QueryExecutionPlan {

    // possible roots
    protected String entityName;
    protected String objectClass;

    protected String qualifier;
    protected Map parameters;

    protected List orderings;
    protected Set prefetches;

    // exists for Hessian serializer benefit.
    private Select() {

    }

    /**
     * Creates a Select query with entity name and string qualifier expression.
     * 
     * @param entityName a named of mapped Cayenne entity. If null, an exception is
     *            thrown.
     * @param qualifier string expression, compatible with
     *            <em>Expression.fromString(..)</em>. Can be null.
     */
    public Select(String entityName, String qualifier) {
        if (entityName == null) {
            throw new IllegalArgumentException("Null entityName");
        }

        this.entityName = entityName;
        this.qualifier = qualifier;
    }

    /**
     * Creates a Select query with entity name and string qualifier expression.
     * 
     * @param entityName a named of mapped Cayenne entity. If null, an exception is
     *            thrown.
     * @param qualifier qualifier Expression. Can be null.
     */
    public Select(String entityName, Expression qualifier) {
        this(entityName, qualifier != null ? qualifier.toString() : null);
    }

    /**
     * Creates a Select query with object class and string qualifier expression.
     * 
     * @param objectClass a class of persistent objects to fetch. If null, an exception is
     *            thrown. This class must be mapped in Cayenne, otherwise an exception
     *            will be thrown later during query execution.
     * @param qualifier string expression, compatible with
     *            <em>Expression.fromString(..)</em>. Can be null.
     */
    public Select(Class objectClass, String qualifier) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        this.objectClass = objectClass.getName();
        this.qualifier = qualifier;
    }

    /**
     * Creates a Select query with object class andqualifier expression.
     * 
     * @param objectClass a class of persistent objects to fetch. If null, an exception is
     *            thrown. This class must be mapped in Cayenne, otherwise an exception
     *            will be thrown later during query execution.
     * @param qualifier qualifier Expression. Can be null.
     */
    public Select(Class objectClass, Expression qualifier) {
        this(objectClass, qualifier != null ? qualifier.toString() : null);
    }

    /**
     * Puts all entries from parameters map into internal map.
     */
    public void setParameters(Map parameters) {
        if (parameters == null || parameters.isEmpty()) {
            this.parameters = null;
        }
        else {
            this.parameters = new HashMap(parameters);
        }
    }

    /**
     * Sets parameters passed as two matching arrays of keys and values.
     */
    public void setParameters(String[] keys, Object[] values) {
        this.parameters = Util.toMap(keys, values);
    }

    /**
     * Returns an immutable map of parameters.
     */
    public Map getParameters() {
        return parameters != null
                ? Collections.unmodifiableMap(parameters)
                : Collections.EMPTY_MAP;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public String getQualifier() {
        return qualifier;
    }

    /**
     * Returns an unmodifiable list of orderings.
     */
    public List getOrderings() {
        return (orderings != null)
                ? Collections.unmodifiableList(orderings)
                : Collections.EMPTY_LIST;
    }

    /**
     * Adds ordering specification.
     */
    public void addOrdering(Ordering ordering) {
        nonNullOrderings().add(ordering);
    }

    /**
     * Adds a collection of orderings.
     */
    public void addOrderings(Collection orderings) {
        nonNullOrderings().addAll(orderings);
    }

    /** Adds ordering specification to this query orderings. */
    public void addOrdering(String sortPathSpec, boolean isAscending) {
        this.addOrdering(new Ordering(sortPathSpec, isAscending));
    }

    /** Adds ordering specification to this query orderings. */
    public void addOrdering(String sortPathSpec, boolean isAscending, boolean ignoreCase) {
        this.addOrdering(new Ordering(sortPathSpec, isAscending, ignoreCase));
    }

    /**
     * Returns a collection of String paths indicating relationships to objects that are
     * prefetched together with this query.
     */
    public Collection getPrefetches() {
        return (prefetches != null)
                ? Collections.unmodifiableSet(prefetches)
                : Collections.EMPTY_SET;
    }

    /**
     * Adds a relationship path to the internal collection of paths that should be
     * prefetched when the query is executed.
     */
    public void addPrefetch(String relationshipPath) {
        nonNullPrefetches().add(relationshipPath);
    }

    /**
     * Adds all relationship paths in a collection to the internal collection of
     * prefetched paths.
     */
    public void addPrefetches(Collection relationshipPaths) {
        nonNullPrefetches().addAll(relationshipPaths);
    }

    /**
     * Returns a SelectQuery that can be executed in the ORM Tier.
     */
    public Query resolve(EntityResolver resolver) {
        return buildReplacementQuery(resolver);
    }

    /**
     * Delegates routing to a SelectQuery that can be executed in the ORM Tier.
     */
    public void route(QueryRouter router, EntityResolver resolver) {
        buildReplacementQuery(resolver).route(router, resolver);
    }

    /**
     * Thhrows an exception as this is not an executable query.
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException(this
                + " doesn't support its own sql actions. "
                + "It should've been delegated to another "
                + "query during resolution phase.");
    }

    /**
     * Creates an executable query.
     */
    protected Query buildReplacementQuery(EntityResolver resolver) {
        if (entityName == null && objectClass == null) {
            throw new CayenneRuntimeException(
                    "Can't resolve query - both entityName and objectClass are null.");
        }

        if (entityName != null && objectClass != null) {
            throw new CayenneRuntimeException(
                    "Can't resolve query - both entityName and objectClass are set.");
        }

        Expression qualifier = null;
        if (this.qualifier != null) {
            qualifier = Expression.fromString(this.qualifier);

            if (parameters != null) {
                // (Andrus, 09/17/2005) should we ever allow prune == false?
                qualifier = qualifier.expWithParameters(parameters, true);
            }
        }

        SelectQuery query;
        if (entityName != null) {
            query = new SelectQuery(entityName, qualifier);
        }
        else {
            // TODO (Andrus, 09/17/2005) How to tell the difference between client and
            // server class? We should make sure we set a server class here...
            Class rootClass;
            try {
                rootClass = Class.forName(objectClass, true, Thread
                        .currentThread()
                        .getContextClassLoader());
            }
            catch (ClassNotFoundException e) {
                throw new CayenneRuntimeException("Unknown root class: " + objectClass, e);
            }

            query = new SelectQuery(rootClass, qualifier);
        }

        if (orderings != null) {
            query.addOrderings(orderings);
        }

        if (prefetches != null) {
            query.addPrefetches(prefetches);
        }

        return query;
    }

    /**
     * Returns a List that stores orderings, creating such list on demand.
     */
    List nonNullOrderings() {
        if (orderings == null) {
            orderings = new ArrayList();
        }

        return orderings;
    }

    /**
     * Returns a Set that stores prefetches, creating such set on demand.
     */
    Set nonNullPrefetches() {
        if (prefetches == null) {
            prefetches = new HashSet();
        }

        return prefetches;
    }
}
