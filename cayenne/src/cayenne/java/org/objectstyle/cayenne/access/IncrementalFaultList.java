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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A synchronized list that serves as a container of DataObjects. 
 * It is returned when a paged query is performed by DataContext. 
 * On creation, only the first "page" is fully resolved, for the rest 
 * of the objects only their ObjectIds are read. 
 * Pages following the first page are resolved on demand only. 
 * On access to an element, the list would ensure that this 
 * element as well as all its siblings on the same page are fully 
 * resolved.
 * 
 * <p>Note that this list would only allow addition of DataObjects. Attempts to add any
 * other object types will result in an exception.</p>
 * 
 * <p>Performance note: certain operations like <code>toArray</code> would trigger full list 
 * fetch.</p>
 * 
 * @author Andrei Adamchik
 */
public class IncrementalFaultList implements List {
    static Logger logObj = Logger.getLogger(IncrementalFaultList.class.getName());

    protected int pageSize;
    protected List elements;
    protected DataContext dataContext;
    protected ObjEntity rootEntity;
    protected SelectQuery internalQuery;
    protected int unfetchedObjects;

    /**
     * Creates a new list copying settings from another list.
     * Elements WILL NOT be copied or fetched.
     */
    public IncrementalFaultList(IncrementalFaultList list) {
        this.pageSize = list.pageSize;
        this.internalQuery = list.internalQuery;
        this.dataContext = list.dataContext;
        this.rootEntity = list.rootEntity;
        elements = Collections.synchronizedList(new ArrayList());
    }

    public IncrementalFaultList(DataContext dataContext, GenericSelectQuery query) {
        if (query.getPageSize() <= 0) {
            throw new CayenneRuntimeException(
                "IncrementalFaultList does not support unpaged queries. Query page size is "
                    + query.getPageSize());
        }

        this.elements = Collections.synchronizedList(new ArrayList());
        this.dataContext = dataContext;
        this.pageSize = query.getPageSize();
        this.rootEntity = dataContext.getEntityResolver().lookupObjEntity(query.getObjEntityName());

        // create an internal query, it is a partial replica of 
        // the original query and will serve as a value holder for 
        // various parameters
        this.internalQuery = new SelectQuery(query.getObjEntityName());
        this.internalQuery.setLoggingLevel(query.getLoggingLevel());
        if (query instanceof SelectQuery) {
            this.internalQuery.addPrefetches(((SelectQuery) query).getPrefetchList());
        }

        fillIn(query);
    }

    /**
     * Performs initialization of the internal list of objects.
     * Only the first page is fully resolved. For the rest of
     * the list, only ObjectIds are read.
     */
    protected void fillIn(GenericSelectQuery query) {
        synchronized (elements) {

            // start fresh
            elements.clear();

            try {
                long t1 = System.currentTimeMillis();
                ResultIterator it = dataContext.performIteratedQuery(query);
                try {
                    // read first page completely, the rest as ObjectIds
                    for (int i = 0; i < pageSize && it.hasNextRow(); i++) {
                        Map row = it.nextDataRow();
                        elements.add(
                            dataContext.objectFromDataRow(rootEntity, row, true));
                    }

                    // continue reading ids
                    while (it.hasNextRow()) {
                        elements.add(it.nextObjectId());
                    }

                    QueryLogger.logSelectCount(
                        query.getLoggingLevel(),
                        elements.size(),
                        System.currentTimeMillis() - t1);

                } finally {
                    it.close();
                }
            } catch (CayenneException e) {
                throw new CayenneRuntimeException("Error performing query.", e);
            }

            // process prefetching
            if (internalQuery.getPrefetchList().size() > 0) {
                int endOfPage = (elements.size() < pageSize) ? elements.size() : pageSize;
                dataContext.prefetchRelationships(
                    internalQuery,
                    elements.subList(0, endOfPage));
            }
            
            unfetchedObjects = elements.size() - pageSize;
        }
    }

    /**
     * Will resolve all unread objects.
     */
    public void resolveAll() {
        resolveInterval(0, size());
    }

    /**
     * Resolves a sublist of objects starting at <code>fromIndex</code>
     * up to but not including <code>toIndex</code>. Internally performs
     * bound checking and trims indexes accordingly.
     */
    protected void resolveInterval(int fromIndex, int toIndex) {
        if (fromIndex >= toIndex) {
            return;
        }

        synchronized (elements) {
        	if(elements.size() == 0) {
        		return;
        	}
        	
        	// perform bound checking
        	if(fromIndex < 0) {
        		fromIndex = 0;
        	}
        	
        	if(toIndex > elements.size()) {
        		toIndex = elements.size();
        	}
        	
            ArrayList quals = new ArrayList();
            ArrayList ids = new ArrayList();
            for (int i = fromIndex; i < toIndex; i++) {
                Object obj = elements.get(i);
                if (obj instanceof Map) {
                    ids.add(obj);
                    quals.add(
                        ExpressionFactory.matchAllDbExp((Map) obj, Expression.EQUAL_TO));
                }
            }

            if (quals.size() == 0) {
                return;
            }

            SelectQuery query =
                new SelectQuery(
                    rootEntity.getName(),
                    ExpressionFactory.joinExp(Expression.OR, quals));

            query.setLoggingLevel(query.getLoggingLevel());

            List objects = dataContext.performQuery(query);

            // sanity check - database data may have changed
            if (objects.size() < ids.size()) {
                // find missing ids
                StringBuffer buf = new StringBuffer();
                buf.append("Some ObjectIds are missing from the database. ");
                buf.append("Expected ").append(ids.size()).append(", fetched ").append(
                    objects.size());

                Iterator idsIt = ids.iterator();
                boolean first = true;
                while (idsIt.hasNext()) {
                    boolean found = false;
                    Object id = (Object) idsIt.next();
                    Iterator oIt = objects.iterator();
                    while (oIt.hasNext()) {
                        if (((DataObject) oIt.next())
                            .getObjectId()
                            .getIdSnapshot()
                            .equals(id)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(", ");
                        }

                        buf.append(id.toString());
                    }
                }

                throw new CayenneRuntimeException(buf.toString());
            } else if (objects.size() > ids.size()) {
                throw new CayenneRuntimeException(
                    "Expected " + ids.size() + " objects, retrieved " + objects.size());
            }

            // replace ids in the list with objects
            Iterator it = objects.iterator();
            while (it.hasNext()) {
                DataObject obj = (DataObject) it.next();
                Map idMap = obj.getObjectId().getIdSnapshot();

                boolean found = false;
                for (int i = fromIndex; i < toIndex; i++) {
                    if (idMap.equals(elements.get(i))) {
                        elements.set(i, obj);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new CayenneRuntimeException("Can't find id for " + idMap);
                }
            }
            
            unfetchedObjects -= objects.size();
        }

        // process prefetching
        if (internalQuery.getPrefetchList().size() > 0) {
            int endOfPage = (elements.size() < toIndex) ? elements.size() : toIndex;
            dataContext.prefetchRelationships(
                internalQuery,
                elements.subList(fromIndex, endOfPage));
        }
    }

    public int pageIndex(int elementIndex) {
        if (pageSize <= 0 || elementIndex < 0) {
            return -1;
        }

        return elementIndex / pageSize;
    }

    /**
     * Returns the dataContext.
     * @return DataContext
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Returns the pageSize.
     * @return int
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator() {
        throw new UnsupportedOperationException("'listIterator' is not supported.");
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index) {
        throw new UnsupportedOperationException("'listIterator' is not supported.");
    }

    /**
     * This method would resolve all unresolved objects and then return
     * an iterator over an internal list.
     * 
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
    	resolveAll();
        return elements.iterator();
    }

    /**
     * @see java.util.List#add(int, Object)
     */
    public void add(int index, Object element) {
        if (!(element instanceof DataObject)) {
            throw new IllegalArgumentException("Only DataObjects can be stored in this list.");
        }

        synchronized (elements) {
            elements.add(index, element);
        }
    }

    /**
     * @see java.util.Collection#add(Object)
     */
    public boolean add(Object o) {
        if (!(o instanceof DataObject)) {
            throw new IllegalArgumentException("Only DataObjects can be stored in this list.");
        }

        synchronized (elements) {
            return elements.add(o);
        }
    }

    /**
     * @see java.util.Collection#addAll(Collection)
     */
    public boolean addAll(Collection c) {
        synchronized (elements) {
            return elements.addAll(c);
        }
    }

    /**
     * @see java.util.List#addAll(int, Collection)
     */
    public boolean addAll(int index, Collection c) {
        synchronized (elements) {
            return elements.addAll(index, c);
        }
    }

    /**
     * @see java.util.Collection#clear()
     */
    public void clear() {
        synchronized (elements) {
            elements.clear();
        }
    }

    /**
     * @see java.util.Collection#contains(Object)
     */
    public boolean contains(Object o) {
        synchronized (elements) {
            return elements.contains(o);
        }
    }

    /**
     * @see java.util.Collection#containsAll(Collection)
     */
    public boolean containsAll(Collection c) {
        synchronized (elements) {
            return elements.containsAll(c);
        }
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        synchronized (elements) {
            Object o = elements.get(index);

            if (o instanceof Map) {
                // read this page
                int pageStart = pageIndex(index) * pageSize;
                resolveInterval(pageStart, pageStart + pageSize);

                return elements.get(index);
            } else {
                return o;
            }
        }
    }

    /**
     * @see java.util.List#indexOf(Object)
     */
    public int indexOf(Object o) {
        if (!(o instanceof DataObject)) {
            return -1;
        }

        DataObject dataObj = (DataObject) o;
        if (dataObj.getDataContext() != dataContext) {
            return -1;
        }

        if (!dataObj.getObjectId().getObjEntityName().equals(rootEntity.getName())) {
            return -1;
        }

        Map idMap = dataObj.getObjectId().getIdSnapshot();

        synchronized (elements) {
            for (int i = 0; i < elements.size(); i++) {
                // objects are in the same context, 
                // just comparing ids should be enough
                Object obj = elements.get(i);
                if (obj == dataObj) {
                    return i;
                }

                Map otherIdMap =
                    (obj instanceof DataObject)
                        ? ((DataObject) obj).getObjectId().getIdSnapshot()
                        : (Map) obj;
                if (idMap.equals(otherIdMap)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        synchronized (elements) {
            return elements.isEmpty();
        }
    }

    /**
     * @see java.util.List#lastIndexOf(Object)
     */
    public int lastIndexOf(Object o) {
        if (!(o instanceof DataObject)) {
            return -1;
        }

        DataObject dataObj = (DataObject) o;
        if (dataObj.getDataContext() != dataContext) {
            return -1;
        }

        if (!dataObj.getObjectId().getObjEntityName().equals(rootEntity.getName())) {
            return -1;
        }

        Map idMap = dataObj.getObjectId().getIdSnapshot();

        synchronized (elements) {
            for (int i = elements.size() - 1; i <= 0; i--) {
                // objects are in the same context, 
                // just comparing ids should be enough
                Object obj = elements.get(i);
                if (obj == dataObj) {
                    return i;
                }

                Map otherIdMap =
                    (obj instanceof DataObject)
                        ? ((DataObject) obj).getObjectId().getIdSnapshot()
                        : (Map) obj;
                if (idMap.equals(otherIdMap)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int index) {
        synchronized (elements) {
            return elements.remove(index);
        }
    }

    /**
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(Object o) {
        synchronized (elements) {
            return elements.remove(o);
        }
    }

    /**
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection c) {
        synchronized (elements) {
            return elements.removeAll(c);
        }
    }

    /**
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection c) {
        synchronized (elements) {
            return elements.retainAll(c);
        }
    }

    /**
     * @see java.util.List#set(int, Object)
     */
    public Object set(int index, Object element) {
        if (!(element instanceof DataObject)) {
            throw new IllegalArgumentException("Only DataObjects can be stored in this list.");
        }

        synchronized (elements) {
            return elements.set(index, element);
        }
    }

    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        synchronized (elements) {
            return elements.size();
        }
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    public List subList(int fromIndex, int toIndex) {
        synchronized (elements) {
            List sublist = elements.subList(fromIndex, toIndex);
            IncrementalFaultList list = new IncrementalFaultList(this);
            list.elements = Collections.unmodifiableList(sublist);
            return list;
        }
    }

    /**
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        resolveAll();

        return elements.toArray();
    }

    /**
     * @see java.util.Collection#toArray(Object[])
     */
    public Object[] toArray(Object[] a) {
        resolveAll();

        return elements.toArray(a);
    }

    /**
     * Returns the unfetchedObjects.
     * @return int
     */
    public int getUnfetchedObjects() {
        return unfetchedObjects;
    }
}
