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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectstyle.cayenne.query.Query;


/** 
 * Simple implementation of OperationObserver interface. 
 * Useful as a superclass of other implementations of OperationObserver.
 * This class would collect useful ststistics about the operation process. 
 * 
 * <p><i>For more information see <a href="../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrei Adamchik
 */
public class DefaultOperationObserver implements OperationObserver {
    static Logger logObj = Logger.getLogger(DefaultOperationObserver.class.getName());
    
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    
    protected ArrayList globalExceptions = new ArrayList();
    protected HashMap queryExceptions = new HashMap();
    protected boolean transactionCommitted;
    protected boolean transactionRolledback;
    protected Level queryLogLevel = DEFAULT_LOG_LEVEL;
    protected boolean useIteratedResult;
    
    
    /** Returns a list of global exceptions that occured during data operation run. */
    public List getGlobalExceptions() {
        return globalExceptions;
    }
    
    
    /** Returns a list of exceptions that occured during data operation run by query. */
    public HashMap getQueryExceptions() {
        return queryExceptions;
    }
    
    /** Returns <code>true</code> if at least one exception was registered
      * during query execution. */
    public boolean hasExceptions() {
        return globalExceptions.size() > 0 || queryExceptions.size() > 0;
    }
    
    
    public boolean isTransactionCommitted() {
        return transactionCommitted;
    }  
    
    
    public boolean isTransactionRolledback() {
        return transactionRolledback;
    } 
    
    /** Returns Level.INFO as a default logging level. */
    public Level queryLogLevel() {
        return queryLogLevel;
    }
    
    /** 
     * Sets log level that should be used for queries. 
     * If <code>level</code> argument is null, level is set to
     * DEFAULT_LOG_LEVEL. If <code>level</code> is equal or higher
     * than log level configured for QueryLogger, query SQL statements
     * will be logged.
     */
    public void setQueryLogLevel(Level level) {
        this.queryLogLevel = (level == null) ? DEFAULT_LOG_LEVEL : level;
    }
    
    public void nextCount(Query query, int resultCount) {
        logObj.fine("update count: " + resultCount);
    }
    
    
    public void nextDataRows(Query query, List dataRows) {
        int count = (dataRows == null) ? -1 : dataRows.size();
        logObj.fine("result count: " + count);
    }
    
    public void nextDataRows(ResultIterator it, Query q) {
    	throw new RuntimeException("Not implemented.");
    }
    
    
    public void nextQueryException(Query query, Exception ex) {
        logObj.log(Level.WARNING, "query exception", ex);
        queryExceptions.put(query, ex);
    }
    
    
    public void nextGlobalException(Exception ex) {
        logObj.log(Level.WARNING, "global exception", ex);
        globalExceptions.add(ex);
    }
    
    
    public void transactionCommitted() {
        logObj.fine("transaction committed");
        transactionCommitted = true;
    }
    
    public void transactionRolledback() {
        logObj.fine("*** transaction rolled back");
        transactionRolledback = true;
    }
    
    
    /** Returns <code>true</code> so that individual queries are executed in separate
     *  transactions. */
    public boolean useAutoCommit() {
        return true;
    }
    
    /** Returns query list without altering its ordering. */
    public List orderQueries(DataNode aNode, List queryList) {
        return queryList;
    }
    
    public boolean useIteratedResult() {
    	return useIteratedResult;
    }
    
    public void setUseIteratedResult(boolean flag) {
    	this.useIteratedResult = flag;
    }
}
