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

package org.objectstyle.cayenne.perform.test;

import java.sql.Connection;
import java.sql.Statement;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.perform.CayennePerformanceTest;
import org.objectstyle.cayenne.perform.PerformMain;


/** Simple performance test. Performs an insert of 1000 rows to the database, 
 *  then a select of the same rows. Compares performance of Cayenne and straight
 *  JDBC queries.
 */
public class InsertTest extends CayennePerformanceTest {
	public static final int objCount = 2000;
	
	protected DataContext ctxt;

	public InsertTest(String name) {
		super("Insert");
		super.setDesc("Inserting " + objCount + " objects.");
	}

	public void prepare() throws Exception {
		ctxt = PerformMain.sharedDomain.createDataContext();
	}
	
	
	public void runTest() throws Exception {
		for (int i = 0; i < objCount; i++) {
			Artist a = (Artist) ctxt.createAndRegisterNewObject("Artist");
			a.setArtistName("name_" + i);
		}

		// save all at once
		ctxt.commitChanges();
	}
/*

	private void testCayenneSmallInserts(DataContext ctxt) throws Exception {
		long t1 = System.currentTimeMillis();

		for (int j = 0; j < batchCount; j++) {
			int startInd = j * 20;
			for (int i = 0; i < 20; i++) {
				Artist a = (Artist) ctxt.createAndRegisterNewObject("Artist");
				a.setArtistName("name_" + (startInd + i));
			}
			// save batch
			ctxt.commitChanges();
		}

		long t2 = System.currentTimeMillis();
		insertCayenne1 = t2 - t1;
	}

	private void testCayenneSelect(DataContext ctxt) throws Exception {
		long t2 = System.currentTimeMillis();

		// fetch to the same context
		SelectQuery q = new SelectQuery("Artist");
		ctxt.performQuery(q);
		long t3 = System.currentTimeMillis();

		// fetch to a different context with no cache
		domain.createDataContext().performQuery(q);
		long t4 = System.currentTimeMillis();

		// fetch into data domain 
		domain.performQuery(q, new SelectObserver());
		long t5 = System.currentTimeMillis();

		// fetch into the new context in small chunks 
		ArrayList in = new ArrayList();
		Expression listE = ExpressionFactory.unaryExp(Expression.LIST, in);
		Expression e =
			ExpressionFactory.binaryPathExp(Expression.IN, "artistName", listE);
		q.setQualifier(e);

		for (int j = 0; j < batchCount; j++) {
			int startInd = j * 20;
			int endInd = startInd + 20;
			in.clear();
			for (int i = startInd; i < endInd; i++) {
				in.add("name_" + i);
			}
			ctxt.performQuery(q);
		}
		long t6 = System.currentTimeMillis();

		selectCayenne1 = t3 - t2;
		selectCayenne2 = t4 - t3;
		selectCayenne3 = t5 - t4;
		selectCayenne4 = t6 - t5;
	}

	public void testJDBC() throws Exception {
		cleanup();

		long t1 = System.currentTimeMillis();
		Connection con = getConnection();
		con.setAutoCommit(false);
		PreparedStatement st =
			con.prepareStatement(
				"INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (?, ?)");
		for (int i = 1; i <= objCount; i++) {
			st.setInt(1, i);
			st.setString(2, "name_" + i);
			st.executeUpdate();
		}

		// save all at once
		con.commit();
		con.close();
		long t2 = System.currentTimeMillis();

		// get connection again, since this 
		// this way it is a fair comparison to Cayenne
		con = getConnection();

		Statement sel = con.createStatement();
		ResultSet rs =
			sel.executeQuery("SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
		while (rs.next()) {
			Artist a = new Artist();
			a.setArtistName(rs.getString(2));
		}
		con.close();
		long t3 = System.currentTimeMillis();

		insertJDBC = t2 - t1;
		selectJDBC = t3 - t2;
	}
*/
}