package org.objectstyle.cayenne.query;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * @author cmiskell
 */
public class FlattenedRelationshipDeleteQuery extends QualifiedQuery {
	private DataObject source;
	private DataObject destination;
	private String relationshipName;

	/**
	 * Constructor FlattenedRelationshipDeleteQuery.  Creates a query which will delete 
	 * the required link record for the relationship called <code>relName</code> 
	 * between <code>source</code>  and <code>destination</code> 
	 * @param cayenneDataObject
	 * @param dataObject
	 * @param relName
	 */
	public FlattenedRelationshipDeleteQuery(
		DataObject sourceObject,
		DataObject destinationObject,
		String relName) {
		this.source = sourceObject;
		this.destination = destinationObject;
		this.relationshipName = relName;
		
		ObjRelationship relationship = this.getRelationship();
		DbRelationship firstRel = (DbRelationship) relationship.getDbRelationshipList().get(0);
		this.setRoot((DbEntity)firstRel.getTargetEntity());
		
		this.createQualifier();
	}

	private ObjEntity getSourceObjEntity() {
		DataObject sourceObject = this.getSource();
		return sourceObject.getDataContext().getEntityResolver().lookupObjEntity(sourceObject);
	}
	
	private ObjRelationship getRelationship() {
		return (ObjRelationship) this.getSourceObjEntity().getRelationship(this.getRelationshipName());
	}

	//Chunk of functionality that's really too larget to be in the constructor
	private void createQualifier() {
		List pkExpressions = new ArrayList();

		ObjRelationship relationship = this.getRelationship();
		DbRelationship dbRel = (DbRelationship) relationship.getDbRelationshipList().get(0);

		//First relationship - use source of joins to get a value, target of joins to get attribute name
		ObjectId oid = this.getSource().getObjectId();
		Map id = (oid != null) ? oid.getIdSnapshot() : null;
		List joins = dbRel.getJoins();
		int i;
		for (i = 0; i < joins.size(); i++) {
			DbAttributePair thisJoin = (DbAttributePair) joins.get(i);
			DbAttribute sourceAttribute = thisJoin.getSource();
			DbAttribute targetAttribute = thisJoin.getTarget();
			Expression thisExpression =
				ExpressionFactory.binaryDbPathExp(
					Expression.EQUAL_TO,
					targetAttribute.getName(),
					id.get(sourceAttribute.getName()));
			pkExpressions.add(thisExpression);
		}

		//Second relationship- use target of joins to get a value, source of joins to get attribute name
		oid = this.getDestination().getObjectId();
		id = (oid != null) ? oid.getIdSnapshot() : null;
		dbRel = (DbRelationship) relationship.getDbRelationshipList().get(1);
		joins = dbRel.getJoins();
		for (i = 0; i < joins.size(); i++) {
			DbAttributePair thisJoin = (DbAttributePair) joins.get(i);
			DbAttribute sourceAttribute = thisJoin.getSource();
			DbAttribute targetAttribute = thisJoin.getTarget();
			Expression thisExpression =
				ExpressionFactory.binaryDbPathExp(
					Expression.EQUAL_TO,
					sourceAttribute.getName(),
					id.get(targetAttribute.getName()));
			pkExpressions.add(thisExpression);
		}

		//Plug them all togehter... voila
		this.setQualifier(ExpressionFactory.joinExp(Expression.AND, pkExpressions));
	}
	/**
	 * @see org.objectstyle.cayenne.query.Query#getQueryType()
	 */
	public int getQueryType() {
		return DELETE_QUERY;
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
