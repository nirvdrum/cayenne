package org.objectstyle.cayenne.pref;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.pref.auto._DomainPreference;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DomainPreference extends _DomainPreference {

    protected DomainProperties properties;

    protected Properties getProperties() {

        if (properties == null) {
            properties = new DomainProperties();

            String values = getKeyValuePairs();
            if (values != null && values.length() > 0) {
                try {
                    properties.load(new ByteArrayInputStream(values.getBytes()));
                }
                catch (IOException ex) {
                    throw new PreferenceException("Error loading properties.", ex);
                }
            }
        }

        return properties;
    }

    protected void encodeProperties() {
        if (this.properties == null) {
            setKeyValuePairs(null);
        }
        else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try {
                properties.store(out, null);
            }
            catch (IOException ex) {
                throw new PreferenceException("Error storing properties.", ex);
            }

            setKeyValuePairs(out.toString());
        }
    }

    /**
     * Returns a generic preference for associated with a given DomainPreference.
     */
    protected PreferenceDetail getPreference() {
        PreferenceDetail preference = new PreferenceDetail();
        preference.setDomainPreference(this);
        return preference;
    }

    /**
     * Locates and returns a detail preference of a given class.
     */
    protected PreferenceDetail getPreference(Class javaClass, boolean create) {

        // detail object PK must match...

        int pk = DataObjectUtils.intPKForObject(this);
        PreferenceDetail preference = (PreferenceDetail) DataObjectUtils.objectForPK(
                getDataContext(),
                javaClass,
                pk);

        if (preference != null) {
            preference.setDomainPreference(this);
        }

        if (preference != null || !create) {
            return preference;
        }

        preference = (PreferenceDetail) getDataContext().createAndRegisterNewObject(
                javaClass);

        preference.setDomainPreference(this);
        getDataContext().commitChanges();
        return preference;
    }

    class DomainProperties extends Properties {

        public Object setProperty(String key, String value) {
            Object old = super.setProperty(key, value);
            if (!Util.nullSafeEquals(old, value)) {
                modified();
            }

            return old;
        }

        void modified() {
            // more efficient implementation should only call encode on commit using
            // DataContext events... still there is a bug that prevents DataObject from
            // changing its state during "willCommit", so for now do this...
            encodeProperties();
        }
    }

}

