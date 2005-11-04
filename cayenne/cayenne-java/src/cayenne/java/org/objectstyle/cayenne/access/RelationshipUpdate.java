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
package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * A holder of relationship modification data. Used for tracking relationship
 * modifications that can't be derived on the fly from object graph. Overrides
 * Object.equals() and Object.hasCode() to allow smart comparison of different
 * RelationshipUpdate objects.
 * 
 * @author Andrei Adamchik
 * @since 1.2
 */
class RelationshipUpdate {

    DataObject source;
    DataObject destination;
    ObjRelationship relationship;
    ObjRelationship reverseRelationship;
    String compareToken;

    RelationshipUpdate(DataObject source, DataObject destination,
            ObjRelationship relationship) {

        this.source = source;
        this.destination = destination;
        this.relationship = relationship;
        this.reverseRelationship = relationship.getReverseRelationship();

        // build a string token to make comparison (or at least hashcode) indepent from
        // direction
        String relName1 = relationship.getName();
        if (reverseRelationship != null) {
            String relName2 = reverseRelationship.getName();

            // Find the lexically lesser name and use it as the name of the source, then
            // use the second.
            // If equal (the same name), it doesn't matter which order...
            if (relName1.compareTo(relName2) <= 0) {
                this.compareToken = relName1 + "." + relName2;
            }
            else {
                this.compareToken = relName2 + "." + relName1;
            }
        }
        else {
            this.compareToken = relName1;
        }
    }

    boolean isBidirectional() {
        return reverseRelationship != null;
    }

    /**
     * Defines equal based on whether the relationship is bidirectional.
     */
    public boolean equals(Object object) {

        if (!(object instanceof RelationshipUpdate)) {
            return false;
        }

        if (this == object) {
            return true;
        }

        RelationshipUpdate update = (RelationshipUpdate) object;

        if (!this.compareToken.equals(update.compareToken)) {
            return false;
        }

        boolean bidi = isBidirectional();
        if (bidi != update.isBidirectional()) {
            return false;
        }

        return (bidi) ? bidiEquals(update) : uniEquals(update);
    }

    public int hashCode() {
        // TODO: use hashcode builder to make a better hashcode...though
        // RelationshipUpdate is stored in collections, not maps, so it is probably
        // irrelevant.
        return source.hashCode() + destination.hashCode() + compareToken.hashCode();
    }

    boolean bidiEquals(RelationshipUpdate update) {
        return (this.source.equals(update.source) && this.destination
                .equals(update.destination))
                || (this.source.equals(update.destination) && this.destination
                        .equals(update.source));
    }

    boolean uniEquals(RelationshipUpdate update) {
        return (this.source.equals(update.source) && this.destination
                .equals(update.destination));
    }

    ObjRelationship getRelationship() {
        return relationship;
    }

    ObjRelationship getReverseRelationship() {
        return reverseRelationship;
    }

    /**
     * Returns the destination object of the relationship.
     */
    DataObject getDestination() {
        return destination;
    }

    /**
     * Returns the source object of the relationship.
     */
    DataObject getSource() {
        return source;
    }

}