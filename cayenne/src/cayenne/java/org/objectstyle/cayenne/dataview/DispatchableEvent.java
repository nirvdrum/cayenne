package org.objectstyle.cayenne.dataview;

import java.util.*;

public abstract class DispatchableEvent extends EventObject {

  public DispatchableEvent(Object source) {
    super(source);
  }

  public abstract void dispatch(EventListener listener);

  public String toString() {
    String cn = getClass().getName();
    return cn.substring(cn.lastIndexOf('.')+1) +  "[" + paramString() + "]";
  }

  protected String paramString() {
    return "";
  }

}
