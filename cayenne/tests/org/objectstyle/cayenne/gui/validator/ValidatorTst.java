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
package org.objectstyle.cayenne.gui.validator;

import java.sql.Types;

import junit.framework.TestCase;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.map.*;

/**
 * Test cases for the Validator class.
 * 
 * @author Andrei Adamchik
 */
public class ValidatorTst extends TestCase {
	protected Validator validator;

	/**
	 * Constructor for ValidatorTst.
	 */
	public ValidatorTst(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		validator = new Validator(Mediator.getMediator());
	}

	public void testValidateDomains() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		assertEquals(
			ErrorMsg.NO_ERROR,
			validator.validateDomains(new DataDomain[] { d1 }));

		// should complain about no name
		DataDomain d2 = new DataDomain();
		assertEquals(
			ErrorMsg.ERROR,
			validator.validateDomains(new DataDomain[] { d2 }));

		// should complain about duplicate name
		DataDomain d3 = new DataDomain(d1.getName());
		assertEquals(
			ErrorMsg.ERROR,
			validator.validateDomains(new DataDomain[] { d1, d3 }));
	}

	public void testValidateDataNodes() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DataNode n1 = new DataNode("xyz");
		n1.setAdapter(new JdbcAdapter());
		n1.setDataSourceFactory("123");
		assertEquals(
			ErrorMsg.NO_ERROR,
			validator.validateDataNodes(d1, new DataNode[] { n1 }));

		// should complain about no name
		DataNode n2 = new DataNode();
		n2.setAdapter(new JdbcAdapter());
		n2.setDataSourceFactory("123");
		assertEquals(
			ErrorMsg.ERROR,
			validator.validateDataNodes(d1, new DataNode[] { n2 }));

		// should complain about duplicate name
		DataNode n3 = new DataNode(n1.getName());
		n3.setAdapter(new JdbcAdapter());
		n3.setDataSourceFactory("123");
		assertEquals(
			ErrorMsg.ERROR,
			validator.validateDataNodes(d1, new DataNode[] { n1, n3 }));
	}

	public void testValidateDbAttributes() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DataMap m1 = new DataMap();
		
		DbAttribute a1 = new DbAttribute();
		a1.setName("a1");
		a1.setType(Types.CHAR);
		a1.setMaxLength(2);
		DbEntity e1 = new DbEntity("e1");
		e1.addAttribute(a1);
		assertEquals(
			ErrorMsg.NO_ERROR,
			validator.validateDbAttributes(d1, m1, e1));

		// should complain about no name
		DbAttribute a2 = new DbAttribute();
		a2.setType(Types.CHAR);
		a2.setMaxLength(2);
		DbEntity e2 = new DbEntity("e2");
		e2.addAttribute(a2);
		assertEquals(
			ErrorMsg.ERROR,
			validator.validateDbAttributes(d1, m1, e2));

		// should complain about no max length
		DbAttribute a3 = new DbAttribute();
		a3.setName("a3");
		a3.setType(Types.CHAR);
		DbEntity e3 = new DbEntity("e3");
		e3.addAttribute(a3);
		assertEquals(
			ErrorMsg.WARNING,
			validator.validateDbAttributes(d1, m1, e3));
			
		// should complain about no type
		DbAttribute a4 = new DbAttribute();
		a4.setName("a4");
		DbEntity e4 = new DbEntity("e4");
		e4.addAttribute(a4);
		assertEquals(
			ErrorMsg.WARNING,
			validator.validateDbAttributes(d1, m1, e4));
	}
}
