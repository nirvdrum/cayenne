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

package org.objectstyle.cayenne.dba.oracle;

import java.sql.*;
import java.util.*;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DbEntity;

/** 
 * Sequence-based primary key generator implementation for Oracle. 
 * Uses Oracle sequences to generate primary key values. This approach is 
 * at least 50% faster when tested with Oracle compared to the lookup table
 * approach.
 * 
 * <p>When using Cayenne key caching mechanism, make sure that sequences in 
 * the database have "INCREMENT BY" greater or equal to OraclePkGenerator 
 * "pkCacheSize" property value. If this is not the case, you will need to
 * adjust PkGenerator value accordingly. For example when sequence is
 * incremented by 1 each time, use the following code:</p>
 * 
 * <pre>
 * dataNode.getAdapter().getPkGenerator().setPkCacheSize(1);
 * </pre>
 * 
 * @author Andrei Adamchik
 */
public class OraclePkGenerator extends JdbcPkGenerator {
	static Logger logObj = Logger.getLogger(OraclePkGenerator.class.getName());

	private static final String _SEQUENCE_PREFIX = "pk_";

	public void createAutoPk(DataNode node, List dbEntities) throws Exception {
		List sequences = getExistingSequences(node);
		
		// create needed sequences
		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			if (!sequences.contains(sequenceName(ent.getName()))) {
				runUpdate(node, createSequenceString(ent.getName()));
			}
		}
	}

	public List createAutoPkStatements(List dbEntities) {
		ArrayList list = new ArrayList();
		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			list.add(createSequenceString(ent.getName()));
		}

		return list;
	}

	public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
        List sequences = getExistingSequences(node);

		// create needed sequences
		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			if (sequences.contains(sequenceName(ent.getName()))) {
				runUpdate(node, dropSequenceString(ent.getName()));
			}
		}
	}

	public List dropAutoPkStatements(List dbEntities) {
		ArrayList list = new ArrayList();
		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			list.add(dropSequenceString(ent.getName()));
		}

		return list;
	}

	protected String createSequenceString(String entName) {
		StringBuffer buf = new StringBuffer();
		buf
			.append("CREATE SEQUENCE ")
			.append(sequenceName(entName))
			.append(" START WITH 200")
			.append(" INCREMENT BY ")
			.append(getPkCacheSize());
		return buf.toString();
	}

	/** 
	 * Returns a SQL string needed to drop any database objects associated 
	 * with automatic primary key generation process for a specific DbEntity. 
	 */
	protected String dropSequenceString(String entName) {
		StringBuffer buf = new StringBuffer();
		buf.append("DROP SEQUENCE ").append(sequenceName(entName));
		return buf.toString();
	}

	/** 
	 * Generates primary key by calling Oracle sequence corresponding to the
	 * <code>dbEntity</code>. Executed SQL looks like this:
	 * 
	 * <pre>
	 * SELECT pk_table_name.nextval FROM DUAL
	 * </pre>
	 */
	protected int pkFromDatabase(DataNode node, DbEntity ent)
		throws Exception {

		Connection con = node.getDataSource().getConnection();
		try {
			Statement st = con.createStatement();
			try {
				ResultSet rs =
					st.executeQuery(
						"SELECT "
							+ sequenceName(ent.getName())
							+ ".nextval FROM DUAL");
				try {
					Object pk = null;
					if (!rs.next()) {
						throw new CayenneRuntimeException(
							"Error generating pk for DbEntity "
								+ ent.getName());
					}
					return rs.getInt(1);
				} finally {
					rs.close();
				}
			} finally {
				st.close();
			}
		} finally {
			con.close();
		}
	}

	/** Returns expected primary key sequence name for a DbEntity. */
	protected String sequenceName(String entName) {
		return _SEQUENCE_PREFIX + entName.toLowerCase();
	}

	/** 
	 * Fetches a list of existing sequences that might match Cayenne
	 * generated ones.
	 */
	protected List getExistingSequences(DataNode node) throws SQLException {
		
		// check existing sequences
		Connection con = node.getDataSource().getConnection();

		try {
			Statement sel = con.createStatement();
			try {
				StringBuffer q = new StringBuffer();
				q.append(
					"SELECT LOWER(SEQUENCE_NAME) FROM ALL_SEQUENCES WHERE LOWER(SEQUENCE_NAME)");
				q.append(" LIKE '").append(_SEQUENCE_PREFIX).append("%'");
				ResultSet rs = sel.executeQuery(q.toString());
				try {
					List sequenceList = new ArrayList();
					while (rs.next()) {
						sequenceList.add(rs.getString(1));
					}
					return sequenceList;
				} finally {
					rs.close();
				}
			} finally {
				sel.close();
			}
		} finally {
			con.close();
		}
	}
}