package org.objectstyle.cayenne.pref;

/**
 * An exception describing a problem with preferences operations.
 * 
 * @author Andrei Adamchik
 */
public class PreferenceException extends RuntimeException {

    public PreferenceException() {
        super();
    }

    public PreferenceException(String message) {
        super(message);
    }

    public PreferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreferenceException(Throwable cause) {
        super(cause);
    }

}