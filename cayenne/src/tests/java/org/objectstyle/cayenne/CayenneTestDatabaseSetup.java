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
package org.objectstyle.cayenne;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.objectstyle.TestMain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.access.OperationSorter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
 * @author Andrei Adamchik
 */
public class CayenneTestDatabaseSetup {
	static Logger logObj = Logger.getLogger(CayenneTestDatabaseSetup.class);

	protected DataMap map;
	protected CayenneTestResources resources;

	public CayenneTestDatabaseSetup(CayenneTestResources resources, DataMap map) throws Exception {
		this.map = map;
		this.resources = resources;
	}

	/** Deletes all data from the database tables mentioned in the DataMap. */
	public void cleanTableData() throws Exception {
		Connection conn = resources.getSharedConnection();

		List list = dbEntitiesInInsertOrder();
		try {
			if (conn.getAutoCommit()) {
				conn.setAutoCommit(false);
			}

			Statement stmt = conn.createStatement();

			ListIterator it = list.listIterator(list.size());
			while (it.hasPrevious()) {
				DbEntity ent = (DbEntity) it.previous();
				if (ent instanceof DerivedDbEntity) {
					continue;
				}

				String deleteSql = "DELETE FROM " + ent.getName();
				stmt.executeUpdate(deleteSql);
			}
			conn.commit();
			stmt.close();
		} finally {
			conn.close();
		}

		// lets recreate pk support, since there is no
		// generic way to reset pk info
		DataNode node = resources.getSharedNode();
		DbAdapter adapter = node.getAdapter();

		// drop
		adapter.getPkGenerator().dropAutoPk(node, list);

		// create
		adapter.getPkGenerator().createAutoPk(node, list);
	}

	/** Drops all test tables. */
	public void dropTestTables() throws Exception {
		Connection conn = resources.getSharedConnection();
		DataNode node = resources.getSharedNode();
		DbAdapter adapter = node.getAdapter();
		List list = dbEntitiesInInsertOrder();

		try {
			DatabaseMetaData md = conn.getMetaData();
			ResultSet tables = md.getTables(null, null, "%", null);
			ArrayList allTables = new ArrayList();

			while (tables.next()) {
				// 'toUpperCase' is needed since most databases
				// are case insensitive, and some will convert names to lower case (PostgreSQL)
				String name = tables.getString("TABLE_NAME");
				if (name != null)
					allTables.add(name.toUpperCase());
			}
			tables.close();

			// drop all tables in the map
			Statement stmt = conn.createStatement();

			ListIterator it = list.listIterator(list.size());
			while (it.hasPrevious()) {
				DbEntity ent = (DbEntity) it.previous();
				if (!allTables.contains(ent.getName())) {
					continue;
				}

				try {
					String dropSql = adapter.dropTable(ent);
					logObj.warn("Drop table: " + dropSql);
					stmt.execute(dropSql);
				} catch (SQLException sqe) {
					logObj.warn(
						"Can't drop table " + ent.getName() + ", ignoring...",
						sqe);
				}
			}
		} finally {
			conn.close();
		}

		// drop primary key support
		adapter.getPkGenerator().dropAutoPk(node, list);
	}

	/** Creates all test tables in the database. */
	public void setupTestTables() throws Exception {
		Connection conn = resources.getSharedConnection();

		try {
			Statement stmt = conn.createStatement();
			Iterator it = tableCreateQueries();
			while (it.hasNext()) {
				String query = (String) it.next();
				logObj.warn("Create table: " + query);
				stmt.execute(query);
			}
		} finally {
			conn.close();
		}

		// create primary key support
		DataNode node = resources.getSharedNode();
		DbAdapter adapter = node.getAdapter();
		adapter.getPkGenerator().createAutoPk(
			node,
			node.getDataMaps()[0].getDbEntitiesAsList());
	}

	/** 
	 * Creates primary key support for all node DbEntities.
	 * Will use its facilities provided by DbAdapter to generate
	 * any necessary database objects and data for primary
	 * key support.
	 */
	public void createPkSupportForMapEntities(DataNode node) throws Exception {
		DataMap[] dataMaps = node.getDataMaps();
		int len = dataMaps.length;
		for (int i = 0; i < len; i++) {
			DbEntity[] ents = dataMaps[i].getDbEntities();
			node.getAdapter().getPkGenerator().createAutoPk(
				node,
				dataMaps[i].getDbEntitiesAsList());
		}
	}

	/** Oracle 8i does not support more then 1 "LONG xx" column per table
	  * PAINTING_INFO need to be fixed. */
	private void applyOracleHack() {
		DbEntity paintingInfo = map.getDbEntity("PAINTING_INFO");
		DbAttribute textReview =
			(DbAttribute) paintingInfo.getAttribute("TEXT_REVIEW");
		textReview.setType(Types.VARCHAR);
		textReview.setMaxLength(255);
	}

	/** Returns iterator of preprocessed table create queries */
	public Iterator tableCreateQueries() throws Exception {
		ArrayList queries = new ArrayList();
		DbAdapter adapter = resources.getSharedNode().getAdapter();
		DbGenerator gen = new DbGenerator(adapter, map);

		// Oracle 8i does not support more then 1 "LONG xx" column per table
		// PAINTING_INFO need to be fixed
		if (adapter
			instanceof org.objectstyle.cayenne.dba.oracle.OracleAdapter) {
			applyOracleHack();
		}

		// table definitions
		Iterator it = dbEntitiesInInsertOrder().iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			if (ent instanceof DerivedDbEntity) {
				continue;
			}

			queries.add(adapter.createTable(ent));
		}

		// FK constraints
		if (adapter.supportsFkConstraints()) {
			it = dbEntitiesInInsertOrder().iterator();
			while (it.hasNext()) {
				DbEntity ent = (DbEntity) it.next();
				if (ent instanceof DerivedDbEntity) {
					continue;
				}

				List qs = gen.createFkConstraintsQueries(ent);
				queries.addAll(qs);
			}
		}

		return queries.iterator();
	}

	/** Helper method that orders DbEntities to satisfy referential
	 *  constraints and returns an ordered list. */
	private List dbEntitiesInInsertOrder() {
		List list = map.getDbEntitiesAsList();

		DataNode node = resources.getSharedNode();
		OperationSorter sorter = node.getAdapter().getOpSorter(node);
		if (sorter != null) {
			sorter.sortEntitiesInInsertOrder(list);
		}
		return list;
	}

}

