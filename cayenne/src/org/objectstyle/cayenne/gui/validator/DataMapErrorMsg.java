package org.objectstyle.cayenne.gui.validator;

import javax.swing.JFrame;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;

public class DataMapErrorMsg implements ErrorMsg
{
	private String errMsg;
	private int severity = ErrorMsg.ERROR;
	private DataDomain domain;
	private DataMap map;
	
	public DataMapErrorMsg(String temp_msg, int temp_severity
						  , DataDomain temp_domain, DataMap temp_map)
	{ 
		domain = temp_domain;
		map = temp_map;
		errMsg = temp_msg;
		severity = temp_severity;
	}
	
	public String getMessage() { return errMsg; }
	
	public void displayField(Mediator mediator, JFrame frame){
		DataMapDisplayEvent event;
		event = new DataMapDisplayEvent(frame, map, domain);
		mediator.fireDataMapDisplayEvent(event);
	}
	
	public int getSeverity() {return severity;}
	
	public String toString() {return getMessage();}
}