package org.objectstyle.cayenne.access;
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

import java.util.List;
import java.util.logging.Level;

import org.objectstyle.cayenne.query.Query;

/**
 * Interface used by QueryEngine to notify interested object about different 
 * stages of queries execution.
 *
 * Implementing objects are passed to a QueryEngine that will execute
 * one or more queries. QueryEngine will pass results of the execution 
 * of any kind of queries - selects, updates, store proc. calls, etc..
 * to the interested objects. This includes result counts, created objects, 
 * thrown exceptions, etc.
 * 
 * <p><i>For more information see <a href="../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @see org.objectstyle.cayenne.access.QueryEngine
 */
public interface OperationObserver {
    public void nextCount(Query query, int resultCount);
    
    /** Called when the next query results are read. */
    public void nextSnapshots(Query query, List resultSnapshots);
    
    public void nextQueryException(Query query, Exception ex);
    public void nextGlobalException(Exception ex);
    
    /** Returns a log level level that should be used when logging query execution. */ 
    public Level queryLogLevel();
    
    /** Called when a batch of queries was processed as a single transaction,
     *  and this transaction was successfully committed.
     */
    public void transactionCommitted();
    
    /** Called when a batch of queries was processed as a single transaction,
     *  and this transaction was failed and was rolled back. 
    */
    public void transactionRolledback();
    
    
    /** <p>DataNode executing a list of statements will consult OperationObserver
     *  about transactional behavior by calling this method.</p>
     * 
     *  <ul>
     *  <li>If this method returns true, each statement in a batch will be run as a separate 
     *  transaction. OperationObserver methods <code>transactionCommitted</code> and 
     *  <code>transactionRolledback</code> will not be invoked at all.
     *
     *  <li>If this method returns false, the whole batch will be wrapped in a transaction.
     *   OperationObserver methods <code>transactionCommitted</code> and 
     *  <code>transactionRolledback</code> will be called depending on the transaction outcome.
     *  </ul>
     */
    public boolean useAutoCommit();
    
    
    /** This method may be called by DataNode. It gives a chance to OperationObserver to order 
     *  queries to satisfy database referential integrity constraints.
     *
     *  @param aNode data node that is about to run a list of queries...
     *  @param queryList a list of queries being executed by QueryEngine as a single transaction
     *
     *  @return ordered query list (of course some implementations may just return unmodified original
     *  query list if they do not care about ordering)
     *
     */
    public List orderQueries(DataNode aNode, List queryList);
}

