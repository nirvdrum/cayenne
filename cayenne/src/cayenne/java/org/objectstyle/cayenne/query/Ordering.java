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
package org.objectstyle.cayenne.query;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;


/** 
 * Defines ordering policy. Queries can have multiple Ordering's. 
 * 
 * @author Andrei Adamchik
 * @author Craig Miskell
 */
public class Ordering implements Comparator {
    /** Symbolic representation of ascending ordering criterion. */
    public static final boolean ASC = true;

    /** Symbolic representation of descending ordering criterion. */
    public static final boolean DESC = false;

    protected Expression sortSpec;
    protected boolean ascending;
    protected boolean caseInsensitive;

    /**
     * Orders the given list of objects according to the list of orderings specified
     * Requires that the objects in objects are all subclasses of CayenneDataObject.
     * Throws an IllegalArgumentException if any aren't
     * Modifies theList in place
     * @param theList a List of objects to be sorted
     */
    public static void orderList(List objects, List orderings) {
        checkObjectsClass(objects);
        Collections.sort(objects, new Ordering.ListSorter(orderings));
    }

    public Ordering() {}

    public Ordering(String sortPathSpec, boolean ascending) {
        this(sortPathSpec, ascending, false);
    }

    public Ordering(String sortPathSpec, boolean ascending, boolean caseInsensitive) {
        setSortSpec(sortPathSpec);
        this.ascending = ascending;
        this.caseInsensitive = caseInsensitive;
    }

    public Ordering(Expression sortExpression, boolean ascending) {
        this(sortExpression, ascending, false);
    }

    public Ordering(
        Expression sortExpression,
        boolean ascending,
        boolean caseInsensitive) {
        setSortSpec(sortExpression);
        this.ascending = ascending;
        this.caseInsensitive = caseInsensitive;
    }

    /** 
     * Returns sortPathSpec OBJ_PATH specification used in ordering.
     * 
     * @deprecated Since ordering now supports expression types other than OBJ_PATH,
     * this method is deprected. Use <code>getSortSpec().getOperand(0)</code> instead.
        */
    public String getSortPathSpec() {
        return (String) getSortSpec().getOperand(0);
    }

    /** 
     * Sets path of the sort specification. 
     * 
     * @deprecated Since ordering now supports expression types other than OBJ_PATH,
     * this method is deprected. Use <code>setSortSpec()</code> instead.
     */
    public void setSortPathSpec(String sortPathSpec) {
        setSortSpec(sortPathSpec);
    }

    /** 
     * Sets sortSpec to be OBJ_PATH expression. 
     * with path specified as <code>sortPathSpec</code>
     * parameter.
     */
    public void setSortSpec(String sortPathSpec) {
        this.sortSpec = ExpressionFactory.unaryExp(Expression.OBJ_PATH, sortPathSpec);
    }

    /** Returns true if sorting is done in ascending order. */
    public boolean isAscending() {
        return ascending;
    }

    /** Sets <code>ascending</code> property of this Ordering. */
    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    /** Returns true if the sorting is case insensitive */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /** Sets <code>caseInsensitive</code> property of this Ordering. */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Returns the sortSpec.
     * @return Expression
     */
    public Expression getSortSpec() {
        return sortSpec;
    }

    /**
     * Sets the sortSpec.
     * @param sortSpec The sortSpec to set
     */
    public void setSortSpec(Expression sortSpec) {
        this.sortSpec = sortSpec;
    }

    private static void checkObjectsClass(List objects) {
        int i;
        for (i = 0; i < objects.size(); i++) {
            if (!(objects.get(i) instanceof CayenneDataObject)) {
                throw new IllegalArgumentException(
                    "Object ("
                        + objects.get(i)
                        + ") at index "
                        + i
                        + " of list sent to Ordering is not a CayenneDataObject");
            }
        }

    }

    /**
     * Orders the given list of objects according to the ordering that this object specifies
     * Requires that the objects in object are all subclasses of CayenneDataObject.
     * Throws an IllegalArgumentException if any aren't
     * Modifies theList in place
     * @param theList a List of objects to be sorted
     */
    public void orderList(List objects) {
        Ordering.checkObjectsClass(objects);
        Collections.sort(objects, this);
    }

    public int compare(Object o1, Object o2) {
        String operand0 = (String) this.sortSpec.getOperand(0);
        Comparable value1 =
            (Comparable) ((CayenneDataObject) o1).readNestedProperty(operand0);
        Comparable value2 =
            (Comparable) ((CayenneDataObject) o2).readNestedProperty(operand0);
        if (this.caseInsensitive) {
            //Assumes that value1 and value2 are both the same class - will be the case if
            // both objects being ordered are the same class - they'd better be, otherwise ordering
            // them is a dodgy situation.
            if (value1 instanceof String) {
                value1 = ((String) value1).toUpperCase();
                value2 = ((String) value2).toUpperCase();
            }
        }
        int compareResult = value1.compareTo(value2);
        if (ascending == ASC) {
            return compareResult;
        } else {
            return -compareResult;
        }
    }

    public static class ListSorter implements Comparator {
        private Ordering[] orderings;
        /**
         * Constructor ListSorter.
         * @param orderings
         */
        public ListSorter(List orderingsList) {
            super();
            int i;
            orderings = new Ordering[orderingsList.size()];
            for (i = 0; i < orderingsList.size(); i++) {
                orderings[i] = (Ordering) orderingsList.get(i);
            }
        }

        public int compare(Object o1, Object o2) {
            int i = 0;
            int result = 0;
            //Evaluate each ordering until one returns non-zero.  If all return 0, all are equal
            //As soon as one returns non-equal, return that value (all lower orderings are irrelevant)
            while ((i < orderings.length) && (result == 0)) {
                result = orderings[i++].compare(o1, o2);
            }
            return result;
        }
    }
}
