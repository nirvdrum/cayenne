package org.objectstyle.cayenne.gui.validator;

import javax.swing.JFrame;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.event.DomainDisplayEvent;
import org.objectstyle.cayenne.access.DataDomain;

public class DomainErrorMsg implements ErrorMsg
{
	private String errMsg;
	private int severity;
	private DataDomain domain;

	
	public DomainErrorMsg(String temp_msg, int temp_severity
						, DataDomain temp_domain)
	{ 
		domain = temp_domain;
		severity = temp_severity;
		errMsg = temp_msg;
	}
	
	public String getMessage() { return errMsg; }
	
	public void displayField(Mediator mediator, JFrame frame){
		DomainDisplayEvent event;
		event = new DomainDisplayEvent(frame, domain);
		mediator.fireDomainDisplayEvent(event);
	}
	
	public int getSeverity(){return severity;}

	public String toString() {return getMessage();}	
}