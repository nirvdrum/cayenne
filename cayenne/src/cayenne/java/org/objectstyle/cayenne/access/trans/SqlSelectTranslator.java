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
package org.objectstyle.cayenne.access.trans;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.query.SqlSelectQuery;

/** 
 * Class works as a translator of raw SELECT queries to JDBC statements.
 * 
 * @author Andrei Adamchik
 */
public class SqlSelectTranslator extends SelectQueryAssembler {
	private static Logger logObj = Logger.getLogger(SqlSelectTranslator.class);

	public String createSqlString() throws Exception {
		return getRawQuery().getSqlString();
	}

	public int getFetchLimit() {
		return getRawQuery().getFetchLimit();
	}

	private final SqlSelectQuery getRawQuery() {
		return (SqlSelectQuery) query;
	}

	/**     
	 * Returns an ordered array of DbAttributes that describe the
	 * result columns in the in the ResultSet. Uses ResultSet info. 
	 */
	public DbAttribute[] getSnapshotDesc(ResultSet rs) {
		DbAttribute[] attrs = getRawQuery().getResultDescriptors();
		return (attrs == null || attrs.length == 0)
			? getSnapshotLabelsFromMetadata(rs)
			: attrs;
	}

	/** 
	 * Returns ordered array of DbAttributes for the result set.
	 * This is a failover method to obtain result description when 
	 * query has no description data. It is called internally from 
	 * "getSnapshotDesc".
	 * 
	 * <p><i>Note that DbAttributes created by this method do not belong to
	 * any entity and only have their name and type initialized.</i></p>
	 */
	public DbAttribute[] getSnapshotLabelsFromMetadata(ResultSet rs) {
		try {
			ResultSetMetaData md = rs.getMetaData();
			int len = md.getColumnCount();
			if (len == 0) {
				throw new CayenneRuntimeException("No columns in ResultSet.");
			}

			DbAttribute[] desc = new DbAttribute[len];
			for (int i = 0; i < len; i++) {

				// figure out column name
				String name = md.getColumnLabel(i + 1);
				if (name == null || name.length() == 0) {
					name = md.getColumnName(i + 1);

					if (name == null || name.length() == 0) {
						name = "column_" + (i + 1);
					}
				}

				desc[i] = new DbAttribute();
				desc[i].setName(name);
				desc[i].setType(md.getColumnType(i + 1));
			}
			return desc;
		} catch (SQLException sqex) {
			throw new CayenneRuntimeException("Error reading metadata.", sqex);
		}
	}

	/** Returns ordered list of Java class names that
	  *  should be used for fetched values. */
	public String[] getResultTypes(ResultSet rs) {
		ObjAttribute[] attrs = getRawQuery().getObjDescriptors();
		if (attrs == null || attrs.length == 0) {
			return getResultTypesFromMetadata(rs);
		}

		int len = attrs.length;
		String[] types = new String[len];
		for (int i = 0; i < len; i++) {
			types[i] = attrs[i].getType();
		}
		return types;
	}

	/** Returns ordered list of Java class names that
	  * should be used for fetched values according to default Java
	  * class to JDBC type mapping. This is a failover method to
	  * obtain Java types used when query has no such data. */
	public String[] getResultTypesFromMetadata(ResultSet rs) {
		try {
			ResultSetMetaData md = rs.getMetaData();
			int len = md.getColumnCount();
			if (len == 0)
				throw new CayenneRuntimeException("No columns in ResultSet.");

			String[] types = new String[len];
			for (int i = 0; i < len; i++) {
				int sqlType = md.getColumnType(i + 1);
				types[i] = TypesMapping.getJavaBySqlType(sqlType);
			}
			return types;
		} catch (SQLException sqex) {
			logObj.error("Error", sqex);
			throw new CayenneRuntimeException("Error reading metadata.", sqex);
		}
	}

	public String aliasForTable(DbEntity dbEnt) {
		throw new RuntimeException("aliases not supported");
	}

	public void dbRelationshipAdded(DbRelationship dbRel) {
		throw new RuntimeException("db relationships not supported");
	}

	public boolean supportsTableAliases() {
		return false;
	}
}