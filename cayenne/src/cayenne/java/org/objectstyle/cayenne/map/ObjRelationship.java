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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * Describes navigational association between two ObjEntities. Stores
 * navigation information in terms of the DbEntity relationships.
 * ObjRelationships are owned by source ObjEntities.
 * 
 * @author Andrei Adamchik
 */
public class ObjRelationship extends Relationship implements EventListener {
    private static Logger logObj = Logger.getLogger(ObjRelationship.class);

    // What to do with any inverse relationship when the source object
    // is deleted
    private int deleteRule = DeleteRule.NO_ACTION;

    // Not flattened initially;
    // will be set when dbRels are added that make it flattened
    private boolean isFlattened = false;

    // Initially all relationships are read/write;
    // a flattened relationship may be readonly (in certain circumstances),
    // will be set in that case
    private boolean isReadOnly = false;

    private List dbRelationships = new ArrayList();
    private List dbRelationshipsRef = Collections.unmodifiableList(dbRelationships);

    public ObjRelationship() {
        super();
    }

    public ObjRelationship(String name) {
        super(name);
    }

    public ObjRelationship(ObjEntity source, ObjEntity target, boolean toMany) {
        this();
        this.setSourceEntity(source);
        this.setTargetEntity(target);
        if (toMany) {
            this.setName(target.getName() + "Array");
        }
        else {
            this.setName("to" + target.getName());
        }
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        ObjEntity source = (ObjEntity) getSourceEntity();
        if (source == null) {
            logObj.warn(
                "No source entity, will not encode ObjRelationship: " + getName());
            return;
        }

        ObjEntity target = (ObjEntity) getTargetEntity();
        if (target == null) {
            logObj.warn("No target entity, will not encode ObjRelationship " + getName());
            return;
        }

        encoder.print("<obj-relationship name=\"" + getName() + '\"');
        encoder.print(" source=\"" + source.getName() + '\"');
        encoder.print(" target=\"" + target.getName() + '\"');
        encoder.print(" toMany=\"" + isToMany() + '\"');

        String deleteRule = DeleteRule.deleteRuleName(getDeleteRule());
        if (getDeleteRule() != DeleteRule.NO_ACTION && deleteRule != null) {
            encoder.print(" deleteRule=\"" + deleteRule + '\"');
        }

        encoder.println('>');
        encoder.indent(1);

        // do the first empty run, to see that the chain of
        // relationships is valid
        boolean validChain = true;
        Iterator dryRun = getDbRelationships().iterator();
        while (dryRun.hasNext()) {
            DbRelationship relationship = (DbRelationship) dryRun.next();
            if (!dbRelationships.contains(relationship)) {
                validChain = false;
                break;
            }
        }

        if (validChain) {
            // TODO: this is dumb, store chain as a path expression...
            Iterator iter = getDbRelationships().iterator();
            while (iter.hasNext()) {
                DbRelationship rel = (DbRelationship) iter.next();
                encoder.print("<db-relationship-ref source=\"");
                encoder.print(rel.getSourceEntity().getName());
                encoder.print("\" target=\"");
                encoder.print(rel.getTargetEntityName());
                encoder.print("\" name=\"");
                encoder.print(rel.getName());
                encoder.println("\"/>");
            }
        }

        encoder.indent(-1);
        encoder.println("</obj-relationship>");
    }

    public Entity getTargetEntity() {
        if (getTargetEntityName() == null) {
            return null;
        }

        Entity src = getSourceEntity();
        if (src == null) {
            return null;
        }

        DataMap map = src.getDataMap();
        if (map == null) {
            return null;
        }

        return map.getObjEntity(getTargetEntityName(), true);
    }

    /**
     * Returns ObjRelationship that is the opposite of this ObjRelationship.
     * returns null if no such relationship exists.
     */
    public ObjRelationship getReverseRelationship() {
        // reverse the list
        List reversed = new ArrayList();
        Iterator rit = this.getDbRelationships().iterator();
        while (rit.hasNext()) {
            DbRelationship rel = (DbRelationship) rit.next();
            DbRelationship reverse = rel.getReverseRelationship();
            if (reverse == null)
                return null;

            reversed.add(0, reverse);
        }

        Entity target = this.getTargetEntity();
        Entity src = this.getSourceEntity();

        Iterator it = target.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.getTargetEntity() != src)
                continue;

            List otherRels = rel.getDbRelationships();
            if (reversed.size() != otherRels.size())
                continue;

            int len = reversed.size();
            boolean relsMatch = true;
            for (int i = 0; i < len; i++) {
                if (otherRels.get(i) != reversed.get(i)) {
                    relsMatch = false;
                    break;
                }
            }

            if (relsMatch)
                return rel;
        }

        return null;
    }

    /**
     * Returns a list of underlying DbRelationships.
     */
    public List getDbRelationships() {
        return dbRelationshipsRef;
    }

    /** Appends a DbRelationship to the existing list of DbRelationships. */
    public void addDbRelationship(DbRelationship dbRel) {
        //Adding a second is creating a flattened relationship.
        //Ensure that the new relationship properly continues on the flattened
        // path
        int numDbRelationships = dbRelationships.size();
        if (numDbRelationships > 0) {
            DbRelationship lastRel =
                (DbRelationship) dbRelationships.get(numDbRelationships - 1);
            if (!lastRel
                .getTargetEntityName()
                .equals(dbRel.getSourceEntity().getName())) {
                throw new CayenneRuntimeException(
                    "Error adding db relationship "
                        + dbRel
                        + " to ObjRelationship "
                        + this
                        + " because the source of the newly added relationship "
                        + "is not the target of the previous relationship "
                        + "in the chain");
            }
            isFlattened = true;
            //Now there will be more than one dbRel - this is a flattened
            // relationship
        }

        EventManager.getDefaultManager().addListener(
            this,
            "dbRelationshipDidChange",
            RelationshipEvent.class,
            DbRelationship.PROPERTY_DID_CHANGE,
            dbRel);

        dbRelationships.add(dbRel);
        //Recalculate whether this relationship is readOnly,
        this.calculateReadOnlyValue();
        // and whether it is toMany
        this.calculateToManyValue();
    }

    /**
     * Removes the relationship <code>dbRel</code> from the list of
     * relationships.
     */
    public void removeDbRelationship(DbRelationship dbRel) {
        dbRelationships.remove(dbRel);
        //Do not listen any more
        EventManager.getDefaultManager().removeListener(
            this,
            DbRelationship.PROPERTY_DID_CHANGE,
            dbRel);
        //If we removed all but one dbRel, then it's no longer flattened
        if (dbRelationships.size() <= 1) {
            isFlattened = false;
        }
        this.calculateReadOnlyValue();
        this.calculateToManyValue();
    }

    public void clearDbRelationships() {
        dbRelationships.clear();
        this.isReadOnly = false;
        this.toMany = false;
    }

    //Recalculates whether a relationship is toMany or toOne, based on the
    // underlying db relationships
    private void calculateToManyValue() {
        //If there is a single toMany along the path, then the flattend
        // rel is toMany. If all are toOne, then the rel is toOne.
        // Simple (non-flattened) relationships form the degenerate case
        // taking the value of the single underlying dbrel.
        Iterator dbRelIterator = this.dbRelationships.iterator();
        while (dbRelIterator.hasNext()) {
            DbRelationship thisRel = (DbRelationship) dbRelIterator.next();
            if (thisRel.isToMany()) {
                this.toMany = true;
                return;
            }
        }
        this.toMany = false;
    }

    //Implements logic to calculate a new readonly value after having
    // added/removed dbRelationships
    private void calculateReadOnlyValue() {
        //Quickly filter the single dbrel case
        if (dbRelationships.size() < 2) {
            this.isReadOnly = false;
            return;
        }

        //Also quickly filter any really complex db rel cases
        if (dbRelationships.size() > 2) {
            this.isReadOnly = true;
            return;
        }

        //Now check for a toMany -> toOne series (to return false)
        DbRelationship firstRel = (DbRelationship) dbRelationships.get(0);
        DbRelationship secondRel = (DbRelationship) dbRelationships.get(1);

        //First toOne or second toMany means read only
        if (!firstRel.isToMany() || secondRel.isToMany()) {
            this.isReadOnly = true;
            return;
        }

        //Relationship type is in order, now we only have to check the
        // intermediate table
        DataMap map = firstRel.getTargetEntity().getDataMap();
        if (map == null) {
            throw new CayenneRuntimeException(
                this.getClass().getName()
                    + " could not obtain a DataMap for the destination of "
                    + firstRel.getName());
        }

        DbEntity intermediateEntity =
            map.getDbEntity(firstRel.getTargetEntityName(), true);
        List pkAttribs = intermediateEntity.getPrimaryKey();

        Iterator allAttribs = intermediateEntity.getAttributes().iterator();
        while (allAttribs.hasNext()) {
            if (!pkAttribs.contains(allAttribs.next())) {
                this.isReadOnly = true;
                return;
                //one of the attributes of intermediate entity is not in the
                // pk. Must be readonly
            }
        }
        this.isReadOnly = false;
    }

    /**
     * Returns a flag indicating whether modifying a target of such relationship 
     * in any way will not change the underlying table row of the source.
     * 
     * @since 1.1
     */
    public boolean isSourceIndependentFromTargetChange() {
        // note - call "isToPK" at the end of the chain, since
        // if it is to a dependent PK, we still should return true...
        return isToMany() || isFlattened() || isToDependentEntity() || !isToPK();
    }

    /**
     * Returns true if underlying DbRelationships point to dependent entity.
     */
    public boolean isToDependentEntity() {
        return ((DbRelationship) dbRelationships.get(0)).isToDependentPK();
    }

    /**
     * Returns true if the underlying DbRelationships point to a at least one 
     * of the columns of the target entity.
     * 
     * @since 1.1
     */
    public boolean isToPK() {
        return ((DbRelationship) dbRelationships.get(0)).isToPK();
    }

    /**
     * Returns true if the relationship is a "flattened" relationship.
     * Flattened ObjRelationship transparently represents a series of
     * DbRelationships, also called "relationship path". All flattened
     * relationships are "readable", but only those formed across a many-many
     * join table (with no custom attributes other than foreign keys) can be
     * automatically written.
     * 
     * @see #isReadOnly
     * @return flag indicating if the relationship is flattened or not
     */
    public boolean isFlattened() {
        return isFlattened;
    }

    /**
     * Returns true if the relationship is flattened, but is not of the single
     * case that can have automatic write support. Otherwise, it returns false.
     * 
     * @return flag indicating if the relationship is read only or not
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Returns the deleteRule. The delete rule is a constant from the
     * DeleteRule class, and specifies what should happen to the destination
     * object when the source object is deleted.
     * 
     * @return int a constant from DeleteRule
     * @see #setDeleteRule
     */
    public int getDeleteRule() {
        return deleteRule;
    }

    /**
     * Sets the deleteRule.
     * 
     * @param deleteRule
     *            The deleteRule to set
     * @see #getDeleteRule
     * @throws IllegalArgumentException
     *             if the value is not a known value.
     */
    public void setDeleteRule(int value) {
        if ((value != DeleteRule.CASCADE)
            && (value != DeleteRule.DENY)
            && (value != DeleteRule.NULLIFY)
            && (value != DeleteRule.NO_ACTION)) {

            throw new IllegalArgumentException(
                "Delete rule value "
                    + value
                    + " is not a constant from the DeleteRule class");
        }
        this.deleteRule = value;
    }

    public void dbRelationshipDidChange(RelationshipEvent event) {
        this.calculateToManyValue();
    }
}
