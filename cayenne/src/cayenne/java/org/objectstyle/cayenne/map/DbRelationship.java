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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.map.event.RelationshipEvent;

/**
 * A DbRelationship is a descriptor of a database inter-table relationship
 * based on one or more primary key/foreign key pairs.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbRelationship extends Relationship {
	//DbRelationship events
    public static final EventSubject PROPERTY_DID_CHANGE =
        EventSubject.getSubject(DbRelationship.class, "PropertyDidChange");

	// The columns through which the join is implemented.
	protected List joins = new ArrayList();
	
	// Is relationship from source to target points to dependent primary
	//  key (primary key column of destination table that is also a FK to the source column)
	protected boolean toDependentPK;

	public DbRelationship() {
		super();
	}

	public DbRelationship(String name) {
		super(name);
	}

	public DbRelationship(DbEntity src, DbEntity target, DbAttributePair pr) {
		this.setSourceEntity(src);
		this.setTargetEntity(target);
		this.addJoin(pr);
	}

	public Entity getTargetEntity() {
		if (getTargetEntityName() == null) {
			return null;
		}

		Entity src = getSourceEntity();
		if (src == null) {
			return null;
		}

		DataMap map = src.getDataMap();
		if (map == null) {
			return null;
		}

		return map.getDbEntity(getTargetEntityName(), true);
	}

	/** 
	 * Returns DbRelationship that is the opposite of this DbRelationship.
	 * This means a relationship from this target entity to this source entity with the same
	 * join semantics. Returns null if no such relationship exists. 
	 */
	public DbRelationship getReverseRelationship() {
		Entity target = this.getTargetEntity();
		Entity src = this.getSourceEntity();
		DbAttributePair testJoin = new DbAttributePair(null, null);

		Iterator it = target.getRelationships().iterator();
		while (it.hasNext()) {
			DbRelationship rel = (DbRelationship) it.next();
			if (rel.getTargetEntity() != src)
				continue;

			List otherJoins = rel.getJoins();
			if (otherJoins.size() != joins.size())
				continue;

			Iterator jit = otherJoins.iterator();
			boolean joinsMatch = true;
			while (jit.hasNext()) {
				DbAttributePair join = (DbAttributePair) jit.next();

				// flip join and try to find similar
				testJoin.setSource(join.getTarget());
				testJoin.setTarget(join.getSource());
				if (!joins.contains(testJoin)) {
					joinsMatch = false;
					break;
				}
			}

			if (joinsMatch)
				return rel;
		}
		return null;
	}

	/** Returns <code>true</code> if a method <code>isToDependentPK</code> of reverse relationship
	 * of this relationship returns <code>true</code>. */
	public boolean isToMasterPK() {
		if (isToMany() || isToDependentPK()) {
			return false;
		}

		DbRelationship revRel = getReverseRelationship();
		return (revRel != null) ? revRel.isToDependentPK() : false;
	}

	/** 
	 * Returns <code>true</code> if relationship from source to 
	 * target points to dependent primary key. Dependent PK is
	 * a primary key column of the destination table that is 
	 * also a FK to the source column. 
	 */
	public boolean isToDependentPK() {
		return toDependentPK;
	}

	public void setToDependentPK(boolean flag) {
		toDependentPK = flag;
	}

	/**
	 * Returns a list of joins. List is returned by reference, so 
	 * any modifications of the list will affect this relationship.
	 */
	public List getJoins() {
		return joins;
	}

	/** Adds a join. */
	public void addJoin(DbAttributePair join) {
		joins.add(join);
	}

	public void removeJoin(DbAttributePair join) {
		joins.remove(join);
	}

	public void removeAllJoins() {
		joins.clear();
	}

	public void setJoins(List newJoins) {
		if (null != newJoins) {
			this.removeAllJoins();
			joins.addAll(newJoins);
		}
	}

	/** Creates a snapshot of primary key attributes of a target
	  * object of this relationship based on a snapshot of a source.
	  * Only "to-one" relationships are supported.
	  * Returns null if relationship does not point to an object.
	  * Throws CayenneRuntimeException if relationship is "to many" or
	  * if snapshot is missing id components. */
    public Map targetPkSnapshotWithSrcSnapshot(Map srcSnapshot) {

        if (isToMany()) {
            throw new CayenneRuntimeException("Only 'to one' relationships support this method.");
        }

        Map idMap;

        int numJoins = joins.size();
        int foundNulls = 0;

        // optimize for the most common single column join
        if (numJoins == 1) {
            DbAttributePair join = (DbAttributePair) joins.get(0);
            Object val = srcSnapshot.get(join.getSource().getName());
            if (val == null) {
                foundNulls++;
                idMap = Collections.EMPTY_MAP;
            }
            else {
                idMap = Collections.singletonMap(join.getTarget().getName(), val);
            }
        }
        // handle generic case: numJoins > 1
        else {
            idMap = new HashMap(numJoins * 2);
            for (int i = 0; i < numJoins; i++) {
                DbAttributePair join = (DbAttributePair) joins.get(i);
                Object val = srcSnapshot.get(join.getSource().getName());
                if (val == null) {
                    foundNulls++;
                }
                else {
                    idMap.put(join.getTarget().getName(), val);
                }
            }
        }

        if (foundNulls == 0) {
            return idMap;
        }
        else if (foundNulls == numJoins) {
            return null;
        }
        else {
            throw new CayenneRuntimeException("Some parts of FK are missing in snapshot.");
        }
    }

	/** Common code to srcSnapshotWithTargetSnapshot.  Both are functionally the
	 * same, except for the name, and whether they operate on a toMany or a toOne.*/
	private Map srcSnapshotWithTargetSnapshot(Map targetSnapshot) {
		Map idMap = new HashMap();
		int len = joins.size();
		for (int i = 0; i < len; i++) {
			DbAttributePair join = (DbAttributePair) joins.get(i);
			Object val = targetSnapshot.get(join.getTarget().getName());
			if (val == null) {
				throw new CayenneRuntimeException("Some parts of FK are missing in snapshot.");
			}
			else {
				idMap.put(join.getSource().getName(), val);
			}
		}

		return idMap;
	}
	/** 
	 * Creates a snapshot of foreign key attributes of a source
	 * object of this relationship based on a snapshot of a target.
	 * Only "to-one" relationships are supported.
	 * Throws CayenneRuntimeException if relationship is "to many" or
	 * if snapshot is missing id components.
	 */
	public Map srcFkSnapshotWithTargetSnapshot(Map targetSnapshot) {

		if (isToMany())
			throw new CayenneRuntimeException("Only 'to one' relationships support this method.");
		return srcSnapshotWithTargetSnapshot(targetSnapshot);
	}

	/** 
	 * Creates a snapshot of primary key attributes of a source
	 * object of this relationship based on a snapshot of a target.
	 * Only "to-many" relationships are supported.
	 * Throws CayenneRuntimeException if relationship is "to one" or
	 * if snapshot is missing id components.
	 */
	public Map srcPkSnapshotWithTargetSnapshot(Map targetSnapshot) {
		if (!isToMany())
			throw new CayenneRuntimeException("Only 'to many' relationships support this method.");
		return srcSnapshotWithTargetSnapshot(targetSnapshot);
	}
	
	/** Set relationship multiplicity. */
	public void setToMany(boolean toMany) {
		this.toMany = toMany;
		this.firePropertyDidChange();
	}

	protected void firePropertyDidChange() {
		RelationshipEvent event=new RelationshipEvent(this, this, this.getSourceEntity());
		EventManager.getDefaultManager().postEvent(event, PROPERTY_DID_CHANGE);
	}
}
