
package org.objectstyle.cayenne.event;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

import junit.framework.Assert;

public class ObserverManagerTst extends CayenneTestCase
{
	private static final Logger log = Logger.getLogger(ObserverManagerTst.class);
	
	private ObserverManager _observerManager;
	private boolean _didReceiveNotification;

	static
	{
		BasicConfigurator.configure();	
	}
	
	public ObserverManagerTst(String arg0)
	{
		super(arg0);
	}
	
	public void setUp() throws Exception
	{
		_observerManager = new ObserverManager();
		_didReceiveNotification = false;
	}

	public void testNullObserver() throws NoSuchMethodException
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "hansi");
			_observerManager.addObserver(null, null, subject);
			Assert.fail();
		}

		catch (IllegalArgumentException ia)
		{
			// OK: argument is supposed to be illegal
		}
	}
	
	public void testNullNotification()
	{
		// null notification
		try
		{
			_observerManager.addObserver(this, "testNullObserver", null);
			Assert.fail();
		}
		catch (Exception e) 
		{	
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}
		
		// empty string notification
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "");		
			_observerManager.addObserver(this, "testNullObserver", subject);
			Assert.fail();
		}
		catch (Exception e)
		{
			Assert.assertTrue(e instanceof IllegalArgumentException);
		}
	}
	
	public void testNonexistingMethod()
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "hansi");		
			_observerManager.addObserver(this, "thisMethodDoesNotExist", subject);
			Assert.fail();
		}
		catch (Exception e)
		{
			Assert.assertTrue(e instanceof NoSuchMethodException);
		}
	}
	
	public void testInvalidArgumentTypes()
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "hansi");		
			_observerManager.addObserver(this, "seeTheWrongMethod", subject);
			Assert.fail();
		}
		catch (Exception e)
		{
			Assert.assertTrue(e instanceof NoSuchMethodException);
		}
	}
	
	public void testNonretainedObserver() throws NoSuchMethodException
	{
		ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");
		_observerManager.addObserver(new ObserverManagerTst(""), "seeNotification", subject);
		
		System.gc();
		_observerManager.postObserverEvent(subject, this);
	}
	
	public void testSuccessfulNotification()
	{
		try
		{
			ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");		
			_observerManager.addObserver(this, "seeNotification", subject);
			_observerManager.postObserverEvent(subject, this);
			
			Assert.assertTrue(_didReceiveNotification);
		}
		catch (Exception e)
		{
			log.error("testSuccessfulNotification", e);
			Assert.fail();
		}
	}
	
	public void testRemoveOnEmptyList()
	{
		ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");		
		_observerManager.removeObserver(this, subject);
	}
	
	public void testRemoveOnNullObserverSubject()
	{
		Assert.assertEquals(false, _observerManager.removeObserver(this, null));
	}
	
	public void testRemove() throws NoSuchMethodException
	{
		ObserverSubject subject = ObserverSubject.getSubject(this.getClass(), "XXX");		
		_observerManager.addObserver(this, "seeNotification", subject);
		
		boolean didRemove = _observerManager.removeObserver(this, subject);
		Assert.assertTrue(didRemove);
	}

	//-------------------------------------------------------------------------
	// notification method
	//-------------------------------------------------------------------------
	public void seeNotification(ObserverEvent event)
	{
		log.debug("seeNotification. publisher: " + event.getPublisher().getClass().getName());
		_didReceiveNotification = true;
	}
	
	public void seeTheWrongMethod(int hansi)
	{
		log.debug("seeTheWrongMethod: " + hansi);
	}
}
