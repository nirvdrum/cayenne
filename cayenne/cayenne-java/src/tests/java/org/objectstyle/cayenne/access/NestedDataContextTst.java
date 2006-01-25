/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.unit.CayenneTestCase;

public class NestedDataContextTst extends CayenneTestCase {

    public void testCreateChildDataContext() {
        DataContext parent = createDataContext();
        parent.setValidatingObjectsOnCommit(true);

        DataContext child1 = parent.createChildDataContext();

        assertNotNull(child1);
        assertSame(parent, child1.getChannel());
        assertTrue(child1.isValidatingObjectsOnCommit());

        parent.setValidatingObjectsOnCommit(false);

        DataContext child2 = parent.createChildDataContext();

        assertNotNull(child2);
        assertSame(parent, child2.getChannel());
        assertFalse(child2.isValidatingObjectsOnCommit());

        // second level of nesting
        DataContext child21 = child2.createChildDataContext();

        assertNotNull(child21);
        assertSame(child2, child21.getChannel());
        assertFalse(child2.isValidatingObjectsOnCommit());
    }

    // public void testLocalObjects() throws Exception {
    // deleteTestData();
    // createTestData("testArtists");
    //
    // DataContext context = createDataContext();
    // DataContext childContext = context.createChildDataContext();
    //
    // DataObject _new = context.createAndRegisterNewObject(Artist.class);
    //
    // DataObject hollow = context.registeredObject(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // 33001));
    // DataObject committed = DataObjectUtils.objectForQuery(
    // context,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // 33002)));
    //
    // int modifiedId = 33003;
    // Artist modified = (Artist) DataObjectUtils.objectForQuery(
    // context,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // modifiedId)));
    // modified.setArtistName("M1");
    // DataObject deleted = DataObjectUtils.objectForQuery(
    // context,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // 33004)));
    // context.deleteObject(deleted);
    //
    // assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
    // assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
    // assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
    // assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
    // assertEquals(PersistenceState.NEW, _new.getPersistenceState());
    //
    // // now check how objects in different state behave
    //
    // blockQueries();
    //
    // try {
    //
    // List news = childContext.localObjects(Collections.singletonList(_new));
    // assertEquals(1, news.size());
    // DataObject newPeer = (DataObject) news.get(0);
    // assertEquals(PersistenceState.COMMITTED, newPeer.getPersistenceState());
    // assertEquals(_new.getObjectId(), newPeer.getObjectId());
    // assertSame(childContext, newPeer.getDataContext());
    // assertSame(context, _new.getDataContext());
    //
    // List hollows = childContext.localObjects(Collections.singletonList(hollow));
    // assertEquals(1, hollows.size());
    // DataObject hollowPeer = (DataObject) hollows.get(0);
    // assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
    // assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
    // assertSame(childContext, hollowPeer.getDataContext());
    // assertSame(context, hollow.getDataContext());
    //
    // List commits = childContext
    // .localObjects(Collections.singletonList(committed));
    // assertEquals(1, commits.size());
    // DataObject committedPeer = (DataObject) commits.get(0);
    // assertEquals(PersistenceState.COMMITTED, committedPeer.getPersistenceState());
    // assertEquals(committed.getObjectId(), committedPeer.getObjectId());
    // assertSame(childContext, committedPeer.getDataContext());
    // assertSame(context, committed.getDataContext());
    //
    // List mods = childContext.localObjects(Collections.singletonList(modified));
    // assertEquals(1, mods.size());
    // Artist modifiedPeer = (Artist) mods.get(0);
    // assertEquals(PersistenceState.COMMITTED, modifiedPeer.getPersistenceState());
    // assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
    // assertEquals("M1", modifiedPeer.getArtistName());
    // assertSame(childContext, modifiedPeer.getDataContext());
    // assertSame(context, modified.getDataContext());
    //
    // List deletes = childContext.localObjects(Collections.singletonList(deleted));
    // assertEquals(1, deletes.size());
    // DataObject deletedPeer = (DataObject) deletes.get(0);
    // assertEquals(PersistenceState.COMMITTED, deletedPeer.getPersistenceState());
    // assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
    // assertSame(childContext, deletedPeer.getDataContext());
    // assertSame(context, deleted.getDataContext());
    // }
    // finally {
    // unblockQueries();
    // }
    // }
    //
    // public void testLocalObjectsNoOverride() throws Exception {
    // deleteTestData();
    // createTestData("testArtists");
    //
    // DataContext context = createDataContext();
    // DataContext childContext = context.createChildDataContext();
    //
    // int modifiedId = 33003;
    // Artist modified = (Artist) DataObjectUtils.objectForQuery(
    // context,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // modifiedId)));
    // Artist peerModified = (Artist) DataObjectUtils.objectForQuery(
    // childContext,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // modifiedId)));
    //
    // modified.setArtistName("M1");
    // peerModified.setArtistName("M2");
    //
    // assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
    // assertEquals(PersistenceState.MODIFIED, peerModified.getPersistenceState());
    //
    // blockQueries();
    //
    // try {
    //
    // List mods = childContext.localObjects(Collections.singletonList(modified));
    // assertEquals(1, mods.size());
    // DataObject peerModified2 = (DataObject) mods.get(0);
    // assertSame(peerModified, peerModified2);
    // assertEquals(PersistenceState.MODIFIED, peerModified2.getPersistenceState());
    // assertEquals("M2", peerModified.getArtistName());
    // assertEquals("M1", modified.getArtistName());
    // }
    // finally {
    // unblockQueries();
    // }
    // }
    //
    // public void testSelect() throws Exception {
    // deleteTestData();
    // createTestData("testSelect");
    //
    // DataContext parent = createDataContext();
    // DataContext child = parent.createChildDataContext();
    //
    // // test how different object states appear in the child on select
    //
    // DataObject _new = parent.createAndRegisterNewObject(Artist.class);
    //
    // DataObject hollow = parent.registeredObject(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // 33001));
    // DataObject committed = DataObjectUtils.objectForQuery(
    // parent,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // 33002)));
    //
    // int modifiedId = 33003;
    // Artist modified = (Artist) DataObjectUtils.objectForQuery(
    // parent,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // modifiedId)));
    // modified.setArtistName("MODDED");
    // DataObject deleted = DataObjectUtils.objectForQuery(
    // parent,
    // new SingleObjectQuery(new ObjectId(
    // "Artist",
    // Artist.ARTIST_ID_PK_COLUMN,
    // 33004)));
    // parent.deleteObject(deleted);
    //
    // assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
    // assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
    // assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
    // assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
    // assertEquals(PersistenceState.NEW, _new.getPersistenceState());
    //
    // List objects = child.performQuery(new SelectQuery(Artist.class));
    // assertEquals("All but NEW object must have been included", 4, objects.size());
    //
    // Iterator it = objects.iterator();
    // while (it.hasNext()) {
    // DataObject next = (DataObject) it.next();
    // assertEquals(PersistenceState.COMMITTED, next.getPersistenceState());
    //
    // int id = DataObjectUtils.intPKForObject(next);
    // if (id == modifiedId) {
    // assertEquals("MODDED", next.readProperty(Artist.ARTIST_NAME_PROPERTY));
    // }
    // }
    // }
    //
    // public void testReadToOneRelationship() throws Exception {
    // deleteTestData();
    // createTestData("testReadRelationship");
    //
    // DataContext parent = createDataContext();
    // DataContext child = parent.createChildDataContext();
    //
    // // test how different object states appear in the child on select
    //
    // int hollowTargetSrcId = 33001;
    // int modifiedTargetSrcId = 33002;
    // int deletedTargetSrcId = 33003;
    // int committedTargetSrcId = 33004;
    // int newTargetSrcId = 33005;
    //
    // Painting hollowTargetSrc = (Painting) DataObjectUtils.objectForPK(
    // parent,
    // Painting.class,
    // hollowTargetSrcId);
    // Artist hollowTarget = hollowTargetSrc.getToArtist();
    //
    // Painting modifiedTargetSrc = (Painting) DataObjectUtils.objectForPK(
    // parent,
    // Painting.class,
    // modifiedTargetSrcId);
    // Artist modifiedTarget = modifiedTargetSrc.getToArtist();
    // modifiedTarget.setArtistName("M1");
    //
    // Painting deletedTargetSrc = (Painting) DataObjectUtils.objectForPK(
    // parent,
    // Painting.class,
    // deletedTargetSrcId);
    // Artist deletedTarget = deletedTargetSrc.getToArtist();
    // deletedTargetSrc.setToArtist(null);
    // parent.deleteObject(deletedTarget);
    //
    // Painting committedTargetSrc = (Painting) DataObjectUtils.objectForPK(
    // parent,
    // Painting.class,
    // committedTargetSrcId);
    // Artist committedTarget = committedTargetSrc.getToArtist();
    // committedTarget.getArtistName();
    //
    // Painting newTargetSrc = (Painting) DataObjectUtils.objectForPK(
    // parent,
    // Painting.class,
    // newTargetSrcId);
    // Artist newTarget = (Artist) parent.createAndRegisterNewObject(Artist.class);
    // newTarget.setArtistName("N1");
    // newTargetSrc.setToArtist(newTarget);
    //
    // assertEquals(PersistenceState.COMMITTED, hollowTargetSrc.getPersistenceState());
    // assertEquals(PersistenceState.COMMITTED, modifiedTargetSrc.getPersistenceState());
    // assertEquals(PersistenceState.MODIFIED, deletedTargetSrc.getPersistenceState());
    // assertEquals(PersistenceState.COMMITTED, committedTargetSrc.getPersistenceState());
    // assertEquals(PersistenceState.MODIFIED, newTargetSrc.getPersistenceState());
    //
    // assertEquals(PersistenceState.HOLLOW, hollowTarget.getPersistenceState());
    // assertEquals(PersistenceState.MODIFIED, modifiedTarget.getPersistenceState());
    // assertEquals(PersistenceState.DELETED, deletedTarget.getPersistenceState());
    // assertEquals(PersistenceState.COMMITTED, committedTarget.getPersistenceState());
    // assertEquals(PersistenceState.NEW, newTarget.getPersistenceState());
    //
    // // run an ordered query, so we can address specific objects directly by index
    // SelectQuery q = new SelectQuery(Painting.class);
    // q.addOrdering(Painting.PAINTING_TITLE_PROPERTY, true);
    // List childSources = child.performQuery(q);
    // assertEquals(5, childSources.size());
    //
    // blockQueries();
    // try {
    // Artist childHollowTarget = ((Painting) childSources.get(0)).getToArtist();
    // assertEquals(PersistenceState.HOLLOW, childHollowTarget.getPersistenceState());
    //
    // Artist childModifiedTarget = ((Painting) childSources.get(1)).getToArtist();
    // assertEquals(PersistenceState.COMMITTED, childModifiedTarget
    // .getPersistenceState());
    // assertEquals("M1", childModifiedTarget.getArtistName());
    //
    // Painting childDeletedTargetSrc = (Painting) childSources.get(2);
    // // make sure we got the right object...
    // assertEquals(deletedTargetSrc.getObjectId(), childDeletedTargetSrc.getObjectId());
    // Artist childDeletedTarget = childDeletedTargetSrc.getToArtist();
    // assertNull(childDeletedTarget);
    //
    // Artist childCommittedTarget = ((Painting) childSources.get(3)).getToArtist();
    // assertEquals(PersistenceState.COMMITTED, childCommittedTarget
    // .getPersistenceState());
    //
    // Painting childNewTargetSrc = (Painting) childSources.get(4);
    // // make sure we got the right object...
    // assertEquals(newTargetSrc.getObjectId(), childNewTargetSrc.getObjectId());
    // Artist childNewTarget = childNewTargetSrc.getToArtist();
    // assertNotNull(childNewTarget);
    // assertEquals(PersistenceState.COMMITTED, childNewTarget.getPersistenceState());
    // assertEquals("N1", childNewTarget.getArtistName());
    // }
    // finally {
    // unblockQueries();
    // }
    // }
}
