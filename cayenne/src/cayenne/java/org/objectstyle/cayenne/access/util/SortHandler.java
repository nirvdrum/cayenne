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

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * Defines a set of sorting methods based on object dependencies. The actual
 * dependency tracking algorithm is pluggable via a sorter class.
 * 
 * @author Andrei Adamchik
 */
public class SortHandler {
    private static Logger logObj = Logger.getLogger(SortHandler.class);
    protected static Class defaultSorterClass = DefaultSorter.class;

    protected DependencySorter sorter;
    protected Class sorterClass;
    protected QueryEngine queryEngine;
    protected boolean dirty;

    public static Class getDefaultSorterClass() {
        return defaultSorterClass;
    }

    /**
     * Sets the class of the sorter that should be used as a source of
     * dependency sorting algorithms. Class must be an instance of
     * DependencySorter and must have a default no-argument constructor.
     * 
     * @param sorterClass The sorterClass to set
     */
    public static void setDefaultSorterClass(Class defaultSorterClass) {
        SortHandler.defaultSorterClass = defaultSorterClass;
    }

    /**
     * Constructor for SortHandler.
     */
    public SortHandler(QueryEngine queryEngine) {
        this(defaultSorterClass, queryEngine);
    }

    /**
      * Creates SortHandler with a specified sorter class.
      */
    public SortHandler(Class sorterClass, QueryEngine queryEngine) {
        // sanity check
        if (sorterClass == null) {
            throw new IllegalArgumentException("DependencySorter class can not be null.");
        }
        if (queryEngine == null) {
            throw new IllegalArgumentException("QueryEngine can not be null.");
        }

        this.sorterClass = sorterClass;
        this.queryEngine = queryEngine;

        // don't immediately index sorter, since parent QueryEngine
        // may not be ready yet, besides "good program is lazy program"...        
        this.dirty = true;
    }

    /**
     * Returns the internal sorting engine instance.
     */
    public DependencySorter getSorter() {
        _indexSorter();
        return sorter;
    }

    /**
     * Marks itself as "dirty", so that it will be indexed lazily on next
     * invocation.
     */
    public void indexSorter() {
        dirty = true;
    }

    /**
     * Reindexes internal sorter.
     */
    protected synchronized void _indexSorter() {
        if (!dirty) {
            return;
        }
        DependencySorter newSorter;

        try {
            newSorter = (DependencySorter) sorterClass.newInstance();

        } catch (Exception ex) {
            throw new CayenneRuntimeException(
                "Error instantiating sorter from " + sorterClass.getName(),
                ex);
        }

        newSorter.initSorter(queryEngine);
        this.sorter = newSorter;
        this.dirty = false;
    }

    /**
     * Sorts a list of ObjEntities in the correct order for inserting objects
     * into a database.
     */
    public void sortObjEntitiesInInsertOrder(List objEntities) {
        _indexSorter();
        Collections.sort(objEntities, sorter.getObjEntityComparator(false));
    }

    /**
      * Sorts a list of ObjEntities in the correct order for deleting objects
      * from the database.
      */
    public void sortObjEntitiesInDeleteOrder(List objEntities) {
        _indexSorter();
        Collections.sort(objEntities, sorter.getObjEntityComparator(true));
    }

    /**
     * Sorts a list of DbEntities in the correct order for inserting objects
     * into or creating the tables of, those entities.
     */
    public void sortDbEntitiesInInsertOrder(List dbEntities) {
        _indexSorter();
        Collections.sort(dbEntities, sorter.getDbEntityComparator(false));
    }

    /**
      * Sorts a list of DbEntities in <code>entities</code>, in the correct
      * order for deleting objects from or removing the tables of, those
      * entities.
      */
    public void sortDbEntitiesInDeleteOrder(List dbEntities) {
        _indexSorter();
        Collections.sort(dbEntities, sorter.getDbEntityComparator(true));
    }

    /**
     *  Sorts an unsorted array of DataObjects in the right
     *  insert order for database constraints not to be violated,
     *  and so that master pk's for dependent relationships
     *  are in place prior to being needed for the dependent
     *  object, and reflexive relationships are correctly handled
     */
    public void sortObjectsInInsertOrder(ObjEntity entity, List objects) {
        _indexSorter();
        sorter.sortObjectsForEntity(entity, objects, false);
    }

    /**
     *  Sorts an unsorted array of DataObjects in the right
     *  delete order for database constraints not to be violated,
     *  and so that master pk's for dependent relationships
     *  are in place prior to being needed for the dependent
     *  object and reflexive relationships are correctly handled
     */
    public void sortObjectsInDeleteOrder(ObjEntity entity, List objects) {
        _indexSorter();
        sorter.sortObjectsForEntity(entity, objects, true);
    }
}
