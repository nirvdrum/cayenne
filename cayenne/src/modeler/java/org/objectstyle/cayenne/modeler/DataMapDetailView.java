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
package org.objectstyle.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.DataNodeWrapper;
import org.objectstyle.cayenne.modeler.util.MapUtil;

/** 
 * Detail view of the DataNode and DataSourceInfo
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DataMapDetailView
    extends CayenneActionPanel
    implements DataMapDisplayListener, ItemListener {

    protected EventController eventController;

    protected JTextField name;

    protected JLabel location;
    protected JPanel depMapsPanel;

    protected JComboBox nodeSelector;

    protected Map mapLookup = new HashMap();

    public DataMapDetailView(EventController mediator) {
        super();
        this.eventController = mediator;
        mediator.addDataMapDisplayListener(this);
        // Create and layout components
        init();

        // Add listeners
        eventController.addDataMapDisplayListener(this);
        nodeSelector.addActionListener(this);
        InputVerifier inputCheck = new FieldVerifier();
        name.setInputVerifier(inputCheck);
    }

    protected void init() {
        this.setLayout(new BorderLayout());

        name = CayenneWidgetFactory.createTextField();
        location = CayenneWidgetFactory.createLabel("");

        nodeSelector = CayenneWidgetFactory.createComboBox();

        Component[] leftComp = new Component[3];
        leftComp[0] = CayenneWidgetFactory.createLabel("DataMap name: ");
        leftComp[1] = CayenneWidgetFactory.createLabel("File: ");
        leftComp[2] = CayenneWidgetFactory.createLabel("Linked to DataNode: ");

        Component[] rightComp = new Component[3];
        rightComp[0] = name;
        rightComp[1] = location;
        rightComp[2] = nodeSelector;

        add(PanelFactory.createForm(leftComp, rightComp, 5, 5, 5, 5), BorderLayout.NORTH);
    }

    /** 
       * Creates DefaultComboBoxModel for linked DataNode selection
       */
    protected ComboBoxModel createComboBoxModel(DataMap map) {

        Collection nodes = eventController.getCurrentDataDomain().getDataNodes();
        int len = nodes.size();
        Object[] nodesModel = new Object[len + 1];

        // First add empty element.
        nodesModel[0] = new DataNodeWrapper();
        Object currentSelection = nodesModel[0];

        // go via an iterator in an indexed loop, since
        // we already obtained the size 
        // (and index is required to initialize array)
        Iterator nodesIt = nodes.iterator();
        for (int i = 1; i <= len; i++) {
            DataNode node = (DataNode) nodesIt.next();
            nodesModel[i] = new DataNodeWrapper(node);

            if (node.getDataMaps().contains(map)) {
                currentSelection = nodesModel[i];
            }
        }

        Arrays.sort(nodesModel);
        DefaultComboBoxModel model = new DefaultComboBoxModel(nodesModel);
        model.setSelectedItem(currentSelection);
        return model;
    }

    /**
     * Refreshes the view, rebuilds the list of other DataMaps that this one 
     * may depend upon. 
     */
    public void currentDataMapChanged(DataMapDisplayEvent e) {
        DataMap map = e.getDataMap();

        if (null == map) {
            return;
        }

        name.setText(map.getName());
        String locationText = map.getLocation();
        location.setText((locationText != null) ? locationText : "(no file)");

        // rebuild data node list
        nodeSelector.setModel(createComboBoxModel(map));

        // rebuild dependency list

        if (depMapsPanel != null) {
            remove(depMapsPanel);
            depMapsPanel = null;
        }

        mapLookup.clear();

        // add a list of dependencies
        Collection maps = eventController.getCurrentDataDomain().getDataMaps();

        if (maps.size() < 2) {
            return;
        }

        Component[] leftComp = new Component[maps.size() - 1];
        Component[] rightComp = new Component[maps.size() - 1];

        int i = 0;
        Iterator it = maps.iterator();
        while (it.hasNext()) {
            DataMap nextMap = (DataMap) it.next();
            if (nextMap != map) {
                JCheckBox check = new JCheckBox();
                JLabel label = CayenneWidgetFactory.createLabel(nextMap.getName());

                check.addItemListener(this);
                if (nextMap.isDependentOn(map)) {
                    check.setEnabled(false);
                    label.setEnabled(false);
                }

                if (map.isDependentOn(nextMap)) {
                    check.setSelected(true);
                }

                mapLookup.put(check, nextMap);
                leftComp[i] = label;
                rightComp[i] = check;
                i++;
            }
        }

        depMapsPanel = PanelFactory.createForm(leftComp, rightComp, 5, 5, 5, 5);
        depMapsPanel.setBorder(BorderFactory.createTitledBorder("Depends on DataMaps"));
        add(depMapsPanel, BorderLayout.CENTER);
        validate();
    }

    /**
     * ItemListener implementation. Processes (un)selection of dependent DataMaps.
     */
    public void itemStateChanged(ItemEvent e) {
        JCheckBox src = (JCheckBox) e.getSource();
        DataMap map = (DataMap) mapLookup.get(src);

        if (map != null) {
            DataMap curMap = eventController.getCurrentDataMap();
            if (e.getStateChange() == ItemEvent.SELECTED) {
                curMap.addDependency(map);
            }
            else if (e.getStateChange() == ItemEvent.DESELECTED) {
                curMap.removeDependency(map);
            }

            eventController.fireDataMapEvent(new DataMapEvent(this, curMap));
        }
    }

    public void performAction(ActionEvent e) {
        if (e.getSource() == nodeSelector) {

            DataNodeWrapper wrapper = (DataNodeWrapper) nodeSelector.getSelectedItem();
            DataNode node = (wrapper != null) ? wrapper.getDataNode() : null;
            DataMap map = eventController.getCurrentDataMap();

            // no change?
            if (node != null && node.getDataMaps().contains(map)) {
                return;
            }

            boolean hasChanges = false;

            // unlink map from any nodes
            Iterator nodes =
                eventController.getCurrentDataDomain().getDataNodes().iterator();

            while (nodes.hasNext()) {
                DataNode nextNode = (DataNode) nodes.next();

                // Theoretically only one node may contain a datamap at each given time.
                // Being paranoid, we will still scan through all.
                if (nextNode != node && nextNode.getDataMaps().contains(map)) {
                    nextNode.removeDataMap(map.getName());

                    // announce DataNode change
                    eventController.fireDataNodeEvent(new DataNodeEvent(this, nextNode));

                    hasChanges = true;
                }
            }

            // link to a selected node
            if (node != null) {
                node.addDataMap(map);
                hasChanges = true;

                // announce DataNode change
                eventController.fireDataNodeEvent(new DataNodeEvent(this, node));
            }

            if (hasChanges) {
                // maybe reindexing is an overkill in the modeler?
                eventController.getCurrentDataDomain().reindexNodes();
            }
        }
    }

    class FieldVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            if (input == name) {
                return verifyName();
            }
            else {
                return true;
            }
        }

        protected boolean verifyName() {
            String text = name.getText();
            if (text == null || text.trim().length() == 0) {
                text = "";
            }

            DataDomain domain = eventController.getCurrentDataDomain();
            DataMap map = eventController.getCurrentDataMap();
            DataMap matchingMap = domain.getMap(text);

            if (matchingMap == null) {
                // completely new name, set new name for domain
                DataMapEvent e = new DataMapEvent(this, map, map.getName());
                MapUtil.setDataMapName(domain, map, text);
                eventController.fireDataMapEvent(e);
                return true;
            }
            else if (matchingMap == map) {
                // no name changes, just return
                return true;
            }
            else {
                // there is an entity with the same name
                return false;
            }
        }
    }
}
