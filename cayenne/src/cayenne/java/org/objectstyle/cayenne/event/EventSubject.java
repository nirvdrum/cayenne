/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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

import java.util.Map;

import org.apache.commons.collections.ReferenceMap;

/**
 * This class encapsulates the String that is used to identify the <em>subject</em> 
 * that a listener is  interested in. Using plain Strings causes several severe
 * problems:
 * <ul>
 * <li>it's easy to misspell a subject, leading to undesired behaviour at
 * runtime that is hard to debug.</li>
 * <li>in systems with many different subjects there is no safeguard for
 * defining the same subject twice for different purposes. This is especially
 * true in a distributed setting.
 * </ul>
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstaette
 */

public class EventSubject extends Object {

	// a Map that will allow the values to be GC'ed
	private static Map _registeredSubjects = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

	// Subject identifier in the form "com.foo.bar/SubjectName"
	private String _fullyQualifiedSubjectName;

	/**
	 * Returns an event subject identified by the given owner and subject name.
	 * 
	 * @param subjectOwner the Class used for uniquely identifying this subject
	 * @param subjectName a String used as name, e.g. "MyEventTopic"
	 * @throws IllegalArgumentException if subjectOwner/subjectName are
	 * <code>null</code> or subjectName is empty.
	 */
	public static EventSubject getSubject(Class subjectOwner, String subjectName) {
		if (subjectOwner == null) {
			throw new IllegalArgumentException("Owner class must not be null.");
		}

		if ((subjectName == null) || (subjectName.length() == 0)) {
			throw new IllegalArgumentException("Subject name must not be null or empty.");
		}

		String fullSubjectName = subjectOwner.getName() + "/" + subjectName;
		EventSubject newSubject = (EventSubject)_registeredSubjects.get(fullSubjectName);
		if (newSubject == null) {
			newSubject = new EventSubject(fullSubjectName);
			_registeredSubjects.put(newSubject.getSubjectName(), newSubject);
		}

		return newSubject;
	}

	/**
	 * Private constructor to force use of #getSubject(Class, String)
	 */
	private EventSubject() {
	}

	/**
	 * Protected constructor for new subjects.
	 * 
	 * @param subject the name of the new subject to be created
	 */
	protected EventSubject(String fullSubjectName) {
		super();
		_fullyQualifiedSubjectName = fullSubjectName;
	}

	public boolean equals(Object obj) {
		if (obj instanceof EventSubject) {
			return _fullyQualifiedSubjectName.equals(((EventSubject)obj).getSubjectName());
		}
		
		return false;
	}

	public int hashCode() {
		return (super.hashCode() | _fullyQualifiedSubjectName.hashCode());
	}

	public String getSubjectName() {
		return _fullyQualifiedSubjectName;
	}

	/**
	 * @return a String in the form
	 * <code>&lt;ClassName 0x123456&gt; SomeName</code>
	 * 
	 * @see Object#toString()
	 */
	public String toString() {
        StringBuffer buf = new StringBuffer(64);

        buf.append("<");
        buf.append(this.getClass().getName());
        buf.append(" 0x");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append("> ");
        buf.append(_fullyQualifiedSubjectName);
        
        return buf.toString();
	}

}
