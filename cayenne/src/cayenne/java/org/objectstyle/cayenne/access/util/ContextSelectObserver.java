/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.SnapshotManager;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;

/** 
 * ContextSelectObserver is a SelectObserver that would 
 * convert fetched data rows into objects of an associated 
 * DataContext.
 */
public class ContextSelectObserver extends SelectObserver {
    protected DataContext context;

    /**
     * Constructor for ContextSelectObserver.
     * @param logLevel
     */
    public ContextSelectObserver(DataContext context, Level logLevel) {
        super(logLevel);
        this.context = context;
    }

    /** 
     * Overrides superclass behavior to convert each  data row to a real
     * object. Registers objects with parent DataContext.
     */
    public void nextDataRows(Query query, List dataRows) {
        List result = null;
        if (dataRows != null && dataRows.size() > 0) {
            ObjEntity ent = context.getEntityResolver().lookupObjEntity(query);

            // do a sanity check on ObjEntity... if it's DbEntity has no PK defined,
            // we can't build a valid ObjectId
            DbEntity dbEntity = ent.getDbEntity();
            if (dbEntity == null) {
                throw new CayenneRuntimeException(
                    "ObjEntity '" + ent.getName() + "' has no DbEntity.");
            }

            if (dbEntity.getPrimaryKey().size() == 0) {
                throw new CayenneRuntimeException(
                    "Can't create ObjectId for '"
                        + ent.getName()
                        + "'. Reason: DbEntity '"
                        + dbEntity.getName()
                        + "' has no Primary Key defined.");
            }

            result = new ArrayList(dataRows.size());
            Iterator it = dataRows.iterator();
            while (it.hasNext()) {
                result.add(context.objectFromDataRow(ent, (Map) it.next(), true));
            }
        }
        else {
            result = new ArrayList();
        }

        if (query instanceof PrefetchSelectQuery) {
            PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) query;
            ObjRelationship theRelationship =
                prefetchQuery.getSingleStepToManyRelationship();
            if (theRelationship != null) {
                //The root query should have already executed, so we can get it's
                // results
                List rootQueryResults = this.getResults(prefetchQuery.getRootQuery());
                if (rootQueryResults == null) {
                    throw new CayenneRuntimeException(
                        "Prefetch query for path "
                            + prefetchQuery.getPrefetchPath()
                            + " executed before it's root query "
                            + prefetchQuery.getRootQuery());
                }
                SnapshotManager.getSharedInstance().mergePrefetchResultsRelationships(
                    rootQueryResults,
                    theRelationship,
                    result);
            }
        }

        super.nextDataRows(query, result);
    }
}
