package org.objectstyle.cayenne.wocompat;
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

import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.wocompat.parser.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import org.objectstyle.cayenne.dba.*;


/**
 *  Class that converts EOModels to org.objectstyle.cayenne.map.DataMap objects.
 */
public class EOModelProcessor {
    static Logger logObj = Logger.getLogger(EOModelProcessor.class.getName());
    
    public static String eomodelNameForPath(String path) {
        // strip trailing "/" if needed
        if(path.endsWith(File.separator))
            path = path.substring(0, path.length() - 1);
        
        int startInd = path.lastIndexOf(File.separator);
        int endInd = (path.endsWith(".eomodeld")) ? path.length() - ".eomodeld".length() : path.length();
        return path.substring(startInd + 1, endInd);
    }
    
    /** Do some ObjC->Java data type conversion */
    public static String javaTypeForEOModelerType(String type) {
        if(type.equals("NSString"))
            return "java.lang.String";
        if(type.equals("NSNumber"))
            return "java.lang.Integer";
        if(type.equals("NSCalendarDate"))
            return "java.sql.Time";
        if(type.equals("NSDecimalNumber"))
            return "java.math.BigDecimal";
        if(type.equals("NSData"))
            return "byte[]";
        
        throw new IllegalArgumentException("Unknown data type: " + type);
    }
    
    
    public static void saveEOModel(DataMap map, String path) {
        
    }
    
    
    // plist parser
    private Parser plistParser = new Parser();
    
    // map of EO attribute names to external column names
    private Map attributeMap = new HashMap();
    
    // map of EOModel relationship info with ObjEntities as keys
    private HashMap relationshipMap = new HashMap();
    
    
    public void reset() {
        attributeMap.clear();
        relationshipMap.clear();
    }
    
    
    public DataMap loadEOModel(String path) throws java.lang.Exception {
        reset();
        
        // strip trailing "/" if needed
        if(path.endsWith(File.separator))
            path = path.substring(0, path.length() - 1);
        
        // fix path if needed....
        if(!path.endsWith(".eomodeld"))
            path = path + ".eomodeld";
        
        File eomDir = new File(path);
        File indexFile = new File(eomDir, "index.eomodeld");
        
        if(!indexFile.exists())
            throw new IllegalArgumentException("No model found at '" + path + "'.");
        
        plistParser.ReInit(new FileInputStream(indexFile));
        Map indexMap = (Map)plistParser.propertyList();
        List entities = (List)indexMap.get("entities");
        
        
        // do indiv. enitities
        DataMap dataMap = new DataMap("Untitled Map");
        Iterator it = entities.iterator();
        while(it.hasNext()) {
            Map entity = (Map)it.next();
            String entName = (String)entity.get("name");
            String javaClass = (String)entity.get("className");
            
            ObjEntity objEntity = new ObjEntity(entName);
            objEntity.setClassName(javaClass);
            dataMap.addObjEntity(objEntity);
            objEntity.setDataMap(dataMap);
            
            // load entity info
            processObjEntity(objEntity, eomDir);
        }
        
        // do relationships
        Iterator relIt = relationshipMap.keySet().iterator();
        while(relIt.hasNext()) {
            ObjEntity nextEnt = (ObjEntity)relIt.next();
            List rels = (List)relationshipMap.get(nextEnt);
            if(rels != null)
                processEntityRelationships(nextEnt, rels);
        }
        
        return dataMap;
    }
    
    
    /** Take an ObjEntity stub and initialize it with the data from EntName.plist file.
     */
    private void processObjEntity(ObjEntity entity, File modelDir) throws Exception {
        File entFile = new File(modelDir, entity.getName() + ".plist");
        
        if(!entFile.exists())
            throw new IllegalArgumentException("No entity found at '" + entFile + "'.");
        
        // parse plist file
        plistParser.ReInit(new FileInputStream(entFile));
        Map entDetails = (Map)plistParser.propertyList();
        
        // retrieve misc info
        List pks = (List)entDetails.get("primaryKeyAttributes");
        List classProps = (List)entDetails.get("classProperties");
        
        // create DbEntity
        String dbEntName = (String)entDetails.get("externalName");
        DbEntity dbEntity = new DbEntity(dbEntName);
        entity.setDbEntity(dbEntity);
        entity.getDataMap().addDbEntity(dbEntity);
        
        // save relationship info for future use
        Object relInfo = entDetails.get("relationships");
        if(relInfo != null)
            relationshipMap.put(entity, relInfo);
        
        // process attribute list creating both Db and Obj attributes
        List attributes = (List)entDetails.get("attributes");
        Iterator it = attributes.iterator();
        while(it.hasNext()) {
            Map attrMap = (Map)it.next();
            String dbAttrName = (String)attrMap.get("columnName");
            String attrName = (String)attrMap.get("name");
            String attrType = (String)attrMap.get("valueClassName");
            String javaType = javaTypeForEOModelerType(attrType);
            
            // save mapping of external attr. name to internal name for future use
            attributeMap.put(attrName, dbAttrName);
            
            DbAttribute dbAttr = new DbAttribute(dbAttrName, TypesMapping.getSqlTypeByJava(javaType), dbEntity);
            dbEntity.addAttribute(dbAttr);
            
            Integer width = (Integer)attrMap.get("width");
            if (width != null)
                dbAttr.setMaxLength(width.intValue());
            
            if(pks.contains(attrName))
                dbAttr.setPrimaryKey(true);
            
            Object allowsNull = attrMap.get("allowsNull");
            dbAttr.setMandatory(!"Y".equals(allowsNull));
            if(classProps.contains(attrName)) {
                ObjAttribute attr = new ObjAttribute(attrName, javaType, entity);
                attr.setDbAttribute(dbAttr);
                entity.addAttribute(attr);
            }
        }
    }
    
    private void processEntityRelationships(ObjEntity src, List entRels) {
        Iterator it = entRels.iterator();
        DataMap dataMap = src.getDataMap();
        
        
        while(it.hasNext()) {
            Map relMap = (Map)it.next();
            String targetName = (String)relMap.get("destination");
            String relName = (String)relMap.get("name");
            boolean toMany = "Y".equals(relMap.get("isToMany"));
            boolean toDependentPK = "Y".equals(relMap.get("propagatesPrimaryKey"));
            ObjEntity target = dataMap.getObjEntity(targetName);
            DbEntity dbSrc = src.getDbEntity();
            DbEntity dbTarget = target.getDbEntity();
            
            ObjRelationship rel = new ObjRelationship();
            rel.setName(relName);
            rel.setSourceEntity(src);
            rel.setTargetEntity(target);
            rel.setToMany(toMany);
            src.addRelationship(rel);
            
            // process underlying DbRelationship
            // Note - there is no flattened rel. support here....
            DbRelationship dbRel = new DbRelationship();
            dbRel.setSourceEntity(dbSrc);
            dbRel.setTargetEntity(dbTarget);
            dbRel.setToMany(toMany);
            dbRel.setName(relName);
            dbRel.setToDependentPK(toDependentPK);
            rel.addDbRelationship(dbRel);
            
            List joins = (List)relMap.get("joins");
            Iterator jIt = joins.iterator();
            while(jIt.hasNext()) {
                Map joinMap = (Map)jIt.next();
                String srcAttrName = (String)joinMap.get("sourceAttribute");
                String targetAttrName = (String)joinMap.get("destinationAttribute");
                
                
                DbAttribute srcAttr = (DbAttribute)dbSrc.getAttribute((String)attributeMap.get(srcAttrName));
                DbAttribute targetAttr = (DbAttribute)dbTarget.getAttribute((String)attributeMap.get(targetAttrName));
                
                DbAttributePair join = new DbAttributePair(srcAttr, targetAttr);
                dbRel.addJoin(join);
            }
        }
    }
}
