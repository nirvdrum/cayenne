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

package org.objectstyle.cayenne.gui.validator;

import java.util.*;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.map.*;

/** 
 * Used for validating dirty elements in the Mediator.
 * If errors are found, displays them in the dialog window.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class Validator {
	protected Mediator mediator;
	protected Vector errorMessages;
	protected int errorSeverity;

	/** 
	 * Creates validator for specified mediator. 
	 * 
	 * @param mediator The mediator with dirty elements to check.
	 */
	public Validator(Mediator mediator) {
		this.mediator = mediator;
	}

	/** 
	 * Resets internal state. 
	 * Called internally before starting validation.
	 */
	protected void reset() {
		errorMessages = new Vector();
		errorSeverity = ErrorMsg.NO_ERROR;
	}

	/** 
	 * Returns maximum error severity level encountered during 
	 * the last validation run. 
	 */
	public int getErrorSeverity() {
		return errorSeverity;
	}

	/**
	 * Adds new error message to the list of messages. Increases severity
	 * if <code>msg</code> parameter
	 * has a higher severity then the current value. 
	 * Leaves current value unchanged otherwise.
	 */
	public void addErrorMessage(ErrorMsg msg) {
		errorMessages.add(msg);
		if (errorSeverity < msg.getSeverity()) {
			errorSeverity = msg.getSeverity();
		}
	}

	/** Return collection of ErrorMsg objects from last validation. */
	public Vector getErrorMessages() {
		return errorMessages;
	}

	/** 
	 * Validates all project elements.
	 * 
	 * @return ErrorMsg.NO_ERROR if no errors are found, 
	 * another error code if errors are found.
	 */
	public synchronized int validate() {
		reset();

		// Validate domains. 
		// This will recursively run validation on maps, nodes, etc.
		validateDomains(mediator.getDomains());

		return getErrorSeverity();
	}

	/** 
	 * Checks if there are empty or duplicate domain names. 
	 * Also checks data nodes. 
	 */
	protected void validateDomains(DataDomain[] domains) {
		// Used to check for duplicate names
		HashMap name_map = new HashMap();

		for (int i = 0; i < domains.length; i++) {
			String name =
				(domains[i].getName() != null)
					? domains[i].getName().trim()
					: "";

			if (name.length() == 0) {
				addErrorMessage(
					new DomainErrorMsg(
						"Domain has no name",
						ErrorMsg.ERROR,
						domains[i]));
			} else if (name_map.containsKey(name)) {
				addErrorMessage(
					new DomainErrorMsg(
						"Duplicate domain name \"" + name + "\".",
						ErrorMsg.ERROR,
						domains[i]));
			}

			name_map.put(name, domains[i]);

			// Validate data nodes.
			validateDataNodes(domains[i], domains[i].getDataNodes());

			// Validate data maps. 
			List list = domains[i].getMapList();
			validateDataMaps(domains[i], list);
		}
	}

	/** Checks for duplicate data node names and other stuff. */
	protected void validateDataNodes(DataDomain domain, DataNode[] nodes) {
		// Used to check for duplicate names
		HashMap name_map = new HashMap();

		for (int i = 0; i < nodes.length; i++) {
			String name =
				(nodes[i].getName() != null) ? nodes[i].getName().trim() : "";
			if (name.length() == 0) {
				addErrorMessage(
					new DataNodeErrorMsg(
						"Data node has no name",
						ErrorMsg.ERROR,
						domain,
						nodes[i]));
			} else if (name_map.containsKey(name)) {
				addErrorMessage(
					new DataNodeErrorMsg(
						"Duplicate data node name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						nodes[i]));
			}

			validateDataNode(domain, nodes[i]);
			name_map.put(name, nodes[i]);
		}
	}

	protected void validateDataNode(DataDomain domain, DataNode node) {

		String factory = node.getDataSourceFactory();
		if (factory == null)
			factory = "";
		else
			factory = factory.trim();
		// If direct factory, make sure the location is a valid file name.
		if (factory.length() == 0) {
			String temp = "Must select factory to use to get to data source.";
			addErrorMessage(
				new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node));
		} else if (factory.equals(DataSourceFactory.DIRECT_FACTORY)) {
			String location = node.getDataSourceLocation();
			if (location == null)
				location = "";
			else
				location = location.trim();
			if (location.length() == 0) {
				String temp = "Must specify file name for Data Node Location";
				addErrorMessage(
					new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node));
			}
		} else if (factory.equals(DataSourceFactory.JNDI_FACTORY)) {
			String location = node.getDataSourceLocation();
			if (location == null)
				location = "";
			else
				location = location.trim();
			if (location.length() == 0) {
				String temp = "Must specify valid Data Source location";
				addErrorMessage(
					new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node));
			}
		}

		if (node.getAdapter() == null) {
			String temp = "Must specify DB query adapter.";
			addErrorMessage(
				new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node));
		}
	}

	protected void validateDataMaps(DataDomain domain, List maps) {
		// Used to check for duplicate names
		HashMap nameMap = new HashMap();

		Iterator iter = maps.iterator();
		while (iter.hasNext()) {
			DataMap map = (DataMap) iter.next();
			String name = map.getName();
			name = (name == null) ? "" : name.trim();

			if (name.length() == 0) {
				addErrorMessage(
					new DataMapErrorMsg(
						"Data map has no name",
						ErrorMsg.ERROR,
						domain,
						map));
			} else if (nameMap.containsKey(name)) {
				addErrorMessage(
					new DataMapErrorMsg(
						"Duplicate data map name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map));
			}

			validateDataMap(domain, map);
			nameMap.put(name, map);
		}
	}

	protected void validateDataMap(DataDomain domain, DataMap map) {
		// If directory factory, make sure the location is a valid file name.
		String location = map.getLocation();
		location = (location == null) ? "" : location.trim();

		// Must have data map file name
		if (location.length() == 0) {
			addErrorMessage(
				new DataMapErrorMsg(
					"Must specify valid Data Map file name",
					ErrorMsg.ERROR,
					domain,
					map));
		}

		// Validate obj entities
		ObjEntity[] entities = map.getObjEntities();
		validateObjEntities(domain, map, entities);

		// Validate db entities
		DbEntity[] dbEntities = map.getDbEntities();
		validateDbEntities(domain, map, dbEntities);
	}

	protected void validateObjEntities(
		DataDomain domain,
		DataMap map,
		ObjEntity[] entities) {

		if (entities == null) {
			return;
		}

		// Used to check for duplicate names
		HashMap nameMap = new HashMap();
		for (int i = 0; i < entities.length; i++) {

			// validate name
			String name = entities[i].getName();
			name = (name == null) ? "" : name.trim();
			if (name.length() == 0) {
				addErrorMessage(
					new EntityErrorMsg(
						"Entity has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
			} else if (nameMap.containsKey(name)) {
				addErrorMessage(
					new EntityErrorMsg(
						"Duplicate entity name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
			}
			nameMap.put(name, map);

			// validate DbEntity presence
			if (entities[i].getDbEntity() == null) {
				addErrorMessage(
					new EntityErrorMsg(
						"No DbEntity for ObjEntity \"" + name + "\".",
						ErrorMsg.WARNING,
						domain,
						map,
						entities[i]));
			}

			// validate Java Class
			String className = entities[i].getClassName();
			if (className == null || className.trim().length() == 0) {
				addErrorMessage(
					new EntityErrorMsg(
						"No Java class for \"" + name + "\".",
						ErrorMsg.WARNING,
						domain,
						map,
						entities[i]));
			}

			validateObjAttributes(domain, map, entities[i]);
			validateObjRels(domain, map, entities[i]);
		}
	}

	protected void validateObjAttributes(
		DataDomain domain,
		DataMap map,
		ObjEntity entity) {

		List attributes = entity.getAttributeList();
		Iterator iter = attributes.iterator();
		while (iter.hasNext()) {
			ObjAttribute attribute = (ObjAttribute) iter.next();
			// Must have name
			if (attribute.getName() == null
				|| attribute.getName().trim().length() == 0) {
				addErrorMessage(
					new AttributeErrorMsg(
						"Attribute has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						attribute));
			}
			// Should have type (WARNING)
			if (attribute.getType() == null
				|| attribute.getType().trim().length() == 0) {
				addErrorMessage(
					new AttributeErrorMsg(
						"Must specify attribute type",
						ErrorMsg.WARNING,
						domain,
						map,
						attribute));
			}
		}
	}

	protected void validateObjRels(
		DataDomain domain,
		DataMap map,
		ObjEntity entity) {

		List rels = entity.getRelationshipList();
		Iterator iter = rels.iterator();
		while (iter.hasNext()) {
			ObjRelationship rel = (ObjRelationship) iter.next();
			if (rel.getName() == null) {
				addErrorMessage(
					new RelationshipErrorMsg(
						"Relationship has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						rel));
			}

			if (rel.getTargetEntity() == null) {
				addErrorMessage(
					new RelationshipErrorMsg(
						"Must specify target entity",
						ErrorMsg.WARNING,
						domain,
						map,
						rel));
			} else {
				// check for missing DbRelationship mappings
				List dbRels = rel.getDbRelationshipList();
				if (dbRels.size() == 0) {
					addErrorMessage(
						new RelationshipErrorMsg(
							"No DbRelationship mapping",
							ErrorMsg.WARNING,
							domain,
							map,
							rel));
				} else {
					DbEntity expectedSrc =
						((ObjEntity) rel.getSourceEntity()).getDbEntity();
					DbEntity expectedTarget =
						((ObjEntity) rel.getTargetEntity()).getDbEntity();

					if (((DbRelationship) dbRels.get(0)).getSourceEntity()
						!= expectedSrc
						|| ((DbRelationship) dbRels.get(dbRels.size() - 1))
							.getTargetEntity()
							!= expectedTarget) {
						addErrorMessage(
							new RelationshipErrorMsg(
								"Incomplete DbRelationship mapping",
								ErrorMsg.WARNING,
								domain,
								map,
								rel));
					}
				}
			}

		}
	}

	protected void validateDbEntities(
		DataDomain domain,
		DataMap map,
		DbEntity[] entities) {

		if (null == entities) {
			return;
		}

		// Used to check for duplicate names
		HashMap name_map = new HashMap();

		for (int i = 0; i < entities.length; i++) {
			String name = entities[i].getName();
			if (name == null)
				name = "";
			else
				name = name.trim();
			// Must have name
			if (name.length() == 0) {
				addErrorMessage(
					new EntityErrorMsg(
						"Entity has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
				// Cannot have duplicate names within data map
			} else if (name_map.containsKey(name)) {
				addErrorMessage(
					new EntityErrorMsg(
						"Duplicate entity name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
			} else if (
				(entities[i] instanceof DerivedDbEntity)
					&& ((DerivedDbEntity) entities[i]).getParentEntity()
						== null) {
				addErrorMessage(
					new EntityErrorMsg(
						"No parent selected for derived entity \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
			}
			name_map.put(name, map);

			validateDbAttributes(domain, map, entities[i]);
			validateDbRels(domain, map, entities[i]);
		}
	}

	protected void validateDbAttributes(
		DataDomain domain,
		DataMap map,
		DbEntity entity) {

		List attributes = entity.getAttributeList();
		Iterator iter = attributes.iterator();
		while (iter.hasNext()) {
			DbAttribute attribute = (DbAttribute) iter.next();
			// Must have name
			if (attribute.getName() == null
				|| attribute.getName().trim().length() == 0) {
				addErrorMessage(
					new AttributeErrorMsg(
						"Attribute has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						attribute));
			}
			// all attributes must have type
			else if (attribute.getType() == TypesMapping.NOT_DEFINED) {
				addErrorMessage(
					new AttributeErrorMsg(
						"Attribute \""
							+ attribute.getName()
							+ "\" doesn't have a type selected",
						ErrorMsg.WARNING,
						domain,
						map,
						attribute));
			}
			// VARCHAR and CHAR attributes must have max length
			else if (
				attribute.getMaxLength() < 0
					&& (attribute.getType() == java.sql.Types.VARCHAR
						|| attribute.getType() == java.sql.Types.CHAR)) {
				addErrorMessage(
					new AttributeErrorMsg(
						"Attribute \""
							+ attribute.getName()
							+ "\" doesn't have max length",
						ErrorMsg.WARNING,
						domain,
						map,
						attribute));
			}
		}
	}

	protected void validateDbRels(
		DataDomain domain,
		DataMap map,
		DbEntity entity) {

		List rels = entity.getRelationshipList();
		Iterator iter = rels.iterator();
		while (iter.hasNext()) {
			DbRelationship rel = (DbRelationship) iter.next();
			if (rel.getTargetEntity() == null) {
				addErrorMessage(
					new RelationshipErrorMsg(
						"Must specify target entity",
						ErrorMsg.WARNING,
						domain,
						map,
						rel));
			} else if (rel.getJoins().size() == 0) {
				addErrorMessage(
					new RelationshipErrorMsg(
						"Must specify at least one join",
						ErrorMsg.WARNING,
						domain,
						map,
						rel));
			}

			if (rel.getName() == null || rel.getName().trim().length() == 0) {
				addErrorMessage(
					new RelationshipErrorMsg(
						"Relationship has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						rel));
			}
		}
	}
}