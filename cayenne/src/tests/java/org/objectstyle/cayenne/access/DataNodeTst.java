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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * DataNode test cases.
 * 
 * @author Andrei Adamchik
 */
public class DataNodeTst extends IteratorTestBase {
	protected DataNode sharedNode;

	public void testRunSelect() throws Exception {
		SelectObserver observer = new SelectObserver();

		try {
			init();
			sharedNode.runSelect(conn, query, observer);
			assertEquals(
				DataContextTst.artistCount,
				observer.getResults(transl.getQuery()).size());
		} finally {
			cleanup();
		}
	}

	public void testRunIteratedSelect() throws Exception {
		IteratedObserver observer = new IteratedObserver();

		init();
		
		// first assert that created node is valid
		assertNotNull(sharedNode.getEntityResolver().lookupObjEntity(query));
		
		sharedNode.runSelect(conn, query, observer);
		assertEquals(DataContextTst.artistCount, observer.getResultCount());

		// no cleanup is needed, since observer will close the iterator
	}

	public void testFailIterated() throws Exception {
		// must fail multiple queries when one of them is iterated
		IteratedObserver observer = new IteratedObserver();

		List queries = new ArrayList();
		queries.add(new SelectQuery("Artist"));
		queries.add(new SelectQuery("Artist"));

		Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);

		try {
			getNode().performQueries(queries, observer);

			assertEquals(0, observer.getResultCount());
			assertTrue(
				"Iterated queries are not allowed in batches.",
				observer.hasExceptions());
		} finally {
			observerLogger.setLevel(oldLevel);
		}
	}

	protected DataNode newDataNode() {
		DataNode node = getNode().getAdapter().createDataNode("dummy");
		node.setDataMaps(getNode().getDataMaps());
		return node;
	}

	protected void init() throws Exception {
		super.init();
		sharedNode = newDataNode();
	}

	class IteratedObserver extends DefaultOperationObserver {
		protected int count;

		public boolean isIteratedResult() {
			return true;
		}

		public void nextDataRows(Query q, ResultIterator it) {
			super.nextDataRows(q, it);

			try {
				while (it.hasNextRow()) {
					it.nextDataRow();
					count++;
				}
			} catch (Exception ex) {
				throw new CayenneRuntimeException(
					"Error processing result iterator",
					ex);
			} finally {

				try {
					it.close();
				} catch (Exception ex) {
					throw new CayenneRuntimeException(
						"Error closing result iterator",
						ex);
				}
			}
		}

		public int getResultCount() {
			return count;
		}
	}
}
