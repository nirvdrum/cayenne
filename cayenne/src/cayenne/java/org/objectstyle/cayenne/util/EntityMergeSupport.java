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
package org.objectstyle.cayenne.util;

import java.util.*;

import org.objectstyle.cayenne.dba.*;
import org.objectstyle.cayenne.map.*;

/**
 * Class that implements methods for entity merging.
 * At this point it is at experimental stage, so chances of
 * API changes are about 100%. 
 * 
 * @author Andrei Adamchik
 */
public class EntityMergeSupport {
	protected DataMap map;
	protected ObjEntity objEntity;

	public EntityMergeSupport(DataMap map) {
		this.map = map;
	}

	protected void reset() {
		objEntity = null;
	}

	/**
	 * Updates ObjEntity attributes and relationships
	 * based on the current state of its DbEntity.
	 */
	public void synchronizeWithDbEntity(ObjEntity entity) {
		reset();

		if (entity == null || entity.getDbEntity() == null) {
			return;
		}

		this.setEntity(entity);

		List addAttributes = getAttributesToAdd();
		List addRelationships = getRelationshipsToAdd();

		// add missing attributes
		Iterator ait = addAttributes.iterator();
		while (ait.hasNext()) {
			DbAttribute da = (DbAttribute) ait.next();
			String attName =
				NameConverter.undescoredToJava(da.getName(), false);
			String type = TypesMapping.getJavaBySqlType(da.getType());

			ObjAttribute oa = new ObjAttribute(attName, type, entity);
			oa.setDbAttribute(da);
			entity.addAttribute(oa);
		}

		// add missing relationships
		Iterator rit = addRelationships.iterator();
		while (rit.hasNext()) {
			DbRelationship dr = (DbRelationship) rit.next();
			List mappedTargets =
				map.getMappedEntities((DbEntity) dr.getTargetEntity());
			if (mappedTargets.size() == 0) {
                continue;
			}

			ObjRelationship or = new ObjRelationship(dr.getName());
			or.addDbRelationship(dr);
			or.setToMany(dr.isToMany());
			or.setSourceEntity(entity);
			or.setTargetEntity((Entity) mappedTargets.get(0));
			entity.addRelationship(or);
		}
	}

	/**
	 * Returns a list of attributes that exist in the DbEntity, but 
	 * are missing from the ObjEntity.
	 */
	protected List getAttributesToAdd() {
		List missing = new ArrayList();
		Iterator it = objEntity.getDbEntity().getAttributeList().iterator();
		List rels = objEntity.getDbEntity().getRelationshipList();

		while (it.hasNext()) {
			DbAttribute dba = (DbAttribute) it.next();
			// already there
			if (objEntity.getAttributeForDbAttribute(dba) != null) {
				continue;
			}

			// check if adding it makes sense at all
			if (dba.getName() == null || dba.isPrimaryKey()) {
				continue;
			}

			// check FK's 
			boolean isFK = false;
			Iterator rit = rels.iterator();
			while (!isFK && rit.hasNext()) {
				DbRelationship rel = (DbRelationship) rit.next();
				Iterator jit = rel.getJoins().iterator();
				while (jit.hasNext()) {
					DbAttributePair join = (DbAttributePair) jit.next();
					if (join.getSource() == dba) {
						isFK = true;
						break;
					}
				}
			}

			if (isFK) {
				continue;
			}

			missing.add(dba);
		}

		return missing;
	}

	protected List getRelationshipsToAdd() {
		List missing = new ArrayList();
		Iterator it = objEntity.getDbEntity().getRelationshipList().iterator();
		while (it.hasNext()) {
			DbRelationship dbrel = (DbRelationship) it.next();
			// check if adding it makes sense at all
			if (dbrel.getName() == null) {
				continue;
			}

			if (objEntity.getRelationshipForDbRelationship(dbrel) == null) {
				missing.add(dbrel);
			}
		}

		return missing;
	}

	public ObjEntity getEntity() {
		return objEntity;
	}

	public void setEntity(ObjEntity entity) {
		this.objEntity = entity;
	}

	public DataMap getMap() {
		return map;
	}

	public void setMap(DataMap map) {
		this.map = map;
	}
}
