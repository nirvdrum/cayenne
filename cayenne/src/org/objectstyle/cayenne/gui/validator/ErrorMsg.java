package org.objectstyle.cayenne.gui.validator;

import javax.swing.JFrame;
import org.objectstyle.cayenne.gui.event.Mediator;

/** Interface for error messages found by Validator.*/
public interface ErrorMsg
{
	public static final int NO_ERROR = 0;
	public static final int WARNING  = 1;
	public static final int ERROR    = 2;

	/** Get the text of the error message. */
	public String getMessage();
	/** Get the severity of the error message.
	  @see org.objectstyle.cayenne.gui.validator.Validator#WARNING. */
	public int getSeverity();
	/** Fire event to display the screen where error should be corrected. */
	public void displayField(Mediator mediator, JFrame frame);
}