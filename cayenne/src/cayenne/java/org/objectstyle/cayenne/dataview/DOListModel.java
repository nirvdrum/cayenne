package org.objectstyle.cayenne.dataview;

import javax.swing.*;
import org.objectstyle.cayenne.*;

public class DOListModel extends AbstractListModel implements DataObjectChangeListener, FieldValueChangeListener {
  protected ObjEntityViewField viewField;
  protected DataObjectList dataObjects = new DataObjectList(1);

  public DOListModel() {
  }
  public void setViewField(ObjEntityViewField field) {
    if (this.viewField != null) {
      this.viewField.getOwner().getOwner().removeFieldValueChangeListener(this);
    }
    this.viewField = field;
    viewField.getOwner().getOwner().addFieldValueChangeListener(this);
    fireContentsChanged(this, 0, getSize());
  }
  public void setDataObjects(DataObjectList dataObjects) {
    this.dataObjects.removeDataObjectChangeListener(this);
    this.dataObjects = dataObjects;
    this.dataObjects.addDataObjectChangeListener(this);
    fireContentsChanged(this, 0, getSize());
  }
  public int getSize() {
    return dataObjects.size();
  }
  public DataObject getDataObject(int index) {
    return (DataObject)dataObjects.get(index);
  }
  public Object getElementAt(int index) {
    if (viewField == null)
      return getDataObject(index);
    return viewField.getValue(getDataObject(index));
  }
  public void dataChanged(DataObjectChangeEvent event) {
    if (event.isMultiObjectChange()) {
      fireContentsChanged(this, 0, getSize());
      return;
    }
    int affectedRow = event.getAffectedDataObjectIndex();
    switch (event.getId()) {
      case DataObjectChangeEvent.DATAOBJECT_ADDED:
        fireIntervalAdded(this, affectedRow, affectedRow);
        break;
      case DataObjectChangeEvent.DATAOBJECT_REMOVED:
        fireIntervalRemoved(this, affectedRow, affectedRow);
        break;
      case DataObjectChangeEvent.DATAOBJECT_CHANGED:
        fireContentsChanged(this, affectedRow, affectedRow);
        break;
      default:
        fireContentsChanged(this, 0, getSize());
    }
  }
  public ObjEntityViewField getViewField() {
    return viewField;
  }
  public DataObjectList getDataObjects() {
    return dataObjects;
  }
  public void fieldValueChanged(FieldValueChangeEvent event) {
    if (viewField != null && viewField.isSameObjAttribute(event.getField())) {
      int index = dataObjects.indexOf(event.getModifiedObject());
      if (index >= 0)
        fireContentsChanged(this, index, index);
    }
  }
}