package org.objectstyle.cayenne.dataview;

import java.util.*;

public interface DataObjectChangeListener extends EventListener {
  void dataChanged(DataObjectChangeEvent event);
}
