package org.objectstyle.cayenne.dataview;

import java.util.*;
import org.objectstyle.cayenne.*;

public class LookupCache {
  private Map fieldCache = new HashMap();
  private static final Object[] EMPTY_ARRAY = new Object[] {};

  public LookupCache() {
  }

  public void cache(ObjEntityViewField field, List dataObjects) {
    Lookup lookup = getLookup(field);
    if (lookup == null) {
      lookup = new Lookup();
      fieldCache.put(field, lookup);
    }
    lookup.cache(field, dataObjects);
  }

  public void clear() {
    fieldCache.clear();
  }

  public boolean removeFromCache(ObjEntityViewField field) {
    return fieldCache.remove(field) != null;
  }

  public Object[] getCachedValues(ObjEntityViewField field) {
    Lookup lookup = getLookup(field);
    Object[] values = (lookup != null ? lookup.values : EMPTY_ARRAY);
    if (values.length == 0)
      return values;
    else {
      Object[] valuesCopy = new Object[values.length];
      System.arraycopy(values, 0, valuesCopy, 0, values.length);
      return valuesCopy;
    }
  }

  public DataObject getDataObject(ObjEntityViewField field, Object value) {
    Lookup lookup = getLookup(field);
    if (lookup == null)
      return null;
    return lookup.getDataObject(value);
  }

  private Lookup getLookup(ObjEntityViewField field) {
    if (field == null) return null;
    return (Lookup)fieldCache.get(field);
  }

  private class Lookup {
    ObjEntityViewField field;
    Object[] values = EMPTY_ARRAY;
    Map valueDataObjectMap;

    void cache(ObjEntityViewField field, List dataObjects) {
      this.field = field;
      if (values.length != dataObjects.size())
        values = new Object[dataObjects.size()];
      valueDataObjectMap = new HashMap(values.length + 1);
      int index = 0;
      for (Iterator i = dataObjects.iterator(); i.hasNext(); ) {
        DataObject item = (DataObject)i.next();
        values[index] = field.getValue(item);
        valueDataObjectMap.put(values[index], item);
        index++;
      }
    }

    DataObject getDataObject(Object value) {
      return (DataObject)valueDataObjectMap.get(value);
    }
  }
}