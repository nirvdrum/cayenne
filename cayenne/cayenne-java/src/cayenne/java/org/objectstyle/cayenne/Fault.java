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

import java.io.Serializable;
import java.util.List;

import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.query.RelationshipQuery;

/**
 * Represents a placeholder for an unresolved relationship from a source object. Fault is
 * resolved via {@link #resolveFault(Persistent, String)}. Depending on the type of fault
 * it is resolved differently. Each type of fault is a singleton that can be obtained via
 * corresponding static method.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */

// TODO: serialization of faults should take into account the fact that
// they are used as singletons to avoid duplicate creation on deserialization
public abstract class Fault implements Serializable {

    protected static final Fault toOneFault = new ToOneFault();
    protected static final Fault toManyFault = new ToManyFault();

    public static Fault getToOneFault() {
        return toOneFault;
    }

    public static Fault getToManyFault() {
        return toManyFault;
    }

    protected Fault() {
    }

    /**
     * Returns an object for a given source object and relationship.
     */
    public abstract Object resolveFault(Persistent sourceObject, String relationshipName);

    final static class ToManyFault extends Fault {

        /**
         * Resolves this fault to a List of objects.
         */
        public Object resolveFault(Persistent sourceObject, String relationshipName) {
            return new ToManyList(sourceObject, relationshipName);
        }
    }

    final static class ToOneFault extends Fault {

        /**
         * Resolves this fault to a DataObject.
         */
        public Object resolveFault(Persistent sourceObject, String relationshipName) {
            ObjectContext context = sourceObject.getObjectContext();

            RelationshipQuery query = new RelationshipQuery(
                    sourceObject.getObjectId(),
                    relationshipName,
                    false);

            List objects = context.performQuery(query);

            if (objects.isEmpty()) {
                return null;
            }
            else if (objects.size() == 1) {
                return objects.get(0);
            }
            else {
                throw new CayenneRuntimeException("Error resolving to-one fault. "
                        + "More than one object found. "
                        + "Source Id: "
                        + sourceObject.getObjectId()
                        + ", relationship: "
                        + relationshipName);
            }
        }
    }
}