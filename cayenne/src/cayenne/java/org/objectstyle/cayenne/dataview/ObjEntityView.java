package org.objectstyle.cayenne.dataview;

import java.util.*;
import org.objectstyle.cayenne.map.*;
import org.apache.commons.lang.*;

public class ObjEntityView {
  private DataView owner;
  private ObjEntity objEntity;
  private List fields = new ArrayList();
  private Map nameFieldMap = new HashMap();
  private List readOnlyFields = Collections.unmodifiableList(fields);
  private String name;

  public ObjEntityView() {
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    Validate.notNull(name);
    this.name = name;
  }
  public List getFields() {
    return readOnlyFields;
  }
  public List getVisibleFields() {
    int size = getFieldCount();
    List dst = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      ObjEntityViewField field = getField(i);
      if (field.isVisible())
        dst.add(field);
    }
    return dst;
  }
  public List getEditableFields() {
    int size = getFieldCount();
    List dst = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      ObjEntityViewField field = getField(i);
      if (field.isVisible() && field.isEditable())
        dst.add(field);
    }
    return dst;
  }
  public ObjEntity getObjEntity() {
    return objEntity;
  }
  public void setObjEntity(ObjEntity objEntity) {
    Validate.notNull(objEntity);
    this.objEntity = objEntity;
  }

  public ObjEntityViewField getField(int index) {
    return (ObjEntityViewField)fields.get(index);
  }

  public ObjEntityViewField getField(String fieldName) {
    return (ObjEntityViewField)nameFieldMap.get(fieldName);
  }

  public ObjEntityViewField getFieldForObjAttribute(String objAttributeName) {
    for (int i = 0; i < fields.size(); i++) {
      ObjEntityViewField field = getField(i);
      if (objAttributeName.equals(field.getObjAttribute().getName()))
        return field;
    }
    return null;
  }

  public int getFieldCount() {
    return fields.size();
  }

  public boolean hasFields() {
    return !fields.isEmpty();
  }

  public void clearFields() {
    for (int i = 0; i < getFieldCount(); i++) {
      ObjEntityViewField field = getField(i);
      field.setOwner(null);
      field.setIndex(-1);
    }
    fields.clear();
    nameFieldMap.clear();
  }

  public boolean removeField(ObjEntityViewField field) {
    if (field.getOwner() != this)
      return false;
    field.setOwner(null);
    field.setIndex(-1);
    nameFieldMap.remove(field.getName());
    return fields.remove(field);
  }

  public int insertField(ObjEntityViewField field) {
    if (field.getOwner() == this)
      return field.getIndex();

    Validate.notNull(field.getName());
    Validate.isTrue(!nameFieldMap.containsKey(field.getName()));
    field.setOwner(this);
    nameFieldMap.put(field.getName(), field);

    int prefIndex = field.getPreferredIndex();
    int fieldCount = getFieldCount();

    if (prefIndex < 0) {
      int newIndex = fieldCount;
      fields.add(field);
      field.setIndex(newIndex);
      return newIndex;
    }

    int index = 0;
    int curPrefIndex = -1;
    while (index < fieldCount &&
           ((curPrefIndex = getField(index++).getPreferredIndex()) <= prefIndex) &&
           curPrefIndex >= 0);

    fields.add(index, field);
    field.setIndex(index);
    fieldCount++;

    for (int i = (index + 1); i < fieldCount; i++) {
      getField(i).setIndex(i);
    }

    return index;
  }
  public DataView getOwner() {
    return owner;
  }
  void setOwner(DataView owner) {
    this.owner = owner;
  }
}