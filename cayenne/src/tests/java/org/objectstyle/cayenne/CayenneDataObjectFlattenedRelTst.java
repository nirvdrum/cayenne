package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.FlattenedTest1;
import org.objectstyle.art.FlattenedTest2;
import org.objectstyle.art.FlattenedTest3;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Test case for objects with flattened relationships.
 * 
 * @author Andrei Adamchik
 */
public class CayenneDataObjectFlattenedRelTst extends CayenneDOTestBase {
    public void testToOneSeriesFlattenedRel() {
        FlattenedTest1 ft1 =
            (FlattenedTest1) ctxt.createAndRegisterNewObject("FlattenedTest1");
        ft1.setName("FT1Name");
        FlattenedTest2 ft2 =
            (FlattenedTest2) ctxt.createAndRegisterNewObject("FlattenedTest2");
        ft2.setName("FT2Name");
        FlattenedTest3 ft3 =
            (FlattenedTest3) ctxt.createAndRegisterNewObject("FlattenedTest3");
        ft3.setName("FT3Name");

        ft2.setToFT1(ft1);
        ft2.addToFt3Array(ft3);
        ctxt.commitChanges();

		ctxt = createDataContext(); //We need a new context
        SelectQuery q = new SelectQuery(FlattenedTest3.class);
        q.setQualifier(ExpressionFactory.matchExp("name", "FT3Name"));
        List results = ctxt.performQuery(q);

        assertEquals(1, results.size());

        FlattenedTest3 fetchedFT3 = (FlattenedTest3) results.get(0);
        FlattenedTest1 fetchedFT1 = fetchedFT3.getToFT1();
        assertEquals("FT1Name", fetchedFT1.getName());
    }

    public void testReadFlattenedRelationship() throws Exception {
        //Test no groups
        TestCaseDataFactory.createArtistBelongingToGroups(artistName, new String[] {
        });

        Artist a1 = fetchArtist();
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(0, groupList.size());
    }

    public void testReadFlattenedRelationship2() throws Exception {
        //Test no groups
        TestCaseDataFactory.createArtistBelongingToGroups(
            artistName,
            new String[] { groupName });

        Artist a1 = fetchArtist();
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(1, groupList.size());
        assertEquals(
            PersistenceState.COMMITTED,
            ((ArtGroup) groupList.get(0)).getPersistenceState());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());
    }

    public void testAddToFlattenedRelationship() throws Exception {
        TestCaseDataFactory.createArtist(artistName);
        TestCaseDataFactory.createUnconnectedGroup(groupName);

        Artist a1 = fetchArtist();
        assertEquals(0, a1.getGroupArray().size());

        SelectQuery q =
            new SelectQuery(
                ArtGroup.class,
                ExpressionFactory.matchExp("name", groupName));
        List results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        assertFalse(ctxt.hasChanges());
        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        assertTrue(ctxt.hasChanges());

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());

        //Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        //and check again
        assertFalse(ctxt.hasChanges());

        // refetch artist with a different context
		ctxt = createDataContext();
        a1 = fetchArtist();
        groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());
    }

    //Test case to show up a bug in committing more than once
    public void testDoubleCommitAddToFlattenedRelationship() throws Exception {
        TestCaseDataFactory.createArtistBelongingToGroups(artistName, new String[] {
        });
        TestCaseDataFactory.createUnconnectedGroup(groupName);
        Artist a1 = fetchArtist();

        SelectQuery q =
            new SelectQuery(
                ArtGroup.class,
                ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "name", groupName));
        List results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());

        //Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        try {
            //The bug caused the second commit to fail (the link record
            // was inserted again)
            a1.getDataContext().commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }

    }

    public void testRemoveFromFlattenedRelationship() throws Exception {
        TestCaseDataFactory.createArtistBelongingToGroups(
            artistName,
            new String[] { groupName });
        Artist a1 = fetchArtist();

        ArtGroup group = (ArtGroup) a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());

        //Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        //and check again
        groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());
    }

    //Shows up a possible bug in ordering of deletes, when a flattened relationships link record is deleted
    // at the same time (same transaction) as one of the record to which it links.
    public void testRemoveFlattenedRelationshipAndRootRecord() throws Exception {
        TestCaseDataFactory.createArtistBelongingToGroups(
            artistName,
            new String[] { groupName });
        Artist a1 = fetchArtist();
        DataContext dc = a1.getDataContext();

        ArtGroup group = (ArtGroup) a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group); //Cause the delete of the link record

        dc.deleteObject(a1); //Cause the deletion of the artist

        try {
            dc.commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown the exception :" + e.getMessage());
        }
    }

    /* Catches a bug in the flattened relationship registration which just inserted/deleted willy-nilly, 
     * even if unneccessary */
    public void testAddRemoveAddFlattenedRelationship() throws Exception {
        String specialGroupName = "Special Group2";
        TestCaseDataFactory.createArtistBelongingToGroups(artistName, new String[] {
        });
        TestCaseDataFactory.createUnconnectedGroup(specialGroupName);
        Artist a1 = fetchArtist();

        SelectQuery q =
            new SelectQuery(
                ArtGroup.class,
                ExpressionFactory.binaryPathExp(
                    Expression.EQUAL_TO,
                    "name",
                    specialGroupName));
        List results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        group.removeFromArtistArray(a1);
        //a1.addToGroupArray(group);

        try {
            ctxt.commitChanges();
        }
        catch (Exception e) {
            Util.unwindException(e).printStackTrace();
            fail("Should not have thrown the exception " + e.getMessage());
        }

		ctxt = createDataContext();
        results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        group = (ArtGroup) results.get(0);
        assertEquals(0, group.getArtistArray().size());
        //a1 = fetchArtist();
        //assertTrue(group.getArtistArray().contains(a1));
    }
}
