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
import java.util.*;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.access.trans.SelectQueryAssembler;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;

/**
 * Default implementation of ResultIterator interface. It works as a 
 * factory that creates data rows from <code>java.sql.ResultSet</code>.
 * 
 * <p><i>For more information see <a href="../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrei Adamchik
 */
public class DefaultResultIterator implements ResultIterator {
	protected PreparedStatement prepStmt;
	protected ResultSet resultSet;
	protected Map dataRow;
	protected DbAttribute[] rowDescriptor;
	protected ExtendedType[] converters;
	protected int resultSize;

	/** 
	 * Creates new DefaultResultIterator. Initializes it
	 * with ResultSet and query metadata.
	 */
	public DefaultResultIterator(
		PreparedStatement prepStmt,
		DbAdapter adapter,
		SelectQueryAssembler queryAssembler)
		throws SQLException, CayenneException {
			
		this.prepStmt = prepStmt;
		this.resultSet = prepStmt.executeQuery();
		this.rowDescriptor = queryAssembler.getSnapshotDesc(resultSet);
		String[] rowTypes = queryAssembler.getResultTypes(resultSet);

		resultSize = rowDescriptor.length;
		converters = new ExtendedType[resultSize];
		ExtendedTypeMap typeMap = adapter.getTypeConverter();
		for (int i = 0; i < resultSize; i++) {
			converters[i] = typeMap.getRegisteredType(rowTypes[i]);
		}

		checkNextRow();
	}

	/** 
	 * Moves internal ResultSet cursor position down one row. 
	 * Checks if the next row is available.
	 */
	protected void checkNextRow() throws SQLException, CayenneException {
		dataRow = null;
		if (resultSet.next()) {
			readDataRow();
		}
	}

	/** 
	 * Returns true if there is at least one more record
	 * that can be read from the iterator.
	 */
	public boolean hasNextRow() {
		return dataRow != null;
	}

	/** 
	 * Returns the next result row as a Map.
	 */
	public Map nextDataRow() throws CayenneException {
		if (!hasNextRow()) {
			throw new CayenneException("An attempt to read uninitialized row or past the end of the iterator.");
		}

		Map row = dataRow;

		try {
			checkNextRow();
		} catch (SQLException sqex) {
			throw new CayenneException("Exception reading ResultSet.", sqex);
		}

		return row;
	}

	/**
	 * Returns all unread data rows from ResultSet and closes
	 * this iterator.
	 */
	public List dataRows() throws CayenneException {
		ArrayList list = new ArrayList();
		while (this.hasNextRow()) {
			list.add(this.nextDataRow());
		}

		this.close();

		return list;
	}

	/** 
	 * Reads a row from the internal ResultSet at the current
	 * cursor position.
	 */
	protected void readDataRow() throws SQLException, CayenneException {
		try {
			dataRow = new HashMap();

			// process result row columns,
			// set object properties right away,
			// FK & PK columns will be stored in temp maps that will be converted to id's later
			Object fetchedValue = null;
			for (int i = 0; i < resultSize; i++) {
				// note: jdbc column indexes start from 1 , not 0 as in arrays
				Object val =
					converters[i].materializeObject(
						resultSet,
						i + 1,
						rowDescriptor[i].getType());
				dataRow.put(rowDescriptor[i].getName(), val);
			}
		} catch (CayenneException cex) {
			// rethrow unmodified
			throw cex;
		} catch (Exception otherex) {
			throw new CayenneException(
				"Exception materializing column.",
				otherex);
		}
	}

	/** 
	 * Closes ResultIterator and associated ResultSet. This method must be
	 * called explicitly when the user is finished processing the records.
	 * Otherwise unused database resources will not be released properly.
	 */
	public void close() throws CayenneException {
		dataRow = null;
		
		try {
			resultSet.close();
		} catch (SQLException sqex) {
			throw new CayenneException("Exception closing ResultSet.", sqex);
		}

		try {
			prepStmt.close();
		} catch (SQLException sqex) {
			throw new CayenneException(
				"Exception closing PreparedStatement.",
				sqex);
		}
	}
}
