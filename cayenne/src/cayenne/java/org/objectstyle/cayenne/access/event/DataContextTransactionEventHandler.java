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

package org.objectstyle.cayenne.access.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.event.ObserverManager;

public class DataContextTransactionEventHandler
	extends Object
	implements DataContextEventListener {

	// shared instance that will be registered for DataContext events
	private static DataContextTransactionEventHandler _instance = null;

	public static synchronized void initialize() {
		if (_instance == null) {
			_instance = new DataContextTransactionEventHandler();

			try {
				ObserverManager mgr = ObserverManager.getDefaultManager();
				mgr.addObserver(_instance, DataContextEvent.class, "dataContextWillCommit", DataContext.WILL_COMMIT);
				mgr.addObserver(_instance, DataContextEvent.class, "dataContextDidCommit", DataContext.DID_COMMIT);
			}

			catch (NoSuchMethodException ex) {
				// this can never happen, we define the appropriate methods in this class
				throw new CayenneRuntimeException(ex);
			}
		}
	}

	// holds per-manager collecton of objects
	private Collection _objectsToBeNotified;

	private DataContextTransactionEventHandler() {
		super();
	}

	public void dataContextWillCommit(DataContextEvent event) {
		DataContext ctx = (DataContext)event.getSource();

		// build a list of objects that will be send the observerEvents and cache
		// them here. Later, when notifying about a successful completion of a
		// transaction we cannot build this list anymore since all the work will be done then
		_objectsToBeNotified = this.getEventRecipientsFromDataContext(ctx);
		Iterator iter = _objectsToBeNotified.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			if (element instanceof DataObjectTransactionEventListener) {
				((DataObjectTransactionEventListener)element).willCommit();
			}		
		}
	}

	public void dataContextDidCommit(DataContextEvent event) {
		Iterator iter = _objectsToBeNotified.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			if (element instanceof DataObjectTransactionEventListener) {
				((DataObjectTransactionEventListener)element).didCommit();
			}
		}
		
		// done notifying everything. Release the collection early
		_objectsToBeNotified = null;
	}
	
	private Collection getEventRecipientsFromDataContext(DataContext ctx) {
		ArrayList candidates = new ArrayList();
		candidates.addAll(ctx.deletedObjects());
		candidates.addAll(ctx.modifiedObjects());
		candidates.addAll(ctx.newObjects());
		return candidates;
	}
}
