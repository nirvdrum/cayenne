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
package org.objectstyle.cayenne.modeler.datamap;

import java.util.ArrayList;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.MapUtil;

/** 
 * Table model to display ObjRelationships. 
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjRelationshipTableModel extends CayenneTableModel {
    // Columns
    static final int REL_NAME = 0;
    static final int REL_TARGET = 1;
    static final int REL_CARDINALITY = 2;
    static final int REL_DELETERULE = 3;

    protected ObjEntity entity;

    public ObjRelationshipTableModel(
        ObjEntity entity,
        EventController mediator,
        Object eventSource) {
        super(mediator, eventSource, new ArrayList(entity.getRelationships()));
        this.entity = entity;
    }

    /**
     * Returns ObjRelationship class.
     */
    public Class getElementsClass() {
        return ObjRelationship.class;
    }

    public int getColumnCount() {
        return 4;
    }

    public String getColumnName(int column) {
        if (column == REL_NAME)
            return "Name";
        else if (column == REL_TARGET)
            return "Target";
        else if (column == REL_CARDINALITY)
            return "To many";
        else if (column == REL_DELETERULE)
            return "Delete rule";
        else
            return "";
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case REL_CARDINALITY :
                return Boolean.class;
            default :
                return String.class;
        }
    }

    public ObjRelationship getRelationship(int row) {
        return (row >= 0 && row < objectList.size())
            ? (ObjRelationship) objectList.get(row)
            : null;
    }

    public Object getValueAt(int row, int column) {
        ObjRelationship rel = getRelationship(row);
        // If name column
        if (column == REL_NAME) {
            return rel.getName();
            // If target column
        }
        else if (column == REL_TARGET) {
            if (null == rel.getTargetEntity())
                return null;
            return rel.getTargetEntity().getName();
        }
        else if (column == REL_CARDINALITY) {
            return new Boolean(rel.isToMany());
        }
        else if (column == REL_DELETERULE) {
            return DeleteRule.deleteRuleName(rel.getDeleteRule());
        }
        else {
            return "";
        }
    }

    public void setUpdatedValueAt(Object aValue, int row, int column) {
        ObjRelationship rel = getRelationship(row);

        // If name column
        if (column == REL_NAME) {
            String text = (String) aValue;
            String old_name = rel.getName();
            MapUtil.setRelationshipName(entity, rel, text);
            RelationshipEvent e =
                new RelationshipEvent(eventSource, rel, entity, old_name);
            mediator.fireObjRelationshipEvent(e);
            fireTableCellUpdated(row, column);
        }
        // If target column
        else if (column == REL_TARGET) {
            if (aValue == null) {
                return;
            }

            String targetName = aValue.toString().trim();

            // Remove db relationship mappings.
            rel.clearDbRelationships();

            // Set new target, if applicable
            ObjEntity target = null;
            if (!"".equals(targetName)) {
                DataMap map = mediator.getCurrentDataMap();
                target = map.getObjEntity(targetName, true);
            }

            rel.setTargetEntity(target);
            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
            mediator.fireObjRelationshipEvent(e);

            // now try to connect DbEntities if we can do it in one step
            if (target != null) {
                DbEntity srcDB = ((ObjEntity) rel.getSourceEntity()).getDbEntity();
                DbEntity targetDB = target.getDbEntity();
                if (srcDB != null && targetDB != null) {
                    Relationship anyConnector = srcDB.getAnyRelationship(targetDB);
                    if (anyConnector != null) {
                        rel.addDbRelationship((DbRelationship) anyConnector);
                    }
                }
            }

        }
        else if (column == REL_DELETERULE) {
            String temp = (String) aValue;
            rel.setDeleteRule(DeleteRule.deleteRuleForName(temp));
            RelationshipEvent e = new RelationshipEvent(eventSource, rel, entity);
            mediator.fireObjRelationshipEvent(e);
        }
        fireTableRowsUpdated(row, row);
    }

    public void removeRow(int row) {
        if (row < 0)
            return;
        Relationship rel = getRelationship(row);
        RelationshipEvent e;
        e = new RelationshipEvent(eventSource, rel, entity, RelationshipEvent.REMOVE);
        mediator.fireObjRelationshipEvent(e);
        objectList.remove(row);
        entity.removeRelationship(rel.getName());
        fireTableRowsDeleted(row, row);
    }

    /** Relationship just needs to be removed from the model. 
     *  It is already removed from the DataMap. */
    void removeRelationship(Relationship rel) {
        objectList.remove(rel);
        fireTableDataChanged();
    }

    public boolean isCellEditable(int row, int col) {
        if (col == REL_CARDINALITY) {
            return false; //Cannot edit the toMany flag on an ObjRelationship
        }
        return true;
    }

}
