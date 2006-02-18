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
package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SQLActionVisitor;

/**
 * A specialized query used for communicating of an DataContext with DataRowStore.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
abstract class SnapshotUpdateQuery implements Query {

    static final QueryMetadata defaultMetadata = new QueryMetadata() {

        public String getCacheKey() {
            return null;
        }

        public String getCachePolicy() {
            return null;
        }

        public DataMap getDataMap() {
            return null;
        }

        public DbEntity getDbEntity() {
            return null;
        }

        public int getFetchLimit() {
            return 0;
        }

        public ObjEntity getObjEntity() {
            return null;
        }

        public int getPageSize() {
            return 0;
        }

        public PrefetchTreeNode getPrefetchTree() {
            return null;
        }

        public Procedure getProcedure() {
            return null;
        }

        public boolean isFetchingDataRows() {
            return false;
        }

        public boolean isRefreshingObjects() {
            return false;
        }

        public boolean isResolvingInherited() {
            return false;
        }

    };

    protected Collection deletedIds;
    protected Collection invalidatedIds;
    protected Map modifiedDiffs;
    protected Collection indirectlyModifiedIds;

    public SnapshotUpdateQuery(Object sender, Map modifiedDiffs, Collection deletedIds,
            Collection invalidatedIds, Collection indirectlyModifiedIds) {

        this.modifiedDiffs = modifiedDiffs;
        this.deletedIds = deletedIds;
        this.invalidatedIds = invalidatedIds;
        this.indirectlyModifiedIds = indirectlyModifiedIds;
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        return defaultMetadata;
    }

    /**
     * Does nothing, as this query is not intended for DB execution.
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        // noop
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated since 1.2 super is deprecated
     */
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws an exception.
     */
    public Object getRoot() {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated since 1.2 super is deprecated
     */
    public void setRoot(Object root) {
        throw new UnsupportedOperationException();
    }

    public Map getModifiedDiffs() {
        return (modifiedDiffs != null) ? modifiedDiffs : Collections.EMPTY_MAP;
    }

    public Collection getDeletedIds() {
        return (deletedIds != null) ? deletedIds : Collections.EMPTY_LIST;
    }

    public Collection getInvalidatedIds() {
        return (invalidatedIds != null) ? invalidatedIds : Collections.EMPTY_LIST;
    }

    public Collection getIndirectlyModifiedIds() {
        return (indirectlyModifiedIds != null)
                ? indirectlyModifiedIds
                : Collections.EMPTY_LIST;
    }
}
