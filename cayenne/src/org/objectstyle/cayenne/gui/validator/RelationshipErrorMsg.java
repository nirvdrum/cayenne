package org.objectstyle.cayenne.gui.validator;

import javax.swing.JFrame;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Relationship;

public class RelationshipErrorMsg implements ErrorMsg
{
	private String errMsg;
	private int severity = ErrorMsg.ERROR;
	private DataDomain domain;
	private DataMap map;
	private Entity entity;
	private Relationship rel;
	
	public RelationshipErrorMsg(String temp_msg, int temp_severity
						  , DataDomain temp_domain, DataMap temp_map
						  , Relationship temp_rel)
	{ 
		domain = temp_domain;
		map = temp_map;
		rel = temp_rel;
		entity = rel.getSourceEntity();
		errMsg = temp_msg;
	}
	
	public String getMessage() { return errMsg; }
	
	public void displayField(Mediator mediator, JFrame frame){
		RelationshipDisplayEvent event;
		event = new RelationshipDisplayEvent(frame, rel, entity, map, domain);
		if (entity instanceof org.objectstyle.cayenne.map.ObjEntity)
			mediator.fireObjRelationshipDisplayEvent(event);
	}
	
	public int getSeverity() {return severity;}
	
	public String toString() {return getMessage();}
}