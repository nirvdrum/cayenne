package org.objectstyle.cayenne;

import org.objectstyle.art.ArtGroup;

public class CDOReflexiveRelDeleteTst extends CayenneDOTestBase {
	private ArtGroup parentGroup;
	private ArtGroup childGroup1;
	private ArtGroup childGroup2;
	private ArtGroup childGroup3;
	/**
	 * Constructor for CayenneDataObjectReflexiveRelDeleteTst.
	 * @param name
	 */
	public CDOReflexiveRelDeleteTst(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		this.resetContext();

		parentGroup=(ArtGroup)ctxt.createAndRegisterNewObject("ArtGroup");
		parentGroup.setName("parent");

		childGroup1=(ArtGroup)ctxt.createAndRegisterNewObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup);
		
		childGroup2=(ArtGroup)ctxt.createAndRegisterNewObject("ArtGroup");
		childGroup2.setName("child2");
		childGroup2.setToParentGroup(parentGroup);
	
		childGroup3=(ArtGroup)ctxt.createAndRegisterNewObject("ArtGroup");
		childGroup3.setName("subchild");
		childGroup3.setToParentGroup(childGroup1);
		
		ctxt.commitChanges();
	}
	//Test various delete orders.  There are more possible literal combinations, but the ones below fairly well
	//encompass the various orders that might be a problem.  Add more if additional problems come to light
	public void testReflexiveRelationshipDelete1() {
		ctxt.deleteObject(parentGroup);
		ctxt.deleteObject(childGroup1);
		ctxt.deleteObject(childGroup2);
		ctxt.deleteObject(childGroup3);
		ctxt.commitChanges();
	}
	
	public void testReflexiveRelationshipDelete2() {
		ctxt.deleteObject(childGroup1);
		ctxt.deleteObject(parentGroup);
		ctxt.deleteObject(childGroup2);
		ctxt.deleteObject(childGroup3);
		ctxt.commitChanges();
	}
	
	public void testReflexiveRelationshipDelete3() {
		ctxt.deleteObject(childGroup1);
		ctxt.deleteObject(childGroup3);
		ctxt.deleteObject(parentGroup);
		ctxt.deleteObject(childGroup2);
		ctxt.commitChanges();
	}
	
	public void testReflexiveRelationshipDelete4() {
		ctxt.deleteObject(childGroup3);
		ctxt.deleteObject(parentGroup);
		ctxt.deleteObject(childGroup1);
		ctxt.deleteObject(childGroup2);
		ctxt.commitChanges();
	}

}
