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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DbAttributeListener;
import org.objectstyle.cayenne.map.event.DbEntityListener;
import org.objectstyle.cayenne.map.event.DbRelationshipListener;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * A DbEntity is a mapping descriptor that defines a structure of a database table.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbEntity extends Entity implements DbEntityListener, 
												DbAttributeListener, 
												DbRelationshipListener {

    protected String catalog;
    protected String schema;
    protected List primaryKey;
    protected List primaryKeyRef;

    /**
     * @since 1.2
     */
    protected Collection generatedAttributes;

    /**
     * @since 1.2
     */
    protected Collection generatedAttributesRef;

    protected DbKeyGenerator primaryKeyGenerator;

    /**
     * Creates an unnamed DbEntity.
     */
    public DbEntity() {
        super();

        this.primaryKey = new ArrayList(2);
        this.primaryKeyRef = Collections.unmodifiableList(primaryKey);

        this.generatedAttributes = new ArrayList(2);
        this.generatedAttributesRef = Collections
                .unmodifiableCollection(generatedAttributes);
    }

    /**
     * Creates a named DbEntity.
     */
    public DbEntity(String name) {
        this();
        this.setName(name);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<db-entity name=\"");
        encoder.print(Util.encodeXmlAttribute(getName()));
        encoder.print('\"');

        if (getSchema() != null && getSchema().trim().length() > 0) {
            encoder.print(" schema=\"");
            encoder.print(Util.encodeXmlAttribute(getSchema().trim()));
            encoder.print('\"');
        }

        if (getCatalog() != null && getCatalog().trim().length() > 0) {
            encoder.print(" catalog=\"");
            encoder.print(Util.encodeXmlAttribute(getCatalog().trim()));
            encoder.print('\"');
        }

        encoder.println('>');

        encoder.indent(1);
        encoder.print(getAttributeMap());

        if (getPrimaryKeyGenerator() != null) {
            getPrimaryKeyGenerator().encodeAsXML(encoder);
        }

        encoder.indent(-1);
        encoder.println("</db-entity>");
    }

    /**
     * Returns table name including schema, if present.
     */
    public String getFullyQualifiedName() {
        return (schema != null) ? schema + '.' + getName() : getName();
    }

    /**
     * Returns database schema of this table.
     * 
     * @return table's schema, null if not set.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the database schema name of the table described by this DbEntity.
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns the catalog name of the table described by this DbEntity.
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Sets the catalog name of the table described by this DbEntity.
     */
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    /**
     * Returns an unmodifiable list of DbAttributes representing the primary key of the
     * table described by this DbEntity.
     */
    public List getPrimaryKey() {
        return primaryKeyRef;
    }

    /**
     * Returns an unmodifiable collection of DbAttributes that are generated by the
     * database.
     * 
     * @since 1.2
     */
    public Collection getGeneratedAttributes() {
        return generatedAttributesRef;
    }

    public void addAttribute(Attribute attr) {
        super.addAttribute(attr);
        this.dbAttributeAdded(new AttributeEvent(this, attr, this, MapEvent.ADD));
    }

    /**
     * Removes attribute from the entity, removes any relationship joins containing this
     * attribute.
     * 
     * @see org.objectstyle.cayenne.map.Entity#removeAttribute(String)
     */
    public void removeAttribute(String attrName) {
        Attribute attr = getAttribute(attrName);
        if (attr == null) {
            return;
        }

        DataMap map = getDataMap();
        if (map != null) {
            Iterator ents = map.getDbEntities().iterator();
            while (ents.hasNext()) {
                DbEntity ent = (DbEntity) ents.next();
                Iterator it = ent.getRelationships().iterator();
                while (it.hasNext()) {
                    DbRelationship rel = (DbRelationship) it.next();
                    Iterator joins = rel.getJoins().iterator();
                    while (joins.hasNext()) {
                        DbJoin join = (DbJoin) joins.next();
                        if (join.getSource() == attr || join.getTarget() == attr) {
                            joins.remove();
                        }
                    }
                }
            }
        }

        super.removeAttribute(attrName);
        this.dbAttributeRemoved(new AttributeEvent(this, attr, this, MapEvent.REMOVE));
    }

    public void clearAttributes() {
        super.clearAttributes();
        // post dummy event for no specific attribute
        this.dbAttributeRemoved(new AttributeEvent(this, null, this, MapEvent.REMOVE));
    }

    public Iterator resolvePathComponents(Expression pathExp) throws ExpressionException {
        if (pathExp.getType() != Expression.DB_PATH) {
            throw new ExpressionException("Invalid expression type: '"
                    + pathExp.expName()
                    + "',  DB_PATH is expected.");
        }

        return new PathIterator((String) pathExp.getOperand(0));
    }

    public void setPrimaryKeyGenerator(DbKeyGenerator primaryKeyGenerator) {
        this.primaryKeyGenerator = primaryKeyGenerator;
        if (primaryKeyGenerator != null) {
            primaryKeyGenerator.setDbEntity(this);
        }
    }

    public DbKeyGenerator getPrimaryKeyGenerator() {
        return primaryKeyGenerator;
    }

    /**
     * DbEntity property changed. 
	 * May be name, attribute or relationship added or removed, etc. 
	 * Attribute and relationship property changes are handled in
	 * respective listeners.
	 * @since 1.2 
	 */
    public void dbEntityChanged(EntityEvent e){
        if ((e == null) || (e.getEntity() != this)) {
            // not our concern
            return;
        }
        
        // handle entity name changes
	    if (e.getId() == EntityEvent.CHANGE && e.isNameChange()){
	        String newName = e.getNewName();
	        DataMap map = getDataMap();
	        if (map != null) {
	            // handle all of the relationship target names that need to be changed
	            Iterator ents = map.getDbEntities().iterator();
	            while (ents.hasNext()) {
	               DbEntity dbe = (DbEntity) ents.next();
	               Iterator rit = dbe.getRelationships().iterator();
	               while (rit.hasNext()){
	                   DbRelationship rel = (DbRelationship) rit.next();
		               if (rel.getTargetEntity() == this){
		                   rel.setTargetEntityName(newName);
		               }
	               }
	            }
	            // get all of the related object entities
	            ents = map.getMappedEntities(this).iterator();
	            while (ents.hasNext()) {
	               ObjEntity oe = (ObjEntity) ents.next();
	               if (oe.getDbEntity() == this){
	                   oe.setDbEntityName(newName);
	               }
	            }
	        }
	    }
	}

    /** New entity has been created/added.*/
	public void dbEntityAdded(EntityEvent e){
	    // does nothing currently
	}

	/** Entity has been removed.*/
	public void dbEntityRemoved(EntityEvent e){
	    // does nothing currently
	}

    public void dbAttributeAdded(AttributeEvent e) {
        this.handleAttributeUpdate(e);
    }

    public void dbAttributeChanged(AttributeEvent e) {
        this.handleAttributeUpdate(e);
    }

    public void dbAttributeRemoved(AttributeEvent e) {
        this.handleAttributeUpdate(e);
    }

    private void handleAttributeUpdate(AttributeEvent e) {
        if ((e == null) || (e.getEntity() != this)) {
            // not our concern
            return;
        }

        // catch clearing (event with null ('any') DbAttribute)
        Attribute attr = e.getAttribute();
        if ((attr == null) && (this.attributes.isEmpty())) {
            this.primaryKey.clear();
            this.generatedAttributes.clear();
            return;
        }

        // make sure we handle a DbAttribute
        if (!(attr instanceof DbAttribute)) {
            return;
        }

        DbAttribute dbAttr = (DbAttribute) attr;
        
        // handle attribute name changes
	    if (e.getId() == AttributeEvent.CHANGE && e.isNameChange()){
	        String oldName = e.getOldName();
	        String newName = e.getNewName();
	        
	        DataMap map = getDataMap();
	        if (map != null) {
	            Iterator ents = map.getDbEntities().iterator();
	            while (ents.hasNext()) {
	                DbEntity ent = (DbEntity) ents.next();

	                // handle all of the dependent object entity attribute changes
	                Iterator it = map.getMappedEntities(ent).iterator();
	                while (it.hasNext()) {
	                    ObjEntity oe = (ObjEntity) it.next();
	                    Iterator ait = oe.getAttributes().iterator();
	                    while (ait.hasNext()) {
	                        ObjAttribute oa = (ObjAttribute) ait.next();
	                        if (oa.getDbAttribute() == dbAttr){
	                            oa.setDbAttributeName(newName);
	                        }
	                    }
	                }
	                
	                // handle all of the relationships / joins that use the changed attribute
	                it = ent.getRelationships().iterator();
	                while (it.hasNext()) {
	                    DbRelationship rel = (DbRelationship) it.next();
	                    Iterator joins = rel.getJoins().iterator();
	                    while (joins.hasNext()) {
	                        DbJoin join = (DbJoin) joins.next();
	                        if (join.getSource() == dbAttr){
	                            join.setSourceName(newName);
	                        }
	                        if (join.getTarget() == dbAttr){
	                            join.setTargetName(newName);
	                        }	                        
	                    }
	                }
	            }
	        }

	        // clear the attribute out of the collection
	        attributes.remove(oldName);
	        
	        // add the attribute back in with the new name
	        attributes.put(newName, dbAttr);
	    }

        // handle PK refresh
        if (primaryKey.contains(dbAttr) || dbAttr.isPrimaryKey()) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    this.primaryKey.add(attr);
                    break;

                case MapEvent.REMOVE:
                    this.primaryKey.remove(attr);
                    break;

                default:
                    // generic update
                    this.primaryKey.clear();
                    Iterator it = this.getAttributes().iterator();
                    while (it.hasNext()) {
                        DbAttribute dba = (DbAttribute) it.next();
                        if (dba.isPrimaryKey()) {
                            this.primaryKey.add(dba);
                        }
                    }
            }
        }

        // handle generated key refresh
        if (generatedAttributes.contains(dbAttr) || dbAttr.isGenerated()) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    this.generatedAttributes.add(attr);
                    break;

                case MapEvent.REMOVE:
                    this.generatedAttributes.remove(attr);
                    break;

                default:
                    // generic update
                    this.generatedAttributes.clear();
                    Iterator it = this.getAttributes().iterator();
                    while (it.hasNext()) {
                        DbAttribute dba = (DbAttribute) it.next();
                        if (dba.isGenerated()) {
                            this.generatedAttributes.add(dba);
                        }
                    }
            }
        }
    }

    /** Relationship property changed. */
	public void dbRelationshipChanged(RelationshipEvent e){
        if ((e == null) || (e.getEntity() != this)) {
            // not our concern
            return;
        }

        Relationship rel = e.getRelationship();
        // make sure we handle a DbRelationship
        if (!(rel instanceof DbRelationship)) {
            return;
        }

        DbRelationship dbRel = (DbRelationship) rel;
        
        // handle relationship name changes
	    if (e.getId() == RelationshipEvent.CHANGE && e.isNameChange()){
	        String oldName = e.getOldName();
	        String newName = e.getNewName();
	        
	        DataMap map = getDataMap();
	        if (map != null) {
	            // finds all object entities with a db relationship path to the renamed relationship
	            Iterator ents = map.getObjEntities().iterator();
	            while (ents.hasNext()) {
	               ObjEntity oe = (ObjEntity) ents.next();
                   Iterator rit = oe.getRelationships().iterator();
                   while (rit.hasNext()) {
                        ObjRelationship or = (ObjRelationship) rit.next();
                        // rename the db relationship path with the new name
                        if (or.getDbRelationshipPath().equals(oldName)){
                            or.setDbRelationshipPath(newName);
                        }
                    }
	            }
	        }
	        
	        // clear the relationship out of the collection
	        relationships.remove(oldName);
	        
	        // add the relationship back in with the new name
	        relationships.put(newName, dbRel);
	    }
	}
	
	/** Relationship has been created/added.*/
	public void dbRelationshipAdded(RelationshipEvent e){
	    // does nothing currently
	}
	
	/** Relationship has been removed.*/
	public void dbRelationshipRemoved(RelationshipEvent e){
	    // does nothing currently
	}

    /**
     * Returns true if there is full replacement id is attached to an ObjectId. "Full" means
     * that all PK columns are present and only PK columns are present.
     * 
     * @since 1.2
     */
    public boolean isFullReplacementIdAttached(ObjectId id) {
        if (!id.isReplacementIdAttached()) {
            return false;
        }

        Map replacement = id.getReplacementIdMap();
        Collection pk = getPrimaryKey();
        if(pk.size() != replacement.size()) {
            return false;
        }
        
        Iterator it = pk.iterator();
        while(it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            if(!replacement.containsKey(attribute.getName())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Transforms Expression rooted in this entity to an analogous expression rooted in
     * related entity.
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

        return expression.transform(new RelationshipPathConverter(relationshipPath));
    }

    final class RelationshipPathConverter implements Transformer {

        String relationshipPath;

        RelationshipPathConverter(String relationshipPath) {
            this.relationshipPath = relationshipPath;
        }

        public Object transform(Object input) {
            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;

            if (expression.getType() != Expression.DB_PATH) {
                return input;
            }

            String path = (String) expression.getOperand(0);
            String converted = translatePath(path);
            Expression transformed = ExpressionFactory
                    .expressionOfType(Expression.DB_PATH);
            transformed.setOperand(0, converted);
            return transformed;
        }

        String translatePath(String path) {

            // algorithm to determine the translated path:
            // 1. If relationship path equals to input, travel one step back, and then one
            // step forward.
            // 2. If input completely includes relationship path, use input's remaining
            // tail.
            // 3. If relationship path and input have none or some leading components in
            // common,
            //    (a) strip common leading part;
            //    (b) reverse the remaining relationship part;
            //    (c) append remaining input to the reversed remaining relationship.

            // case (1)
            if (path.equals(relationshipPath)) {

                LinkedList finalPath = new LinkedList();
                Iterator it = resolvePathComponents(path);

                // just do one step back and one step forward to create correct joins...
                // find last rel...
                DbRelationship lastDBR = null;

                while (it.hasNext()) {
                    // relationship path components must be DbRelationships
                    lastDBR = (DbRelationship) it.next();
                }

                if (lastDBR != null) {
                    prependReversedPath(finalPath, lastDBR);
                    appendPath(finalPath, lastDBR);
                }

                return convertToPath(finalPath);
            }

            // case (2)
            String relationshipPathWithDot = relationshipPath + Entity.PATH_SEPARATOR;
            if (path.startsWith(relationshipPathWithDot)) {
                return path.substring(relationshipPathWithDot.length());
            }

            // case (3)
            Iterator pathIt = resolvePathComponents(path);
            Iterator relationshipIt = resolvePathComponents(relationshipPath);

            // for inserts from the both ends use LinkedList
            LinkedList finalPath = new LinkedList();

            while (relationshipIt.hasNext() && pathIt.hasNext()) {
                // relationship path components must be DbRelationships
                DbRelationship nextDBR = (DbRelationship) relationshipIt.next();

                // expression components may be attributes or relationships
                MapObject next = (MapObject) pathIt.next();

                if (nextDBR != next) {
                    // found split point
                    // consume the last iteration components,
                    // then break out to finish the iterators independenly
                    prependReversedPath(finalPath, nextDBR);
                    appendPath(finalPath, next);
                    break;
                }

                break;
            }

            // append remainder of the relationship, reversing it
            while (relationshipIt.hasNext()) {
                DbRelationship nextDBR = (DbRelationship) relationshipIt.next();
                prependReversedPath(finalPath, nextDBR);
            }

            while (pathIt.hasNext()) {
                // components may be attributes or relationships
                MapObject next = (MapObject) pathIt.next();
                appendPath(finalPath, next);
            }

            return convertToPath(finalPath);
        }

        private String convertToPath(List path) {
            StringBuffer converted = new StringBuffer();
            int len = path.size();
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    converted.append(Entity.PATH_SEPARATOR);
                }

                converted.append(path.get(i));
            }

            return converted.toString();
        }

        private void prependReversedPath(LinkedList finalPath, DbRelationship relationship) {
            DbRelationship revNextDBR = relationship.getReverseRelationship();

            if (revNextDBR == null) {
                throw new CayenneRuntimeException(
                        "Unable to find reverse DbRelationship for "
                                + relationship.getSourceEntity().getName()
                                + Entity.PATH_SEPARATOR
                                + relationship.getName()
                                + ".");
            }

            finalPath.addFirst(revNextDBR.getName());
        }

        private void appendPath(LinkedList finalPath, MapObject pathComponent) {
            finalPath.addLast(pathComponent.getName());
        }
    }
}