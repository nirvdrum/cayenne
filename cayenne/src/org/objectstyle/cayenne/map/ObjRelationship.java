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


import java.util.*;
import java.util.logging.*;
import org.objectstyle.cayenne.*;

/** Metadata for the navigational association between the data objects.
 *  For example, if class "Employee" you may need to get to the department
 *  entity by calling "employee.getDepartment()". In this case you navigate
 *  from data object class Employee to Department. In this case Employee is
 *  source and Department is target. The navigation from Department to the
 *  list of employees would be expressed by another instance of
 *  ObjRelationship.
 *  ObjRelationship class also stores the navigation information in terms
 *  of the database entity relationships.
 *  The ObjRelationship objects are stored in the ObjEntitys. */
public class ObjRelationship extends Relationship {
    static Logger logObj = Logger.getLogger(ObjRelationship.class.getName());

	private List dbRelationships = new ArrayList();

    public ObjRelationship() {}
    
    public ObjRelationship(String name) {
        super(name);
    }

	public ObjRelationship(ObjEntity source, ObjEntity target, boolean toMany){
		setSourceEntity(source);
		setTargetEntity(target);
		setToMany(toMany);
        if(toMany)
            setName(target.getName() + "Array");
        else
            setName("to" + target.getName());
	}
    
    /** Returns true if underlying DbRelationships point to dependent entity. */
    public boolean isToDependentEntity() {
        return ((DbRelationship)dbRelationships.get(0)).isToDependentPK();
    }
    
    /** Returns ObjRelationship that is the opposite of this ObjRelationship.
    * returns null if no such relationship exists. */
    public ObjRelationship getReverseRelationship() {
        Entity target = getTargetEntity();
        Entity src = getSourceEntity();

        // reverse the list
        ArrayList reversed = new ArrayList();
        Iterator rit = getDbRelationshipList().iterator();
        while(rit.hasNext()) {
            DbRelationship rel = (DbRelationship)rit.next();
            DbRelationship reverse = rel.getReverseRelationship();
            if(reverse == null)
                return null;

            reversed.add(0, reverse);
        }

        Iterator it = target.getRelationshipList().iterator();
        while(it.hasNext()) {
            ObjRelationship rel = (ObjRelationship)it.next();
            if(rel.getTargetEntity() != src)
                continue;

            List otherRels = rel.getDbRelationshipList();
            if(reversed.size() != otherRels.size())
                continue;

            int len = reversed.size();
            boolean relsMatch = true;
            for(int i = 0; i < len; i++) {
                if(otherRels.get(i) != reversed.get(i)) {
                    relsMatch = false;
                    break;
                }
            }

            if(relsMatch)
                return rel;
        }

        return null;
    }

    /** Returns a list of underlying DbRelationships */
    public List getDbRelationshipList() {
        return Collections.unmodifiableList(dbRelationships);
    }


	/** Appends a DbRelationship to the existing list of DbRelationships.*/
	public void addDbRelationship(DbRelationship dbRel) {
		dbRelationships.add(dbRel);
	}


    /** Removes a relationship <code>dbRel</code> from the list of relationships. */
    public void removeDbRelationship(DbRelationship dbRel) {
        dbRelationships.remove(dbRel);
    }

    public void removeAllDbRelationships() {
    	dbRelationships.clear();
    }


    public String toString() {
        StringBuffer sb = new StringBuffer("ObjRelationship: \n");
        sb.append("Relationship '").append(name);
        if(toMany)
            sb.append("' (to-many)\n");
        else
            sb.append("' (to-one)\n");

        sb.append("Source entity: ");
        if(sourceEntity == null)
            sb.append("<null>");
        else
            sb.append(sourceEntity.getName());

        sb.append("Target entity: ");
        if(targetEntity == null)
            sb.append("<null>");
        else
            sb.append(targetEntity.getName());

        sb.append("\n------------------\n");
        return sb.toString();
    }
}
