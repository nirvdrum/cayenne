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

import java.util.*;

/**
 * Provides the entry point into the relational and object meta information.
 * In many ways, particularly for the tools responsible for creation of the
 * DataMap, plays the role of the Facade design pattern.
 *
 * @author Michael Shengaout
 * @author Andrei Adamchik  
 */
public class DataMap {
	protected String name;
	protected String location;

	/** 
	 * Contains a list of DataMaps that are used by this map.
	 */
	protected ArrayList dependencies = new ArrayList();

	/** ObjEntities representing the data object classes.
	  * The name of ObjEntity serves as a key. */
	private SortedMap objEntityMap = new TreeMap();

	/** DbEntities representing metadata for individual database tables.
	  * The name of DbEntity (which is also a database table
	  * name) serves as a key. */
	private SortedMap dbEntityMap = new TreeMap();

	/** Creates an empty DataMap */
	public DataMap() {
	}

	/** Creates an empty DataMap and assigns it a <code>name</code>. */
	public DataMap(String name) {
		this.name = name;
	}

	/**
	 * Adds a data map that has entities used by this map.
	 */
	public void addDependency(DataMap map) {
		dependencies.add(map);
	}

	public void removeDependency(DataMap map) {
		dependencies.remove(map);
	}

	public List getDependencies() {
		return Collections.unmodifiableList(dependencies);
	}

	public boolean dependsOn(DataMap map) {
		return dependencies.contains(map);
	}

	public void clearDependencies() {
		dependencies.clear();
	}

	/** Returns "name" property value. */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** 
	 * Adds all Object and DB entities from another map
	 * to this map. Overwrites all existing entities with the
	 * new ones.
	 * 
	 * <p><i>FIXME: will need to implement advanced merge
	 * that allows different policies for overwriting entities.
	 * </i></p>
	 */
	public void mergeWithDataMap(DataMap map) {
		Iterator dbs = map.getDbEntitiesAsList().iterator();
		while (dbs.hasNext()) {
			DbEntity ent = (DbEntity) dbs.next();
			this.removeDbEntity(ent.getName());
			this.addDbEntity(ent);
		}

		Iterator objs = map.getObjEntitiesAsList().iterator();
		while (objs.hasNext()) {
			ObjEntity ent = (ObjEntity) objs.next();
			this.removeObjEntity(ent.getName());
			this.addObjEntity(ent);
		}
	}

	/** Returns "location" property value.
	  * Location is abstract and treated differently
	  * by different loaders. See Configuration class
	  * for more details on how location is resolved. */
	public String getLocation() {
		return location;
	}

	/** Sets "location" property. Usually location is set by
	  * map loader when map is created from a XML file,
	  * or by a GUI tool when such a tool wants to change where 
	  * the map should be saved. */
	public void setLocation(String location) {
		this.location = location;
	}

	public SortedMap getObjEntityMap() {
		return Collections.unmodifiableSortedMap(objEntityMap);
	}

	/** Returns sorted unmodifiable map of DbEntities
	  * contained in this DataMap. Keys are DbEntity
	  * names (same as database table names). */
	public SortedMap getDbEntityMap() {
		return Collections.unmodifiableSortedMap(dbEntityMap);
	}

	/** Adds ObjEntity to the list of map entities.
	  * If there is another entity registered under the same name
	  * already, does nothing. */
	public void addObjEntity(ObjEntity objEntity) {
		String name = objEntity.getName();
		if (!objEntityMap.containsKey(name)) {
			objEntityMap.put(name, objEntity);
		}
	}

	/** Adds DbEntity to the list of map entities.
	  * If there is another entity registered under the same name
	  * already, does nothing. */
	public void addDbEntity(DbEntity dbEntity) {
		String name = dbEntity.getName();
		if (!dbEntityMap.containsKey(name))
			dbEntityMap.put(name, dbEntity);
	}

	/** Return an array of object entities (by copy) */
	public ObjEntity[] getObjEntities() {
		Collection objEnts = getObjEntityMap().values();
		ObjEntity[] immutableEnts = null;
		if (objEnts == null || objEnts.size() == 0)
			immutableEnts = new ObjEntity[0];
		else {
			immutableEnts = new ObjEntity[objEnts.size()];
			objEnts.toArray(immutableEnts);
		}
		return immutableEnts;
	}

	/** Return an array of database entities (by copy) */
	public DbEntity[] getDbEntities() {
		Collection dbEnts = getDbEntityMap().values();
		DbEntity[] immutableEnts = null;
		if (dbEnts == null || dbEnts.size() == 0)
			immutableEnts = new DbEntity[0];
		else {
			immutableEnts = new DbEntity[dbEnts.size()];
			dbEnts.toArray(immutableEnts);
		}
		return immutableEnts;
	}

	/** Returns a list of ObjEntities stored in this DataMap. */
	public List getObjEntitiesAsList() {
		return new ArrayList(objEntityMap.values());
	}

	/**
	 * Returns all DbEntities in this DataMap.
	 */
	public List getDbEntitiesAsList() {
		return getDbEntitiesAsList(false);
	}

	/**
	 * Returns all DbEntities in this DataMap, including entities
	 * from dependent maps if <code>includeDeps</code> is <code>true</code>.
	 */
	public List getDbEntitiesAsList(boolean includeDeps) {
		ArrayList ents = new ArrayList(dbEntityMap.values());
		
		if (includeDeps) {
			Iterator it = dependencies.iterator();
			while (it.hasNext()) {
				DataMap dep = (DataMap) it.next();
				// using "false" to avoid problems with circular dependencies
				ents.addAll(dep.getDbEntitiesAsList(false));
			}
		}
		return ents;
	}
	
	public List getDbEntityNames(boolean includeDeps) {
		ArrayList ents = new ArrayList(dbEntityMap.keySet());
		
		if (includeDeps) {
			Iterator it = dependencies.iterator();
			while (it.hasNext()) {
				DataMap dep = (DataMap) it.next();
				// using "false" to avoid problems with circular dependencies
				ents.addAll(dep.getDbEntityNames(false));
			}
		}
		return ents;
	}

	/** 
	 * Returnms DbEntity matching the <code>name</code> parameter.
	 * No dependencies will be searched.
	 */
	public DbEntity getDbEntity(String name) {
		return getDbEntity(name, false);
	}

	public DbEntity getDbEntity(String name, boolean searchDependencies) {
		DbEntity ent = (DbEntity) dbEntityMap.get(name);
		if (ent != null || !searchDependencies) {
			return ent;
		}

		Iterator it = dependencies.iterator();
		while (it.hasNext()) {
			DataMap dep = (DataMap) it.next();
			// using "false" to avoid problems with circular dependencies
			DbEntity e = dep.getDbEntity(name, false);
			if (e != null) {
				return e;
			}
		}
		return null;
	}

	/** Get ObjEntity by its name. */
	public ObjEntity getObjEntity(String name) {
		return (ObjEntity) objEntityMap.get(name);
	}

	/** Get the object entity mapped to the specified database table.
	*  The search is conducted in case-independent manner.
	*  @return The corresponding object entity, or null if not found.*/
	public ObjEntity getObjEntityByDbEntityName(String db_table_name) {
		Collection obj_entity_coll = objEntityMap.values();
		Iterator iter = obj_entity_coll.iterator();
		while (iter.hasNext()) {
			ObjEntity temp = (ObjEntity) iter.next();
			if (temp.getDbEntity().getName().equalsIgnoreCase(db_table_name))
				return temp;
		} // End while()
		return null;
	}

	/** Get the database entity mapped to the specified data object class */
	public DbEntity getDbEntityByObjEntityName(String data_object_class_name) {
		ObjEntity obj_temp =
			(ObjEntity) getObjEntityMap().get(data_object_class_name);
		if (null == obj_temp)
			return null;
		return obj_temp.getDbEntity();
	}

	/** Renames DbEntity. */
	public void renameDbEntity(String oldName, String newName) {
		getDbEntity(oldName).setName(newName);
	}

	/** Renames ObjEntity */
	public void renameObjEntity(String oldName, String newName) {
		getObjEntity(oldName).setName(newName);
	}

	/** "Dirty" remove of the DbEntity from the data map. */
	public void removeDbEntity(String entity_name) {
		dbEntityMap.remove(entity_name);
	}

	/** Clean remove of the DbEntity from the data map.
	 *  If there are any ObjEntities or ObjRelationship entities
	 *  referencing given entity, removes them as well. No relationships
	 *  are re-established. Also, if the ObjEntity is removed, all the
	 *  ObjRelationship-s referencing it are removed as well.
	 */
	public void deleteDbEntity(String entity_name) {
		DbEntity db_entity = getDbEntity(entity_name);
		// No db entity to remove? return.
		if (null == db_entity) {
			return;
		}
		dbEntityMap.remove(entity_name);

		DbEntity[] db_entity_arr = getDbEntities();
		for (int i = 0; i < db_entity_arr.length; i++) {
			Iterator rel_iter =
				db_entity_arr[i].getRelationshipList().iterator();
			while (rel_iter.hasNext()) {
				DbRelationship rel = (DbRelationship) rel_iter.next();
				if (rel.getTargetEntity() == db_entity)
					db_entity_arr[i].removeRelationship(rel.getName());
			}
		}

		// Remove all obj relationships referencing removed DbRelationships.
		Collection obj_entity_coll = objEntityMap.values();
		Iterator obj_entity_iter = obj_entity_coll.iterator();
		while (obj_entity_iter.hasNext()) {
			ObjEntity temp = (ObjEntity) obj_entity_iter.next();
			if (temp.getDbEntity() == db_entity) {
				temp.clearDbMapping();
			} else {
				Iterator iter = temp.getRelationshipList().iterator();
				while (iter.hasNext()) {
					ObjRelationship rel = (ObjRelationship) iter.next();
					Iterator db_rel_iter =
						rel.getDbRelationshipList().iterator();
					while (db_rel_iter.hasNext()) {
						DbRelationship db_rel =
							(DbRelationship) db_rel_iter.next();
						if (db_rel.getTargetEntity() == db_entity) {
							rel.clearDbRelationships();
							break;
						}
					}
				}
			}
		}
	}

	/** 
	 * Clean remove of the ObjEntity from the data map.
	 * Removes all ObjRelationships referencing this ObjEntity
	 */
	public void deleteObjEntity(String entity_name) {
		ObjEntity entity = (ObjEntity) objEntityMap.get(entity_name);
		if (null == entity) {
			return;
		}
		objEntityMap.remove(entity_name);
		ObjEntity[] obj_entity_arr = getObjEntities();
		for (int i = 0; i < obj_entity_arr.length; i++) {
			Iterator rel_iter =
				obj_entity_arr[i].getRelationshipList().iterator();
			while (rel_iter.hasNext()) {
				ObjRelationship rel = (ObjRelationship) rel_iter.next();
				if (rel.getTargetEntity() == entity
					|| rel.getSourceEntity() == entity) {
					obj_entity_arr[i].removeRelationship(rel.getName());
				}
			}
		}
	}

	/** "Dirty" remove of the ObjEntity from the data map.*/
	public void removeObjEntity(String entity_name) {
		objEntityMap.remove(entity_name);
	}

	//methods needed by DataMapValidator
	//there are no friend classes in Java
	//so we use methods with package visibility
	//author: Andriy Shapochka

	SortedMap getObjMap() {
		return objEntityMap;
	}

	SortedMap getDbMap() {
		return dbEntityMap;
	}
}