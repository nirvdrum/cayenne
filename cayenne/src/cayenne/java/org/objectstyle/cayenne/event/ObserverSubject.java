/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */ 

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
