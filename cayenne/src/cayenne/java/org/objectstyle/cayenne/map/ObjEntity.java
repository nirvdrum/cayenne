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
package org.objectstyle.cayenne.map;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * ObjEntity is a mapping descriptor for a DataObject Java class.
 * It contains the information about the Java class itself, as well
 * as its mapping to the DbEntity layer.
 *
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjEntity extends Entity {
    protected String superClassName;
    protected String className;
    protected DbEntity dbEntity;
    protected boolean readOnly;

    public ObjEntity() {
    	super();
    }

    public ObjEntity(String name) {
    	this();
        this.setName(name);
    }

    /** Returns the name of the corresponding data object class */
    public String getClassName() {
        return className;
    }

    /** Sets the name of the data object class described by this obj entity*/
    public void setClassName(String className) {
        this.className = className;
    }
    /**
     *
     * Returns the fully qualified name of the super class of the data object class for this entity
     * Used in the modeller and in class generation only, not at "runtime"
     * @return String
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * Sets the name of the super class of the data object class for this entity
     * Used in the modeller and in class generation only, not at "runtime"
     * @param superClassName fully qualified class namee
     */
    public void setSuperClassName(String parentClassName) {
        this.superClassName = parentClassName;
    }

    /** Returns a DbEntity that this ObjEntity is mapped to. */
    public DbEntity getDbEntity() {
        return dbEntity;
    }

    /** Sets the DbEntity used by this ObjEntity. */
    public void setDbEntity(DbEntity dbEntity) {
        this.dbEntity = dbEntity;
    }

    /**
     * Returns ObjAttribute of this entity that maps to <code>dbAttr</code>
     * parameter. Returns null if no such attribute is found.
     */
    public ObjAttribute getAttributeForDbAttribute(DbAttribute dbAttr) {
        Iterator it = getAttributeMap().values().iterator();
        while (it.hasNext()) {
            ObjAttribute objAttr = (ObjAttribute) it.next();
            if (objAttr.getDbAttribute() == dbAttr)
                return objAttr;
        }
        return null;
    }

    /**
     * Returns ObjRelationship of this entity that maps to
     * <code>dbRel</code> parameter. Returns null if no
     * such relationship is found.
     */
    public ObjRelationship getRelationshipForDbRelationship(DbRelationship dbRel) {
        Iterator it = getRelationshipMap().values().iterator();
        while (it.hasNext()) {
            ObjRelationship objRel = (ObjRelationship)it.next();
            List relList = objRel.getDbRelationships();
            if (relList.size() != 1)
                continue;

            if (relList.get(0) == dbRel)
                return objRel;
        }
        return null;
    }

    /**
     * Creates an object id from the values in object snapshot.
     * If needed attributes are missing in a snapshot or if it is null,
     * CayenneRuntimeException is thrown.
     */
    public ObjectId objectIdFromSnapshot(Map objectSnapshot) {
        Map idMap = new HashMap();
        Iterator it = getDbEntity().getPrimaryKey().iterator();
        while (it.hasNext()) {
            DbAttribute attr = (DbAttribute)it.next();
            Object val = objectSnapshot.get(attr.getName());
            if (val == null) {
                throw new CayenneRuntimeException(
                    "Invalid snapshot value for '"
                        + attr.getName()
                        + "'. Must be present and not null.");
            }

            idMap.put(attr.getName(), val);
        }

        Class objClass;
        try {
            objClass = Class.forName(this.getClassName());
        } catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException(
                "Failed to load class for name "
                    + this.getClassName()
                    + " because "
                    + e.getMessage());
        }
        ObjectId id = new ObjectId(objClass, idMap);
        return id;
    }

    /** Clears all the mapping between this obj entity and its current db entity.
     *  Clears mapping between entities, attributes and relationships. */
    public void clearDbMapping() {
        if (dbEntity == null)
            return;

        Iterator it = getAttributeMap().values().iterator();
        while (it.hasNext()) {
            ObjAttribute objAttr = (ObjAttribute) it.next();
            DbAttribute dbAttr = objAttr.getDbAttribute();
            if (null != dbAttr) {
                objAttr.setDbAttribute(null);
            }
        }

        Iterator rels = this.getRelationships().iterator();
        while (rels.hasNext()) {
            ((ObjRelationship)rels.next()).clearDbRelationships();
        }

        dbEntity = null;
    }

    /**
     * Returns <code>true</code> if this ObjEntity represents
     * a set of read-only objects.
     *
     * @return boolean
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    protected void validateQueryRoot(Query query)
        throws IllegalArgumentException {

        if ((query.getRoot() instanceof Class)
            && ((Class) query.getRoot()).getName().equals(getClassName())) {
            return;
        }

        if (query.getRoot() == this) {
            return;
        }

        if (Util.nullSafeEquals(getName(), query.getRoot())) {
            return;
        }

        throw new IllegalArgumentException(
            "Wrong query root for ObjEntity: " + query.getRoot());
    }

    public void validate() throws CayenneException {
        if (getName() == null)
            throw new CayenneException("ObjEntity name not defined.");

        String head = "ObjEntity: " + getName();

        if (getDbEntity() == null)
            throw new CayenneException(head + "DbEntity not defined.");

        if (getClassName() == null)
            throw new CayenneException(head + "ObjEntity's class not defined.");

        Iterator it = getAttributeMap().values().iterator();
        while (it.hasNext()) {
            ObjAttribute objAttr = (ObjAttribute) it.next();
            objAttr.validate();

            if (!readOnly &&
                objAttr.isCompound() &&
                !objAttr.mapsToDependentDbEntity()) {
                throw new CayenneException(head + "ObjAttribute: " + objAttr.getName() + " compound, read only.");
            }
        }
    }
}
