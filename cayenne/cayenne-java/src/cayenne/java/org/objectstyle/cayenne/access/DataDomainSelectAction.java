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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainSelectAction {

    DataDomain domain;
    DataContext context;
    Query query;
    QueryMetadata metadata;

    DataDomainSelectAction(DataDomain domain, ObjectContext context, Query query) {

        this.domain = domain;
        this.context = (DataContext) context;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());

        // sanity check
        if (!metadata.isFetchingDataRows()) {
            if (context == null) {
                throw new IllegalArgumentException(
                        "Null context for query fetching DataObjects.");
            }
            else if (!(context instanceof DataContext)) {
                throw new IllegalArgumentException(
                        "DataDomain can only select into DataContext. Unsupported context: "
                                + context);
            }
        }
    }

    List execute() {

        QueryResponse response = domain.onQuery(context, query);
        List mainRows = response.getFirstRows(query);

        if (metadata.isFetchingDataRows()) {
            return mainRows;
        }

        if (mainRows.isEmpty()) {
            return new ArrayList(1);
        }

        ObjEntity entity = metadata.getObjEntity();
        PrefetchTreeNode prefetchTree = metadata.getPrefetchTree();

        // take a shortcut when no prefetches exist...
        if (prefetchTree == null) {
            return new ObjectResolver(
                    context,
                    entity,
                    metadata.isRefreshingObjects(),
                    metadata.isResolvingInherited())
                    .synchronizedObjectsFromDataRows(mainRows);
        }

        // map results to prefetch paths
        Map rowsByPath = new HashMap();

        // find result set
        Iterator it = response.allQueries().iterator();

        while (it.hasNext()) {
            Query q = (Query) it.next();

            if (q instanceof PrefetchSelectQuery) {
                PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) q;
                rowsByPath.put(prefetchQuery.getPrefetchPath(), response.getFirstRows(q));
            }
        }

        ObjectTreeResolver resolver = new ObjectTreeResolver(context, metadata);
        return resolver.resolveObjectTree(prefetchTree, mainRows, rowsByPath);
    }
}
