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
package org.objectstyle.cayenne.access.util;

import java.io.Serializable;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * This class represents a placeholder for an unresolved relationship from a source object.
 * RelationshipFault is used in cases when it is impossible to create a HOLLOW object using 
 * the information from the relationship source object. These cases include dependent to-one 
 * relationships and flattened to-one relationships.
 * 
 * @since 1.0.1
 * @author Andrei Adamchik
 */

// TODO: RelationshipFault should likely be used as a shared flyweight, one per relationship.
// This would allow to avoid creating faults instances per object. Also it can be extended
// to normal to-one relationships... then calling "resolveToOne" would create a HOLLOW object,
// but not before it is requested, speeding things up and improving memory use.
public class RelationshipFault implements Serializable {
    protected String relationshipName;
    protected DataObject sourceObject;

    public RelationshipFault(DataObject sourceObject, String relationshipName) {
        this.relationshipName = relationshipName;
        this.sourceObject = sourceObject;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public DataObject getSourceObject() {
        return sourceObject;
    }

    /**
     * Resolves this fault to a DataObject.
     */
    public DataObject resolveToOne() {
        DataContext context = sourceObject.getDataContext();
        SelectQuery select =
            QueryUtils.selectRelationshipObjects(context, sourceObject, relationshipName);
        select.setFetchLimit(2);

        List objects = context.performQuery(select);

        if (objects.isEmpty()) {
            return null;
        }
        else if (objects.size() == 1) {
            return (DataObject) objects.get(0);
        }
        else {
            ObjEntity entity = faultEntity();
            String label = (entity != null) ? entity.getName() : "Unknown";
            throw new CayenneRuntimeException(
                "Error resolving to-one fault. "
                    + "More than one object found. "
                    + "Fault entity: "
                    + label);
        }
    }

    /**
     * Determines ObjEntity of this fault.
     */
    protected ObjEntity faultEntity() {
        DataContext context = sourceObject.getDataContext();
        ObjEntity srcEntity = context.getEntityResolver().lookupObjEntity(sourceObject);
        Relationship relationship = srcEntity.getRelationship(relationshipName);

        if (relationship == null) {
            throw new IllegalStateException(
                "Non-existent relationship: " + relationshipName);
        }

        return (ObjEntity) relationship.getTargetEntity();
    }
}
