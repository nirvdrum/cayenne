package org.objectstyle.cayenne.gui;

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
    DataMapDetailView dataMapView;
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
		dataMapView = new DataMapDetailView(temp_mediator);
		detailPanel.add(dataMapView, DATA_MAP_VIEW);
		
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
   		if (e.getDataMap() == null)
	   		detailLayout.show(detailPanel, EMPTY_VIEW);
   		else
	   		detailLayout.show(detailPanel, DATA_MAP_VIEW);
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