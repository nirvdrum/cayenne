/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
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

import java.util.Collection;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Query;


/**
 * Defines methods used to run Cayenne queries.
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public interface QueryEngine {
    
    /**
     * Executes queries in the transactional context provided by the caller. 
     * It is caller's responsibility to commit or rollback the Transaction 
     * and close any connections that were added to it.
     * 
     * @since 1.1
     * @see OperationObserver
     * @see Transaction
     */
    public void performQueries(Collection queries, OperationObserver resultConsumer, Transaction transaction);

    /** 
     * Executes a list of queries wrapping them in its own transaction. 
     * Results of execution are passed to {@link OperationObserver} object via its 
     * callback methods.
     * 
     * @since 1.1 The signiture has changed from List to Collection.
     */
    public void performQueries(Collection queries, OperationObserver resultConsumer);

    /**
     * Executes a single query. Will notify <code>resultConsumer</code>
     * about query progress and results.
     *
     * @deprecated Since 1.1 use {@link #performQueries(java.util.Collection,OperationObserver,Transaction)}
     */
    public void performQuery(Query query, OperationObserver resultConsumer);

   	/** 
     * Returns a DataNode that handles database operations for
     * a specified <code>ObjEntity</code>.
     * 
     * @deprecated Since 1.1 use {@link #lookupDataNode(DataMap)} since
     * queries are not necessarily based on an ObjEntity.
     */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity);
    
    /**
     * Returns a DataNode that should handle queries for all
     * DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap);

    /**
     * Returns a resolver for this query engine that is capable of resolving
     * between classes, entity names, and obj/db entities
     */
    public EntityResolver getEntityResolver();

	/** 
	 * Returns a collection of DataMaps associated with this QueryEngine.
	 */
	public Collection getDataMaps();
}

