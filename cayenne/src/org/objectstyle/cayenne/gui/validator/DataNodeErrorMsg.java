package org.objectstyle.cayenne.gui.validator;

import javax.swing.JFrame;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;

public class DataNodeErrorMsg implements ErrorMsg
{
	private String errMsg;
	private int severity = ErrorMsg.ERROR;
	private DataDomain domain;
	private DataNode node;
	
	public DataNodeErrorMsg(String temp_msg, int temp_severity
						  , DataDomain temp_domain, DataNode temp_node)
	{ 
		domain = temp_domain;
		node = temp_node;
		errMsg = temp_msg;
	}
	
	public String getMessage() { return errMsg; }
	
	public void displayField(Mediator mediator, JFrame frame){
		DataNodeDisplayEvent event;
		event = new DataNodeDisplayEvent(frame, domain, node);
		mediator.fireDataNodeDisplayEvent(event);
	}
	
	public int getSeverity() {return severity;}

	public String toString() {return getMessage();}	
}