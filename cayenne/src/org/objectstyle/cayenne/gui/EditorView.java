package org.objectstyle.cayenne.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.datamap.*;

/** Panel for the Editor window. */
class EditorView extends JPanel 
implements ObjEntityDisplayListener, DbEntityDisplayListener
, DomainDisplayListener, DataMapDisplayListener, DataNodeDisplayListener {
    Mediator mediator;

    private static final int INIT_DIVIDER_LOCATION = 170;

    private static final String EMPTY_VIEW    = "Empty";
    private static final String DOMAIN_VIEW   = "Domain";
    private static final String NODE_VIEW   = "Node";
    private static final String DATA_MAP_VIEW = "DataMap";
    private static final String OBJ_VIEW    = "ObjView";
    private static final String DB_VIEW    = "DbView";


    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    BrowseView treePanel;
    JPanel detailPanel = new JPanel();
    JPanel emptyPanel = new JPanel();
    DomainDetailView domainView;
    DataNodeDetailView nodeView;
    JPanel dataMapView = new JPanel();
    ObjDetailView objDetailView;
    DbDetailView dbDetailView;
    CardLayout detailLayout;

    public EditorView(Mediator temp_mediator) {
        super(new BorderLayout());
        mediator = temp_mediator;

        add(splitPane, BorderLayout.CENTER);
        treePanel = new BrowseView(temp_mediator);
        splitPane.setLeftComponent(treePanel);
	    splitPane.setRightComponent(detailPanel);

		Dimension minimumSize = new Dimension(350, 200);
	    detailPanel.setMinimumSize(minimumSize);
		minimumSize = new Dimension(170, 200);
		treePanel.setMinimumSize(minimumSize);

        detailLayout = new CardLayout();
        detailPanel.setLayout(detailLayout);

		JPanel temp = new JPanel(new FlowLayout());
		detailPanel.add(temp, EMPTY_VIEW);
		domainView = new DomainDetailView(temp_mediator);
		detailPanel.add(domainView, DOMAIN_VIEW);
		nodeView = new DataNodeDetailView(temp_mediator);
		detailPanel.add(nodeView, NODE_VIEW);
		
        objDetailView = new ObjDetailView(temp_mediator);
        detailPanel.add(objDetailView, OBJ_VIEW);
        dbDetailView = new DbDetailView(temp_mediator);
        detailPanel.add(dbDetailView, DB_VIEW);
        
        mediator.addDomainDisplayListener(this);
        mediator.addDataNodeDisplayListener(this);
        mediator.addDataMapDisplayListener(this);
        mediator.addObjEntityDisplayListener(this);
        mediator.addDbEntityDisplayListener(this);
    }

   	public void currentDomainChanged(DomainDisplayEvent e)
   	{
   		if (e.getDomain() == null)
	   		detailLayout.show(detailPanel, EMPTY_VIEW);
   		else
	   		detailLayout.show(detailPanel, DOMAIN_VIEW);
   	}

   	public void currentDataNodeChanged(DataNodeDisplayEvent e)
   	{
   		if (e.getDataNode() == null)
	   		detailLayout.show(detailPanel, EMPTY_VIEW);
   		else
   			detailLayout.show(detailPanel, NODE_VIEW);
   	}
    


   	public void currentDataMapChanged(DataMapDisplayEvent e)
   	{
   		detailLayout.show(detailPanel, EMPTY_VIEW);
   	}
    
   	public void currentObjEntityChanged(EntityDisplayEvent e)
   	{
   		if (e.getEntity() == null)
	   		detailLayout.show(detailPanel, EMPTY_VIEW);
   		else
	   		detailLayout.show(detailPanel, OBJ_VIEW);
   	}


   	public void currentDbEntityChanged(EntityDisplayEvent e)
   	{
   		if (e.getEntity() == null)
	   		detailLayout.show(detailPanel, EMPTY_VIEW);
   		else
	   		detailLayout.show(detailPanel, DB_VIEW);
   	}
}