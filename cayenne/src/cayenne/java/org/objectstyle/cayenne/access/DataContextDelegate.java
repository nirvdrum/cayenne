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

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.query.GenericSelectQuery;

/**
 * Defines API for a DataContext "delegate" - an object that is temporarily passed control 
 * by DataContext at some critical points in the normal flow of execution. A delegate thus can 
 * modify the flow, abort an operation, modify the objects participating in an operation, 
 * or perform any other tasks it deems necessary. DataContextDelegate is shared by DataContext 
 * and its ObjectStore.
 * 
 * @see org.objectstyle.cayenne.access.DataContext
 * 
 * @author Mike Kienenberger
 * @author Andrus Adamchik
 * 
 * @since 1.1
 */
public interface DataContextDelegate {
	
    /** 
     * Invoked before a <code>GenericSelectQuery</code> is executed.  The delegate
     * may modify the <code>GenericSelectQuery</code> by returning a different
     * <code>GenericSelectQuery</code>, or may return null to discard the query.
     * 
     * @return the original or modified <code>GenericSelectQuery</code> or null to discard the query.
     */
	public GenericSelectQuery willPerformSelect(DataContext context, GenericSelectQuery query);

	/**
	 * Invoked by parent DataContext whenever a change is detected to the object snapshot.
	 * 
	 * <p>Note that this delegate method may not be invoked even if the database row
	 * has changed compared to the snapshot an update is built against. The reasons for 
	 * that are the latency of distributed notifications and the fact that sometiomes DataContext commit 
	 * operations may not be synchronized on DataRowStore.
	 * </p>
	 */
    public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore);
    
    /**
     * Invoked by ObjectStore whenever it is detected that a database
     * row was deleted for object. If a delegate returns <code>true</code>,
     * ObjectStore will change MODIFIED objects to NEW (resulting in recreating the 
     * deleted record on next commit) and all other objects - to TRANSIENT. 
     * To block this behavior, delegate should return <code>false</code>, and
     * possibly do its own processing.
     * 
     * @param object
     */
    public boolean shouldProcessDelete(DataObject object);
}

