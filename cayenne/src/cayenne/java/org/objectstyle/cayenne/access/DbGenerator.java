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

package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.conn.*;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
  * Utility class that does forward engineering of the database.
  * It can generate database schema using the data map. It is a 
  * counterpart of DbLoader class. 
  *
  * @author Andrei Adamchik
  */
public class DbGenerator {

	protected DataNode node;
	protected DataMap map;

	protected Map dropTables;
	protected Map createTables;
	protected Map createFK;
	protected List createPK;
	protected List dropPK;

	protected boolean shouldDropTables;
	protected boolean shouldCreateTables;
	protected boolean shouldDropPKSupport;
	protected boolean shouldCreatePKSupport;
	protected boolean shouldCreateFKConstraints;

	/** Creates and initializes new DbGenerator. */
	public DbGenerator(DbAdapter adapter, DataMap map) {
		// sanity check
		if (adapter == null) {
			throw new IllegalArgumentException("Adapter must not be null.");
		}

		this.map = map;
		this.node = adapter.createDataNode("internal");
		node.addDataMap(map);

		resetToDefaults();
		buildStatements();
	}

	protected void resetToDefaults() {
		this.shouldDropTables = false;
		this.shouldDropPKSupport = false;
		this.shouldCreatePKSupport = true;
		this.shouldCreateTables = true;
		this.shouldCreateFKConstraints = true;
	}

	/**
	 * Creates and stores internally a set of statements 
	 * for database schema creation, ignoring configured schema creation
	 * preferences. Statements are NOT executed in this method.
	 */
	protected void buildStatements() {
		dropTables = new HashMap();
		createTables = new HashMap();
		createFK = new HashMap();

		DbAdapter adapter = getAdapter();
		List dbEntities = new ArrayList(map.getDbEntities());
		Iterator it = dbEntities.iterator();
		boolean supportsFK = adapter.supportsFkConstraints();
		while (it.hasNext()) {
			DbEntity dbe = (DbEntity) it.next();

			// view creation support is pending
			if (dbe instanceof DerivedDbEntity) {
				continue;
			}

			String name = dbe.getName();

			// build "DROP TABLE"
			dropTables.put(name, adapter.dropTable(dbe));

			// build "CREATE TABLE"
			createTables.put(name, adapter.createTable(dbe));

			// build "FK"
			if (supportsFK) {
				createFK.put(name, createFkConstraintsQueries(dbe));
			}
		}

		dropPK = adapter.getPkGenerator().dropAutoPkStatements(dbEntities);
		createPK = adapter.getPkGenerator().createAutoPkStatements(dbEntities);
	}

	/** Returns DbAdapter associated with this DbGenerator. */
	public DbAdapter getAdapter() {
		return node.getAdapter();
	}

	/**
	 * Returns a list of all schema statements that
	 * should be executed with the current configuration.
	 */
	public List configuredStatements() {
		List list = new ArrayList();
		List orderedEnts = dbEntitiesInInsertOrder();

		if (shouldDropTables) {
			ListIterator it = orderedEnts.listIterator(orderedEnts.size());
			while (it.hasPrevious()) {
				DbEntity ent = (DbEntity) it.previous();
				list.add(dropTables.get(ent.getName()));
			}
		}

		if (shouldCreateTables) {
			Iterator it = orderedEnts.iterator();
			while (it.hasNext()) {
				DbEntity ent = (DbEntity) it.next();
				list.add(createTables.get(ent.getName()));
			}
		}

		if (shouldCreateFKConstraints
			&& getAdapter().supportsFkConstraints()) {
			Iterator it = orderedEnts.iterator();
			while (it.hasNext()) {
				DbEntity ent = (DbEntity) it.next();
				List fks = (List) createFK.get(ent.getName());
				list.addAll(fks);
			}
		}

		if (shouldDropPKSupport) {
			list.addAll(dropPK);
		}

		if (shouldCreatePKSupport) {
			list.addAll(createPK);
		}

		return list;
	}

	/**
	 * Creates a temporary DataSource out of DataSourceInfo and
	 * invokes <code>public void runGenerator(DataSource ds)</code>.
	 */
	public void runGenerator(DataSourceInfo dsi) throws Exception {
		PoolManager dataSource =
			new PoolManager(
				dsi.getJdbcDriver(),
				dsi.getDataSourceUrl(),
				dsi.getMinConnections(),
				dsi.getMaxConnections(),
				dsi.getUserName(),
				dsi.getPassword());

		try {
			runGenerator(dataSource);
		} finally {
			dataSource.dispose();
		}
	}

	/** 
	 * Main method to generate database objects out of the DataMap.
	 */
	public void runGenerator(DataSource ds) throws Exception {
		Connection con = ds.getConnection();
		List orderedEnts = dbEntitiesInInsertOrder();

		try {
			List nonExistent = filterNonExistentTables(con);
			Statement stmt = con.createStatement();

			try {
				if (shouldDropTables) {
					ListIterator it =
						orderedEnts.listIterator(orderedEnts.size());
					while (it.hasPrevious()) {
						DbEntity ent = (DbEntity) it.previous();

						// check if this table even exists
						if (!nonExistent.contains(ent.getName())) {
							executeStatement(
								(String) dropTables.get(ent.getName()),
								stmt);
						}
					}
				}

				//Refresh the list to ensure all required tables will be created
				nonExistent = filterNonExistentTables(con);
				List createdTables = new ArrayList();

				if (shouldCreateTables) {
					Iterator it = orderedEnts.iterator();
					while (it.hasNext()) {
						DbEntity ent = (DbEntity) it.next();

						// only create missing tables
						if (nonExistent.contains(ent.getName())) {
							createdTables.add(ent.getName());
							executeStatement(
								(String) createTables.get(ent.getName()),
								stmt);
						}
					}
				}

				if (shouldCreateTables
					&& shouldCreateFKConstraints
					&& getAdapter().supportsFkConstraints()) {
					Iterator it = orderedEnts.iterator();
					while (it.hasNext()) {
						DbEntity ent = (DbEntity) it.next();

						if (createdTables.contains(ent.getName())) {
							List fks = (List) createFK.get(ent.getName());
							Iterator fkIt = fks.iterator();
							while (fkIt.hasNext()) {
								executeStatement((String) fkIt.next(), stmt);
							}
						}
					}
				}
			} finally {
				stmt.close();
			}
		} finally {
			con.close();
		}

		// run PK generation via adapter's generator
		node.setDataSource(ds);

		try {
			if (shouldDropPKSupport) {

				getAdapter().getPkGenerator().dropAutoPk(node, orderedEnts);
			}

			if (shouldCreatePKSupport) {
				getAdapter().getPkGenerator().createAutoPk(node, orderedEnts);
			}
		} finally {
			node.setDataSource(null);
		}
	}

	/**
	 * Executes a DDL statement, logging the execution via the QueryLogger.
	 */
	protected void executeStatement(String stmtText, Statement stmt)
		throws SQLException {
		QueryLogger.logQuery(Level.INFO, stmtText, null);
		stmt.execute(stmtText);
	}

	/** 
	 * Returns an array of queries to create foreign key constraints
	 * for a particular DbEntity. Throws CayenneRuntimeException, if called
	 * for adapter that does not support FK constraints.
	 */
	public List createFkConstraintsQueries(DbEntity dbEnt) {
		if (!getAdapter().supportsFkConstraints()) {
			throw new CayenneRuntimeException("FK constraints are not supported by adapter.");
		}

		List list = new ArrayList();
		Iterator it = dbEnt.getRelationships().iterator();
		while (it.hasNext()) {
			DbRelationship rel = (DbRelationship) it.next();
			if (!rel.isToMany() && !rel.isToDependentPK()) {
				list.add(getAdapter().createFkConstraint(rel));
			}
		}
		return list;
	}

	/** Returns a subset of DbEntity names from the <code>map</code>
	 *  that have no corresponding database tables. 
	 * 
	 * @throws SQLException if an error occurred while processing
	 * a list of database tables. 
	 */
	private List filterNonExistentTables(Connection con) throws SQLException {
		// read a list of tables
		DatabaseMetaData md = con.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		List tables = new ArrayList();
		try {
			while (rs.next()) {
				tables.add(rs.getString("TABLE_NAME").toLowerCase());
			}
		} finally {
			rs.close();
		}

		// find tables that are in the map but not in the database
		List missing = new ArrayList();
		Iterator it = map.getDbEntities().iterator();
		while (it.hasNext()) {
			DbEntity e = (DbEntity) it.next();
			if (!tables.contains(e.getName().toLowerCase())) {
				missing.add(e.getName());
			}
		}
		return missing;
	}

	public boolean shouldCreatePKSupport() {
		return shouldCreatePKSupport;
	}

	public boolean shouldCreateTables() {
		return shouldCreateTables;
	}

	public boolean shouldDropPKSupport() {
		return shouldDropPKSupport;
	}

	public boolean shouldDropTables() {
		return shouldDropTables;
	}

	public boolean shouldCreateFKConstraints() {
		return shouldCreateFKConstraints;
	}

	public void setShouldCreatePKSupport(boolean shouldCreatePKSupport) {
		this.shouldCreatePKSupport = shouldCreatePKSupport;
	}

	public void setShouldCreateTables(boolean shouldCreateTables) {
		this.shouldCreateTables = shouldCreateTables;
	}

	public void setShouldDropPKSupport(boolean shouldDropPKSupport) {
		this.shouldDropPKSupport = shouldDropPKSupport;
	}

	public void setShouldDropTables(boolean shouldDropTables) {
		this.shouldDropTables = shouldDropTables;
	}

	public void setShouldCreateFKConstraints(boolean shouldCreateFKConstraints) {
		this.shouldCreateFKConstraints = shouldCreateFKConstraints;
	}

	/** 
	 * Helper method that orders DbEntities to satisfy referential
	 * constraints and returns an ordered list. 
	 */
	private List dbEntitiesInInsertOrder() {
		// remove derived db entities
		List filteredList = new ArrayList();
		Iterator it = map.getDbEntities().iterator();
		while (it.hasNext()) {
			Object next = it.next();
			if (!(next instanceof DerivedDbEntity)) {
				filteredList.add(next);
			}
		}

		// sort the list
		node.getDependencySorter().sortDbEntities(filteredList, false);
		return filteredList;
	}

}