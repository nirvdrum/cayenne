package org.objectstyle.cayenne.gui.validator;

import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;

import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.gui.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/** Used for validating dirty elements in the Mediator.
  * If errors are found, displays them in the dialog window.*/
public class Validator
{
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
	
	/** Validates dirty elements in mediator.
	  * Displays non-modal dialog window if errors are found.
	  * @return true if no errors are found, false if errors are found.*/
	public int validate() {
		int status = ErrorMsg.NO_ERROR;
		int temp_err_level;
		errMsg = new Vector();
		
		// Validate domains. If error level worse than current status,
		// set status to new error level
		temp_err_level = validateDomains(mediator.getDomains());
		if (temp_err_level > status)
			status = temp_err_level;
		return status;
	}
	
	/** Return collection of ErrorMsg objects from last validation. */
	public Vector getErrorMessages() {
		return errMsg;
	}
	
	/** Checks if there are empty or duplicate domain names. 
	  * Also checks data nodes. */
	private int validateDomains(DataDomain[] domains) {
		int status = ErrorMsg.NO_ERROR;
		DomainErrorMsg msg;
		// Used to check for duplicate names
		HashMap name_map = new HashMap();
		
		for (int i = 0; i < domains.length; i++) {
			String name = domains[i].getName().trim();
			if (name == null) 
				name = "";
			else name = name.trim();
			if (name.length() == 0) {
				msg = new DomainErrorMsg("Domain has no name"
										, ErrorMsg.ERROR, domains[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			} else if (name_map.containsKey(name)) {
				msg = new DomainErrorMsg("Duplicate domain name \""+name+"\"."
										, ErrorMsg.ERROR, domains[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
			name_map.put(name, domains[i]);
			// Validate data nodes. If error level worse than current status,
			// set status to new error level
			int temp_err_level = validateDataNodes(domains[i], domains[i].getDataNodes());
			if (temp_err_level > status) 
				status = temp_err_level;
			// Validate data maps. If error level worse than current status,
			// set status to new error level
			List list = domains[i].getMapList();
			temp_err_level = validateDataMaps(domains[i], list);
			if (temp_err_level > status) 
				status = temp_err_level;
		}// End for()
		
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
			else name = name.trim();
			if (name.length() == 0) {
				msg = new DataNodeErrorMsg("Data node has no name", ErrorMsg.ERROR
											, domain, nodes[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			} else if (name_map.containsKey(name)) {
				msg = new DataNodeErrorMsg("Duplicate data node name \""
										+name+"\".", ErrorMsg.ERROR
										, domain, nodes[i]);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
			
			int temp_err_level = validateDataNode(domain, nodes[i]);
			if (temp_err_level > status) 
				status = temp_err_level;
			name_map.put(name, nodes[i]);
		}// End for()
		return status;
	}

	private int validateDataNode(DataDomain domain, DataNode node)
	{
		int status = ErrorMsg.NO_ERROR;
		DataNodeErrorMsg msg;
		
		String factory = node.getDataSourceFactory();
		if (factory == null)
			factory = "";
		else factory = factory.trim();
		// If directo factory, make sure the location is a valid file name.
		if (factory.equals(DataSourceFactory.DIRECT_FACTORY)) {
			String location = node.getDataSourceLocation();
			if (location == null)
				location = "";
			else location = location.trim();
			if (location.length() == 0) {
				String temp;
				temp = "Must specify valid Data Node file name when using data"
					 	+" source factory " + DataSourceFactory.DIRECT_FACTORY;
				msg = new DataNodeErrorMsg(temp, ErrorMsg.ERROR, domain, node);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			} else {
				
			}
		}
		return status;
	}
	
	private int validateDataMaps(DataDomain domain, List maps)
	{
		int status = ErrorMsg.NO_ERROR;
		DataMapErrorMsg msg;
		// Used to check for duplicate names
		HashMap name_map = new HashMap();
		
		Iterator iter = maps.iterator();
		while (iter.hasNext()) {
			DataMap map = (DataMap)iter.next();
			String name = map.getName();
			if (name == null)
				name = "";
			else name = name.trim();
			if (name.length() == 0) {
				msg = new DataMapErrorMsg("Data map has no name", ErrorMsg.ERROR
											, domain, map);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			} else if (name_map.containsKey(name)) {
				msg = new DataMapErrorMsg("Duplicate data map name \""
										+name+"\".", ErrorMsg.ERROR
										, domain, map);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
			
			int temp_err_level = validateDataMap(domain, map);
			if (temp_err_level > status) 
				status = temp_err_level;
			name_map.put(name, map);
		}// End for()
		return status;
	}

	private int validateDataMap(DataDomain domain, DataMap map) {
		int status = ErrorMsg.NO_ERROR;
		DataMapErrorMsg msg;
		
		// If directo factory, make sure the location is a valid file name.
		String location = map.getLocation();
		if (location == null)
			location = "";
		else location = location.trim();
		if (location.length() == 0) {
			msg = new DataMapErrorMsg("Must specify valid Data Map file name"
									, ErrorMsg.ERROR, domain, map);
			errMsg.add(msg);
			status = ErrorMsg.ERROR;
		}
		
		ObjEntity[] entities = map.getObjEntities();
		int temp_err_level = validateObjEntities(domain, map, entities);
		if (temp_err_level > status) 
			status = temp_err_level;
		return status;
	}

	private int validateObjEntities(DataDomain domain, DataMap map
									, ObjEntity[] entities)
	{
		int status = ErrorMsg.NO_ERROR;

		for (int i = 0; i < entities.length; i++) {
			List rel = entities[i].getRelationshipList();
			int temp_err_level = validateObjRels(domain, map, entities[i]);
			if (temp_err_level > status) 
				status = temp_err_level;
		}
		return status;
	}
	
	private int validateObjRels(DataDomain domain, DataMap map
									, ObjEntity entity)
	{
		int status = ErrorMsg.NO_ERROR;
		RelationshipErrorMsg msg;
		
		List rels = entity.getRelationshipList();
		Iterator iter = rels.iterator();
		while (iter.hasNext()) {
			ObjRelationship rel = (ObjRelationship)iter.next();
			if (rel.getTargetEntity() == null) {
				msg = new RelationshipErrorMsg("Must specify target entity"
									, ErrorMsg.ERROR, domain, map, rel);
				errMsg.add(msg);
				status = ErrorMsg.ERROR;
			}
		}// End while()
		
		return status;
	}

}


