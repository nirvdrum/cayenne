package org.objectstyle.cayenne.pref;

/**
 * @author Andrei Adamchik
 */
public interface PreferenceService {

    /**
     * Returns a preferences domain for an application.
     */
    public Domain getDomain(String name, boolean create);

    /**
     * Starts PreferenceService.
     */
    public void startService();

    /**
     * Stops PreferenceService.
     */
    public void stopService();

    /**
     * A method for explicitly committing the preferences to the external store. Generally
     * a PreferenceService implementation will also do periodic commits internally
     * without calling this method.
     */
    public void savePreferences();
}