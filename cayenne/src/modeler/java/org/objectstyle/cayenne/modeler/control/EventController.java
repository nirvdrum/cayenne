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
package org.objectstyle.cayenne.modeler.control;

import java.util.EventListener;
import java.util.EventObject;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.DataMapListener;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.map.event.DataNodeListener;
import org.objectstyle.cayenne.map.event.DbAttributeListener;
import org.objectstyle.cayenne.map.event.DbEntityListener;
import org.objectstyle.cayenne.map.event.DbRelationshipListener;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.map.event.DomainListener;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.map.event.ObjAttributeListener;
import org.objectstyle.cayenne.map.event.ObjEntityListener;
import org.objectstyle.cayenne.map.event.ObjRelationshipListener;
import org.objectstyle.cayenne.map.event.ProcedureEvent;
import org.objectstyle.cayenne.map.event.ProcedureListener;
import org.objectstyle.cayenne.map.event.ProcedureParameterEvent;
import org.objectstyle.cayenne.map.event.ProcedureParameterListener;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.event.AttributeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbAttributeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbRelationshipDisplayListener;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjAttributeDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjRelationshipDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureParameterDisplayListener;
import org.objectstyle.cayenne.modeler.event.RelationshipDisplayEvent;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectPath;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/** 
 * Implementation of event dispatching in CayenneModeler using <i>mediator</i>
 * design pattern. 
 * 
 * <p>TODO: Refactor the event model, so that events are generic and contain "path"
 * to a project node in question. After this is done, EventController should no longer
 * maintain the selection model (currentXYZ ivars), rather it should update internal model.  
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class EventController extends ModelerController {

    private static Logger logObj = Logger.getLogger(EventController.class);

    protected EventListenerList listenerList;

    protected DataDomain currentDomain;
    protected DataNode currentNode;
    protected DataMap currentMap;
    protected ObjEntity currentObjEntity;
    protected DbEntity currentDbEntity;
    protected ObjAttribute currentObjAttr;
    protected DbAttribute currentDbAttr;
    protected ObjRelationship currentObjRel;
    protected DbRelationship currentDbRel;
    protected Procedure currentProcedure;
    protected ProcedureParameter currentProcedureParameter;

    /** Changes have been made, need to be saved. */
    protected boolean dirty;

    public EventController(ModelerController parent) {
        super(parent);
        this.listenerList = new EventListenerList();
    }

    /**
      * Performs control handling.
      */
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(PROJECT_CLOSED_ID)) {
            reset();
        }
        else if (control.matchesID(PROJECT_OPENED_ID)) {
            setDirty(((Project) control.getParameter()).isLocationUndefined());
            addDataNodeDisplayListener(CayenneModelerFrame.getFrame());
            addDataMapDisplayListener(CayenneModelerFrame.getFrame());
            addObjEntityDisplayListener(CayenneModelerFrame.getFrame());
            addDbEntityDisplayListener(CayenneModelerFrame.getFrame());
            addObjAttributeDisplayListener(CayenneModelerFrame.getFrame());
            addDbAttributeDisplayListener(CayenneModelerFrame.getFrame());
            addObjRelationshipDisplayListener(CayenneModelerFrame.getFrame());
            addDbRelationshipDisplayListener(CayenneModelerFrame.getFrame());
            addProcedureDisplayListener(CayenneModelerFrame.getFrame());
            addProcedureParameterDisplayListener(CayenneModelerFrame.getFrame());
        }
    }

    /**
     * Sends control to parent controller.
     */
    protected void sendControlToParent(String id, Object parameter) {
        if (getParent() != null) {
            getParent().handleControl(new Control(id, parameter));
        }
    }

    public void reset() {
        clearState();
        setDirty(false);
        listenerList = new EventListenerList();
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Resets all current models to null. */
    private void clearState() {
        currentDomain = null;
        currentNode = null;
        currentMap = null;
        currentObjEntity = null;
        currentDbEntity = null;
        currentObjAttr = null;
        currentDbAttr = null;
        currentObjRel = null;
        currentDbRel = null;
        currentProcedure = null;
        currentProcedureParameter = null;
        getTopModel().setSelectedPath(ProjectPath.EMPTY_PATH);
    }

    public DataNode getCurrentDataNode() {
        return currentNode;
    }

    public DataDomain getCurrentDataDomain() {
        return currentDomain;
    }

    public DataMap getCurrentDataMap() {
        return currentMap;
    }

    public ObjEntity getCurrentObjEntity() {
        return currentObjEntity;
    }

    public DbEntity getCurrentDbEntity() {
        return currentDbEntity;
    }

    public ObjAttribute getCurrentObjAttribute() {
        return currentObjAttr;
    }

    public DbAttribute getCurrentDbAttribute() {
        return currentDbAttr;
    }

    public ObjRelationship getCurrentObjRelationship() {
        return currentObjRel;
    }

    public DbRelationship getCurrentDbRelationship() {
        return currentDbRel;
    }

    public Procedure getCurrentProcedure() {
        return currentProcedure;
    }

    public ProcedureParameter getCurrentProcedureParameter() {
        return currentProcedureParameter;
    }

    public void addDomainDisplayListener(DomainDisplayListener listener) {
        addListener(DomainDisplayListener.class, listener);
    }

    public void addDomainListener(DomainListener listener) {
        addListener(DomainListener.class, listener);
    }

    public void addDataNodeDisplayListener(DataNodeDisplayListener listener) {
        addListener(DataNodeDisplayListener.class, listener);
    }

    public void addDataNodeListener(DataNodeListener listener) {
        addListener(DataNodeListener.class, listener);
    }

    public void addDataMapDisplayListener(DataMapDisplayListener listener) {
        addListener(DataMapDisplayListener.class, listener);
    }

    public void addDataMapListener(DataMapListener listener) {
        addListener(DataMapListener.class, listener);
    }

    public void addDbEntityListener(DbEntityListener listener) {
        addListener(DbEntityListener.class, listener);
    }

    public void addObjEntityListener(ObjEntityListener listener) {
        addListener(ObjEntityListener.class, listener);
    }

    public void addDbEntityDisplayListener(DbEntityDisplayListener listener) {
        addListener(DbEntityDisplayListener.class, listener);
    }

    public void addObjEntityDisplayListener(ObjEntityDisplayListener listener) {
        addListener(ObjEntityDisplayListener.class, listener);
    }

    public void addDbAttributeListener(DbAttributeListener listener) {
        addListener(DbAttributeListener.class, listener);
    }

    public void addDbAttributeDisplayListener(DbAttributeDisplayListener listener) {
        addListener(DbAttributeDisplayListener.class, listener);
    }

    public void addObjAttributeListener(ObjAttributeListener listener) {
        addListener(ObjAttributeListener.class, listener);
    }

    public void addObjAttributeDisplayListener(ObjAttributeDisplayListener listener) {
        addListener(ObjAttributeDisplayListener.class, listener);
    }

    public void addDbRelationshipListener(DbRelationshipListener listener) {
        addListener(DbRelationshipListener.class, listener);
    }

    public void addDbRelationshipDisplayListener(DbRelationshipDisplayListener listener) {
        addListener(DbRelationshipDisplayListener.class, listener);
    }

    public void addObjRelationshipListener(ObjRelationshipListener listener) {
        addListener(ObjRelationshipListener.class, listener);
    }

    public void addObjRelationshipDisplayListener(ObjRelationshipDisplayListener listener) {
        addListener(ObjRelationshipDisplayListener.class, listener);
    }

    public void addProcedureDisplayListener(ProcedureDisplayListener listener) {
        addListener(ProcedureDisplayListener.class, listener);
    }

    public void addProcedureListener(ProcedureListener listener) {
        addListener(ProcedureListener.class, listener);
    }

    public void addProcedureParameterListener(ProcedureParameterListener listener) {
        addListener(ProcedureParameterListener.class, listener);
    }

    public void addProcedureParameterDisplayListener(ProcedureParameterDisplayListener listener) {
        addListener(ProcedureParameterDisplayListener.class, listener);
    }

    public void fireDomainDisplayEvent(DomainDisplayEvent e) {
        if (e.getDomain() == currentDomain) {
            e.setDomainChanged(false);
        }

        clearState();

        currentDomain = e.getDomain();
        getTopModel().setSelectedPath(currentDomain);

        EventListener[] list = getListeners(DomainDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DomainDisplayListener temp = (DomainDisplayListener) list[i];
            temp.currentDomainChanged(e);
        }

        // also send control to parent
        sendControlToParent(DATA_DOMAIN_SELECTED_ID, currentDomain);
    }

    /** Informs all listeners of the DomainEvent. 
      * Does not send the event to its originator. */
    public void fireDomainEvent(DomainEvent e) {
        setDirty(true);

        EventListener[] list = getListeners(DomainListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DomainListener temp = (DomainListener) list[i];
            switch (e.getId()) {
                case DomainEvent.ADD :
                    temp.domainAdded(e);
                    break;
                case DomainEvent.CHANGE :
                    temp.domainChanged(e);
                    break;
                case DomainEvent.REMOVE :
                    temp.domainRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid DomainEvent type: " + e.getId());
            }
        }
    }

    public void fireDataNodeDisplayEvent(DataNodeDisplayEvent e) {
        if (e.getDataNode() == this.getCurrentDataNode())
            e.setDataNodeChanged(false);
        clearState();
        currentDomain = e.getDomain();
        currentNode = e.getDataNode();
        EventListener[] list = getListeners(DataNodeDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ((DataNodeDisplayListener) list[i]).currentDataNodeChanged(e);
        }
    }

    /** Informs all listeners of the DataNodeEvent. 
      * Does not send the event to its originator. */
    public void fireDataNodeEvent(DataNodeEvent e) {
        EventListener[] list = getListeners(DataNodeListener.class);
        debugEvent(e, list);

        // FIXME: "dirty" flag and other procesisng is
        // done in the loop. Loop should only care about 
        // notifications...
        for (int i = 0; i < list.length; i++) {
            DataNodeListener temp = (DataNodeListener) list[i];
            switch (e.getId()) {
                case DataNodeEvent.ADD :
                    temp.dataNodeAdded(e);
                    setDirty(true);
                    break;
                case DataNodeEvent.CHANGE :
                    temp.dataNodeChanged(e);
                    setDirty(true);
                    break;
                case DataNodeEvent.REMOVE :
                    temp.dataNodeRemoved(e);
                    setDirty(true);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid DataNodeEvent type: " + e.getId());
            }
        }

    }

    public void fireDataMapDisplayEvent(DataMapDisplayEvent e) {
        if (e.getDataMap() == this.getCurrentDataMap())
            e.setDataMapChanged(false);
        clearState();
        currentMap = e.getDataMap();
        currentDomain = e.getDomain();
        currentNode = e.getDataNode();
        EventListener[] list = getListeners(DataMapDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DataMapDisplayListener temp = (DataMapDisplayListener) list[i];
            temp.currentDataMapChanged(e);
        }

    }

    /** Informs all listeners of the DataMapEvent. 
      * Does not send the event to its originator. */
    public void fireDataMapEvent(DataMapEvent e) {
        EventListener[] list = getListeners(DataMapListener.class);
        debugEvent(e, list);

        for (int i = 0; i < list.length; i++) {
            DataMapListener temp = (DataMapListener) list[i];
            switch (e.getId()) {
                case DataMapEvent.ADD :
                    temp.dataMapAdded(e);
                    setDirty(true);
                    break;
                case DataMapEvent.CHANGE :
                    temp.dataMapChanged(e);
                    setDirty(true);
                    break;
                case DataMapEvent.REMOVE :
                    temp.dataMapRemoved(e);
                    setDirty(true);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid DataMapEvent type: " + e.getId());
            }
        }
    }

    /** Informs all listeners of the EntityEvent. 
      * Does not send the event to its originator. */
    public void fireObjEntityEvent(EntityEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(ObjEntityListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ObjEntityListener temp = (ObjEntityListener) list[i];
            switch (e.getId()) {
                case EntityEvent.ADD :
                    temp.objEntityAdded(e);
                    break;
                case EntityEvent.CHANGE :
                    temp.objEntityChanged(e);
                    break;
                case EntityEvent.REMOVE :
                    temp.objEntityRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid EntityEvent type: " + e.getId());
            }
        }
    }

    /** Informs all listeners of the EntityEvent. 
      * Does not send the event to its originator. */
    public void fireDbEntityEvent(EntityEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(DbEntityListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DbEntityListener temp = (DbEntityListener) list[i];
            switch (e.getId()) {
                case EntityEvent.ADD :
                    temp.dbEntityAdded(e);
                    break;
                case EntityEvent.CHANGE :
                    temp.dbEntityChanged(e);
                    break;
                case EntityEvent.REMOVE :
                    temp.dbEntityRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid EntityEvent type: " + e.getId());
            }
        }
    }

    /** 
     * Informs all listeners of the ProcedureEvent. 
     * Does not send the event to its originator. 
     */
    public void fireProcedureEvent(ProcedureEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(ProcedureListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ProcedureListener listener = (ProcedureListener) list[i];
            switch (e.getId()) {
                case EntityEvent.ADD :
                    listener.procedureAdded(e);
                    break;
                case EntityEvent.CHANGE :
                    listener.procedureChanged(e);
                    break;
                case EntityEvent.REMOVE :
                    listener.procedureRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid ProcedureEvent type: " + e.getId());
            }
        }
    }

    /** 
       * Informs all listeners of the ProcedureEvent. 
       * Does not send the event to its originator. 
       */
    public void fireProcedureParameterEvent(ProcedureParameterEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(ProcedureParameterListener.class);
        for (int i = 0; i < list.length; i++) {
            ProcedureParameterListener listener = (ProcedureParameterListener) list[i];
            switch (e.getId()) {
                case EntityEvent.ADD :
                    listener.procedureParameterAdded(e);
                    break;
                case EntityEvent.CHANGE :
                    listener.procedureParameterChanged(e);
                    break;
                case EntityEvent.REMOVE :
                    listener.procedureParameterRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid ProcedureParameterEvent type: " + e.getId());
            }
        }
    }

    public void fireObjEntityDisplayEvent(EntityDisplayEvent e) {
        if (currentObjEntity == e.getEntity())
            e.setEntityChanged(false);
        clearState();
        currentDomain = e.getDomain();
        currentNode = e.getDataNode();
        currentMap = e.getDataMap();

        currentObjEntity = (ObjEntity) e.getEntity();
        EventListener[] list = getListeners(ObjEntityDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ObjEntityDisplayListener temp = (ObjEntityDisplayListener) list[i];
            temp.currentObjEntityChanged(e);
        }
    }

    public void fireProcedureDisplayEvent(ProcedureDisplayEvent e) {
        if (currentProcedure == e.getProcedure())
            e.setProcedureChanged(false);

        clearState();

        currentDomain = e.getDomain();
        currentMap = e.getDataMap();
        currentProcedure = e.getProcedure();

        EventListener[] list = getListeners(ProcedureDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ProcedureDisplayListener listener = (ProcedureDisplayListener) list[i];
            listener.currentProcedureChanged(e);
        }
    }

    public void fireProcedureParameterDisplayEvent(ProcedureParameterDisplayEvent e) {
        if (currentProcedure == e.getProcedure())
            e.setProcedureChanged(false);

        clearState();

        currentDomain = e.getDomain();
        currentMap = e.getDataMap();
        currentProcedure = e.getProcedure();
        currentProcedureParameter = e.getProcedureParameter();

        EventListener[] list = getListeners(ProcedureParameterDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ProcedureParameterDisplayListener listener =
                (ProcedureParameterDisplayListener) list[i];
            listener.currentProcedureParameterChanged(e);
        }

    }

    public void fireDbEntityDisplayEvent(EntityDisplayEvent e) {
        if (currentDbEntity == e.getEntity())
            e.setEntityChanged(false);
        clearState();
        currentDomain = e.getDomain();
        currentNode = e.getDataNode();
        currentMap = e.getDataMap();
        currentDbEntity = (DbEntity) e.getEntity();
        EventListener[] list = getListeners(DbEntityDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DbEntityDisplayListener temp = (DbEntityDisplayListener) list[i];
            temp.currentDbEntityChanged(e);
        }
    }

    /** Notifies all listeners of the change(add, remove) and does the change.*/
    public void fireDbAttributeEvent(AttributeEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(DbAttributeListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DbAttributeListener temp = (DbAttributeListener) list[i];
            switch (e.getId()) {
                case AttributeEvent.ADD :
                    temp.dbAttributeAdded(e);
                    break;
                case AttributeEvent.CHANGE :
                    temp.dbAttributeChanged(e);
                    break;
                case AttributeEvent.REMOVE :
                    temp.dbAttributeRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid AttributeEvent type: " + e.getId());
            }
        }
    }

    public void fireDbAttributeDisplayEvent(AttributeDisplayEvent e) {
        this.fireDbEntityDisplayEvent(e);
        clearState();
        // Must follow DbEntityDisplayEvent, 
        // as it resets curr Attr and Rel values to null.
        this.currentDbAttr = (DbAttribute) e.getAttribute();
        this.currentDbEntity = (DbEntity) e.getEntity();
        this.currentMap = e.getDataMap();
        this.currentDomain = e.getDomain();

        EventListener[] list = getListeners(DbAttributeDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DbAttributeDisplayListener temp = (DbAttributeDisplayListener) list[i];
            temp.currentDbAttributeChanged(e);
        }
    }

    /** Notifies all listeners of the change (add, remove) and does the change.*/
    public void fireObjAttributeEvent(AttributeEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(ObjAttributeListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ObjAttributeListener temp = (ObjAttributeListener) list[i];
            switch (e.getId()) {
                case AttributeEvent.ADD :
                    temp.objAttributeAdded(e);
                    break;
                case AttributeEvent.CHANGE :
                    temp.objAttributeChanged(e);
                    break;
                case AttributeEvent.REMOVE :
                    temp.objAttributeRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid AttributeEvent type: " + e.getId());
            }
        }
    }

    public void fireObjAttributeDisplayEvent(AttributeDisplayEvent e) {
        this.fireObjEntityDisplayEvent(e);
        // Must follow ObjEntityDisplayEvent, 
        // as it resets curr Attr and Rel values to null.
        this.currentObjAttr = (ObjAttribute) e.getAttribute();
        this.currentObjEntity = (ObjEntity) e.getEntity();
        this.currentMap = e.getDataMap();
        this.currentDomain = e.getDomain();
        EventListener[] list = getListeners(ObjAttributeDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            ObjAttributeDisplayListener temp = (ObjAttributeDisplayListener) list[i];
            temp.currentObjAttributeChanged(e);
        }
    }

    /** Notifies all listeners of the change(add, remove) and does the change.*/
    public void fireDbRelationshipEvent(RelationshipEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(DbRelationshipListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DbRelationshipListener temp = (DbRelationshipListener) list[i];
            switch (e.getId()) {
                case RelationshipEvent.ADD :
                    temp.dbRelationshipAdded(e);
                    break;
                case RelationshipEvent.CHANGE :
                    temp.dbRelationshipChanged(e);
                    break;
                case RelationshipEvent.REMOVE :
                    temp.dbRelationshipRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public void fireDbRelationshipDisplayEvent(RelationshipDisplayEvent e) {
        if (e.getRelationship() == this.getCurrentDbRelationship())
            e.setRelationshipChanged(false);
        this.fireDbEntityDisplayEvent(e);
        this.currentDbEntity = (DbEntity) e.getEntity();
        this.currentMap = e.getDataMap();
        this.currentDomain = e.getDomain();
        // Must follow DbEntityDisplayEvent, 
        // as it resets curr Attr and Rel values to null.
        currentDbRel = (DbRelationship) e.getRelationship();
        EventListener[] list = getListeners(DbRelationshipDisplayListener.class);
        debugEvent(e, list);
        for (int i = 0; i < list.length; i++) {
            DbRelationshipDisplayListener temp = (DbRelationshipDisplayListener) list[i];
            temp.currentDbRelationshipChanged(e);
        }
    }

    /** Notifies all listeners of the change(add, remove) and does the change.*/
    public void fireObjRelationshipEvent(RelationshipEvent e) {
        setDirty(true);
        EventListener[] list = getListeners(ObjRelationshipListener.class);
        debugEvent(e, list);

        for (int i = 0; i < list.length; i++) {
            ObjRelationshipListener temp = (ObjRelationshipListener) list[i];
            switch (e.getId()) {
                case RelationshipEvent.ADD :
                    temp.objRelationshipAdded(e);
                    break;
                case RelationshipEvent.CHANGE :
                    temp.objRelationshipChanged(e);
                    break;
                case RelationshipEvent.REMOVE :
                    temp.objRelationshipRemoved(e);
                    break;
                default :
                    throw new IllegalArgumentException(
                        "Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public void fireObjRelationshipDisplayEvent(RelationshipDisplayEvent e) {
        if (e.getRelationship() == this.getCurrentObjRelationship())
            e.setRelationshipChanged(false);
        this.fireObjEntityDisplayEvent(e);
        // Must follow DbEntityDisplayEvent, 
        // as it resets curr Attr and Rel values to null.
        currentObjRel = (ObjRelationship) e.getRelationship();
        this.currentObjEntity = (ObjEntity) e.getEntity();
        this.currentMap = e.getDataMap();
        this.currentDomain = e.getDomain();
        EventListener[] list = getListeners(ObjRelationshipDisplayListener.class);
        debugEvent(e, list);

        for (int i = 0; i < list.length; i++) {
            ObjRelationshipDisplayListener temp =
                (ObjRelationshipDisplayListener) list[i];
            temp.currentObjRelationshipChanged(e);
        }
    }

    public void addDataMap(Object src, DataMap wrap) {
        addDataMap(src, wrap, true);
    }

    public void addDataMap(Object src, DataMap map, boolean makeCurrent) {

        // new map was added.. link it to domain (and node if possible)
        currentDomain.addMap(map);
        fireDataMapEvent(new DataMapEvent(src, map, DataMapEvent.ADD));

        if (currentNode != null) {
            currentNode.addDataMap(map);
            fireDataNodeEvent(new DataNodeEvent(this, currentNode));

            // TODO: maybe reindexing is an overkill in the modeler?
            currentDomain.reindexNodes();
        }

        if (makeCurrent) {
            fireDataMapDisplayEvent(
                new DataMapDisplayEvent(src, map, currentDomain, currentNode));
        }
    }

    private void addListener(Class aClass, EventListener listener) {
        // make sure we do 
        listenerList.add(aClass, listener);
    }

    private EventListener[] getListeners(Class aClass) {
        return listenerList.getListeners(aClass);
    }

    public void setDirty(boolean dirty) {
        if (this.dirty != dirty) {
            this.dirty = dirty;
            CayenneModelerFrame.getFrame().setDirty(dirty);
        }
    }

    protected void debugEvent(EventObject event, EventListener[] listeners) {
        if (logObj.isDebugEnabled()) {
            int listenerCount = (listeners != null) ? listeners.length : -1;
            String className = event.getClass().getName();
            int dot = className.lastIndexOf('.');
            if (dot >= 0) {
                className = className.substring(dot);
            }

            String source = event.getSource().getClass().getName();

            String listenerString =
                (listenerCount == 1) ? "1 listener" : listenerCount + " listeners";

            String eventLabel;

            if (event instanceof MapEvent) {
                MapEvent mapEvent = (MapEvent) event;
                switch (mapEvent.getId()) {
                    case MapEvent.ADD :
                        eventLabel = "ADD";
                        break;
                    case MapEvent.REMOVE :
                        eventLabel = "REMOVE";
                        break;
                    default :
                        eventLabel = "CHANGE";
                }
            }
            else {
                eventLabel = "DISPLAY";
            }
            logObj.debug(
                "--- ["
                    + eventLabel
                    + ": "
                    + className
                    + " to "
                    + listenerString
                    + ", source: "
                    + source
                    + "]");

            // listener debugging is commented out to reduce the number of generated logs
            /*     for (int i = 0; i < listenerCount; i++) {
                     String listenerName = listeners[i].getClass().getName();
                     int listenerDot = listenerName.lastIndexOf('.');
                     if (listenerDot >= 0) {
                         listenerName = listenerName.substring(listenerDot);
                     }
                     logObj.debug(" {listener: " + listenerName + "}");
                 }
                 */
        }
    }
}