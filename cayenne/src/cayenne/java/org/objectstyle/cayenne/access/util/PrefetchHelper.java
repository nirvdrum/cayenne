/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Arndt Brenschede
 */
public class PrefetchHelper {

    /**
     * Resolves a toOne relationship for a list of objects.
     * (performance tuning only)
     */
    public static void resolveToOneRelations(
        DataContext context,
        List objects,
        String relName) {
        int nobjects = objects.size();
        if (nobjects == 0)
            return;

        List oids = new ArrayList(nobjects);

        for (int i = 0; i < nobjects; i++) {
            DataObject sourceObject = (DataObject) objects.get(i);
            DataObject targetObject =
                (DataObject) sourceObject.readPropertyDirectly(relName);

            ObjectId oid = targetObject.getObjectId();
            oids.add(oid);
        }
        // this maybe suboptimal, cause it uses an OR .. OR .. OR .. expression
        // instead of IN (..) - to be compatble with compound keys - 
        // however, it seems to be quite fast as well
        SelectQuery sel = QueryUtils.selectQueryForIds(oids);
        context.performQuery(sel);
    }

    /**
     * Resolves a toMany relation for a list of objects.
     * (performance tuning only)
     * 
     * <p>WARNING: this is a bit of a hack - it works for my
     * toMany's, but it possibly doesn't work in all cases.</p>
     * 
     * 
     * <p>*** It definitly does not work for compound keys ***</p>
     */
    public static void resolveToManyRelations(
        DataContext context,
        List objects,
        String relName) {

        int nobjects = objects.size();
        if (nobjects == 0)
            return;

        String dbKey = null;
        Map listMap = new HashMap(nobjects);

        // put the object-ids in a map for later assignment of the
        // query results

        for (int i = 0; i < nobjects; i++) {
            DataObject object = (DataObject) objects.get(i);
            ObjectId oid = object.getObjectId();
            if (dbKey == null) {
                Map id = oid.getIdSnapshot();
                if (id.size() != 1) {
                    throw new CayenneRuntimeException("resolveToManyRelations expects single keys for now...");
                }
                dbKey = (String) id.keySet().iterator().next();
            }
            listMap.put(oid.getValueForAttribute(dbKey), new ArrayList());
        }

        ObjEntity ent =
            context.getEntityResolver().lookupObjEntity((DataObject) objects.get(0));
        ObjRelationship rel = (ObjRelationship) ent.getRelationship(relName);
        ObjEntity destEnt = (ObjEntity) rel.getTargetEntity();

        List dbRels = rel.getDbRelationships();

        // sanity check
        if (dbRels == null || dbRels.size() == 0) {
            throw new CayenneRuntimeException(
                "ObjRelationship '" + rel.getName() + "' is unmapped.");
        }

        // build a reverse DB path
        // ...while reverse ObjRelationship may be absent,
        // reverse DB must always be there...
        StringBuffer buf = new StringBuffer();
        ListIterator it = dbRels.listIterator(dbRels.size());
        while (it.hasPrevious()) {
            if (buf.length() > 0) {
                buf.append(".");
            }
            DbRelationship dbRel = (DbRelationship) it.previous();
            DbRelationship reverse = dbRel.getReverseRelationship();

            // another sanity check
            if (reverse == null) {
                throw new CayenneRuntimeException(
                    "DbRelatitionship '"
                        + dbRel.getName()
                        + "' has no reverse relationship");
            }

            buf.append(reverse.getName());
        }

        // do the query
        SelectQuery sel =
            new SelectQuery(
                destEnt,
                ExpressionFactory.binaryDbPathExp(
                    Expression.IN,
                    buf.toString(),
                    ExpressionFactory.unaryExp(Expression.LIST, objects)));
        sel.setFetchingDataRows(true);
        List results = context.performQuery(sel);

        // sort the resulting objects into individual lists for each source object

        List destObjects = context.objectsFromDataRows(destEnt, results, false, false);
        int nrows = destObjects.size();
        for (int k = 0; k < nrows; k++) {
            Map row = (Map) results.get(k);
            ((List) listMap.get(row.get(dbKey))).add(destObjects.get(k));
        }

        // and finally set these lists in the relation targets
        for (int i = 0; i < nobjects; i++) {
            DataObject object = (DataObject) objects.get(i);
            ObjectId oid = object.getObjectId();
            List list = (List) listMap.get(oid.getValueForAttribute(dbKey));

            ((ToManyList) object.readPropertyDirectly(relName)).setObjectList(list);
        }
    }
}
