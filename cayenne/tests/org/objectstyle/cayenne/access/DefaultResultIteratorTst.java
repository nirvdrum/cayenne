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

import java.sql.*;
import java.util.Map;

import junit.framework.TestCase;

import org.objectstyle.TestMain;
import org.objectstyle.cayenne.access.trans.SelectQueryAssembler;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.query.SelectQuery;

public class DefaultResultIteratorTst extends TestCase {

	private DefaultResultIterator it;
	private Connection conn;
	private PreparedStatement st;

	public DefaultResultIteratorTst(String name) {
		super(name);
	}

	public void setUp() throws java.lang.Exception {
		conn = null;
		st = null;
		it = null;

		TestMain.getSharedDatabaseSetup().cleanTableData();
		new DataContextTst("noop").populateTables();
	}

	public void testCheckNextRow() throws java.lang.Exception {
		try {
			createIterator();

			assertNotNull(it.dataRow);
			it.checkNextRow();
			assertNotNull(it.dataRow);

		} finally {
			cleanup();
		}
	}

	public void testHasNextRow() throws java.lang.Exception {
		try {
			createIterator();
			assertTrue(it.hasNextRow());
		} finally {
			cleanup();
		}
	}

	public void testNextDataRow() throws java.lang.Exception {
		try {
			createIterator();

			// must be as many rows as we have artists
			// inserted in the database
			for (int i = 0; i < DataContextTst.artistCount; i++) {
				assertTrue(it.hasNextRow());
				it.nextDataRow();
			}

			// rows must end here
			assertTrue(!it.hasNextRow());

		} finally {
			cleanup();
		}
	}

	public void testReadDataRow() throws java.lang.Exception {
		try {
			createIterator();

			// must be as many rows as we have artists
			// inserted in the database
			Map dataRow = null;
			for (int i = 1; i <= DataContextTst.artistCount; i++) {
				assertTrue(it.hasNextRow());
				dataRow = it.nextDataRow();
			}

			assertEquals(
				"Failed row: " + dataRow,
				new DataContextTst("noop").artistName(9),
				dataRow.get("ARTIST_NAME"));

		} finally {
			cleanup();
		}
	}

	protected void cleanup() throws Exception {
		if (it != null) {
			it.close();
		}

		if (st != null) {
			st.close();
		}

		if (conn != null) {
			conn.close();
		}
	}

	protected void createIterator() throws Exception {
		conn = TestMain.getSharedConnection();

		DbAdapter adapter = TestMain.getSharedNode().getAdapter();
		SelectQuery q = new SelectQuery("Artist");
		q.addOrdering("artistName", true);

		SelectQueryAssembler assembler =
			(SelectQueryAssembler) adapter.getQueryTranslator(q);
		assembler.setEngine(TestMain.getSharedNode());

		assembler.setCon(conn);

		st =
			assembler.createStatement(
				DefaultOperationObserver.DEFAULT_LOG_LEVEL);

		it = new DefaultResultIterator(st, adapter, assembler);
	}
}