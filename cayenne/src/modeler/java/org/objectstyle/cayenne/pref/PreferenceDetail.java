package org.objectstyle.cayenne.pref;

import java.util.Properties;

import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.util.Util;

/**
 * A superclass of concrete preference classes.
 * <p>
 * Complete preference descriptor is composed out of two classes - DomainPreference that
 * defines how the preference is located with in domain, and a GenericPreference.
 * GenericPreference API is designed for the application use, while internal
 * DomainPreference is managed behidn the scenes. Note that there is no real Cayenne
 * relationship from concrete preference entity to the preference framework entities, so
 * this class handles all needed wiring...
 * 
 * @author Andrei Adamchik
 */
public class PreferenceDetail extends CayenneDataObject {

    protected DomainPreference domainPreference;

    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex) {
            throw new PreferenceException("Error converting to int: " + value);
        }
    }

    public String getKey() {
        if (getDomainPreference() == null) {
            throw new PreferenceException(
                    "Preference not initialized, can't work with properties.");
        }

        return domainPreference.getKey();
    }

    public void setIntProperty(String key, int value) {
        setProperty(key, String.valueOf(value));
    }

    /**
     * Returns a named property for a given key.
     */
    public String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    public void setProperty(String key, String value) {
        getProperties().setProperty(key, value);
    }

    public DomainPreference getDomainPreference() {
        if (domainPreference == null) {
            // try to fetch..

            DataContext context = getDataContext();

            if (context != null) {
                ObjectId oid = getObjectId();

                if (oid.isTemporary()) {
                    oid = oid.getReplacementId();
                }

                if (oid != null) {
                    domainPreference = (DomainPreference) DataObjectUtils.objectForPK(
                            context,
                            DomainPreference.class,
                            oid.getIdSnapshot());
                }
            }
        }

        return domainPreference;
    }

    /**
     * Initializes internal DomainPreference object.
     */
    public void setDomainPreference(DomainPreference domainPreference) {
        if (this.domainPreference != domainPreference) {
            this.domainPreference = domainPreference;

            ObjectId oid = getObjectId();
            if (oid != null && oid.isTemporary()) {
                oid.setReplacementId(buildPermanentId());
            }
        }
    }

    /**
     * Returns initialized non-null properties map.
     */
    protected Properties getProperties() {
        if (getDomainPreference() == null) {
            throw new PreferenceException(
                    "Preference not initialized, can't work with properties.");
        }

        return domainPreference.getProperties();
    }

    /**
     * Creates permanent ID based on DomainPreference id.
     */
    protected ObjectId buildPermanentId() {
        ObjectId otherId = getDomainPreference().getObjectId();
        if (otherId == null) {
            throw new PreferenceException(
                    "Can't persist preference. DomainPreference has no ObjectId");
        }

        // force creation of otherId
        if (otherId.isTemporary() && otherId.getReplacementId() == null) {
            DbEntity entity = getDataContext().getEntityResolver().lookupDbEntity(
                    domainPreference);

            DataNode node = getDataContext().lookupDataNode(entity.getDataMap());

            try {
                Object pk = node.getAdapter().getPkGenerator().generatePkForDbEntity(
                        node,
                        entity);

                ObjectId permanentOtherId = new ObjectId(
                        DomainPreference.class,
                        DomainPreference.ID_PK_COLUMN,
                        pk);
                otherId.setReplacementId(permanentOtherId);
            }
            catch (Throwable th) {
                throw new PreferenceException("Error creating primary key", Util
                        .unwindException(th));
            }
        }

        int id = DataObjectUtils.intPKForObject(domainPreference);
        return new ObjectId(getClass(), "id", id);
    }
}