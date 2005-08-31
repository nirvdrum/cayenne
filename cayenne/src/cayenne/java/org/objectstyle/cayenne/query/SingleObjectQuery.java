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

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.distribution.GlobalID;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * A query that returns a single object matching a GlobalID or an ObjectId.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: implement some sort of batch faulting for multiple ids....
public class SingleObjectQuery implements QueryExecutionPlan {

    protected GlobalID globalID;
    protected ObjectId objectID;

    public SingleObjectQuery(GlobalID globalID) {
        if (globalID == null) {
            throw new NullPointerException("Null globalID");
        }

        this.globalID = globalID;
    }

    public SingleObjectQuery(ObjectId objectID) {
        if (objectID == null) {
            throw new NullPointerException("Null objectID");
        }

        this.objectID = objectID;
    }

    public GlobalID getGlobalID() {
        return globalID;
    }

    public ObjectId getObjectID() {
        return objectID;
    }

    public Query resolve(EntityResolver resolver) {
        // TODO: this query wouldn't take advantage of the cache... may need support at
        // the framework level to provide the result from cache or add cache access
        // ability to the query lifecycle API.
        return buildReplacementQuery(resolver);
    }

    public void route(QueryRouter router, EntityResolver resolver) {
        buildReplacementQuery(resolver).route(router, resolver);
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException(this
                + " doesn't support its own sql actions. "
                + "It should've been delegated to another "
                + "query during resolution or routing phase.");
    }

    /**
     * Creates a query that should be run instead of this query.
     */
    protected Query buildReplacementQuery(EntityResolver resolver) {
        if (objectID == null && globalID == null) {
            throw new CayenneRuntimeException(
                    "Can't resolve query - both objectID and globalID are null.");
        }

        ObjectId id = (objectID != null) ? objectID : resolver
                .convertToObjectID(globalID);

        return new SelectQuery(id.getObjectClass(), ExpressionFactory.matchAllDbExp(id
                .getIdSnapshot(), Expression.EQUAL_TO));
    }
}
