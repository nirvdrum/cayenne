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

import java.util.*;

/** 
 * A DbEntity is a mapping descriptor that defines a structure of a database table. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbEntity extends Entity {
	protected String catalog;
	protected String schema;

   /**
    * Creates an unnamed DbEntity. 
    */
	public DbEntity() {}

    /**
     * Creates a named DbEntity. 
     */
	public DbEntity(String name) {
		setName(name);
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
	 * Sets the database schema name of the table described
	 * by this DbEntity. 
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

	/** 
	 * Returns the catalog name of the table described
	 * by this DbEntity. 
	 */
	public String getCatalog() {
		return catalog;
	}

	/** 
	 * Sets the catalog name of the table described
	 * by this DbEntity. 
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/** 
	 * Returns a list of DbAttributes representing the primary
	 * key of the table described by this DbEntity. 
	 */
	public List getPrimaryKey() {
		List list = new ArrayList();
		Iterator it = this.getAttributeList().iterator();
		while (it.hasNext()) {
			DbAttribute dba = (DbAttribute) it.next();
			if (dba.isPrimaryKey())
				list.add(dba);
		}
		return list;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("DbEntity:");
		sb.append("\nTable name: ").append(name);

		// 1. print attributes
		Iterator attIt = attributes.values().iterator();
		while (attIt.hasNext()) {
			DbAttribute dbAttribute = (DbAttribute) attIt.next();
			String name = dbAttribute.getName();
			int type = dbAttribute.getType();
			sb.append("\n   Column name: ").append(name);
			if (dbAttribute.isPrimaryKey())
				sb.append(" (pk)");

			sb.append("\n   Column type: ").append(type);
			sb.append("\n------------------");
		}

		// 2. print relationships
		Iterator relIt = getRelationshipList().iterator();
		while (relIt.hasNext()) {
			DbRelationship dbRel = (DbRelationship) relIt.next();
			sb.append("\n   Rel. to: ").append(
				dbRel.getTargetEntity().getName());
			sb.append("\n------------------");
		}

		return sb.toString();
	}
	
    /**
     * Removes attribute from the entity, removes any relationship
     * joins containing this attribute.
     * 
     * @see org.objectstyle.cayenne.map.Entity#removeAttribute(String)
     */
    public void removeAttribute(String attrName) {
    	Attribute attr = getAttribute(attrName);
    	if(attr == null) {
    		return;
    	}
    	
    	DataMap map = getDataMap();
    	if(map != null) {
    		DbEntity[] ents = map.getDbEntities();
    		for(int i = 0; i < ents.length; i++) {
    			Iterator it = ents[i].getRelationshipList().iterator();
    			while(it.hasNext()) {
    				DbRelationship rel = (DbRelationship)it.next();
    				Iterator joins = rel.getJoins().iterator();
    				while(joins.hasNext()) {
    					DbAttributePair join = (DbAttributePair)joins.next();
    					if(join.getSource() == attr || join.getTarget() == attr) {
    						joins.remove();
    					}
    				}
    			}    			
    		}
    	}
    	    	
        super.removeAttribute(attrName);
    }
}
