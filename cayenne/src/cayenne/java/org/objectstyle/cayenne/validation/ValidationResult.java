/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.SequencedHashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Represents a result of a validation execution. Contains a set of failures ({@link ValidationFailure}) 
 * occured in a given context. All failures are kept in the same order they were added.
 *
 * <i><p>Implementation Note:</p>
 * <p>
 * Failures are stored in a map indexed by source and property name. As most of the times a pair (source, property) will
 * only one failure, the failure itself is stored as the value. When more than one failure is added to the same pair,
 * a list is created to hold all failures, maintaining the order which they were added.</p></i>
 *
 * @author Fabricio Voznika
 * @since 1.1
 */
public class ValidationResult implements Serializable {
    private static final Logger logObj = Logger.getLogger(ValidationResult.class);

    private Map errors = new SequencedHashMap();

    /**
     * Convenience method to add a <code>StringValidationFailure</code>.
     *
     * @param source object that generated the failure. It may be null if failure was not generated by any specific object.
     * @param property name of the property that generated the failure. It may be null if failure was not generated
     * by any specific property.
     * @param errorMessage failure error message.
     * @see StringValidationFailure
     * @see #addFailure(ValidationFailure)
     */
    public void addFailure(Object source, String property, String errorMessage) {
        StringValidationFailure failure =
            new StringValidationFailure(source, property, errorMessage);
        this.addFailure(failure);
    }

    /**
     * Add a failure to the validation result.
     *
     * @param failure failure to be added. It may not be null.
     * @see ValidationFailure
     */
    public void addFailure(ValidationFailure failure) {
        if (failure == null) {
            throw new IllegalArgumentException("failure cannot be null.");
        }
        if (failure.getSource() == null && failure.getProperty() != null) {
            throw new IllegalArgumentException("ValidationFailure cannot have 'property' when 'source' is null.");
        }

        if (logObj.isDebugEnabled()) {
            logObj.log(Level.DEBUG, failure);
        }

        Tuple tuple = new Tuple(failure.getSource(), failure.getProperty());
        Object obj = this.errors.get(tuple);
        if (obj != null) {
            List list;
            if (obj instanceof List) {
                list = (List) obj;

            }
            else {
                if (!(obj instanceof ValidationFailure)) { // Assertion
                    throw new IllegalStateException(
                        "Wrong object type '" + obj.getClass().getName() + "'.");
                }
                list = new ArrayList(3);
                list.add(obj);
                errors.put(tuple, list);
            }
            list.add(failure);

        }
        else {
            errors.put(tuple, failure);
        }
    }

    /**
     * @return all failures added to this result. It never returns null.
     */
    public List getFailures() {
        Collection values = this.errors.values();

        // Guess 2 errors each on average.
        ArrayList failures = new ArrayList(values.size() * 2);

        Iterator it = values.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof List) {
                List res = (List) obj;
                failures.addAll(res);
            }
            else {
                failures.add(obj);
            }
        }

        return failures;
    }

    /**
     * @param source it may be null.
     * @return all failures related to <code>source</code>. It never returns null.
     * @see ValidationFailure#getSource()
     */
    public List getFailures(Object source) {
        if (source == null) {
            return this.getFailures(null, null);
        }
        ArrayList ret = new ArrayList(5); //Guess 5 errors max.
        for (Iterator it = this.errors.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            Tuple tuple = (Tuple) entry.getKey();
            if (source.equals(tuple.one)) {
                ret.add(entry.getValue());
            }
        }
        return ret;
    }

    /**
     * @param source it may be null iff property is also null.
     * @param property it may be null
     * @return all failures related to <code>source</code> and <code>property</code>. It never returns null.
     * @see ValidationFailure#getSource()
     * @see ValidationFailure#getProperty()
     */
    public List getFailures(Object source, String property) {
        if (source == null && property != null) {
            throw new IllegalArgumentException("Param 'source' cannot be null when 'property' is not null.");
        }

        Tuple tuple = new Tuple(source, property);
        Object obj = errors.get(tuple);
        
        if (obj instanceof List) {
            return Collections.unmodifiableList((List) obj);
        }
        else if (obj != null) {
            return Collections.singletonList(obj);
        }
        else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * @return true if at least one failure has been added to this result. False otherwise.
     */
    public boolean hasFailures() {
        return !errors.isEmpty();
    }

    /**
     * @param source it may be null.
     * @return true if there is at least one failure for <code>source</code>. False otherwise.
     */
    public boolean hasFailures(Object source) {
        if (source == null) {
            return this.hasFailures(null, null);
        }

        Iterator it = this.errors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Tuple tuple = (Tuple) entry.getKey();
            if (source.equals(tuple.one)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param source it may be null.
     * @param property it may be null.
     * @return true if there is at least one failure for <code>source</code> and <code>property</code>. False otherwise.
     */
    public boolean hasFailures(Object source, String property) {
        if (source == null && property != null) {
            throw new IllegalArgumentException("Param 'source' cannot be null when 'property' is not null.");
        }
        
        return this.errors.containsKey(new Tuple(source, property));
    }

    public String toString() {
        StringBuffer ret = new StringBuffer(1024);
        String separator = System.getProperty("line.separator");

        Iterator it = getFailures().iterator();
        while (it.hasNext()) {
            if (ret.length() > 0) {
                ret.append(separator);
            }
            
            ret.append(it.next());
        }

        return ret.toString();
    }

    // used as a key in hash maps
    private static class Tuple implements Serializable {
        Object one;
        Object two;

        public Tuple(Object one, Object two) {
            this.one = one;
            this.two = two;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Tuple))
                return false;

            final Tuple tuple = (Tuple) o;

            if (one != null ? !one.equals(tuple.one) : tuple.one != null)
                return false;
            if (two != null ? !two.equals(tuple.two) : tuple.two != null)
                return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (one != null ? one.hashCode() : 0);
            result = 29 * result + (two != null ? two.hashCode() : 0);
            return result;
        }
    }
}