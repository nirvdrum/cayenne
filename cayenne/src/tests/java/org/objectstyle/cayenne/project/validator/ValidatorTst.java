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
package org.objectstyle.cayenne.project.validator;

import java.io.File;
import java.sql.Types;

import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.project.Project;

/**
 * Test cases for the Validator class.
 * 
 * @author Andrei Adamchik
 */
public class ValidatorTst extends CayenneTestCase {

	/**
	 * Constructor for ValidatorTst.
	 */
	public ValidatorTst(String name) {
		super(name);
	}
    
	public void testProject() throws Exception {
		Project project = new Project("abc", new File(System.getProperty("user.dir")));
		Validator validator = new Validator(project);
		assertSame(project, validator.getProject());
	}

/*	

	
	public void testValidateObjAttributes() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		ObjAttribute oa1 = buildValidObjAttribute("a1");
		
		validator.reset();
		validator.validateObjAttributes(d1, map, (ObjEntity)oa1.getEntity());
		assertValidator(ValidationResult.VALID);
		
		oa1.setDbAttribute(null);
		validator.reset();
		validator.validateObjAttributes(d1, map, (ObjEntity)oa1.getEntity());
		assertValidator(ValidationResult.WARNING);
	}
	
	public void testValidateDbAttributes() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");

		DbAttribute a1 = new DbAttribute();
		a1.setName("a1");
		a1.setType(Types.CHAR);
		a1.setMaxLength(2);
		DbEntity e1 = new DbEntity("e1");
		map.addDbEntity(e1);
		e1.addAttribute(a1);
		validator.reset();
		validator.validateDbAttributes(d1, map, e1);
		assertValidator(ValidationResult.VALID);

		// should complain about no max length
		DbAttribute a3 = new DbAttribute();
		a3.setName("a3");
		a3.setType(Types.CHAR);
		DbEntity e3 = new DbEntity("e3");
		map.addDbEntity(e3);
		e3.addAttribute(a3);
		validator.reset();
		validator.validateDbAttributes(d1, map, e3);
		assertValidator(ValidationResult.WARNING);

		// should complain about no type
		DbAttribute a4 = new DbAttribute();
		a4.setName("a4");
		DbEntity e4 = new DbEntity("e4");
		map.addDbEntity(e4);
		e4.addAttribute(a4);
		validator.reset();
		validator.validateDbAttributes(d1, map, e4);
		assertValidator(ValidationResult.WARNING);
	}

	public void testValidateObjRels() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		ObjRelationship or1 = buildValidObjRelationship("r1");
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or1.getSourceEntity());
		assertValidator(ValidationResult.VALID);

		// no target entity, must give a warning
		ObjRelationship or2 = buildValidObjRelationship("r2");
		or2.setTargetEntity(null);
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or2.getSourceEntity());
		assertValidator(ValidationResult.WARNING);

		// no DbRelationship mapping, must give a warning
		ObjRelationship or3 = buildValidObjRelationship("r2");
		or3.clearDbRelationships();
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or3.getSourceEntity());
		assertValidator(ValidationResult.WARNING);

		// no name, must give an error
		ObjRelationship or4 = buildValidObjRelationship("r2");
		or4.setName(null);
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or4.getSourceEntity());
		assertValidator(ValidationResult.ERROR);
	}
	
    public void testValidateDbRels() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DbRelationship dr1 = buildValidDbRelationship("r1");
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr1.getSourceEntity());
		assertValidator(ValidationResult.VALID);
		
		// no target entity
		DbRelationship dr2 = buildValidDbRelationship("r2");
		dr2.setTargetEntity(null);
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr2.getSourceEntity());
		assertValidator(ValidationResult.WARNING);
		
    	// no name
		DbRelationship dr3 = buildValidDbRelationship("r3");
		dr3.setName(null);
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr3.getSourceEntity());
		assertValidator(ValidationResult.ERROR);		
		
		// no joins
		DbRelationship dr4 = buildValidDbRelationship("r4");
		dr4.removeAllJoins();
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr4.getSourceEntity());
		assertValidator(ValidationResult.WARNING);		
	}

	protected DbRelationship buildValidDbRelationship(String name) {
		DbEntity src = new DbEntity("e1" + counter++);
		DbEntity target = new DbEntity("e2" + counter++);
		map.addDbEntity(src);
		map.addDbEntity(target);
		DbRelationship dr1 = new DbRelationship(src, target, null);
		dr1.setName(name);
		src.addRelationship(dr1);
		return dr1;
	}

	protected ObjRelationship buildValidObjRelationship(String name) {
		DbRelationship dr1 = buildValidDbRelationship("d" + name);
		
		ObjEntity src = new ObjEntity("ey" + counter++);
		map.addObjEntity(src);
		src.setDbEntity((DbEntity) dr1.getSourceEntity());
		
		ObjEntity target = new ObjEntity("oey" + counter++);
		map.addObjEntity(target);
		target.setDbEntity((DbEntity) dr1.getTargetEntity());

		ObjRelationship r1 = new ObjRelationship(src, target, dr1.isToMany());
		r1.setName(name);
		src.addRelationship(r1);
		
		r1.addDbRelationship(dr1);
		return r1;
	}
	
	protected ObjAttribute buildValidObjAttribute(String name) {
		DbAttribute a1 = new DbAttribute();
		a1.setName("d" + name);
		a1.setType(Types.CHAR);
		a1.setMaxLength(2);
		DbEntity e1 = new DbEntity("ex" + counter++);
		map.addDbEntity(e1);
		e1.addAttribute(a1);
		
		ObjEntity oe1 = new ObjEntity("oex" + counter++);
		map.addObjEntity(oe1);
		oe1.setDbEntity(e1);
		
		ObjAttribute oa1 = new ObjAttribute(name, "java.lang.Integer", oe1);
		oe1.addAttribute(oa1);
		oa1.setDbAttribute(a1);
	
		return oa1;
	}

*/
}
