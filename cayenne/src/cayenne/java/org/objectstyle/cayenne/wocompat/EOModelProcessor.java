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

package org.objectstyle.cayenne.wocompat;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.parser.ASTDbPath;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.wocompat.parser.Parser;

/**
 * Class for converting stored Apple EOModel mapping files to Cayenne DataMaps.
 */
public class EOModelProcessor {

    /**
     * Returns index.eomodeld contents as a Map.
     * 
     * @since 1.1
     */
    // TODO: refactor EOModelHelper to provide a similar method without loading
    // all entity files in memory... here we simply copied stuff from EOModelHelper
    public Map loadModeIndex(String path) throws Exception {

        ResourceLocator locator = new ResourceLocator();
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(false);
        locator.setSkipHomeDirectory(true);
        locator.setSkipAbsolutePath(false);

        if (!path.endsWith(".eomodeld")) {
            path += ".eomodeld";
        }

        URL base = locator.findDirectoryResource(path);
        if (base == null) {
            throw new FileNotFoundException("Can't find EOModel: " + path);
        }

        Parser plistParser = new Parser();
        InputStream in = new URL(base, "index.eomodeld").openStream();

        try {
            plistParser.ReInit(in);
            return (Map) plistParser.propertyList();
        }
        finally {
            in.close();
        }
    }

    /**
     * Performs EOModel loading.
     * 
     * @param path A path to ".eomodeld" directory. If path doesn't end with ".eomodeld",
     *            ".eomodeld" suffix is automatically assumed.
     */
    public DataMap loadEOModel(String path) throws Exception {
        return loadEOModel(path, false);
    }

    /**
     * Performs EOModel loading.
     * 
     * @param path A path to ".eomodeld" directory. If path doesn't end with ".eomodeld",
     *            ".eomodeld" suffix is automatically assumed.
     * @param generateClientClass if true then loading of EOModel is java client classes
     *            aware and the following processing will work with Java client class
     *            settings of the EOModel.
     */
    public DataMap loadEOModel(String path, boolean generateClientClass) throws Exception {
        EOModelHelper helper = makeHelper(path, generateClientClass);

        // create empty map
        DataMap dataMap = helper.getDataMap();

        // process enitities
        Iterator it = helper.modelNames();
        while (it.hasNext()) {
            String name = (String) it.next();
            
            // skip EOPrototypes
            if(isPrototypesEntity(name)) {
                continue;
            }
            
            // create and register entity
            makeEntity(helper, name, generateClientClass);
        }
 
        List sortedModelNames = helper.modelNamesAsList();
        Collections.sort(sortedModelNames, new InheritanceComparator(dataMap));
        
        // after all entities are loaded, process attributes
        it = sortedModelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            
            // skip EOPrototypes
            if(isPrototypesEntity(name)) {
                continue;
            }
            
            EOObjEntity e = (EOObjEntity) dataMap.getObjEntity(name);
            // process entity attributes
            makeAttributes(helper, e);
        }

        // after all entities are loaded, process relationships
        it = sortedModelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            
            // skip EOPrototypes
            if(isPrototypesEntity(name)) {
                continue;
            }
            
            makeRelationships(helper, dataMap.getObjEntity(name));
        }

        // after all normal relationships are loaded, process falttened relationships
        it = sortedModelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            
            // skip EOPrototypes
            if(isPrototypesEntity(name)) {
                continue;
            }
            
            makeFlatRelationships(helper, dataMap.getObjEntity(name));
        }

        // now create missing reverse DB (but not OBJ) relationships
        // since Cayenne requires them
        it = sortedModelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            
            // skip EOPrototypes
            if(isPrototypesEntity(name)) {
                continue;
            }
            
            DbEntity dbEntity = dataMap.getObjEntity(name).getDbEntity();

            if (dbEntity != null) {
                makeReverseDbRelationships(dbEntity);
            }
        }

        // build SelectQueries out of EOFetchSpecifications...
        it = sortedModelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            
            // skip EOPrototypes
            if(isPrototypesEntity(name)) {
                continue;
            }
            
            Iterator queries = helper.queryNames(name);
            while (queries.hasNext()) {
                String queryName = (String) queries.next();

                EOObjEntity entity = (EOObjEntity) dataMap.getObjEntity(name);
                makeQuery(helper, entity, queryName);
            }
        }

        return dataMap;
    }

    /**
     * Returns whether an Entity is an EOF EOPrototypes entity. According to EOF 
     * conventions EOPrototypes and EO[Adapter]Prototypes entities are considered 
     * to be prototypes.
     * 
     * @since 1.1 
     */
    protected boolean isPrototypesEntity(String entityName) {
        return entityName != null
                && entityName.startsWith("EO")
                && entityName.endsWith("Prototypes");
    }

    /**
     * @deprecated since 1.0.4 use {@link #makeHelper(String, boolean)}.
     */
    protected EOModelHelper makeHelper(String path) throws Exception {
        return makeHelper(path, false);
    }

    /**
     * Creates an returns new EOModelHelper to process EOModel. Exists mostly for the
     * benefit of subclasses.
     */
    protected EOModelHelper makeHelper(String path, boolean genereateClientClass)
            throws Exception {
        return new EOModelHelper(path);
    }

    /**
     * @deprecated since 1.0.4 use {@link #makeEntity(EOModelHelper, String, boolean)}.
     */
    protected ObjEntity makeEntity(EOModelHelper helper, String name) throws Exception {
        return makeEntity(helper, name, false);
    }

    /**
     * Creates a Cayenne query out of EOFetchSpecification data.
     * 
     * @since 1.1
     */
    protected Query makeQuery(EOModelHelper helper, EOObjEntity entity, String queryName) {

        DataMap dataMap = helper.getDataMap();
        Map queryPlist = helper.queryPListMap(entity.getName(), queryName);
        if (queryPlist == null) {
            return null;
        }

        EOQuery query = new EOQuery(entity, queryPlist);
        query.setName(entity.qualifiedQueryName(queryName));
        dataMap.addQuery(query);

        return query;
    }

    /**
     * Creates and returns a new ObjEntity linked to a corresponding DbEntity.
     */
    protected EOObjEntity makeEntity(
            EOModelHelper helper,
            String name,
            boolean generateClientClass) {

        DataMap dataMap = helper.getDataMap();
        Map entityPlist = helper.entityPListMap(name);

        // create ObjEntity
        EOObjEntity objEntity = new EOObjEntity(name);
        objEntity.setIsClientEntity(generateClientClass);
        String parent = (String) entityPlist.get("parent");
        objEntity.setClassName(helper.entityClass(name, generateClientClass));

        if (parent != null) {
            objEntity.setHasSuperClass(true);
            objEntity.setSuperClassName(helper.entityClass(parent, generateClientClass));
        }

        // add flag whether this entity is set as abstract in the model
        objEntity.setIsAbstractEntity("Y".equals(entityPlist.get("isAbstractEntity")));

        // create DbEntity...since EOF allows the same table to be
        // associated with multiple EOEntities, check for name duplicates
        String dbEntityName = (String) entityPlist.get("externalName");
        if (dbEntityName != null) {

            // ... if inheritance is involved and parent hierarchy uses the same DBEntity,
            // do not create a DbEntity...
            boolean createDbEntity = true;
            if (parent != null) {
                String parentName = parent;
                while (parentName != null) {
                    Map parentData = helper.entityPListMap(parentName);
                    if (parentData == null) {
                        break;
                    }

                    String parentExternalName = (String) parentData.get("externalName");
                    if (parentExternalName == null) {
                        parentName = (String) parentData.get("parent");
                        continue;
                    }

                    if (dbEntityName.equals(parentExternalName)) {
                        createDbEntity = false;
                    }

                    break;
                }
            }

            if (createDbEntity) {
                int i = 0;
                String dbEntityBaseName = dbEntityName;
                while (dataMap.getDbEntity(dbEntityName) != null) {
                    dbEntityName = dbEntityBaseName + i++;
                }

                objEntity.setDbEntityName(dbEntityName);
                DbEntity de = new DbEntity(dbEntityName);
                dataMap.addDbEntity(de);
            }
        }

        // set various flags
        objEntity.setReadOnly("Y".equals(entityPlist.get("isReadOnly")));
        objEntity.setSuperEntityName((String) entityPlist.get("parent"));

        dataMap.addObjEntity(objEntity);

        return objEntity;
    }

    /**
     * @deprecated since 1.0.4 use {@link #makeAttributes(EOModelHelper, EOObjEntity)}.
     */
    protected void makeAttributes(EOModelHelper helper, ObjEntity objEntity) {
        makeAttributes(helper, objEntity);
    }

    /**
     * Create ObjAttributes of the specified entity, as well as DbAttributes of the
     * corresponding DbEntity.
     */
    protected void makeAttributes(EOModelHelper helper, EOObjEntity objEntity) {
        Map entityPlistMap = helper.entityPListMap(objEntity.getName());
        List primaryKeys = (List) entityPlistMap.get("primaryKeyAttributes");

        List classProperties;
        if (objEntity.getIsClientEntity()) {
            classProperties = (List) entityPlistMap.get("clientClassProperties");
        }
        else {
            classProperties = (List) entityPlistMap.get("classProperties");
        }

        List attributes = (List) entityPlistMap.get("attributes");
        DbEntity dbEntity = objEntity.getDbEntity();

        if (primaryKeys == null) {
            primaryKeys = Collections.EMPTY_LIST;
        }

        if (classProperties == null) {
            classProperties = Collections.EMPTY_LIST;
        }

        if (attributes == null) {
            attributes = Collections.EMPTY_LIST;
        }

        // detect single table inheritance
        boolean singleTableInheritance = false;
        String parentName = (String) entityPlistMap.get("parent");
        while (parentName != null) {
            Map parentData = helper.entityPListMap(parentName);
            if (parentData == null) {
                break;
            }

            String parentExternalName = (String) parentData.get("externalName");
            if (parentExternalName == null) {
                parentName = (String) parentData.get("parent");
                continue;
            }

            if (dbEntity.getName() != null
                    && dbEntity.getName().equals(parentExternalName)) {
                singleTableInheritance = true;
            }

            break;
        }

        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Map attrMap = (Map) it.next();

            String prototypeName = (String) attrMap.get("prototypeName");
            Map prototypeAttrMap = helper.getPrototypeAttributeMapFor(prototypeName);

            String dbAttrName = (String) attrMap.get("columnName");
            if (null == dbAttrName) {
                dbAttrName = (String) prototypeAttrMap.get("columnName");
            }

            String attrName = (String) attrMap.get("name");
            if (null == attrName) {
                attrName = (String) prototypeAttrMap.get("name");
            }

            String attrType = (String) attrMap.get("valueClassName");
            if (null == attrType) {
                attrType = (String) prototypeAttrMap.get("valueClassName");
            }

            String valueType = (String) attrMap.get("valueType");
            if (valueType == null) {
                valueType = (String) prototypeAttrMap.get("valueType");
            }

            String javaType = helper.javaTypeForEOModelerType(attrType, valueType);
            EODbAttribute dbAttr = null;

            if (dbAttrName != null && dbEntity != null) {

                // if inherited attribute, skip it for DbEntity...
                if (!singleTableInheritance || dbEntity.getAttribute(dbAttrName) == null) {

                    // create DbAttribute...since EOF allows the same column name for
                    // more than one Java attribute, we need to check for name duplicates
                    int i = 0;
                    String dbAttributeBaseName = dbAttrName;
                    while (dbEntity.getAttribute(dbAttrName) != null) {
                        dbAttrName = dbAttributeBaseName + i++;
                    }

                    dbAttr = new EODbAttribute(dbAttrName, TypesMapping
                            .getSqlTypeByJava(javaType), dbEntity);
                    dbAttr.setEoAttributeName(attrName);
                    dbEntity.addAttribute(dbAttr);

                    Integer width = (Integer) attrMap.get("width");
                    if (null == width)
                        width = (Integer) prototypeAttrMap.get("width");

                    if (width != null)
                        dbAttr.setMaxLength(width.intValue());

                    Integer scale = (Integer) attrMap.get("scale");
                    if (null == scale)
                        scale = (Integer) prototypeAttrMap.get("scale");

                    if (scale != null)
                        dbAttr.setPrecision(scale.intValue());

                    if (primaryKeys.contains(attrName))
                        dbAttr.setPrimaryKey(true);

                    Object allowsNull = attrMap.get("allowsNull");
                    // TODO: Unclear that allowsNull should be inherited from EOPrototypes
                    // if (null == allowsNull) allowsNull =
                    // prototypeAttrMap.get("allowsNull");;

                    dbAttr.setMandatory(!"Y".equals(allowsNull));
                }
            }

            if (classProperties.contains(attrName)) {
                EOObjAttribute attr = new EOObjAttribute(attrName, javaType, objEntity);

                // set readOnly flag of Attribute if either attribute is read or
                // if entity is readOnly
                String entityReadOnlyString = (String) entityPlistMap.get("isReadOnly");
                String attributeReadOnlyString = (String) attrMap.get("isReadOnly");
                if ("Y".equals(entityReadOnlyString)
                        || "Y".equals(attributeReadOnlyString)) {
                    attr.setReadOnly(true);
                }
                

                // set name instead of the actual attribute, as it may be inherited....
                attr.setDbAttributeName(dbAttrName);
                objEntity.addAttribute(attr);
            }
        }
    }

    /**
     * Create ObjRelationships of the specified entity, as well as DbRelationships of the
     * corresponding DbEntity.
     */
    protected void makeRelationships(EOModelHelper helper, ObjEntity objEntity) {
        Map entityPlistMap = helper.entityPListMap(objEntity.getName());
        List classProps = (List) entityPlistMap.get("classProperties");
        List rinfo = (List) entityPlistMap.get("relationships");

        Collection attributes = (Collection) entityPlistMap.get("attributes");

        if (rinfo == null) {
            return;
        }

        if (classProps == null) {
            classProps = Collections.EMPTY_LIST;
        }

        if (attributes == null) {
            attributes = Collections.EMPTY_LIST;
        }

        DbEntity dbSrc = objEntity.getDbEntity();
        Iterator it = rinfo.iterator();
        while (it.hasNext()) {
            Map relMap = (Map) it.next();
            String targetName = (String) relMap.get("destination");

            // ignore flattened relationships for now
            if (targetName == null) {
                continue;
            }

            String relName = (String) relMap.get("name");
            boolean toMany = "Y".equals(relMap.get("isToMany"));
            boolean toDependentPK = "Y".equals(relMap.get("propagatesPrimaryKey"));
            ObjEntity target = helper.getDataMap().getObjEntity(targetName);

            // target maybe null for cross-EOModel relationships
            // ignoring those now.
            if (target == null) {
                continue;
            }

            DbEntity dbTarget = target.getDbEntity();
            Map targetPlistMap = helper.entityPListMap(targetName);
            Collection targetAttributes = (Collection) targetPlistMap.get("attributes");
            DbRelationship dbRel = null;

            // process underlying DbRelationship
            // Note: there is no flattened rel. support here....
            // Note: source maybe null, e.g. an abstract entity.
            if (dbSrc != null && dbTarget != null) {

                // in case of inheritance EOF stores duplicates of all inherited
                // relationships, so we must skip this relationship in DB entity if it is
                // already there...

                dbRel = (DbRelationship) dbSrc.getRelationship(relName);
                if (dbRel == null) {

                    dbRel = new DbRelationship();
                    dbRel.setSourceEntity(dbSrc);
                    dbRel.setTargetEntity(dbTarget);
                    dbRel.setToMany(toMany);
                    dbRel.setName(relName);
                    dbRel.setToDependentPK(toDependentPK);
                    dbSrc.addRelationship(dbRel);

                    List joins = (List) relMap.get("joins");
                    Iterator jIt = joins.iterator();
                    while (jIt.hasNext()) {
                        Map joinMap = (Map) jIt.next();

                        DbJoin join = new DbJoin(dbRel);

                        // find source attribute dictionary and extract the column name
                        String sourceAttributeName = (String) joinMap
                                .get("sourceAttribute");
                        join.setSourceName(columnName(attributes, sourceAttributeName));

                        String targetAttributeName = (String) joinMap
                                .get("destinationAttribute");

                        join.setTargetName(columnName(
                                targetAttributes,
                                targetAttributeName));
                        dbRel.addJoin(join);
                    }
                }
            }

            // only create obj relationship if it is a class property
            if (classProps.contains(relName)) {
                ObjRelationship rel = new ObjRelationship();
                rel.setName(relName);
                rel.setSourceEntity(objEntity);
                rel.setTargetEntity(target);
                objEntity.addRelationship(rel);

                if (dbRel != null) {
                    rel.addDbRelationship(dbRel);
                }
            }
        }
    }

    /**
     * Create reverse DbRelationships that were not created so far, since Cayenne requires
     * them.
     * 
     * @since 1.0.5
     */
    protected void makeReverseDbRelationships(DbEntity dbEntity) {
        if (dbEntity == null) {
            throw new NullPointerException(
                    "Attempt to create reverse relationships for the null DbEntity.");
        }

        // iterate over a copy of the collection, since in case of
        // reflexive relationships, we may modify source entity relationship map
        Collection clone = new ArrayList(dbEntity.getRelationships());
        Iterator it = clone.iterator();
        while (it.hasNext()) {
            DbRelationship relationship = (DbRelationship) it.next();

            if (relationship.getReverseRelationship() == null) {
                DbRelationship reverse = relationship.createReverseRelationship();

                String name = NamedObjectFactory.createName(DbRelationship.class, reverse
                        .getSourceEntity(), relationship.getName() + "Reverse");
                reverse.setName(name);
                relationship.getTargetEntity().addRelationship(reverse);
            }
        }
    }

    /**
     * Create Flattened ObjRelationships of the specified entity.
     */
    protected void makeFlatRelationships(EOModelHelper helper, ObjEntity e) {
        Map info = helper.entityPListMap(e.getName());
        List rinfo = (List) info.get("relationships");
        if (rinfo == null) {
            return;
        }

        Iterator it = rinfo.iterator();
        while (it.hasNext()) {
            Map relMap = (Map) it.next();
            String targetPath = (String) relMap.get("definition");

            // ignore normal relationships
            if (targetPath == null) {
                continue;
            }

            Expression exp = new ASTDbPath(targetPath);
            Iterator path = e.getDbEntity().resolvePathComponents(exp);

            ObjRelationship flatRel = new ObjRelationship();
            flatRel.setName((String) relMap.get("name"));

            DbRelationship firstRel = null;
            DbRelationship lastRel = null;
            while (path.hasNext()) {
                lastRel = (DbRelationship) path.next();
                flatRel.addDbRelationship(lastRel);

                if (firstRel == null) {
                    firstRel = lastRel;
                }
            }

            if ((firstRel != null) && (lastRel != null)) {
                flatRel.setSourceEntity(e);

                Collection potentialTargets = e.getDataMap().getMappedEntities(
                        (DbEntity) lastRel.getTargetEntity());

                // sanity check
                if (potentialTargets.size() != 1) {
                    throw new CayenneRuntimeException(
                            "One and only one entity should be mapped"
                                    + " to "
                                    + lastRel.getTargetEntity().getName()
                                    + ". Instead found : "
                                    + potentialTargets.size());
                }

                flatRel.setTargetEntity((ObjEntity) potentialTargets.iterator().next());
                e.addRelationship(flatRel);
            }
            else {
                throw new CayenneRuntimeException("relationship in the path was null!");
            }
        }

    }

    /**
     * Locates an attribute map matching the name and returns column name for this
     * attribute.
     * 
     * @since 1.1
     */
    String columnName(Collection entityAttributes, String attributeName) {
        if (attributeName == null) {
            return null;
        }

        Iterator it = entityAttributes.iterator();
        while (it.hasNext()) {
            Map map = (Map) it.next();
            if (attributeName.equals(map.get("name"))) {
                return (String) map.get("columnName");
            }
        }

        return null;
    }

    /**
     * Special DbAttribute subclass that stores extra info needed to work with EOModels.
     */
    // TODO: we have all EO-specific subclasses declared as public... Any reason to keep 
    // this one as an inner class?
    static class EODbAttribute extends DbAttribute {

        protected String eoAttributeName;

        public static DbAttribute findForEOAttributeName(DbEntity e, String name) {
            Iterator it = e.getAttributes().iterator();
            while (it.hasNext()) {
                EODbAttribute attr = (EODbAttribute) it.next();
                if (name.equals(attr.getEoAttributeName())) {
                    return attr;
                }
            }
            return null;
        }

        public EODbAttribute() {
        }

        public EODbAttribute(String name, int type, DbEntity entity) {
            super(name, type, entity);
        }

        public String getEoAttributeName() {
            return eoAttributeName;
        }

        public void setEoAttributeName(String eoAttributeName) {
            this.eoAttributeName = eoAttributeName;
        }
    }

    // sorts ObjEntities so that subentities in inheritance hierarchy are shown last
    final class InheritanceComparator implements Comparator {

        DataMap dataMap;

        InheritanceComparator(DataMap dataMap) {
            this.dataMap = dataMap;
        }

        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                return o2 != null ? -1 : 0;
            }
            else if (o2 == null) {
                return 1;
            }

            String name1 = o1.toString();
            String name2 = o2.toString();

            ObjEntity e1 = dataMap.getObjEntity(name1);
            ObjEntity e2 = dataMap.getObjEntity(name2);

            return compareEntities(e1, e2);
        }

        int compareEntities(ObjEntity e1, ObjEntity e2) {
            if (e1 == null) {
                return e2 != null ? -1 : 0;
            }
            else if (e2 == null) {
                return 1;
            }

            // entity goes first if it is a direct or indirect superentity of another
            // one
            if (e1.isSubentityOf(e2)) {
                return 1;
            }

            if (e2.isSubentityOf(e1)) {
                return -1;
            }

            // sort alphabetically
            return e1.getName().compareTo(e2.getName());
        }
    }
}