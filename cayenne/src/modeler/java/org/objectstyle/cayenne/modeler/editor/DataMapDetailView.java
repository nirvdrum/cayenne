/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.MapUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Detail view of the DataNode and DataSourceInfo
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DataMapDetailView extends JPanel implements DataMapDisplayListener {

    protected EventController eventController;

    protected JTextField name;
    protected JLabel location;
    protected JPanel depMapsPanel;
    protected JComboBox nodeSelector;
    protected Map mapLookup = new HashMap();

    private ActionListener mapCheckboxListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            JCheckBox src = (JCheckBox) e.getSource();
            DataMap map = (DataMap) mapLookup.get(src);

            if (map != null) {
                DataMap curMap = eventController.getCurrentDataMap();
                if (src.isSelected()) {
                    curMap.addDependency(map);
                }
                else {
                    curMap.removeDependency(map);
                }

                eventController.fireDataMapEvent(new DataMapEvent(this, curMap));
            }
        }
    };

    public DataMapDetailView(EventController eventController) {
        this.eventController = eventController;

        initView();
        initController();
    }

    protected void initView() {
        // create widgets
        name = CayenneWidgetFactory.createTextField();
        location = CayenneWidgetFactory.createLabel("");
        nodeSelector = CayenneWidgetFactory.createComboBox();
        nodeSelector.setRenderer(CellRenderers.listRendererWithIcons());

        // assemble
        this.setLayout(new BorderLayout());
        add(buildTopPanel(), BorderLayout.NORTH);
    }

    protected void initController() {
        eventController.addDataMapDisplayListener(this);
        name.setInputVerifier(new FieldVerifier());

        nodeSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DataNode node = (DataNode) nodeSelector.getSelectedItem();
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
                        eventController.fireDataNodeEvent(
                            new DataNodeEvent(this, nextNode));

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
                    // TODO: maybe reindexing is an overkill in the modeler?
                    eventController.getCurrentDataDomain().reindexNodes();
                }
            }
        });
    }

    /**
     * Updates the view from the current model state.
     * Invoked when a currently displayed ObjEntity is changed.
     */
    private void initFromModel(DataMap map) {
        name.setText(map.getName());
        String locationText = map.getLocation();
        location.setText((locationText != null) ? locationText : "(no file)");

        // rebuild data node list
        Object nodes[] = eventController.getCurrentDataDomain().getDataNodes().toArray();

        // add an empty item to the front
        Object[] objects = new Object[nodes.length + 1];
        // objects[0] = null;

        // now add the entities
        if (nodes.length > 0) {
            Arrays.sort(nodes, Comparators.getNamedObjectComparator());
            System.arraycopy(nodes, 0, objects, 1, nodes.length);
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(objects);

        // find selected node
        for (int i = 0; i < nodes.length; i++) {
            DataNode node = (DataNode) nodes[i];
            if (node.getDataMaps().contains(map)) {
                model.setSelectedItem(node);
                break;
            }
        }

        nodeSelector.setModel(model);

        // rebuild dependency list

        if (depMapsPanel != null) {
            remove(depMapsPanel);
            depMapsPanel = null;
        }

        mapLookup.clear();

        // add a list of dependencies
        Collection maps = eventController.getCurrentDataDomain().getDataMaps();

        if (maps.size() > 1) {
            depMapsPanel = buildMapsPanel(map, maps);
            add(depMapsPanel, BorderLayout.CENTER);
            validate();
        }
    }

    private JPanel buildTopPanel() {
        FormLayout layout =
            new FormLayout("right:max(50dlu;pref), 3dlu, fill:max(170dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataMap Configuration");
        builder.append("DataMap Name:", name);
        builder.append("File:", location);
        builder.append("DataNode:", nodeSelector);
        return builder.getPanel();
    }

    private JPanel buildMapsPanel(DataMap selectedMap, Collection allMaps) {
        FormLayout layout =
            new FormLayout("right:max(50dlu;pref), 3dlu, left:max(170dlu;pref)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("Depends on DataMaps");
        Iterator it = allMaps.iterator();
        while (it.hasNext()) {
            DataMap nextMap = (DataMap) it.next();
            if (nextMap != selectedMap) {
                JCheckBox check = new JCheckBox();
                JLabel label = CayenneWidgetFactory.createLabel(nextMap.getName());
                builder.append(check, label);
                check.addActionListener(mapCheckboxListener);
                if (nextMap.isDependentOn(selectedMap)) {
                    check.setEnabled(false);
                    label.setEnabled(false);
                }

                if (selectedMap.isDependentOn(nextMap)) {
                    check.setSelected(true);
                }

                mapLookup.put(check, nextMap);
            }
        }

        return builder.getPanel();
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

        initFromModel(map);
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
