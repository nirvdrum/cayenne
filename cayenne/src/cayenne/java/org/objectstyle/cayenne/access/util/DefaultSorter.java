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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.ash.dbutil.DbUtils;
import net.ash.dbutil.Table;
import net.ash.graph.CollectionFactory;
import net.ash.graph.Digraph;
import net.ash.graph.IndegreeTopologicalSort;
import net.ash.graph.MapDigraph;
import net.ash.graph.StrongConnection;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Query;

/**
 * Operation sorter implementation using 
 * <a href="http://objectstyle.org/ashwood">Ashwood library</a>.
 * 
 * @author Andriy Shapochka
 * @author Craig Miskell
 * @author Andrei Adamchik
 */
public class DefaultSorter implements DependencySorter {

    protected QueryEngine queryEngine;
    protected Map dbEntityToTableMap;
    protected Digraph referentialDigraph;
    protected Digraph contractedReferentialDigraph;
    protected Map components;

    protected TableComparator tableComparator;
    protected DbEntityComparator dbEntityComparator;
    protected ObjEntityComparator objEntityComparator;
    protected DataObjectComparator dataObjectComparator;
    protected InsertQueryComparator insertQueryComparator;
    protected Comparator deleteQueryComparator;
    protected QueryComparator queryComparator;

    public DefaultSorter() {
        tableComparator = new TableComparator();
        dbEntityComparator = new DbEntityComparator();
        objEntityComparator = new ObjEntityComparator();
        dataObjectComparator = new DataObjectComparator();
        insertQueryComparator = new InsertQueryComparator();
        deleteQueryComparator =
            ComparatorUtils.reversedComparator(insertQueryComparator);
        queryComparator = new QueryComparator();
    }

    public void initSorter(QueryEngine queryEngine, DataMap[] maps) {
        this.queryEngine = queryEngine;
        Collection tables = new ArrayList();
        dbEntityToTableMap = new HashMap();

        if (maps == null) {
            return;
        }

        for (int i = 0; i < maps.length; i++) {
            DbEntity[] entitiesToConvert = maps[i].getDbEntities();
            for (int j = 0; j < entitiesToConvert.length; j++) {
                DbEntity entity = entitiesToConvert[j];
                Table table =
                    new Table(
                        entity.getCatalog(),
                        entity.getSchema(),
                        entity.getName());
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

    public Comparator getDataObjectComparator(boolean dependantFirst) {
        Comparator c = dataObjectComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
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
        Class entClass = dataObject.getObjectId().getObjClass();
        DbEntity dbEntity =
            queryEngine.getEntityResolver().lookupDbEntity(entClass);
        return getTable(dbEntity);
    }

    public Table getTable(Query query) {
        DbEntity dbEntity =
            queryEngine.getEntityResolver().lookupDbEntity(query);
        return getTable(dbEntity);
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
