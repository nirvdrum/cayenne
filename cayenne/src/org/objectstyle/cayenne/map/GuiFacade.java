package org.objectstyle.cayenne.map;
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

import java.util.Iterator;

/** Provides utility methods to access DataMap, Entities, etc.
 *  For example, setName() in Attribute requires changing
 *  the keys in attribute Maps in Entities. */
public class GuiFacade {

	public static void setObjEntityName(DataMap map, ObjEntity entity
									  , String new_name)
	{
		String old_name = entity.getName();
		// If name hasnt change, just return
		if (old_name != null && old_name.equals(new_name)) {
			return;
		}
		entity.setName(new_name);
		map.removeObjEntity(old_name);
		map.addObjEntity(entity);
	}
	
	public static void setDbEntityName(DataMap map, DbEntity entity
									  , String new_name)
	{
		String old_name = entity.getName();
		// If name hasnt change, just return
		if (old_name != null && old_name.equals(new_name)) {
			return;
		}
		entity.setName(new_name);
		map.removeDbEntity(old_name);
		map.addDbEntity(entity);
	}
	
	/** Changes the name of the attribute in all places in DataMap. */
	public static void setObjAttributeName(DataMap map, ObjAttribute attrib
								  , String new_name)
	{
		ObjEntity entity = (ObjEntity)attrib.getEntity();
		String old_name = attrib.getName();
		entity.attributes.remove(old_name);
		attrib.setName(new_name);
		entity.attributes.put(new_name, attrib);
		
		Iterator iter = entity.attributes.keySet().iterator();
		while(iter.hasNext()) {
			System.out.println("Key is {" + (String)iter.next() + "}");
		}
	}


	/** Changes the name of the attribute in all places in DataMap. */
	public static void setDbAttributeName(DataMap map, DbAttribute attrib
								  , String new_name)
	{
		DbEntity entity = (DbEntity)attrib.getEntity();
		String old_name = attrib.getName();
		entity.attributes.remove(old_name);
		attrib.setName(new_name);
		entity.attributes.put(new_name, attrib);
	}

	/** Changes the name of the attribute in all places in DataMap. */
	public static void setObjRelationshipName(ObjEntity entity
										, ObjRelationship rel, String new_name)
	{
		ObjRelationship temp_rel;
		temp_rel = (ObjRelationship)entity.relationships.get(rel.getName());
		// If rel is not in the entity - we have a problem
		if (null == temp_rel || temp_rel != rel) {
			System.out.println("Cannot find obj relationship " + rel.getName()
							+ " in obj entity " + entity.getName());
			Thread.currentThread().dumpStack();
			return;
		}
		entity.relationships.remove(rel.getName());
		rel.setName(new_name);
		entity.relationships.put(rel.getName(), rel);
	}
	
	public static void setDbRelationshipName(DbEntity entity
						, DbRelationship rel, String new_name)
	{
		DbRelationship temp_rel;
		temp_rel = (DbRelationship)entity.relationships.get(rel.getName());
		if (null == temp_rel || temp_rel != rel) {
			System.out.println("Cannot find db relationship " + rel.getName()
							+ " in db entity " + entity.getName());
			Thread.currentThread().dumpStack();
			return;
		}
		entity.relationships.remove(rel.getName());
		rel.setName(new_name);
		entity.relationships.put(rel.getName(), rel);
	}

	
     /** Clears all the mapping between this obj entity and its current db entity.
      *  Clears mapping between entities, attributes and relationships. */
     public static void clearDbMapping(ObjEntity entity) {
     	DbEntity db_entity = entity.getDbEntity();
     	if (db_entity == null)
     		return;

        Iterator it = entity.getAttributeMap().values().iterator();
        while (it.hasNext()) {
             ObjAttribute objAttr = (ObjAttribute)it.next();
             DbAttribute dbAttr = objAttr.getDbAttribute();
             if (null != dbAttr) {
             	objAttr.setDbAttribute(null);
             }
        }// End while()
        
        Iterator rel_it = entity.getRelationshipList().iterator();
        while(rel_it.hasNext()) {
        	ObjRelationship obj_rel = (ObjRelationship)rel_it.next();
        	obj_rel.removeAllDbRelationships();
        }
        entity.setDbEntity(null);
     }
}