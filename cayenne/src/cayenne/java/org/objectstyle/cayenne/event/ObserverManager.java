
package org.objectstyle.cayenne.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class acts as bridge between an Object that wants to inform others about
 * its current state (Publisher) and a list of objects interested in the Subject
 * (Observers).
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstätte
 */
public class ObserverManager extends Object
{
	private static final Logger log = Logger.getLogger(ObserverManager.class);
	private static final ObserverManager _instance = new ObserverManager();
	
	private HashMap _subjects;

	public static ObserverManager getInstance()
	{
		return _instance;
	}
	
	public ObserverManager()
	{
		super();
		_subjects = new HashMap();
	}
	
	public void addObserver(Object observer, String methodName, ObserverSubject subject) throws NoSuchMethodException
	{
		if (observer == null)
		{
			throw new IllegalArgumentException("observer may not be null");
		}
		
		if (subject == null)
		{
			throw new IllegalArgumentException("subject may not be null");
		}

		Method method = observer.getClass().getMethod(methodName, new Class[] { ObserverEvent.class });		
		Invocation inv = new Invocation(observer, method);

		Set observersForSubject = this.observersForSubject(subject);
		if (observersForSubject == null)
		{
			this.addSubject(subject);
			observersForSubject = this.observersForSubject(subject);
		}

		observersForSubject.add(inv);
	}
	
	public void postObserverEvent(ObserverSubject subject, Object sender)
	{
		this.postObserverEvent(subject, sender, null);
	}

	public void postObserverEvent(ObserverSubject subject, Object sender, Map info)
	{
		// get current observers for subject
		Set observersForEvent = this.observersForSubject(subject);

		if (observersForEvent != null)
		{
			// create only one ObserverEvent that will be reused for all Observers
			ObserverEvent event = new ObserverEvent(sender, info);

			// used to collect all invalid invocations in order to remove
			// them at the end of this posting cycle
			List invalidInvocations = null;

			Iterator iter = observersForEvent.iterator();
			while (iter.hasNext())
			{
				Invocation invocation = (Invocation)iter.next();

				// fire invocation, detect if anything went wrong
				if (!invocation.fire(event))
				{
					log.debug("invalid invocation detected");

					if (invalidInvocations == null)
					{
						invalidInvocations = new ArrayList();
					}

					invalidInvocations.add(invocation);
				}
			}

			// clear out all invalid invocations
			if (invalidInvocations != null)
			{
				observersForEvent.removeAll(invalidInvocations);	
			}
		}
	}
	
	/**
	 * @return <code>true</code> if <code>observer</code> could be removed,
	 * else returns <code>false</code>.
	 */
	public boolean removeObserver(Object observer, ObserverSubject subject)
	{
		boolean didRemove = false;

		// @HH: is this correct? observer/null subject should IMHO remove for all subjects..
		if ((observer != null) && (subject != null))
		{
			Set observersForSubject = this.observersForSubject(subject);
	
			if (observersForSubject != null)
			{
				// remove all invocations with the given target
				Iterator observerIter = observersForSubject.iterator();
				while (observerIter.hasNext())
				{
					Invocation element = (Invocation)observerIter.next();
					if (element.getTarget() == observer)
					{
						log.debug("will remove " + observer);
						observerIter.remove();
						didRemove = true;
					}	
				}

				// remove the subject if the corresponding observer queue is empty
				if (observersForSubject.isEmpty())
				{
					log.debug("no remaining observers for '" + subject + "' - removing subject");
					this.removeSubject(subject);
				}
			}
		}

		return didRemove;
	}

	/**
	 * add a new subject & its observer queue to the observer map
	 * @param subject the ObserverSubject to be added
	 */
	private void addSubject(ObserverSubject subject)
	{
		// make sure we don't add a subject twice, losing the old observers
		if (_subjects.get(subject) == null)
		{
			log.debug("no observers for '" + subject + "' - creating new subject");
			_subjects.put(subject, new HashSet());
		}
	}

	/**
	 * remove a subject & its observer queue from the observer map
	 * @param subject the ObserverSubject to be removed
	 */
	private void removeSubject(ObserverSubject subject)
	{
		_subjects.remove(subject);
	}

	/**
	 * @return Set containing all the observers for <code>subject</code>.
	 */
	private Set observersForSubject(ObserverSubject subject)
	{
		return (Set)_subjects.get(subject);
	}
}
