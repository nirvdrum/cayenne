package org.objectstyle.cayenne.conn;
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

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.*;

/**
 *   <p>This class allows to generate pooled connections.</p>
 *
 *   <p>It is implemented as a wrapper around a non-pooled data source object. Delegates all method
 *   calls except for "getPooledConnection" to the underlying datasource...
 *   </p>
 *
 */
public class PoolDataSource implements ConnectionPoolDataSource {
	static Logger logObj = Logger.getLogger(PoolDataSource.class.getName());

	private DataSource nonPooledDatasource;

	/** Creates new PoolDataSource */
	public PoolDataSource(DataSource nonPooledDatasource) {
		logObj.fine("Init new datasource");
		this.nonPooledDatasource = nonPooledDatasource;
	}

	public PoolDataSource(String jdbcDriver, String connectionUrl) throws SQLException {
		nonPooledDatasource = new DriverDataSource(jdbcDriver, connectionUrl);
	}

	public int getLoginTimeout() throws java.sql.SQLException {
		return nonPooledDatasource.getLoginTimeout();
	}

	public void setLoginTimeout(int seconds) throws java.sql.SQLException {
		nonPooledDatasource.setLoginTimeout(seconds);
	}

	public PrintWriter getLogWriter() throws java.sql.SQLException {
		return nonPooledDatasource.getLogWriter();
	}

	public void setLogWriter(PrintWriter out) throws java.sql.SQLException {
		nonPooledDatasource.setLogWriter(out);
	}

	public PooledConnection getPooledConnection() throws java.sql.SQLException {
		return new PooledConnectionImpl(nonPooledDatasource.getConnection());
	}

	public PooledConnection getPooledConnection(String user, String password) throws java.sql.SQLException {
		return new PooledConnectionImpl(nonPooledDatasource.getConnection(user, password));
	}
}
