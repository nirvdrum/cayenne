package org.objectstyle.cayenne.dataview;

import org.objectstyle.cayenne.*;
import javax.swing.ComboBoxModel;

public class DOComboBoxModel extends DOListModel implements ComboBoxModel {
  protected DataObject selectedObject;

  public DOComboBoxModel() {
  }

  public void setSelectedDataObject(DataObject dataObject) {
    if ((selectedObject != null && !selectedObject.equals( dataObject )) ||
        selectedObject == null && dataObject != null) {
      selectedObject = dataObject;
      fireContentsChanged(this, -1, -1);
    }
  }
  public DataObject getSelectedDataObject() {
    return selectedObject;
  }

  public void setSelectedItem(Object selectedValue) {
    if (viewField == null) {
      if (selectedValue instanceof DataObject)
        setSelectedDataObject((DataObject)selectedValue);
      else
        setSelectedDataObject(null);
    } else {
      LookupCache cache = viewField.getOwner().getOwner().getLookupCache();
      setSelectedDataObject(cache.getDataObject(viewField, selectedValue));
    }
  }
  public Object getSelectedItem() {
    if (viewField == null)
      return getSelectedDataObject();
    return viewField.getValue(getSelectedDataObject());
  }
}