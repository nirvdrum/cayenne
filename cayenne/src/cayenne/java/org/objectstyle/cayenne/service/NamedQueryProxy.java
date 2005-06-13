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

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.ParameterizedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SQLActionVisitor;

/**
 * A reference to a named parameterized query stored in Cayenne mapping. The actual query
 * is resolved during the routing phase and is used to build a SQLAction.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class NamedQueryProxy implements Query {

    protected String name;
    protected Map parameters;

    protected Level loggingLevel;

    public NamedQueryProxy(String name, Map parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Level getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(Level loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public Object getRoot() {
        throw new CayenneRuntimeException("'getRoot' is not supported by this query: "
                + this);
    }

    public void setRoot(Object root) {
        throw new CayenneRuntimeException("'setRoot' is not supported by this query: "
                + this);
    }

    /**
     * Throws an exception as query execution is expected to be delegated to a resolved
     * named query during routing phase.
     */
    public SQLAction toSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException(this
                + " doesn't support its own execution. "
                + "It should've been delegated to another "
                + "query during routing phase.");
    }

    /**
     * Resolves a real query for the name and delegates further execution to this query.
     */
    public void routeQuery(QueryRouter router, EntityResolver resolver) {
        Query substituteQuery = substituteQuery(resolver);

        if (substituteQuery == null) {
            throw new CayenneRuntimeException("Can't find named query for name '"
                    + getName()
                    + "'");
        }

        substituteQuery.routeQuery(router, resolver);
    }

    /**
     * Locates and initializes a substitution query.
     */
    protected Query substituteQuery(EntityResolver resolver) {
        Query query = resolver.lookupQuery(getName());

        if (parameters != null
                && !parameters.isEmpty()
                && query instanceof ParameterizedQuery) {

            query = ((ParameterizedQuery) query).createQuery(parameters);
        }

        return query;
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", name).toString();
    }
}
