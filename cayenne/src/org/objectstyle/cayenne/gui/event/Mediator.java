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

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.GuiConfiguration;
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
  * */
public interface Mediator
{
	public GuiConfiguration getConfig();
	public ObjEntity getCurrentObjEntity();
	public DbEntity getCurrentDbEntity();
	public DataMap getCurrentDataMap();
	public DataNode getCurrentDataNode();
	public DataDomain getCurrentDataDomain();

	public DataDomain[] getDomains();
	
	public void removeDomain(Object src, DataDomain domain);
	public void removeObjEntity(Object event_src, ObjEntity entity);
	public void removeDbEntity(Object event_src, DbEntity entity);
	public void removeDataMap(Object event_src, DataMap map);
	public void addDataMap(Object event_src, DataMap map);
	public void addDataMap(Object event_src, DataMap map, boolean make_current);
	public void addDomain(Object src, DataDomain domain);
	public void addDomain(Object src, DataDomain domain, boolean make_current);
	
	public void addDomainDisplayListener(DomainDisplayListener listener);
	public void addDomainListener(DomainListener listener);
	public void addDataNodeDisplayListener(DataNodeDisplayListener listener);
	public void addDataNodeListener(DataNodeListener listener);
	public void addDataMapDisplayListener(DataMapDisplayListener listener);
	public void addDataMapListener(DataMapListener listener);
	public void addDbEntityListener(DbEntityListener listener);
	public void addObjEntityListener(ObjEntityListener listener);
	public void addDbEntityDisplayListener(DbEntityDisplayListener listener);
	public void addObjEntityDisplayListener(ObjEntityDisplayListener listener);	
	public void addDbAttributeListener(DbAttributeListener listener);
	public void addObjAttributeListener(ObjAttributeListener listener);
	public void addDbRelationshipListener(DbRelationshipListener listener);
	public void addObjRelationshipListener(ObjRelationshipListener listener);
	
	public void fireDomainDisplayEvent(DomainDisplayEvent e);
	public void fireDomainEvent(DomainEvent e);
	public void fireDataNodeDisplayEvent(DataNodeDisplayEvent e);
	public void fireDataNodeEvent(DataNodeEvent e);
	public void fireDataMapDisplayEvent(DataMapDisplayEvent e);
	public void fireDataMapEvent(DataMapEvent e);
	public void fireObjEntityEvent(EntityEvent e);
	public void fireDbEntityEvent(EntityEvent e);
	public void fireObjEntityDisplayEvent(EntityDisplayEvent e);
	public void fireDbEntityDisplayEvent(EntityDisplayEvent e);
	public void fireObjAttributeEvent(AttributeEvent e);
	public void fireDbAttributeEvent(AttributeEvent e);	
	public void fireObjRelationshipEvent(RelationshipEvent e);
	public void fireDbRelationshipEvent(RelationshipEvent e);	
	
	public boolean isDirty();
	public boolean isDirty(DataMap map);
	public boolean isDirty(DataDomain domain);
	public void setDirty(DataMap map);
	public void setDirty(DataDomain domain);
	
}