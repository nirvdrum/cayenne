package org.objectstyle.cayenne.pref;

/**
 * @author Andrei Adamchik
 */
public interface PreferenceEditor {

    public PreferenceService getService();

    /**
     * Creates a generic PreferenceDetail.
     */
    public PreferenceDetail createDetail(Domain domain, String key);
    
    /**
     * Creates PreferenceDetail of specified class.
     */
    public PreferenceDetail createDetail(Domain domain, String key, Class javaClass);

    public PreferenceDetail deleteDetail(Domain domain, String key);
    
    public Domain editableInstance(Domain domain);

    public void save();

    public void revert();
}