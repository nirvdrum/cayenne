/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.CayenneMap;

/**
 * Stores a collection of related mapping objects that describe database and object layers
 * of an application. DataMap contains DbEntities mapping database tables, ObjEntities -
 * mapping persistent Java classes, Procedures - mapping database stored procedures.
 *
 * @author Michael Shengaout
 * @author Andrei Adamchik  
 * @author Craig Miskell
 */
public class DataMap {
    private static Logger logObj = Logger.getLogger(DataMap.class);

    protected String name;
    protected String location;

    // ====================================================
    // DataMaps that provide dependencies for this DataMap
    // ====================================================
    private List dependencies = new ArrayList();

    // read-through reference for public access
    private List dependenciesRef = Collections.unmodifiableList(dependencies);

    // ====================================================
    // ObjEntities
    // ====================================================
    private CayenneMap objEntityMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap objEntityMapRef = Collections.unmodifiableSortedMap(objEntityMap);

    // read-through reference for public access to the ObjEntities
    private Collection objEntityValuesRef =
        Collections.unmodifiableCollection(objEntityMap.values());

    // ====================================================
    // DbEntities
    // ====================================================
    private CayenneMap dbEntityMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap dbEntityMapRef = Collections.unmodifiableSortedMap(dbEntityMap);

    // read-through reference for public access
    private Collection dbEntityValuesRef =
        Collections.unmodifiableCollection(dbEntityMap.values());

    // ====================================================
    // Procedures
    // ====================================================
    private CayenneMap procedureMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap procedureMapRef = Collections.unmodifiableSortedMap(procedureMap);

    // read-through reference for public access
    private Collection procedureValuesRef =
        Collections.unmodifiableCollection(procedureMap.values());

    // ====================================================
    // Queries
    // ====================================================
    protected CayenneMap queries = new CayenneMap(this);
    
    // read-through reference for public access
     private SortedMap queryMapRef = Collections.unmodifiableSortedMap(queries);
    
    /** 
     * Creates an new unnamed DataMap. 
     */
    public DataMap() {
    }

    /**
     * Creates an empty DataMap and assigns it a <code>name</code>.
     */
    public DataMap(String mapName) {
        this.setName(mapName);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("DataMap '").append(name).append("'");
        return buf.toString();
    }

    /**
     * Adds a data map that has entities used by this map.
     * 
     * @throws IllegalArgumentException if a <code>map</code> argument
     * already depends on thsi map directly or indirectly.
     */
    public void addDependency(DataMap map) throws IllegalArgumentException {
        if (map.isDependentOn(this)) {
            StringBuffer buf = new StringBuffer(128);
            buf
                .append("Attempt to create circular dependency. ")
                .append('\'')
                .append(map.getName())
                .append("' already depends on '")
                .append(this.getName())
                .append("'.");
            throw new IllegalArgumentException(buf.toString());
        }
        dependencies.add(map);
        logObj.debug("added dependency: '" + map.getName() + "'");
    }

    public void removeDependency(DataMap map) {
        dependencies.remove(map);
        logObj.debug("removed dependency: '" + map.getName() + "'");
    }

    public List getDependencies() {
        return dependenciesRef;
    }

    /**
     * Returns <code>true</code> if this map
     * depends on DataMap supplied as a <code>map</code>
     * parameter. Checks for nested dependencies as well.
     * For instance if the following dependency exists:
     * map1 -> map2 -> map3, calling <code>map1.isDependentOn(map3)</code>
     * will return <code>true</code>.
     */
    public boolean isDependentOn(DataMap map) {
        if (dependencies.size() == 0) {
            return false;
        }

        if (dependencies.contains(map)) {
            return true;
        }

        Iterator it = dependencies.iterator();
        while (it.hasNext()) {
            // check dependencies recursively
            DataMap dep = (DataMap) it.next();
            if (dep.isDependentOn(map)) {
                return true;
            }
        }

        return false;
    }

    public void clearDependencies() {
        dependencies.clear();
    }

    /** 
     * Returns "name" property value. 
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name != null ? name : "unnamed");
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
        Iterator dbs = new ArrayList(map.getDbEntities()).iterator();
        while (dbs.hasNext()) {
            DbEntity ent = (DbEntity) dbs.next();
            this.removeDbEntity(ent.getName());
            this.addDbEntity(ent);
        }

        Iterator objs = new ArrayList(map.getObjEntities()).iterator();
        while (objs.hasNext()) {
            ObjEntity ent = (ObjEntity) objs.next();
            this.removeObjEntity(ent.getName());
            this.addObjEntity(ent);
        }
    }

    /**
     * Returns "location" property value. Location is abstract and can depend
     * on how the DataMap was loaded. E.g. location can be a File on the filesystem
     * or a location within a JAR.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets "location" property.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns a sorted unmodifiable map of ObjEntities
     * contained in this DataMap, keyed by ObjEntity name.
     */
    public SortedMap getObjEntityMap() {
        return objEntityMapRef;
    }

    /**
     * Returns a sorted unmodifiable map of DbEntities
     * contained in this DataMap, keyed by DbEntity name.
     */
    public SortedMap getDbEntityMap() {
        return dbEntityMapRef;
    }
    
    
    /**
     * Returns a named query associated with this DataMap.
     * 
     * @since 1.1
     */
    public Query getQuery(String queryName) {
        return (Query) queries.get(queryName);
    }

    /**
     * Stores a query under its name.
     * 
     * @since 1.1
     */
    public void addQuery(Query query) {
        if (query == null) {
            throw new NullPointerException("Can't add null query.");
        }

        if (query.getName() == null) {
            throw new NullPointerException("Query name can't be null.");
        }

        queries.put(query.getName(), query);
    }

    /**
     * Removes a named query from the DataMap.
     * 
     * @since 1.1
     */
    public void removeQuery(String queryName) {
        queries.remove(queryName);
    }

    /**
     * @since 1.1
     */
    public void clearQueries() {
        queries.clear();
    }
    
    /**
     * @since 1.1
     */
    public SortedMap getQueryMap() {
        return queryMapRef;
    }

    /** 
     * Adds a new ObjEntity to this DataMap.
     */
    public void addObjEntity(ObjEntity objEntity) {
        if (objEntity.getName() == null) {
            throw new NullPointerException("Attempt to add ObjEntity with no name.");
        }

        objEntityMap.put(objEntity.getName(), objEntity);
    }

    /** 
     * Adds a new DbEntity to this DataMap.
     */
    public void addDbEntity(DbEntity dbEntity) {
        if (dbEntity.getName() == null) {
            throw new NullPointerException("Attempt to add DbEntity with no name.");
        }

        dbEntityMap.put(dbEntity.getName(), dbEntity);
    }

    /**
     * Returns a list of ObjEntities stored in this DataMap.
     */
    public Collection getObjEntities() {
        return this.getObjEntities(false);
    }

    /**
     * Returns all ObjEntities in this DataMap, including entities
     * from dependent maps if <code>includeDeps</code> is <code>true</code>.
     */
    public Collection getObjEntities(boolean includeDeps) {
        if (!includeDeps || this.getDependencies().isEmpty()) {
            return objEntityValuesRef;
        }
        else {
            // create a copy until we can start to cache allObjEnts as well
            List allObjEnts = new ArrayList(objEntityValuesRef);
            Iterator dependentMaps = this.getDependencies().iterator();
            while (dependentMaps.hasNext()) {
                DataMap depMap = (DataMap) dependentMaps.next();
                allObjEnts.addAll(depMap.getObjEntities());
            }
            return allObjEnts;
        }
    }

    /**
     * Returns all DbEntities in this DataMap.
     */
    public Collection getDbEntities() {
        return this.getDbEntities(false);
    }

    /**
     * Returns all DbEntities in this DataMap, including entities
     * from dependent maps if <code>includeDeps</code> is <code>true</code>.
     */
    public Collection getDbEntities(boolean includeDeps) {
        if (!includeDeps || getDependencies().isEmpty()) {
            return dbEntityValuesRef;
        }
        else {
            // create a copy until we can start to cache allDbEnts as well
            List allDbEnts = new ArrayList(dbEntityValuesRef);
            Iterator dependentMaps = this.getDependencies().iterator();
            while (dependentMaps.hasNext()) {
                DataMap depMap = (DataMap) dependentMaps.next();
                allDbEnts.addAll(depMap.getDbEntities());
            }
            return allDbEnts;
        }
    }

    /**
     * Returns a list of DbEntity names.
     */
    public List getDbEntityNames(boolean includeDeps) {
        List ents = new ArrayList(dbEntityMap.keySet());

        if (includeDeps) {
            Iterator it = this.getDependencies().iterator();
            while (it.hasNext()) {
                DataMap dep = (DataMap) it.next();
                // using "false" to avoid problems with circular dependencies
                ents.addAll(dep.getDbEntityNames(false));
            }
        }
        return ents;
    }

    /** 
     * Returns DbEntity matching the <code>name</code> parameter. No
     * dependencies will be searched.
     */
    public DbEntity getDbEntity(String dbEntityName) {
        return this.getDbEntity(dbEntityName, false);
    }

    public DbEntity getDbEntity(String dbEntityName, boolean searchDependencies) {
        DbEntity ent = (DbEntity) dbEntityMap.get(dbEntityName);
        if (ent != null || !searchDependencies) {
            return ent;
        }

        Iterator it = this.getDependencies().iterator();
        while (it.hasNext()) {
            DataMap dep = (DataMap) it.next();
            // using "false" to avoid problems with circular dependencies
            DbEntity e = dep.getDbEntity(dbEntityName, false);
            if (e != null) {
                return e;
            }
        }
        return null;
    }
    
    /**
     * Returns an ObjEntity for a DataObject class name.
     * 
     * @since 1.1
     */
    public ObjEntity getObjEntityForJavaClass(String javaClassName) {
        if(javaClassName == null) {
            return null;
        }
        
        Iterator it = getObjEntityMap().entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjEntity entity = (ObjEntity) entry.getValue();
            if(javaClassName.equals(entity.getClassName())) {
                return entity;
            }
        }
        
        return null;
    }

    /**
     * Returns an ObjEntity for a given name.
     */
    public ObjEntity getObjEntity(String objEntityName) {
        return (ObjEntity) objEntityMap.get(objEntityName);
    }

    public ObjEntity getObjEntity(String objEntityName, boolean searchDependencies) {
        ObjEntity ent = (ObjEntity) objEntityMap.get(objEntityName);
        if (ent != null || !searchDependencies) {
            return ent;
        }

        Iterator it = this.getDependencies().iterator();
        while (it.hasNext()) {
            DataMap dep = (DataMap) it.next();
            // using "false" to avoid problems with circular dependencies
            ObjEntity e = dep.getObjEntity(objEntityName, false);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** 
     * Returns a list of ObjEntities mapped to the given DbEntity.
     */
    public Collection getMappedEntities(DbEntity dbEntity) {
        if (dbEntity == null) {
            return Collections.EMPTY_LIST;
        }

        List result = new ArrayList();
        Iterator iter = this.getObjEntities(true).iterator();
        while (iter.hasNext()) {
            ObjEntity objEnt = (ObjEntity) iter.next();
            if (objEnt.getDbEntity() == dbEntity) {
                result.add(objEnt);
            }
        }

        return result;
    }

    /** 
     * Removes DbEntity, also removing all DbRelationships and ObjRelationships
     * that reference it. This method is a "clean" remove.
     * 
     * @deprecated Since 1.1 use {@link #removeDbEntity(String, boolean)}
     */
    public void deleteDbEntity(String dbEntityName) {
        removeDbEntity(dbEntityName, true);
    }

    /** 
     * "Dirty" remove of the DbEntity from the data map.
     * 
     * @deprecated Since 1.1 use {@link #removeDbEntity(String, boolean)}
     */
    public void removeDbEntity(String dbEntityName) {
        removeDbEntity(dbEntityName, false);
    }

    /** 
     * Removes DbEntity from the DataMap. If <code>clearDependencies</code> is true,
     * all DbRelationships that reference this entity are also removed. ObjEntities
     * that rely on this entity are cleaned up.
     * 
     * @since 1.1
     */
    public void removeDbEntity(String dbEntityName, boolean clearDependencies) {
        DbEntity dbEntityToDelete = (DbEntity) dbEntityMap.remove(dbEntityName);

        if (dbEntityToDelete != null && clearDependencies) {
            Iterator dbEnts = this.getDbEntities().iterator();
            while (dbEnts.hasNext()) {
                DbEntity dbEnt = (DbEntity) dbEnts.next();
                // take a copy since we're going to modifiy the entity
                Iterator rels = new ArrayList(dbEnt.getRelationships()).iterator();
                while (rels.hasNext()) {
                    DbRelationship rel = (DbRelationship) rels.next();
                    if (dbEntityName.equals(rel.getTargetEntityName())) {
                        dbEnt.removeRelationship(rel.getName());
                    }
                }
            }

            // Remove all obj relationships referencing removed DbRelationships.
            Iterator objEnts = this.getObjEntities().iterator();
            while (objEnts.hasNext()) {
                ObjEntity objEnt = (ObjEntity) objEnts.next();
                if (objEnt.getDbEntity() == dbEntityToDelete) {
                    objEnt.clearDbMapping();
                }
                else {
                    Iterator iter = objEnt.getRelationships().iterator();
                    while (iter.hasNext()) {
                        ObjRelationship rel = (ObjRelationship) iter.next();
                        Iterator dbRels = rel.getDbRelationships().iterator();
                        while (dbRels.hasNext()) {
                            DbRelationship dbRel = (DbRelationship) dbRels.next();
                            if (dbRel.getTargetEntity() == dbEntityToDelete) {
                                rel.clearDbRelationships();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /** 
     * Removes ObjEntity, also removing all ObjRelationships that reference it.
     * This method is a "clean" remove.
     * 
     * @deprecated Since 1.1 use {@link #removeObjEntity(String, boolean)}
     */
    public void deleteObjEntity(String objEntityName) {
        removeObjEntity(objEntityName, true);
    }

    /** 
     * "Dirty" remove of the ObjEntity from the data map.
     * 
     * @deprecated Since 1.1 use {@link #removeObjEntity(String, boolean)}
     */
    public void removeObjEntity(String objEntityName) {
        removeObjEntity(objEntityName, false);
    }

    /** 
     * Removes ObjEntity from the DataMap. If <code>clearDependencies</code> is true,
     * all ObjRelationships that reference this entity are also removed.
     * 
     * @since 1.1
     */
    public void removeObjEntity(String objEntityName, boolean clearDependencies) {
        ObjEntity entity = (ObjEntity) objEntityMap.remove(objEntityName);

        if (entity != null && clearDependencies) {
            Iterator entities = this.getObjEntityMap().values().iterator();
            while (entities.hasNext()) {
                ObjEntity ent = (ObjEntity) entities.next();
                // take a copy since we're going to modifiy the entity
                Iterator rels = new ArrayList(ent.getRelationships()).iterator();
                while (rels.hasNext()) {
                    ObjRelationship rel = (ObjRelationship) rels.next();
                    if (objEntityName.equals(rel.getTargetEntityName())
                        || objEntityName.equals(rel.getTargetEntityName())) {
                        ent.removeRelationship(rel.getName());
                    }
                }
            }
        }
    }

    /**
     * Returns stored procedures associated with this DataMap.
     */
    public Collection getProcedures() {
        return getProcedures(false);
    }

    /**
     * Returns stored procedures associated with this DataMap, including procedures from 
     * dependent maps if requested.
     */
    public Collection getProcedures(boolean includeDeps) {
        if (!includeDeps || this.getDependencies().isEmpty()) {
            return procedureValuesRef;
        }
        else {
            // create a copy until we can start to cache allDbEnts as well
            List allProcedures = new ArrayList(procedureValuesRef);
            Iterator dependentMaps = this.getDependencies().iterator();
            while (dependentMaps.hasNext()) {
                DataMap depMap = (DataMap) dependentMaps.next();
                allProcedures.addAll(depMap.getProcedures());
            }
            return allProcedures;
        }
    }

    /**
     * Returns a named stored procedure or null if no such procedure exists in the map.
     */
    public Procedure getProcedure(String name) {
        return getProcedure(name, false);
    }

    /** 
     * Adds stored procedure to the list of procedures.
     * If there is another procedure registered under the same name,
     * throws an IllegalArgumentException.
     */
    public void addProcedure(Procedure procedure) {
        if (procedure.getName() == null) {
            throw new NullPointerException("Attempt to add procedure with no name.");
        }

        procedureMap.put(procedure.getName(), procedure);
    }

    public void removeProcedure(String name) {
        procedureMap.remove(name);
    }

    /**
     * Returns a named stored procedure or null if no such procedure exists in the map,
     * searching dependent maps if requested.
     */
    public Procedure getProcedure(String name, boolean searchDependencies) {
        Procedure procedure = (Procedure) procedureMap.get(name);
        if (procedure != null || !searchDependencies) {
            return procedure;
        }

        Iterator it = this.getDependencies().iterator();
        while (it.hasNext()) {
            DataMap dep = (DataMap) it.next();
            // using "false" to avoid problems with circular dependencies
            procedure = dep.getProcedure(name, false);
            if (procedure != null) {
                return procedure;
            }
        }
        return null;
    }

    /**
     * Returns a sorted unmodifiable map of Procedures 
     * in this DataMap keyed by name.
     */
    public SortedMap getProcedureMap() {
        return procedureMapRef;
    }
}