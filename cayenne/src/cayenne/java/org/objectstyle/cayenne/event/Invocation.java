
package org.objectstyle.cayenne.event;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

public class Invocation extends Object
{
	private static final Logger log = Logger.getLogger(Invocation.class);
	
	private Method _method;
	private WeakReference _target;
	
	public Invocation(Object target, Method method)
	{
		super();

		if (target == null)
		{
			throw new IllegalArgumentException("target argument must not be null");
		}

		if (method == null)
		{
			throw new IllegalArgumentException("method argument must not be null");
		}	

		_method = method;
		_target = new WeakReference(target);
	}

	public boolean fire(ObserverEvent event)
	{
		boolean success = true;

		if (event == null)
		{
			throw new IllegalArgumentException("event may not be null!");
		}

		Object currentTarget = _target.get();

		if (currentTarget != null)
		{
			try
			{	
				_method.invoke(currentTarget, new Object[] { event });
			}
	
			catch (Exception ex)
			{
				log.error("exception while firing '" + _method.getName() + "'", ex);
				success = false;
			}
		}
		else
		{
			success = false;
		}

		return success;
	}

	public boolean equals(Object obj)
	{
		if ((obj != null) && (obj.getClass().equals(this.getClass())))
		{
			Invocation otherInvocation = (Invocation)obj;
			if (_method.equals(otherInvocation.getMethod()))
			{
				Object otherTarget = otherInvocation.getTarget();
				Object target = _target.get();

				if ((target == null) && (otherTarget == null))
				{
					return true;
				}

				if ((target == null) && (otherTarget != null))
				{
					return false;
				}

				if (target != null)
				{
					return target.equals(otherTarget);
				}
			}

			return false;
		}
		else
		{
			return super.equals(obj);
		}
	}

	public int hashCode()
	{
		int hash = 42;
		int hashMultiplier = 59;
		hash = hash * hashMultiplier + _method.hashCode();
		if (_target.get() != null)
		{
			hash = hash * hashMultiplier + _target.get().hashCode();
		}
		return hash;
	}
		
	protected Method getMethod()
	{
		return _method;
	}

	protected Object getTarget()
	{
		return _target.get();
	}
}
