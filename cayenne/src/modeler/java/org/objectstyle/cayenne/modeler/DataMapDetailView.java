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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;

/** 
 * Detail view of the DataNode and DataSourceInfo
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DataMapDetailView
    extends JPanel
    implements DocumentListener, DataMapDisplayListener, ItemListener {

    protected EventController mediator;

    protected JLabel nameLabel;
    protected JTextField name;
    protected String oldName;

    protected JLabel locationLabel;
    protected JTextField location;
    protected JPanel depMapsPanel;

    protected Map mapLookup = new HashMap();

    /** Cludge to prevent marking map as dirty during initial load. */
    private boolean ignoreChange;

    public DataMapDetailView(EventController mediator) {
        super();
        this.mediator = mediator;
        mediator.addDataMapDisplayListener(this);
        // Create and layout components
        init();

        // Add listeners
        location.getDocument().addDocumentListener(this);
        name.getDocument().addDocumentListener(this);
    }

    protected void init() {
        this.setLayout(new BorderLayout());
        nameLabel = new JLabel("Data map name: ");
        name = new JTextField(25);
        locationLabel = new JLabel("File: ");
        location = new JTextField(25);
        location.setEditable(false);

        Component[] leftComp = new Component[2];
        leftComp[0] = nameLabel;
        leftComp[1] = locationLabel;

        Component[] rightComp = new Component[2];
        rightComp[0] = name;
        rightComp[1] = location;

        JPanel temp = PanelFactory.createForm(leftComp, rightComp, 5, 5, 5, 5);
        add(temp, BorderLayout.NORTH);
    }

    public void insertUpdate(DocumentEvent e) {
        textFieldChanged(e);
    }
    public void changedUpdate(DocumentEvent e) {
        textFieldChanged(e);
    }
    public void removeUpdate(DocumentEvent e) {
        textFieldChanged(e);
    }

    private void textFieldChanged(DocumentEvent e) {
        if (ignoreChange) {
            return;
        }

        DataMap map = mediator.getCurrentDataMap();
        DataDomain domain = mediator.getCurrentDataDomain();
        DataMapEvent event;
        if (e.getDocument() == name.getDocument()) {
            String new_name = name.getText();
            // If name hasn't changed, do nothing
            if (oldName != null && new_name.equals(oldName)) {
                return;
            }

            // must fully relink renamed map
            List nodes = new ArrayList();
            Iterator allNodes = domain.getDataNodes().iterator();
            while (allNodes.hasNext()) {
                DataNode node = (DataNode) allNodes.next();
                if(node.getDataMaps().contains(map)) {
                	nodes.add(node);
                }
            }

            domain.removeMap(map.getName());
            map.setName(new_name);
            domain.addMap(map);
            
            Iterator relinkNodes = nodes.iterator();
            while(relinkNodes.hasNext()) {
            	DataNode node = (DataNode) relinkNodes.next();
            	node.addDataMap(map);
            }

            event = new DataMapEvent(this, map, oldName);
            mediator.fireDataMapEvent(event);
            oldName = new_name;
        } else if (e.getDocument() == location.getDocument()) {
            if (map.getLocation().equals(location.getText()))
                return;
            map.setLocation(location.getText());
            event = new DataMapEvent(this, map);
            mediator.fireDataMapEvent(event);
        }
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        DataMap map = e.getDataMap();
        if (null == map) {
            return;
        }

        oldName = map.getName();
        ignoreChange = true;
        name.setText(oldName);
        location.setText(map.getLocation());
        ignoreChange = false;

        if (depMapsPanel != null) {
            remove(depMapsPanel);
            depMapsPanel = null;
        }

        mapLookup.clear();

        // add a list of dependencies
        Collection maps = mediator.getCurrentDataDomain().getDataMaps();

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
                JLabel label = new JLabel(nextMap.getName());

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
     * @see java.awt.event.ItemListener#itemStateChanged(ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
        JCheckBox src = (JCheckBox) e.getSource();
        DataMap map = (DataMap) mapLookup.get(src);

        if (map != null) {
            DataMap curMap = mediator.getCurrentDataMap();
            if (e.getStateChange() == ItemEvent.SELECTED) {
                curMap.addDependency(map);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                curMap.removeDependency(map);
            }

            mediator.fireDataMapEvent(new DataMapEvent(this, curMap));
        }
    }
}
