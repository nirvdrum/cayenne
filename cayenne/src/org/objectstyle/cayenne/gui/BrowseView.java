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
package org.objectstyle.cayenne.gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.gui.action.CayenneAction;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.DataDomainWrapper;
import org.objectstyle.cayenne.gui.util.DataMapWrapper;
import org.objectstyle.cayenne.gui.util.DataNodeWrapper;
import org.objectstyle.cayenne.gui.util.EntityWrapper;
import org.objectstyle.cayenne.map.*;

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
	static Logger logObj = Logger.getLogger(BrowseView.class.getName());

	private static final int DOMAIN_NODE = 1;
	private static final int NODE_NODE = 2;
	private static final int MAP_NODE = 3;
	private static final int OBJ_ENTITY_NODE = 4;
	private static final int DB_ENTITY_NODE = 5;

	protected Mediator mediator;
	protected JTree browseTree = new JTree();
	protected DefaultMutableTreeNode rootNode;
	protected DefaultMutableTreeNode currentNode;

	protected DefaultTreeModel model;

	public BrowseView(Mediator data_map_editor) {
		super();
		mediator = data_map_editor;
		setViewportView(browseTree);
		browseTree.setCellRenderer(new BrowseViewRenderer());
		load();

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

	/** Traverses domains, nodes, maps and entities and populates tree.
	 */
	private void load() {
		rootNode = new DefaultMutableTreeNode(null, true);
		// create tree model with root objects
		model = new DefaultTreeModel(rootNode);
		// Populate obj tree
		DataDomain[] temp_domains = mediator.getDomains();
		for (int i = 0; i < temp_domains.length; i++) {
			DefaultMutableTreeNode domain_ele;
			domain_ele = loadDomain(temp_domains[i]);
			rootNode.add(domain_ele);
		} // End for(data domains)

		// Put models into trees.
		browseTree.setModel(model);
		browseTree.setRootVisible(false);
		// Set selection policy (one at a time) and add listeners
		browseTree.getSelectionModel().setSelectionMode(
			TreeSelectionModel.SINGLE_TREE_SELECTION);
		Enumeration level = rootNode.children();
		if (null == level)
			return;
		while (level.hasMoreElements()) {
			DefaultMutableTreeNode node =
				(DefaultMutableTreeNode) level.nextElement();
			TreePath path = new TreePath(node.getPath());
			browseTree.expandPath(path);
		} // End level
	}

	private DefaultMutableTreeNode loadDomain(DataDomain temp_domain) {
		DefaultMutableTreeNode domain_ele =
			new DefaultMutableTreeNode(
				new DataDomainWrapper(temp_domain),
				true);
		List map_list = temp_domain.getMapList();
		Iterator map_iter = map_list.iterator();
		while (map_iter.hasNext()) {
			DefaultMutableTreeNode map_ele = loadMap((DataMap) map_iter.next());
			domain_ele.add(map_ele);
		}
		DataNode[] nodes = temp_domain.getDataNodes();
		for (int node_count = 0; node_count < nodes.length; node_count++) {
			DefaultMutableTreeNode node_ele = loadNode(nodes[node_count]);
			domain_ele.add(node_ele);
		}
		return domain_ele;
	}

	private DefaultMutableTreeNode loadMap(DataMap map) {
		DefaultMutableTreeNode map_ele;
		DataMapWrapper map_wrap = new DataMapWrapper(map);
		map_ele = new DefaultMutableTreeNode(map_wrap, true);
		List obj_entities = map.getObjEntitiesAsList();
		Iterator obj_iter = obj_entities.iterator();
		while (obj_iter.hasNext()) {
			Entity entity = (Entity) obj_iter.next();
			EntityWrapper obj_entity_wrap = new EntityWrapper(entity);
			DefaultMutableTreeNode obj_entity_ele;
			obj_entity_ele = new DefaultMutableTreeNode(obj_entity_wrap, false);
			map_ele.add(obj_entity_ele);
		} // End obj entities
		List db_entities = map.getDbEntitiesAsList();
		Iterator db_iter = db_entities.iterator();
		while (db_iter.hasNext()) {
			Entity entity = (Entity) db_iter.next();
			EntityWrapper db_entity_wrap = new EntityWrapper(entity);
			DefaultMutableTreeNode db_entity_ele;
			db_entity_ele = new DefaultMutableTreeNode(db_entity_wrap, false);
			map_ele.add(db_entity_ele);
		} // End db entities
		return map_ele;
	}

	private DefaultMutableTreeNode loadNode(DataNode node) {
		DefaultMutableTreeNode node_ele;
		DataNodeWrapper node_wrap = new DataNodeWrapper(node);
		node_ele = new DefaultMutableTreeNode(node_wrap, true);
		DataMap[] maps = node.getDataMaps();
		for (int j = 0; j < maps.length; j++) {
			DefaultMutableTreeNode map_ele = loadMap(maps[j]);
			node_ele.add(map_ele);
		}
		return node_ele;
	}

	public void currentDomainChanged(DomainDisplayEvent e) {
		if (e.getSource() == this) {
			return;
		}
		DefaultMutableTreeNode temp;
		temp = getDomainNode(e.getDomain());
		if (null == temp)
			return;
		showNode(temp);

	}
	public void currentDataNodeChanged(DataNodeDisplayEvent e) {
		if (e.getSource() == this || e.isDataNodeChanged() == false)
			return;
		DefaultMutableTreeNode temp;
		temp = getDataSourceNode(e.getDomain(), e.getDataNode());
		if (null == temp)
			return;
		showNode(temp);
	}
	public void currentDataMapChanged(DataMapDisplayEvent e) {
		if (e.getSource() == this || e.isDataMapChanged() == false)
			return;
		DefaultMutableTreeNode temp;
		temp = getMapNode(e.getDomain(), e.getDataMap());
		if (null == temp)
			return;
		showNode(temp);
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

		DefaultMutableTreeNode treeNode =
			getEntityNode(e.getDomain(), e.getDataMap(), e.getEntity());
		if (treeNode == null) {
			return;
		}
		showNode(treeNode);
	}

	public void domainChanged(DomainEvent e) {
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode node;
		node = getDomainNode(e.getDomain());
		if (null != node)
			model.nodeChanged(node);
	}

	public void domainAdded(DomainEvent e) {
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode node;
		node = loadDomain(e.getDomain());
		model.insertNodeInto(node, rootNode, rootNode.getChildCount());
	}

	public void domainRemoved(DomainEvent e) {
		if (e.getSource() == this) {
			return;
		}

		DefaultMutableTreeNode treeNode = getDomainNode(e.getDomain());
		if (treeNode != null) {
			removeNode(treeNode);
		}
	}

	public void dataNodeChanged(DataNodeEvent e) {
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode node =
			getDataSourceNode(mediator.getCurrentDataDomain(), e.getDataNode());
		if (null != node) {
			model.nodeChanged(node);
			DataMap[] maps = e.getDataNode().getDataMaps();
			// If added map to this node
			if (maps.length > node.getChildCount()) {
				logObj.fine("About to add map to node");
				// Find map not already under node and add it
				for (int i = 0; i < maps.length; i++) {
					boolean found = false;
					for (int j = 0; j < node.getChildCount(); j++) {
						DefaultMutableTreeNode child;
						child = (DefaultMutableTreeNode) node.getChildAt(j);
						DataMapWrapper wrap;
						wrap = (DataMapWrapper) child.getUserObject();
						if (maps[i] == wrap.getDataMap()) {
							found = true;
							break;
						}
					}
					if (false == found) {
						DefaultMutableTreeNode map_ele = loadMap(maps[i]);
						model.insertNodeInto(
							map_ele,
							node,
							node.getChildCount());
						logObj.fine(
							"Added map " + maps[i].getName() + " to node");
						break;
					}
				} // End for(i)
			} else if (maps.length < node.getChildCount()) {
				for (int j = 0; j < node.getChildCount(); j++) {
					boolean found = false;
					DefaultMutableTreeNode child;
					child = (DefaultMutableTreeNode) node.getChildAt(j);
					DataMapWrapper wrap;
					wrap = (DataMapWrapper) child.getUserObject();
					for (int i = 0; i < maps.length; i++) {
						if (maps[i] == wrap.getDataMap()) {
							found = true;
							break;
						}
					}
					if (!found) {
						logObj.fine(
							"About to remove map " + wrap + " from node");
						removeNode(child);
						break;
					}
				} // End for(j)
			}

		}
	}

	public void dataNodeAdded(DataNodeEvent e) {
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode parent;
		parent = getDomainNode(mediator.getCurrentDataDomain());
		if (null == parent)
			return;
		DefaultMutableTreeNode node;
		node = loadNode(e.getDataNode());
		model.insertNodeInto(node, parent, parent.getChildCount());
	}

	public void dataNodeRemoved(DataNodeEvent e) {
		if (e.getSource() == this) {
			return;
		}

		DefaultMutableTreeNode treeNode =
			getDataSourceNode(mediator.getCurrentDataDomain(), e.getDataNode());
		if (treeNode != null) {
			removeNode(treeNode);
		}
	}

	public void dataMapChanged(DataMapEvent e) {
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode node;
		node = getMapNode(mediator.getCurrentDataDomain(), e.getDataMap());
		if (null != node)
			model.nodeChanged(node);
	}

	public void dataMapAdded(DataMapEvent e) {
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode parent;
		parent = getDomainNode(mediator.getCurrentDataDomain());
		if (null == parent)
			return;
		DefaultMutableTreeNode node;
		node = loadMap(e.getDataMap());
		model.insertNodeInto(node, parent, parent.getChildCount());
	}

	public void dataMapRemoved(DataMapEvent e) {
		if (e.getSource() == this) {
			return;
		}

		DataMap map = e.getDataMap();
		DataDomain domain = mediator.getCurrentDataDomain();
		DefaultMutableTreeNode treeNode = getMapNode(domain, map);
		if (treeNode != null) {
			removeNode(treeNode);
		}

		// Clean up map from the nodes
		DataNode[] nodes = domain.getDataNodes();
		for (int i = 0; i < nodes.length; i++) {
			DataMap[] maps = nodes[i].getDataMaps();
			for (int j = 0; j < maps.length; j++) {
				if (maps[j] == map) {
					DefaultMutableTreeNode mapNode =
						getMapNode(domain, nodes[i], map);
					if (null != mapNode) {
						model.removeNodeFromParent(mapNode);
					}
				}
			}
		}
	}

	private ArrayList getNodesWithMap(DataMap map) {
		return null;
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
		if (e.getSource() == this)
			return;
		DefaultMutableTreeNode temp;
		temp =
			getEntityNode(
				mediator.getCurrentDataDomain(),
				mediator.getCurrentDataMap(),
				e.getEntity());
		if (null != temp)
			model.nodeChanged(temp);
		temp =
			getEntityNode(
				mediator.getCurrentDataDomain(),
				mediator.getCurrentDataNode(),
				mediator.getCurrentDataMap(),
				e.getEntity());
		if (null != temp)
			model.nodeChanged(temp);
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
		EntityWrapper wrapper = new EntityWrapper(entity);
		if (mediator.getCurrentDataNode() != null) {
			DefaultMutableTreeNode mapNode =
				getMapNode(
					mediator.getCurrentDataDomain(),
					mediator.getCurrentDataNode(),
					mediator.getCurrentDataMap());
			if (mapNode != null) {
				currentNode = new DefaultMutableTreeNode(wrapper, false);
				model.insertNodeInto(
					currentNode,
					mapNode,
					mapNode.getChildCount());
			}
		}

		DefaultMutableTreeNode mapNode =
			getMapNode(
				mediator.getCurrentDataDomain(),
				mediator.getCurrentDataMap());

		currentNode = new DefaultMutableTreeNode(wrapper, false);
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
		DefaultMutableTreeNode treeNode =
			getEntityNode(
				mediator.getCurrentDataDomain(),
				mediator.getCurrentDataMap(),
				e.getEntity());

		if (treeNode != null) {
			removeNode(treeNode);
		}

		// remove from DataMap *reference* tree
		treeNode =
			getEntityNode(
				mediator.getCurrentDataDomain(),
				mediator.getCurrentDataNode(),
				mediator.getCurrentDataMap(),
				e.getEntity());

		if (treeNode != null) {
			removeNode(treeNode);
		}
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
					newSelection =
						(DefaultMutableTreeNode) toBeRemoved.getParent();

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

	/** Get domain node by DataDomain. */
	private DefaultMutableTreeNode getDomainNode(DataDomain domain) {
		if (null == domain)
			return null;
		Enumeration domains = rootNode.children();
		while (domains.hasMoreElements()) {
			DefaultMutableTreeNode temp_node;
			temp_node = (DefaultMutableTreeNode) domains.nextElement();
			DataDomainWrapper wrap =
				(DataDomainWrapper) temp_node.getUserObject();
			if (wrap.getDataDomain() == domain)
				return temp_node;
		}
		return null;
	}

	/** Get map node by DataDomain and DataMap. */
	private DefaultMutableTreeNode getMapNode(DataDomain domain, DataMap map) {
		if (null == map)
			return null;
		DefaultMutableTreeNode domain_node = getDomainNode(domain);
		if (null == domain_node)
			return null;
		Enumeration maps = domain_node.children();
		while (maps.hasMoreElements()) {
			DefaultMutableTreeNode temp_node;
			temp_node = (DefaultMutableTreeNode) maps.nextElement();
			Object obj = temp_node.getUserObject();
			// Skip Data Node-s under domain. Go only for DataMapWrappers.
			if (!(obj instanceof DataMapWrapper))
				continue;
			DataMapWrapper wrap = (DataMapWrapper) obj;
			if (wrap.getDataMap() == map)
				return temp_node;
		}
		return null;
	}

	/** Get data source (a.k.a. data node) node by DataDomain and DataNode. */
	private DefaultMutableTreeNode getDataSourceNode(
		DataDomain domain,
		DataNode data) {
		if (null == data)
			return null;
		DefaultMutableTreeNode domain_node = getDomainNode(domain);
		if (null == domain_node)
			return null;
		Enumeration data_sources = domain_node.children();
		while (data_sources.hasMoreElements()) {
			DefaultMutableTreeNode temp_node;
			temp_node = (DefaultMutableTreeNode) data_sources.nextElement();
			if (temp_node.getUserObject() instanceof DataNodeWrapper) {
				DataNodeWrapper wrap =
					(DataNodeWrapper) temp_node.getUserObject();
				if (wrap.getDataNode() == data)
					return temp_node;
			}
		}
		return null;
	}

	/** Get map node by DataDomain, DataNode and DataMap. */
	private DefaultMutableTreeNode getMapNode(
		DataDomain domain,
		DataNode data,
		DataMap map) {
		if (null == map)
			return null;
		DefaultMutableTreeNode data_node = getDataSourceNode(domain, data);
		if (null == data_node)
			return null;
		Enumeration maps = data_node.children();
		while (maps.hasMoreElements()) {
			DefaultMutableTreeNode temp_node;
			temp_node = (DefaultMutableTreeNode) maps.nextElement();
			DataMapWrapper wrap = (DataMapWrapper) temp_node.getUserObject();
			if (wrap.getDataMap() == map)
				return temp_node;
		}
		return null;
	}

	/** Get entity node by DataDomain, DataMap and Entity. */
	private DefaultMutableTreeNode getEntityNode(
		DataDomain domain,
		DataMap map,
		Entity entity) {
		if (null == entity)
			return null;
		DefaultMutableTreeNode map_node = getMapNode(domain, map);
		if (null == map_node)
			return null;
		Enumeration entities = map_node.children();
		while (entities.hasMoreElements()) {
			DefaultMutableTreeNode temp_node;
			temp_node = (DefaultMutableTreeNode) entities.nextElement();
			EntityWrapper wrap = (EntityWrapper) temp_node.getUserObject();
			if (wrap.getEntity() == entity)
				return temp_node;
		}
		return null;
	}

	/** Get entity node by DataDomain, DataNode, DataMap and Entity. */
	private DefaultMutableTreeNode getEntityNode(
		DataDomain domain,
		DataNode data,
		DataMap map,
		Entity entity) {
		if (null == entity)
			return null;
		DefaultMutableTreeNode map_node = getMapNode(domain, data, map);
		if (null == map_node)
			return null;
		Enumeration entities = map_node.children();
		while (entities.hasMoreElements()) {
			DefaultMutableTreeNode temp_node;
			temp_node = (DefaultMutableTreeNode) entities.nextElement();
			EntityWrapper wrap = (EntityWrapper) temp_node.getUserObject();
			if (wrap.getEntity() == entity)
				return temp_node;
		}
		return null;
	}

	/** Makes node current, visible and selected.*/
	protected void showNode(DefaultMutableTreeNode node) {
		currentNode = node;
		TreePath path = new TreePath(currentNode.getPath());
		browseTree.scrollPathToVisible(path);
		browseTree.setSelectionPath(path);
	}

	/** 
	 * Processes node selection regardless of wether 
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
			EntityDisplayEvent e =
				new EntityDisplayEvent(this, (Entity) obj);
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
		ArrayList list = new ArrayList();
		Object[] arr;
		while (!node.isRoot()) {
			Object obj = node.getUserObject();
			if (obj instanceof DataDomainWrapper)
				list.add(0, ((DataDomainWrapper) obj).getDataDomain());
			else if (obj instanceof DataNodeWrapper)
				list.add(0, ((DataNodeWrapper) obj).getDataNode());
			else if (obj instanceof DataMapWrapper)
				list.add(0, ((DataMapWrapper) obj).getDataMap());
			else if (obj instanceof EntityWrapper)
				list.add(0, ((EntityWrapper) obj).getEntity());
			else
				throw new UnsupportedOperationException("Tree contains invalid wrapper");
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
		Entity curEnt =
			((EntityWrapper) entityNode.getUserObject()).getEntity();
		boolean isObj = curEnt instanceof ObjEntity;

		int len = parent.getChildCount();
		int ins = -1;
		int rm = -1;

		for (int i = 0; i < len; i++) {
			DefaultMutableTreeNode node =
				(DefaultMutableTreeNode) parent.getChildAt(i);

			// remeber to remove node
			if (node == entityNode) {
				rm = i;
				continue;
			}

			// no more insert checks
			if (ins >= 0) {
				continue;
			}

			Entity e = ((EntityWrapper) node.getUserObject()).getEntity();

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
			URL url =
				cl.getResource(
					CayenneAction.RESOURCE_PATH + "images/icon-dom.gif");
			domainIcon = new ImageIcon(url);

			url =
				cl.getResource(
					CayenneAction.RESOURCE_PATH + "images/icon-node.gif");
			nodeIcon = new ImageIcon(url);

			url =
				cl.getResource(
					CayenneAction.RESOURCE_PATH + "images/icon-datamap.gif");
			mapIcon = new ImageIcon(url);

			url =
				cl.getResource(
					CayenneAction.RESOURCE_PATH + "images/icon-dbentity.gif");
			dbEntityIcon = new ImageIcon(url);

			url =
				cl.getResource(
					CayenneAction.RESOURCE_PATH
						+ "images/icon-derived-dbentity.gif");
			derivedDbEntityIcon = new ImageIcon(url);

			url =
				cl.getResource(
					CayenneAction.RESOURCE_PATH + "images/icon-objentity.gif");
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
			if (node.isRoot())
				return this;
			Object obj = node.getUserObject();
			if (obj instanceof DataDomainWrapper) {
				setIcon(domainIcon);
			} else if (obj instanceof DataNodeWrapper) {
				setIcon(nodeIcon);
			} else if (obj instanceof DataMapWrapper) {
				setIcon(mapIcon);
			} else if (obj instanceof EntityWrapper) {
				EntityWrapper wrap = (EntityWrapper) obj;
				if (wrap.getEntity() instanceof DerivedDbEntity) {
					setIcon(derivedDbEntityIcon);
				} else if (wrap.getEntity() instanceof DbEntity) {
					setIcon(dbEntityIcon);
				} else if (wrap.getEntity() instanceof ObjEntity) {
					setIcon(objEntityIcon);
				}
			}
			return this;
		}
	}
}
