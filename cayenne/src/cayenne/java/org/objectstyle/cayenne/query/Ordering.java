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
package org.objectstyle.cayenne.query;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.util.DataObjectPropertyComparator;
import org.objectstyle.cayenne.util.XMLSerializable;


/** 
 * Defines ordering policy. Queries can have multiple Ordering's. 
 * 
 * @author Andrei Adamchik
 * @author Craig Miskell
 */
public class Ordering implements Comparator, XMLSerializable {
	private static Logger logObj = Logger.getLogger(Ordering.class);
	
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
        Collections.sort(objects, new DataObjectPropertyComparator(orderings));
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
        
        if(value1==null) {
        	if(value2==null) {
        		return 0;
        	}
        	return -1; //value 1 is null, value2 isn't... value1 should come first
        } else if (value2==null) {
        	return 1; //value 2 is null, value 1 isn't... value1 should come second
        }
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
    
    /**
     * Encodes itself as a query ordering.
     * 
     * @since 1.1
     */
    public void encodeAsXML(PrintWriter pw, String linePadding) {
        pw.print(linePadding);
        pw.print("<ordering path=\"");
        pw.print(sortSpec);
        
        if(!ascending) {
            pw.println("\" ascending=\"false");
        }
        
        pw.println("\"/>");
    }
}
