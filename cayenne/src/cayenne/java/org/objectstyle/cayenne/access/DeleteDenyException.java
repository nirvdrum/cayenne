package org.objectstyle.cayenne.access;

/**
 * 
 * @author Craig Miskell
 */
public class DeleteDenyException extends RuntimeException {

	/**
	 * Constructor for DeleteDenyException.
	 */
	public DeleteDenyException() {
		super();
	}

	/**
	 * Constructor for DeleteDenyException.
	 * @param message
	 */
	public DeleteDenyException(String message) {
		super(message);
	}

}
