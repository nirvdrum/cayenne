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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

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
	protected ResultSet resultSet;
	protected Map dataRow;
	protected DbAttribute[] rowDescriptor;
	protected String[] rowTypes;
	protected ExtendedType[] converters;

    /** 
     * Creates new DefaultResultIterator. Initializes it
     * with ResultSet and query metadata.
     */
	public DefaultResultIterator(
		ResultSet resultSet,
		DbAdapter adapter,
		SelectQueryAssembler queryAssembler)
		throws SQLException, CayenneException {
			
		this.resultSet = resultSet;
		this.rowDescriptor = queryAssembler.getSnapshotDesc(resultSet);
        this.rowTypes = queryAssembler.getResultTypes(resultSet);
                          
        int len = rowDescriptor.length;
        converters = new ExtendedType[len];
        ExtendedTypeMap typeMap = adapter.getTypeConverter();
        for (int i = 0; i < len; i++) {
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

		return dataRow;
	}

	/** 
	 * Reads a row from the internal ResultSet at the current
	 * cursor position.
	 */
	protected void readDataRow() throws CayenneException {
		throw new CayenneException("Not implemented yet.");
	}

	/** 
	 * Closes ResultIterator and associated ResultSet. This method must be
	 * called explicitly when the user is finished processing the records.
	 * Otherwise unused database resources will not be released properly.
	 */
	public void close() throws CayenneException {
		throw new CayenneException("Not implemented yet.");
	}
}
