package org.objectstyle.cayenne.dataview;

import java.util.*;

public class DataObjectChangeEvent extends DispatchableEvent {
  public static final int DATAOBJECT_ADDED         = 1;
  public static final int DATAOBJECT_REMOVED       = 2;
  public static final int DATAOBJECT_CHANGED       = 3;
  public static final int DATAOBJECTS_CHANGED      = 4;

  private int id;
  private int affectedDataObjectIndex;

  public DataObjectChangeEvent(Object source, int id) {
    this(source, id, -1);
  }

  public DataObjectChangeEvent(Object source, int id, int affectedDataObjectIndex) {
    super(source);
    this.id = id;
    this.affectedDataObjectIndex = affectedDataObjectIndex;
  }

  public void dispatch(EventListener listener) {
    ((DataObjectChangeListener)listener).dataChanged(this);
  }


  public boolean isMultiObjectChange() {
    return affectedDataObjectIndex == -1;
  }

  public int getAffectedDataObjectIndex() {
    return affectedDataObjectIndex;
  }

  public final int    getId() {
    return id;
  }

  public String toString() {
    return super.toString()+" "+id;
  }
}
