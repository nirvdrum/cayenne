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

package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.log4j.Logger;
import org.objectstyle.ashwood.dbutil.DbUtils;
import org.objectstyle.ashwood.dbutil.ForeignKey;
import org.objectstyle.ashwood.dbutil.Table;
import org.objectstyle.ashwood.graph.CollectionFactory;
import org.objectstyle.ashwood.graph.Digraph;
import org.objectstyle.ashwood.graph.IndegreeTopologicalSort;
import org.objectstyle.ashwood.graph.MapDigraph;
import org.objectstyle.ashwood.graph.StrongConnection;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.Query;

/**
 * DefaultSorter is a default implementation of DependencySorter based on
 * ASHWOOD library. Presently it works for acyclic database schemas with
 * possible multi-reflexive tables. The class uses topological sorting from
 * ASHWOOD.
 *
 * @author Andriy Shapochka
 */

public class DefaultSorter implements DependencySorter {
    private static Logger logObj = Logger.getLogger(DefaultSorter.class);

    protected QueryEngine queryEngine;
    protected Map dbEntityToTableMap;
    protected Digraph referentialDigraph;
    protected Digraph contractedReferentialDigraph;
    protected Map components;
    protected Map reflexiveDbEntities;

    protected TableComparator tableComparator;
    protected DbEntityComparator dbEntityComparator;
    protected ObjEntityComparator objEntityComparator;

    public DefaultSorter() {
    }

    public void initSorter(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
        Collection tables = new ArrayList();
        dbEntityToTableMap = new HashMap();
        reflexiveDbEntities = new HashMap();
        for (Iterator i = queryEngine.getDataMapsAsList().iterator();
            i.hasNext();
            ) {
            DataMap map = (DataMap) i.next();
            Iterator entitiesToConvert = map.getDbEntitiesAsList().iterator();
            while (entitiesToConvert.hasNext()) {
                DbEntity entity = (DbEntity) entitiesToConvert.next();
                Table table =
                    new Table(
                        entity.getCatalog(),
                        entity.getSchema(),
                        entity.getName());
                fillInMetadata(table, entity);
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

        tableComparator = new TableComparator();
        dbEntityComparator = new DbEntityComparator();
        objEntityComparator = new ObjEntityComparator();
    }

    private void fillInMetadata(Table table, DbEntity entity) {
        //in this case quite a dummy
        short keySequence = 1;
        Iterator i = entity.getRelationshipMap().values().iterator();

        while (i.hasNext()) {
            DbRelationship candidate = (DbRelationship) i.next();
            if ((!candidate.isToMany() && !candidate.isToDependentPK())
                || candidate.isToMasterPK()) {

                DbEntity target = (DbEntity) candidate.getTargetEntity();
                boolean newReflexive = entity.equals(target);
                Iterator j = candidate.getJoins().iterator();
                while (j.hasNext()) {
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
                                (List) reflexiveDbEntities.get(entity);
                            if (reflexiveRels == null) {
                                reflexiveRels = new ArrayList(1);
                                reflexiveDbEntities.put(entity, reflexiveRels);
                            }
                            reflexiveRels.add(candidate);
                            newReflexive = false;
                        }
                    }
                }
            }
        }
    }
    

    public void sortObjectsForEntity(
        ObjEntity objEntity,
        List objects,
        boolean dependentFirst) {

        DbEntity dbEntity = objEntity.getDbEntity();

        // if no sorting is required
        if (!isReflexive(dbEntity)) {
            return;
        }

        int size = objects.size();
        List reflexiveRels = (List) reflexiveDbEntities.get(dbEntity);
        String[] objRelNames = new String[reflexiveRels.size()];
        for (int i = 0; i < objRelNames.length; i++) {
            DbRelationship dbRel = (DbRelationship) reflexiveRels.get(i);
            ObjRelationship objRel =
                (dbRel != null
                    ? objEntity.getRelationshipForDbRelationship(dbRel)
                    : null);
            objRelNames[i] = (objRel != null ? objRel.getName() : null);
        }

        List sorted = new ArrayList(size);

        Digraph objectDependencyGraph =
            new MapDigraph(MapDigraph.HASHMAP_FACTORY);
        DataObject[] masters = new DataObject[objRelNames.length];
        for (int i = 0; i < size; i++) {
            DataObject current = (DataObject) objects.get(i);
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
                if (masters[k] == null) {
                    masters[k] =
                        findReflexiveMaster(
                            current,
                            (ObjRelationship) objEntity.getRelationship(
                                objRelName),
                            current.getObjectId().getObjClass());
                }

                if (masters[k] != null) {
                    actualMasterCount++;
                }
            }
            int mastersFound = 0;

            for (int j = 0;
                j < size && mastersFound < actualMasterCount;
                j++) {

                if (i == j) {
                    continue;
                }

                DataObject masterCandidate = (DataObject) objects.get(j);
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
                throw new CayenneRuntimeException(
                    "Sorting objects for "
                        + objEntity.getClassName()
                        + " failed. Cycles found.");
            sorted.add(o);
        }

        // since API requires sorting within the same array,
        // simply replace all objects with objects in the right order... 
        // may come up with something cleaner later
        objects.clear();
        objects.addAll(sorted);
        
        if(dependentFirst) {
        	Collections.reverse(objects);
        }
    }

    protected DataObject findReflexiveMaster(
        DataObject obj,
        ObjRelationship toOneRel,
        Class targetClass) {
        DbRelationship finalRel =
            (DbRelationship) toOneRel.getDbRelationshipList().get(0);
        Map snapshot = obj.getCommittedSnapshot();
        if (snapshot == null) {
            snapshot = obj.getCurrentSnapshot();
        }

        Map pksnapshot = finalRel.targetPkSnapshotWithSrcSnapshot(snapshot);
        if (pksnapshot != null) {
            ObjectId destId = new ObjectId(targetClass, pksnapshot);
            return obj.getDataContext().registeredObject(destId);
        }

        return null;
    }

    public Comparator getDbEntityComparator(boolean dependantFirst) {
        Comparator c = dbEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    public Comparator getObjEntityComparator(boolean dependantFirst) {
        Comparator c = objEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
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
            queryEngine.getEntityResolver().lookupObjEntity(objEntityClass);
        if (objEntity == null)
            return null;
        DbEntity dbEntity = objEntity.getDbEntity();
        return getTable(dbEntity);
    }

    public Table getTable(Query query) {
        DbEntity dbEntity =
            queryEngine.getEntityResolver().lookupDbEntity(query);
        return getTable(dbEntity);
    }

    public boolean isReflexive(DbEntity metadata) {
        return reflexiveDbEntities.containsKey(metadata);
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