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

import org.objectstyle.cayenne.unittest.CayenneTestCase;

import junit.framework.Assert;

public class EventSubjectTst
	extends CayenneTestCase
{

	public void testIllegalArguments() {
		try {
			EventSubject.getSubject(null, "Subject");
			Assert.fail();
		}
		
		catch (IllegalArgumentException ex) {
			// OK
		}

		try {
			EventSubject.getSubject(Object.class, null);
			Assert.fail();
		}
		
		catch (IllegalArgumentException ex) {
			// OK
		}

		try {
			EventSubject.getSubject(Object.class, "");
			Assert.fail();
		}
		
		catch (IllegalArgumentException ex) {
			// OK
		}
	}

	public void testIdenticalSubject() {
		EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
		EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
		Assert.assertSame(s1, s2);
	}

	public void testEqualityOfIdenticalSubjects() {
		EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
		EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
		Assert.assertEquals(s1, s2);
	}

	public void testEqualityOfSubjectsByDifferentOwner() {
		EventSubject s1 = EventSubject.getSubject(EventSubject.class, "MySubject");
		EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
		Assert.assertFalse(s1.equals(s2));
	}

	public void testEqualityOfSubjectsByDifferentTopic() {
		EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "Subject1");
		EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "Subject2");
		Assert.assertFalse(s1.equals(s2));
	}

	public void testSubjectEqualsNull() {
		EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
		Assert.assertFalse(s1.equals(null));
	}

	public void testSubjectGC() {
		EventSubject s = EventSubject.getSubject(EventSubjectTst.class, "GCSubject");
		long hash1 = s.hashCode();

		// try to make the subject go away
		s = null;
		System.gc();
		System.gc();

		s = EventSubject.getSubject(EventSubjectTst.class, "GCSubject");
		long hash2 = s.hashCode();

		Assert.assertTrue(hash1 != hash2);
	}

}

