package org.objectstyle.cayenne.access.trans;
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

import java.sql.Connection;
import java.util.*;
import org.apache.log4j.Logger;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.query.InsertQuery;
import org.objectstyle.cayenne.query.Query;

/** Class implements default translation mechanism of org.objectstyle.cayenne.query.InsertQuery
  * objects to SQL INSERT statements. Note that in order for this query to execute successfully,
  * ObjectId contained within InsertQuery must be fully initialized.
  *
  * @author Andrei Adamchik  
 */
public class InsertTranslator extends QueryAssembler {
	static Logger logObj = Logger.getLogger(InsertTranslator.class.getName());

	private ArrayList columnList = new ArrayList();

	public String aliasForTable(DbEntity dbEnt) {
		throw new RuntimeException("aliases not supported");
	}

	public void dbRelationshipAdded(DbRelationship dbRel) {
		throw new RuntimeException("db relationships not supported");
	}

	/** Method that converts an insert query into SQL string */
	public String createSqlString() throws Exception {
		prepareLists();
		StringBuffer queryBuf = new StringBuffer("INSERT INTO ");
		DbEntity dbE = engine.getEntityResolver().lookupDbEntity(query);
		queryBuf.append(dbE.getFullyQualifiedName()).append(" (");

		int len = columnList.size();

		// 1. Append column names

		// unroll the loop to avoid condition checking in the loop
		queryBuf.append(columnList.get(0)); // assume we have at least 1 column
		for (int i = 1; i < len; i++) {
			queryBuf.append(", ").append(columnList.get(i));
		}

		// 2. Append values ('?' in place of actual parameters)
		queryBuf.append(") VALUES (");
		if (len > 0) {
			queryBuf.append('?');
			for (int i = 1; i < len; i++) {
				queryBuf.append(", ?");
			}
		}

		queryBuf.append(')');
		return queryBuf.toString();
	}

	public InsertQuery insertQuery() {
		return (InsertQuery) query;
	}

	/** Creates 2 matching lists: columns names and values */
	private void prepareLists() throws Exception {
		//DbEntity dbE =
		//	engine.lookupEntity(query.getObjEntityName()).getDbEntity();
		DbEntity dbE = engine.getEntityResolver().lookupDbEntity(query);
		ObjectId oid = insertQuery().getObjectId();
		Map id = (oid != null) ? oid.getIdSnapshot() : null;

		if (id != null) {
			Iterator idIt = id.keySet().iterator();
			while (idIt.hasNext()) {
				String attrName = (String) idIt.next();
				Attribute attr = dbE.getAttribute(attrName);
				Object attrValue = id.get(attrName);
				columnList.add(attrName);
				values.add(attrValue);
				attributes.add(attr);
			}
		}

		Map snapshot = insertQuery().getObjectSnapshot();
		Iterator columnsIt = snapshot.keySet().iterator();
		while (columnsIt.hasNext()) {
			String attrName = (String) columnsIt.next();

			// values taken from ObjectId take precedence.
			if (id != null && id.get(attrName) != null)
				continue;

			Attribute attr = dbE.getAttribute(attrName);
			Object attrValue = snapshot.get(attrName);
			columnList.add(attrName);
			values.add(attrValue);
			attributes.add(attr);
		}
	}
}
