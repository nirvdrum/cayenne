package org.objectstyle.cayenne.dataview;

import java.util.EventListener;

public interface FieldValueChangeListener extends EventListener {
  void fieldValueChanged(FieldValueChangeEvent event);
}