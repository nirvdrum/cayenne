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
package org.objectstyle.cayenne.util;

import java.util.*;

import org.objectstyle.cayenne.unittest.*;

/**
 * @author Andrei Adamchik
 */
public class CayenneMapTst extends CayenneTestCase {

	/**
	 * Constructor for CayenneMapTst.
	 * @param arg0
	 */
	public CayenneMapTst(String arg0) {
		super(arg0);
	}

	protected CayenneMapEntry makeEntry() {
		return new CayenneMapEntry() {
			protected Object parent;

			public String getName() {
				return "abc";
			}

			public Object getParent() {
				return parent;
			}

			public void setParent(Object parent) {
				this.parent = parent;
			}
		};
	}

	public void testConstructor1() throws Exception {
		Object o1 = new Object();
		String k1 = "123";
		Map map = new HashMap();
		map.put(k1, o1);
		CayenneMap cm = new CayenneMap(null, map);
		assertSame(o1, cm.get(k1));
	}

	public void testConstructor2() throws Exception {
		Object parent = new Object();
		CayenneMapEntry o1 = makeEntry();
		String k1 = "123";
		Map map = new HashMap();
		map.put(k1, o1);
		CayenneMap cm = new CayenneMap(parent, map);
		assertSame(o1, cm.get(k1));
		assertSame(parent, o1.getParent());
	}

	public void testPut() throws Exception {
		Object parent = new Object();
		CayenneMapEntry o1 = makeEntry();
		String k1 = "123";
		CayenneMap cm = new CayenneMap(parent);
		cm.put(k1, o1);
		assertSame(o1, cm.get(k1));
		assertSame(parent, o1.getParent());
	}

	public void testParent() throws Exception {
		Object parent = new Object();
		CayenneMap cm = new CayenneMap(null);
		assertNull(cm.getParent());
		cm.setParent(parent);
		assertSame(parent, cm.getParent());
	}
}
