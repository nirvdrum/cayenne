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

import org.objectstyle.cayenne.unittest.CayenneTestCase;

/** 
 * DataMap unit tests.
 * 
 * @author Andrei Adamchik 
 */
public class DataMapTst extends CayenneTestCase {
	protected DataMap map;

	public DataMapTst(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		map = new DataMap();
	}

	public void testName() throws Exception {
		String tstName = "tst_name";
		assertNull(map.getName());
		map.setName(tstName);
		assertEquals(tstName, map.getName());
	}

	public void testLocation() throws Exception {
		String tstName = "tst_name";
		assertNull(map.getLocation());
		map.setLocation(tstName);
		assertEquals(tstName, map.getLocation());
	}

	public void testAddObjEntity() throws Exception {
		ObjEntity e = new ObjEntity("b");
		e.setClassName("b");
		map.addObjEntity(e);
		assertSame(e, map.getObjEntity(e.getName()));
		assertSame(map, e.getDataMap());
	}

	public void testAddEntityWithSameName() throws Exception {
		//Give them different class-names... we are only testing for the same entity name being a problem
		ObjEntity e1 = new ObjEntity("c");
		e1.setClassName("c1");
		ObjEntity e2 = new ObjEntity("c");
		e2.setClassName("c2");
		map.addObjEntity(e1);
		try {
			map.addObjEntity(e2);
			fail("Should not be able to add more than one entity with the same name");	
		} catch (Exception e) {
		}	
	}
	
	public void testAddEntityWithSameClassName() throws Exception {
		ObjEntity e1 = new ObjEntity("d");
		ObjEntity e2 = new ObjEntity("e");
		e1.setClassName("d");
		e2.setClassName("d");
		map.addObjEntity(e1);
		try {
			map.addObjEntity(e2);
			fail("Should not be able to add more than one entity with the same class name");	
		} catch (Exception e) {
		}	
	}

	//It should be possible to cleanly remove and then add the same entity again.
	//Uncovered the need for this while testing modeller manually.
	public void testRemoveThenAddNullClassName() {
		ObjEntity e = new ObjEntity("f");
		map.addObjEntity(e);
		
		map.removeObjEntity(e.getName());
		map.addObjEntity(e);
	}
	
	public void testRemoveThenAddRealClassName() {
		ObjEntity e = new ObjEntity("f");
		e.setClassName("f");
		map.addObjEntity(e);
		
		map.removeObjEntity(e.getName());
		map.addObjEntity(e);
	}

	public void testAddDbEntity() throws Exception {
		DbEntity e = new DbEntity("b");
		map.addDbEntity(e);
		assertSame(e, map.getDbEntity(e.getName()));
		assertSame(map, e.getDataMap());
	}
	
	public void testAddDependency1() throws Exception {
		map.setName("m1");
		DataMap map2 = new DataMap("m2");
		assertTrue(!map.isDependentOn(map2));
		map.addDependency(map2);
		assertTrue(map.isDependentOn(map2));
	}
	
	public void testAddDependency2() throws Exception {
		map.setName("m1");
		DataMap map2 = new DataMap("m2");
		DataMap map3 = new DataMap("m3");
		map.addDependency(map2);
		map2.addDependency(map3);
		assertTrue(map.isDependentOn(map3));
	}
	
	
	public void testAddDependency3() throws Exception {
		map.setName("m1");
		DataMap map2 = new DataMap("m2");
		map.addDependency(map2);
		
		try {
			map2.addDependency(map);
			fail("Circular dependencies should throw exceptions.");
		}
		catch(RuntimeException ex) {
			// exception expected
		}
	}
	
	public void testAddDependency4() throws Exception {
		map.setName("m1");
		DataMap map2 = new DataMap("m2");
		map.addDependency(map2);
		DataMap map3 = new DataMap("m3");
		map2.addDependency(map3);
		
		try {
			map3.addDependency(map);
			fail("Circular dependencies should throw exceptions.");
		}
		catch(RuntimeException ex) {
			// exception expected
		}
	}
}
