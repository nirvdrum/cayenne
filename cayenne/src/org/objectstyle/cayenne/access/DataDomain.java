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

import java.util.*;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DataMap;
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
 * <p><i>For more information see <a href="../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public class DataDomain implements QueryEngine {
	static Logger logObj = Logger.getLogger(DataDomain.class.getName());

	/** Stores "name" property. */
	protected String name;

	/** Stores mapping of data nodes to DataNode name keys. */
	protected HashMap dataNodes = new HashMap();

	/** Stores DataMaps by name. */
	protected HashMap maps = new HashMap();

	/** Stores mapping of data nodes to ObjEntity names.
	  * Its goal is to speed up lookups for data operation 
	  * switching. */
	private HashMap nodesByEntityName = new HashMap();

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
	public DataMap getMap(String name) {
		return (DataMap) maps.get(name);
	}

	/** Unregisters DataMap matching <code>name</code> parameter. */
	public void removeMap(String name) {
		maps.remove(name);
	}

	/** Unregisters DataNode. Also removes entities mapped to the current node. */
	public void removeDataNode(String name) {
		DataNode node_to_remove = (DataNode) dataNodes.get(name);
		if (null == node_to_remove)
			return;
		dataNodes.remove(name);
		Iterator iter = nodesByEntityName.keySet().iterator();
		while (iter.hasNext()) {
			String text = (String) iter.next();
			DataNode node = (DataNode) nodesByEntityName.get(text);
			if (node == node_to_remove)
				nodesByEntityName.remove(text);
		} // End while()
	}

	/** Returns a list of registered DataMap objects. */
	public List getMapList() {
		ArrayList list = new ArrayList();
		Iterator it = maps.keySet().iterator();
		while (it.hasNext()) {
			list.add(maps.get(it.next()));
		}
		return list;
	}

	/** Return an array of DataNodes (by copy) */
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
		}
	}

	/** Adds new DataNode to this domain. */
	public void addNode(DataNode node) {
		synchronized (dataNodes) {
			// add node to name->node map
			dataNodes.put(node.getName(), node);

			// add node to "ent name->node" map
			DataMap[] maps = node.getDataMaps();
			if (maps != null) {
				int mapsCount = maps.length;
				for (int i = 0; i < mapsCount; i++) {
					addMap(maps[i]);
					Iterator it = maps[i].getObjEntitiesAsList().iterator();
					while (it.hasNext()) {
						ObjEntity e = (ObjEntity) it.next();
						nodesByEntityName.put(e.getName(), node);
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

	/** Returns DataNode that should handle database operations for
	  * a specified <code>objEntityName</code>. */
	public DataNode dataNodeForObjEntityName(String objEntityName) {
		return (DataNode) nodesByEntityName.get(objEntityName);
	}

	/** Returns DataNode that should handle database operations for
	  * a specified <code>objEntity</code>. */
	public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
		return dataNodeForObjEntityName(objEntity.getName());
	}

	/** Returns ObjEntity whose name matches <code>name</code> parameter. */
	public ObjEntity lookupEntity(String name) {
		Iterator it = maps.values().iterator();
		while (it.hasNext()) {
			DataMap map = (DataMap) it.next();
			ObjEntity anEntity = map.getObjEntity(name);
			if (anEntity != null)
				return anEntity;
		}
		return null;
	}

	/** 
	 * Returns a DataMap that contains DbEntity matching the 
	 * <code>entityName</code> parameter.
	 */
	public DataMap getMapForDbEntity(String entityName) {
		Iterator it = maps.values().iterator();
		while (it.hasNext()) {
			DataMap map = (DataMap) it.next();
			if(map.getDbEntity(entityName) != null) {
				return map;
			}
		}
		return null;
	}
	
	/** 
	 * Returns a DataMap that contains ObjEntity matching the 
	 * <code>entityName</code> parameter.
	 */
	public DataMap getMapForObjEntity(String entityName) {
		Iterator it = maps.values().iterator();
		while (it.hasNext()) {
			DataMap map = (DataMap) it.next();
			if(map.getObjEntity(entityName) != null) {
				return map;
			}
		}
		return null;
	}

	/** Analyzes each query and sends it to appropriate DataNode for execution. */
	public void performQueries(List queries, OperationObserver resultCons) {
		Iterator it = queries.iterator();
		HashMap queryMap = new HashMap();

		// organize queries by node
		while (it.hasNext()) {
			Query nextQ = (Query) it.next();
			DataNode aNode =
				this.dataNodeForObjEntityName(nextQ.getObjEntityName());

			if (aNode == null) {
				throw new CayenneRuntimeException(
					"No suitable DataNode to handle entity '"
						+ nextQ.getObjEntityName()
						+ "'.");
			}

			ArrayList nodeQueries = (ArrayList) queryMap.get(aNode);
			if (nodeQueries == null) {
				nodeQueries = new ArrayList();
				queryMap.put(aNode, nodeQueries);
			}

			nodeQueries.add(nextQ);
		}

		// perform queries on each node
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
		DataNode aNode =
			this.dataNodeForObjEntityName(query.getObjEntityName());

		if (aNode == null) {
			throw new CayenneRuntimeException(
				"No DataNode to handle entity '"
					+ query.getObjEntityName()
					+ "'.");
		}

		aNode.performQuery(query, resultCons);
	}
}