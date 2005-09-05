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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.distribution.GlobalID;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.util.Util;

/**
 * A query that is a reference to a named parameterized query stored in the mapping. The
 * actual query is resolved during execution.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class NamedQuery implements QueryExecutionPlan {

    protected String name;
    protected Map parameters;

    public NamedQuery(String name) {
        this(name, null);
    }

    public NamedQuery(String name, Map parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * Creates NamedQuery with parameters passed as two matching arrays of keys and
     * values.
     */
    public NamedQuery(String name, String[] keys, Object[] values) {
        this.name = name;
        this.parameters = Util.toMap(keys, values);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A callback method invoked by Cayenne during the first phase of execution. Allows
     * query to resolve itself. For example a query can be a "proxy" for another query
     * stored by name in the DataMap. In this method such query would find the actual
     * mapped query and return it to the caller for execution.
     */
    public Query resolve(EntityResolver resolver) {
        Query substituteQuery = substituteQuery(resolver);

        if (substituteQuery == null) {
            throw new CayenneRuntimeException("Can't find named query for name '"
                    + getName()
                    + "'");
        }

        return substituteQuery;
    }

    /**
     * Resolves a real query for the name and delegates further execution to this query.
     */
    public void route(QueryRouter router, EntityResolver resolver) {
        throw new CayenneRuntimeException(this
                + " doesn't support its own routing. "
                + "It should've been delegated to another "
                + "query during resolution phase.");
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
     * Locates and initializes a substitution query.
     */
    protected Query substituteQuery(EntityResolver resolver) {
        Query query = resolver.lookupQuery(getName());

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

                    if (value instanceof GlobalID) {
                        substitute = resolver.convertToObjectID((GlobalID) value);
                    }
                    else if ((value instanceof Persistent)
                            && !(value instanceof DataObject)) {

                        substitute = resolver.convertToObjectID(((Persistent) value)
                                .getGlobalID());
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
     * Overrides toString() outputting a short string with query class and name.
     */
    public String toString() {
        return StringUtils.substringAfterLast(getClass().getName(), ".")
                + ":"
                + getName();
    }
}
