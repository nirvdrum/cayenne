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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;

/**
 * An immutable non-synchronized list implementation that would internally 
 * populate itself with database data only when it is needed. 
 * IncrementalFaultList has an internal page size. On creation list 
 * would only read the first "page". 
 * On access to a list element, it will make sure that all pages from
 * the first one to the one containing requested object are resolved. Pages are indexed
 * starting from zero and up.
 * 
 * <p>Note that some operations on the list would cause the whole list 
 * to be faulted. For instance, calling <code>size()</code>, 
 * <code>contains()</code>, <code>indexOf()</code> method.</p>
 * 
 * @author Andrei Adamchik
 */
public class IncrementalFaultList extends AbstractList {
    static Logger logObj = Logger.getLogger(IncrementalFaultList.class.getName());

    protected int pagesRead;
    protected int pageSize;
    protected List elements;
    protected DataContext dataContext;
    protected GenericSelectQuery query;
    protected boolean fullyResolved;

    public IncrementalFaultList(DataContext dataContext, GenericSelectQuery query) {
        if (query.getPageSize() <= 0) {
            throw new CayenneRuntimeException(
                "IncrementalFaultList does not support unpaged queries. Query page size is "
                    + query.getPageSize());
        }

        elements = new ArrayList();
        this.dataContext = dataContext;
        this.query = query;
        this.pageSize = query.getPageSize();
        readUpToPage(1);
    }

    /**
     * Returns a number of pages that are already faulted. 
     */
    public int getPagesRead() {
        return pagesRead;
    }
    
    public int getObjectsRead() {
    	return pagesRead * pageSize;
    }

    /**
     * Completely resolves all list objects.
     */
    public void readAll() {
        if (fullyResolved) {
            return;
        }

        readPageInterval(pagesRead, Integer.MAX_VALUE);
    }

    public void readUpToObject(int elementIndex) {
        int ind = pageIndex(elementIndex);

        // if element is not the first on the page,
        // fault element page too, otherwise fault
        // all pages till the element page
        if (elementIndex % pageSize > 0) {
            ind++;
        }

        readUpToPage(ind);
    }

    /**
     * Faults this list resolving pages from the beginning
     * up to but not including <code>pageIndex</code>. After this
     * method is successfully called, the list is guaranteed to have
     * first <code>pageIndex</code> pages resolved.
     */
    public void readUpToPage(int pageIndex) {
        if (fullyResolved || pageIndex <= pagesRead) {
            return;
        }

        readPageInterval(pagesRead, pageIndex);
    }

    /**
     * Faults this list resolving pages starting at <code>fromIndex</code>
     * up to but not including <code>toIndex</code>.
     */
    protected void readPageInterval(int fromIndex, int toIndex) {
        if (fromIndex >= toIndex) {
            return;
        }

        int readFrom = fromIndex * pageSize;
        int readTo = toIndex * pageSize;

        try {

            ResultIterator it = dataContext.performIteratedQuery(query);
            try {

                // skip through read
                for (int i = 0; i < readFrom; i++) {
                    if (!it.hasNextRow()) {
                        // results ended before we need to read anything
                        fullyResolved = true;
                        pagesRead = pageIndex(i) + 1;
                        return;
                    }

                    // skip processed
                    it.skipDataRow();
                }

                // read all requested pages
                ObjEntity ent = dataContext.lookupEntity(query.getObjEntityName());
                for (int i = readFrom; i < readTo; i++) {
                    if (!it.hasNextRow()) {
                        fullyResolved = true;
                        pagesRead = pageIndex(i) + 1;
                        return;
                    }

                    // read objects
                    Map row = it.nextDataRow();
                    elements.add(dataContext.objectFromDataRow(ent, row, true));
                }
                pagesRead = toIndex;

                // check if coincidentally we read the whole thing 
                if (!it.hasNextRow()) {
                    fullyResolved = true;
                }

            } finally {
                it.close();
            }

        } catch (CayenneException e) {
            throw new CayenneRuntimeException("Error faulting page.", e);
        }
    }

    public int pageIndex(int elementIndex) {
        if (query.getPageSize() <= 0 || elementIndex < 0) {
            return -1;
        }

        return elementIndex / query.getPageSize();
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        if (!fullyResolved && pageIndex(index) <= pagesRead) {
            readUpToObject(index);
        }

        return elements.get(index);
    }

    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        if (!fullyResolved) {
            readAll();
        }

        return elements.size();
    }

    /**
     * Returns the dataContext.
     * @return DataContext
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Returns the fullyResolved.
     * @return boolean
     */
    public boolean isFullyResolved() {
        return fullyResolved;
    }

    /**
     * Returns the query.
     * @return GenericSelectQuery
     */
    public GenericSelectQuery getQuery() {
        return query;
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
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        return new FaultIterator(elements);
    }

    /**
     * Removes all elements from the list. Subscequent calls
     * to <code>isFullyResolved</code> will return <code>true</code>.
     * 
     * @see java.util.Collection#clear()
     */
    public void clear() {
        elements.clear();
        pagesRead = 0;
        fullyResolved = true;
    }

    /**
     * Iterator that avoids calling <code>size()</code>
     * to allow incremental resolution of the objects on the list.
     */
    class FaultIterator implements Iterator {
        private int cursor;
        private List list;
        private boolean hasNext;

        private FaultIterator(List list) {
            this.list = list;
        }
        
        /**
        * @see java.util.Iterator#hasNext()
        */
        public boolean hasNext() {
        	if(isFullyResolved()) {
        		return cursor < list.size();
        	}
        	
        	if(cursor >= getObjectsRead()) {
        		// read objects till cursor
        		readUpToObject(cursor);
        	}
        	
            return cursor < getObjectsRead();
        }

        /**
         * @see java.util.Iterator#next()
         */
        public Object next() {
            Object obj = list.get(cursor);
            cursor++;
            return obj;
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException("'remove' is not supported.");
        }
    }
}
