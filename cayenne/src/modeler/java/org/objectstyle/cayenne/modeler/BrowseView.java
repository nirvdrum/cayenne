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
package org.objectstyle.cayenne.modeler;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.control.EventController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataMapEvent;
import org.objectstyle.cayenne.modeler.event.DataMapListener;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataNodeEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityListener;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.event.DomainEvent;
import org.objectstyle.cayenne.modeler.event.DomainListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.EntityEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ObjEntityListener;
import org.objectstyle.cayenne.modeler.util.ProjectTree;

/** 
 * Tree of domains, data maps, data nodes (sources) and entities. 
 * When item of the tree is selected, detailed view for that 
 * item comes up. 
 * 
 *  @author Michael Misha Shengaout. 
 */
public class BrowseView
    extends JScrollPane
    implements
        DomainDisplayListener,
        DomainListener,
        DataMapDisplayListener,
        DataMapListener,
        DataNodeDisplayListener,
        DataNodeListener,
        ObjEntityListener,
        ObjEntityDisplayListener,
        DbEntityListener,
        DbEntityDisplayListener {
    private static Logger logObj = Logger.getLogger(BrowseView.class);

    private static final int DOMAIN_NODE = 1;
    private static final int NODE_NODE = 2;
    private static final int MAP_NODE = 3;
    private static final int OBJ_ENTITY_NODE = 4;
    private static final int DB_ENTITY_NODE = 5;

    protected EventController mediator;
    protected ProjectTree browseTree;
    protected DefaultMutableTreeNode rootNode;
    protected DefaultMutableTreeNode currentNode;

    protected DefaultTreeModel model;

    public BrowseView(EventController mediator) {
        super();
        this.mediator = mediator;

        browseTree = new ProjectTree(Editor.getProject());
        browseTree.setCellRenderer(new BrowseViewRenderer());
        setViewportView(browseTree);

        model = (DefaultTreeModel) browseTree.getModel();
        rootNode = (DefaultMutableTreeNode) model.getRoot();

        // listen for mouse events
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = browseTree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() >= 1) {
                        processSelection(
                            browseTree.getPathForLocation(e.getX(), e.getY()));
                    }
                }
            }
        };
        browseTree.addMouseListener(ml);

        // listen to tree events (since not al selections
        // are done by clicking tree with mouse)
        TreeSelectionListener tsl = new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                processSelection(e.getPath());
            }
        };
        browseTree.addTreeSelectionListener(tsl);

        mediator.addDomainListener(this);
        mediator.addDomainDisplayListener(this);
        mediator.addDataNodeListener(this);
        mediator.addDataNodeDisplayListener(this);
        mediator.addDataMapListener(this);
        mediator.addDataMapDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjEntityDisplayListener(this);
        mediator.addDbEntityListener(this);
        mediator.addDbEntityDisplayListener(this);
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        showNode(new Object[] { e.getDomain()});
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        if (e.getSource() == this || e.isDataNodeChanged() == false)
            return;

        showNode(new Object[] { e.getDomain(), e.getDataNode()});
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        if (e.getSource() == this || !e.isDataMapChanged())
            return;

        showNode(new Object[] { e.getDomain(), e.getDataMap()});
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        currentEntityChanged(e);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        currentEntityChanged(e);
    }

    protected void currentEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this || !e.isEntityChanged()) {
            return;
        }
        showNode(new Object[] { e.getDomain(), e.getDataMap(), e.getEntity()});
    }

    public void domainChanged(DomainEvent e) {
        if (e.getSource() == this)
            return;

        updateNode(new Object[] { e.getDomain()});
    }

    public void domainAdded(DomainEvent e) {
        if (e.getSource() == this)
            return;
        browseTree.insertObject(e.getDomain(), rootNode);
    }

    public void domainRemoved(DomainEvent e) {
        if (e.getSource() == this) {
            return;
        }

        removeNode(new Object[] { e.getDomain()});
    }

    public void dataNodeChanged(DataNodeEvent e) {
        if (e.getSource() == this)
            return;
        DefaultMutableTreeNode node =
            browseTree.getProjectModel().getNodeForObjectPath(
                new Object[] { mediator.getCurrentDataDomain(), e.getDataNode()});

        if (null != node) {
            model.nodeChanged(node);
            DataMap[] maps = e.getDataNode().getDataMaps();
            // If added map to this node
            if (maps.length > node.getChildCount()) {
                // Find map not already under node and add it
                for (int i = 0; i < maps.length; i++) {
                    boolean found = false;
                    for (int j = 0; j < node.getChildCount(); j++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);
                        if (maps[i] == child.getUserObject()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        browseTree.insertObject(maps[i], node);
                        break;
                    }
                } // End for(i)
            } else if (maps.length < node.getChildCount()) {
                for (int j = 0; j < node.getChildCount(); j++) {
                    boolean found = false;
                    DefaultMutableTreeNode child;
                    child = (DefaultMutableTreeNode) node.getChildAt(j);
                    Object obj = child.getUserObject();
                    for (int i = 0; i < maps.length; i++) {
                        if (maps[i] == obj) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        removeNode(child);
                        break;
                    }
                }
            }

        }
    }

    public void dataNodeAdded(DataNodeEvent e) {
        if (e.getSource() == this)
            return;
        DefaultMutableTreeNode parent =
            browseTree.getProjectModel().getNodeForObjectPath(
                new Object[] { mediator.getCurrentDataDomain()});

        browseTree.insertObject(e.getDataNode(), parent);
    }

    public void dataNodeRemoved(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        removeNode(new Object[] { mediator.getCurrentDataDomain(), e.getDataNode()});
    }

    public void dataMapChanged(DataMapEvent e) {
        if (e.getSource() == this) {
            return;
        }

        updateNode(new Object[] { mediator.getCurrentDataDomain(), e.getDataMap()});
    }

    public void dataMapAdded(DataMapEvent e) {
        if (e.getSource() == this)
            return;
        DefaultMutableTreeNode parent =
            browseTree.getProjectModel().getNodeForObjectPath(
                new Object[] { mediator.getCurrentDataDomain()});

        if (null == parent)
            return;

        browseTree.insertObject(e.getDataMap(), parent);
    }

    public void dataMapRemoved(DataMapEvent e) {
        if (e.getSource() == this) {
            return;
        }

        DataMap map = e.getDataMap();
        DataDomain domain = mediator.getCurrentDataDomain();

        removeNode(new Object[] { domain, map });

        // Clean up map from the nodes
        DataNode[] nodes = domain.getDataNodes();
        for (int i = 0; i < nodes.length; i++) {
            removeNode(new Object[] { domain, nodes[i], map });
        }
    }

    public void objEntityChanged(EntityEvent e) {
        entityChanged(e);
    }

    public void objEntityAdded(EntityEvent e) {
        entityAdded(e);
    }

    public void objEntityRemoved(EntityEvent e) {
        entityRemoved(e);
    }

    public void dbEntityChanged(EntityEvent e) {
        entityChanged(e);
    }
    public void dbEntityAdded(EntityEvent e) {
        entityAdded(e);
    }
    public void dbEntityRemoved(EntityEvent e) {
        entityRemoved(e);
    }

    /** Makes Entity visible and selected.
     * 
     *  <ul>
     *  <li>If entity is from the current node, refreshes the node making sure 
     *      changes in the entity name are reflected.</li>
     *  <li>If entity is in a different node, makes that node visible and 
     *      selected.</li>
     *  </ul>
     */
    public void entityChanged(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        updateNode(
            new Object[] {
                mediator.getCurrentDataDomain(),
                mediator.getCurrentDataMap(),
                e.getEntity()});

        updateNode(
            new Object[] {
                mediator.getCurrentDataDomain(),
                mediator.getCurrentDataNode(),
                mediator.getCurrentDataMap(),
                e.getEntity()});
    }

    /** 
     * Event handler for ObjEntity and DbEntity additions.
     * Adds a tree node for the entity and make it selected. 
     */
    protected void entityAdded(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        Entity entity = e.getEntity();

        // Add a node and make it selected.
        if (mediator.getCurrentDataNode() != null) {
            DefaultMutableTreeNode mapNode =
                browseTree.getProjectModel().getNodeForObjectPath(
                    new Object[] {
                        mediator.getCurrentDataDomain(),
                        mediator.getCurrentDataNode(),
                        mediator.getCurrentDataMap()});

            if (mapNode != null) {
                currentNode = new DefaultMutableTreeNode(entity, false);
                model.insertNodeInto(currentNode, mapNode, mapNode.getChildCount());
            }
        }

        DefaultMutableTreeNode mapNode =
            browseTree.getProjectModel().getNodeForObjectPath(
                new Object[] {
                    mediator.getCurrentDataDomain(),
                    mediator.getCurrentDataMap()});

        if(mapNode == null) {
        	return;
        }
        
        currentNode = new DefaultMutableTreeNode(entity, false);
        fixEntityPosition(mapNode, currentNode);
        showNode(currentNode);
    }

    /** 
     * Event handler for ObjEntity and DbEntity removals.
     * Removes a tree node for the entity and selects its sibling. 
     */
    protected void entityRemoved(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        // remove from DataMap tree
        removeNode(
            new Object[] {
                mediator.getCurrentDataDomain(),
                mediator.getCurrentDataMap(),
                e.getEntity()});

        // remove from DataMap *reference* tree
        removeNode(
            new Object[] {
                mediator.getCurrentDataDomain(),
                mediator.getCurrentDataNode(),
                mediator.getCurrentDataMap(),
                e.getEntity()});
    }

    /** 
     * Removes current node from the tree. 
     * Selects a new node adjacent to the currently selected node instead.
     */
    protected void removeNode(DefaultMutableTreeNode toBeRemoved) {

        // lookup for the new selected node
        if (currentNode == toBeRemoved) {

            // first search siblings
            DefaultMutableTreeNode newSelection = toBeRemoved.getNextSibling();
            if (newSelection == null) {
                newSelection = toBeRemoved.getPreviousSibling();

                // try parent
                if (newSelection == null) {
                    newSelection = (DefaultMutableTreeNode) toBeRemoved.getParent();

                    // search the whole tree
                    if (newSelection == null) {

                        newSelection = toBeRemoved.getNextNode();
                        if (newSelection == null) {

                            newSelection = toBeRemoved.getPreviousNode();
                        }
                    }
                }
            }

            currentNode = newSelection;
            showNode(currentNode);
        }

        // remove this node
        model.removeNodeFromParent(toBeRemoved);
    }

    /** Makes node current, visible and selected.*/
    protected void showNode(DefaultMutableTreeNode node) {
        currentNode = node;
        TreePath path = new TreePath(currentNode.getPath());
        browseTree.scrollPathToVisible(path);
        browseTree.setSelectionPath(path);
    }

    protected void showNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node =
            browseTree.getProjectModel().getNodeForObjectPath(path);

        if (node == null) {
            return;
        }
        currentNode = node;
        TreePath treePath = new TreePath(currentNode.getPath());
        browseTree.scrollPathToVisible(treePath);
        browseTree.setSelectionPath(treePath);
    }

    protected void updateNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node =
            browseTree.getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            model.nodeChanged(node);
        }
    }

    protected void removeNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node =
            browseTree.getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            removeNode(node);
        }
    }

    /** 
     * Processes node selection regardless of whether 
     * a new node was selected, or an already selected node
     * was clicked again. Normally called from event listener
     * methods.
     */
    public void processSelection(TreePath path) {
        if (path == null) {
            return;
        }

        currentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        Object[] data = getUserObjects(currentNode);
        if (data.length == 0) {
            // this should clear the right-side panel
            mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, null));
            return;
        }

        Object obj = data[data.length - 1];
        if (obj instanceof DataDomain) {
            mediator.fireDomainDisplayEvent(
                new DomainDisplayEvent(this, (DataDomain) obj));
        } else if (obj instanceof DataMap) {
            if (data.length == 3) {
                mediator.fireDataMapDisplayEvent(
                    new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataDomain) data[data.length - 3],
                        (DataNode) data[data.length - 2]));
            } else if (data.length == 2) {
                mediator.fireDataMapDisplayEvent(
                    new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataDomain) data[data.length - 2]));
            }
        } else if (obj instanceof DataNode) {
            if (data.length == 2) {
                mediator.fireDataNodeDisplayEvent(
                    new DataNodeDisplayEvent(
                        this,
                        (DataDomain) data[data.length - 2],
                        (DataNode) obj));
            }
        } else if (obj instanceof Entity) {
            EntityDisplayEvent e = new EntityDisplayEvent(this, (Entity) obj);
            e.setUnselectAttributes(true);
            if (data.length == 4) {
                e.setDataMap((DataMap) data[data.length - 2]);
                e.setDomain((DataDomain) data[data.length - 4]);
                e.setDataNode((DataNode) data[data.length - 3]);
            } else if (data.length == 3) {
                e.setDataMap((DataMap) data[data.length - 2]);
                e.setDomain((DataDomain) data[data.length - 3]);
            }

            if (obj instanceof ObjEntity) {
                mediator.fireObjEntityDisplayEvent(e);
            } else if (obj instanceof DbEntity) {
                mediator.fireDbEntityDisplayEvent(e);
            }
        }
    }

    /** Gets array of the user objects ending with this and starting with one under root. 
      * That is the array of actual objects rather than wrappers.*/
    private Object[] getUserObjects(DefaultMutableTreeNode node) {
        List list = new ArrayList();
        while (!node.isRoot()) {
            list.add(0, node.getUserObject());
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return list.toArray();
    }

    /** 
     * Inserts entity node in alphabetical order. 
     * Assumes that the tree is already ordered, except for one node. 
     */
    private void fixEntityPosition(
        DefaultMutableTreeNode parent,
        DefaultMutableTreeNode entityNode) {
        Entity curEnt = (Entity) entityNode.getUserObject();
        boolean isObj = curEnt instanceof ObjEntity;

        int len = parent.getChildCount();
        int ins = -1;
        int rm = -1;

        for (int i = 0; i < len; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);

            // remeber to remove node
            if (node == entityNode) {
                rm = i;
                continue;
            }

            // no more insert checks
            if (ins >= 0) {
                continue;
            }

            Entity e = (Entity) node.getUserObject();

            // ObjEntities go before DbEntities
            if (isObj && (e instanceof DbEntity)) {
                ins = i;
            }
            if (!isObj && (e instanceof ObjEntity)) {
                continue;
            }

            // Ignore unnamed
            if (e.getName() == null) {
                continue;
            }

            // Do alphabetical comparison
            if (e.getName().compareTo(curEnt.getName()) > 0) {
                ins = i;
            }
        }

        if (ins < 0) {
            ins = len;
        }

        // remove
        if (rm >= 0) {
            model.removeNodeFromParent(entityNode);
            if (rm < ins) {
                ins--;
            }
        }

        // insert
        model.insertNodeInto(entityNode, parent, ins);
    }

    static class BrowseViewRenderer extends DefaultTreeCellRenderer {
        ImageIcon domainIcon;
        ImageIcon nodeIcon;
        ImageIcon mapIcon;
        ImageIcon dbEntityIcon;
        ImageIcon objEntityIcon;
        ImageIcon derivedDbEntityIcon;

        public BrowseViewRenderer() {
            ClassLoader cl = BrowseViewRenderer.class.getClassLoader();
            URL url = cl.getResource(CayenneAction.RESOURCE_PATH + "icon-dom.gif");
            domainIcon = new ImageIcon(url);

            url = cl.getResource(CayenneAction.RESOURCE_PATH + "icon-node.gif");
            nodeIcon = new ImageIcon(url);

            url = cl.getResource(CayenneAction.RESOURCE_PATH + "icon-datamap.gif");
            mapIcon = new ImageIcon(url);

            url = cl.getResource(CayenneAction.RESOURCE_PATH + "icon-dbentity.gif");
            dbEntityIcon = new ImageIcon(url);

            url =
                cl.getResource(CayenneAction.RESOURCE_PATH + "icon-derived-dbentity.gif");
            derivedDbEntityIcon = new ImageIcon(url);

            url = cl.getResource(CayenneAction.RESOURCE_PATH + "icon-objentity.gif");
            objEntityIcon = new ImageIcon(url);
        }

        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

            super.getTreeCellRendererComponent(
                tree,
                value,
                sel,
                expanded,
                leaf,
                row,
                hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object obj = node.getUserObject();
            if (obj instanceof DataDomain) {
                setIcon(domainIcon);
            } else if (obj instanceof DataNode) {
                setIcon(nodeIcon);
            } else if (obj instanceof DataMap) {
                setIcon(mapIcon);
            } else if (obj instanceof Entity) {
                Entity ent = (Entity) obj;
                if (ent instanceof DerivedDbEntity) {
                    setIcon(derivedDbEntityIcon);
                } else if (ent instanceof DbEntity) {
                    setIcon(dbEntityIcon);
                } else if (ent instanceof ObjEntity) {
                    setIcon(objEntityIcon);
                }
            }
            return this;
        }
    }
}
