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
package org.objectstyle.cayenne.modeler.control;

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.datamap.ResolveDbRelationshipDialog;
import org.objectstyle.cayenne.modeler.model.EntityRelationshipsModel;
import org.objectstyle.cayenne.modeler.model.MapObjRelationshipModel;
import org.objectstyle.cayenne.modeler.view.MapObjRelationshipDialog;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @since 1.1
 * @author Andrei Adamchik
 */
public class MapObjRelationshipController extends BasicController {
    static final Logger logObj = Logger.getLogger(MapObjRelationshipController.class);

    public static final String SAVE_CONTROL =
        "cayenne.modeler.mapObjRelationship.save.button";
    public static final String CANCEL_CONTROL =
        "cayenne.modeler.mapObjRelationship.cancel.button";
    public static final String NEW_TOONE_CONTROL =
        "cayenne.modeler.mapObjRelationship.newtoone.button";
    public static final String NEW_TOMANY_CONTROL =
        "cayenne.modeler.mapObjRelationship.newtomany.button";

    protected EventController mediator;

    public MapObjRelationshipController(
        EventController mediator,
        ObjRelationship relationship) {

        this.mediator = mediator;
        Collection objEntities = mediator.getCurrentDataMap().getObjEntities(true);
        MapObjRelationshipModel model =
            new MapObjRelationshipModel(relationship, objEntities);
        setModel(model);
    }

    /**
     * Creates and runs the classpath dialog.
     */
    public void startup() {
        setView(new MapObjRelationshipDialog());
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONTROL)) {
            saveMapping();
        }
        else if (control.matchesID(NEW_TOONE_CONTROL)) {
            createRelationship(false);
        }
        else if (control.matchesID(NEW_TOMANY_CONTROL)) {
            createRelationship(true);
        }
    }

    protected void saveMapping() {
        MapObjRelationshipModel model = (MapObjRelationshipModel) getModel();

        if (model.savePath()) {
            mediator.fireObjRelationshipEvent(
                new RelationshipEvent(
                    CayenneModelerFrame.getFrame(),
                    model.getRelationship(),
                    model.getRelationship().getSourceEntity()));
        }
        shutdown();
    }

    /**
     * Creates a new relationship connecting currently selected source entity 
     * with ObjRelationship target entity. User is allowed to edit the relationship, 
     * change its name, and create joins.
     */
    protected void createRelationship(boolean toMany) {
        cancelEditing();

        MapObjRelationshipModel model = (MapObjRelationshipModel) getModel();
        DbEntity source = model.getStartEntity();
        DbEntity target = model.getEndEntity();

        EntityRelationshipsModel selectedPathComponent = model.getSelectedPathComponent();
        if (selectedPathComponent != null) {
            source = (DbEntity) selectedPathComponent.getSourceEntity();
        }

        DbRelationship dbRelationship =
            (DbRelationship) NamedObjectFactory.createRelationship(
                source,
                target,
                toMany);
        // note: NamedObjectFactory doesn't set source or target, just the name
        dbRelationship.setSourceEntity(source);
        dbRelationship.setTargetEntity(target);
        dbRelationship.setToMany(toMany);

        // TODO: creating relationship outside of ResolveDbRelationshipDialog confuses it
        // to send incorrect event - CHANGE instead of ADD
        ResolveDbRelationshipDialog dialog =
            new ResolveDbRelationshipDialog(
                Collections.singletonList(dbRelationship),
                source,
                target,
                toMany);

        dialog.setVisible(true);
        if (!dialog.isCancelPressed()) {
            // use new relationship
            source.addRelationship(dbRelationship);

            if (!dialog.isCancelPressed()) {
                // use new relationship
                source.addRelationship(dbRelationship);

                if (selectedPathComponent == null) {
                    selectedPathComponent =
                        (EntityRelationshipsModel) model.getDbRelationshipPath().get(0);
                    model.setSelectedPathComponent(selectedPathComponent);
                }

                selectedPathComponent.setRelationshipName(dbRelationship.getName());
            }
        }

        dialog.dispose();
    }

    protected void cancelEditing() {
        ((MapObjRelationshipDialog) getView()).cancelTableEditing();
    }
}
