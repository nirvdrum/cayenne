package org.objectstyle.cayenne.access.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.map.DataMap;

/**
 * Defines a set of sorting methods based on object dependencies. The actual
 * dependency tracking algorithm is pluggable via a sporter class.
 * 
 * @author Andrei Adamchik
 */
public class SortHandler {
    protected static Class defaultSorterClass = DefaultSorter.class;

    protected DependencySorter sorter;

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
    public SortHandler(QueryEngine queryEngine, DataMap[] dataMaps) {
        this(defaultSorterClass, queryEngine, dataMaps);
    }

    /**
      * Creates SortHandler with a specified sorter class.
      */
    public SortHandler(
        Class sorterClass,
        QueryEngine queryEngine,
        DataMap[] dataMaps) {

        // sanity check
        if (sorterClass == null) {
            throw new IllegalArgumentException("DependencySorter class can not be null.");
        }

        if (!DependencySorter.class.isAssignableFrom(sorterClass)) {
            throw new IllegalArgumentException(
                "Sorter class "
                    + sorterClass.getName()
                    + " must implement DependencySorter interface.");
        }

        try {
            sorter = (DependencySorter) sorterClass.newInstance();
        } catch (Exception ex) {
            throw new CayenneRuntimeException(
                "Error instantiating sorter from " + sorterClass.getName(),
                ex);
        }

        sorter.initSorter(queryEngine, dataMaps);
    }

    /**
      * Creates and returns an array of queries in the right sorting order from
      * an unsorted array.
      */
    public List sortedQueries(List unsortedQueries) {
        Object[] array = unsortedQueries.toArray();
        Arrays.sort(array, sorter.getQueryComparator());
        return Arrays.asList(array);
    }

    /**
     * Returns a new list containing all the DbEntities in
     * <code>entities</code>,  in the correct order for inserting objects int,o
     * or creating the tables of, those entities.
     */
    public List sortedDbEntitiesInInsertOrder(List dbEntities) {
        Object[] array = dbEntities.toArray();
        Arrays.sort(array, sorter.getDbEntityComparator(true));
        return Arrays.asList(array);
    }

    /**
      *  Returns a new list containing all the DbEntities in <code>entities</code>,
      *  in the correct order for deleting objects from or removing the tables of, those entities.
      */
    public List sortedDbEntitiesInDeleteOrder(List dbEntities) {
        Object[] array = dbEntities.toArray();
        Arrays.sort(array, sorter.getDbEntityComparator(false));
        return Arrays.asList(array);
    }

    /**
     *  Sorts an unsorted array of DataObjects in the right
     *  insert order for database constraints not to be violated,
     *  and so that master pk's for dependent relationships
     *  are in place prior to being needed for the dependent
     *  object, and reflexive relationships are correctly handled
     */
    public void sortObjectsInInsertOrder(List objects) {
        Collections.sort(objects, sorter.getDataObjectComparator(true));
    }

    /**
     *  Sorts an unsorted array of DataObjects in the right
     *  delete order for database constraints not to be violated,
     *  and so that master pk's for dependent relationships
     *  are in place prior to being needed for the dependent
     *  object and reflexive relationships are correctly handled
     */
    public void sortObjectsInDeleteOrder(List objects) {
        Collections.sort(objects, sorter.getDataObjectComparator(false));
    }
}
