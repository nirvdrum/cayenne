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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

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
    protected String superEntityName;
    protected Expression qualifier;
    protected boolean readOnly;

    public ObjEntity() {
        super();
    }

    public ObjEntity(String name) {
        this();
        this.setName(name);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<obj-entity name=\"");
        encoder.print(getName());

        if (getSuperEntityName() != null) {
            encoder.print(" superEntityName=\"");
            encoder.print(getSuperEntityName());
            encoder.print("\"");
        }

        if (getClassName() != null) {
            encoder.print("\" className=\"");
            encoder.print(getClassName());
        }

        if (isReadOnly()) {
            encoder.print("\" readOnly=\"true");
        }

        encoder.print('\"');

        if (getDbEntity() != null) {
            encoder.print(" dbEntityName=\"");
            encoder.print(getDbEntity().getName());
            encoder.print('\"');
        }

        if (getSuperClassName() != null) {
            encoder.print(" superClassName=\"");
            encoder.print(getSuperClassName());
            encoder.print("\"");
        }

        encoder.println('>');
        encoder.indent(1);

        if (qualifier != null) {
            encoder.print("<qualifier>");
            qualifier.encodeAsXML(encoder);
            encoder.println("</qualifier>");
        }

        encoder.print(getAttributeMap());

        encoder.indent(-1);
        encoder.println("</obj-entity>");
    }

    /**
     * Returns Java class of persistent objects described by this entity.
     * Casts any thrown exceptions into CayenneRuntimeException.
     */
    public Class getJavaClass() {
        try {
            return Class.forName(this.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException(
                "Failed to load class for name '"
                    + this.getClassName()
                    + "': "
                    + e.getMessage(),
                e);
        }
    }

    /**
     * Returns a qualifier that imposes a limit on what objects
     * belong to this entity.
     * 
     * @since 1.1
     */
    public Expression getQualifier() {
        return qualifier;
    }

    /**
     * Returns an entity name for a parent entity in the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public String getSuperEntityName() {
        return superEntityName;
    }

    /**
     * Sets a qualifier that imposes a limit on what objects
     * belong to this entity.
     * 
     * @since 1.1
     */
    public void setQualifier(Expression expression) {
        qualifier = expression;
    }

    /**
     * Sets an entity name for a parent entity in the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public void setSuperEntityName(String string) {
        superEntityName = string;
    }

    /** 
     * Returns the name of the corresponding data object class 
     */
    public String getClassName() {
        return className;
    }

    /** 
     * Sets the name of the data object class described by this obj entity.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns a fully-qualified name of the super class of the DataObject class.
     * This value is used as a hint for class generation.
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * Sets a fully-qualified name of the super class of the DataObject class.
     * This value is used as a hint for class generation.
     */
    public void setSuperClassName(String parentClassName) {
        this.superClassName = parentClassName;
    }

    /**
     * Returns a "super" entity in the entity inheritance hierarchy.
     * 
     * @since 1.1
     */
    public ObjEntity getSuperEntity() {
        return (superEntityName != null)
            ? getDataMap().getObjEntity(superEntityName)
            : null;
    }

    /** 
     * Returns a DbEntity associated with this ObjEntity. 
     */
    public DbEntity getDbEntity() {
        return dbEntity;
    }

    /** 
     * Sets the DbEntity used by this ObjEntity. 
     */
    public void setDbEntity(DbEntity dbEntity) {
        if (superEntityName != null) {
            throw new UnsupportedOperationException("Setting DBEntity is not allowed in inherited entities.");
        }

        this.dbEntity = dbEntity;
    }

    /**
     * Returns a named attribute that either belongs to this ObjEntity or is
     * inherited. Returns null if no matching attribute is found.
     */
    public Attribute getAttribute(String name) {
        Attribute attribute = super.getAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        if (superEntityName == null) {
            return null;
        }

        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getAttribute(name) : null;
    }

    /**
     * Returns a SortedMap of all attributes that either belong to this ObjEntity 
     * or inherited.
     */
    public SortedMap getAttributeMap() {
        if (superEntityName == null) {
            return super.getAttributeMap();
        }

        SortedMap attributeMap = new TreeMap();
        appendAttributes(attributeMap);
        return attributeMap;
    }

    /**
     * Recursively appends all attributes in the entity inheritance hierarchy.
     */
    final void appendAttributes(Map map) {
        map.putAll(super.getAttributeMap());

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            superEntity.appendAttributes(map);
        }
    }

    /**
     * Returns a Collection of all attributes that either belong to 
     * this ObjEntity or inherited.
     */
    public Collection getAttributes() {
        if (superEntityName == null) {
            return super.getAttributes();
        }

        return getAttributeMap().values();
    }

    /**
     * Returns a named Relationship that either belongs to this ObjEntity or is
     * inherited. Returns null if no matching attribute is found.
     */
    public Relationship getRelationship(String name) {
        Relationship relationship = super.getRelationship(name);
        if (relationship != null) {
            return relationship;
        }

        if (superEntityName == null) {
            return null;
        }

        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getRelationship(name) : null;
    }

    public SortedMap getRelationshipMap() {
        if (superEntityName == null) {
            return super.getRelationshipMap();
        }

        SortedMap relationshipMap = new TreeMap();
        appendRelationships(relationshipMap);
        return relationshipMap;
    }

    /**
     * Recursively appends all relationships in the entity inheritance hierarchy.
     */
    final void appendRelationships(Map map) {
        map.putAll(super.getRelationshipMap());

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            superEntity.appendRelationships(map);
        }
    }

    public Collection getRelationships() {
        if (superEntityName == null) {
            return super.getRelationships();
        }

        return getRelationshipMap().values();
    }

    /**
     * Returns ObjAttribute of this entity that maps to <code>dbAttribute</code>
     * parameter. Returns null if no such attribute is found.
     */
    public ObjAttribute getAttributeForDbAttribute(DbAttribute dbAttribute) {
        Iterator it = getAttributeMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjAttribute objAttr = (ObjAttribute) entry.getValue();
            if (objAttr.getDbAttribute() == dbAttribute)
                return objAttr;
        }
        return null;
    }

    /**
     * Returns ObjRelationship of this entity that maps to
     * <code>dbRelationship</code> parameter. Returns null if no
     * such relationship is found.
     */
    public ObjRelationship getRelationshipForDbRelationship(DbRelationship dbRelationship) {
        Iterator it = getRelationshipMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjRelationship objRel = (ObjRelationship) entry.getValue();

            List relList = objRel.getDbRelationships();
            if (relList.size() != 1) {
                continue;
            }

            if (relList.get(0) == dbRelationship) {
                return objRel;
            }
        }
        return null;
    }

    /**
     * Creates an id snapshot (the key/value pairs for the pk of the object)
     * from the values in object snapshot.
     * If needed attributes are missing in a snapshot or if it is null,
     * CayenneRuntimeException is thrown.
     * 
     * @deprecated Since 1.1 this method is no longer relevant in Cayenne. It is deprecated 
     * to decouple mapping layer from the access layer implementation.
     */
    public Map idSnapshotMapFromSnapshot(Map objectSnapshot) {
        // create a cheaper map for the mosty common case - 
        // single attribute id.
        List pk = getDbEntity().getPrimaryKey();
        if (pk.size() == 1) {
            DbAttribute attr = (DbAttribute) pk.get(0);
            Object val = objectSnapshot.get(attr.getName());
            return Collections.singletonMap(attr.getName(), val);
        }

        // multiple attributes in id...
        Map idMap = new HashMap(pk.size() * 2);
        Iterator it = pk.iterator();
        while (it.hasNext()) {
            DbAttribute attr = (DbAttribute) it.next();
            Object val = objectSnapshot.get(attr.getName());
            if (val == null) {
                throw new CayenneRuntimeException(
                    "Null value for '"
                        + attr.getName()
                        + "'. Snapshot: "
                        + objectSnapshot);
            }

            idMap.put(attr.getName(), val);
        }
        return idMap;
    }

    /**
     * Creates an object id from the values in object snapshot.
     * If needed attributes are missing in a snapshot or if it is null,
     * CayenneRuntimeException is thrown.
     * 
     * @deprecated Since 1.1 use {@link org.objectstyle.cayenne.DataRow#createObjectId(ObjEntity)}. 
     * This method is deprecated to remove the dependency of mapping layer from the access layer.
     */
    public ObjectId objectIdFromSnapshot(Map objectSnapshot) {
        DataRow dataRow =
            (objectSnapshot instanceof DataRow)
                ? (DataRow) objectSnapshot
                : new DataRow(objectSnapshot);
        return dataRow.createObjectId(this);
    }

    /** 
     * Clears all the mapping between this obj entity and its current db entity.
     *  Clears mapping between entities, attributes and relationships. 
     */
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
            ((ObjRelationship) rels.next()).clearDbRelationships();
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

    public Iterator resolvePathComponents(Expression pathExp)
        throws ExpressionException {

        // resolve DB_PATH if we can
        if (pathExp.getType() == Expression.DB_PATH) {
            if (getDbEntity() == null) {
                throw new ExpressionException(
                    "Can't resolve DB_PATH '" + pathExp + "', DbEntity is not set.");
            }

            return getDbEntity().resolvePathComponents(pathExp);
        }

        if (pathExp.getType() == Expression.OBJ_PATH) {
            return new PathIterator((String) pathExp.getOperand(0));
        }

        throw new ExpressionException(
            "Invalid expression type: '"
                + pathExp.expName()
                + "',  OBJ_PATH is expected.");
    }

    /**
     * @deprecated Unused since 1.1
     */
    protected void validateQueryRoot(Query query) throws IllegalArgumentException {

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

    /**
     * @deprecated Unused since 1.1
     */
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

            if (!readOnly
                && objAttr.isCompound()
                && !objAttr.mapsToDependentDbEntity()) {
                throw new CayenneException(
                    head
                        + "ObjAttribute: "
                        + objAttr.getName()
                        + " compound, read only.");
            }
        }
    }

    /**
     * Transforms Expression rooted in this entity to an analogous expression 
     * rooted in related entity.
     * 
     * @since 1.1
     */
    public Expression translateToRelatedEntity(
        Expression expression,
        String relationshipPath) {

        if (expression == null) {
            return null;
        }

        if (relationshipPath == null) {
            return expression;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException(
                "Can't transform expression, no DbEntity for '" + getName() + "'.");
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity

        DBPathConverter transformer = new DBPathConverter();

        String dbPath = transformer.toDbPath(resolvePathComponents(relationshipPath));
        Expression dbClone = expression.transform(transformer);

        return getDbEntity().translateToRelatedEntity(dbClone, dbPath);
    }

    final class DBPathConverter implements Transformer {
        // TODO: make it a public method - resolveDBPathComponents or something... 
        // seems generally useful
        String toDbPath(Iterator objectPathComponents) {
            StringBuffer buf = new StringBuffer();
            while (objectPathComponents.hasNext()) {
                Object component = objectPathComponents.next();

                Iterator dbSubpath;

                if (component instanceof ObjRelationship) {
                    dbSubpath =
                        ((ObjRelationship) component).getDbRelationships().iterator();
                }
                else if (component instanceof ObjAttribute) {
                    dbSubpath = ((ObjAttribute) component).getDbPathIterator();
                }
                else {
                    throw new CayenneRuntimeException(
                        "Unknown path component: " + component);
                }

                while (dbSubpath.hasNext()) {
                    MapObject subComponent = (MapObject) dbSubpath.next();
                    if (buf.length() > 0) {
                        buf.append(Entity.PATH_SEPARATOR);
                    }

                    buf.append(subComponent.getName());
                }
            }

            return buf.toString();
        }

        public Object transform(Object input) {

            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;

            if (expression.getType() != Expression.OBJ_PATH) {
                return input;
            }

            // convert obj_path to db_path

            String converted = toDbPath(resolvePathComponents(expression));
            Expression transformed =
                ExpressionFactory.expressionOfType(Expression.DB_PATH);
            transformed.setOperand(0, converted);
            return transformed;
        }
    }
}
