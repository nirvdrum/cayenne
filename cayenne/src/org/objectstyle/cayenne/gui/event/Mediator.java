package org.objectstyle.cayenne.gui.event;
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
import java.awt.Component;
import javax.swing.*;
import javax.swing.event.*;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.GuiConfiguration;
import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.util.*;


/** Interface for mediator of all views of DataMapEditor. 
  * DataMapEditor is implemented using Mediator pattern.
  * Views (Pane-s) responsible for the display of the 
  * group of teh properties of the DataMap elements,
  * such as Entities, attributes and relationships,
  * are going to listen to the different types of 
  * events and act as controllers. 
  * Do not make listeners out of models!
  * Models (table models, combo box models)
  * may create events, but they will assign the
  * view (pane) they belong to as the source of the event.
  * This is done to prevent eternal loops.
  * @author Michael Misha Shengaout */
public class Mediator
{
	//DataMapEditor parent;
	protected EventListenerList listenerList;
	/** The list of the currently open DataMapModel-s */
	DataDomain currentDomain 	= null;
	DataNode currentNode 		= null;
	DataMap currentMap 			= null;	
	ObjEntity currentObjEntity 	= null;
	DbEntity  currentDbEntity  	= null;
	ObjAttribute currentObjAttr = null;
	DbAttribute currentDbAttr 	= null;
	ObjRelationship currentObjRel=null;
	DbRelationship currentDbRel = null;
	
	/** The list of changed data domains.*/
	ArrayList dirtyDomains = new ArrayList();
	/** The list of changed data maps.*/
	ArrayList dirtyMaps = new ArrayList();
	/** The list of changed data nodes.*/
	ArrayList dirtyNodes = new ArrayList();
	
	GuiConfiguration config;
	/** Changes have been made, need to be saved. */
	boolean dirty;
	private static Mediator mediator;
	
	private Mediator()	{
		listenerList = new EventListenerList();
	}
	

	private Mediator(GuiConfiguration temp_config) {
		this();
		config = temp_config;
	}
	
	public static Mediator getMediator() {
		if (mediator == null) {
			mediator = new Mediator();
		}
		return mediator;
	}
	

	public static Mediator getMediator(GuiConfiguration gui_config) {
		mediator = new Mediator(gui_config);
		return mediator;
	}
	
	public GuiConfiguration getConfig() {
		return config;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public boolean isDirty(DataMap map) {
		if (dirtyMaps.contains(map))
			return true;
		return false;
	}
	
	public boolean isDirty(DataDomain domain) {
		if (dirtyDomains.contains(domain))
			return true;
		return false;
	}

	/** Makes data map current. This means also clearing the current
	  * entities, populating browse trees and making detail views invisible
	  * until entities in browse trees are selected. */
	public void setCurrentDataMap(DataMap model) {
		if (model == currentMap) {
			return;
		}
		currentMap = model;
		currentObjEntity = null;
		currentDbEntity = null;
	}
	
	/** Makes Db Attribute current. Clears Db Relationship.*/
	public void setCurrentDbAttribute(DbAttribute temp_attribute) {
		this.currentDbAttr = temp_attribute;
		this.currentDbRel = null;
	}
	
	/** Makes Obj Attribute current. Clears nothing.*/
	public void setCurrentObjAttribute(ObjAttribute temp_attribute) {
		this.currentObjAttr = temp_attribute;
	}
	
	/** Makes Db Relationship current. Clears Db Attribute.*/
	public void setCurrentDbRelationship(DbRelationship temp_rel) {
		this.currentDbRel = temp_rel;
		this.currentDbAttr = null;
	}
	
	/** Makes Obj Relationship current. Clears Obj Attribute.*/
	public void setCurrentObjRelationship(ObjRelationship temp_rel) {
		this.currentObjRel = temp_rel;
		this.currentObjAttr = null;
	}
	

	
	/** Resets all current models to null. */
	private void clearState() {
		currentDomain 	= null;
		currentNode 	= null;
		currentMap 		= null;	
		currentObjEntity= null;
		currentDbEntity = null;
		currentObjAttr 	= null;
		currentDbAttr	= null;
		currentObjRel	= null;
		currentDbRel	= null;
	}
	
	/** Makes data map current. This means also clearing the current
	  * entities, populating browse trees and making detail views invisible
	  * until entities in browse trees are selected. 
	  * @deprecated
	  * @see #setCurrentDataMap */
	public void setCurrentModel(DataMap model) {
		setCurrentDataMap(model);
	}
	
	
	/** Gets data map under specified name. 
	  * @deprecated
	  * @see #getDataMap */
	public DataMapWrapper getModel(String name) {
		return getDataMap(name);
	}


	/** Gets data map under specified name. */
	public DataMapWrapper getDataMap(String name)
	{
		return null;
	}

	public DataDomain[] getDomains() {
		java.util.List domains = config.getDomainList();
		if (null == domains)
			return new DataDomain[0];
		return (DataDomain[])domains.toArray(new DataDomain[domains.size()]);
	}

	
	public DataNode getCurrentDataNode() {return currentNode;}
	public DataDomain getCurrentDataDomain() {return currentDomain;}
	public DataMap getCurrentDataMap() {return currentMap;}
	public ObjEntity getCurrentObjEntity() {return currentObjEntity;}
	public DbEntity getCurrentDbEntity() {return currentDbEntity;}
	public ObjAttribute getCurrentObjAttribute() {return currentObjAttr;}
	public DbAttribute getCurrentDbAttribute() {return currentDbAttr;}
	public ObjRelationship getCurrentObjRelationship() {return currentObjRel;}
	public DbRelationship getCurrentDbRelationship() {return currentDbRel;}


	public void addDomainDisplayListener(DomainDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DomainDisplayListener", listener);
	}

	public void addDomainListener(DomainListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DomainListener", listener);
	}

	public void addDataNodeDisplayListener(DataNodeDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DataNodeDisplayListener", listener);
	}

	public void addDataNodeListener(DataNodeListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DataNodeListener", listener);
	}
	
	public void addDataMapDisplayListener(DataMapDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DataMapDisplayListener", listener);
	}

	public void addDataMapListener(DataMapListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DataMapListener", listener);
	}

	public void addDbEntityListener(DbEntityListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DbEntityListener", listener);
	}

	public void addObjEntityListener(ObjEntityListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.ObjEntityListener", listener);
	}

	public void addDbEntityDisplayListener(DbEntityDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DbEntityDisplayListener", listener);
	}

	public void addObjEntityDisplayListener(ObjEntityDisplayListener listener){
		addListener("org.objectstyle.cayenne.gui.event.ObjEntityDisplayListener", listener);
	}

	public void addDbAttributeListener(DbAttributeListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DbAttributeListener", listener);
	}

	public void addDbAttributeDisplayListener(DbAttributeDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DbAttributeDisplayListener", listener);
	}

	public void addObjAttributeListener(ObjAttributeListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.ObjAttributeListener", listener);		
	}

	public void addObjAttributeDisplayListener(ObjAttributeDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.ObjAttributeDisplayListener", listener);		
	}

	public void addDbRelationshipListener(DbRelationshipListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DbRelationshipListener", listener);
	}

	public void addDbRelationshipDisplayListener(DbRelationshipDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.DbRelationshipDisplayListener", listener);
	}

	public void addObjRelationshipListener(ObjRelationshipListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.ObjRelationshipListener", listener);
	}

	public void addObjRelationshipDisplayListener(ObjRelationshipDisplayListener listener) {
		addListener("org.objectstyle.cayenne.gui.event.ObjRelationshipDisplayListener", listener);
	}


	public void fireDomainDisplayEvent(DomainDisplayEvent e)
	{
		if (e.getDomain() == currentDomain)
			e.setDomainChanged(false);
		clearState();
		currentDomain = e.getDomain();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DomainDisplayListener");
		for (int i = 0; i < list.length; i++) {
			DomainDisplayListener temp = (DomainDisplayListener)list[i];
			temp.currentDomainChanged(e);
		}// End for()
	}
	

	/** Informs all listeners of the DomainEvent. 
	  * Does not send the event to its originator. */
	public void fireDomainEvent(DomainEvent e) {
		setDirty(true);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DomainListener");
		for (int i = 0; i < list.length; i++) {
			DomainListener temp = (DomainListener)list[i];
			switch(e.getId()) {
				case DomainEvent.ADD:
					temp.domainAdded(e);
					break;
				case DomainEvent.CHANGE:
					temp.domainChanged(e);
					break;
				case DomainEvent.REMOVE:
					temp.domainRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid DomainEvent type: " + e.getId());
			}// End switch
		}// End for()
		
	}


	public void fireDataNodeDisplayEvent(DataNodeDisplayEvent e)
	{
		if (e.getDataNode() == this.getCurrentDataNode())
			e.setDataNodeChanged(false);
		clearState();
		currentDomain = e.getDomain();
		currentNode = e.getDataNode();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DataNodeDisplayListener");
		for (int i = 0; i < list.length; i++) {
			DataNodeDisplayListener temp = (DataNodeDisplayListener)list[i];
			temp.currentDataNodeChanged(e);
		}// End for()
	}
	

	/** Informs all listeners of the DataNodeEvent. 
	  * Does not send the event to its originator. */
	public void fireDataNodeEvent(DataNodeEvent e) {
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DataNodeListener");
		for (int i = 0; i < list.length; i++) {
			DataNodeListener temp = (DataNodeListener)list[i];
			switch(e.getId()) {
				case DataNodeEvent.ADD:
					temp.dataNodeAdded(e);
					setDirty(e.getDataNode());
					break;
				case DataNodeEvent.CHANGE:
					temp.dataNodeChanged(e);
					setDirty(currentDomain);
					setDirty(e.getDataNode());
					break;
				case DataNodeEvent.REMOVE:
					temp.dataNodeRemoved(e);
					dirtyNodes.remove(e.getDataNode());
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid DataNodeEvent type: " + e.getId());
			}// End switch
		}// End for()
		
	}




	public void fireDataMapDisplayEvent(DataMapDisplayEvent e)
	{
		if (e.getDataMap() == this.getCurrentDataMap())
			e.setDataMapChanged(false);
		clearState();
		currentMap = e.getDataMap();
		currentDomain = e.getDomain();
		currentNode = e.getDataNode();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DataMapDisplayListener");
		for (int i = 0; i < list.length; i++) {
			DataMapDisplayListener temp = (DataMapDisplayListener)list[i];
			temp.currentDataMapChanged(e);
		}// End for()
	}
	

	/** Informs all listeners of the DataMapEvent. 
	  * Does not send the event to its originator. */
	public void fireDataMapEvent(DataMapEvent e) {
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DataMapListener");
		for (int i = 0; i < list.length; i++) {
			DataMapListener temp = (DataMapListener)list[i];
			switch(e.getId()) {
				case DataMapEvent.ADD:
					temp.dataMapAdded(e);
					setDirty(e.getDataMap());
					setDirty(currentDomain);
					break;
				case DataMapEvent.CHANGE:
					temp.dataMapChanged(e);
					setDirty(e.getDataMap());
					break;
				case DataMapEvent.REMOVE:
					temp.dataMapRemoved(e);
					dirtyMaps.remove(e.getDataMap());
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid DataMapEvent type: " + e.getId());
			}// End switch
		}// End for()
		
	}

	/** Informs all listeners of the EntityEvent. 
	  * Does not send the event to its originator. */
	public void fireObjEntityEvent(EntityEvent e) {
		setDirty(currentMap);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.ObjEntityListener");
		for (int i = 0; i < list.length; i++) {
			ObjEntityListener temp = (ObjEntityListener)list[i];
			switch(e.getId()) {
				case EntityEvent.ADD:
					temp.objEntityAdded(e);
					break;
				case EntityEvent.CHANGE:
					temp.objEntityChanged(e);
					break;
				case EntityEvent.REMOVE:
					temp.objEntityRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid EntityEvent type: " + e.getId());
			}// End switch
		}// End for()
	}
	
	/** Informs all listeners of the EntityEvent. 
	  * Does not send the event to its originator. */
	public void fireDbEntityEvent(EntityEvent e) 
	{
		setDirty(currentMap);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DbEntityListener");
		for (int i = 0; i < list.length; i++) {
			DbEntityListener temp = (DbEntityListener)list[i];
			switch(e.getId()) {
				case EntityEvent.ADD:
					temp.dbEntityAdded(e);
					break;
				case EntityEvent.CHANGE:
					temp.dbEntityChanged(e);
					break;
				case EntityEvent.REMOVE:
					temp.dbEntityRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid EntityEvent type: " + e.getId());
			}// End switch
		}// End for()
	}
	
	public void fireObjEntityDisplayEvent(EntityDisplayEvent e)
	{
		if (currentObjEntity == e.getEntity())
			e.setEntityChanged(false);
		clearState();
		currentDomain = e.getDomain();
		currentNode = e.getDataNode();
		currentMap = e.getDataMap();
			
		currentObjEntity = (ObjEntity)e.getEntity();		
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.ObjEntityDisplayListener");
		for (int i = 0; i < list.length; i++) {
			ObjEntityDisplayListener temp = (ObjEntityDisplayListener)list[i];
			temp.currentObjEntityChanged(e);
		}// End for()
	}
	
	public void fireDbEntityDisplayEvent(EntityDisplayEvent e){
		if (currentDbEntity == e.getEntity())
			e.setEntityChanged(false);
		clearState();
		currentDomain = e.getDomain();
		currentNode = e.getDataNode();
		currentMap = e.getDataMap();
		currentDbEntity = (DbEntity)e.getEntity();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DbEntityDisplayListener");
		for (int i = 0; i < list.length; i++) {
			DbEntityDisplayListener temp = (DbEntityDisplayListener)list[i];
			temp.currentDbEntityChanged(e);
		}// End for()
	}
	
	/** Notifies all listeners of the change(add, remove) and does the change.*/
	public void fireDbAttributeEvent(AttributeEvent e) 
	{
		setDirty(currentMap);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DbAttributeListener");
		for (int i = 0; i < list.length; i++) {
			DbAttributeListener temp = (DbAttributeListener)list[i];
			switch(e.getId()) {
				case AttributeEvent.ADD:
					temp.dbAttributeAdded(e);
					break;
				case AttributeEvent.CHANGE:
					temp.dbAttributeChanged(e);
					break;
				case AttributeEvent.REMOVE:
					temp.dbAttributeRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid AttributeEvent type: " + e.getId());
			}// End switch
		}// End for()
	}

	public void fireDbAttributeDisplayEvent(AttributeDisplayEvent e)
	{
		if (e.getAttribute() == this.getCurrentDbAttribute())
			e.setAttributeChanged(false);
		this.fireDbEntityDisplayEvent(e);
		clearState();
		// Must follow DbEntityDisplayEvent, 
		// as it resets curr Attr and Rel values to null.
		currentDbAttr = (DbAttribute)e.getAttribute();
		this.currentDbEntity = (DbEntity)e.getEntity();
		this.currentMap = e.getDataMap();
		this.currentDomain = e.getDomain();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DbAttributeDisplayListener");
		for (int i = 0; i < list.length; i++) {
			DbAttributeDisplayListener temp = (DbAttributeDisplayListener)list[i];
			temp.currentDbAttributeChanged(e);
		}// End for()
		currentDbAttr = (DbAttribute)e.getAttribute();
	}
	

	/** Notifies all listeners of the change (add, remove) and does the change.*/
	public void fireObjAttributeEvent(AttributeEvent e) 
	{
		setDirty(currentMap);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.ObjAttributeListener");
		for (int i = 0; i < list.length; i++) {
			ObjAttributeListener temp = (ObjAttributeListener)list[i];
			switch(e.getId()) {
				case AttributeEvent.ADD:
					temp.objAttributeAdded(e);
					break;
				case AttributeEvent.CHANGE:
					temp.objAttributeChanged(e);
					break;
				case AttributeEvent.REMOVE:
					temp.objAttributeRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid AttributeEvent type: " + e.getId());
			}// End switch
		}// End for()
	}

	public void fireObjAttributeDisplayEvent(AttributeDisplayEvent e)
	{
		if (e.getAttribute() == this.getCurrentObjAttribute())
			e.setAttributeChanged(false);
		this.fireObjEntityDisplayEvent(e);
		// Must follow ObjEntityDisplayEvent, 
		// as it resets curr Attr and Rel values to null.
		currentObjAttr = (ObjAttribute)e.getAttribute();
		this.currentObjEntity = (ObjEntity)e.getEntity();
		this.currentMap = e.getDataMap();
		this.currentDomain = e.getDomain();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.ObjAttributeDisplayListener");
		for (int i = 0; i < list.length; i++) {
			ObjAttributeDisplayListener temp = (ObjAttributeDisplayListener)list[i];
			temp.currentObjAttributeChanged(e);
		}// End for()
	}
	
	/** Notifies all listeners of the change(add, remove) and does the change.*/
	public void fireDbRelationshipEvent(RelationshipEvent e) 
	{
		setDirty(currentMap);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DbRelationshipListener");
		for (int i = 0; i < list.length; i++) {
			DbRelationshipListener temp = (DbRelationshipListener)list[i];
			switch(e.getId()) {
				case RelationshipEvent.ADD:
					temp.dbRelationshipAdded(e);
					break;
				case RelationshipEvent.CHANGE:
					temp.dbRelationshipChanged(e);
					break;
				case RelationshipEvent.REMOVE:
					temp.dbRelationshipRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid RelationshipEvent type: " + e.getId());
			}// End switch
		}// End for()
	}

	public void fireDbRelationshipDisplayEvent(RelationshipDisplayEvent e)
	{
		if (e.getRelationship() == this.getCurrentDbRelationship())
			e.setRelationshipChanged(false);
		this.fireDbEntityDisplayEvent(e);
		this.currentDbEntity = (DbEntity)e.getEntity();
		this.currentMap = e.getDataMap();
		this.currentDomain = e.getDomain();
		// Must follow DbEntityDisplayEvent, 
		// as it resets curr Attr and Rel values to null.
		currentDbRel = (DbRelationship)e.getRelationship();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.DbRelationshipDisplayListener");
		for (int i = 0; i < list.length; i++) {
			DbRelationshipDisplayListener temp = (DbRelationshipDisplayListener)list[i];
			temp.currentDbRelationshipChanged(e);
		}// End for()
	}
	


	/** Notifies all listeners of the change(add, remove) and does the change.*/
	public void fireObjRelationshipEvent(RelationshipEvent e) 
	{
		setDirty(currentMap);
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.ObjRelationshipListener");
		for (int i = 0; i < list.length; i++) {
			ObjRelationshipListener temp = (ObjRelationshipListener)list[i];
			switch(e.getId()) {
				case RelationshipEvent.ADD:
					temp.objRelationshipAdded(e);
					break;
				case RelationshipEvent.CHANGE:
					temp.objRelationshipChanged(e);
					break;
				case RelationshipEvent.REMOVE:
					temp.objRelationshipRemoved(e);
					break;
				default:
					throw new IllegalArgumentException(
								"Invalid RelationshipEvent type: " + e.getId());
			}// End switch
		}// End for()
	}

	public void fireObjRelationshipDisplayEvent(RelationshipDisplayEvent e)
	{
		if (e.getRelationship() == this.getCurrentObjRelationship())
			e.setRelationshipChanged(false);
		this.fireObjEntityDisplayEvent(e);
		// Must follow DbEntityDisplayEvent, 
		// as it resets curr Attr and Rel values to null.
		currentObjRel = (ObjRelationship)e.getRelationship();
		this.currentObjEntity = (ObjEntity)e.getEntity();
		this.currentMap = e.getDataMap();
		this.currentDomain = e.getDomain();
		EventListener[] list;
		list = getListeners("org.objectstyle.cayenne.gui.event.ObjRelationshipDisplayListener");
		for (int i = 0; i < list.length; i++) {
			ObjRelationshipDisplayListener temp = (ObjRelationshipDisplayListener)list[i];
			temp.currentObjRelationshipChanged(e);
		}// End for()
		
	}
	


	/** Clean remove of the ObjEntity from map and all views. 
	  * Removes entity from the map. 
	  * If entity is current entity and last entity in the map, 
	  * hide the detail views by sending <code>null</code> as current entity.
	  * Otherwise select another current entity. */
	public void removeObjEntity(Object src, ObjEntity entity) {
		java.util.List list = currentMap.getObjEntitiesAsList();
		int index = list.indexOf(entity);
		currentMap.deleteObjEntity(entity.getName());
		if (entity != currentObjEntity)
			return;
		// If no more entities (1 - the one that was deleted),
		// hide detail view by firing event with no entity
		if (list.size() <= 1)
			fireObjEntityDisplayEvent(new EntityDisplayEvent(this
													, null
													, currentMap
													, currentNode
													, currentDomain));
		else {
			ObjEntity temp = (ObjEntity)list.get(index < list.size() - 1 
												? index 
												: index-1);
			fireObjEntityDisplayEvent(new EntityDisplayEvent(this
													, temp
													, currentMap
													, currentNode
													, currentDomain));
		}
		fireObjEntityEvent(new EntityEvent(src, entity, EntityEvent.REMOVE));
	}

	/** Clean remove of the DbEntity from map and all views. 
	  * Removes entity from the map. 
	  * If entity is current entity and last entity in the map, 
	  * hide the detail views by sending <code>null</code> as current entity.
	  * Otherwise select another current entity. */
	public void removeDbEntity(Object src, DbEntity entity) {
		java.util.List list = currentMap.getDbEntitiesAsList();
		int index = list.indexOf(entity);
		currentMap.deleteDbEntity(entity.getName());
		if (entity != currentDbEntity)
			return;
		// If no more entities (1 - the one that was deleted),
		// hide detail view by firing event with no entity
		if (list.size() <= 1)
			fireDbEntityDisplayEvent(new EntityDisplayEvent(src
													, null
													, currentMap
													, currentNode
													, currentDomain));
		else {
			DbEntity temp = (DbEntity)list.get(index < list.size() - 1 
												? index 
												: index-1);
			fireDbEntityDisplayEvent(new EntityDisplayEvent(src
													, temp
													, currentMap
													, currentNode
													, currentDomain));
		}
		fireDbEntityEvent(new EntityEvent(src, entity, EntityEvent.REMOVE));
	}	
	
	public void removeDataMap(Object src, DataMap map) {
		currentDomain.removeMap(map.getName());
		fireDataMapEvent(new DataMapEvent(src, map, DataMapEvent.REMOVE));
		fireDataMapDisplayEvent(new DataMapDisplayEvent(src
												, null
												, currentDomain
												, currentNode));
	}
	

	public void addDataMap(Object src, DataMap wrap){
		addDataMap(src, wrap, true);
	}
	
	public void addDataMap(Object src, DataMap map, boolean make_current)
	{
		currentDomain.addMap(map);
		fireDataMapEvent(new DataMapEvent(src, map, DataMapEvent.ADD));
		if (make_current)
			fireDataMapDisplayEvent(new DataMapDisplayEvent(src
												, map
												, currentDomain
												, currentNode));
	}


	public void removeDomain(Object src, DataDomain domain) {
		config.removeDomain(domain.getName());
		dirtyDomains.remove(domain);
		setDirty(true);
		java.util.List list = domain.getMapList();
		Iterator iter = list.iterator();
		while (iter.hasNext())
			dirtyMaps.remove(iter.next());
		fireDomainEvent(new DomainEvent(src, domain, DomainEvent.REMOVE));
		fireDomainDisplayEvent(new DomainDisplayEvent(src, null));
	}


	public void removeDataNode(Object src, DataNode node) {
		currentDomain.removeDataNode(node.getName());
		fireDataNodeEvent(new DataNodeEvent(src, node, DataNodeEvent.REMOVE));
		fireDataNodeDisplayEvent(new DataNodeDisplayEvent(src, currentDomain, null));
	}
	
	public void addDomain(Object src, DataDomain domain) {
		addDomain(src, domain, true);
	}
	
	public void addDomain(Object src, DataDomain domain, boolean make_current) 
	{
		config.addDomain(domain);
		fireDomainEvent(new DomainEvent(src, domain, DomainEvent.ADD));
		if (make_current)
			fireDomainDisplayEvent(new DomainDisplayEvent(src, domain));
	}

	private void addListener(String name, EventListener listener) {
		Class temp_class;
		try {
			temp_class = Class.forName(name);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		listenerList.add(temp_class, listener);
	}

	private EventListener[] getListeners(String class_name) {
		Class temp_class;
		try {
			temp_class = Class.forName(class_name);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		EventListener[] list = listenerList.getListeners(temp_class);
		return list;
	}
	
	public void setDirty(boolean temp_dirty) {
		if (dirty == temp_dirty)
			return;
		dirty = temp_dirty;
		Editor.getFrame().setDirty(dirty);
	}
	
	public void setDirty(DataMap map) {
		if (dirtyMaps.contains(map))
			return;
		dirtyMaps.add(map);
		setDirty(true);
	}

	public void setDirty(DataNode node) {
		if (dirtyNodes.contains(node))
			return;
		dirtyNodes.add(node);
		setDirty(true);
	}

	public void setDirty(DataDomain domain) {
		if (dirtyDomains.contains(domain))
			return;
		dirtyDomains.add(domain);
		setDirty(true);
	}
	
	public ArrayList getDirtyDataMaps(){
		return dirtyMaps;
	}

	public ArrayList getDirtyDataNodes(){
		return dirtyNodes;
	}

	public ArrayList getDirtyDomains(){
		return dirtyDomains;
	}
}