package org.objectstyle.cayenne.dataview;

import java.util.*;

public class EventDispatcher {
  protected transient ArrayList listeners = new ArrayList(1);

  public void dispatch(DispatchableEvent e) {
    EventListener[] listenersCopy = null;
    synchronized(this) {
      if (hasListeners())
        listenersCopy = (EventListener[])listeners.toArray(new EventListener[listeners.size()]);
    }

    if (listenersCopy != null) {
      int count = listenersCopy.length;
      for (int index = 0; index < count; ++index) {
        e.dispatch(listenersCopy[index]);
      }
    }
  }

  public synchronized boolean hasListeners() {
    return !listeners.isEmpty();
  }

  public synchronized int getListenerCount() {
    return listeners.size();
  }

  public synchronized int find(EventListener listener) {
    return listeners.indexOf(listener);
  }

  public synchronized void add(EventListener listener) {
    if (find(listener) < 0)
      listeners.add(listener);
  }

  public synchronized void remove(EventListener listener) {
    listeners.remove(listener);
  }

  public synchronized void clear() {
    listeners.clear();
  }

  public static EventDispatcher add(EventDispatcher dispatcher, EventListener listener) {
    if (dispatcher == null)
      dispatcher = new EventDispatcher();
    dispatcher.add(listener);
    return dispatcher;
  }

  public final static EventDispatcher remove(EventDispatcher dispatcher, EventListener listener) {
    if (dispatcher != null) {
      dispatcher.remove(listener);
      if (!dispatcher.hasListeners())
        dispatcher = null;
    }
    return dispatcher;
  }
}
