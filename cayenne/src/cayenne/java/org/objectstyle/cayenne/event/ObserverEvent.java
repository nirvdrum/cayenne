
package org.objectstyle.cayenne.event;

import java.util.Collections;
import java.util.Map;

/**
 * This class encapsulates the information that is passed from the ObserverManager
 * to the Observer.
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstätte
 */

public class ObserverEvent extends Object
{
	private Object _publisher;
	private Map _info;

	private ObserverEvent()
	{
		super();
	}	

	public ObserverEvent(Object sender)
	{
		// do not create an empty map object; rather reuse the immutable singleton
		// that's provided by the JDK instead.
		this(sender, Collections.EMPTY_MAP);
	}

	public ObserverEvent(Object sender, Map info)
	{
		this();
		_publisher = sender;
		_info = info;
	}

	public Object getPublisher()
	{
		return _publisher;
	}
	
	public Map getInfo()
	{
		return _info;
	}
}

