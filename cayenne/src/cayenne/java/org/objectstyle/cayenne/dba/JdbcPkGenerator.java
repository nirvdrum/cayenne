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

package org.objectstyle.cayenne.dba;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DefaultOperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SqlModifyQuery;
import org.objectstyle.cayenne.query.SqlSelectQuery;

/** 
 * Default primary key generator implementation. Uses a lookup table named
 * "AUTO_PK_SUPPORT" to search and increment primary keys for tables.  
 * 
 * @author Andrei Adamchik
 */
public class JdbcPkGenerator implements PkGenerator {
	private static Logger logObj = Logger.getLogger(JdbcPkGenerator.class);

	private static final String NEXT_ID = "NEXT_ID";
	private static final ObjAttribute[] objDesc =
		new ObjAttribute[] { new ObjAttribute("nextId", Integer.class.getName(), null)};
	private static final DbAttribute[] resultDesc =
		new DbAttribute[] { new DbAttribute(NEXT_ID, Types.INTEGER, null)};

	protected Map pkCache = new HashMap();
	protected int pkCacheSize = 20;


	public void createAutoPk(DataNode node, List dbEntities) throws Exception {
		// check if a table exists

		// create AUTO_PK_SUPPORT table
		if (!autoPkTableExists(node)) {
			runUpdate(node, pkTableCreateString());
		}

		// delete any existing pk entries
		runUpdate(node, pkDeleteString(dbEntities));

		// insert all needed entries
		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			runUpdate(node, pkCreateString(ent.getName()));
		}
	}

	public List createAutoPkStatements(List dbEntities) {
		List list = new ArrayList();

		list.add(pkTableCreateString());
		list.add(pkDeleteString(dbEntities));

		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			list.add(pkCreateString(ent.getName()));
		}

		return list;
	}

	/** 
	 * Drops table named "AUTO_PK_SUPPORT" if it exists in the 
	 * database. 
	 */
	public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
		if (autoPkTableExists(node)) {
			runUpdate(node, dropAutoPkString());
		}
	}

	public List dropAutoPkStatements(List dbEntities) {
		List list = new ArrayList();
		list.add(dropAutoPkString());
		return list;
	}

	protected String pkTableCreateString() {
		StringBuffer buf = new StringBuffer();
		buf
			.append("CREATE TABLE AUTO_PK_SUPPORT (")
			.append("  TABLE_NAME CHAR(100) NOT NULL,")
			.append("  NEXT_ID INTEGER NOT NULL")
			.append(")");

		return buf.toString();
	}

	protected String pkDeleteString(List dbEntities) {
		StringBuffer buf = new StringBuffer();
		buf.append("DELETE FROM AUTO_PK_SUPPORT WHERE TABLE_NAME IN (");
		int len = dbEntities.size();
		for (int i = 0; i < len; i++) {
			if (i > 0) {
				buf.append(", ");
			}
			DbEntity ent = (DbEntity) dbEntities.get(i);
			buf.append('\'').append(ent.getName()).append('\'');
		}
		buf.append(')');
		return buf.toString();
	}

	protected String pkCreateString(String entName) {
		StringBuffer buf = new StringBuffer();
		buf
			.append("INSERT INTO AUTO_PK_SUPPORT")
			.append(" (TABLE_NAME, NEXT_ID)")
			.append(" VALUES ('")
			.append(entName)
			.append("', 200)");
		return buf.toString();
	}

	protected String pkSelectString(String entName) {
		StringBuffer buf = new StringBuffer();
		buf
			.append("SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = '")
			.append(entName)
			.append('\'');
		return buf.toString();
	}

	protected String pkUpdateString(String entName) {
		StringBuffer buf = new StringBuffer();
		buf
			.append("UPDATE AUTO_PK_SUPPORT")
			.append(" SET NEXT_ID = NEXT_ID + ")
			.append(pkCacheSize)
			.append(" WHERE TABLE_NAME = '")
			.append(entName)
			.append('\'');
		return buf.toString();
	}

	protected String dropAutoPkString() {
		return "DROP TABLE AUTO_PK_SUPPORT";
	}

	/** 
	 * Checks if AUTO_PK_TABLE already exists in the database.
	 */
	protected boolean autoPkTableExists(DataNode node) throws SQLException {
		Connection con = node.getDataSource().getConnection();
		boolean exists = false;
		try {
			DatabaseMetaData md = con.getMetaData();
			ResultSet tables = md.getTables(null, null, "AUTO_PK_SUPPORT", null);
			try {
				exists = tables.next();
			} finally {
				tables.close();
			}
		} finally {
			// return connection to the pool
			con.close();
		}

		return exists;
	}

	/** 
	 * Runs JDBC update over a Connection obtained from DataNode. 
	 * Returns a number of objects returned from update.
	 * 
	 * @throws SQLException in case of query failure. 
	 */
	public int runUpdate(DataNode node, String sql) throws SQLException {
		QueryLogger.logQuery(QueryLogger.getLoggingLevel(), sql, Collections.EMPTY_LIST);
		
		Connection con = node.getDataSource().getConnection();
		try {
			Statement upd = con.createStatement();
			try {
				return upd.executeUpdate(sql);
			} finally {
				upd.close();
			}
		} finally {
			con.close();
		}
	}

	/** 
	 * Creates and executes SqlModifyQuery using inner class PkSchemaProcessor
	 * to track the results of the execution.
	 * 
	 * @throws java.lang.Exception in case of query failure. */
	protected List runSelect(DataNode node, String sql) throws Exception {
		SqlSelectQuery q = new SqlSelectQuery();
		q.setSqlString(sql);

		SelectObserver observer = new SelectObserver();
		node.performQuery(q, observer);
		return observer.getResults(q);
	}

	public String generatePkForDbEntityString(DbEntity ent) {
		StringBuffer buf = new StringBuffer();
		buf.append(pkSelectString(ent.getName())).append('\n').append(
			pkUpdateString(ent.getName()));
		return buf.toString();
	}

	/**
	 * <p>Generates new (unique and non-repeating) primary key for specified 
	 * dbEntity.</p>
	 *
	 * <p>This implementation is naive and can have problems with high 
	 * volume databases, when multiple applications can use this to get 
	 * a primary key value. There is a possiblity that 2 clients will 
	 * recieve the same value of primary key. So database specific 
	 * implementations should be created for cleaner approach (like Oracle
	 * sequences, for example).</p>
	 */
	public Object generatePkForDbEntity(DataNode node, DbEntity ent) throws Exception {

		PkRange r = (PkRange) pkCache.get(ent.getName());
		if (r == null || r.isExhausted()) {
			int val = pkFromDatabase(node, ent);

			if (pkCacheSize == 1) {
				return new Integer(val);
			}

			r = new PkRange(val, val + pkCacheSize - 1);
			pkCache.put(ent.getName(), r);
		}

		return r.getNextPrimaryKey();
	}

	/** 
	 * Performs primary key generation ignoring cache. Generates 
	 * a range of primary keys as specified by
	 * "pkCacheSize" bean property. 
	 * 
	 * <p>This method is called internally from "generatePkForDbEntity" 
	 * and then generated range of key values is saved in cache for 
	 * performance. Subclasses that implement different primary key 
	 * generation solutions should override this method, 
	 * not "generatePkForDbEntity".</p>
	 */
	protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {

		// run queries via DataNode to utilize its transactional behavior
		List queries = new ArrayList(2);

		// 1. prepare select 
		SqlSelectQuery sel = new SqlSelectQuery(ent, pkSelectString(ent.getName()));
		sel.setObjDescriptors(objDesc);
		sel.setResultDescriptors(resultDesc);
		queries.add(sel);

		// 2. prepare update 
		queries.add(new SqlModifyQuery(ent, pkUpdateString(ent.getName())));

		PkRetrieveProcessor observer = new PkRetrieveProcessor(ent.getName());
		node.performQueries(queries, observer);

		if (!observer.successFlag) {
			throw new CayenneRuntimeException("Error generating PK.");
		} else {
			return observer.nextId.intValue();
		}
	}

	/**
	 * Returns a size of the entity primary key cache.
	 * Default value is 20. If cache size is set to a value
	 * less or equals than "one", no primary key caching is done.
	 */
	public int getPkCacheSize() {
		return pkCacheSize;
	}

	/**
	 * Sets the size of the entity primary key cache.
	 * If <code>pkCacheSize</code> parameter is less than 1,
	 * cache size is set to "one".
	 * 
	 * <p><i>Note that our tests show that setting primary key
	 * cache value to anything much bigger than 20 does not give 
	 * any significant performance increase. Therefore it does
	 * not make sense to use bigger values, since this may 
	 * potentially create big gaps in the database primary
	 * key sequences in cases like application crashes or restarts.
	 * </i></p>
	 */
	public void setPkCacheSize(int pkCacheSize) {
		this.pkCacheSize = (pkCacheSize < 1) ? 1 : pkCacheSize;
	}

	/** OperationObserver for primary key retrieval. */
	class PkRetrieveProcessor extends DefaultOperationObserver {
		private boolean successFlag;
		private Integer nextId;
		private String entName;

		public PkRetrieveProcessor(String entName) {
			this.entName = entName;
		}

		public boolean useAutoCommit() {
			return false;
		}

		public void nextDataRows(Query query, List dataRows) {
			super.nextDataRows(query, dataRows);

			// process selected object, issue an update query
			if (dataRows == null || dataRows.size() == 0) {
				throw new CayenneRuntimeException(
					"Error generating PK : entity not supported: " + entName);
			}
			if (dataRows.size() > 1) {
				throw new CayenneRuntimeException(
					"Error generating PK : too many rows for entity: " + entName);
			}

			Map lastPk = (Map) dataRows.get(0);
			nextId = (Integer) lastPk.get(NEXT_ID);
			if (nextId == null) {
				throw new CayenneRuntimeException("Error generating PK : null nextId.");
			}
		}

		public void nextCount(Query query, int resultCount) {
			super.nextCount(query, resultCount);

			if (resultCount != 1)
				throw new CayenneRuntimeException(
					"Error generating PK : update count is wrong: " + resultCount);
		}

		public void transactionCommitted() {
			super.transactionCommitted();
			successFlag = true;
		}

		public void nextQueryException(Query query, Exception ex) {
			super.nextQueryException(query, ex);
			String entityName = ((query != null) && (query.getRoot()!=null)) ? query.getRoot().toString() : null;
			throw new CayenneRuntimeException(
				"Error generating PK for entity '" + entityName + "'.",
				ex);
		}

		public void nextGlobalException(Exception ex) {
			super.nextGlobalException(ex);
			throw new CayenneRuntimeException("Error generating PK.", ex);
		}
	}
}