/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * @author cmiskell
 */
public class FlattenedRelationshipInsertQuery extends AbstractQuery {
	private DataObject source;
	private DataObject destination;
	private String relationshipName;

	/**
	 * Constructor for  FlattenedRelationshipInsertQuery.  Creates a query which will insert 
	 * the required link records for the relationship called <code>relName</code> 
	 * between <code>source</code>  and <code>destination</code> 
	 * @param source the source Object of the relationship
	 * @param destination the destination of the relationship
	 * @param relName the name of the relationship
	 */
	public FlattenedRelationshipInsertQuery(DataObject source, DataObject destination, String relName) {
		super();
		this.source=source;
		this.destination=destination;
		this.relationshipName=relName;
		ObjRelationship relationship = this.getRelationship();
		DbRelationship firstRel = (DbRelationship) relationship.getDbRelationships().get(0);
		this.setRoot(firstRel.getTargetEntity());

	}
	
	private ObjEntity getSourceObjEntity() {
		DataObject sourceObject = this.getSource();
		return sourceObject.getDataContext().getEntityResolver().lookupObjEntity(sourceObject);
	}
	
	private ObjRelationship getRelationship() {
		return (ObjRelationship) this.getSourceObjEntity().getRelationship(this.getRelationshipName());
	}
	
    public int getQueryType() {
        return INSERT_QUERY;
    }
    
    public DataObject getDestination() {
		return destination;
	}

	public void setDestination(DataObject destination) {
		this.destination = destination;
	}

	public String getRelationshipName() {
		return relationshipName;
	}

	public void setRelationshipName(String relName) {
		this.relationshipName = relName;
	}

	public DataObject getSource() {
		return source;
	}
	
	public void setSource(DataObject source) {
		this.source = source;
	}
}
