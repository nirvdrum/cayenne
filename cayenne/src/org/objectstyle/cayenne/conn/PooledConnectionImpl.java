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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.sql.*;

/**
 *  Cayenne implementation of the pooling wrapper for the database connection
 *  as per J2EE spec. Most of the modern JDBC drivers should have its
 *  own implementation that should be used instead of this class...
 */
public class PooledConnectionImpl implements PooledConnection {
    static Logger logObj = Logger.getLogger(PooledConnectionImpl.class.getName());
    
    private Connection connectionObj;
    private ArrayList connectionEventListeners;
    private boolean hadErrors;
    
    /** Creates new PooledConnection */
    public PooledConnectionImpl(Connection connectionObj) {
        this.connectionObj = connectionObj;
        connectionEventListeners = new ArrayList(10);
    }
    
    public void addConnectionEventListener(ConnectionEventListener listener) {
        synchronized(connectionEventListeners) {
            if(!connectionEventListeners.contains(listener))
                connectionEventListeners.add(listener);
        }
    }
    
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        synchronized(connectionEventListeners) {
            connectionEventListeners.remove(listener);
        }
    }
    
    public void close() throws java.sql.SQLException {
        connectionObj.close();
        connectionObj = null;
        
        // remove all listeners
        synchronized(connectionEventListeners) {
            connectionEventListeners.clear();
        }
    }
    
    public Connection getConnection() throws java.sql.SQLException {
        // set autocommit to false to return connection
        // always in consistent state
        if(!connectionObj.getAutoCommit()) {
            connectionObj.setAutoCommit(true);
        }
        
        return new ConnectionWrapper(connectionObj, this);
    }
    
    protected void returnConnectionToThePool() throws java.sql.SQLException  {
        // do not return to pool bad connections
        if(hadErrors)
            close();
        else   
            // notify the listeners that connection is no longer used by application...
            this.connectionClosedNotification();
    }
    
    /** This method creates and sents an event to listeners when an error occurs in the
     *  underlying connection. Listeners can have special logic to
     *  analyze the error and do things like closing this PooledConnection
     *  (if the error is fatal), etc...
     */
    protected void connectionErrorNotification(SQLException exception) {
        // hint for later to avoid returning bad connections to the pool
        hadErrors = true;
        
        synchronized(connectionEventListeners) {
            if(connectionEventListeners.size() == 0)
                return;
            
            ConnectionEvent closedEvent = new ConnectionEvent(this, exception);
            Iterator listeners = connectionEventListeners.iterator();
            while(listeners.hasNext()) {
                ConnectionEventListener nextListener = (ConnectionEventListener)listeners.next();
                nextListener.connectionErrorOccurred(closedEvent);
            }
        }
    }
    
    /** Creates and sends an event to listeners when a user closes
     *  java.sql.Connection object belonging to this PooledConnection.
     */
    protected void connectionClosedNotification() {
        synchronized(connectionEventListeners) {
            if(connectionEventListeners.size() == 0)
                return;
            
            ConnectionEvent closedEvent = new ConnectionEvent(this);
            Iterator listeners = connectionEventListeners.iterator();
            int i = 0;
            while(listeners.hasNext()) {
                ConnectionEventListener nextListener = (ConnectionEventListener)listeners.next();
                nextListener.connectionClosed(closedEvent);
            }
        }
    }
}
