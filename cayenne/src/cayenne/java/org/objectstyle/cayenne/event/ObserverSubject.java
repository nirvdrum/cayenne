
package org.objectstyle.cayenne.event;

/**
 * This class encapsulates the String that is used to identify the <em>subject</em> 
 * an observer is interested in. Using plain Strings causes several problems:
 * <ul>
 * <li>it's easy to misspell a subject leading to undesired behaviour at runtime
 * that is hard to debug.</li>
 * <li>in systems with many different subjects there's no safeguard for defining 
 * the same subject twice for different purposes. This is especially true in a
 * distributed setting.
 * </ul>
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstätte
 */

public class ObserverSubject extends Object
{
	private String _subject;
	
	public static ObserverSubject getSubject(Class sender, String subjectName)
	{
		if (sender == null)
		{
			throw new IllegalArgumentException("sender may not be null");
		}

		if ((subjectName == null) || (subjectName.length() == 0))
		{
			throw new IllegalArgumentException("subjectName must not be empty");
		}
		
		ObserverSubject instance = new ObserverSubject();
		instance.setSubject(sender.getName() + "." + subjectName);
		return instance;
	}

	/**
	 *  make sure that the only way to create ObserverSubjects is via the static method.
	 */
	private ObserverSubject()
	{
		super();
		_subject = null;
	}
	
	private void setSubject(String subject)
	{
		_subject = subject;
	}
	
	public String toString()
	{
        StringBuffer buf = new StringBuffer(64);

        buf.append("<");
        buf.append(this.getClass().getName());
        buf.append(" 0x");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append("> ");
        buf.append(_subject);
        
        return buf.toString();
	}

}
