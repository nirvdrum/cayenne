/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * @author Andrei Adamchik
 */
public class SnapshotManagerTst extends DataContextTestBase {
    public void testMerge() throws Exception {
        String n1 = "changed";
        String n2 = "changed again";

        Artist a1 = fetchArtist("artist1", false);
        a1.setArtistName(n1);

        Map s2 = new HashMap();
        s2.put("ARTIST_NAME", n2);
        s2.put("DATE_OF_BIRTH", new java.util.Date());
        ObjEntity e = context.getEntityResolver().lookupObjEntity(a1);
        SnapshotManager.mergeObjectWithSnapshot(e, a1, s2);

        // name was modified, so it should not change during merge
        assertEquals(n1, a1.getArtistName());

        // date of birth came from database, it should be updated during merge
        assertEquals(s2.get("DATE_OF_BIRTH"), a1.getDateOfBirth());
    }

    public void testIsToOneTargetModified() throws Exception {
        ObjEntity paintingEntity =
            context.getEntityResolver().lookupObjEntity(Painting.class);
        ObjRelationship toArtist =
            (ObjRelationship) paintingEntity.getRelationship("toArtist");

        Painting painting = insertPaintingInContext("p");
        Artist artist = fetchArtist("artist1", false);
        assertNotSame(artist, painting.getToArtist());

        Map map = new HashMap();
        map.put(
            "ARTIST_ID",
            painting.getToArtist().getObjectId().getValueForAttribute("ARTIST_ID"));

        assertFalse(SnapshotManager.isToOneTargetModified(toArtist, painting, map));

        painting.setToArtist(artist);

        assertTrue(SnapshotManager.isToOneTargetModified(toArtist, painting, map));
    }

    public void testIsJoinAttributesModified() throws Exception {
        ObjEntity paintingEntity =
            context.getEntityResolver().lookupObjEntity(Painting.class);
        ObjRelationship toArtist =
            (ObjRelationship) paintingEntity.getRelationship("toArtist");

        Map stored = new HashMap();
        stored.put("ARTIST_ID", new Integer(1));

        Map nullified = new HashMap();
        nullified.put("ARTIST_ID", null);

        Map updated = new HashMap();
        updated.put("ARTIST_ID", new Integer(2));

        Map same = new HashMap();
        same.put("ARTIST_ID", new Integer(1));

        assertFalse(SnapshotManager.isJoinAttributesModified(toArtist, stored, same));

        assertTrue(SnapshotManager.isJoinAttributesModified(toArtist, stored, nullified));

        assertTrue(SnapshotManager.isJoinAttributesModified(toArtist, stored, updated));
    }

    public void testObjectIdFromSnapshot() throws Exception {
        Class entityClass = Number.class;
        ObjEntity ent = new ObjEntity();
        
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
        ObjectId oid = SnapshotManager.objectIdFromSnapshot(ent, map2);

        assertEquals(ref, oid);
    }
}
