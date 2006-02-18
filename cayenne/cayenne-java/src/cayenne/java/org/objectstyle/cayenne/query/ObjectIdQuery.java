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

import org.apache.commons.lang.StringUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.util.Util;

/**
 * A query that matches zero or one object or data row corresponding to the ObjectId. Used
 * internally by Cayenne to lookup objects by id.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ObjectIdQuery extends IndirectQuery {

    protected ObjectId objectId;
    protected boolean refreshing;
    protected boolean fetchingDataRows;

    // needed for hessian serialization
    private ObjectIdQuery() {

    }

    /**
     * Creates a refreshing SingleObjectQuery.
     */
    public ObjectIdQuery(ObjectId objectID) {
        this(objectID, false, true);
    }

    public ObjectIdQuery(ObjectId objectID, boolean fetchingDataRows, boolean refreshing) {
        if (objectID == null) {
            throw new NullPointerException("Null objectID");
        }

        this.objectId = objectID;
        this.refreshing = refreshing;
        this.fetchingDataRows = fetchingDataRows;
    }

    /**
     * Returns query metadata object.
     */
    // return metadata without creating replacement, as this is not always possible to
    // create replacement (e.g. temp ObjectId).
    public QueryMetadata getMetaData(final EntityResolver resolver) {
        return new DefaultQueryMetadata() {

            public boolean isRefreshingObjects() {
                return refreshing;
            }

            public ObjEntity getObjEntity() {
                return resolver.lookupObjEntity(objectId.getEntityName());
            }

            public boolean isFetchingDataRows() {
                return fetchingDataRows;
            }
        };
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    protected Query createReplacementQuery(EntityResolver resolver) {
        if (objectId == null) {
            throw new CayenneRuntimeException("Can't resolve query - objectId is null.");
        }

        if (objectId.isTemporary() && !objectId.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("Can't build a query for temporary id: "
                    + objectId);
        }

        SelectQuery query = new SelectQuery(objectId.getEntityName(), ExpressionFactory
                .matchAllDbExp(objectId.getIdSnapshot(), Expression.EQUAL_TO));
        query.setRefreshingObjects(refreshing);
        query.setFetchingDataRows(fetchingDataRows);
        return query;
    }

    /**
     * @deprecated since 1.2
     */
    public Object getRoot() {
        return objectId.getEntityName();
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    /**
     * Overrides toString() outputting a short string with query class and ObjectId.
     */
    public String toString() {
        return StringUtils.substringAfterLast(getClass().getName(), ".") + ":" + objectId;
    }

    /**
     * An object is considered equal to this query if it is also a SingleObjectQuery with
     * an equal ObjectId.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ObjectIdQuery)) {
            return false;
        }

        ObjectIdQuery query = (ObjectIdQuery) object;

        return Util.nullSafeEquals(objectId, query.getObjectId());
    }

    /**
     * Implements a standard hashCode contract considering custom 'equals' implementation.
     */
    public int hashCode() {
        return (objectId != null) ? objectId.hashCode() : 11;
    }
}
