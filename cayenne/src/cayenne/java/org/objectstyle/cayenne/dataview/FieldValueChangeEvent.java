package org.objectstyle.cayenne.dataview;

import java.util.*;
import org.objectstyle.cayenne.*;

public class FieldValueChangeEvent extends DispatchableEvent {
  private DataObject modifiedObject;
  private Object oldValue;
  private Object newValue;

  public FieldValueChangeEvent(
      ObjEntityViewField source,
      DataObject modifiedObject,
      Object oldValue,
      Object newValue) {
    super(source);
    this.modifiedObject = modifiedObject;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }
  public void dispatch(EventListener listener) {
    ((FieldValueChangeListener)listener).fieldValueChanged(this);
  }
  public ObjEntityViewField getField() {
    return (ObjEntityViewField)getSource();
  }
  public DataObject getModifiedObject() {
    return modifiedObject;
  }
  public Object getNewValue() {
    return newValue;
  }
  public Object getOldValue() {
    return oldValue;
  }
}