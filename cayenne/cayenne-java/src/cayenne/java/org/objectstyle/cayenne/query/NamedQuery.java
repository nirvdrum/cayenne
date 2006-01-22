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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.util.Util;

/**
 * A query that is a reference to a named parameterized query stored in the mapping. The
 * actual query is resolved during execution.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class NamedQuery implements Query {

    protected String name;
    protected String queryName;
    protected Map parameters;

    // using Boolean instead of boolean to implement "trinary" logic - override with
    // refresh, override with no-refresh, no override.
    protected Boolean refreshOverride;

    protected transient int hashCode;

    public NamedQuery(String queryName) {
        this(queryName, null);
    }

    public NamedQuery(String queryName, Map parameters) {
        this.queryName = queryName;
        this.parameters = parameters;
    }

    /**
     * Creates NamedQuery with parameters passed as two matching arrays of keys and
     * values.
     */
    public NamedQuery(String queryName, String[] keys, Object[] values) {
        this.queryName = queryName;
        this.parameters = Util.toMap(keys, values);
    }

    /**
     * Returns the name of this query, which is different from the name this query
     * <strong>points to</strong>.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SelectInfo getSelectInfo(EntityResolver resolver) {
        SelectInfo info = resolveQuery(resolver).getSelectInfo(resolver);

        if (refreshOverride == null) {
            return info;
        }

        SelectInfoWrapper wrapper = new SelectInfoWrapper(info);
        wrapper.override(SelectInfo.REFRESHING_OBJECTS_PROPERTY, refreshOverride);
        return wrapper;
    }

    /**
     * Returns the name of the query this query points to.
     */
    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    /**
     * Resolves a real query for the name and delegates further execution to this query.
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        Query substituteQuery = substituteQuery(resolver);

        substituteQuery.route(router, resolver, substitutedQuery != null
                ? substitutedQuery
                : this);
    }

    /**
     * Throws an exception as query execution is expected to be delegated to a resolved
     * named query during routing phase.
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException(this
                + " doesn't support its own sql actions. "
                + "It should've been delegated to another "
                + "query during resolution phase.");
    }

    /**
     * Returns a query for name, throwing an exception if such query is not mapped in the
     * EntityResolver.
     */
    protected Query resolveQuery(EntityResolver resolver) {
        Query query = resolver.lookupQuery(getQueryName());

        if (query == null) {
            throw new CayenneRuntimeException("Can't find named query for name '"
                    + getQueryName()
                    + "'");
        }

        if (query == this) {
            throw new CayenneRuntimeException("Named query resolves to self: '"
                    + getQueryName()
                    + "'");
        }

        return query;
    }

    /**
     * Locates and initializes a substitution query.
     */
    protected Query substituteQuery(EntityResolver resolver) {
        Query query = resolveQuery(resolver);

        if (query instanceof ParameterizedQuery) {
            // must process the query even if we have no parameters set, so that unused
            // parts of qualifier could be pruned.
            Map parameters = (this.parameters != null)
                    ? this.parameters
                    : Collections.EMPTY_MAP;

            // substitute client-side objects with server-side ObjectIds.

            // TODO: this looks dirty and may need to be revisited once a switch to
            // GlobalId/Persistent is done across the board.
            if (!parameters.isEmpty()) {
                Map substitutes = null;

                Iterator it = parameters.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();

                    Object value = entry.getValue();
                    Object substitute = null;

                    if ((value instanceof Persistent) && !(value instanceof DataObject)) {
                        substitute = ((Persistent) value).getObjectId();
                    }

                    if (substitute != null) {
                        if (substitutes == null) {
                            substitutes = new HashMap(parameters);
                        }

                        substitutes.put(entry.getKey(), substitute);
                    }
                }

                if (substitutes != null) {
                    parameters = substitutes;
                }
            }

            query = ((ParameterizedQuery) query).createQuery(parameters);
        }

        return query;
    }

    /**
     * Returns the root of the named query obtained from the EntityResolver. If such query
     * does not exist (or if it is the same query as this object), null is returned.
     */
    public Object getRoot(EntityResolver resolver) {
        Query query = resolver.lookupQuery(getQueryName());

        if (query == null) {
            return null;
        }

        // sanity check ... there can be an incorrect use of client vs. server
        // EntityResolver resulting in such condition
        if (query == this) {
            return null;
        }

        return query.getRoot(resolver);
    }

    /**
     * @deprecated since 1.2
     */
    public Object getRoot() {
        throw new CayenneRuntimeException("This deprecated method is not implemented");
    }

    /**
     * @deprecated since 1.2
     */
    public void setRoot(Object root) {
        throw new CayenneRuntimeException("This deprecated method is not implemented");
    }

    /**
     * Overrides toString() outputting a short string with query class and name.
     */
    public String toString() {
        return StringUtils.substringAfterLast(getClass().getName(), ".")
                + ":"
                + getName();
    }

    public Boolean getRefreshOverride() {
        return refreshOverride;
    }

    /**
     * Sets whether refreshing behavior of the target query should be overrdiden and if so -
     * what value should be used.
     */
    public void setRefreshOverride(Boolean refreshOverride) {
        this.refreshOverride = refreshOverride;
    }

    /**
     * An object is considered equal to this NamedQuery if it is a NamedQuery with the
     * same queryName and same parameters.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof NamedQuery)) {
            return false;
        }

        NamedQuery query = (NamedQuery) object;

        if (!Util.nullSafeEquals(queryName, query.getQueryName())) {
            return false;
        }

        if (query.parameters == null && parameters == null) {
            return true;
        }

        if (query.parameters == null || parameters == null) {
            return false;
        }

        if (query.parameters.size() != parameters.size()) {
            return false;
        }

        EqualsBuilder builder = new EqualsBuilder();
        Iterator entries = parameters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            Object entryKey = entry.getKey();
            Object entryValue = entry.getValue();

            if (entryValue == null) {
                if (query.parameters.get(entryKey) != null
                        || !query.parameters.containsKey(entryKey)) {
                    return false;
                }
            }
            else {
                // takes care of comparing primitive arrays, such as byte[]
                builder.append(entryValue, query.parameters.get(entryKey));
                if (!builder.isEquals()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Implements a standard hashCode contract considering custom 'equals' implementation.
     */
    public int hashCode() {

        if (this.hashCode == 0) {

            HashCodeBuilder builder = new HashCodeBuilder(13, 17);

            if (queryName != null) {
                builder.append(queryName.hashCode());
            }

            if (parameters != null) {
                Object[] keys = parameters.keySet().toArray();
                Arrays.sort(keys);

                for (int i = 0; i < keys.length; i++) {
                    // HashCodeBuilder will take care of processing object if it
                    // happens to be a primitive array such as byte[]
                    builder.append(keys[i]).append(parameters.get(keys[i]));
                }

            }

            this.hashCode = builder.toHashCode();
            assert hashCode != 0 : "Generated zero hashCode";
        }

        return hashCode;
    }
}
