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

package org.objectstyle.cayenne.conn;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ListIterator;
import org.apache.log4j.Logger;

import javax.sql.*;

/**
 * PoolManager is a DataSource impementation that hides connection pooling logic
 * from the users, acting as a normal DataSource. Application servers may provide 
 * their own DataSources that handle pooling. In such cases Cayenne should 
 * use app server specific implementation instead of PoolManager.
 *
 * <p>TODO: create a low priority thread that will do pool maintenance.</p>
 *
 * @author Andrei Adamchik
 */
public class PoolManager implements DataSource, ConnectionEventListener {
	static Logger logObj = Logger.getLogger(PoolManager.class.getName());

	protected ConnectionPoolDataSource poolDataSource;
	protected int minConnections;
	protected int maxConnections;
	protected String dataSourceUrl;
	protected String jdbcDriver;
	protected String password;
	protected String userName;

	private ArrayList unusedPool;
	private ArrayList usedPool;

	/** Creates new PoolManager using org.objectstyle.cayenne.conn.PoolDataSource
	  * for an underlying ConnectionPoolDataSource. */
	public PoolManager(
		String jdbcDriver,
		String dataSourceUrl,
		int minCons,
		int maxCons,
		String userName,
		String password)
		throws SQLException {

		this.jdbcDriver = jdbcDriver;
		this.dataSourceUrl = dataSourceUrl;
		PoolDataSource poolDS = new PoolDataSource(jdbcDriver, dataSourceUrl);
		init(poolDS, minCons, maxCons, userName, password);
	}

	/** Creates new PoolManager with the specified policy for
	 *  connection pooling and a ConnectionPoolDataSource object.
	 *
	 *  @param poolDataSource data source for pooled connections
	 *  @param minCons Non-negative integer that specifies a minimum number of open connections
	 *  to keep in the pool at all times
	 *  @param maxCons Non-negative integer that specifies maximum number of simultaneuosly open connections
	 *
	 *  @throws SQLException if pool manager can not be created.
	 */
	public PoolManager(
		ConnectionPoolDataSource poolDataSource,
		int minCons,
		int maxCons,
		String userName,
		String password)
		throws SQLException {
		init(poolDataSource, minCons, maxCons, userName, password);
	}

	/** Initializes pool. Normally called from constructor. */
	private void init(
		ConnectionPoolDataSource poolDataSource,
		int minCons,
		int maxCons,
		String userName,
		String password)
		throws SQLException {

		// do sanity checks...
		if (maxConnections < 0) {
			throw new SQLException(
				"Maximum number of connections can not be negative ("
					+ maxCons
					+ ").");
		}

		if (minConnections < 0) {
			throw new SQLException(
				"Minimum number of connections can not be negative ("
					+ minCons
					+ ").");
		}

		if (minConnections > maxConnections) {
			throw new SQLException("Minimum number of connections can not be bigger then maximum.");
		}

		// init properties
		this.userName = userName;
		this.password = password;
		this.minConnections = minCons;
		this.maxConnections = maxCons;
		this.poolDataSource = poolDataSource;

		// init pool
		usedPool = new ArrayList(maxConnections);
		unusedPool = new ArrayList(maxConnections);
		growPool(minConnections, userName, password);
	}

	/** Creates and returns new PooledConnection object. */
	private PooledConnection newPooledConnection(
		String userName,
		String password)
		throws SQLException {
		if (userName != null)
			return poolDataSource.getPooledConnection(userName, password);
		else
			return poolDataSource.getPooledConnection();
	}

	/** Closes all existing connections, removes them from the pool. */
	public void dispose() throws SQLException {
		// clean connections from the pool
		ListIterator unusedIterator = unusedPool.listIterator();
		while (unusedIterator.hasNext()) {
			PooledConnection con = (PooledConnection) unusedIterator.next();
			// close connection
			con.close();
			// remove connection from the list
			unusedIterator.remove();
		}

		// clean used connections
		ListIterator usedIterator = usedPool.listIterator();
		while (usedIterator.hasNext()) {
			PooledConnection con = (PooledConnection) usedIterator.next();
			// stop listening for connection events
			con.removeConnectionEventListener(this);
			// close connection
			con.close();
			// remove connection from the list
			usedIterator.remove();
		}
	}

	/** Increase connection pool by the specified number of connections..
	 *  Throw SQLException if no more connections are allowed, or if
	 *  an error happens when creating a new connection.
	 */
	private void growPool(int addConnections, String userName, String password)
		throws SQLException {
		if (unusedPool.size() + usedPool.size() + addConnections
			> maxConnections) {
			StringBuffer msg = new StringBuffer();
			msg
				.append("An attempt to open more connections ")
				.append("than pool is allowed to handle.")
				.append(
					"\n\tCurrent size: "
						+ (unusedPool.size() + usedPool.size()))
				.append("\n\tTrying to open: " + addConnections)
				.append("\n\tMax allowed: " + maxConnections);
			throw new SQLException(msg.toString());
		}

		for (int i = 0; i < addConnections; i++) {
			PooledConnection newConnection =
				newPooledConnection(userName, password);
			newConnection.addConnectionEventListener(this);
			unusedPool.add(newConnection);
		}
	}

	private void shrinkPool(int closeConnections) throws SQLException {
		int close =
			unusedPool.size() < closeConnections
				? unusedPool.size()
				: closeConnections;
		int lastInd = unusedPool.size() - close;

		for (int i = unusedPool.size() - 1; i >= lastInd; i--) {
			PooledConnection con = (PooledConnection) unusedPool.remove(i);
			con.close();
		}
	}

	/** 
	 * Returns maximum number of connections this pool can keep.
	 * This parameter when configured allows to limit the number of simultaneously
	 * open connections.
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/** Returns the absolute minimum number of connections allowed 
	  * in this pool at any moment in time. */
	public int getMinConnections() {
		return minConnections;
	}

	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	/** Returns a database URL used to initialize this pool.
	  * Will return null if the pool was initialized with ConnectionPoolDataSource. */
	public String getDataSourceUrl() {
		return dataSourceUrl;
	}

	/** Returns a name of a JDBC driver used to initialize this pool.
	  * Will return null if the pool was initialized with ConnectionPoolDataSource. */
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	/** Returns a data source password used to initialize this pool. */
	public String getPassword() {
		return password;
	}

	/** Returns a data source user name used to initialize this pool. */
	public String getUserName() {
		return userName;
	}

	/** 
	 * Returns the number of connections obtained via this DataSource
	 * that are currently in use by the DataSource clients. 
	 */
	public int getCurrentlyInUse() {
		return usedPool.size();
	}

	/** 
	 * Returns the number of connections maintained in the 
	 * pool that are currently not used by any clients and are
	 * available immediately via <code>getConnection</code> method. 
	 */
	public int getCurrentlyUnused() {
		return unusedPool.size();
	}

	/** 
	 * Returns connection from the pool using internal values of user name
	 * and password. Eqivalent to calling: 
	 * 
	 * <p><code>ds.getConnection(ds.getUserName(), ds.getPassword())</code></p> 
	 */
	public synchronized Connection getConnection() throws SQLException {
		return getConnection(userName, password);
	}

	/** Returns connection from the pool. */
	public Connection getConnection(String userName, String password)
		throws SQLException {

		// security check
		int totalCon = usedPool.size() + unusedPool.size();
		if (totalCon > maxConnections) {
			shrinkPool(totalCon - maxConnections);
		}

		// increase pool if needed
		// if further increase is not possible
		// (say we exceed the maximum number of connections)
		// this will throw an SQL exception...
		if (unusedPool.size() == 0) {
			growPool(1, userName, password);
		}

		int lastObjectInd = unusedPool.size() - 1;
		PooledConnection pooledConn =
			(PooledConnection) unusedPool.remove(lastObjectInd);
		usedPool.add(pooledConn);
		return pooledConn.getConnection();
	}

	public int getLoginTimeout() throws java.sql.SQLException {
		return poolDataSource.getLoginTimeout();
	}

	public void setLoginTimeout(int seconds) throws java.sql.SQLException {
		poolDataSource.setLoginTimeout(seconds);
	}

	public PrintWriter getLogWriter() throws java.sql.SQLException {
		return poolDataSource.getLogWriter();
	}

	public void setLogWriter(PrintWriter out) throws java.sql.SQLException {
		poolDataSource.setLogWriter(out);
	}

	/** Returns closed connection to the pool. */
	public synchronized void connectionClosed(ConnectionEvent event) {
		// return connection to the pool
		PooledConnection closedConn = (PooledConnection) event.getSource();

		// remove this connection from the list of connections
		// managed by this pool...
		int usedInd = usedPool.indexOf(closedConn);
		if (usedInd >= 0) {
			usedPool.remove(usedInd);
			unusedPool.add(closedConn);
		}
		// else ....
		// other possibility is that this is a bad connection, so just ignore its closing event,
		// since it was unregistered in "connectionErrorOccurred"
	}

	/** 
	 * Removes connection with an error from the pool. This method
	 * is called by PoolManager connections on connection errors
	 * to notify PoolManager that connection is in invalid state.
	 */
	public synchronized void connectionErrorOccurred(ConnectionEvent event) {
		// later on we should analize the error to see if this
		// is fatal... right now just kill this PooledConnection

		PooledConnection errorSrc = (PooledConnection) event.getSource();

		// remove this connection from the list of connections
		// managed by this pool...

		int usedInd = usedPool.indexOf(errorSrc);
		if (usedInd >= 0)
			usedPool.remove(usedInd);
		else {
			int unusedInd = unusedPool.indexOf(errorSrc);
			if (unusedInd >= 0)
				unusedPool.remove(unusedInd);
		}

		// do not close connection,
		// let the code that catches the exception handle it
		// ....
	}
}