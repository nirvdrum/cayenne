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

package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.Editor;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.util.MapUtil;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.project.ProjectPath;

/** 
 * Removes currently selected object from the project. This can be 
 * Domain, DataNode, Entity, Attribute or Relationship.
 * 
 * @author Misha Shengaout
 */
public class RemoveAction extends CayenneAction {
    private static Logger logObj = Logger.getLogger(RemoveAction.class);
    public static final String ACTION_NAME = "Remove";

    public RemoveAction() {
        super(ACTION_NAME);
    }

    public String getIconName() {
        return "icon-trash.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK);
    }

    public void performAction(ActionEvent e) {
        remove();
    }

    private void remove() {
        EventController mediator = getMediator();

        if (mediator.getCurrentObjAttribute() != null) {
            removeObjAttribute();
        } else if (mediator.getCurrentDbAttribute() != null) {
            removeDbAttribute();
        } else if (mediator.getCurrentObjRelationship() != null) {
            removeObjRelationship();
        } else if (mediator.getCurrentDbRelationship() != null) {
            removeDbRelationship();
        } else if (mediator.getCurrentObjEntity() != null) {
            removeObjEntity();
        } else if (mediator.getCurrentDbEntity() != null) {
            removeDbEntity();
        } else if (mediator.getCurrentDataMap() != null) {
            // In context of Data node just remove from Data Node
            if (mediator.getCurrentDataNode() != null) {
                removeDataMapFromDataNode();
            } else {
                // Not under Data Node, remove completely
                removeDataMap();
            }
        } else if (mediator.getCurrentDataNode() != null) {
            removeDataNode();
        } else if (mediator.getCurrentDataDomain() != null) {
            removeDomain();
        }
    }

    protected void removeDomain() {
        ApplicationProject project = (ApplicationProject) Editor.getProject();
        EventController mediator = getMediator();
        DataDomain domain = mediator.getCurrentDataDomain();
        project.getConfig().removeDomain(domain.getName());
        mediator.fireDomainEvent(
            new DomainEvent(Editor.getFrame(), domain, DomainEvent.REMOVE));
    }

    protected void removeDataMap() {
        EventController mediator = getMediator();
        DataMap map = mediator.getCurrentDataMap();
        DataDomain domain = mediator.getCurrentDataDomain();
        domain.removeMap(map.getName());
        mediator.fireDataMapEvent(
            new DataMapEvent(Editor.getFrame(), map, DataMapEvent.REMOVE));
    }

    protected void removeDataNode() {
        EventController mediator = getMediator();
        DataNode node = mediator.getCurrentDataNode();
        DataDomain domain = mediator.getCurrentDataDomain();
        domain.removeDataNode(node.getName());
        mediator.fireDataNodeEvent(
            new DataNodeEvent(Editor.getFrame(), node, DataNodeEvent.REMOVE));
    }

    /** 
     * Removes current DbEntity from its DataMap and fires 
     * "remove" EntityEvent.
     */
    protected void removeDbEntity() {
        EventController mediator = getMediator();
        DbEntity ent = mediator.getCurrentDbEntity();
        DataMap map = mediator.getCurrentDataMap();
        map.deleteDbEntity(ent.getName());
        mediator.fireDbEntityEvent(
            new EntityEvent(Editor.getFrame(), ent, EntityEvent.REMOVE));
    }

    /** 
     * Removes current object entity from its DataMap. 
     */
    protected void removeObjEntity() {
        EventController mediator = getMediator();
        ObjEntity ent = mediator.getCurrentObjEntity();
        DataMap map = mediator.getCurrentDataMap();
        map.deleteObjEntity(ent.getName());
        mediator.fireObjEntityEvent(
            new EntityEvent(Editor.getFrame(), ent, EntityEvent.REMOVE));
    }

    protected void removeObjAttribute() {
        EventController mediator = getMediator();
        ObjEntity entity = mediator.getCurrentObjEntity();
        ObjAttribute attrib = mediator.getCurrentObjAttribute();
        entity.removeAttribute(attrib.getName());
        AttributeEvent e =
            new AttributeEvent(Editor.getFrame(), attrib, entity, AttributeEvent.REMOVE);
        mediator.fireObjAttributeEvent(e);
    }

    protected void removeDbAttribute() {
        EventController mediator = getMediator();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbAttribute attrib = mediator.getCurrentDbAttribute();
        entity.removeAttribute(attrib.getName());
        MapUtil.cleanObjMappings(mediator.getCurrentDataMap());

        AttributeEvent e =
            new AttributeEvent(Editor.getFrame(), attrib, entity, AttributeEvent.REMOVE);
        mediator.fireDbAttributeEvent(e);
    }

    protected void removeObjRelationship() {
        EventController mediator = getMediator();
        ObjEntity entity = mediator.getCurrentObjEntity();
        ObjRelationship rel = mediator.getCurrentObjRelationship();
        entity.removeRelationship(rel.getName());
        RelationshipEvent e =
            new RelationshipEvent(
                Editor.getFrame(),
                rel,
                entity,
                RelationshipEvent.REMOVE);
        mediator.fireObjRelationshipEvent(e);
    }

    protected void removeDbRelationship() {
        EventController mediator = getMediator();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbRelationship rel = mediator.getCurrentDbRelationship();
        entity.removeRelationship(rel.getName());
        MapUtil.cleanObjMappings(mediator.getCurrentDataMap());

        RelationshipEvent e =
            new RelationshipEvent(
                Editor.getFrame(),
                rel,
                entity,
                RelationshipEvent.REMOVE);
        mediator.fireDbRelationshipEvent(e);
    }

    protected void removeDataMapFromDataNode() {
        EventController mediator = getMediator();
        DataNode node = mediator.getCurrentDataNode();
        DataMap map = mediator.getCurrentDataMap();
        node.removeDataMap(map.getName());

        // Force reloading of the data node in the browse view
        mediator.fireDataNodeEvent(new DataNodeEvent(Editor.getFrame(), node));
    }

    /**
     * Returns <code>true</code> if last object in the path contains
     * a removable object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        Object lastObject = path.getObject();

        if (lastObject instanceof DataDomain) {
            return true;
        } else if (lastObject instanceof DataMap) {
            return true;
        } else if (lastObject instanceof DataNode) {
            return true;
        } else if (lastObject instanceof Entity) {
            return true;
        } else if (lastObject instanceof Attribute) {
            return true;
        } else if (lastObject instanceof Relationship) {
            return true;
        } else {
            return false;
        }
    }
}