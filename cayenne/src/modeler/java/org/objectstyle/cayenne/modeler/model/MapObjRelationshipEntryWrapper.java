/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * @since 1.1
 * @author Andrei Adamchik
 */
public class MapObjRelationshipEntryWrapper extends BasicModel {

    public static final Selector RELATIONSHIP_NAME_SELECTOR =
        Selector.fromString("relationshipName");
    public static final Selector SOURCE_NAME_SELECTOR =
        Selector.fromString("sourceEntityName");
    public static final Selector TARGET_NAME_SELECTOR =
        Selector.fromString("targetEntityName");

    protected DbEntity sourceEntity;
    protected String relationshipName;
    protected String defaultTargetName;
    protected Object[] relationshipNames;

    /**
     * Creates MapObjRelationshipEntryWrapper with two unconnected DbEntities.
     */
    public MapObjRelationshipEntryWrapper(DbEntity sourceEntity, DbEntity targetEntity) {
        this.sourceEntity = sourceEntity;
        this.defaultTargetName = targetEntity.getName();
        this.relationshipName = "";
    }

    /**
     * Creates MapObjRelationshipEntryWrapper over the relationship connecting
     * two DbEntities.
     */
    public MapObjRelationshipEntryWrapper(DbRelationship relationship) {
        this.sourceEntity = (DbEntity) relationship.getSourceEntity();
        this.relationshipName = relationship.getName();
    }

    public synchronized Object[] getRelationshipNames() {
        if (relationshipNames == null) {
            Collection relationships = getSourceEntity().getRelationships();
            int size = relationships.size();
            Object[] names = new Object[size];

            Iterator it = relationships.iterator();
            for (int i = 0; i < size; i++) {
                names[i] = ((DbRelationship) it.next()).getName();
            }
            Arrays.sort(names);
            this.relationshipNames = names;
        }

        return relationshipNames;
    }

    public DbEntity getSourceEntity() {
        return sourceEntity;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        if (!Util.nullSafeEquals(relationshipName, this.relationshipName)) {
            this.relationshipName = relationshipName;
            relationshipNames = null;
            fireModelChange(ModelChangeEvent.VALUE_CHANGED, RELATIONSHIP_NAME_SELECTOR);
        }
    }

    public DbRelationship getSelectedRelationship() {
        return (DbRelationship) sourceEntity.getRelationship(relationshipName);
    }

    public String getSourceEntityName() {
        return sourceEntity.getName();
    }

    public String getTargetEntityName() {
        DbRelationship selected = getSelectedRelationship();
        return (selected != null) ? selected.getTargetEntityName() : defaultTargetName;
    }
}
