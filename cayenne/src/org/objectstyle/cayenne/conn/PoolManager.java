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

import java.util.*;
import java.sql.*;
import javax.sql.*;
import java.io.PrintWriter;
import java.util.logging.*;
import org.objectstyle.cayenne.*;


/**
 * PoolManager is a DataSource impementation that hides connection pooling logic
 * from the users, acting as a normal DataSource. Application servers may provide their own
 * DataSources that handle pooling. In this case Cayenne should not use PoolManager, and should
 * use app server specific implementation instead.
 *
 * TODO: create a low priority thread that will do pool maintenance when CPU
 * cycles are not in a great demand.
 */
public class PoolManager implements DataSource, ConnectionEventListener {
    static Logger logObj = Logger.getLogger(PoolManager.class.getName());
    
    /** Constant that holds the name of the key
     *  that is used to retrieve database password from the map of parameters
     */
    
    private ConnectionPoolDataSource poolDataSource;
    private int minConnections;
    private int maxConnections;
    private ArrayList unusedPool;
    private ArrayList usedPool;
    
    
    /** Creates new PoolManager with the specified policy for
     *  connection pooling and a ConnectionPoolDataSource object.
     *
     *  @param poolDataSource Data source for pooled connections
     *  @param minConnections int >= 0 that specifies a minimum number of open connections
     *  to keep in the pool at all times
     *  @param maxConnections int >= minConnections >= 0 that specifies maximum number of connections
     *  to keep open in the pool.
     *  @param dataSourceInfo map of additional parameters needed to create a data source (can be null).
     *
     *  @throws SQLException if pool manager can not be created.
     */
    public PoolManager(
    ConnectionPoolDataSource poolDataSource, 
    int minCons, 
    int maxCons, 
    String userName, 
    String password
    ) throws SQLException {
        // do some sanity checks...
        if(maxConnections < 0)
            throw new SQLException("Maximum number of connections can not be negative (" + maxCons +").");
        
        if(minConnections < 0)
            throw new SQLException("Minimum number of connections can not be negative (" + minCons +").");
        
        if(minConnections > maxConnections)
            throw new SQLException("Minimum number of connections can not be bigger then maximum.");
        
        this.minConnections = minCons;
        this.maxConnections = maxCons;
        this.poolDataSource = poolDataSource;        
        
        usedPool = new ArrayList(maxConnections);
        unusedPool = new ArrayList(maxConnections);
        
        // create an initial connection pool...
        growPool(minConnections, userName, password);
    }
    
    private PooledConnection newPooledConnection(String userName, String password) throws SQLException {
        if(userName != null)
            return poolDataSource.getPooledConnection(userName, password);
        else
            return poolDataSource.getPooledConnection();
    }
    
    /** Close all existing connections, remove them from the pool. */
    public void dispose() throws SQLException {
        // clean connections from the pool
        
        ListIterator unusedIterator = unusedPool.listIterator();
        while (unusedIterator.hasNext()) {
            PooledConnection con = (PooledConnection)unusedIterator.next();
            // close connection
            con.close();
            // remove connection from the list
            unusedIterator.remove();
        }
        
        // clean used connections
        
        ListIterator usedIterator = usedPool.listIterator();
        while (usedIterator.hasNext()) {
            PooledConnection con = (PooledConnection)usedIterator.next();
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
    private void growPool(int addConnections, String userName, String password) throws SQLException {
        if(unusedPool.size() + usedPool.size() + addConnections > maxConnections)
            throw new SQLException("An attempt to open more connections then pool is allowed to handle.");
        
        for(int i = 0; i < addConnections; i++) {
            PooledConnection newConnection = newPooledConnection(userName, password);
            newConnection.addConnectionEventListener(this);
            unusedPool.add(newConnection);
        }
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public int getMinConnections() {
        return minConnections;
    }
    
    public int getCurrentlyInUse() {
        return usedPool.size();
    }
    
    public int getCurrentlyUnused() {
        return unusedPool.size();
    }
    
    /** DataSource interface method */
    public synchronized Connection getConnection() throws SQLException {
        return getConnection(null, null); 
    }
    
    /** DataSource interface method */
    public Connection getConnection(String userName, String password)  throws java.sql.SQLException {
        // increase pool if needed
        // if further increase is not possible
        // (say we exceed the maximum number of connections)
        // this will throw an SQL exception...
        if(unusedPool.size() == 0)
            growPool(1, userName, password);
        
        int lastObjectInd = unusedPool.size() - 1;
        PooledConnection pooledConn = (PooledConnection)unusedPool.remove(lastObjectInd);
        usedPool.add(pooledConn);
        return pooledConn.getConnection();
    }
    
    /** DataSource interface method */
    public int getLoginTimeout() throws java.sql.SQLException {
        return poolDataSource.getLoginTimeout();
    }
    
    /** DataSource interface method */
    public void setLoginTimeout(int seconds) throws java.sql.SQLException {
        poolDataSource.setLoginTimeout(seconds);
    }
    
    /** DataSource interface method */
    public PrintWriter getLogWriter() throws java.sql.SQLException {
        return poolDataSource.getLogWriter();
    }
    
    /** DataSource interface method */
    public void setLogWriter(PrintWriter out) throws java.sql.SQLException {
        poolDataSource.setLogWriter(out);
    }
    
    /** ConnectionEventListener interface method.
     *  This implementation returns closed connection
     *  to the pool.
     */
    public synchronized void connectionClosed(ConnectionEvent event) {
        // return connection to the pool
        PooledConnection closedConn = (PooledConnection)event.getSource();
        
        // remove this connection from the list of connections
        // managed by this pool...
        int usedInd = usedPool.indexOf(closedConn);
        if(usedInd >= 0) {
            usedPool.remove(usedInd);
            unusedPool.add(closedConn);
        }
        // else ....
        // other possibility is that this is a bad connection, so just ignore its closing event,
        // since it was unregistered in "connectionErrorOccurred"
    }
    
    
    /** ConnectionEventListener interface method */
    public synchronized void connectionErrorOccurred(ConnectionEvent event) {
        // later on we should analize the error to see if this
        // is fatal... right now just kill this PooledConnection
        
        PooledConnection errorSrc = (PooledConnection)event.getSource();
        
        // remove this connection from the list of connections
        // managed by this pool...
        
        
        int usedInd = usedPool.indexOf(errorSrc);
        if(usedInd >= 0)
            usedPool.remove(usedInd);
        else {
            int unusedInd = unusedPool.indexOf(errorSrc);
            if(unusedInd >= 0)
                unusedPool.remove(unusedInd);
        }
        
        // do not close connection,
        // let the code that catches the exception handle it
        // ....
    }
    
}
