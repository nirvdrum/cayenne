package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class EntityResolverTst extends CayenneTestCase {
    protected EntityResolver sharedResolver;
    private List allMaps = new ArrayList();

    public EntityResolverTst(String name) {
        super(name);
        allMaps.addAll(getDomain().getMapList());
    }

    protected void setUp() throws Exception {
        sharedResolver = new EntityResolver(allMaps);
    }

    ////Private conveniences

    private DbEntity getArtistDbEntity() {
        return getDomain().getMapForDbEntity("ARTIST").getDbEntity("ARTIST");
    }

    private ObjEntity getArtistObjEntity() {
        return getDomain().getMapForObjEntity("Artist").getObjEntity("Artist");
    }

    private void assertIsArtistDbEntity(DbEntity ae) {
        assertNotNull(ae);
        assertEquals(ae, this.getArtistDbEntity());
    }

    private void assertIsArtistObjEntity(ObjEntity ae) {
        assertNotNull(ae);
        assertEquals(ae, this.getArtistObjEntity());
    }

    ////Test DbEntitylookups

    public void testLookupDbEntityByEntityName() throws Exception {
        assertIsArtistDbEntity(sharedResolver.lookupDbEntity("Artist"));
    }

    public void testLookupDbEntityByObjEntity() throws Exception {
        assertIsArtistDbEntity(
            sharedResolver.lookupDbEntity(getArtistObjEntity()));
    }

    public void testLookupDbEntityByClass() throws Exception {
        assertIsArtistDbEntity(sharedResolver.lookupDbEntity(Artist.class));
    }

    public void testLookupDbEntityByDataobject() throws Exception {
        Artist artist =
            (Artist) this.createDataContext().createAndRegisterNewObject(
                "Artist");
        assertIsArtistDbEntity(sharedResolver.lookupDbEntity(artist));
    }

    ////Test ObjEntity lookups

    public void testLookupObjEntityByEntityName() throws Exception {
        assertIsArtistObjEntity(sharedResolver.lookupObjEntity("Artist"));
    }

    public void testLookupObjEntityByClass() throws Exception {
        assertIsArtistObjEntity(sharedResolver.lookupObjEntity(Artist.class));
    }

    public void testLookupObjEntityByInstance() throws Exception {
        assertIsArtistObjEntity(sharedResolver.lookupObjEntity(new Artist()));
    }

    public void testLookupObjEntityByDataobject() throws Exception {
        Artist artist =
            (Artist) this.createDataContext().createAndRegisterNewObject(
                "Artist");
        assertIsArtistObjEntity(sharedResolver.lookupObjEntity(artist));
    }

    public void testGetDataMapList() throws Exception {
        DataMap m1 = new DataMap();
        DataMap m2 = new DataMap();
        List list = new ArrayList();
        list.add(m1);
        list.add(m2);

        EntityResolver resolver = new EntityResolver(list);
        List maps = resolver.getDataMapsList();
        assertNotNull(maps);
        assertEquals(2, maps.size());
        assertTrue(maps.containsAll(list));
    }

    public void testAddDataMap() throws Exception {

        // create empty resolver
        EntityResolver resolver = new EntityResolver();
        assertEquals(0, resolver.getDataMapsList().size());
        assertNull(resolver.lookupObjEntity(Object.class));

        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);

        resolver.addDataMap(m1);

        assertEquals(1, resolver.getDataMapsList().size());
        assertSame(oe1, resolver.lookupObjEntity(Object.class));
    }

    public void testRemoveDataMap() throws Exception {
        // create a resolver with a single map
        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);
        List list = new ArrayList();
        list.add(m1);
        EntityResolver resolver = new EntityResolver(list);

        assertEquals(1, resolver.getDataMapsList().size());
        assertSame(oe1, resolver.lookupObjEntity(Object.class));

        resolver.removeDataMap(m1);

        assertEquals(0, resolver.getDataMapsList().size());
        assertNull(resolver.lookupObjEntity(Object.class));
    }

    public void testAddObjEntity() throws Exception {
        // create a resolver with a single map
        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test1");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);
        List list = new ArrayList();
        list.add(m1);
        EntityResolver resolver = new EntityResolver(list);

        assertSame(oe1, resolver.lookupObjEntity(Object.class));

        ObjEntity oe2 = new ObjEntity("test2");
        oe2.setClassName(String.class.getName());
        m1.addObjEntity(oe2);

        assertSame(oe2, resolver.lookupObjEntity(String.class));
    }

    public void testLookupObjQuery() throws Exception {
        // create a resolver with a single map
        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test1");
        oe1.setClassName(Object.class.getName());
        SelectQuery q = new SelectQuery(oe1);
        oe1.addQuery("abc", q);
        m1.addObjEntity(oe1);
        List list = new ArrayList();
        list.add(m1);
        EntityResolver resolver = new EntityResolver(list);

        assertSame(q, resolver.lookupQuery(Object.class, "abc"));
    }
}
