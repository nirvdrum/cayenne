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
package org.objectstyle.cayenne.access;

import java.util.Date;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.DeleteRuleTest1;
import org.objectstyle.art.DeleteRuleTest2;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.PaintingInfo;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;

/**
 * 
 * @author Craig Miskell
 */
public class DataContextDeleteRulesTst extends CayenneTestCase {
	private DataContext context;

	/**
	 * Constructor for DataContextDeleteRulesTst.
	 * @param name
	 */
	public DataContextDeleteRulesTst(String name) {
		super(name);
	}

	public void setUp() throws java.lang.Exception {
		CayenneTestDatabaseSetup setup = getDatabaseSetup();
		setup.cleanTableData();

		DataDomain dom = getDomain();
		setup.createPkSupportForMapEntities(dom.getDataNodes()[0]);

		context = dom.createDataContext();
	}

	public void testNullifyToOne() {
		//ArtGroup toParentGroup
		ArtGroup parentGroup =
			(ArtGroup) context.createAndRegisterNewObject("ArtGroup");
		parentGroup.setName("Parent");

		ArtGroup childGroup =
			(ArtGroup) context.createAndRegisterNewObject("ArtGroup");
		childGroup.setName("Child");
		parentGroup.addToChildGroupsArray(childGroup);

		//Check to make sure that the relationships are both exactly correct
		// before starting.  We're not really testing this, but it is imperative
		// that it is correct before testing the real details.
		assertEquals(parentGroup, childGroup.getToParentGroup());
		assertTrue(parentGroup.getChildGroupsArray().contains(childGroup));

		//Always good to commit before deleting... bad things happen otherwise
		context.commitChanges();

		context.deleteObject(childGroup);

		//The things we are testing.
		assertTrue(!parentGroup.getChildGroupsArray().contains(childGroup));
		//Although deleted, the property should be null (good cleanup policy)
		//assertNull(childGroup.getToParentGroup());

		//And be sure that the commit works afterwards, just for sanity
		context.commitChanges();
	}

	public void testNullifyToManyFlattened() {
		//ArtGroup artistArray
		ArtGroup aGroup =
			(ArtGroup) context.createAndRegisterNewObject("ArtGroup");
		aGroup.setName("Group Name");
		Artist anArtist = (Artist) context.createAndRegisterNewObject("Artist");
		anArtist.setArtistName("A Name");
		aGroup.addToArtistArray(anArtist);

		//Preconditions - good to check to be sure
		assertTrue(aGroup.getArtistArray().contains(anArtist));
		assertTrue(anArtist.getGroupArray().contains(aGroup));
		context.commitChanges();

		context.deleteObject(aGroup);

		//The things to test
		assertTrue(!anArtist.getGroupArray().contains(aGroup));
		//Although the group is deleted, the array should still be 
		//cleaned up correctly
		//assertTrue(!aGroup.getArtistArray().contains(anArtist));
		context.commitChanges();

	}

	public void testNullifyToManyNonFlattened() {
		//ArtGroup childGroupsArray
		ArtGroup parentGroup =
			(ArtGroup) context.createAndRegisterNewObject("ArtGroup");
		parentGroup.setName("Parent");

		ArtGroup childGroup =
			(ArtGroup) context.createAndRegisterNewObject("ArtGroup");
		childGroup.setName("Child");
		parentGroup.addToChildGroupsArray(childGroup);

		//Preconditions - good to check to be sure
		assertEquals(parentGroup, childGroup.getToParentGroup());
		assertTrue(parentGroup.getChildGroupsArray().contains(childGroup));

		context.commitChanges();
		context.deleteObject(parentGroup);

		//The things we are testing.
		assertNull(childGroup.getToParentGroup());

		//Although deleted, the property should be null (good cleanup policy)
		//assertTrue(!parentGroup.getChildGroupsArray().contains(childGroup));
		context.commitChanges();
	}

	public void testCascadeToOne() {
		//Painting toPaintingInfo
		Painting painting =
			(Painting) context.createAndRegisterNewObject("Painting");
		painting.setPaintingTitle("A Title");

		PaintingInfo info =
			(PaintingInfo) context.createAndRegisterNewObject("PaintingInfo");
		painting.setToPaintingInfo(info);

		//Must commit before deleting.. this relationship is dependent,
		// and everything must be committed for certain things to work.
		context.commitChanges();

		context.deleteObject(painting);

		//info must also be deleted
		assertEquals(PersistenceState.DELETED, info.getPersistenceState());
		assertNull(info.getPainting());
		assertNull(painting.getToPaintingInfo());
		context.commitChanges();
	}

	public void testCascadeToMany() {
		//Artist artistExhibitArray
		Artist anArtist = (Artist) context.createAndRegisterNewObject("Artist");
		anArtist.setArtistName("A Name");
		Exhibit anExhibit =
			(Exhibit) context.createAndRegisterNewObject("Exhibit");
		anExhibit.setClosingDate(new Date());
		anExhibit.setOpeningDate(new Date());
		
		//Needs a gallery... required for data integrity
		Gallery gallery =
			(Gallery) context.createAndRegisterNewObject("Gallery");
		gallery.setGalleryName("A Name");
		
		anExhibit.setToGallery(gallery);
		
		ArtistExhibit artistExhibit =
			(ArtistExhibit) context.createAndRegisterNewObject("ArtistExhibit");

		artistExhibit.setToArtist(anArtist);
		artistExhibit.setToExhibit(anExhibit);
		context.commitChanges();

		context.deleteObject(anArtist);

		//Test that the link record was deleted, and removed from the relationship
		assertEquals(
			PersistenceState.DELETED,
			artistExhibit.getPersistenceState());
		assertTrue(!anArtist.getArtistExhibitArray().contains(artistExhibit));
		context.commitChanges();
	}

	public void testDenyToOne() {
		//DeleteRuleTest1 test2
		DeleteRuleTest1 test1 =
			(DeleteRuleTest1) context.createAndRegisterNewObject(
				"DeleteRuleTest1");
		DeleteRuleTest2 test2 =
			(DeleteRuleTest2) context.createAndRegisterNewObject(
				"DeleteRuleTest2");
		test1.setTest2(test2);
		context.commitChanges();

		try {
			context.deleteObject(test1);
			fail("Should have thrown an exception");
		} catch (Exception e) {
			//GOOD!
		}
		context.commitChanges();

	}

	public void testDenyToMany() {
		//Gallery paintingArray
		Gallery gallery =
			(Gallery) context.createAndRegisterNewObject("Gallery");
		gallery.setGalleryName("A Name");
		Painting painting =
			(Painting) context.createAndRegisterNewObject("Painting");
		painting.setPaintingTitle("A Title");
		gallery.addToPaintingArray(painting);
		context.commitChanges();

		try {
			context.deleteObject(gallery);
			fail("Should have thrown an exception");
		} catch (Exception e) {
			//GOOD!
		}
		context.commitChanges();
	}

}
