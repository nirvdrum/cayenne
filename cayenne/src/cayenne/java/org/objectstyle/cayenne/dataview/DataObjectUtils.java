package org.objectstyle.cayenne.dataview;

import org.objectstyle.cayenne.*;

public class DataObjectUtils {
  public static Object readProperty(DataObject obj, String propertyName) {
    if (obj.getPersistenceState() == PersistenceState.HOLLOW) {
      try {
        obj.getDataContext().refetchObject(obj.getObjectId());
      } catch (Exception ex) {
        obj.setPersistenceState(PersistenceState.TRANSIENT);
      }
    }
    return obj.readPropertyDirectly(propertyName);
  }

  public static void writeProperty(
      DataObject obj, String propertyName, Object value) {
    if (obj.getPersistenceState() == PersistenceState.HOLLOW) {
      try {
        obj.getDataContext().refetchObject(obj.getObjectId());
        obj.setPersistenceState(PersistenceState.MODIFIED);
      } catch (Exception ex) {
        obj.setPersistenceState(PersistenceState.TRANSIENT);
      }
    } else if (obj.getPersistenceState() == PersistenceState.COMMITTED) {
      obj.setPersistenceState(PersistenceState.MODIFIED);
    }
    obj.writePropertyDirectly(propertyName, value);
  }
}