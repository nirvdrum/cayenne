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
package org.objectstyle.cayenne.map;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class ObjEntityTst extends CayenneTestCase {
	protected ObjEntity ent;

	public ObjEntityTst(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		ent = new ObjEntity();
	}

	public void testClassName() throws Exception {
		String tstName = "tst_name";
		ent.setClassName(tstName);
		assertEquals(tstName, ent.getClassName());
	}

	public void testSuperClassName() throws Exception {
		String tstName = "super_tst_name";
		ent.setSuperClassName(tstName);
		assertEquals(tstName, ent.getSuperClassName());
	}
	
	public void testAttributeForDbAttribute() throws Exception {
		ObjEntity ae =
			getDomain().getEntityResolver().lookupObjEntity("Artist");
		DbEntity dae = ae.getDbEntity();

		assertNull(
			ae.getAttributeForDbAttribute(
				(DbAttribute) dae.getAttribute("ARTIST_ID")));
		assertNotNull(
			ae.getAttributeForDbAttribute(
				(DbAttribute) dae.getAttribute("ARTIST_NAME")));
	}

	public void testRelationshipForDbRelationship() throws Exception {
		ObjEntity ae =
			getDomain().getEntityResolver().lookupObjEntity("Artist");
		DbEntity dae = ae.getDbEntity();

		assertNull(ae.getRelationshipForDbRelationship(new DbRelationship()));
		assertNotNull(
			ae.getRelationshipForDbRelationship(
				(DbRelationship) dae.getRelationship("paintingArray")));
	}

	public void testObjectIdFromSnapshot() throws Exception {
		Class entityClass=Number.class;
		DbAttribute at = new DbAttribute();
		at.setName("xyz");
		at.setPrimaryKey(true);
		DbEntity dbe = new DbEntity("123");
		dbe.addAttribute(at);
		ent.setDbEntity(dbe);
		ent.setName("456");
		ent.setClassName(entityClass.getName());

		// test same id created by different methods
		Map map = new HashMap();
		map.put(at.getName(), "123");

		Map map2 = new HashMap();
		map2.put(at.getName(), "123");

		ObjectId ref = new ObjectId(entityClass, map);
		ObjectId oid = ent.objectIdFromSnapshot(map2);

		assertEquals(ref, oid);
	}
	
	public void testReadOnly() throws Exception {
		assertTrue(!ent.isReadOnly());
		ent.setReadOnly(true);
		assertTrue(ent.isReadOnly());
	}
}
