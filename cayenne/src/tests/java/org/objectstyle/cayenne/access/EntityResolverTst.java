package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class EntityResolverTst extends CayenneTestCase {
	protected EntityResolver resolver;
	private List maps=new ArrayList();
	
	public EntityResolverTst(String name) {
		super(name);
		maps.addAll(getSharedDomain().getMapList());
	}
	
    protected void setUp() throws java.lang.Exception {
        resolver = new EntityResolver(maps);
    }
    
    ////Private conveniences

	private DbEntity getArtistDbEntity() {
		return getSharedDomain().getMapForDbEntity("ARTIST").getDbEntity("ARTIST");
	}
	
	private ObjEntity getArtistObjEntity() {
		return getSharedDomain().getMapForObjEntity("Artist").getObjEntity("Artist");
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
	
	public void testLookupDbEntityByEntityName() {
		assertIsArtistDbEntity(resolver.lookupDbEntity("Artist"));
	}
	
	public void testLookupDbEntityByObjEntity() {
		assertIsArtistDbEntity(resolver.lookupDbEntity(getArtistObjEntity()));
	}
	
	public void testLookupDbEntityByClass() {
		assertIsArtistDbEntity(resolver.lookupDbEntity(Artist.class));
	}
	
	////Test ObjEntity lookups
	
	public void testLookupObjEntityByEntityName() {
		assertIsArtistObjEntity(resolver.lookupObjEntity("Artist"));
	}
		
	public void testLookupObjEntityByClass() {
		assertIsArtistObjEntity(resolver.lookupObjEntity(Artist.class));
	}
}

