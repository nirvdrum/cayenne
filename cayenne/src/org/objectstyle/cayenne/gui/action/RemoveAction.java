package org.objectstyle.cayenne.gui.action;
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

import java.awt.event.ActionEvent;
import java.util.List;
import java.io.File;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.AbstractAction;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.util.Preferences;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.AddDataMapDialog;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;


/** Removes proper Domain, DataNode, Entity, Attribute or Relationship.
 */
public class RemoveAction extends AbstractAction
{
	static Logger logObj = Logger.getLogger(RemoveAction.class.getName());

	Mediator mediator;

	public RemoveAction(Mediator temp_mediator) {
		mediator = temp_mediator;
	}
	
	public void actionPerformed(ActionEvent e) {
		remove();
	}
	
	private void remove() {
		if (mediator.getCurrentDbRelationship() != null)  {
			logObj.fine("current Db Rel is not null");
		}
		
		Object src = Editor.getFrame();
		if (mediator.getCurrentObjAttribute() != null) {
			removeObjAttribute();
		} else if (mediator.getCurrentDbAttribute() != null) {
			removeDbAttribute();
		} else if (mediator.getCurrentObjRelationship() != null) {
			removeObjRelationship();
		} else if (mediator.getCurrentDbRelationship() != null) {
			removeDbRelationship();
		} else if (mediator.getCurrentObjEntity() != null) {
			mediator.removeObjEntity(src, mediator.getCurrentObjEntity());
		} else if (mediator.getCurrentDbEntity() != null) {
			mediator.removeDbEntity(src, mediator.getCurrentDbEntity());
		} else if (mediator.getCurrentDataMap() != null) {
			// In context of Data node just remove from Data Node
			if (mediator.getCurrentDataNode() != null) {
				removeDataMapFromDataNode();
			// Not under Data Node? Remove completely
			} else {
				mediator.removeDataMap(src, mediator.getCurrentDataMap());
			}
		} else if (mediator.getCurrentDataNode() != null) {
			mediator.removeDataNode(src, mediator.getCurrentDataNode());
		} else if (mediator.getCurrentDataDomain() != null) {
			mediator.removeDomain(src, mediator.getCurrentDataDomain());
		}
	}
	
	private void removeObjAttribute(){
		ObjEntity entity = mediator.getCurrentObjEntity();
		ObjAttribute attrib = mediator.getCurrentObjAttribute();
		entity.removeAttribute(attrib.getName());
		AttributeEvent e;
		e = new AttributeEvent(Editor.getFrame(), attrib, entity
							 , AttributeEvent.REMOVE);
		mediator.fireObjAttributeEvent(e);
		AttributeDisplayEvent ev;
		ev = new AttributeDisplayEvent(Editor.getFrame(), null
									, entity, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain());
		mediator.fireObjAttributeDisplayEvent(ev);
	}
	
	private void removeDbAttribute(){
		DbEntity entity = mediator.getCurrentDbEntity();
		DbAttribute attrib = mediator.getCurrentDbAttribute();
		entity.removeAttribute(attrib.getName());
		AttributeEvent e;
		e = new AttributeEvent(Editor.getFrame(), attrib, entity
							 , AttributeEvent.REMOVE);
		mediator.fireDbAttributeEvent(e);
		AttributeDisplayEvent ev;
		ev = new AttributeDisplayEvent(Editor.getFrame(), null
									, entity, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain());
		mediator.fireDbAttributeDisplayEvent(ev);
	}
	

	private void removeObjRelationship(){
		ObjEntity entity = mediator.getCurrentObjEntity();
		ObjRelationship rel = mediator.getCurrentObjRelationship();
		entity.removeRelationship(rel.getName());
		RelationshipEvent e;
		e = new RelationshipEvent(Editor.getFrame(), rel, entity
								, RelationshipEvent.REMOVE);
		mediator.fireObjRelationshipEvent(e);
		RelationshipDisplayEvent ev;
		ev = new RelationshipDisplayEvent(Editor.getFrame(), null
									, entity, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain());
		mediator.fireObjRelationshipDisplayEvent(ev);
	}
	
	private void removeDbRelationship(){
		DbEntity entity = mediator.getCurrentDbEntity();
		DbRelationship rel = mediator.getCurrentDbRelationship();
		entity.removeRelationship(rel.getName());
		RelationshipEvent e;
		e = new RelationshipEvent(Editor.getFrame(), rel, entity
								, RelationshipEvent.REMOVE);
		mediator.fireDbRelationshipEvent(e);
		RelationshipDisplayEvent ev;
		ev = new RelationshipDisplayEvent(Editor.getFrame(), null
									, entity, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain());
		mediator.fireDbRelationshipDisplayEvent(ev);
	}
	
	private void removeDataMapFromDataNode(){
		DataNode node = mediator.getCurrentDataNode();
		DataMap map = mediator.getCurrentDataMap();
		// Get existing data maps
		Object[] maps = node.getDataMaps();
		DataMap[] arr = new DataMap[maps.length - 1];
		int j = 0;
		for (int i = 0; i < maps.length; i++) {
			DataMap temp = (DataMap)maps[i];
			if (temp == map) {
				logObj.fine("Skipping map " + map.getName());
				continue;
			}
			arr[j] = temp;
			j++;
		}
		node.setDataMaps(arr);
		// Force reloading of the data node in the browse view
		DataNodeEvent ev;
		ev = new DataNodeEvent(Editor.getFrame(), node);
		mediator.fireDataNodeEvent(ev);
	}
}