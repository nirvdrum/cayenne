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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Query;

/**
 * DataDomain is Cayenne "router". It has zero or more DataNodes that work
 * with data sources. For each query coming to DataDomain, an appropriate node
 * is selected and query is forwarded to this node. This way DataDomain creates
 * single data source abstraction hiding multiple physical data sources from the
 * user.
 *
 * Other functions of DataDomain are:
 * <ul>
 * <li>Factory of DataContexts
 * <li>Storage of DataMaps
 * </ul>
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public class DataDomain implements QueryEngine {
	private static Logger logObj = Logger.getLogger(DataDomain.class);

	/** Stores "name" property. */
	protected String name;

	/** Stores mapping of data nodes to DataNode name keys. */
	protected Map dataNodes = Collections.synchronizedMap(new HashMap());
    protected Map nodesByDbEntityName = Collections.synchronizedMap(new HashMap());

	/** Stores DataMaps by name. */
	protected Map maps = Collections.synchronizedMap(new HashMap());

	/** Stores mapping of data nodes to ObjEntity names.
	  * Its goal is to speed up lookups for data operation
	  * switching. */
	protected Map nodesByEntityName = Collections.synchronizedMap(new HashMap());

	private EntityResolver entityResolver;

	/** Creates an unnamed DataDomain */
	public DataDomain() {
	}

	/** Creates DataDomain and assigns it a <code>name</code>. */
	public DataDomain(String name) {
		this.name = name;
	}

	/** Returns "name" property value. */
	public String getName() {
		return name;
	}

	/** Sets "name" property to a new value. */
	public void setName(String name) {
		this.name = name;
	}

	/** Registers new DataMap with this domain. */
	public void addMap(DataMap map) {
		maps.put(map.getName(), map);
	}

	/** Returns DataMap matching <code>name</code> parameter. */
	public DataMap getMap(String mapName) {
		return (DataMap) maps.get(mapName);
	}

	/**
	 * Unregisters DataMap matching <code>name</code> parameter.
	 * Also removes map from any child DataNodes that use it.
	 */
	public synchronized void removeMap(String mapName) {
		DataMap map = (DataMap) maps.remove(mapName);
		if (map == null) {
			logObj.debug("attempt to remove non-existing map: " + mapName);
			return;
		}

		// remove from data nodes
		Iterator it = dataNodes.keySet().iterator();
		while (it.hasNext()) {
			DataNode node = (DataNode) dataNodes.get(it.next());
			node.removeDataMap(mapName);
		}

		// reindex nodes to remove references on removed map entities
		reindexNodes();
	}

	/** Unregisters DataNode. Also removes entities mapped to the current node. */
	public synchronized void removeDataNode(String nodeName) {
		DataNode node_to_remove = (DataNode) dataNodes.get(nodeName);
		if (null == node_to_remove)
			return;
		dataNodes.remove(nodeName);
		Iterator iter = nodesByEntityName.keySet().iterator();
		while (iter.hasNext()) {
			String text = (String) iter.next();
			DataNode node = (DataNode) nodesByEntityName.get(text);
			if (node == node_to_remove) {
				nodesByEntityName.remove(text);
			}
		}
		iter = nodesByDbEntityName.keySet().iterator();
        while (iter.hasNext()) {
            String text = (String) iter.next();
            DataNode node = (DataNode) nodesByDbEntityName.get(text);
            if (node == node_to_remove) {
                nodesByDbEntityName.remove(text);
            }
        }

	}

	/** Returns a list of registered DataMap objects. */
	public List getMapList() {
		List list;

		synchronized (maps) {
			list = new ArrayList(maps.values());
		}

		return list;
	}

	/**
	 * Returns a list of DataNodes associated with this domain.
	 * List is returned by copy.
	 */
	public List getDataNodeList() {
		List list;

		synchronized (dataNodes) {
			list = new ArrayList(dataNodes.values());
		}

		return list;
	}

	/** Returns an array of DataNodes (by copy) */
	public DataNode[] getDataNodes() {
		DataNode[] dataNodesArray = null;
		synchronized (dataNodes) {
			Collection nodes = dataNodes.values();

			if (nodes == null || nodes.size() == 0)
				dataNodesArray = new DataNode[0];
			else {
				dataNodesArray = new DataNode[nodes.size()];
				nodes.toArray(dataNodesArray);
			}
		}
		return dataNodesArray;
	}

	/** Closes all data nodes, removes them from the list
	*  of available nodes. */
	public void reset() {
		synchronized (dataNodes) {
			dataNodes.clear();
			nodesByEntityName.clear();
            nodesByDbEntityName.clear();
            if (entityResolver != null) {
              entityResolver.clearCache();
              entityResolver = null;
            }
		}
	}

	/** Adds new DataNode to this domain. */
	public void addNode(DataNode node) {
		synchronized (dataNodes) {
			// add node to name->node map
			dataNodes.put(node.getName(), node);

			// add node to "ent name->node" map
			DataMap[] nodeMaps = node.getDataMaps();
			if (nodeMaps != null) {
				int mapsCount = nodeMaps.length;
				for (int i = 0; i < mapsCount; i++) {
					addMap(nodeMaps[i]);
					Iterator it = nodeMaps[i].getObjEntitiesAsList().iterator();
					while (it.hasNext()) {
						ObjEntity e = (ObjEntity) it.next();
						nodesByEntityName.put(e.getName(), node);
					}
					it = nodeMaps[i].getDbEntitiesAsList().iterator();
                    while (it.hasNext()) {
                        DbEntity e = (DbEntity) it.next();
                        nodesByDbEntityName.put(e.getName(), node);
                    }

				}
			}
		}
	}

	/** Creates and returns new DataContext. */
	public DataContext createDataContext() {
		return new DataContext(this);
	}

	/** Returns registered DataNode whose name matches
	  * <code>name</code> parameter. */
	public DataNode getNode(String nodeName) {
		return (DataNode) dataNodes.get(nodeName);
	}

	/**
	 * Returns DataNode that should handle database operations for
	 * a specified <code>objEntityName</code>. Method is synchronized
	 * since it can potentially update the index of DataNodes.
	 */
	public synchronized DataNode dataNodeForObjEntityName(String objEntityName) {
		DataNode node = (DataNode) nodesByEntityName.get(objEntityName);

		// if lookup fails, it may mean that internal index
		// in 'nodesByEntityName' need to be updated
		// do it and then try again.
		if (node == null) {
			reindexNodes();
			return (DataNode) nodesByEntityName.get(objEntityName);
		} else {
			return node;
		}
	}

	/**
	 * Updates internal index of DataNodes stored by the entity name.
	 */
	public synchronized void reindexNodes() {
		nodesByEntityName.clear();
 		nodesByDbEntityName.clear();

		DataNode[] nodes = this.getDataNodes();
		for (int j = 0; j < nodes.length; j++) {
			DataNode node = nodes[j];
			DataMap[] nodeMaps = node.getDataMaps();

			if (nodeMaps != null) {
				int mapsCount = nodeMaps.length;
				for (int i = 0; i < mapsCount; i++) {
					addMap(nodeMaps[i]);
					Iterator it = nodeMaps[i].getObjEntitiesAsList().iterator();
					while (it.hasNext()) {
						ObjEntity e = (ObjEntity) it.next();
						nodesByEntityName.put(e.getName(), node);
					}
					it = nodeMaps[i].getDbEntitiesAsList().iterator();
                    while (it.hasNext()) {
                        DbEntity e = (DbEntity) it.next();
                        nodesByDbEntityName.put(e.getName(), node);
                    }

				}
			}
		}
	}

	/**
	 * Returns DataNode that should handle database operations for
	 * a specified <code>objEntity</code>.
	 */
	public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
		return dataNodeForObjEntityName(objEntity.getName());
	}

	/**
	 * Returns DataNode that should handle database operations for
	 * a specified <code>dbEntity</code>.
	 */
	public DataNode dataNodeForDbEntity(DbEntity dbEntity) {
		return this.dataNodeForDbEntityName(dbEntity.getName());
	}

	public synchronized DataNode dataNodeForDbEntityName(String dbEntityName) {
        DataNode node = (DataNode) nodesByDbEntityName.get(dbEntityName);
        // if lookup fails, it may mean that internal index
        // in 'nodesByDbEntityName' need to be updated
        // do it and then try again.
        if (node == null) {
            reindexNodes();
            return (DataNode) nodesByDbEntityName.get(dbEntityName);
        } else {
            return node;
        }
	}

	/**
	 * Returns a DataMap that contains DbEntity matching the
	 * <code>entityName</code> parameter.
	 */
	public DataMap getMapForDbEntity(String dbEntityName) {
		Iterator it = maps.values().iterator();
		while (it.hasNext()) {
			DataMap map = (DataMap) it.next();
			if (map.getDbEntity(dbEntityName) != null) {
				return map;
			}
		}
		return null;
	}

	/**
	 * Returns a DataMap that contains ObjEntity matching the
	 * <code>entityName</code> parameter.
	 */
	public DataMap getMapForObjEntity(String objEntityName) {
		Iterator it = maps.values().iterator();
		while (it.hasNext()) {
			DataMap map = (DataMap) it.next();
			if (map.getObjEntity(objEntityName) != null) {
				return map;
			}
		}
		return null;
	}

	/** Analyzes each query and sends it to appropriate DataNode for execution. */
	public void performQueries(List queries, OperationObserver resultCons) {
		Iterator it = queries.iterator();
		Map queryMap = new HashMap();
		// organize queries by node
		while (it.hasNext()) {
			Query nextQ = (Query) it.next();
			DbEntity dbe=this.getEntityResolver().lookupDbEntity(nextQ);
			if(dbe==null) {
				throw new CayenneRuntimeException("Could not find DbEntity for query with root :"+nextQ.getRoot());
			}
			DataNode aNode = this.dataNodeForDbEntity(dbe);
			if (aNode == null) {
				throw new CayenneRuntimeException(
					"No suitable DataNode to handle entity '" + dbe.getName() + "'.");
			}

			List nodeQueries = (List) queryMap.get(aNode);
			if (nodeQueries == null) {
				nodeQueries = new ArrayList();
				queryMap.put(aNode, nodeQueries);
			}
			nodeQueries.add(nextQ);
		} // perform queries on each node
		Iterator nodeIt = queryMap.keySet().iterator();
		while (nodeIt.hasNext()) {
			DataNode nextNode = (DataNode) nodeIt.next();
			List nodeQueries = (List) queryMap.get(nextNode);
			// ? maybe this should be run in parallel on different nodes ?
			// (then resultCons will have to be prepared to handle results coming
			// from multiple threads)
			// another way of handling this (which actually preserves
			nextNode.performQueries(nodeQueries, resultCons);
		}
	}

	/** Analyzes a query and sends it to appropriate DataNode */
	public void performQuery(Query query, OperationObserver resultCons) {
		DbEntity dbe=this.getEntityResolver().lookupDbEntity(query);
	 	DataNode aNode = this.dataNodeForDbEntity(dbe);

		if (aNode == null) {
			throw new CayenneRuntimeException("No DataNode to handle entity '" + dbe.getName() + "'.");
		}

		aNode.performQuery(query, resultCons);
	}

	public EntityResolver getEntityResolver() {
		if (entityResolver == null) {
			entityResolver = new EntityResolver(getMapList());
		}
		return entityResolver;
	}

    public Iterator dataMapIterator() {
      return maps.values().iterator();
    }

    public void clearDataMaps() {
      maps.clear();
    }
}