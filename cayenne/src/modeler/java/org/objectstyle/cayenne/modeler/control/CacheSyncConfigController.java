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

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.model.CacheSyncConfigModel;
import org.objectstyle.cayenne.modeler.model.CustomRemoteEventsConfigModel;
import org.objectstyle.cayenne.modeler.model.JGroupsConfigModel;
import org.objectstyle.cayenne.modeler.model.JMSConfigModel;
import org.objectstyle.cayenne.modeler.view.CacheSyncConfigDialog;
import org.objectstyle.cayenne.modeler.view.CustomRemoteEventsConfigPanel;
import org.objectstyle.cayenne.modeler.view.JGroupsConfigPanel;
import org.objectstyle.cayenne.modeler.view.JMSConfigPanel;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.ModelChangeListener;
import org.scopemvc.core.Selector;

/**
 * @author Andrei Adamchik
 */
public class CacheSyncConfigController
    extends BasicController
    implements ModelChangeListener {

    private static Logger logObj = Logger.getLogger(CacheSyncConfigController.class);

    public static final String SAVE_CONFIG_CONTROL =
        "cayenne.modeler.cacheSyncConfig.save.button";
    public static final String CANCEL_CONFIG_CONTROL =
        "cayenne.modeler.cacheSyncConfig.cancel.button";

    public static final String JGROUPS_DEFAULT_CONTROL =
        "cayenne.modeler.jgroupConfig.radio1";

    public static final String JGROUPS_URL_CONTROL =
        "cayenne.modeler.jgroupConfig.radio2";

    protected Map existingCards;

    public void startup() {
        existingCards = new HashMap();

        CacheSyncConfigModel model = new CacheSyncConfigModel();
        model.addModelChangeListener(this);
        setModel(model);
        setView(new CacheSyncConfigDialog());
        super.startup();
    }

    public void modelChanged(ModelChangeEvent inEvent) {
        logObj.info("ModelChangeEvent: " + inEvent.getSelector());

        Selector selector = inEvent.getSelector();

        if (selector.startsWith(CacheSyncConfigModel.SELECTED_TYPE_SELECTOR)) {
            ((CacheSyncConfigModel) getModel()).setModified(true);

            if (selector.getLast().equals(CacheSyncConfigModel.SELECTED_TYPE_SELECTOR)) {
                changeConfigView();
            }
        }
    }

    protected void doHandleControl(Control control) throws ControlException {
        logObj.info("Control: " + control);

        if (control.matchesID(CANCEL_CONFIG_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONFIG_CONTROL)) {
            commitChanges();
        }
        else if (control.matchesID(JGROUPS_DEFAULT_CONTROL)) {
            jgroupsDefaultConfig();
        }
        else if (control.matchesID(JGROUPS_URL_CONTROL)) {
            jgroupsURLConfig();
        }
    }

    protected void jgroupsDefaultConfig() {
        BasicController controller = findController(CacheSyncConfigModel.JGROUPS_TYPE);
        if (controller != null) {
            ((JGroupsConfigPanel) controller.getView()).showDefaultConfig();
        }
    }

    protected void jgroupsURLConfig() {
        BasicController controller = findController(CacheSyncConfigModel.JGROUPS_TYPE);
        if (controller != null) {
            ((JGroupsConfigPanel) controller.getView()).showCustomConfig();
        }
    }

    /**
     * Stores configuration changes in the data domain properties.
     */
    protected void commitChanges() {
        CacheSyncConfigModel model = (CacheSyncConfigModel) getModel();
        logObj.info("Has changes?: " + model.isModified());
        shutdown();
    }

    /**
     * Changes a subview to a panel specific for the currently selected 
     * configuration type.
     */
    protected void changeConfigView() {
        CacheSyncConfigModel model = (CacheSyncConfigModel) getModel();
        BasicController controller = findController(model.getSelectedType());

        if (controller != null) {
            model.setConfigDetail(controller.getModel());
            ((CacheSyncConfigDialog) getView()).showCard(model.getSelectedType());
            logObj.warn("Show view: " + model.getSelectedType());
        }
        else {
            ((CacheSyncConfigDialog) getView()).showCard(
                CacheSyncConfigDialog.EMPTY_CARD_KEY);
        }
    }

    /**
     * Locates controller that should handle a subview, creating one if needed.
     */
    protected BasicController findController(String key) {

        BasicController controller = (BasicController) existingCards.get(key);
        if (controller == null) {

            if (CacheSyncConfigModel.JGROUPS_TYPE.equals(key)) {
                controller = new GenericController(this);
                controller.setModel(new JGroupsConfigModel());
                controller.setView(new JGroupsConfigPanel());
            }
            else if (CacheSyncConfigModel.JMS_TYPE.equals(key)) {
                controller = new GenericController(this);
                controller.setModel(new JMSConfigModel());
                controller.setView(new JMSConfigPanel());
            }
            else if (CacheSyncConfigModel.CUSTOM_TYPE.equals(key)) {
                controller = new GenericController(this);
                controller.setModel(new CustomRemoteEventsConfigModel());
                controller.setView(new CustomRemoteEventsConfigPanel());
            }

            if (controller != null) {
                ((CacheSyncConfigDialog) getView()).addCard(
                    (Component) controller.getView(),
                    key);
                existingCards.put(key, controller);
            }
        }

        return controller;
    }

    // generic controller that will propagate all controls to this object
    class GenericController extends BasicController {
        GenericController(BasicController parent) {
            setParent(parent);
        }
    }
}
