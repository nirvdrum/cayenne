/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002 The ObjectStyle Group
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

package org.objectstyle.cayenne.access;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ComparatorUtils;
import org.objectstyle.ashwood.dbutil.DbUtils;
import org.objectstyle.ashwood.dbutil.ForeignKey;
import org.objectstyle.ashwood.dbutil.Table;
import org.objectstyle.ashwood.graph.CollectionFactory;
import org.objectstyle.ashwood.graph.Digraph;
import org.objectstyle.ashwood.graph.IndegreeTopologicalSort;
import org.objectstyle.ashwood.graph.MapDigraph;
import org.objectstyle.ashwood.graph.StrongConnection;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.Query;

public class RefIntegritySupport {
    private DataNode supportedNode;
    private Map dbEntityToTableMap;
    private Digraph referentialDigraph;
    private Digraph contractedReferentialDigraph;
    private HashMap components;
    private HashMap reflexiveDbEntities;

    private TableComparator tableComparator;
    private DbEntityComparator dbEntityComparator;
    private ObjEntityComparator objEntityComparator;
    private DataObjectComparator dataObjectComparator;
    private InsertQueryComparator insertQueryComparator;
    private Comparator deleteQueryComparator;
    private QueryComparator queryComparator;

    public RefIntegritySupport(DataNode nodeToSupport)
        throws CayenneException {
        supportedNode = nodeToSupport;
        try {
            init();
            tableComparator = new TableComparator();
            dbEntityComparator = new DbEntityComparator();
            objEntityComparator = new ObjEntityComparator();
            dataObjectComparator = new DataObjectComparator();
            insertQueryComparator = new InsertQueryComparator();
            deleteQueryComparator =
                ComparatorUtils.reversedComparator(insertQueryComparator);
            queryComparator = new QueryComparator();
        } catch (SQLException sqle) {
            String msg =
                "Failed to create RefIntegritySupport: " + sqle.getMessage();
            throw new CayenneException(msg, sqle);
        }
    }

    public void reset(DataNode nodeToSupport) throws CayenneException {
        supportedNode = nodeToSupport;
        try {
            init();
        } catch (SQLException sqle) {
            String msg =
                "Failed to reset RefIntegritySupport: " + sqle.getMessage();
            throw new CayenneException(msg, sqle);
        }
    }

    public Comparator getDbEntityComparator() {
        return dbEntityComparator;
    }

    public Comparator getObjEntityComparator() {
        return objEntityComparator;
    }

    public Comparator getDataObjectComparator() {
        return dataObjectComparator;
    }

    public Comparator getQueryComparator() {
        return queryComparator;
    }

    public Comparator getInsertQueryComparator() {
        return insertQueryComparator;
    }

    public Comparator getDeleteQueryComparator() {
        return deleteQueryComparator;
    }

    public Comparator getTableComparator() {
        return tableComparator;
    }

    public Table getTable(DbEntity dbEntity) {
        return (
            dbEntity != null ? (Table) dbEntityToTableMap.get(dbEntity) : null);
    }

    public Table getTable(ObjEntity objEntity) {
        return getTable(objEntity.getDbEntity());
    }

    public int getIndex(Table table) {
        return ((ComponentRecord) components.get(table)).index;
    }

    public Table getTable(DataObject dataObject) {
        Class objEntityClass = dataObject.getObjectId().getObjClass();
        ObjEntity objEntity =
            supportedNode.getEntityResolver().lookupObjEntity(objEntityClass);
        if (objEntity == null)
            return null;
        DbEntity dbEntity = objEntity.getDbEntity();
        return getTable(dbEntity);
    }

    public Table getTable(Query query) {
        ObjEntity objEntity =
            supportedNode.getEntityResolver().lookupObjEntity(query);
        if (objEntity == null)
            return null;
        DbEntity dbEntity = objEntity.getDbEntity();
        return getTable(dbEntity);
    }

    public boolean isReflexive(DbEntity metadata) {
        return reflexiveDbEntities.containsKey(metadata);
    }

    public List sort(List dataObjects, ObjEntity objEntity)
        throws CayenneException {
        DbEntity metadata = objEntity.getDbEntity();
        if (!isReflexive(metadata))
            return dataObjects;

        int size = dataObjects.size();
        List reflexiveRels = (List) reflexiveDbEntities.get(metadata);
        String[] objRelNames = new String[reflexiveRels.size()];
        for (int i = 0; i < objRelNames.length; i++) {
            DbRelationship dbRel = (DbRelationship) reflexiveRels.get(i);
            ObjRelationship objRel =
                (dbRel != null
                    ? objEntity.getRelationshipForDbRelationship(dbRel)
                    : null);
            objRelNames[i] = (objRel != null ? objRel.getName() : null);
        }
        //HashSet lookup = new HashSet(dataObjects);
        List sortedObjects = new ArrayList(dataObjects.size());
        Digraph objectDependencyGraph =
            new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        DataObject[] masters = new DataObject[objRelNames.length];
        for (int i = 0; i < size; i++) {
            DataObject current = (DataObject) dataObjects.get(i);
            objectDependencyGraph.addVertex(current);
            int actualMasterCount = 0;
            for (int k = 0; k < objRelNames.length; k++) {
                String objRelName = objRelNames[k];
                if (objRelName == null)
                    continue;
                masters[k] =
                    (objRelName != null
                        ? (DataObject) current.readPropertyDirectly(objRelName)
                        : null);
                if (masters[k] != null)
                    actualMasterCount++;
            }
            int mastersFound = 0;
            for (int j = 0;
                j < size && mastersFound < actualMasterCount;
                j++) {
                if (i == j)
                    continue;
                DataObject masterCandidate = (DataObject) dataObjects.get(j);
                for (int k = 0; k < masters.length; k++) {
                    if (masterCandidate.equals(masters[k])) {
                        objectDependencyGraph.putArc(
                            masterCandidate,
                            current,
                            Boolean.TRUE);
                        mastersFound++;
                    }
                }
            }
        }
        IndegreeTopologicalSort sorter =
            new IndegreeTopologicalSort(objectDependencyGraph);
        while (sorter.hasNext()) {
            DataObject o = (DataObject) sorter.next();
            if (o == null)
                throw new CayenneException(
                    "Sorting objects for "
                        + objEntity.getClassName()
                        + " failed. Cycles found.");
            sortedObjects.add(o);
        }
        return sortedObjects;
    }

    private void init() throws SQLException {
        Collection tables = new ArrayList();
        dbEntityToTableMap = new HashMap();
        reflexiveDbEntities = new HashMap();
        for (Iterator i = supportedNode.dataMapIterator(); i.hasNext();) {
            DataMap map = (DataMap) i.next();
            DbEntity[] entitiesToConvert = map.getDbEntities();
            for (int j = 0; j < entitiesToConvert.length; j++) {
                DbEntity entity = entitiesToConvert[j];
                Table table =
                    new Table(
                        entity.getCatalog(),
                        entity.getSchema(),
                        entity.getName());
                fillWithMetadata(table, entity);
                dbEntityToTableMap.put(entity, table);
                tables.add(table);
            }
        }
        referentialDigraph = new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        DbUtils.buildReferentialDigraph(referentialDigraph, tables);
        StrongConnection contractor =
            new StrongConnection(
                referentialDigraph,
                CollectionFactory.ARRAYLIST_FACTORY);
        contractedReferentialDigraph =
            new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        contractor.contract(
            contractedReferentialDigraph,
            CollectionFactory.ARRAYLIST_FACTORY);
        IndegreeTopologicalSort sorter =
            new IndegreeTopologicalSort(contractedReferentialDigraph);
        components = new HashMap(contractedReferentialDigraph.order());
        int componentIndex = 0;
        while (sorter.hasNext()) {
            Collection component = (Collection) sorter.next();
            ComponentRecord rec =
                new ComponentRecord(componentIndex++, component);
            for (Iterator i = component.iterator(); i.hasNext();) {
                components.put(i.next(), rec);
            }
        }
    }

    //This, hopefully, replaces the full scale metadata retrieval
    //Side effect: reflexive DbEntities are being found
    private void fillWithMetadata(Table table, DbEntity metadata) {
        //in this case quite a dummy
        short keySequence = 1;
        for (Iterator i = metadata.getRelationshipMap().values().iterator();
            i.hasNext();
            ) {
            DbRelationship candidate = (DbRelationship) i.next();
            if (!candidate.isToMany() && !candidate.isToDependentPK()) {
                DbEntity target = (DbEntity) candidate.getTargetEntity();
                boolean newReflexive = metadata.equals(target);
                for (Iterator j = candidate.getJoins().iterator();
                    j.hasNext();
                    ) {
                    DbAttributePair join = (DbAttributePair) j.next();
                    DbAttribute targetAttribute = join.getTarget();
                    if (targetAttribute.isPrimaryKey()) {
                        ForeignKey fk = new ForeignKey();
                        fk.setPkTableCatalog(target.getCatalog());
                        fk.setPkTableSchema(target.getSchema());
                        fk.setPkTableName(target.getName());
                        fk.setPkColumnName(targetAttribute.getName());
                        fk.setColumnName(join.getSource().getName());
                        fk.setKeySequence(keySequence++);
                        table.addForeignKey(fk);

                        if (newReflexive) {
                            List reflexiveRels =
                                (List) reflexiveDbEntities.get(metadata);
                            if (reflexiveRels == null) {
                                reflexiveRels = new ArrayList(1);
                                reflexiveDbEntities.put(
                                    metadata,
                                    reflexiveRels);
                            }
                            reflexiveRels.add(candidate);
                            newReflexive = false;
                        }
                    }
                }
            }
        }
    }

    private class DbEntityComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Table t1 = getTable((DbEntity) o1);
            Table t2 = getTable((DbEntity) o2);
            return tableComparator.compare(t1, t2);
        }
    }

    private class ObjEntityComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Table t1 = getTable((ObjEntity) o1);
            Table t2 = getTable((ObjEntity) o2);
            return tableComparator.compare(t1, t2);
        }
    }

    private class DataObjectComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Table t1 = getTable((DataObject) o1);
            Table t2 = getTable((DataObject) o2);
            return tableComparator.compare(t1, t2);
        }
    }

    private class QueryComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Query q1 = (Query) o1;
            Query q2 = (Query) o2;
            int type1 = q1.getQueryType();
            int type2 = q2.getQueryType();
            switch (type1) {
                case Query.INSERT_QUERY :
                    if (type2 != Query.INSERT_QUERY)
                        return -1;
                    else
                        return insertQueryComparator.compare(q1, q2);
                case Query.UPDATE_QUERY :
                    if (type2 == Query.INSERT_QUERY)
                        return 1;
                    else if (type2 == Query.UPDATE_QUERY)
                        return 0;
                    else
                        return -1;
                case Query.DELETE_QUERY :
                    if (type2 == Query.INSERT_QUERY
                        || type2 == Query.UPDATE_QUERY)
                        return 1;
                    else if (type2 == Query.DELETE_QUERY)
                        return deleteQueryComparator.compare(q1, q2);
                    else
                        return -1;
                default :
                    if (type2 == Query.INSERT_QUERY
                        || type2 == Query.UPDATE_QUERY
                        || type2 == Query.DELETE_QUERY)
                        return 1;
                    else
                        return 0;
            }
        }
    }

    private class InsertQueryComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            Table t1 = getTable((Query) o1);
            Table t2 = getTable((Query) o2);
            return tableComparator.compare(t1, t2);
        }
    }

    private class TableComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int result = 0;
            Table t1 = (Table) o1;
            Table t2 = (Table) o2;
            if (t1 == t2)
                return 0;
            if (t1 == null)
                result = -1;
            else if (t2 == null)
                result = 1;
            else {
                ComponentRecord rec1 = (ComponentRecord) components.get(t1);
                ComponentRecord rec2 = (ComponentRecord) components.get(t2);
                int index1 = rec1.index;
                int index2 = rec2.index;
                result = (index1 > index2 ? 1 : (index1 < index2 ? -1 : 0));
                if (result != 0 && rec1.component == rec2.component)
                    result = 0;
            }
            return result;
        }
    }

    private static class ComponentRecord {
        ComponentRecord(int index, Collection component) {
            this.index = index;
            this.component = component;
        }
        int index;
        Collection component;
    }
}