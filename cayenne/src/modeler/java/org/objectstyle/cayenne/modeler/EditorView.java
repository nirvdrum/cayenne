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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.objectstyle.cayenne.modeler.action.ProcedureDetailView;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.datamap.DbDetailView;
import org.objectstyle.cayenne.modeler.datamap.ObjDetailView;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;

/** 
 * Panel for the Editor window.
 * 
 *  @author Michael Misha Shengaout
 *  @author Andrei Adamchik
 */
public class EditorView
    extends JPanel
    implements
        ObjEntityDisplayListener,
        DbEntityDisplayListener,
        DomainDisplayListener,
        DataMapDisplayListener,
        DataNodeDisplayListener,
        ProcedureDisplayListener,
        PropertyChangeListener {

    private static final int INIT_DIVIDER_LOCATION = 170;

    private static final String EMPTY_VIEW = "Empty";
    private static final String DOMAIN_VIEW = "Domain";
    private static final String NODE_VIEW = "Node";
    private static final String DATA_MAP_VIEW = "DataMap";
    private static final String OBJ_VIEW = "ObjView";
    private static final String DB_VIEW = "DbView";
    private static final String PROCEDURE_VIEW = "ProcedureView";

    protected EventController eventController;
    protected JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    protected BrowseView treePanel;
    protected JPanel detailPanel;
    protected JPanel emptyPanel;
    protected DomainDetailView domainView;
    protected DataNodeDetailView nodeView;
    protected DataMapDetailView dataMapView;
    protected ObjDetailView objDetailView;
    protected DbDetailView dbDetailView;
    protected ProcedureDetailView procedureView;

    protected CardLayout detailLayout;
    protected ModelerPreferences prefs;

    public EditorView(EventController eventController) {
        super(new BorderLayout());

        this.eventController = eventController;
        this.detailPanel = new JPanel();
        this.emptyPanel = new JPanel();

        add(splitPane, BorderLayout.CENTER);
        treePanel = new BrowseView(eventController);
        splitPane.setLeftComponent(treePanel);
        splitPane.setRightComponent(detailPanel);
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, this);

        treePanel.setMinimumSize(new Dimension(INIT_DIVIDER_LOCATION, 200));

        prefs = ModelerPreferences.getPreferences();
        int preferredSize =
            prefs.getInt(ModelerPreferences.EDITOR_TREE_WIDTH, INIT_DIVIDER_LOCATION);
        splitPane.setDividerLocation(preferredSize);

        detailLayout = new CardLayout();
        detailPanel.setLayout(detailLayout);

        // some but not all panels must be wrapped in a scroll pane
        addPanelToDetailView(new JPanel(), EMPTY_VIEW);
        domainView = new DomainDetailView(eventController);
        addPanelToDetailView(new JScrollPane(domainView), DOMAIN_VIEW);
        nodeView = new DataNodeDetailView(eventController);
        addPanelToDetailView(new JScrollPane(nodeView), NODE_VIEW);
        dataMapView = new DataMapDetailView(eventController);
        addPanelToDetailView(new JScrollPane(dataMapView), DATA_MAP_VIEW);
        procedureView = new ProcedureDetailView(eventController);
        addPanelToDetailView(new JScrollPane(procedureView), PROCEDURE_VIEW);

        objDetailView = new ObjDetailView(eventController);
        addPanelToDetailView(objDetailView, OBJ_VIEW);
        dbDetailView = new DbDetailView(eventController);
        addPanelToDetailView(dbDetailView, DB_VIEW);

		eventController.addDomainDisplayListener(this);
		eventController.addDataNodeDisplayListener(this);
		eventController.addDataMapDisplayListener(this);
		eventController.addObjEntityDisplayListener(this);
		eventController.addDbEntityDisplayListener(this);
		eventController.addProcedureDisplayListener(this);
    }

    protected void addPanelToDetailView(JComponent panel, String name) {
        detailPanel.add(panel, name);
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        if (e.getProcedure() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, PROCEDURE_VIEW);
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        if (e.getDomain() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, DOMAIN_VIEW);
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        if (e.getDataNode() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, NODE_VIEW);
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        if (e.getDataMap() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, DATA_MAP_VIEW);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getEntity() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, OBJ_VIEW);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        if (e.getEntity() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, DB_VIEW);
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == splitPane) {
            prefs.put(
                ModelerPreferences.EDITOR_TREE_WIDTH,
                String.valueOf(splitPane.getDividerLocation()));
        }
    }

}
