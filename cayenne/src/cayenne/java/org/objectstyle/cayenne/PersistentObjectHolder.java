/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne;

import java.util.List;

/**
 * A ValueHolder implementation that holds a single Persistent object related to an object
 * used to initialize PersistentObjectHolder. Value is resolved on first access.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PersistentObjectHolder extends RelationshipFault implements ValueHolder {

    protected boolean resolved;
    protected Object value;

    public PersistentObjectHolder(Persistent relationshipOwner, String relationshipName) {
        super(relationshipOwner, relationshipName);
    }

    /**
     * Returns a value resolving it via a query on the first call to this method.
     */
    public Object getValue(Class valueClass) throws CayenneRuntimeException {
        typeSafetyCheck(valueClass, value);

        if (!resolved) {
            resolve();
        }

        return value;
    }

    /**
     * Sets an object value, marking this ValueHolder as resolved.
     */
    public synchronized Object setValue(Class valueClass, Object value)
            throws CayenneRuntimeException {

        typeSafetyCheck(valueClass, value);

        if (!resolved) {
            resolve();
        }

        Object oldValue = this.value;

        if (oldValue != value) {
            this.value = value;

            // notify ObjectContext
            if (relationshipOwner.getObjectContext() != null) {
                relationshipOwner.getObjectContext().propertyChanged(
                        relationshipOwner,
                        relationshipName,
                        oldValue,
                        value);
            }
        }

        return oldValue;
    }

    /**
     * Performs a type-safety check on the value. A value must be of the specified class
     * or its sublcass or implement specified interface. Otherwise CayenneRuntimeException
     * is thrown.
     */
    protected void typeSafetyCheck(Class valueClass, Object value) {
        if (value != null && !(valueClass.isInstance(value))) {
            throw new CayenneRuntimeException("Expected value of class '"
                    + valueClass.getName()
                    + "', got: "
                    + value.getClass().getName());
        }
    }

    /**
     * Reads an object from the database.
     */
    protected synchronized void resolve() {
        if (resolved) {
            return;
        }

        // TODO: should build a HOLLOW object instead of running a query if relationship
        // is required and thus expected to be not null.

        List objects = resolveFromDB();

        if (objects.size() == 0) {
            this.value = null;
        }
        else if (objects.size() == 1) {
            this.value = objects.get(0);
        }
        else {
            throw new FaultFailureException(
                    "Expected either no objects or a single object, instead fault query resolved to "
                            + objects.size()
                            + " objects.");
        }

        resolved = true;
    }
}
