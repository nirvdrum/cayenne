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

package org.objectstyle.cayenne.access.util;

import java.util.*;

import org.apache.log4j.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.query.*;

/** 
 * OperationObserver that accumulates select query results provided 
 * by callback methods. Later the results can be retrieved
 * via different <code>getResults</code> methods. 
 * 
 * <p>This class can serve as a helper for classes that work with 
 * DataNode directly, bypassing DataContext. Also it is used by DataContext
 * to implement "data rows" functionality - retrieving data without 
 * instantiating and registering DataObjects.
 * </p>
 * 
 * <p>If exceptions happen during the execution, they are immediately rethrown.
 * </p>
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 *  @author Andrei Adamchik
 */
public class SelectObserver extends DefaultOperationObserver {
	protected Map results = new HashMap();
	protected int selectCount;

	public SelectObserver() {}
	
	public SelectObserver(Level logLevel) {
		super.setLoggingLevel(logLevel);
	}

	/** 
	 * Returns a count of select queries that returned results
	 * since the last time "clear" was called, or since this object
	 * was created.
	 */
	public int getSelectCount() {
		return selectCount;
	}

	/** 
	 * Returns a list of result snapshots for the specified query,
	 * or null if this query has never produced any results.
	 */
	public List getResults(Query q) {
		return (List) results.get(q);
	}

	/** 
	 * Returns query results accumulated during query execution with this
	 * object as an operation observer. 
	 */
	public Map getResults() {
		return results;
	}

	/** Clears fetched objects stored in an internal list. */
	public void clear() {
		selectCount = 0;
		results.clear();
	}

	/** 
	 * Stores all objects in <code>dataRows</code> in an internal
	 * result list. 
	 */
	public void nextDataRows(Query query, List dataRows) {
		super.nextDataRows(query, dataRows);
		if (dataRows != null) {
			results.put(query, dataRows);
		}

		selectCount++;
	}

	/** 
	 * Overrides superclass implementation to rethrow an exception
	 *  immediately. 
	 */
	public void nextQueryException(Query query, Exception ex) {
		super.nextQueryException(query, ex);
		throw new CayenneRuntimeException("Query exception.", ex);
	}

	/** 
	 * Overrides superclass implementation to rethrow an exception
	 * immediately. 
	 */
	public void nextGlobalException(Exception ex) {
		super.nextGlobalException(ex);
		throw new CayenneRuntimeException("Global exception.", ex);
	}
}