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
		validator.reset();
		validator.validateDomains(new DataDomain[] { d1 });
		assertValidator(ErrorMsg.NO_ERROR);

		// should complain about no name
		DataDomain d2 = new DataDomain();
		validator.reset();
		validator.validateDomains(new DataDomain[] { d2 });
		assertValidator(ErrorMsg.ERROR);

		// should complain about duplicate name
		DataDomain d3 = new DataDomain(d1.getName());
		validator.reset();
		validator.validateDomains(new DataDomain[] { d1, d3 });
		assertValidator(ErrorMsg.ERROR);
	}

	public void testValidateDataNodes() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DataNode n1 = new DataNode("xyz");
		n1.setAdapter(new JdbcAdapter());
		n1.setDataSourceFactory("123");
		validator.reset();
		validator.validateDataNodes(d1, new DataNode[] { n1 });
		assertValidator(ErrorMsg.NO_ERROR);

		// should complain about no name
		DataNode n2 = new DataNode();
		n2.setAdapter(new JdbcAdapter());
		n2.setDataSourceFactory("123");
		validator.reset();
		validator.validateDataNodes(d1, new DataNode[] { n2 });
		assertValidator(ErrorMsg.ERROR);

		// should complain about duplicate name
		DataNode n3 = new DataNode(n1.getName());
		n3.setAdapter(new JdbcAdapter());
		n3.setDataSourceFactory("123");
		validator.reset();
		validator.validateDataNodes(d1, new DataNode[] { n1, n3 });
		assertValidator(ErrorMsg.ERROR);
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
		validator.reset();
		validator.validateDbAttributes(d1, m1, e1);
		assertValidator(ErrorMsg.NO_ERROR);

		// should complain about no name
		DbAttribute a2 = new DbAttribute();
		a2.setType(Types.CHAR);
		a2.setMaxLength(2);
		DbEntity e2 = new DbEntity("e2");
		e2.addAttribute(a2);
		validator.reset();
		validator.validateDbAttributes(d1, m1, e2);
		assertValidator(ErrorMsg.ERROR);

		// should complain about no max length
		DbAttribute a3 = new DbAttribute();
		a3.setName("a3");
		a3.setType(Types.CHAR);
		DbEntity e3 = new DbEntity("e3");
		e3.addAttribute(a3);
		validator.reset();
		validator.validateDbAttributes(d1, m1, e3);
		assertValidator(ErrorMsg.WARNING);

		// should complain about no type
		DbAttribute a4 = new DbAttribute();
		a4.setName("a4");
		DbEntity e4 = new DbEntity("e4");
		e4.addAttribute(a4);
		validator.reset();
		validator.validateDbAttributes(d1, m1, e4);
		assertValidator(ErrorMsg.WARNING);
	}

	public void testValidateObjRels() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DataMap m1 = new DataMap();
		ObjRelationship or1 = buildValidObjRelationship("r1");
		validator.reset();
		validator.validateObjRels(d1, m1, (ObjEntity) or1.getSourceEntity());
		assertValidator(ErrorMsg.NO_ERROR);

		// no target entity, must give a warning
		ObjRelationship or2 = buildValidObjRelationship("r2");
		or2.setTargetEntity(null);
		validator.reset();
		validator.validateObjRels(d1, m1, (ObjEntity) or2.getSourceEntity());
		assertValidator(ErrorMsg.WARNING);

		// no DbRelationship mapping, must give a warning
		ObjRelationship or3 = buildValidObjRelationship("r2");
		or3.removeAllDbRelationships();
		validator.reset();
		validator.validateObjRels(d1, m1, (ObjEntity) or3.getSourceEntity());
		assertValidator(ErrorMsg.WARNING);

		// no name, must give an error
		ObjRelationship or4 = buildValidObjRelationship("r2");
		or4.setName(null);
		validator.reset();
		validator.validateObjRels(d1, m1, (ObjEntity) or4.getSourceEntity());
		assertValidator(ErrorMsg.ERROR);
	}
	
    public void testValidateDbRels() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DataMap m1 = new DataMap();
		DbRelationship dr1 = buildValidDbRelationship("r1");
		validator.reset();
		validator.validateDbRels(d1, m1, (DbEntity) dr1.getSourceEntity());
		assertValidator(ErrorMsg.NO_ERROR);
		
		// no target entity
		DbRelationship dr2 = buildValidDbRelationship("r2");
		dr2.setTargetEntity(null);
		validator.reset();
		validator.validateDbRels(d1, m1, (DbEntity) dr2.getSourceEntity());
		assertValidator(ErrorMsg.WARNING);
		
    	// no name
		DbRelationship dr3 = buildValidDbRelationship("r3");
		dr3.setName(null);
		validator.reset();
		validator.validateDbRels(d1, m1, (DbEntity) dr3.getSourceEntity());
		assertValidator(ErrorMsg.ERROR);		
		
		// no joins
		DbRelationship dr4 = buildValidDbRelationship("r4");
		dr4.removeAllJoins();
		validator.reset();
		validator.validateDbRels(d1, m1, (DbEntity) dr4.getSourceEntity());
		assertValidator(ErrorMsg.WARNING);		
	}

	protected DbRelationship buildValidDbRelationship(String name) {
		DbEntity src = new DbEntity("e1");
		DbEntity target = new DbEntity("e2");
		DbRelationship dr1 = new DbRelationship(src, target, null);
		dr1.setName(name);
		src.addRelationship(dr1);
		return dr1;
	}

	protected ObjRelationship buildValidObjRelationship(String name) {
		DbRelationship dr1 = buildValidDbRelationship("d" + name);
		
		ObjEntity src = new ObjEntity("e1");
		src.setDbEntity((DbEntity) dr1.getSourceEntity());
		
		ObjEntity target = new ObjEntity("e2");
		target.setDbEntity((DbEntity) dr1.getTargetEntity());

		ObjRelationship r1 = new ObjRelationship(src, target, dr1.isToMany());
		r1.setName(name);
		src.addRelationship(r1);
		
		r1.addDbRelationship(dr1);
		return r1;
	}

	protected void assertValidator(int expectedSeverity) {
		assertEquals(expectedSeverity, validator.getErrorSeverity());
	}
}
