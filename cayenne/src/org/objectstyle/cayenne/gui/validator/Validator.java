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
	private Mediator mediator;
	private Vector errMsg = new Vector();

	/** Create validator for specified mediator. 
	  * @param temp_mediator The mediator with dirty elements to check.
	  * @param temp_frame Frame to use for displaying dialog window. 
	  * 	   If null, default frame is used.
	  */
	public Validator(Mediator temp_mediator) {
		mediator = temp_mediator;
	}

	/** 
	 * Validates dirty elements in mediator.
	 * Displays non-modal dialog window if errors are found.
	 * 
	 * @return ErrorMsg.NO_ERROR if no errors are found, 
	 * another error code if errors are found.
	 */
	public int validate() {
		int status = ErrorMsg.NO_ERROR;
		int temp_err_level;
		errMsg = new Vector();

		// Validate domains. If error level worse than current status,
		// set status to new error level
		temp_err_level = validateDomains(mediator.getDomains());
		if (temp_err_level > status) {
			status = temp_err_level;
		}
		return status;
	}

	/** Return collection of ErrorMsg objects from last validation. */
	public Vector getErrorMessages() {
		return errMsg;
	}

	/** 
	 * Checks if there are empty or duplicate domain names. 
	 * Also checks data nodes. 
	 */
	private int validateDomains(DataDomain[] domains) {
		int status = ErrorMsg.NO_ERROR;
		DomainErrorMsg msg;
		// Used to check for duplicate names
		HashMap name_map = new HashMap();

		for (int i = 0; i < domains.length; i++) {
			String name = (domains[i].getName() != null) ? domains[i].getName().trim() : "";

			if (name.length() == 0) {
				msg =
					new DomainErrorMsg(
						"Domain has no name",
						ErrorMsg.ERROR,
						domains[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			} else if (name_map.containsKey(name)) {
				msg =
					new DomainErrorMsg(
						"Duplicate domain name \"" + name + "\".",
						ErrorMsg.ERROR,
						domains[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}

			name_map.put(name, domains[i]);
			// Validate data nodes. If error level worse than current status,
			// set status to new error level
			int errLevel =
				validateDataNodes(domains[i], domains[i].getDataNodes());
			if (errLevel > status) {
				status = errLevel;
			}

			// Validate data maps. If error level worse than current status,
			// set status to new error level
			List list = domains[i].getMapList();
			errLevel = validateDataMaps(domains[i], list);
			if (errLevel > status) {
				status = errLevel;
			}
		}

		return status;
	}

	/** Checks for duplicate data node names and other stuff. */
	private int validateDataNodes(DataDomain domain, DataNode[] nodes) {
		int status = ErrorMsg.NO_ERROR;
		DataNodeErrorMsg msg;
		// Used to check for duplicate names
		HashMap name_map = new HashMap();

		for (int i = 0; i < nodes.length; i++) {
			String name = nodes[i].getName();
			if (name == null)
				name = "";
			else
				name = name.trim();
			if (name.length() == 0) {
				msg =
					new DataNodeErrorMsg(
						"Data node has no name",
						ErrorMsg.ERROR,
						domain,
						nodes[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			} else if (name_map.containsKey(name)) {
				msg =
					new DataNodeErrorMsg(
						"Duplicate data node name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						nodes[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}

			int temp_err_level = validateDataNode(domain, nodes[i]);
			if (temp_err_level > status) {
				status = temp_err_level;
			}
			name_map.put(name, nodes[i]);
		} // End for()
		return status;
	}

	private int validateDataNode(DataDomain domain, DataNode node) {
		int status = ErrorMsg.NO_ERROR;
		DataNodeErrorMsg msg;

		String factory = node.getDataSourceFactory();
		if (factory == null)
			factory = "";
		else
			factory = factory.trim();
		// If direct factory, make sure the location is a valid file name.
		if (factory.length() == 0) {
			String temp;
			temp = "Must select factory to use to get to data source.";
			msg = new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node);
			errMsg.add(msg);
			status = ErrorMsg.ERROR;
		} else if (factory.equals(DataSourceFactory.DIRECT_FACTORY)) {
			String location = node.getDataSourceLocation();
			if (location == null)
				location = "";
			else
				location = location.trim();
			if (location.length() == 0) {
				String temp;
				temp = "Must specify file name for Data Node Location";
				msg = new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
		} else if (factory.equals(DataSourceFactory.JNDI_FACTORY)) {
			String location = node.getDataSourceLocation();
			if (location == null)
				location = "";
			else
				location = location.trim();
			if (location.length() == 0) {
				String temp;
				temp = "Must specify valid Data Source location";
				msg = new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
		}

		if (node.getAdapter() == null) {
			String temp;
			temp = "Must specify DB query adapter.";
			msg = new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node);
			errMsg.add(msg);
			status = ErrorMsg.ERROR;
		}
		return status;
	}

	private int validateDataMaps(DataDomain domain, List maps) {
		int status = ErrorMsg.NO_ERROR;

		// Used to check for duplicate names
		HashMap nameMap = new HashMap();

		Iterator iter = maps.iterator();
		while (iter.hasNext()) {
			DataMap map = (DataMap) iter.next();
			String name = map.getName();
			name = (name == null) ? "" : name.trim();

			if (name.length() == 0) {
				errMsg.add(
					new DataMapErrorMsg(
						"Data map has no name",
						ErrorMsg.ERROR,
						domain,
						map));
				status = ErrorMsg.ERROR;
			} else if (nameMap.containsKey(name)) {
				errMsg.add(
					new DataMapErrorMsg(
						"Duplicate data map name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map));
				status = ErrorMsg.ERROR;
			}

			int errLevel = validateDataMap(domain, map);
			if (errLevel > status) {
				status = errLevel;
			}

			nameMap.put(name, map);
		}
		return status;
	}

	private int validateDataMap(DataDomain domain, DataMap map) {
		int status = ErrorMsg.NO_ERROR;

		// If directo factory, make sure the location is a valid file name.
		String location = map.getLocation();
		location = (location == null) ? "" : location.trim();

		// Must have data map file name
		if (location.length() == 0) {
			errMsg.add(
				new DataMapErrorMsg(
					"Must specify valid Data Map file name",
					ErrorMsg.ERROR,
					domain,
					map));
			status = ErrorMsg.ERROR;
		}

		// Validate obj entities
		ObjEntity[] entities = map.getObjEntities();
		int errLevel = validateObjEntities(domain, map, entities);
		if (errLevel > status) {
			status = errLevel;
		}

		// Validate db entities
		DbEntity[] dbEntities = map.getDbEntities();
		errLevel = validateDbEntities(domain, map, dbEntities);
		if (errLevel > status) {
			status = errLevel;
		}

		return status;
	}

	private int validateObjEntities(
		DataDomain domain,
		DataMap map,
		ObjEntity[] entities) {

		int status = ErrorMsg.NO_ERROR;
		if (entities == null) {
			return status;
		}

		// Used to check for duplicate names
		HashMap nameMap = new HashMap();

		for (int i = 0; i < entities.length; i++) {
			
			// validate name
			String name = entities[i].getName();
			name = (name == null) ? "" : name.trim();
			if (name.length() == 0) {
				errMsg.add(
					new EntityErrorMsg(
						"Entity has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
				status = ErrorMsg.ERROR;
			} else if (nameMap.containsKey(name)) {
				errMsg.add(
					new EntityErrorMsg(
						"Duplicate entity name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]));
				status = ErrorMsg.ERROR;
			}
			nameMap.put(name, map);

            // validate DbEntity presence
            if(entities[i].getDbEntity() == null) {
				errMsg.add(
					new EntityErrorMsg(
						"No DbEntity for ObjEntity \"" + name + "\".",
						ErrorMsg.WARNING,
						domain,
						map,
						entities[i]));
						
				if(status < ErrorMsg.WARNING) {
					status = ErrorMsg.WARNING;
				}
            }
            
            // validate Java Class
            String className = entities[i].getClassName();
            if(className == null || className.trim().length() == 0) {
				errMsg.add(
					new EntityErrorMsg(
						"No Java class for \"" + name + "\".",
						ErrorMsg.WARNING,
						domain,
						map,
						entities[i]));
						
				if(status < ErrorMsg.WARNING) {
					status = ErrorMsg.WARNING;
				}
            }
            
			int errLevel =
				validateObjAttributes(domain, map, entities[i]);
			if (errLevel > status) {
				status = errLevel;
			}
			
			errLevel = validateObjRels(domain, map, entities[i]);
			if (errLevel > status) {
				status = errLevel;
			}	
		}
		
		return status;
	}

	private int validateObjAttributes(
		DataDomain domain,
		DataMap map,
		ObjEntity entity) {
		int status = ErrorMsg.NO_ERROR;
		AttributeErrorMsg msg;

		List attributes = entity.getAttributeList();
		Iterator iter = attributes.iterator();
		while (iter.hasNext()) {
			ObjAttribute attribute = (ObjAttribute) iter.next();
			// Must have name
			if (attribute.getName() == null
				|| attribute.getName().trim().length() == 0) {
				msg =
					new AttributeErrorMsg(
						"Attribute has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						attribute);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
			// Should have type (WARNING)
			if (attribute.getType() == null
				|| attribute.getType().trim().length() == 0) {
				msg =
					new AttributeErrorMsg(
						"Must specify attribute type",
						ErrorMsg.WARNING,
						domain,
						map,
						attribute);
				errMsg.add(msg);
				if (status == ErrorMsg.NO_ERROR)
					status = ErrorMsg.WARNING;
			}
		} // End while()

		return status;
	}

	private int validateObjRels(
		DataDomain domain,
		DataMap map,
		ObjEntity entity) {
		int status = ErrorMsg.NO_ERROR;
		RelationshipErrorMsg msg;

		List rels = entity.getRelationshipList();
		Iterator iter = rels.iterator();
		while (iter.hasNext()) {
			ObjRelationship rel = (ObjRelationship) iter.next();
			if (rel.getTargetEntity() == null) {
				msg =
					new RelationshipErrorMsg(
						"Must specify target entity",
						ErrorMsg.ERROR,
						domain,
						map,
						rel);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
		} // End while()

		return status;
	}

	private int validateDbEntities(
		DataDomain domain,
		DataMap map,
		DbEntity[] entities) {
		int status = ErrorMsg.NO_ERROR;
		if (null == entities)
			return status;
		EntityErrorMsg msg;
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
				msg =
					new EntityErrorMsg(
						"Entity has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
				// Cannot have duplicate names within data map
			} else if (name_map.containsKey(name)) {
				msg =
					new EntityErrorMsg(
						"Duplicate entity name \"" + name + "\".",
						ErrorMsg.ERROR,
						domain,
						map,
						entities[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
			name_map.put(name, map);

			int temp_err_level = validateDbAttributes(domain, map, entities[i]);
			if (temp_err_level > status)
				status = temp_err_level;
			temp_err_level = validateDbRels(domain, map, entities[i]);
			if (temp_err_level > status)
				status = temp_err_level;
		} // End for()
		return status;
	}

	private int validateDbAttributes(
		DataDomain domain,
		DataMap map,
		DbEntity entity) {
		int status = ErrorMsg.NO_ERROR;
		AttributeErrorMsg msg;

		List attributes = entity.getAttributeList();
		Iterator iter = attributes.iterator();
		while (iter.hasNext()) {
			DbAttribute attribute = (DbAttribute) iter.next();
			// Must have name
			if (attribute.getName() == null
				|| attribute.getName().trim().length() == 0) {
				msg =
					new AttributeErrorMsg(
						"Attribute has no name",
						ErrorMsg.ERROR,
						domain,
						map,
						attribute);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
			// VARCHAR and CHAR attributes must have max length
			else if (
				attribute.getMaxLength() == -1
					&& (attribute.getType() == java.sql.Types.VARCHAR
						|| attribute.getType() == java.sql.Types.CHAR)) {
				msg =
					new AttributeErrorMsg(
						"Attribute \""
							+ attribute.getName()
							+ "\" doesn't have max length",
						ErrorMsg.ERROR,
						domain,
						map,
						attribute);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
		} // End while()

		return status;
	}

	private int validateDbRels(
		DataDomain domain,
		DataMap map,
		DbEntity entity) {
		int status = ErrorMsg.NO_ERROR;
		RelationshipErrorMsg msg;

		List rels = entity.getRelationshipList();
		Iterator iter = rels.iterator();
		while (iter.hasNext()) {
			DbRelationship rel = (DbRelationship) iter.next();
			if (rel.getTargetEntity() == null) {
				msg =
					new RelationshipErrorMsg(
						"Must specify target entity",
						ErrorMsg.ERROR,
						domain,
						map,
						rel);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
		} // End while()
		return status;
	}
}